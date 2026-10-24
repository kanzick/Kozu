package vn.kozu.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import vn.kozu.domain.model.AppSettings
import vn.kozu.domain.model.ThemeMode

class SettingsStore(context: Context) {

    private val store = context.applicationContext.kozuDataStore

    private object Keys {
        val theme = stringPreferencesKey("theme_mode")
        val amoled = booleanPreferencesKey("amoled")
        val artworkColors = booleanPreferencesKey("artwork_colors")
        val displayName = stringPreferencesKey("display_name")
        val clientId = stringPreferencesKey("osu_client_id")
        val clientSecret = stringPreferencesKey("osu_client_secret")
        val recentSearches = stringPreferencesKey("recent_searches")
    }

    val settings: Flow<AppSettings> = store.data.map { prefs ->
        AppSettings(
            themeMode = ThemeMode.fromKey(prefs[Keys.theme]),
            amoled = prefs[Keys.amoled] ?: false,
            artworkColors = prefs[Keys.artworkColors] ?: true,
            displayName = prefs[Keys.displayName].orEmpty(),
            osuClientId = prefs[Keys.clientId].orEmpty(),
            osuClientSecret = prefs[Keys.clientSecret].orEmpty()
        )
    }

    val recentSearches: Flow<List<String>> = store.data.map { prefs ->
        prefs[Keys.recentSearches].orEmpty().split('\n').filter { it.isNotBlank() }
    }

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[Keys.theme] = mode.key }
    suspend fun setAmoled(enabled: Boolean) = store.edit { it[Keys.amoled] = enabled }
    suspend fun setArtworkColors(enabled: Boolean) = store.edit { it[Keys.artworkColors] = enabled }
    suspend fun setDisplayName(name: String) = store.edit { it[Keys.displayName] = name.trim().take(24) }
    suspend fun setOsuCredentials(clientId: String, clientSecret: String) = store.edit {
        it[Keys.clientId] = clientId.trim()
        it[Keys.clientSecret] = clientSecret.trim()
    }

    suspend fun addRecentSearch(query: String) {
        val value = query.trim().replace('\n', ' ')
        if (value.isBlank()) return
        store.edit { prefs ->
            val current = prefs[Keys.recentSearches].orEmpty().split('\n').filter { it.isNotBlank() }
            val updated = (listOf(value) + current.filterNot { it.equals(value, ignoreCase = true) }).take(8)
            prefs[Keys.recentSearches] = updated.joinToString("\n")
        }
    }

    suspend fun removeRecentSearch(query: String) = store.edit { prefs ->
        val updated = prefs[Keys.recentSearches].orEmpty().split('\n').filter { it.isNotBlank() && it != query }
        prefs[Keys.recentSearches] = updated.joinToString("\n")
    }

    suspend fun clearRecentSearches() = store.edit { it.remove(Keys.recentSearches) }
}
