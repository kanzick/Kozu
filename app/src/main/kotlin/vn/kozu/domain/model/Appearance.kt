package vn.kozu.domain.model

import org.json.JSONObject

interface KeyedOption {
    val key: String
    val label: String
}

private fun <T : KeyedOption> List<T>.byKey(
    key: String?,
    fallback: T,
): T = firstOrNull { it.key == key } ?: fallback

enum class UiDensity(
    override val key: String,
    override val label: String,
    val description: String,
    val layoutScale: Float,
    val compactRows: Boolean,
) : KeyedOption {
    COMFORTABLE("comfortable", "Comfortable", "Roomy spacing and larger controls", 1f, false),
    COMPACT("compact", "Compact", "Tighter spacing everywhere in the app", 0.9f, true),
    DENSE("dense", "Dense", "Fit as much content on screen as possible", 0.8f, true),
    ;

    companion object {
        fun fromKey(key: String?): UiDensity = entries.byKey(key, COMFORTABLE)
    }
}

enum class MiniPlayerStyle(
    override val key: String,
    override val label: String,
    val description: String,
) : KeyedOption {
    FLOATING("floating", "Floating pill", "Rounded bar floating above the navigation"),
    DOCKED("docked", "Docked bar", "Edge-to-edge bar with a progress line"),
    CARD("card", "Expressive card", "Taller card with a seek bar"),
    MINIMAL("minimal", "Minimal", "Only artwork, title and play"),
    ;

    companion object {
        fun fromKey(key: String?): MiniPlayerStyle = entries.byKey(key, FLOATING)
    }
}

enum class MiniPlayerColors(
    override val key: String,
    override val label: String,
) : KeyedOption {
    GRADIENT("gradient", "Accent gradient"),
    TONAL("tonal", "Tonal"),
    ACCENT("accent", "Solid accent"),
    NEUTRAL("neutral", "Neutral surface"),
    ;

    companion object {
        fun fromKey(key: String?): MiniPlayerColors = entries.byKey(key, GRADIENT)
    }
}

enum class MiniPlayerProgress(
    override val key: String,
    override val label: String,
) : KeyedOption {
    RING("ring", "Ring around artwork"),
    LINE("line", "Thin line"),
    WAVY("wavy", "Wavy line"),
    NONE("none", "Hidden"),
    ;

    companion object {
        fun fromKey(key: String?): MiniPlayerProgress = entries.byKey(key, RING)
    }
}

enum class ArtworkShape(
    override val key: String,
    override val label: String,
) : KeyedOption {
    DEFAULT("default", "Default"),
    SOFT("soft", "Soft rounded"),
    CIRCLE("circle", "Circle"),
    COOKIE("cookie", "Cookie"),
    FLOWER("flower", "Flower"),
    CLOVER("clover", "Clover"),
    SQUARE("square", "Square"),
    ;

    companion object {
        fun fromKey(key: String?): ArtworkShape = entries.byKey(key, DEFAULT)
    }
}


enum class SeekBarStyle(
    override val key: String,
    override val label: String,
) : KeyedOption {
    WAVY("wavy", "Wavy"),
    FLAT("flat", "Flat"),
    THICK("thick", "Thick pill"),
    THIN("thin", "Thin line"),
    ;

    companion object {
        fun fromKey(key: String?): SeekBarStyle = entries.byKey(key, WAVY)
    }
}

enum class SliderStyle(
    override val key: String,
    override val label: String,
) : KeyedOption {
    EXPRESSIVE("expressive", "Expressive"),
    WAVY("wavy", "Wavy"),
    CLASSIC("classic", "Classic"),
    ;

    companion object {
        fun fromKey(key: String?): SliderStyle = entries.byKey(key, EXPRESSIVE)
    }
}

enum class TransportShape(
    override val key: String,
    override val label: String,
) : KeyedOption {
    PILL("pill", "Expressive pills"),
    ROUND("round", "Fully round"),
    SQUARE("square", "Rounded squares"),
    ;

    companion object {
        fun fromKey(key: String?): TransportShape = entries.byKey(key, PILL)
    }
}

