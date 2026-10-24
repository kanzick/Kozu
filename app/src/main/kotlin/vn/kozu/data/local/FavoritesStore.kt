package vn.kozu.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import vn.kozu.domain.model.Beatmap

class FavoritesStore(context: Context) {

    private val store = context.applicationContext.kozuDataStore
    private val key = stringPreferencesKey("favorites_v2")

    val favorites: Flow<List<Beatmap>> = store.data.map { read(it) }
    val favoriteIds: Flow<Set<Long>> = favorites.map { list -> list.map { it.id }.toSet() }

    suspend fun toggle(beatmap: Beatmap) {
        store.edit { prefs ->
            val list = read(prefs).toMutableList()
            val removed = list.removeAll { it.id == beatmap.id }
            if (!removed) list.add(0, beatmap)
            prefs[key] = BeatmapJson.encodeList(list).toString()
        }
    }

    suspend fun clear() {
        store.edit { it.remove(key) }
    }

    private fun read(prefs: Preferences): List<Beatmap> = BeatmapJson.decodeList(prefs[key])
}
