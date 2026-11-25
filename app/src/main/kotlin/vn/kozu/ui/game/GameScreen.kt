package vn.kozu.ui.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.kozu.R
import vn.kozu.game.mania.gamestage.ui.GameStageHost
import vn.kozu.game.ui.game.GameViewModel
import vn.kozu.ui.components.HeaderTitle
import vn.kozu.ui.components.ScreenHeader
import vn.kozu.ui.components.SettingsAction
import vn.kozu.ui.components.NewsAction

@Composable
fun GameScreen(
    isSelected: Boolean,
    bottomPadding: Dp,
    onOpenSettings: () -> Unit,
    onOpenNews: () -> Unit,
    onGameplayChanged: (Boolean) -> Unit,
    onOpenAllBeatmapsFromPreview: (GameViewModel) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = viewModel(factory = GameViewModel.Factory),
) {
    var gameplayActive by remember { mutableStateOf(false) }
    val playerName by viewModel.playerName.collectAsStateWithLifecycle()
    val osuUsername by viewModel.osuUsername.collectAsStateWithLifecycle()
    val requestedMapId by viewModel.requestedMapId.collectAsStateWithLifecycle()
    val customOrderIds by viewModel.customOrderIds.collectAsStateWithLifecycle()
    Column(modifier.fillMaxSize()) {
        if (!gameplayActive) {
            ScreenHeader(
                leading = { HeaderTitle(stringResource(R.string.game)) },
                actions = {
                    NewsAction(onOpenNews)
                    SettingsAction(onOpenSettings)
                },
            )
        }

        GameStageHost(
            preview = viewModel.previewModule,
            game = viewModel.gameModule,
            isActive = isSelected,
            playerName = playerName,
            osuUsername = osuUsername,
            loadRankedAt = viewModel::rankedAt,
            requestedMapId = requestedMapId,
            onMapOpenHandled = viewModel::consumeOpenMapRequest,
            onAddMaps = { onOpenAllBeatmapsFromPreview(viewModel) },
            customOrderIds = customOrderIds,
            onReorderCharts = viewModel::saveCustomOrder,
            onGameplayChanged = {
                gameplayActive = it
                onGameplayChanged(it)
            },
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = if (gameplayActive) 0.dp else bottomPadding
            )
        )
    }
}
