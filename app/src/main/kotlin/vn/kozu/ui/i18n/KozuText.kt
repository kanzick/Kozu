package vn.kozu.ui.i18n

import android.content.Context
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnit.Companion.Unspecified
import androidx.compose.ui.res.stringResource
import java.util.concurrent.ConcurrentHashMap

/**
 * Drop-in Material 3 Text replacement. Source strings and translations live in Android resources.
 * Resource names are resolved at runtime so this helper does not depend on a generated R import.
 */
@Composable
fun KozuText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
) {
    MaterialText(
        text = kozuUiString(text),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style,
    )
}

@Composable
fun kozuUiString(source: String): String {
    val context = LocalContext.current
    val directName = UiStringIds.find(source)
    if (directName != null) {
        val directId = remember(context.resources, directName) {
            resolveStringId(context, directName)
        }
        if (directId != 0) return stringResource(directId)
    }

    val match = matchDynamicText(source) ?: return source
    val resourceId = remember(context.resources, match.resourceName) {
        resolveStringId(context, match.resourceName)
    }
    if (resourceId == 0) return source

    val arguments = match.arguments.map { argument ->
        if (argument is String) localizeArgument(context, argument) else argument
    }.toTypedArray()
    return stringResource(resourceId, *arguments)
}

fun localizeUiString(context: Context, source: String): String {
    UiStringIds.find(source)?.let { name ->
        val id = resolveStringId(context, name)
        if (id != 0) return context.getString(id)
    }

    val match = matchDynamicText(source) ?: return source
    val resourceId = resolveStringId(context, match.resourceName)
    if (resourceId == 0) return source
    val arguments = match.arguments.map { argument ->
        if (argument is String) localizeArgument(context, argument) else argument
    }.toTypedArray()
    return context.getString(resourceId, *arguments)
}

private val stringResourceIds = ConcurrentHashMap<String, Int>()

private fun resolveStringId(context: Context, resourceName: String): Int =
    stringResourceIds.getOrPut(resourceName) {
        context.resources.getIdentifier(resourceName, "string", context.packageName)
    }

private fun localizeArgument(context: Context, value: String): String {
    val name = UiStringIds.find(value) ?: return value
    val id = resolveStringId(context, name)
    return if (id != 0) context.getString(id) else value
}

private data class DynamicText(
    val resourceName: String,
    val arguments: Array<out Any>,
)

private fun matchDynamicText(source: String): DynamicText? {
    for ((pattern, resourceName) in dynamicPatterns) {
        val match = pattern.matchEntire(source) ?: continue
        val values = match.groupValues
        val arguments: Array<out Any> = when (resourceName) {
            "ui_count_songs",
            "ui_count_songs_offline",
            "ui_count_beatmaps",
            "ui_count_results",
            "ui_page",
            "ui_downloading_percent",
            "ui_level",
            "ui_plays",
            "ui_minutes",
            "ui_days_ago",
            "ui_saved_playlist",
            "ui_downloading_songs" -> arrayOf<Any>(values[1].toInt())
            "ui_tracks_in_queue",
            "ui_mix_refreshed_daily",
            "ui_forgotten_tracks_days",
            "ui_connected_latency",
            "ui_hit_sounds_ready" -> arrayOf<Any>(values[1].toInt())
            "ui_page_of" -> arrayOf<Any>(values[1].toInt(), values[2].toInt())
            "ui_download_artist_size_age" -> arrayOf<Any>(values[1], values[2], values[3].toInt())
            "ui_joined_followers",
            "ui_version_checked",
            "ui_rating_range" -> arrayOf<Any>(values[1], values[2])
            "ui_difficulty_summary" -> arrayOf<Any>(values[1], values[2].toInt(), values[3].toInt())
            "ui_up_to_rating",
            "ui_sorted_by",
            "ui_audio_file_log",
            "ui_preparation_failed" -> arrayOf<Any>(values[1])
            "ui_chart_log" -> arrayOf<Any>(values[1], values[2])
            "ui_mania_mode_log" -> arrayOf<Any>(values[1].toInt(), values[2].toInt())
            "ui_artist_play_count" -> arrayOf<Any>(values[1], values[2].toInt())
            "ui_delete_playlist_confirm" -> arrayOf<Any>(values[1])
            "ui_mania_score_line" -> arrayOf<Any>(values[1], values[2], values[3], values[4])
            "ui_beatmap_number" -> arrayOf<Any>(values[1].toLong())
            "ui_days_hours" -> arrayOf<Any>(values[1].toInt(), values[2].toInt())
            "ui_downloading_source" -> arrayOf<Any>(values[1].toInt(), values[2])
            "ui_kozu_available",
            "ui_mapped_by",
            "ui_error_prefix",
            "ui_could_not_save_score",
            "ui_personal_best",
            "ui_could_not_read_chart",
            "ui_remove_chart_files",
            "ui_downloading_track",
            "ui_added_to_playlist",
            "ui_see_all",
            "ui_play_item",
            "ui_delete_item",
            "ui_drag_reorder",
            "ui_remove_item",
            "ui_avatar_for",
            "ui_cover_for",
            "ui_local_charts_refresh_failed",
            "ui_preview_unavailable",
            "ui_save_settings_failed",
            "ui_game_setup_failed",
            "ui_personal_scores_failed",
            "ui_spdx_prefix",
            "ui_ranked_score" -> arrayOf<Any>(values[1])
            "ui_mapped_difficulty" -> arrayOf<Any>(values[1], values[2])
            else -> continue
        }
        return DynamicText(resourceName, arguments)
    }
    return null
}

