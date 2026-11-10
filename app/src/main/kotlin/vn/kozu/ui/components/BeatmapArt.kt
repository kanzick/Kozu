package vn.kozu.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.crossfade
import coil3.request.ImageRequest
import java.io.File
import vn.kozu.domain.model.ArtworkQuality
import vn.kozu.domain.model.Beatmap
import vn.kozu.R

@Composable
fun BeatmapArt(
    beatmap: Beatmap,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    large: Boolean = false,

    followGlobalShape: Boolean = true,
) {
    val appSettings = LocalAppSettings.current
    val artShape = if (followGlobalShape) resolveArtworkShape(shape) else shape
    val metered = LocalMetered.current
    val quality = if (metered) appSettings.artworkQualityMobile else appSettings.artworkQualityWifi
    val url =
        when (quality) {
            ArtworkQuality.LOW -> "https://assets.ppy.sh/beatmaps/${beatmap.id}/covers/list.jpg"
            ArtworkQuality.MEDIUM -> beatmap.thumbUrl
            ArtworkQuality.HIGH -> if (large) beatmap.artUrl else beatmap.thumbUrl
        }
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    Box(
        modifier =
            modifier
                .clip(artShape)
                .background(Brush.linearGradient(listOf(colors.primaryContainer, colors.tertiaryContainer))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = colors.onPrimaryContainer.copy(alpha = 0.55f),
            modifier = Modifier.size(if (large) 56.dp else 28.dp),
        )
        val request =
            remember(url) {
                ImageRequest
                    .Builder(context)
                    .data(url)
                    .crossfade(200)
                    .build()
            }
        AsyncImage(
            model = request,
            contentDescription = stringResource(R.string.cover_for, beatmap.title),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private const val FULL_ART_MIN_ASPECT = 0.6f
private const val FULL_ART_MAX_ASPECT = 3.6f


@Composable
fun FullBeatmapArt(
    beatmap: Beatmap,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    followGlobalShape: Boolean = true,
    wrapToImage: Boolean = true,
) {
    val appSettings = LocalAppSettings.current
    val artShape = if (followGlobalShape) resolveArtworkShape(shape) else shape
    val metered = LocalMetered.current
    val quality = if (metered) appSettings.artworkQualityMobile else appSettings.artworkQualityWifi
    val sources =
        remember(beatmap.id, quality) {
            when (quality) {
                ArtworkQuality.LOW -> listOf("https://assets.ppy.sh/beatmaps/${beatmap.id}/covers/list.jpg")
                else -> listOf(beatmap.fullArtUrl, beatmap.artUrl, beatmap.thumbUrl)
            }
        }
    var stage by remember(sources) { mutableStateOf(0) }


    var original by remember(beatmap.id) { mutableStateOf<File?>(null) }
    val resolver = LocalBackgroundResolver.current
    val wantsFull = quality != ArtworkQuality.LOW
    LaunchedEffect(beatmap.id, stage, wantsFull) {
        if (resolver == null || !wantsFull || original != null) return@LaunchedEffect

        original = resolver(beatmap.id, stage > 0)
    }
    var imageAspect by remember(beatmap.id) { mutableStateOf(1f) }
    val aspect by animateFloatAsState(
        targetValue = if (wrapToImage) imageAspect else 1f,
        label = "fullArtAspect",
    )
    val url = sources[stage.coerceIn(0, sources.lastIndex)]
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val request =
        remember(url, original) {
            ImageRequest
                .Builder(context)
                .data(original ?: url)
                .crossfade(200)
                .build()
        }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier =
                Modifier
                    .aspectRatio(aspect)
                    .clip(artShape)
                    .background(Brush.linearGradient(listOf(colors.primaryContainer, colors.tertiaryContainer))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = colors.onPrimaryContainer.copy(alpha = 0.55f),
                modifier = Modifier.size(56.dp),
            )
            AsyncImage(
                model = request,
                contentDescription = stringResource(R.string.cover_for, beatmap.title),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                onSuccess = { success ->
                    val size = success.painter.intrinsicSize
                    if (size.isSpecified && size.width > 0f && size.height > 0f) {
                        imageAspect = (size.width / size.height).coerceIn(FULL_ART_MIN_ASPECT, FULL_ART_MAX_ASPECT)
                    }
                },
                onError = {
                    if (original != null) {

                        original = null
                    } else if (stage < sources.lastIndex) {
                        stage++
                    }
                },
            )
        }
    }
}
