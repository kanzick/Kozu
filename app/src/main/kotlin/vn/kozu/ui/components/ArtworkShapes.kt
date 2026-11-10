package vn.kozu.ui.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import vn.kozu.domain.model.ArtworkShape
import vn.kozu.ui.theme.KozuCookies

fun ArtworkShape.toShape(fallback: Shape): Shape =
    when (this) {
        ArtworkShape.DEFAULT -> fallback
        ArtworkShape.SOFT -> RoundedCornerShape(percent = 28)
        ArtworkShape.CIRCLE -> CircleShape
        ArtworkShape.COOKIE -> KozuCookies.Soft9
        ArtworkShape.FLOWER -> KozuCookies.Flower8
        ArtworkShape.CLOVER -> KozuCookies.Clover4
        ArtworkShape.SQUARE -> RoundedCornerShape(6.dp)
    }


@Composable
@ReadOnlyComposable
fun resolveArtworkShape(fallback: Shape): Shape = LocalAppSettings.current.appearance.artworkShape.toShape(fallback)
