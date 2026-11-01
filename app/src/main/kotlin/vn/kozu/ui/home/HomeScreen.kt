package vn.kozu.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.ListeningStats
import vn.kozu.domain.model.OsuProfile
import vn.kozu.ui.components.BeatmapArt
import vn.kozu.ui.components.CoverCard
import vn.kozu.ui.components.KozuLoading
import vn.kozu.ui.components.RefreshIndicator
import vn.kozu.ui.components.LocalTrackActions
import vn.kozu.ui.components.ScreenHeader
import vn.kozu.ui.components.SearchAction
import vn.kozu.ui.components.SectionHeader
import vn.kozu.ui.components.SettingsAction
import vn.kozu.ui.components.TrackPill
import vn.kozu.ui.components.TrackRow
import vn.kozu.ui.components.bounceClick
import vn.kozu.ui.components.segmentedShape
import vn.kozu.ui.theme.KozuCookies
import vn.kozu.ui.theme.KozuMotion
import vn.kozu.util.formatDuration
import java.util.Calendar
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    listState: LazyListState,
    bottomPadding: Dp,
    onOpenSettings: () -> Unit,
    onOpenExplore: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSearch: () -> Unit,
    onRefresh: () -> Unit,
    isAppRefreshing: Boolean,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val catalogError = ui.error
    val actions = LocalTrackActions.current
    val pullState = rememberPullToRefreshState()
    val haptics = LocalHapticFeedback.current
    val refreshNow = {
        if (!ui.isRefreshing && !isAppRefreshing) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onRefresh()
        }
    }
    val refreshing = ui.isRefreshing || isAppRefreshing
    val refreshRotation = if (refreshing) {
        val transition = rememberInfiniteTransition(label = "home-refresh")
        val rotation by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "home-refresh-rotation"
        )
        rotation
    } else {
        pullState.distanceFraction.coerceIn(0f, 1f) * 180f
    }

    Column(modifier.fillMaxSize()) {
        ScreenHeader(
            leading = {
                Surface(
                    modifier = Modifier
                        .size(46.dp)
                        .graphicsLayer { rotationZ = refreshRotation },
                    shape = KozuCookies.Soft9,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.MusicNote, contentDescription = null, modifier = Modifier.size(26.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Kozu",
                    style = MaterialTheme.typography.headlineLarge
                )
            },
            actions = {
                SearchAction(onOpenSearch)
                SettingsAction(onOpenSettings)
            }
        )

        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = refreshNow,
            state = pullState,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                RefreshIndicator(
                    state = pullState,
                    isRefreshing = refreshing,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 24.dp, bottom = bottomPadding)
            ) {
                item(key = "greeting") { Greeting(name = ui.displayName) }

                ui.profile?.let { profile ->
                    item(key = "osu-profile") {
                        Column(Modifier.padding(top = 20.dp)) {
                            OsuPulseCard(profile = profile)
                        }
                    }
                }

                if (catalogError != null) {
                    item(key = "catalog-error") {
                        CatalogErrorBanner(
                            error = catalogError,
                            onRetry = viewModel::refresh,
                            onSettings = onOpenSettings
                        )
                    }
                }

                item(key = "quick-header") {
                    Column {
                        Spacer(Modifier.height(28.dp))
                        SectionHeader(title = "Quick Picks", onAction = onOpenExplore)
                        Spacer(Modifier.height(14.dp))
                    }
                }
                item(key = "quick-row") {
                    if (ui.quickPicks.isEmpty()) {
                        if (ui.isLoading) {
                            KozuLoading()
                        } else {
                            Text(
                                "No live beatmaps are available right now.",
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(ui.quickPicks, key = { _, b -> b.id }) { index, beatmap ->
                                CoverCard(
                                    beatmap = beatmap,
                                    size = 176.dp,
                                    shape = RoundedCornerShape(38.dp),
                                    rank = index + 1,
                                    onClick = { actions.play(ui.quickPicks, index) }
                                )
                            }
                        }
                    }
                }

                item(key = "mix") {
                    Column {
                        Spacer(Modifier.height(32.dp))
                        YourMixCard(
                            mix = ui.mix,
                            onShuffle = {
                                actions.playShuffled(ui.mix)
                                viewModel.loadMoreMix()
                            }
                        )
                    }
                }

                if (ui.recent.isNotEmpty()) {
                    item(key = "recent") {
                        Column {
                            Spacer(Modifier.height(32.dp))
                            SectionHeader(title = "Recently Played")
                            Spacer(Modifier.height(14.dp))
                            RecentlyPlayedGrid(recent = ui.recent) { index -> actions.play(ui.recent, index) }
                        }
                    }
                }

                item(key = "stats") {
                    Column {
                        Spacer(Modifier.height(32.dp))
                        ListeningStatsCard(
                            stats = ui.stats,
                            onOpenHistory = onOpenLibrary,
                            onPlayTop = { top -> actions.play(listOf(top), 0) }
                        )
                    }
                }

                if (ui.madeForYou.isNotEmpty()) {
                    item(key = "made-header") {
                        Column {
                            Spacer(Modifier.height(32.dp))
                            SectionHeader(
                                title = "osu! Updates",
                                subtitle = "Recently updated beatmaps",
                                onAction = onOpenExplore
                            )
                            Spacer(Modifier.height(14.dp))
                        }
                    }
                    itemsIndexed(ui.madeForYou, key = { _, b -> "made-${b.id}" }) { index, beatmap ->
                        TrackRow(
                            beatmap = beatmap,
                            shape = segmentedShape(index, ui.madeForYou.size),
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .padding(vertical = 1.5.dp),
                            onClick = { actions.play(ui.madeForYou, index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OsuPulseCard(profile: OsuProfile) {
    val ranks = profile.rankHistory.takeLast(90).filter { it > 0 }
    val rankChange = ranks.firstOrNull()?.minus(ranks.lastOrNull() ?: 0) ?: 0
    val numberFormat = NumberFormat.getIntegerInstance()
    val changeLabel = when {
        rankChange > 0 -> "↑ ${numberFormat.format(rankChange)} places"
        rankChange < 0 -> "↓ ${numberFormat.format(-rankChange)} places"
        else -> "Rank history"
    }
    Surface(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = profile.username,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${profile.flag} ${profile.countryName} · ${profile.mode.uppercase()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RankMetric(
                    title = "Global rank",
                    value = if (profile.globalRank > 0) "#${numberFormat.format(profile.globalRank)}" else "Unranked",
                    modifier = Modifier.weight(1f)
                )
                RankMetric(
                    title = "Country rank",
                    value = if (profile.countryRank > 0) "#${numberFormat.format(profile.countryRank)}" else "—",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(6.dp))
            OsuRankHistoryChart(profile.rankHistory)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "90 days",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        changeLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "Today",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OsuStat("pp", numberFormat.format(profile.pp.toLong()), Modifier.weight(1f))
                OsuStat("Plays", compactOsuCount(profile.playCount), Modifier.weight(1f))
                OsuStat("Play time", formatOsuPlayTime(profile.playTimeSeconds), Modifier.weight(1.2f))
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                val grades = listOf(
                    "SS" to (profile.gradeSS - profile.gradeSSH).coerceAtLeast(0),
                    "SSH" to profile.gradeSSH,
                    "S" to (profile.gradeS - profile.gradeSH).coerceAtLeast(0),
                    "SH" to profile.gradeSH,
                    "A" to profile.gradeA
                )
                grades.forEachIndexed { index, (grade, count) ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = when (index) {
                            0 -> MaterialTheme.colorScheme.primaryContainer
                            1 -> MaterialTheme.colorScheme.secondaryContainer
                            2 -> MaterialTheme.colorScheme.tertiaryContainer
                            3 -> MaterialTheme.colorScheme.surfaceContainerHighest
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 5.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(grade, style = MaterialTheme.typography.labelSmall)
                            Text(compactOsuCount(count), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RankMetric(title: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun OsuStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun compactOsuCount(value: Int): String = when {
    value >= 1_000_000 -> "%.1fM".format(value / 1_000_000.0)
    value >= 10_000 -> "%.1fK".format(value / 1_000.0)
    else -> NumberFormat.getIntegerInstance().format(value)
}

private fun formatOsuPlayTime(seconds: Long): String {
    val hours = seconds.coerceAtLeast(0) / 3_600
    val days = hours / 24
    return if (days > 0) "${days}d ${hours % 24}h" else "${hours}h"
}

@Composable
private fun OsuRankHistoryChart(values: List<Int>) {
    val color = MaterialTheme.colorScheme.primary
    val ranks = values.takeLast(90).filter { it > 0 }
    if (ranks.isEmpty()) {
        Text(
            "Rank history is not available yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 10.dp)
        )
        return
    }
    val minRank = ranks.minOrNull()?.toFloat() ?: return
    val maxRank = ranks.maxOrNull()?.toFloat() ?: return
    val padding = ((maxRank - minRank) * 0.08f).coerceAtLeast(1f)
    val chartMin = (minRank - padding).coerceAtLeast(1f)
    val chartMax = maxRank + padding
    val chartRange = (chartMax - chartMin).coerceAtLeast(1f)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {
        val points = ranks.mapIndexed { index, rank ->
            val x = if (ranks.size == 1) size.width else index.toFloat() / ranks.lastIndex * size.width
            val y = ((rank - chartMin) / chartRange * size.height).coerceIn(0f, size.height)
            androidx.compose.ui.geometry.Offset(x, y)
        }
        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val areaPath = Path().apply {
            moveTo(points.first().x, size.height)
            lineTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, size.height)
            close()
        }
        drawPath(
            areaPath,
            brush = Brush.verticalGradient(
                listOf(color.copy(alpha = 0.24f), color.copy(alpha = 0.01f))
            )
        )
        drawPath(
            linePath,
            color,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        val lastPoint = points.last()
        drawCircle(color.copy(alpha = 0.22f), radius = 8.dp.toPx(), center = lastPoint)
        drawCircle(color, radius = 4.dp.toPx(), center = lastPoint)
    }
}

@Composable
private fun Greeting(name: String) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val line = when (hour) {
        in 5..11 -> "Good morning,"
        in 12..17 -> "Good afternoon,"
        in 18..22 -> "Good evening,"
        else -> "Still up?"
    }
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = line,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${name.ifBlank { "osu! Player" }} 👋",
            style = MaterialTheme.typography.displaySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CatalogErrorBanner(error: String, onRetry: () -> Unit, onSettings: () -> Unit) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(Modifier.padding(start = 18.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Rounded.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = error,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onRetry) { Text("Retry") }
                TextButton(onClick = onSettings) { Text("Settings") }
            }
        }
    }
}

@Composable
private fun YourMixCard(mix: List<Beatmap>, onShuffle: () -> Unit) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(44.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Column(Modifier.padding(start = 26.dp, end = 22.dp, top = 24.dp, bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    val big = MaterialTheme.typography.displayLarge.copy(lineHeight = 56.sp)
                    Text("Your", style = big)
                    Text("Mix", style = big)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Today's mix for you",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.75f)
                    )
                }
                Surface(
                    modifier = Modifier
                        .size(96.dp)
                        .bounceClick(pressedScale = 0.88f, onClick = onShuffle),
                    shape = KozuCookies.Soft9,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shadowElevation = 6.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Shuffle,
                            contentDescription = "Play your mix shuffled",
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
            }
            if (mix.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(width = 164.dp, height = 84.dp)) {
                        mix.take(3).forEachIndexed { index, beatmap ->
                            BeatmapArt(
                                beatmap = beatmap,
                                modifier = Modifier
                                    .offset(x = 40.dp * index)
                                    .size(84.dp),
                                shape = when (index) {
                                    0 -> KozuCookies.Flower8
                                    1 -> CircleShape
                                    else -> KozuCookies.Bold6
                                }
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "${mix.size} songs · refreshed daily",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentlyPlayedGrid(recent: List<Beatmap>, onPlay: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val palette = listOf(
        colors.primaryContainer to colors.onPrimaryContainer,
        colors.secondaryContainer to colors.onSecondaryContainer,
        colors.tertiaryContainer to colors.onTertiaryContainer,
        colors.surfaceContainerHighest to colors.onSurface
    )
    LazyHorizontalGrid(
        rows = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp * 3 + 10.dp * 2),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(recent.size) { index ->
            val (container, content) = palette[index % palette.size]
            TrackPill(
                beatmap = recent[index],
                containerColor = container,
                contentColor = content,
                onClick = { onPlay(index) }
            )
        }
    }
}

@Composable
private fun ListeningStatsCard(
    stats: ListeningStats,
    onOpenHistory: () -> Unit,
    onPlayTop: (Beatmap) -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Listening stats", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Week to date",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .bounceClick(onClick = onOpenHistory),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Open history")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = formatDuration(stats.totalMs),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(16.dp))
            Row {
                StatBlock("Total plays", stats.plays.toString(), Modifier.weight(1f))
                StatBlock("Avg per day", formatDuration(stats.avgPerDayMs), Modifier.weight(1f))
            }

            val top = stats.topTrack
            if (top != null) {
                Spacer(Modifier.height(16.dp))
                Column(Modifier.bounceClick(pressedScale = 0.98f) { onPlayTop(top) }) {
                    Text(
                        "Top track",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        top.title,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${top.artist} • ${stats.topTrackPlays} ${if (stats.topTrackPlays == 1) "play" else "plays"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Play a few songs and your week will show up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(20.dp))
            WeekBars(stats)
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun WeekBars(stats: ListeningStats) {
    val max = (stats.dayMs.maxOrNull() ?: 0L).coerceAtLeast(1L)
    val letters = listOf("M", "T", "W", "T", "F", "S", "S")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        stats.dayMs.forEachIndexed { index, ms ->
            val fraction by animateFloatAsState(
                targetValue = if (ms <= 0L) 0.1f else (ms.toFloat() / max).coerceIn(0.14f, 1f),
                animationSpec = KozuMotion.spatial(),
                label = "bar$index"
            )
            val color = when {
                index == stats.todayIndex -> MaterialTheme.colorScheme.primary
                index < stats.todayIndex -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction.coerceIn(0f, 1f))
                            .background(color, CircleShape)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = letters[index],
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
