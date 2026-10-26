package vn.kozu.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import vn.kozu.domain.model.Beatmap
import java.io.File

data class DownloadedSet(
    val beatmap: Beatmap,
    val audioFile: String,
    val backgroundFile: String,
    val sizeBytes: Long,
    val downloadedAt: Long,
    val source: String,
    val outdated: Boolean = false
)

class DownloadsStore(context: Context) {

    private val appContext = context.applicationContext
    private val store = appContext.downloadsDataStore
    private val key = stringPreferencesKey("downloads_v1")

    val sets: Flow<List<DownloadedSet>> = store.data.map { read(it) }.distinctUntilChanged()

    fun folder(id: Long): File = File(appContext.filesDir, "beatmaps/$id")

    fun audioPath(set: DownloadedSet): File = File(folder(set.beatmap.id), set.audioFile)

    suspend fun find(id: Long): DownloadedSet? = sets.first().firstOrNull { it.beatmap.id == id }

    suspend fun upsert(set: DownloadedSet) = update { list ->
        listOf(set) + list.filterNot { it.beatmap.id == set.beatmap.id }
    }

    suspend fun markOutdated(ids: Set<Long>) = update { list ->
        list.map { it.copy(outdated = it.beatmap.id in ids) }
    }

    suspend fun updateMetadata(fresh: Map<Long, Beatmap>) = update { list ->
        list.map { set ->
            fresh[set.beatmap.id]?.let { set.copy(beatmap = set.beatmap.copy(
                title = it.title.ifBlank { set.beatmap.title },
                artist = it.artist.ifBlank { set.beatmap.artist },
                mapper = it.mapper.ifBlank { set.beatmap.mapper },
                status = it.status.ifBlank { set.beatmap.status }
            )) } ?: set
        }
    }

    suspend fun updateVersions(versions: Map<Long, Long>) = update { list ->
        list.map { set ->
            val version = versions[set.beatmap.id]
            if (version != null && set.beatmap.updatedAt == 0L) set.copy(beatmap = set.beatmap.copy(updatedAt = version)) else set
        }
    }

    suspend fun remove(id: Long) = update { list -> list.filterNot { it.beatmap.id == id } }

    suspend fun clear() = update { emptyList() }

    private suspend fun update(transform: (List<DownloadedSet>) -> List<DownloadedSet>) {
        store.edit { prefs -> prefs[key] = encode(transform(read(prefs))) }
    }

    private fun encode(list: List<DownloadedSet>): String {
        val array = JSONArray()
        list.forEach {
            array.put(
                JSONObject()
                    .put("beatmap", BeatmapJson.encode(it.beatmap))
                    .put("audio", it.audioFile)
                    .put("bg", it.backgroundFile)
                    .put("size", it.sizeBytes)
                    .put("at", it.downloadedAt)
                    .put("source", it.source)
                    .put("outdated", it.outdated)
            )
        }
        return array.toString()
    }

    private fun read(prefs: Preferences): List<DownloadedSet> {
        val json = prefs[key] ?: return emptyList()
        return try {
            val array = JSONArray(json)
            List(array.length()) { index ->
                val o = array.getJSONObject(index)
                DownloadedSet(
                    beatmap = BeatmapJson.decode(o.getJSONObject("beatmap")),
                    audioFile = o.optString("audio"),
                    backgroundFile = o.optString("bg"),
                    sizeBytes = o.optLong("size"),
                    downloadedAt = o.optLong("at"),
                    source = o.optString("source"),
                    outdated = o.optBoolean("outdated")
                )
            }
        } catch (e: JSONException) {
            emptyList()
        }
    }
}
