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
        .put("preview", beatmap.hasPreview)

    fun decode(obj: JSONObject): Beatmap = Beatmap(
        id = obj.getLong("id"),
        title = obj.optString("title"),
        artist = obj.optString("artist"),
        mapper = obj.optString("mapper"),
        status = obj.optString("status"),
        hasPreview = obj.optBoolean("preview", true)
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
}