enum class IconStyle(
    override val key: String,
    override val label: String,
) : KeyedOption {
    ROUNDED("rounded", "Rounded"),
    OUTLINED("outlined", "Outlined"),
    SHARP("sharp", "Sharp"),
    ;

    companion object {
        fun fromKey(key: String?): IconStyle = entries.byKey(key, ROUNDED)
    }
}

/**
 * Full-player design. Each entry is a genuinely different layout (see `PlayerDesigns.kt`),
 * modelled on the design picker in ArchiveTune.
 */
enum class PlayerLayoutStyle(
    override val key: String,
    override val label: String,
    val description: String,
) : KeyedOption {
    CLASSIC("classic", "Classic", "Artwork, track info and a familiar five-button transport row"),
    MODERN("modern", "Modern", "Rounded pill controls with favourite and more beside the title"),
    MINIMAL("minimal", "Minimal", "Quiet and light: small type, thin slider, plain icons"),
    CINEMATIC("cinematic", "Cinematic", "Glowing artwork over a blurred backdrop, centred text"),
    LITTLE("little", "Little", "Chunky three-button transport with a compact toggle row"),
    EXPRESSIVE("expressive", "Expressive", "Bold vertical title on a two-tone colour field"),
    IMMERSIVE("immersive", "Immersive", "Edge-to-edge artwork fading into the controls"),
    IMMERSIVE_EXTENDED("immersive_extended", "Immersive Extended", "Full-screen artwork with the controls floating over it"),
    MATERIAL_EXTENDED("material_extended", "Material Extended", "Material 3 layout with format chip and volume bar"),
    EDITORIAL("editorial", "Editorial", "Cookie artwork and a magazine-style headline"),
    ;

    /** True for the designs that have room for the optional volume slider. */
    val supportsVolumeBar: Boolean
        get() = this == IMMERSIVE || this == IMMERSIVE_EXTENDED || this == MATERIAL_EXTENDED

    companion object {
        /** Maps the old four layouts onto their closest new design so nobody's choice is lost. */
        fun fromKey(key: String?): PlayerLayoutStyle =
            when (key) {
                "balanced" -> CLASSIC
                "compact" -> LITTLE
                else -> entries.byKey(key, CLASSIC)
            }
    }
}

