package vn.kozu.ui.game

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.kozu.game.ui.game.AddMapScreen
import vn.kozu.game.ui.game.GameViewModel
import vn.kozu.game.mania.gamestage.ui.GameStageHost
import vn.kozu.ui.components.HeaderTitle
import vn.kozu.ui.components.ScreenHeader
import vn.kozu.ui.components.SettingsAction

@Composable
fun GameScreen(
    bottomPadding: Dp,
    onOpenSettings: () -> Unit,
    onGameplayChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = viewModel(factory = GameViewModel.Factory)
) {
    var addMapsOpen by remember { mutableStateOf(false) }
    var gameplayActive by remember { mutableStateOf(false) }
    BackHandler(enabled = addMapsOpen && !gameplayActive) { addMapsOpen = false }

    Column(modifier.fillMaxSize()) {
        if (!gameplayActive && !addMapsOpen) {
            ScreenHeader(
                leading = { HeaderTitle("Game") },
                actions = {
                    if (!addMapsOpen) {
                        IconButton(onClick = { addMapsOpen = true }) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = "Add maps",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    SettingsAction(onOpenSettings)
                }
            )
        }

        AnimatedContent(
            targetState = addMapsOpen,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
            label = "game-destination"
        ) { showingAddMaps ->
            if (showingAddMaps) {
                AddMapScreen(
                    viewModel = viewModel,
                    bottomPadding = if (gameplayActive) 0.dp else bottomPadding,
                    onBack = { addMapsOpen = false },
                    onOpenSettings = onOpenSettings
                )
            } else {
                GameStageHost(
                    preview = viewModel.previewModule,
                    game = viewModel.gameModule,
                    onAddMaps = { addMapsOpen = true },
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
    }
}
