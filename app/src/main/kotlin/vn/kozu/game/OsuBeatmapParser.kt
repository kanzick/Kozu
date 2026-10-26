package vn.kozu.game

data class OsuDifficulty(
    val file: String,
    val version: String,
    val mode: Int,
    val circleSize: Float,
    val approachRate: Float,
    val overallDifficulty: Float,
    val hpDrain: Float,
    val objectCount: Int,
    val audioFile: String,
    val previewTime: Int,
    val beatmapId: Long,
    val background: String?
) {
    val modeName: String
        get() = when (mode) {
            1 -> "taiko"
            2 -> "catch"
            3 -> "mania"
            else -> "osu!"
        }
}

object OsuBeatmapParser {

    fun parse(fileName: String, text: String): OsuDifficulty {
        var section = ""
        var audio = ""
        var preview = -1
        var mode = 0
        var version = ""
        var beatmapId = 0L
        var cs = 5f
        var ar = Float.NaN
        var od = 5f
        var hp = 5f
        var objects = 0
        var background: String? = null

        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim().removePrefix("\uFEFF")
            if (line.isEmpty() || line.startsWith("//")) continue
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length - 1)
                continue
            }
            when (section) {
                "General" -> keyValue(line)?.let { (k, v) ->
                    when (k) {
                        "AudioFilename" -> audio = v
                        "PreviewTime" -> preview = v.toIntOrNull() ?: -1
                        "Mode" -> mode = v.toIntOrNull() ?: 0
                    }
                }
                "Metadata" -> keyValue(line)?.let { (k, v) ->
                    when (k) {
                        "Version" -> version = v
                        "BeatmapID" -> beatmapId = v.toLongOrNull() ?: 0L
                    }
                }
                "Difficulty" -> keyValue(line)?.let { (k, v) ->
                    val number = v.toFloatOrNull() ?: return@let
                    when (k) {
                        "CircleSize" -> cs = number
                        "ApproachRate" -> ar = number
                        "OverallDifficulty" -> od = number
                        "HPDrainRate" -> hp = number
                    }
                }
                "Events" -> if (background == null && line.startsWith("0,0,")) {
                    val parts = line.split(',')
                    if (parts.size >= 3) background = parts[2].trim().trim('"').takeIf { it.isNotEmpty() }
                }
                "HitObjects" -> objects++
            }
        }

        return OsuDifficulty(
            file = fileName,
            version = version.ifBlank { fileName.substringBeforeLast('.') },
            mode = mode,
            circleSize = cs,
            approachRate = if (ar.isNaN()) od else ar,
            overallDifficulty = od,
            hpDrain = hp,
            objectCount = objects,
            audioFile = audio,
            previewTime = preview,
            beatmapId = beatmapId,
            background = background
        )
    }

    private fun keyValue(line: String): Pair<String, String>? {
        val index = line.indexOf(':')
        if (index <= 0) return null
        return line.substring(0, index).trim() to line.substring(index + 1).trim()
    }
}
