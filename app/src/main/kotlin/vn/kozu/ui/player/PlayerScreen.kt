package vn.kozu.ui.player

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import vn.kozu.domain.model.ArtworkShape
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.displayArtist
import vn.kozu.domain.model.displayTitle
import vn.kozu.player.PlaybackState
import vn.kozu.player.SleepTimer
import vn.kozu.ui.components.LocalAppSettings
import vn.kozu.ui.components.LocalPerformanceMode
import vn.kozu.ui.components.toShape
import vn.kozu.ui.theme.KozuMotion

/**
 * The full-screen player. The screen owns state, the backdrop and the sheets; what the player
 * actually looks like is chosen by the "Player design style" setting and lives in
 * [PlayerDesignContent].
 */
@Composable
fun PlayerScreen(
    beatmap: Beatmap,
    state: PlaybackState,
    position: StateFlow<Long>,
    duration: StateFlow<Long>,
    isFavorite: Boolean,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onFavorite: () -> Unit,
    onMore: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    onMapperClick: (String) -> Unit = {},
    onSkipToQueueItem: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    modifier: Modifier = Modifier,
    audioInfo: StateFlow<String?>? = null,
    sleepTimer: StateFlow<SleepTimer?>? = null,
    onSetSleepTimer: (Int) -> Unit = {},
    onSleepAtEndOfTrack: () -> Unit = {},
    onCancelSleepTimer: () -> Unit = {},
    onClearQueue: () -> Unit = {},
    onMoveQueueItem: (from: Int, to: Int) -> Unit = { _, _ -> },
    onSaveAsPlaylist: (List<Beatmap>) -> Unit = {},
    onToggleAutoplay: () -> Unit = {},
) {
    var showQueue by remember { mutableStateOf(false) }
    var showSleep by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val settings = LocalAppSettings.current
    val performanceMode = LocalPerformanceMode.current
    val timer by (sleepTimer ?: NoSleepTimer).collectAsStateWithLifecycle()
    val fileInfo by (audioInfo ?: NoAudioInfo).collectAsStateWithLifecycle()
    val design = settings.appearance.playerLayoutStyle
    val blurRadius = if (settings.blurPlayer && !performanceMode) settings.blurIntensity else 0
    val artistName = beatmap.displayArtist(settings.preferUnicode)

    val view = LocalView.current
    DisposableEffect(view, settings.keepScreenOn) {
        val previous = view.keepScreenOn
        if (settings.keepScreenOn) view.keepScreenOn = true
        onDispose { view.keepScreenOn = previous }
    }

    val coverScale by animateFloatAsState(
        // Keep the artwork visually large while paused; only use a subtle scale change.
        targetValue = if (state.isPlaying) 1f else 0.98f,
        animationSpec = KozuMotion.spatial(),
        label = "coverScale",
    )

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(Unit) { detectTapGestures { } },
    ) {
        if (!design.drawsOwnBackground()) {
            if (blurRadius > 0) {
                val backdropRequest =
                    remember(beatmap.id) {
                        ImageRequest
                            .Builder(context)
                            .data(beatmap.thumbUrl)
                            .crossfade(400)
                            .build()
                    }
                AsyncImage(
                    model = backdropRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = 0.55f }
                            .blur(blurRadius.dp, BlurredEdgeTreatment.Unbounded),
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    MaterialTheme.colorScheme.surface,
                                ),
                            ),
                        ),
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            ),
                        ),
                    ),
            )
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            // Resolve the full-player shape once and use the exact same mask for every design.
            val effectiveShape =
                if (settings.appearance.playerArtShape == ArtworkShape.DEFAULT) {
                    settings.appearance.artworkShape
                } else {
                    settings.appearance.playerArtShape
                }
            val playerArtShape = effectiveShape.toShape(RoundedCornerShape(24.dp))
            val queueLabel =
                if (state.queue.size > 1) "${state.queueIndex + 1} of ${state.queue.size}" else null

            val ui =
                PlayerUi(
                    beatmap = beatmap,
                    title = beatmap.displayTitle(settings.preferUnicode),
                    artist = artistName,
                    mapper = beatmap.mapper,
                    queueLabel = queueLabel,
                    isPlaying = state.isPlaying,
                    isFavorite = isFavorite,
                    shuffle = state.shuffle,
                    repeat = state.repeat,
                    position = position,
                    duration = duration,
                    fileInfo = if (settings.showFileInfo) fileInfo else null,
                    sleepActive = timer != null,
                    animated = !performanceMode,
                    compact = maxHeight < 720.dp,
                    artShape = playerArtShape,
                    useOriginalArtwork = effectiveShape == ArtworkShape.SQUARE,
                    coverScale = coverScale,
                    artworkSwipe = settings.artworkSwipe,
                    showToggleRow = settings.appearance.showToggleRow,
                    showVolume = settings.appearance.showVolumeBar && design.supportsVolumeBar,
                    onClose = onClose,
                    onQueue = { showQueue = true },
                    onSleep = { showSleep = true },
                    onMore = onMore,
                    onPlayPause = onPlayPause,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onSeek = onSeek,
                    onShuffle = onShuffle,
                    onRepeat = onRepeat,
                    onFavorite = onFavorite,
                    onShare = {
                        val send =
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "${beatmap.displayTitle(settings.preferUnicode)} - $artistName\n${beatmap.webUrl}",
                                )
                            }
                        runCatching { context.startActivity(Intent.createChooser(send, null)) }
                    },
                    onArtist = { onArtistClick(artistName) },
                    onMapper = { onMapperClick(beatmap.mapper) },
                )
            PlayerDesignContent(design, ui)
        }
    }

    if (showQueue) {
        QueueSheet(
            queue = state.queue,
            currentIndex = state.queueIndex,
            isPlaying = state.isPlaying,
            onDismiss = { showQueue = false },
            onSkipTo = onSkipToQueueItem,
            onRemove = onRemoveFromQueue,
            shuffle = state.shuffle,
            repeat = state.repeat,
            autoplay = settings.autoplay,
            isFavorite = isFavorite,
            onShuffle = onShuffle,
            onRepeat = onRepeat,
            onToggleAutoplay = onToggleAutoplay,
            onFavorite = onFavorite,
            onClear = onClearQueue,
            onMove = onMoveQueueItem,
            onSaveAsPlaylist = onSaveAsPlaylist,
        )
    }

    if (showSleep) {
        SleepTimerSheet(
            timer = timer,
            onDismiss = { showSleep = false },
            onSetMinutes = onSetSleepTimer,
            onEndOfTrack = onSleepAtEndOfTrack,
            onCancel = onCancelSleepTimer,
        )
    }
}

private val NoSleepTimer = MutableStateFlow<SleepTimer?>(null)
private val NoAudioInfo = MutableStateFlow<String?>(null)
