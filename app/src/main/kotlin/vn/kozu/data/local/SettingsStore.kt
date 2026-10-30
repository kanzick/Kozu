package vn.kozu.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import vn.kozu.domain.model.AccentColor
import vn.kozu.domain.model.AppSettings
import vn.kozu.domain.model.Mirror
import vn.kozu.domain.model.ModeFilter
import vn.kozu.domain.model.StatusFilter
import vn.kozu.domain.model.StreamMode
import vn.kozu.domain.model.ThemeMode

class SettingsStore(context: Context) {

    private val appContext = context.applicationContext
    private val store = appContext.kozuDataStore
    private val prefsCache = appContext.getSharedPreferences("kozu_theme_sync", Context.MODE_PRIVATE)

    val cachedThemeMode: ThemeMode get() = initialThemeMode(appContext)
    val cachedAmoled: Boolean get() = prefsCache.getBoolean("amoled", false)

    private object Keys {
        val theme = stringPreferencesKey("theme_mode")
        val amoled = booleanPreferencesKey("amoled")
        val artworkColors = booleanPreferencesKey("artwork_colors")
        val accent = stringPreferencesKey("accent")
        val displayName = stringPreferencesKey("display_name")
        val clientId = stringPreferencesKey("osu_client_id")
        val clientSecret = stringPreferencesKey("osu_client_secret")
        val recentSearches = stringPreferencesKey("recent_searches")
        val streamMode = stringPreferencesKey("stream_mode")
        val wifiOnlyStreaming = booleanPreferencesKey("wifi_only_streaming")
        val streamCacheMb = intPreferencesKey("stream_cache_mb")
        val skipSilence = booleanPreferencesKey("skip_silence")
        val disabledMirrors = stringPreferencesKey("disabled_mirrors")
        val customMirrors = stringPreferencesKey("custom_mirrors")
        val keepFull = booleanPreferencesKey("keep_full_beatmap")
        val wifiOnlyDownloads = booleanPreferencesKey("wifi_only_downloads")
        val autoUpdate = booleanPreferencesKey("auto_update_downloads")
        val explicit = booleanPreferencesKey("show_explicit")
        val modeFilter = stringPreferencesKey("mode_filter")
        val statusFilter = stringPreferencesKey("status_filter")
        val lastSync = longPreferencesKey("last_sync_at")
    }

    val settings: Flow<AppSettings> = store.data.map { prefs ->
        AppSettings(
            themeMode = ThemeMode.fromKey(prefs[Keys.theme]),
            amoled = prefs[Keys.amoled] ?: false,
            artworkColors = prefs[Keys.artworkColors] ?: true,
            accent = AccentColor.fromKey(prefs[Keys.accent]),
            displayName = prefs[Keys.displayName].orEmpty(),
            osuClientId = prefs[Keys.clientId].orEmpty(),
            osuClientSecret = prefs[Keys.clientSecret].orEmpty(),
            streamMode = StreamMode.fromKey(prefs[Keys.streamMode]),
            wifiOnlyStreaming = prefs[Keys.wifiOnlyStreaming] ?: false,
            streamCacheMb = prefs[Keys.streamCacheMb] ?: 512,
            skipSilence = prefs[Keys.skipSilence] ?: false,
            disabledMirrors = prefs[Keys.disabledMirrors].orEmpty().split(',').filter { it.isNotBlank() }.toSet(),
            customMirrors = decodeMirrors(prefs[Keys.customMirrors]),
            keepFullBeatmap = prefs[Keys.keepFull] ?: true,
            wifiOnlyDownloads = prefs[Keys.wifiOnlyDownloads] ?: false,
            autoUpdateDownloads = prefs[Keys.autoUpdate] ?: true,
            showExplicit = false,
            modeFilter = ModeFilter.fromKey(prefs[Keys.modeFilter]),
            statusFilter = StatusFilter.fromKey(prefs[Keys.statusFilter]),
            lastSyncAt = prefs[Keys.lastSync] ?: 0L
        )
    }.distinctUntilChanged()

