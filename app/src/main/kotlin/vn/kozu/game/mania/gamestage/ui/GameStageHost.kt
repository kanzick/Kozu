package vn.kozu.game.mania.gamestage.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import vn.kozu.ui.i18n.KozuText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import vn.kozu.game.mania.gamestage.GameStageModule
import vn.kozu.game.mania.gamestage.chart.ManiaChart
import vn.kozu.game.mania.gamestage.scoring.GameResult
import vn.kozu.game.mania.gamestage.playtype.Portrait
import vn.kozu.game.mania.gamestage.session.GameSession
import vn.kozu.game.mania.previewstage.PreviewStageModule
import vn.kozu.game.mania.previewstage.model.DifficultyRecord
import vn.kozu.game.mania.previewstage.model.SongEntry
import vn.kozu.game.mania.previewstage.ui.PreviewStageScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

private class Selection(val song: SongEntry, val difficulty: DifficultyRecord, val chart: ManiaChart? = null)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Coordinates the full flow: list (PreviewStage) -> info popup -> gameplay -> results -> return to the popup.
 * Replaces direct calls to PreviewStageScreen.
 */
@Composable
fun GameStageHost(
    preview: PreviewStageModule,
    game: GameStageModule,
    isActive: Boolean,
    onAddMaps: () -> Unit,
    onGameplayChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(game.settings) }
    var initializationError by remember { mutableStateOf<String?>(null) }
    var settingsError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(game) {
        try {
            game.start()
            settings = game.settings
            initializationError = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            initializationError = e.message ?: "Could not initialize the game"
        }
    }

    var popup by remember { mutableStateOf<Selection?>(null) }
    var playing by remember { mutableStateOf<Selection?>(null) }
    var session by remember { mutableStateOf<GameSession?>(null) }
    var result by remember { mutableStateOf<ResultData?>(null) }
    var scoreRefresh by remember { mutableIntStateOf(0) }

    fun startPlay(sel: Selection, chart: ManiaChart) {
        preview.stopPreview()
        playing = Selection(sel.song, sel.difficulty, chart)
        result = null
        session = game.newSession(sel.song, sel.difficulty, chart)
        popup = null
    }

    fun backToPopup() {
        val sel = playing
        session = null
        result = null
        playing = null
        scoreRefresh++
        popup = sel?.let { Selection(it.song, it.difficulty) }
    }

    fun stopGameplay() {
        val activeSession = session ?: return
        activeSession.abort()
        backToPopup()
    }

    val currentSession = session
    val currentResult = result
    val currentPlaying = playing
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = LocalContext.current.findActivity()
    val lockOrientation = currentSession != null || currentResult != null
    DisposableEffect(activity, lockOrientation, settings.playType) {
        if (activity == null || !lockOrientation) {
            onDispose { }
        } else {
            val previousOrientation = activity.requestedOrientation
            activity.requestedOrientation = if (settings.playType == Portrait) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            onDispose { activity.requestedOrientation = previousOrientation }
        }
    }
    LaunchedEffect(isActive, currentSession) {
        if (!isActive) {
            preview.stopPreview()
            stopGameplay()
        }
    }
    DisposableEffect(lifecycleOwner, isActive, currentSession) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                preview.stopPreview()
                if (isActive) stopGameplay()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val currentOnGameplayChanged by rememberUpdatedState(onGameplayChanged)
    SideEffect {
        currentOnGameplayChanged(isActive && (currentSession != null || currentResult != null))
    }
    DisposableEffect(Unit) {
        onDispose { currentOnGameplayChanged(false) }
    }

    when {
        currentResult != null && currentPlaying != null -> ResultScreen(
            difficulty = currentPlaying.difficulty,
            data = currentResult,
            onRetry = { currentPlaying.chart?.let { startPlay(currentPlaying, it) } },
            onBack = ::backToPopup,
        )

        currentSession != null && currentPlaying != null -> PlayScreen(
            session = currentSession,
            onQuit = ::backToPopup,
            onRestart = { currentPlaying.chart?.let { startPlay(currentPlaying, it) } },
            onFinished = { r: GameResult ->
                result = ResultData(r)
                scope.launch {
                    result = try {
                        ResultData(r, outcome = game.submitResult(currentPlaying.song, currentPlaying.difficulty, r))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        ResultData(r, saveError = e.message ?: e.javaClass.simpleName)
                    }
                }
            },
        )

        else -> Box(modifier.fillMaxSize()) {
            PreviewStageScreen(
                module = preview,
                onAddMaps = onAddMaps,
                onDifficultyClick = { song, diff -> popup = Selection(song, diff) },
                loadBestScores = game::bestScores,
                scoresRefreshKey = scoreRefresh,
            )
            initializationError?.let { message ->
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        "Game setup failed: $message",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
            popup?.let { sel ->
                SongInfoDialog(
                    game = game,
                    song = sel.song,
                    difficulty = sel.difficulty,
                    settings = settings,
                    settingsError = settingsError,
                    refreshKey = scoreRefresh,
                    onSettingsChange = { settings = it },
                    onSettingsCommit = { s ->
                        scope.launch {
                            try {
                                game.updateSettings(s)
                                settingsError = null
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                settingsError = e.message ?: "Could not save game settings"
                            }
                        }
                    },
                    onPlay = { chart ->
                        scope.launch {
                            try {
                                game.start()
                                initializationError = null
                                startPlay(sel, chart)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                initializationError = e.message ?: "Could not initialize the game"
                            }
                        }
                    },
                    onDismiss = { popup = null },
                )
            }
        }
    }
}
