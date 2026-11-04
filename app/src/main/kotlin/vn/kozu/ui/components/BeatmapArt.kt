package vn.kozu.ui.components

import vn.kozu.ui.i18n.kozuUiString

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import vn.kozu.domain.model.ArtworkQuality
import vn.kozu.domain.model.Beatmap
import java.io.File

@Composable
fun BeatmapArt(
    beatmap: Beatmap,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    large: Boolean = false,
    /** When false the caller's [shape] is used as-is instead of the global album-art shape. */
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
            contentDescription = kozuUiString("${beatmap.title} cover"),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private const val FULL_ART_MIN_ASPECT = 0.6f
private const val FULL_ART_MAX_ASPECT = 3.6f

/**
 * Shows the whole background image instead of the cropped banner/thumbnail variants.
 *
 * Source order: osu!'s `raw.jpg`; if that does not exist for the set, the original picture from the
 * downloaded copy or from the set's .osz on a mirror (see [LocalBackgroundResolver]); the cropped
 * covers are only a placeholder/last resort. With [wrapToImage] the visible frame takes the image's own aspect
 * ratio (inside the space the caller gives it), so no empty bars are left above and below it.
 * Pass `wrapToImage = false` for non-rectangular shapes, which need a square frame.
 */
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
    // The true original, taken from the downloaded set or extracted from a mirror's .osz.
    // osu!'s own raw.jpg only exists for some sets, so this is what makes it work for every song.
    var original by remember(beatmap.id) { mutableStateOf<File?>(null) }
    val resolver = LocalBackgroundResolver.current
    val wantsFull = quality != ArtworkQuality.LOW
    LaunchedEffect(beatmap.id, stage, wantsFull) {
        if (resolver == null || !wantsFull || original != null) return@LaunchedEffect
        // Cheap local lookup first; go online only once raw.jpg has failed.
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
                contentDescription = kozuUiString("${beatmap.title} cover"),
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
                        // A broken cached file must not stick around.
                        original = null
                    } else if (stage < sources.lastIndex) {
                        stage++
                    }
                },
            )
        }
    }
}
