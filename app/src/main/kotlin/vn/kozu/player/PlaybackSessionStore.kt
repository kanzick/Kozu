package vn.kozu.player

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import org.json.JSONException
import org.json.JSONObject
import vn.kozu.data.local.BeatmapJson
import vn.kozu.domain.model.Beatmap

data class PlaybackSessionSnapshot(
    val queue: List<Beatmap>,
    val queueIndex: Int,
    val positionMs: Long,
    val shuffleEnabled: Boolean,
    val repeatMode: Int
)

class PlaybackSessionStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun save(snapshot: PlaybackSessionSnapshot) {
        if (snapshot.queue.isEmpty()) {
            preferences.edit { remove(SESSION_KEY) }
            return
        }

        val json = JSONObject()
            .put("queue", BeatmapJson.encodeList(snapshot.queue))
            .put("index", snapshot.queueIndex.coerceIn(snapshot.queue.indices))
            .put("position", snapshot.positionMs.coerceAtLeast(0L))
            .put("shuffle", snapshot.shuffleEnabled)
            .put("repeat", snapshot.repeatMode)
        preferences.edit { putString(SESSION_KEY, json.toString()) }
    }

    fun load(): PlaybackSessionSnapshot? {
        val encoded = preferences.getString(SESSION_KEY, null) ?: return null
        return try {
            val json = JSONObject(encoded)
            val queue = BeatmapJson.decodeList(json.getJSONArray("queue"))
                .filter { it.id > 0L }
            if (queue.isEmpty()) return null
            PlaybackSessionSnapshot(
                queue = queue,
                queueIndex = json.optInt("index", 0).coerceIn(queue.indices),
                positionMs = json.optLong("position", 0L).coerceAtLeast(0L),
                shuffleEnabled = json.optBoolean("shuffle", false),
                repeatMode = json.optInt("repeat", 0).coerceIn(0, 2)
            )
        } catch (error: JSONException) {
            Log.e(TAG, "Could not restore the saved playback queue", error)
            preferences.edit { remove(SESSION_KEY) }
            null
        }
    }

    fun clear() {
        preferences.edit { remove(SESSION_KEY) }
    }

    private companion object {
        const val TAG = "PlaybackSessionStore"
        const val PREFERENCES_NAME = "kozu_playback_session"
        const val SESSION_KEY = "active_queue"
    }
}
