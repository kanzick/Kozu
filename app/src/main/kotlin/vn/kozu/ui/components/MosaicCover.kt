package vn.kozu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import vn.kozu.domain.model.Beatmap


@Composable
fun MosaicCover(
    tracks: List<Beatmap>,
    modifier: Modifier = Modifier,
    tileSize: Dp = 54.dp
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(2) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(2) { col ->
                    val beatmap = tracks.getOrNull(row * 2 + col)
                    val shape = RoundedCornerShape(
                        topStart = if (row == 0 && col == 0) 22.dp else 6.dp,
                        topEnd = if (row == 0 && col == 1) 22.dp else 6.dp,
                        bottomStart = if (row == 1 && col == 0) 22.dp else 6.dp,
                        bottomEnd = if (row == 1 && col == 1) 22.dp else 6.dp
                    )
                    if (beatmap != null) {
                        BeatmapArt(beatmap, Modifier.size(tileSize), shape = shape)
                    } else {
                        Box(
                            Modifier
                                .size(tileSize)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest, shape)
                        )
                    }
                }
            }
        }
    }
}
