package vn.kozu.domain.model

import androidx.compose.ui.graphics.Color

enum class ThemePreset(
    val key: String,
    val label: String,
    val description: String,
    val seed: Color,
    val secondary: Color? = null,
    val tertiary: Color? = null,
    val style: PaletteStyle = PaletteStyle.TONAL_SPOT,
) {
    OSU(
        "osu",
        "osu! (Default)",
        "Signature pink with violet and blue",
        Color(0xFFFF66AA),
        Color(0xFF8C66FF),
        Color(0xFF3FA9F5),
    ),
    DYNAMIC(
        "dynamic",
        "Dynamic",
        "System wallpaper colors",
        Color(0xFFFF66AA),
    ),
    SAGE_GREEN(
        "sage_green",
        "Sage Green",
        "Soft mint and sage tones",
        Color(0xFF6FCF97),
        Color(0xFF2F7D55),
        Color(0xFF6EC6D3),
    ),
    SUNSET_ORANGE(
        "sunset_orange",
        "Sunset Orange",
        "Warm amber and peach tones",
        Color(0xFFFF9E5E),
        Color(0xFFB5651D),
        Color(0xFFE8C07A),
    ),
    PURPLE_PINK(
        "purple_pink",
        "Purple Pink",
        "Soft orchid and blush pink",
        Color(0xFFD58CD6),
        Color(0xFF8E4A9E),
        Color(0xFFF29BB5),
    ),
    OCEAN_BLUE(
        "ocean_blue",
        "Ocean Blue",
        "Calm ocean and sky tones",
        Color(0xFF5AA9F0),
        Color(0xFF1F5F9E),
        Color(0xFFB7A6E8),
    ),
    SUNNY_YELLOW(
        "sunny_yellow",
        "Sunny Yellow",
        "Warm honey and gold tones",
        Color(0xFFE8BC2F),
        Color(0xFF8A6A00),
        Color(0xFF8FD3A4),
    ),
    DARK_GREY(
        "dark_grey",
        "Dark & Grey",
        "Monochrome and gray tones",
        Color(0xFF9E9EA6),
        style = PaletteStyle.MONOCHROME,
    ),
    CUSTOM(
        "custom",
        "Custom accent",
        "One accent color of your choice",
        Color(0xFFFF66AA),
    ),
    ;

    companion object {
        fun fromKey(key: String?): ThemePreset = entries.firstOrNull { it.key == key } ?: OSU
    }
}
