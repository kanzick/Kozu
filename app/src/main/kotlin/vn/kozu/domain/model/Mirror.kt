package vn.kozu.domain.model

data class Mirror(
    val id: String,
    val name: String,
    val downloadUrl: String,
    val previewUrl: String = "",
    val referer: String = "",
    val builtIn: Boolean = true
) {
    fun downloadFor(setId: Long): String = downloadUrl.replace("{id}", setId.toString())
    fun previewFor(setId: Long): String? =
        previewUrl.takeIf { it.isNotBlank() }?.replace("{id}", setId.toString())
}

object Mirrors {
    val builtIn: List<Mirror> = listOf(
        Mirror(
            id = "catboy",
            name = "Mino (catboy.best)",
            downloadUrl = "https://catboy.best/d/{id}n",
            previewUrl = "https://catboy.best/preview/audio/{id}?set=1",
            referer = "https://catboy.best/"
        ),
        Mirror(
            id = "nerinyan",
            name = "Nerinyan",
            downloadUrl = "https://api.nerinyan.moe/d/{id}?noVideo=true"
        ),
        Mirror(
            id = "sayobot",
            name = "Sayobot",
            downloadUrl = "https://dl.sayobot.cn/beatmaps/download/novideo/{id}",
            previewUrl = "https://cdn.sayobot.cn:25225/preview/{id}.mp3"
        ),
        Mirror(
            id = "osudirect",
            name = "osu.direct",
            downloadUrl = "https://osu.direct/api/d/{id}"
        ),
        Mirror(
            id = "beatconnect",
            name = "Beatconnect",
            downloadUrl = "https://beatconnect.io/b/{id}/"
        )
    )

    const val OFFICIAL_PREVIEW = "https://b.ppy.sh/preview/{id}.mp3"
    const val OFFICIAL_DOWNLOAD = "https://osu.ppy.sh/api/v2/beatmapsets/{id}/download?noVideo=1"
    const val PROBE_SET_ID = 39804L

    fun active(custom: List<Mirror>, disabled: Set<String>): List<Mirror> =
        (builtIn + custom).filter { it.id !in disabled }
}
