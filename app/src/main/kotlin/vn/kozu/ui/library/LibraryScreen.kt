package vn.kozu.ui.library

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.kozu.domain.model.Beatmap
import vn.kozu.ui.components.BeatmapRow

@Composable
internal fun LibraryScreen(
    favorites: List<Beatmap>,
    allBeatmaps: List<Beatmap>,
    onBeatmapSelected: (Beatmap) -> Unit,
    onFavorite: (Beatmap) -> Unit
) {
    var showAllTracks by remember { mutableStateOf(false) }
    val visibleTracks = if (showAllTracks) allBeatmaps else favorites

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            LibraryHeader()
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    LibraryFilterChip(
                        label = "LIKED",
                        selected = !showAllTracks,
                        icon = Icons.Filled.Favorite
                    ) { showAllTracks = false }
                    LibraryFilterChip(
                        label = "ALL SONGS",
                        selected = showAllTracks,
                        icon = Icons.Filled.MusicNote
                    ) { showAllTracks = true }
                }
                Spacer(Modifier.height(15.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (showAllTracks) "All songs" else "Liked songs",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${visibleTracks.size} tracks · osu! collection",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledIconButton(
                        onClick = { visibleTracks.randomOrNull()?.let(onBeatmapSelected) },
                        enabled = visibleTracks.isNotEmpty(),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Filled.Shuffle, contentDescription = "Pick a random track")
                    }
                }
            }
        }
        if (visibleTracks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .animateContentSize(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(68.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Filled.FavoriteBorder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Nothing liked yet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Save songs with the heart button and they will live here.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(items = visibleTracks, key = { it.id }) { beatmap ->
                BeatmapRow(
                    beatmap = beatmap,
                    isFavorite = favorites.any { it.id == beatmap.id },
                    onClick = { onBeatmapSelected(beatmap) },
                    onFavorite = { onFavorite(beatmap) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            }
        }
    }
}

@Composable
private fun LibraryHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp)
    ) {
        Column(
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 22.dp)
        ) {
            Text(
                "YOUR COLLECTION",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Library",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 38.sp),
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "The songs you keep coming back to.",
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
            )
        }
    }
}

@Composable
private fun LibraryFilterChip(
    label: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        border = if (selected) null else BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.size(7.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp
            )
        }
    }
}