    val recentSearches: Flow<List<String>> = store.data.map { prefs ->
        prefs[Keys.recentSearches].orEmpty().split('\n').filter { it.isNotBlank() }
    }.distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) {
        withContext(Dispatchers.IO) {
            check(prefsCache.edit().putString("theme_mode", mode.key).commit()) {
                "Could not persist the startup theme preference"
            }
        }
        store.edit { it[Keys.theme] = mode.key }
    }
    suspend fun setAmoled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            check(prefsCache.edit().putBoolean("amoled", enabled).commit()) {
                "Could not persist the startup AMOLED preference"
            }
        }
        store.edit { it[Keys.amoled] = enabled }
    }
    suspend fun setArtworkColors(enabled: Boolean) = store.edit { it[Keys.artworkColors] = enabled }
    suspend fun setAccent(accent: AccentColor) = store.edit { it[Keys.accent] = accent.key }
    suspend fun setDisplayName(name: String) = store.edit { it[Keys.displayName] = name.trim().take(24) }
    suspend fun setStreamMode(mode: StreamMode) = store.edit { it[Keys.streamMode] = mode.key }
    suspend fun setWifiOnlyStreaming(enabled: Boolean) = store.edit { it[Keys.wifiOnlyStreaming] = enabled }
    suspend fun setStreamCacheMb(mb: Int) = store.edit { it[Keys.streamCacheMb] = mb }
    suspend fun setSkipSilence(enabled: Boolean) = store.edit { it[Keys.skipSilence] = enabled }
    suspend fun setKeepFullBeatmap(enabled: Boolean) = store.edit { it[Keys.keepFull] = enabled }
    suspend fun setWifiOnlyDownloads(enabled: Boolean) = store.edit { it[Keys.wifiOnlyDownloads] = enabled }
    suspend fun setAutoUpdateDownloads(enabled: Boolean) = store.edit { it[Keys.autoUpdate] = enabled }
    @Suppress("UNUSED_PARAMETER")
    suspend fun setShowExplicit(enabled: Boolean) = store.edit { it[Keys.explicit] = false }
    suspend fun setModeFilter(filter: ModeFilter) = store.edit { it[Keys.modeFilter] = filter.key }
    suspend fun setStatusFilter(filter: StatusFilter) = store.edit { it[Keys.statusFilter] = filter.key }
    suspend fun setLastSync(timeMillis: Long) = store.edit { it[Keys.lastSync] = timeMillis }

    suspend fun setOsuCredentials(clientId: String, clientSecret: String) = store.edit {
        it[Keys.clientId] = clientId.trim()
        it[Keys.clientSecret] = clientSecret.trim()
    }

    suspend fun setMirrorEnabled(id: String, enabled: Boolean) = store.edit { prefs ->
        val current = prefs[Keys.disabledMirrors].orEmpty().split(',').filter { it.isNotBlank() }.toMutableSet()
        if (enabled) current.remove(id) else current.add(id)
        prefs[Keys.disabledMirrors] = current.joinToString(",")
    }

    suspend fun addCustomMirror(mirror: Mirror) = store.edit { prefs ->
        val current = decodeMirrors(prefs[Keys.customMirrors]).filterNot { it.id == mirror.id }
        prefs[Keys.customMirrors] = encodeMirrors(current + mirror)
    }

    suspend fun removeCustomMirror(id: String) = store.edit { prefs ->
        prefs[Keys.customMirrors] = encodeMirrors(decodeMirrors(prefs[Keys.customMirrors]).filterNot { it.id == id })
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

    private fun encodeMirrors(list: List<Mirror>): String {
        val array = JSONArray()
        list.forEach {
            array.put(
                JSONObject()
                    .put("id", it.id)
                    .put("name", it.name)
                    .put("download", it.downloadUrl)
                    .put("preview", it.previewUrl)
                    .put("referer", it.referer)
            )
        }
        return array.toString()
    }

    private fun decodeMirrors(json: String?): List<Mirror> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            List(array.length()) { index ->
                val obj = array.getJSONObject(index)
                Mirror(
                    id = obj.getString("id"),
                    name = obj.optString("name"),
                    downloadUrl = obj.optString("download"),
                    previewUrl = obj.optString("preview"),
                    referer = obj.optString("referer"),
                    builtIn = false
                )
            }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    companion object {
        fun initialThemeMode(context: Context): ThemeMode =
            ThemeMode.fromKey(
                context.applicationContext
                    .getSharedPreferences("kozu_theme_sync", Context.MODE_PRIVATE)
                    .getString("theme_mode", "system")
            )
    }
}
