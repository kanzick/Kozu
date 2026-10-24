package vn.kozu.domain.model

enum class ThemeMode(val key: String, val label: String) {
    SYSTEM("system", "System"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val amoled: Boolean = false,
    val artworkColors: Boolean = true,
    val displayName: String = "",
    val osuClientId: String = "",
    val osuClientSecret: String = ""
) {
    val hasCredentials: Boolean get() = osuClientId.isNotBlank() && osuClientSecret.isNotBlank()
}
