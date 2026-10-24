package vn.kozu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import vn.kozu.domain.model.Beatmap

@Composable
fun BeatmapArt(
    beatmap: Beatmap,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    large: Boolean = false
) {
    val url = if (large) beatmap.artUrl else beatmap.thumbUrl
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(colors.primaryContainer, colors.tertiaryContainer))),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = colors.onPrimaryContainer.copy(alpha = 0.55f),
            modifier = Modifier.size(28.dp)
        )
        if (url != null) {
            val request = remember(url) {
                ImageRequest.Builder(context).data(url).crossfade(300).build()
            }
            AsyncImage(
                model = request,
                contentDescription = "${beatmap.title} cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
