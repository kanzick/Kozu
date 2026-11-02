package vn.kozu.data.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import vn.kozu.data.local.BeatmapJson
import vn.kozu.data.local.FavoritesStore
import vn.kozu.data.local.History
import vn.kozu.data.local.HistoryStore
import vn.kozu.data.local.PlayEvent
import vn.kozu.data.local.PlaylistStore
import vn.kozu.data.local.SettingsStore
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.Playlist

enum class BackupSection(
    val key: String,
    val label: String,
    val description: String,
) {
    SETTINGS("settings", "Settings", "Preferences and appearance. Credentials are never included."),
    FAVORITES("favorites", "Favorites", "Songs you have liked."),
    PLAYLISTS("playlists", "Playlists", "Your playlists and their songs."),
    HISTORY("history", "Listening history", "Plays and daily listening time."),
}

class BackupException(
    message: String,
) : Exception(message)

data class BackupInfo(
    val version: Int,
    val createdAt: Long,
    val counts: Map<BackupSection, Int>,
) {
    val sections: Set<BackupSection> get() = counts.keys
}

class BackupManager(
    private val settings: SettingsStore,
    private val favorites: FavoritesStore,
    private val playlists: PlaylistStore,
    private val history: HistoryStore,
) {
    suspend fun create(sections: Set<BackupSection>): String =
        withContext(Dispatchers.IO) {
            if (sections.isEmpty()) throw BackupException("Select at least one section")
            val body = JSONObject()
            if (BackupSection.SETTINGS in sections) body.put(BackupSection.SETTINGS.key, encodeSettings(settings.exportPreferences()))
            if (BackupSection.FAVORITES in sections) {
                body.put(BackupSection.FAVORITES.key, BeatmapJson.encodeList(favorites.snapshot()))
            }
            if (BackupSection.PLAYLISTS in sections) body.put(BackupSection.PLAYLISTS.key, encodePlaylists(playlists.snapshot()))
            if (BackupSection.HISTORY in sections) body.put(BackupSection.HISTORY.key, encodeHistory(history.snapshot()))
            JSONObject()
                .put("app", "kozu")
                .put("version", FORMAT_VERSION)
                .put("createdAt", System.currentTimeMillis())
                .put("sections", body)
                .toString(2)
        }

    fun inspect(json: String): BackupInfo {
        val root = parseRoot(json)
        val body = root.optJSONObject("sections") ?: throw BackupException("This file has no backup data")
        val counts = LinkedHashMap<BackupSection, Int>()
        BackupSection.entries.forEach { section ->
            when (section) {
                BackupSection.SETTINGS -> body.optJSONObject(section.key)?.let { counts[section] = decodeSettings(it).size }
                BackupSection.FAVORITES -> body.optJSONArray(section.key)?.let { counts[section] = decodeBeatmaps(it).size }
                BackupSection.PLAYLISTS -> body.optJSONArray(section.key)?.let { counts[section] = decodePlaylists(it).size }
                BackupSection.HISTORY -> body.optJSONObject(section.key)?.let { counts[section] = decodeHistory(it).events.size }
            }
        }
        if (counts.isEmpty()) throw BackupException("This file contains no recognised data")
        return BackupInfo(root.optInt("version"), root.optLong("createdAt"), counts)
    }

    suspend fun restore(
        json: String,
        sections: Set<BackupSection>,
    ): String =
        withContext(Dispatchers.IO) {
            val root = parseRoot(json)
            val body = root.optJSONObject("sections") ?: throw BackupException("This file has no backup data")
            val lines = mutableListOf<String>()
            sections.forEach { section ->
                when (section) {
                    BackupSection.SETTINGS -> {
                        body.optJSONObject(section.key)?.let {
                            val values = decodeSettings(it)
                            settings.importPreferences(values)
                            lines += "Settings: ${values.size} values restored"
                        }
                    }

                    BackupSection.FAVORITES -> {
                        body.optJSONArray(section.key)?.let {
                            val items = decodeBeatmaps(it)
                            val added = favorites.importAll(items)
                            lines += "Favorites: $added added, ${items.size - added} already saved"
                        }
                    }

                    BackupSection.PLAYLISTS -> {
                        body.optJSONArray(section.key)?.let {
                            val items = decodePlaylists(it)
                            val changed = playlists.importAll(items)
                            lines += "Playlists: ${items.size} processed, $changed changes"
                        }
                    }

                    BackupSection.HISTORY -> {
                        body.optJSONObject(section.key)?.let {
                            val data = decodeHistory(it)
                            val added = history.importAll(data)
                            lines += "History: $added plays added"
                        }
                    }
                }
            }
            if (lines.isEmpty()) throw BackupException("The selected sections are not in this file")
            lines.joinToString("\n")
        }

    private fun parseRoot(json: String): JSONObject {
        if (json.length > MAX_CHARS) throw BackupException("This file is too large to be a Kozu backup")
        val root =
            try {
                JSONObject(json)
            } catch (e: JSONException) {
                throw BackupException("This is not a valid Kozu backup file")
            }
        if (root.optString("app") != "kozu") throw BackupException("This file was not created by Kozu")
        val version = root.optInt("version", 0)
        if (version < 1 || version > FORMAT_VERSION) throw BackupException("Unsupported backup version")
        return root
    }

    private fun encodeSettings(values: Map<String, Any>): JSONObject {
        val result = JSONObject()
        values.toSortedMap().forEach { (name, value) ->
            val tag =
                when (value) {
                    is Boolean -> "b"
                    is Int -> "i"
                    is Long -> "l"
                    is String -> "s"
                    else -> return@forEach
                }
            result.put(name, JSONObject().put("t", tag).put("v", value))
        }
        return result
    }

    private fun decodeSettings(obj: JSONObject): Map<String, Any?> {
        val result = LinkedHashMap<String, Any?>()
        obj.keys().forEach { name ->
            if (!NAME_PATTERN.matches(name)) return@forEach
            val entry = obj.optJSONObject(name) ?: return@forEach
            val tag = entry.optString("t")
            val expected = TYPED_KEYS[name] ?: "s"
            if (tag != expected || !entry.has("v")) return@forEach
            val value: Any =
                try {
                    when (tag) {
                        "b" -> entry.getBoolean("v")
                        "i" -> entry.getInt("v")
                        "l" -> entry.getLong("v")
                        else -> entry.getString("v").take(MAX_STRING)
                    }
                } catch (e: JSONException) {
                    return@forEach
                }
            result[name] = value
        }
        return result
    }

    private fun decodeBeatmaps(array: JSONArray): List<Beatmap> {
        val result = ArrayList<Beatmap>()
        for (index in 0 until array.length()) {
            val obj = array.optJSONObject(index) ?: continue
            val beatmap =
                try {
                    BeatmapJson.decode(obj)
                } catch (e: JSONException) {
                    continue
                }
            if (beatmap.id > 0L) result += beatmap
            if (result.size >= MAX_ITEMS) break
        }
        return result
    }

    private fun encodePlaylists(list: List<Playlist>): JSONArray {
        val array = JSONArray()
        list.forEach { playlist ->
            array.put(
                JSONObject()
                    .put("id", playlist.id)
                    .put("name", playlist.name)
                    .put("createdAt", playlist.createdAt)
                    .put("tracks", BeatmapJson.encodeList(playlist.tracks)),
            )
        }
        return array
    }

    private fun decodePlaylists(array: JSONArray): List<Playlist> {
        val result = ArrayList<Playlist>()
        for (index in 0 until array.length()) {
            val obj = array.optJSONObject(index) ?: continue
            val id = obj.optString("id").trim()
            if (id.isEmpty() || id.length > MAX_STRING) continue
            result +=
                Playlist(
                    id = id,
                    name = obj.optString("name").take(MAX_STRING),
                    tracks = decodeBeatmaps(obj.optJSONArray("tracks") ?: JSONArray()),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                )
            if (result.size >= MAX_ITEMS) break
        }
        return result
    }

    private fun encodeHistory(data: History): JSONObject {
        val events = JSONArray()
        data.events.forEach { events.put(JSONArray().put(it.beatmapId).put(it.at)) }
        val daily = JSONObject()
        data.dailyMs.forEach { (day, ms) -> daily.put(day, ms) }
        return JSONObject()
            .put("tracks", BeatmapJson.encodeList(data.tracks.values.toList()))
            .put("events", events)
            .put("daily", daily)
    }

    private fun decodeHistory(obj: JSONObject): History {
        val tracks = decodeBeatmaps(obj.optJSONArray("tracks") ?: JSONArray()).associateBy { it.id }
        val events = ArrayList<PlayEvent>()
        val array = obj.optJSONArray("events") ?: JSONArray()
        for (index in 0 until minOf(array.length(), MAX_ITEMS)) {
            val pair = array.optJSONArray(index) ?: continue
            if (pair.length() < 2) continue
            val id = pair.optLong(0, 0L)
            val at = pair.optLong(1, 0L)
            if (id > 0L && at > 0L) events += PlayEvent(id, at)
        }
        val daily = LinkedHashMap<String, Long>()
        obj.optJSONObject("daily")?.let { source ->
            source.keys().forEach { day ->
                val ms = source.optLong(day, 0L)
                if (DAY_PATTERN.matches(day) && ms > 0L) daily[day] = ms
            }
        }
        return History(tracks, events, daily)
    }

    private companion object {
        const val FORMAT_VERSION = 1
        const val MAX_CHARS = 20_000_000
        const val MAX_ITEMS = 20_000
        const val MAX_STRING = 4_000
        val NAME_PATTERN = Regex("[a-z0-9_]{1,64}")
        val DAY_PATTERN = Regex("\\d{4}-\\d{2}-\\d{2}")

        val TYPED_KEYS: Map<String, String> =
            buildMap {
                listOf(
                    "amoled",
                    "artwork_colors",
                    "wifi_only_streaming",
                    "skip_silence",
                    "equalizer_enabled",
                    "keep_full_beatmap",
                    "wifi_only_downloads",
                    "auto_update_downloads",
                    "show_explicit",
                    "high_refresh_rate",
                    "performance_mode",
                    "blur_player",
                    "show_file_info",
                    "compact_navigation",
                    "show_mix_card",
                    "keep_playing_after_close",
                    "resume_on_headset",
                    "pause_on_headset_disconnect",
                    "hifi_mode",
                    "hardware_offload",
                    "remember_shuffle",
                    "show_queue_history",
                    "autoplay",
                    "avoid_repeats",
                    "dedupe_queue",
                    "preload_next",
                    "haptic_feedback",
                    "auto_open_player",
                    "artwork_swipe",
                    "keep_screen_on",
                    "double_tap_search",
                    "prefer_unicode",
                    "show_mapper",
                    "auto_sync_library",
                    "auto_clean_cache",
                    "auto_update_check",
                ).forEach { put(it, "b") }
                listOf(
                    "stream_cache_mb",
                    "blur_intensity",
                    "nav_corner_radius",
                    "image_cache_mb",
                    "bass_boost",
                    "virtualizer",
                    "loudness",
                    "top_size",
                    "proxy_port",
                    "download_limit_mb",
                ).forEach { put(it, "i") }
                listOf("last_sync_at", "last_update_check").forEach { put(it, "l") }
            }
    }
}
