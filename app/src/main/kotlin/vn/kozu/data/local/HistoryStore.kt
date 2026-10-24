package vn.kozu.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import vn.kozu.domain.model.Beatmap
import vn.kozu.util.dayKey

data class PlayEvent(val beatmapId: Long, val at: Long)

data class History(
    val tracks: Map<Long, Beatmap> = emptyMap(),
    val events: List<PlayEvent> = emptyList(),
    val dailyMs: Map<String, Long> = emptyMap()
) {

    val recent: List<Beatmap>
        get() = events.asReversed().map { it.beatmapId }.distinct().mapNotNull { tracks[it] }


    val mostPlayed: List<Beatmap>
        get() = events.groupingBy { it.beatmapId }.eachCount()
            .entries.sortedByDescending { it.value }
            .mapNotNull { tracks[it.key] }
}

class HistoryStore(context: Context) {
    private val store = context.applicationContext.kozuDataStore
    private val tracksKey = stringPreferencesKey("history_tracks_v1")
    private val eventsKey = stringPreferencesKey("history_events_v1")
    private val dailyKey = stringPreferencesKey("history_daily_v1")
    val history: Flow<History> = store.data.map { read(it) }

    suspend fun recordPlay(beatmap: Beatmap, at: Long = System.currentTimeMillis()) {
        store.edit { prefs ->
            val current = read(prefs)
            val events = (current.events + PlayEvent(beatmap.id, at)).takeLast(MAX_EVENTS)
            val referenced = events.map { it.beatmapId }.toSet()
            val tracks = (current.tracks + (beatmap.id to beatmap)).filterKeys { it in referenced }
            write(prefs, current.copy(tracks = tracks, events = events))
        }
    }

    suspend fun addListening(day: String, milliseconds: Long) {
        if (milliseconds <= 0L) return
        store.edit { prefs ->
            val current = read(prefs)
            val daily = (current.dailyMs + (day to ((current.dailyMs[day] ?: 0L) + milliseconds)))
                .toSortedMap()
                .let { sorted -> sorted.entries.toList().takeLast(MAX_DAYS).associate { it.key to it.value } }
            write(prefs, current.copy(dailyMs = daily))
        }
    }

    suspend fun clear() {
        store.edit {
            it.remove(tracksKey)
            it.remove(eventsKey)
            it.remove(dailyKey)
        }
    }

    private fun write(prefs: androidx.datastore.preferences.core.MutablePreferences, history: History) {
        prefs[tracksKey] = BeatmapJson.encodeList(history.tracks.values.toList()).toString()
        prefs[eventsKey] = JSONArray().also { array ->
            history.events.forEach { array.put(JSONArray().put(it.beatmapId).put(it.at)) }
        }.toString()
        prefs[dailyKey] = JSONObject().also { obj ->
            history.dailyMs.forEach { (day, ms) -> obj.put(day, ms) }
        }.toString()
    }

    private fun read(prefs: Preferences): History = try {
        val tracks = BeatmapJson.decodeList(prefs[tracksKey]).associateBy { it.id }
        val events = prefs[eventsKey]?.let { json ->
            val array = JSONArray(json)
            List(array.length()) { index ->
                val pair = array.getJSONArray(index)
                PlayEvent(pair.getLong(0), pair.getLong(1))
            }
        }.orEmpty()
        val daily = prefs[dailyKey]?.let { json ->
            val obj = JSONObject(json)
            obj.keys().asSequence().associateWith { obj.getLong(it) }
        }.orEmpty()
        History(tracks, events, daily)
    } catch (e: JSONException) {
        History()
    }

    private companion object {
        const val MAX_EVENTS = 1500
        const val MAX_DAYS = 90
    }
}
