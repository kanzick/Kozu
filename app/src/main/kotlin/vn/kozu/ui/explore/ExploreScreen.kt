package vn.kozu.ui.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.kozu.domain.model.Beatmap
import vn.kozu.ui.components.BeatmapCover
import vn.kozu.ui.components.SectionHeading
import vn.kozu.ui.demoBeatmaps

@Composable
internal fun ExploreScreen(
    recentlyPicked: List<Beatmap>,
    favorites: List<Beatmap>,
    onSearch: () -> Unit,
    onOpenLibrary: () -> Unit,
    onBeatmapSelected: (Beatmap) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 22.dp)
    ) {
        item {
            ExploreHeader(onSearch)
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(21.dp))
                SectionHeading(
                    title = "Your library",
                    action = "See all",
                    onAction = onOpenLibrary
                )
                Spacer(Modifier.height(11.dp))
                if (favorites.isEmpty()) {
                    LibraryPrompt(onSearch)
                } else {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        favorites.take(5).forEach { beatmap ->
                            LibrarySongCard(beatmap) { onBeatmapSelected(beatmap) }
                        }
                    }
                }

                Spacer(Modifier.height(26.dp))
                SectionHeading(
                    title = "Discover beatmaps",
                    action = "Browse",
                    onAction = onSearch
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    demoBeatmaps.forEachIndexed { index, beatmap ->
                        ExploreMapCard(beatmap, index) { onBeatmapSelected(beatmap) }
                    }
                }
            }
        }
        item {
            AnimatedVisibility(
                visible = recentlyPicked.isNotEmpty(),
                enter = fadeIn() + slideInVertically { it / 8 }
            ) {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Spacer(Modifier.height(27.dp))
                    SectionHeading(title = "Pick up where you left off")
                    Spacer(Modifier.height(12.dp))
                    recentlyPicked.take(4).forEach { beatmap ->
                        LibrarySongCard(beatmap) { onBeatmapSelected(beatmap) }
                        Spacer(Modifier.height(9.dp))
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(24.dp))
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(17.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(46.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Made for beatmap lovers", fontWeight = FontWeight.Bold)
                            Text(
                                "Find a song by title or artist.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(onClick = onSearch, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Search, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Search songs and beatmaps")
                }
            }
        }
    }
}

@Composable
private fun ExploreHeader(onSearch: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 18.dp, top = 23.dp, bottom = 23.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "FIND YOUR NEXT LOOP",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    "Explore",
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 38.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Surface(
                modifier = Modifier
                    .size(54.dp)
                    .clickable(onClick = onSearch),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Search, contentDescription = "Search beatmaps")
                }
            }
        }
    }
}

@Composable
private fun ExploreMapCard(
    beatmap: Beatmap,
    index: Int,
    onClick: () -> Unit
) {
    val containers = listOf(
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer
    )
    Card(
        modifier = Modifier
            .width(194.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = containers[index % containers.size])
    ) {
        Column(Modifier.padding(9.dp)) {
            Box {
                BeatmapCover(
                    beatmap,
                    Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(42.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Play ${beatmap.title}",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                beatmap.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${beatmap.artist} · osu! beatmap",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LibrarySongCard(beatmap: Beatmap, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(232.dp)
            .height(78.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(25.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BeatmapCover(beatmap, Modifier.size(56.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    beatmap.title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    beatmap.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LibraryPrompt(onSearch: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSearch),
        shape = RoundedCornerShape(25.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Start your collection", fontWeight = FontWeight.Bold)
                Text(
                    "Search for songs, then save your favorites.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
