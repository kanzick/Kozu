package vn.kozu.ui.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import vn.kozu.domain.model.ArtworkShape
import vn.kozu.ui.theme.KozuCookies

/** Turns the user's artwork-shape choice into a real [Shape]; [fallback] is used for [ArtworkShape.DEFAULT]. */
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

/**
 * Applies the global album-art shape. Components that draw album art pass the shape they would use
 * anyway; when the user picked a global shape it replaces it everywhere in the app.
 */
@Composable
@ReadOnlyComposable
fun resolveArtworkShape(fallback: Shape): Shape = LocalAppSettings.current.appearance.artworkShape.toShape(fallback)
