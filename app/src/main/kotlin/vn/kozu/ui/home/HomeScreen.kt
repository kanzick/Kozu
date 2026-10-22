package vn.kozu.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.kozu.domain.model.Beatmap
import vn.kozu.ui.components.BeatmapCover
import vn.kozu.ui.components.BeatmapRow
import vn.kozu.ui.components.SectionHeading
import vn.kozu.ui.demoBeatmaps

@Composable
internal fun HomeScreen(
    favorites: Set<Long>,
    recentlyPicked: List<Beatmap>,
    onExplore: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onBeatmapSelected: (Beatmap) -> Unit,
    onFavorite: (Beatmap) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        HomeHeader(onSearch = onSearch, onSettings = onSettings)

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(22.dp))
            Text(
                text = "A little rhythm,\na lot of feeling.",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp, lineHeight = 40.sp),
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text = "Soundtracks waiting between the circles.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(
                visible = recentlyPicked.isNotEmpty(),
                enter = fadeIn() + slideInVertically { it / 8 }
            ) {
                Column {
                    Spacer(Modifier.height(28.dp))
                    SectionHeading(
                        title = "Recently picked",
                        action = "Explore",
                        onAction = onExplore
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        recentlyPicked.forEach { beatmap ->
                            RecentPickCard(beatmap) { onBeatmapSelected(beatmap) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            SectionHeading(
                title = "Quick picks",
                action = "See all",
                onAction = onExplore
            )
            Spacer(Modifier.height(13.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                demoBeatmaps.forEachIndexed { index, beatmap ->
                    QuickPickCard(
                        beatmap = beatmap,
                        index = index,
                        onClick = { onBeatmapSelected(beatmap) }
                    )
                }
            }

            Spacer(Modifier.height(26.dp))
            YourMixCard(onExplore)

            Spacer(Modifier.height(28.dp))
            SectionHeading(title = "Made for you", action = "Browse", onAction = onExplore)
            Spacer(Modifier.height(12.dp))
            demoBeatmaps.forEach { beatmap ->
                BeatmapRow(
                    beatmap = beatmap,
                    isFavorite = beatmap.id in favorites,
                    onClick = { onBeatmapSelected(beatmap) },
                    onFavorite = { onFavorite(beatmap) }
                )
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun HomeHeader(
    onSearch: () -> Unit,
    onSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 14.dp, top = 8.dp, bottom = 17.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(15.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(27.dp)
                        )
                    }
                }
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "kozu",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.7).sp
                    )
                    Text(
                        "MUSIC, MAPPED DIFFERENTLY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                        letterSpacing = 0.7.sp
                    )
                }
                HeaderAction(Icons.Filled.Search, "Search", onSearch)
                HeaderAction(Icons.Filled.Settings, "Settings", onSettings)
            }
        }
    }
}

@Composable
private fun HeaderAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(44.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun QuickPickCard(
    beatmap: Beatmap,
    index: Int,
    onClick: () -> Unit
) {
    val cardColors = listOf(
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.primaryContainer
    )
    Card(
        modifier = Modifier
            .width(188.dp)
            .clickable(onClick = onClick)
            .animateContentSize(),
        shape = RoundedCornerShape(27.dp),
        colors = CardDefaults.cardColors(containerColor = cardColors[index % cardColors.size])
    ) {
        Column(modifier = Modifier.padding(9.dp)) {
            Box {
                BeatmapCover(
                    beatmap = beatmap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(172.dp)
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                ) {
                    Text(
                        "BEATMAP",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
            Spacer(Modifier.height(11.dp))
            Text(
                beatmap.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                beatmap.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentPickCard(beatmap: Beatmap, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(228.dp)
            .height(76.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(25.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BeatmapCover(beatmap, Modifier.size(52.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    beatmap.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    beatmap.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Open ${beatmap.title}",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(25.dp)
            )
        }
    }
}

@Composable
private fun YourMixCard(onExplore: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)
                        )
                    )
                )
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "YOUR MIX  ·  #01",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "A new mood\nis one tap away.",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "A hand-picked path through beatmaps.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledIconButton(
                onClick = onExplore,
                modifier = Modifier.size(66.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Filled.Shuffle, contentDescription = "Explore your mix")
            }
        }
    }
}
