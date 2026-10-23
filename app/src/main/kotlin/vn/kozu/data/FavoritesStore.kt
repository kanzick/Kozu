package vn.kozu.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.kozuDataStore by preferencesDataStore(name = "kozu_preferences")

class FavoritesStore(private val context: Context) {
    private val favoritesKey = stringSetPreferencesKey("favorite_beatmap_ids")

    val favoriteIds: Flow<Set<Long>> = context.kozuDataStore.data.map { preferences ->
        preferences[favoritesKey]
            .orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .toSet()
    }

    suspend fun toggleFavorite(id: Long) {
        context.kozuDataStore.edit { preferences ->
            val current = preferences[favoritesKey].orEmpty().toMutableSet()
            val value = id.toString()
            if (value in current) {
                current.remove(value)
            } else {
                current.add(value)
            }
            preferences[favoritesKey] = current
        }
    }
}

