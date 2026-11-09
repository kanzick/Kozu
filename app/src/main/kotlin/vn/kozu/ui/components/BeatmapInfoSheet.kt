package vn.kozu.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Piano
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import vn.kozu.ui.i18n.KozuText as Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.CancellationException
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.BeatmapDifficultyInfo
import vn.kozu.domain.model.BeatmapSetInfo
import vn.kozu.domain.model.BrowseGenre
import vn.kozu.domain.model.BrowseLanguage
import vn.kozu.domain.model.GameModes
import vn.kozu.util.formatTime
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BeatmapInfoSheet(
    beatmap: Beatmap,
    loadInfo: suspend (Long) -> BeatmapSetInfo?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var info by remember(beatmap.id) { mutableStateOf<BeatmapSetInfo?>(null) }
    var failed by remember(beatmap.id) { mutableStateOf(false) }
    var attempt by remember(beatmap.id) { mutableIntStateOf(0) }

    LaunchedEffect(beatmap.id, attempt) {
        failed = false
        val loaded = try {
            loadInfo(beatmap.id)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        if (loaded == null) failed = true else info = loaded
    }

    val shown = info?.beatmap ?: beatmap
    val mapper = shown.mapper.ifBlank { beatmap.mapper }
    val difficulties = info?.difficulties.orEmpty()
    val setBpm = shown.bpm.takeIf { it > 0.0 } ?: difficulties.firstOrNull { it.bpm > 0.0 }?.bpm

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { KozuSheetHandle() },
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
        ),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            BeatmapArt(
                beatmap = shown,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                shape = RoundedCornerShape(28.dp),
                large = true
            )

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(shown.title, style = MaterialTheme.typography.headlineSmall)
                if (shown.titleUnicode.isNotBlank() && shown.titleUnicode != shown.title) {
                    Text(
                        shown.titleUnicode,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(shown.artist, style = MaterialTheme.typography.titleMedium)
                if (shown.artistUnicode.isNotBlank() && shown.artistUnicode != shown.artist) {
                    Text(
                        shown.artistUnicode,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (mapper.isNotBlank()) {
                    Text(
                        "Mapped by $mapper",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (shown.status.isNotBlank()) {
                    InfoPill(
                        shown.status.replaceFirstChar { it.uppercase() },
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                if (shown.hasVideo) {
                    InfoPill(
                        "Video",
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                if (shown.hasStoryboard) {
                    InfoPill(
                        "Storyboard",
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            FlowRow(
                maxItemsInEachRow = 2,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tileModifier = Modifier.weight(1f)
                StatTile("Plays", shown.playCount.takeIf { it > 0 }?.let(::formatCount) ?: "—", tileModifier)
                StatTile("Favourites", shown.favouriteCount.takeIf { it > 0 }?.toLong()?.let(::formatCount) ?: "—", tileModifier)
                StatTile("BPM", setBpm?.let(::compact) ?: "—", tileModifier)
                StatTile(
                    "Rating",
                    info?.takeIf { it.ratingCount > 0 }?.let {
                        String.format(Locale.US, "%.1f / 10 · %s votes", it.ratingAverage, formatCount(it.ratingCount.toLong()))
                    } ?: "—",
                    tileModifier
                )
            }

            val rows = buildList {
                if (shown.rankedAt > 0L) add("Ranked" to formatDate(shown.rankedAt))
                if (shown.submittedAt > 0L) add("Submitted" to formatDate(shown.submittedAt))
                if (shown.updatedAt > 0L) add("Last updated" to formatDate(shown.updatedAt))
                BrowseGenre.labelFor(shown.genreId)?.let { add("Genre" to it) }
                BrowseLanguage.labelFor(shown.languageId)?.let { add("Language" to it) }
                if (shown.source.isNotBlank()) add("Source" to shown.source)
            }
            if (rows.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rows.forEach { (label, value) ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    label,
                                    modifier = Modifier.width(104.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    value,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
            }

            val tags = shown.tags.split(' ').filter { it.isNotBlank() }.distinct().take(MAX_TAGS)
            if (tags.isNotEmpty()) {
                Text("Tags", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tags.forEach { tag ->
                        InfoPill(
                            tag,
                            MaterialTheme.colorScheme.surfaceContainerHighest,
                            MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = if (difficulties.isEmpty()) "Difficulties" else "Difficulties (${difficulties.size})",
                style = MaterialTheme.typography.titleMedium
            )
            when {
                difficulties.isNotEmpty() -> {
                    difficulties.groupBy { it.modeInt }.toSortedMap().forEach { (mode, list) ->
                        ModeHeader(mode, list.size)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            list.forEachIndexed { index, difficulty ->
                                DifficultyRow(difficulty, segmentedShape(index, list.size, large = 20.dp))
                            }
                        }
                    }
                }

                info != null -> Text(
                    "No difficulty details are available for this beatmap.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                failed -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Could not load beatmap details.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { attempt++ }) { Text("Retry") }
                }

                else -> KozuLoading()
            }
            if (info?.isReduced == true) {
                Text(
                    "Limited details from the public mirror. Add osu! API credentials for the full listing.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, shown.webUrl.toUri())) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open on osu!")
            }
        }
    }
}

@Composable
private fun InfoPill(text: String, container: Color, content: Color) {
    Surface(shape = RoundedCornerShape(50), color = container, contentColor = content) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ModeHeader(mode: Int, count: Int) {
    Row(
        modifier = Modifier.padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = modeIcon(mode),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(GameModes.label(mode), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.width(8.dp))
        Text(
            "$count",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DifficultyRow(difficulty: BeatmapDifficultyInfo, shape: Shape) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StarChip(difficulty.stars)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(difficulty.version, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = timingLine(difficulty),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = statLine(difficulty),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                countLine(difficulty)?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StarChip(stars: Double) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when {
        stars < 2.0 -> scheme.secondaryContainer to scheme.onSecondaryContainer
        stars < 4.0 -> scheme.primaryContainer to scheme.onPrimaryContainer
        stars < 6.0 -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        else -> scheme.errorContainer to scheme.onErrorContainer
    }
    Surface(shape = RoundedCornerShape(50), color = container, contentColor = content) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                String.format(Locale.US, "%.2f", stars),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

private fun modeIcon(mode: Int): ImageVector = when (mode) {
    0 -> Icons.Rounded.RadioButtonChecked
    1 -> Icons.Rounded.Album
    2 -> Icons.Rounded.Eco
    else -> Icons.Rounded.Piano
}

private fun timingLine(d: BeatmapDifficultyInfo): String = buildList {
    if (d.lengthSeconds > 0) add(formatTime(d.lengthSeconds * 1000L))
    if (d.bpm > 0.0) add("${compact(d.bpm)} BPM")
    if (d.maxCombo > 0) add("${d.maxCombo}x combo")
    if (d.isConvert) add("Convert")
}.joinToString(" · ").ifBlank { "—" }

private fun statLine(d: BeatmapDifficultyInfo): String = when (d.modeInt) {
    0, 2 -> "CS ${compact(d.circleSize)} · AR ${compact(d.approachRate)} · OD ${compact(d.overallDifficulty)} · HP ${compact(d.hpDrain)}"
    1 -> "OD ${compact(d.overallDifficulty)} · HP ${compact(d.hpDrain)}"
    else -> "${compact(d.circleSize)}K · OD ${compact(d.overallDifficulty)} · HP ${compact(d.hpDrain)}"
}

private fun countLine(d: BeatmapDifficultyInfo): String? {
    if (d.circles + d.sliders + d.spinners <= 0) return null
    val labels = when (d.modeInt) {
        0 -> Triple("circles", "sliders", "spinners")
        1 -> Triple("hits", "drumrolls", "shakers")
        2 -> Triple("fruits", "juice streams", "bananas")
        else -> Triple("notes", "holds", "")
    }
    return buildList {
        add("${d.circles} ${labels.first}")
        add("${d.sliders} ${labels.second}")
        if (d.modeInt != 3) add("${d.spinners} ${labels.third}")
    }.joinToString(" · ")
}

private fun compact(value: Double): String {
    val text = String.format(Locale.US, "%.1f", value)
    return text.removeSuffix(".0")
}

private fun formatCount(value: Long): String = NumberFormat.getIntegerInstance(Locale.US).format(value)

private fun formatDate(millis: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US).format(Date(millis))

private const val MAX_TAGS = 40
