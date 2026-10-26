package vn.kozu.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.Playlist
import java.util.UUID

class PlaylistStore(context: Context) {

    private val store = context.applicationContext.kozuDataStore
    private val key = stringPreferencesKey("playlists_v1")

    val playlists: Flow<List<Playlist>> = store.data.map { read(it) }.distinctUntilChanged()


    suspend fun create(name: String, firstTrack: Beatmap? = null): String {
        val id = UUID.randomUUID().toString()
        update { list ->
            list + Playlist(
                id = id,
                name = name.trim().ifBlank { "New playlist" },
                tracks = listOfNotNull(firstTrack),
                createdAt = System.currentTimeMillis()
            )
        }
        return id
    }

    suspend fun rename(id: String, name: String) = update { list ->
        list.map { if (it.id == id) it.copy(name = name.trim().ifBlank { it.name }) else it }
    }

    suspend fun delete(id: String) = update { list -> list.filterNot { it.id == id } }

    suspend fun addTrack(id: String, beatmap: Beatmap) = update { list ->
        list.map { playlist ->
            if (playlist.id != id || playlist.tracks.any { it.id == beatmap.id }) playlist
            else playlist.copy(tracks = playlist.tracks + beatmap)
        }
    }

    suspend fun removeTrack(id: String, beatmapId: Long) = update { list ->
        list.map { playlist ->
            if (playlist.id == id) playlist.copy(tracks = playlist.tracks.filterNot { it.id == beatmapId })
            else playlist
        }
    }

    suspend fun updateMetadata(fresh: Map<Long, Beatmap>) = update { list ->
        list.map { playlist ->
            playlist.copy(tracks = playlist.tracks.map { old -> fresh[old.id]?.let { BeatmapJson.merge(old, it) } ?: old })
        }
    }

    suspend fun clear() {
        store.edit { it.remove(key) }
    }

    private suspend fun update(transform: (List<Playlist>) -> List<Playlist>) {
        store.edit { prefs -> prefs[key] = encode(transform(read(prefs))) }
    }

    private fun encode(list: List<Playlist>): String {
        val array = JSONArray()
        list.forEach { playlist ->
            array.put(
                JSONObject()
                    .put("id", playlist.id)
                    .put("name", playlist.name)
                    .put("created", playlist.createdAt)
                    .put("tracks", BeatmapJson.encodeList(playlist.tracks))
            )
        }
        return array.toString()
    }

    private fun read(prefs: Preferences): List<Playlist> {
        val json = prefs[key] ?: return emptyList()
        return try {
            val array = JSONArray(json)
            List(array.length()) { index ->
                val obj = array.getJSONObject(index)
                Playlist(
                    id = obj.getString("id"),
                    name = obj.optString("name"),
                    tracks = BeatmapJson.decodeList(obj.optJSONArray("tracks") ?: JSONArray()),
                    createdAt = obj.optLong("created")
                )
            }
        } catch (e: JSONException) {
            emptyList()
        }
    }
}
