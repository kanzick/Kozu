package vn.kozu.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import vn.kozu.data.download.DownloadManager
import vn.kozu.data.local.DownloadsStore
import vn.kozu.data.local.FavoritesStore
import vn.kozu.data.local.HistoryStore
import vn.kozu.data.local.PlaylistStore
import vn.kozu.data.local.SettingsStore
import vn.kozu.domain.model.Beatmap

data class SyncReport(val checked: Int, val refreshed: Int, val outdated: Int)

class LibrarySync(
    private val catalog: BeatmapRepository,
    private val favorites: FavoritesStore,
    private val playlists: PlaylistStore,
    private val history: HistoryStore,
    private val downloadsStore: DownloadsStore,
    private val downloads: DownloadManager,
    private val settings: SettingsStore
) {
    private val mutex = Mutex()
    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    suspend fun runIfStale() {
        val last = settings.settings.first().lastSyncAt
        if (System.currentTimeMillis() - last > STALE_AFTER_MS) run()
    }

    suspend fun run(): SyncReport = mutex.withLock {
        _running.value = true
        try {
            val sets = downloadsStore.sets.first()
            val ids = LinkedHashSet<Long>()
            favorites.favorites.first().forEach { ids.add(it.id) }
            playlists.playlists.first().forEach { playlist -> playlist.tracks.forEach { ids.add(it.id) } }
            history.knownIds(80).forEach { ids.add(it) }
            sets.forEach { ids.add(it.beatmap.id) }

            val gate = Semaphore(4)
            val fresh = HashMap<Long, Beatmap>()
            coroutineScope {
                ids.map { id ->
                    async {
                        gate.withPermit { catalog.details(id) }
                    }
                }.awaitAll().forEach { if (it != null) fresh[it.id] = it }
            }

            if (fresh.isNotEmpty()) {
                favorites.updateMetadata(fresh)
                playlists.updateMetadata(fresh)
                history.updateMetadata(fresh)
                downloadsStore.updateMetadata(fresh)
            }

            val versions = HashMap<Long, Long>()
            val outdated = HashMap<Long, Beatmap>()
            for (set in sets) {
                val remote = fresh[set.beatmap.id] ?: continue
                if (remote.updatedAt <= 0L) continue
                if (set.beatmap.updatedAt <= 0L) versions[set.beatmap.id] = remote.updatedAt
                else if (remote.updatedAt > set.beatmap.updatedAt) outdated[set.beatmap.id] = remote
            }
            if (versions.isNotEmpty()) downloadsStore.updateVersions(versions)
            downloadsStore.markOutdated(outdated.keys)

            if (outdated.isNotEmpty() && settings.settings.first().autoUpdateDownloads) {
                outdated.values.forEach { downloads.enqueue(it, force = true) }
            }

            settings.setLastSync(System.currentTimeMillis())
            SyncReport(ids.size, fresh.size, outdated.size)
        } finally {
            _running.value = false
        }
    }

    private companion object {
        const val STALE_AFTER_MS = 6L * 60L * 60L * 1000L
    }
}