private val dynamicPatterns: List<Pair<Regex, String>> = listOf(
    Regex("^Local charts could not be refreshed: (.+)$") to "ui_local_charts_refresh_failed",
    Regex("^Preview unavailable: (.+)$") to "ui_preview_unavailable",
    Regex("^Could not save settings: (.+)$") to "ui_save_settings_failed",
    Regex("^Game setup failed: (.+)$") to "ui_game_setup_failed",
    Regex("^Could not load personal scores: (.+)$") to "ui_personal_scores_failed",
    Regex("^SPDX: (.+)$") to "ui_spdx_prefix",
    Regex("^Ranked score (.+)$") to "ui_ranked_score",
    Regex("^Up to (.+)$") to "ui_up_to_rating",
    Regex("^(.+) – (.+)$") to "ui_rating_range",
    Regex("^Sorted by (.+)$") to "ui_sorted_by",
    Regex("^Chart: (.+) — (.+)$") to "ui_chart_log",
    Regex("^Mode: Mania • (\\d+) keys • (\\d+) notes$") to "ui_mania_mode_log",
    Regex("^Audio file: (.+)$") to "ui_audio_file_log",
    Regex("^Hit sounds ready: (\\d+) files$") to "ui_hit_sounds_ready",
    Regex("^Preparation failed: (.+)$") to "ui_preparation_failed",
    Regex("^(\\d+) tracks? in queue$") to "ui_tracks_in_queue",
    Regex("^(\\d+) songs · refreshed daily$") to "ui_mix_refreshed_daily",
    Regex("^Offline songs not played in the last (\\d+) days\\.$") to "ui_forgotten_tracks_days",
    Regex("^(.+) · (.+) · (\\d+) days ago$") to "ui_download_artist_size_age",
    Regex("^Joined (.+) · (.+) followers$") to "ui_joined_followers",
    Regex("^Connected in (\\d+) ms$") to "ui_connected_latency",
    Regex("^v(.+) · (.+)$") to "ui_version_checked",
    Regex("^(.+) • (\\d+)K • (\\d+) notes$") to "ui_difficulty_summary",
    Regex("^(.+) • (\\d+) plays$") to "ui_artist_play_count",
    Regex("^\"(.+)\" will be removed\\. Your songs are not deleted\\.$") to "ui_delete_playlist_confirm",
    Regex("^(\\d+)\\s+·\\s+([\\d.]+%)\\s+·\\s+([\\d,]+) pts\\s+·\\s+(\\d+/\\d+) combo$") to "ui_mania_score_line",
    Regex("^(\\d+) songs?$", RegexOption.IGNORE_CASE) to "ui_count_songs",
    Regex("^(\\d+) songs? offline$", RegexOption.IGNORE_CASE) to "ui_count_songs_offline",
    Regex("^(\\d+) beatmaps?$", RegexOption.IGNORE_CASE) to "ui_count_beatmaps",
    Regex("^(\\d+) results$", RegexOption.IGNORE_CASE) to "ui_count_results",
    Regex("^Page (\\d+) of (\\d+)$") to "ui_page_of",
    Regex("^Page (\\d+)$") to "ui_page",
    Regex("^Beatmap #(\\d+)$") to "ui_beatmap_number",
    Regex("^Mapped by (.+)$", RegexOption.IGNORE_CASE) to "ui_mapped_by",
    Regex("^\\[(.+)]\\s*·\\s*mapped by (.+)$", RegexOption.IGNORE_CASE) to "ui_mapped_difficulty",
    Regex("^Downloading (\\d+)%$") to "ui_downloading_percent",
    Regex("^Downloading · (\\d+)% · (.+)$") to "ui_downloading_source",
    Regex("^Kozu v(.+) is available$") to "ui_kozu_available",
    Regex("^Level (\\d+)$") to "ui_level",
    Regex("^(\\d+) plays$") to "ui_plays",
    Regex("^(\\d+) min$") to "ui_minutes",
    Regex("^(\\d+) days ago$") to "ui_days_ago",
    Regex("^(\\d+)d (\\d+)h$") to "ui_days_hours",
    Regex("^Error: (.+)$") to "ui_error_prefix",
    Regex("^Could not save score: (.+)$") to "ui_could_not_save_score",
    Regex("^Personal best: (.+)$") to "ui_personal_best",
    Regex("^Could not read chart: (.+)$") to "ui_could_not_read_chart",
    Regex("^Remove \"(.+)\" and its chart files from this device\\?$") to "ui_remove_chart_files",
    Regex("^Saved (\\d+) tracks as a playlist$") to "ui_saved_playlist",
    Regex("^Downloading (\\d+) songs$") to "ui_downloading_songs",
    Regex("^Downloading (.+)$") to "ui_downloading_track",
    Regex("^Added to (.+)$") to "ui_added_to_playlist",
    Regex("^See all (.+)$") to "ui_see_all",
    Regex("^Play (.+)$") to "ui_play_item",
    Regex("^Delete (.+)$") to "ui_delete_item",
    Regex("^Drag to reorder (.+)$") to "ui_drag_reorder",
    Regex("^Remove (.+)$") to "ui_remove_item",
    Regex("^(.+) avatar$") to "ui_avatar_for",
    Regex("^(.+) cover$") to "ui_cover_for",
)