data class AppearancePrefs(
    val density: UiDensity = UiDensity.COMFORTABLE,
    val textScale: Int = 100,
    val fontWeightShift: Int = 0,
    val fontWidth: Int = 100,
    val fontRoundness: Int = 100,
    val artworkShape: ArtworkShape = ArtworkShape.DEFAULT,
    val iconStyle: IconStyle = IconStyle.ROUNDED,
    val sliderStyle: SliderStyle = SliderStyle.EXPRESSIVE,
    val miniStyle: MiniPlayerStyle = MiniPlayerStyle.FLOATING,
    val miniColors: MiniPlayerColors = MiniPlayerColors.GRADIENT,
    val miniProgress: MiniPlayerProgress = MiniPlayerProgress.RING,
    val miniArtShape: ArtworkShape = ArtworkShape.CIRCLE,
    val miniCorner: Int = 32,
    val miniShowControls: Boolean = true,
    val miniShowArtist: Boolean = true,
    val miniSpinArtwork: Boolean = true,
    val miniSwipeToSkip: Boolean = true,
    val seekStyle: SeekBarStyle = SeekBarStyle.WAVY,
    val playerLayoutStyle: PlayerLayoutStyle = PlayerLayoutStyle.CLASSIC,
    val playerArtShape: ArtworkShape = ArtworkShape.DEFAULT,
    val transportShape: TransportShape = TransportShape.PILL,
    val showToggleRow: Boolean = true,
    val showVolumeBar: Boolean = true,
) {
    fun toJson(): String =
        JSONObject()
            .put("density", density.key)
            .put("textScale", textScale)
            .put("fontWeightShift", fontWeightShift)
            .put("fontWidth", fontWidth)
            .put("fontRoundness", fontRoundness)
            .put("artworkShape", artworkShape.key)
            .put("iconStyle", iconStyle.key)
            .put("sliderStyle", sliderStyle.key)
            .put("miniStyle", miniStyle.key)
            .put("miniColors", miniColors.key)
            .put("miniProgress", miniProgress.key)
            .put("miniArtShape", miniArtShape.key)
            .put("miniCorner", miniCorner)
            .put("miniShowControls", miniShowControls)
            .put("miniShowArtist", miniShowArtist)
            .put("miniSpinArtwork", miniSpinArtwork)
            .put("miniSwipeToSkip", miniSwipeToSkip)
            .put("seekStyle", seekStyle.key)
            .put("playerLayoutStyle", playerLayoutStyle.key)
            .put("playerArtShape", playerArtShape.key)
            .put("transportShape", transportShape.key)
            .put("showToggleRow", showToggleRow)
            .put("showVolumeBar", showVolumeBar)
            .toString()

    companion object {
        const val TEXT_SCALE_MIN = 80
        const val TEXT_SCALE_MAX = 150
        const val WEIGHT_SHIFT_MIN = -200
        const val WEIGHT_SHIFT_MAX = 300
        const val WIDTH_MIN = 75
        const val WIDTH_MAX = 125
        const val MINI_CORNER_MAX = 40

        fun fromJson(json: String?): AppearancePrefs {
            if (json.isNullOrBlank()) return AppearancePrefs()
            return try {
                val o = JSONObject(json)
                val d = AppearancePrefs()
                AppearancePrefs(
                    density = UiDensity.fromKey(o.optString("density")),
                    textScale = o.optInt("textScale", d.textScale).coerceIn(TEXT_SCALE_MIN, TEXT_SCALE_MAX),
                    fontWeightShift = o.optInt("fontWeightShift", d.fontWeightShift).coerceIn(WEIGHT_SHIFT_MIN, WEIGHT_SHIFT_MAX),
                    fontWidth = o.optInt("fontWidth", d.fontWidth).coerceIn(WIDTH_MIN, WIDTH_MAX),
                    fontRoundness = o.optInt("fontRoundness", d.fontRoundness).coerceIn(0, 100),
                    artworkShape = ArtworkShape.fromKey(o.optString("artworkShape")),
                    iconStyle = IconStyle.fromKey(o.optString("iconStyle")),
                    sliderStyle = SliderStyle.fromKey(o.optString("sliderStyle")),
                    miniStyle = MiniPlayerStyle.fromKey(o.optString("miniStyle")),
                    miniColors = MiniPlayerColors.fromKey(o.optString("miniColors")),
                    miniProgress = MiniPlayerProgress.fromKey(o.optString("miniProgress")),
                    miniArtShape =
                        o.optString("miniArtShape").let { key ->
                            if (key.isBlank()) d.miniArtShape else ArtworkShape.fromKey(key)
                        },
                    miniCorner = o.optInt("miniCorner", d.miniCorner).coerceIn(0, MINI_CORNER_MAX),
                    miniShowControls = o.optBoolean("miniShowControls", d.miniShowControls),
                    miniShowArtist = o.optBoolean("miniShowArtist", d.miniShowArtist),
                    miniSpinArtwork = o.optBoolean("miniSpinArtwork", d.miniSpinArtwork),
                    miniSwipeToSkip = o.optBoolean("miniSwipeToSkip", d.miniSwipeToSkip),
                    seekStyle = SeekBarStyle.fromKey(o.optString("seekStyle")),
                    playerLayoutStyle = PlayerLayoutStyle.fromKey(o.optString("playerLayoutStyle")),
                    playerArtShape = ArtworkShape.fromKey(o.optString("playerArtShape")),
                    transportShape = TransportShape.fromKey(o.optString("transportShape")),
                    showToggleRow = o.optBoolean("showToggleRow", d.showToggleRow),
                    showVolumeBar = o.optBoolean("showVolumeBar", d.showVolumeBar),
                )
            } catch (e: Exception) {
                AppearancePrefs()
            }
        }
    }
}
