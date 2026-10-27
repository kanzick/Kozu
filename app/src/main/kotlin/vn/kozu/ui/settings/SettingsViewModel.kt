package vn.kozu.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.kozu.data.local.DownloadedSet
import vn.kozu.data.mirror.MirrorTestResult
import vn.kozu.di.AppContainer
import vn.kozu.domain.model.AccentColor
import vn.kozu.domain.model.AppSettings
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.Mirror
import vn.kozu.domain.model.Mirrors
import vn.kozu.domain.model.ModeFilter
import vn.kozu.domain.model.OsuProfile
import vn.kozu.domain.model.StatusFilter
import vn.kozu.domain.model.StreamMode
import vn.kozu.domain.model.ThemeMode
import vn.kozu.ui.containerFactory
import vn.kozu.util.dayKey
import java.util.UUID

sealed interface VerifyState {
    data object Idle : VerifyState
    data object Checking : VerifyState
    data object Success : VerifyState
    data class Failure(val message: String) : VerifyState
}

enum class StatsRange(val days: Int, val label: String) {
    WEEK(7, "7 days"),
    MONTH(30, "30 days"),
    QUARTER(90, "90 days")
}

data class StatsUi(
    val totalMs: Long = 0L,
    val plays: Int = 0,
    val activeDays: Int = 0,
    val topTracks: List<Pair<Beatmap, Int>> = emptyList(),
    val topArtists: List<Pair<String, Int>> = emptyList(),
    val topMappers: List<Pair<String, Int>> = emptyList(),
    val dayMs: List<Long> = emptyList()
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val settings: StateFlow<AppSettings> = container.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val profile: StateFlow<OsuProfile?> = container.accounts.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val loggedIn: StateFlow<Boolean> = container.accounts.loggedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val downloads: StateFlow<List<DownloadedSet>> = container.downloadsStore.sets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val syncing: StateFlow<Boolean> = container.sync.running

    private val _verify = MutableStateFlow<VerifyState>(VerifyState.Idle)
    val verify: StateFlow<VerifyState> = _verify.asStateFlow()

    private val _profileLoading = MutableStateFlow(false)
    val profileLoading: StateFlow<Boolean> = _profileLoading.asStateFlow()

    private val _mirrorResults = MutableStateFlow<Map<String, MirrorTestResult>>(emptyMap())
    val mirrorResults: StateFlow<Map<String, MirrorTestResult>> = _mirrorResults.asStateFlow()

    private val _testing = MutableStateFlow(false)
    val testing: StateFlow<Boolean> = _testing.asStateFlow()

    private val _officialPreview = MutableStateFlow<Boolean?>(null)
    val officialPreview: StateFlow<Boolean?> = _officialPreview.asStateFlow()

    private val _cacheBytes = MutableStateFlow(container.streams.cacheBytes())
    val cacheBytes: StateFlow<Long> = _cacheBytes.asStateFlow()

    private val _range = MutableStateFlow(StatsRange.WEEK)
    val range: StateFlow<StatsRange> = _range.asStateFlow()

    val stats: StateFlow<StatsUi> = combine(container.history.history, _range) { history, range ->
        val now = System.currentTimeMillis()
        val dayMillis = 24L * 60L * 60L * 1000L
        val cutoff = now - range.days * dayMillis
        val events = history.events.filter { it.at >= cutoff }
        val perTrack = events.groupingBy { it.beatmapId }.eachCount()
        val tracks = perTrack.entries.sortedByDescending { it.value }
            .mapNotNull { entry -> history.tracks[entry.key]?.let { it to entry.value } }
        val artists = HashMap<String, Int>()
        val mappers = HashMap<String, Int>()
        tracks.forEach { (beatmap, count) ->
            if (beatmap.artist.isNotBlank()) artists.merge(beatmap.artist, count, { a, b -> a + b })
            if (beatmap.mapper.isNotBlank()) mappers.merge(beatmap.mapper, count, { a, b -> a + b })
        }
        val daily = List(range.days) { index -> history.dailyMs[dayKey(now - (range.days - 1 - index) * dayMillis)] ?: 0L }
        StatsUi(
            totalMs = daily.sum(),
            plays = events.size,
            activeDays = daily.count { it > 0L },
            topTracks = tracks.take(10),
            topArtists = artists.entries.sortedByDescending { it.value }.take(8).map { it.key to it.value },
            topMappers = mappers.entries.sortedByDescending { it.value }.take(8).map { it.key to it.value },
            dayMs = daily.takeLast(minOf(range.days, 30))
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUi())

    fun setRange(value: StatsRange) = _range.update { value }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { container.settings.setThemeMode(mode) }
    fun setAmoled(enabled: Boolean) = viewModelScope.launch { container.settings.setAmoled(enabled) }
    fun setArtworkColors(enabled: Boolean) = viewModelScope.launch { container.settings.setArtworkColors(enabled) }
    fun setAccent(accent: AccentColor) = viewModelScope.launch { container.settings.setAccent(accent) }
    fun setDisplayName(name: String) = viewModelScope.launch { container.settings.setDisplayName(name) }
    fun setStreamMode(mode: StreamMode) = viewModelScope.launch { container.settings.setStreamMode(mode) }
    fun setWifiOnlyStreaming(enabled: Boolean) = viewModelScope.launch { container.settings.setWifiOnlyStreaming(enabled) }
    fun setSkipSilence(enabled: Boolean) = viewModelScope.launch { container.settings.setSkipSilence(enabled) }
    fun setStreamCacheMb(mb: Int) = viewModelScope.launch { container.settings.setStreamCacheMb(mb) }
    fun setOfficialDownload(enabled: Boolean) = viewModelScope.launch { container.settings.setOfficialDownload(enabled) }
    fun setKeepFull(enabled: Boolean) = viewModelScope.launch { container.settings.setKeepFullBeatmap(enabled) }
    fun setWifiOnlyDownloads(enabled: Boolean) = viewModelScope.launch { container.settings.setWifiOnlyDownloads(enabled) }
    fun setAutoUpdate(enabled: Boolean) = viewModelScope.launch { container.settings.setAutoUpdateDownloads(enabled) }
    fun setShowExplicit(enabled: Boolean) = viewModelScope.launch { container.settings.setShowExplicit(enabled) }
    fun setModeFilter(filter: ModeFilter) = viewModelScope.launch { container.settings.setModeFilter(filter) }
    fun setStatusFilter(filter: StatusFilter) = viewModelScope.launch { container.settings.setStatusFilter(filter) }

    fun clearHistory() = viewModelScope.launch { container.history.clear() }
    fun clearSearches() = viewModelScope.launch { container.settings.clearRecentSearches() }

    fun clearStreamCache() {
        container.streams.clearCache()
        _cacheBytes.value = container.streams.cacheBytes()
    }

    fun refreshCacheSize() {
        _cacheBytes.value = container.streams.cacheBytes()
    }

    fun deleteAllDownloads() = viewModelScope.launch { container.downloads.deleteAll() }

    fun syncNow() = viewModelScope.launch { container.sync.run() }

    fun saveAndVerify(clientId: String, clientSecret: String) {
        viewModelScope.launch {
            _verify.value = VerifyState.Checking
            container.settings.setOsuCredentials(clientId, clientSecret)
            val result = container.catalog.verifyCredentials()
            _verify.value = result.fold(
                onSuccess = { VerifyState.Success },
                onFailure = { VerifyState.Failure(it.message ?: "Could not connect") }
            )
        }
    }

    fun clearCredentials() {
        viewModelScope.launch {
            container.settings.setOsuCredentials("", "")
            _verify.value = VerifyState.Idle
        }
    }

    fun login(open: (String) -> Unit) {
        viewModelScope.launch {
            val url = container.accounts.beginLogin()
            if (url != null) open(url)
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            _profileLoading.value = true
            container.accounts.refreshProfile()
            _profileLoading.value = false
        }
    }

    fun logout() = viewModelScope.launch { container.accounts.logout() }

    fun testMirrors() {
        viewModelScope.launch {
            _testing.value = true
            _mirrorResults.value = emptyMap()
            _officialPreview.value = container.mirrorTester.testOfficialPreview()
            for (mirror in Mirrors.builtIn + settings.value.customMirrors) {
                val result = container.mirrorTester.test(mirror)
                _mirrorResults.update { it + (mirror.id to result) }
            }
            _testing.value = false
        }
    }

    fun setMirrorEnabled(id: String, enabled: Boolean) =
        viewModelScope.launch { container.settings.setMirrorEnabled(id, enabled) }

    fun addMirror(name: String, downloadUrl: String, previewUrl: String) = viewModelScope.launch {
        container.settings.addCustomMirror(
            Mirror(
                id = "custom-" + UUID.randomUUID().toString().take(8),
                name = name.trim(),
                downloadUrl = downloadUrl.trim(),
                previewUrl = previewUrl.trim(),
                builtIn = false
            )
        )
    }

    fun removeMirror(id: String) = viewModelScope.launch { container.settings.removeCustomMirror(id) }

    companion object {
        val Factory = containerFactory { SettingsViewModel(it) }
    }
}
