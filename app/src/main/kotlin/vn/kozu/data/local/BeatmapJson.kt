package vn.kozu.data.local

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import vn.kozu.domain.model.Beatmap

internal object BeatmapJson {

    fun encode(beatmap: Beatmap): JSONObject = JSONObject()
        .put("id", beatmap.id)
        .put("title", beatmap.title)
        .put("artist", beatmap.artist)
        .put("mapper", beatmap.mapper)
        .put("status", beatmap.status)
        .put("updated", beatmap.updatedAt)

    fun decode(obj: JSONObject): Beatmap = Beatmap(
        id = obj.getLong("id"),
        title = obj.optString("title"),
        artist = obj.optString("artist"),
        mapper = obj.optString("mapper"),
        status = obj.optString("status"),
        updatedAt = obj.optLong("updated", 0L)
    )

    fun encodeList(list: List<Beatmap>): JSONArray {
        val array = JSONArray()
        list.forEach { array.put(encode(it)) }
        return array
    }

    fun decodeList(array: JSONArray): List<Beatmap> =
        List(array.length()) { decode(array.getJSONObject(it)) }

    fun decodeList(json: String?): List<Beatmap> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            decodeList(JSONArray(json))
        } catch (e: JSONException) {
            emptyList()
        }
    }

    fun merge(old: Beatmap, fresh: Beatmap): Beatmap = old.copy(
        title = fresh.title.ifBlank { old.title },
        artist = fresh.artist.ifBlank { old.artist },
        mapper = fresh.mapper.ifBlank { old.mapper },
        status = fresh.status.ifBlank { old.status },
        updatedAt = if (fresh.updatedAt > 0L) fresh.updatedAt else old.updatedAt
    )
}
