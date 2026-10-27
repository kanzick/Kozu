package vn.kozu.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.kozu.data.local.DownloadedSet
import vn.kozu.game.OsuDifficulty
import vn.kozu.ui.components.BeatmapArt
import vn.kozu.ui.components.EmptyState
import vn.kozu.ui.components.HeaderTitle
import vn.kozu.ui.components.ScreenHeader
import vn.kozu.ui.components.SearchAction
import vn.kozu.ui.components.SectionHeader
import vn.kozu.ui.components.SettingsAction
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.theme.KozuCookies

@Composable
fun GameScreen(
    bottomPadding: Dp,
    onOpenSettings: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = viewModel(factory = GameViewModel.Factory)
) {
    val sets by viewModel.sets.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf<Long?>(null) }

    Column(modifier.fillMaxSize()) {
        ScreenHeader(
            leading = { HeaderTitle("Game") },
            actions = {
                SearchAction(onOpenSearch)
                SettingsAction(onOpenSettings)
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 24.dp, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "hero") { HeroCard() }

            item(key = "header") {
                Column {
                    Spacer(Modifier.height(14.dp))
                    SectionHeader(
                        title = "Ready to play",
                        subtitle = "Downloaded songs with their beatmap files"
                    )
                }
            }

            if (sets.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Rounded.Download,
                        title = "No beatmaps yet",
                        message = "Download a song for offline and its charts will show up here.",
                        shape = KozuCookies.Flower8,
                        action = {
                            Button(onClick = onOpenLibrary) { Text("Open library") }
                        }
                    )
                }
            } else {
                items(sets, key = { it.beatmap.id }) { set ->
                    GameSetCard(
                        set = set,
                        expanded = expanded == set.beatmap.id,
                        onToggle = { expanded = if (expanded == set.beatmap.id) null else set.beatmap.id },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCard() {
    val colors = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "approach")
    val ring by transition.animateFloat(
        initialValue = 2.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "ring"
    )

    Surface(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(44.dp),
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer
    ) {
        Row(
            modifier = Modifier.padding(start = 26.dp, end = 12.dp, top = 22.dp, bottom = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Surface(shape = CircleShape, color = colors.primary, contentColor = colors.onPrimary) {
                    Text(
                        "COMING SOON",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text("Tap to the beat", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Turn any downloaded song into a rhythm game. Charts are already prepared on your device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(132.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val base = size.minDimension / 2f / 2.4f
                    drawCircle(colors.primary, radius = base, center = center)
                    drawCircle(colors.onPrimary, radius = base, center = center, style = Stroke(3.dp.toPx()))
                    val fade = (1f - (ring - 1f) / 1.4f).coerceIn(0f, 1f)
                    drawCircle(
                        colors.primary.copy(alpha = fade),
                        radius = base * ring,
                        center = center,
                        style = Stroke(3.dp.toPx())
                    )
                }
                Text("1", style = MaterialTheme.typography.headlineMedium, color = colors.onPrimary)
            }
        }
    }
}

@Composable
private fun GameSetCard(
    set: DownloadedSet,
    expanded: Boolean,
    onToggle: () -> Unit,
    viewModel: GameViewModel
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(pressedScale = 0.98f, onClick = onToggle)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BeatmapArt(set.beatmap, Modifier.size(60.dp), shape = RoundedCornerShape(20.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        set.beatmap.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        set.beatmap.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.SportsEsports,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            AnimatedVisibility(visible = expanded) {
                val difficulties by produceState(initialValue = emptyList<OsuDifficulty>(), set.beatmap.id) {
                    value = viewModel.loadDifficulties(set.beatmap.id)
                }
                Column(
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (difficulties.isEmpty()) {
                        Text(
                            "No chart files were kept for this song. Enable “Keep full beatmap” in Offline settings and download again.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                    difficulties.forEach { DifficultyRow(it) }
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Start · coming soon") }
                }
            }
        }
    }
}

@Composable
private fun DifficultyRow(difficulty: OsuDifficulty) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    difficulty.version,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "CS ${fmt(difficulty.circleSize)} · AR ${fmt(difficulty.approachRate)} · OD ${fmt(difficulty.overallDifficulty)} · HP ${fmt(difficulty.hpDrain)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(difficulty.modeName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    "${difficulty.objectCount} objects",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun fmt(value: Float): String =
    if (value == value.toInt().toFloat()) value.toInt().toString() else "%.1f".format(value)
