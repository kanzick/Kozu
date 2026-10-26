package vn.kozu.ui.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import vn.kozu.ui.components.LocalTrackActions
import vn.kozu.ui.components.TrackRow
import vn.kozu.ui.components.segmentedShape
import vn.kozu.ui.theme.KozuMotion
import vn.kozu.util.formatDuration

@Composable
fun StatsPage(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val range by viewModel.range.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val actions = LocalTrackActions.current

    SettingsScaffold(title = "Listening Stats", onBack = onBack) {
        SettingsGroup(
            title = null,
            items = listOf({
                ChoiceRow(
                    title = "Period",
                    options = StatsRange.entries.toList(),
                    selected = range,
                    label = { it.label },
                    onSelect = viewModel::setRange
                )
            })
        )
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Listening time", formatDuration(stats.totalMs), Modifier.weight(1.4f), highlight = true)
            StatTile("Plays", formatInt(stats.plays), Modifier.weight(1f))
            StatTile("Active days", stats.activeDays.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))

        if (stats.dayMs.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Daily listening", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(14.dp))
                    DayBars(stats.dayMs)
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        if (stats.topTracks.isEmpty()) {
            SettingsGroup(
                title = null,
                items = listOf({
                    Text(
                        "Nothing to show yet. Listen to a few songs and your top tracks, artists and mappers will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                })
            )
            return@SettingsScaffold
        }

        Text(
            "Top songs",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 10.dp)
        )
        val queue = stats.topTracks.map { it.first }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            stats.topTracks.forEachIndexed { index, (beatmap, count) ->
                TrackRow(
                    beatmap = beatmap,
                    shape = segmentedShape(index, stats.topTracks.size),
                    onClick = { actions.play(queue, index) },
                    trailing = {
                        Text(
                            "$count×",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        RankedList("Top artists", stats.topArtists)
        Spacer(Modifier.height(22.dp))
        RankedList("Top mappers", stats.topMappers)
    }
}

@Composable
private fun DayBars(values: List<Long>) {
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        horizontalArrangement = Arrangement.spacedBy(if (values.size > 14) 3.dp else 6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEachIndexed { index, ms ->
            val fraction by animateFloatAsState(
                targetValue = if (ms <= 0L) 0.06f else (ms.toFloat() / max).coerceIn(0.1f, 1f),
                animationSpec = KozuMotion.spatial(),
                label = "dayBar$index"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(fraction.coerceIn(0f, 1f))
                    .background(
                        if (ms > 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        CircleShape
                    )
            )
        }
    }
}

@Composable
private fun RankedList(title: String, items: List<Pair<String, Int>>) {
    if (items.isEmpty()) return
    val max = items.first().second.coerceAtLeast(1)
    SettingsGroup(
        title = title,
        items = items.map { (name, count) ->
            @Composable {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("$count plays", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { count.toFloat() / max }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    )
}
