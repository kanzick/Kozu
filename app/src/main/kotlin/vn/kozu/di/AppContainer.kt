package vn.kozu.di

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import vn.kozu.data.backup.BackupManager
import vn.kozu.data.download.DownloadManager
import vn.kozu.data.local.AccountStore
import vn.kozu.data.local.DownloadsStore
import vn.kozu.data.local.FavoritesStore
import vn.kozu.data.local.HistoryStore
import vn.kozu.data.local.PlaylistStore
import vn.kozu.data.local.SettingsStore
import vn.kozu.data.mirror.MirrorTester
import vn.kozu.data.mirror.StreamResolver
import vn.kozu.data.remote.ProxyController
import vn.kozu.data.remote.osu.OsuApiFactory
import vn.kozu.data.remote.osu.OsuAuth
import vn.kozu.data.repository.AccountRepository
import vn.kozu.data.repository.BeatmapRepository
import vn.kozu.data.repository.DiscoverFeed
import vn.kozu.data.repository.LibrarySync
import vn.kozu.data.storage.CacheCleaner
import vn.kozu.data.update.UpdateChecker
import vn.kozu.player.PlaybackController

class AppContainer(
    context: Context,
) {
    val appContext = context.applicationContext

    val settings = SettingsStore(appContext)
    val favorites = FavoritesStore(appContext)
    val playlists = PlaylistStore(appContext)
    val history = HistoryStore(appContext)
    val downloadsStore = DownloadsStore(appContext)
    private val accountStore = AccountStore(appContext)

    val httpClient = OsuApiFactory.client()
    private val api = OsuApiFactory.create(httpClient)
    val auth = OsuAuth(api, settings, accountStore)

    val accounts = AccountRepository(auth, api, accountStore)
    val catalog = BeatmapRepository(api, auth, settings, httpClient)
    val discover = DiscoverFeed(catalog, settings, auth)

    val streams = StreamResolver(appContext, httpClient, downloadsStore, settings)
    val downloads =
        DownloadManager(
            context = appContext,
            client = httpClient,
            settings = settings,
            store = downloadsStore,
            auth = auth,
            history = history,
        )
    val mirrorTester = MirrorTester(httpClient)

    val sync =
        LibrarySync(
            catalog,
            favorites,
            playlists,
            history,
            downloadsStore,
            downloads,
            settings,
        )

    val playback: PlaybackController by lazy {
        PlaybackController(appContext, history, streams, catalog, settings.settings)
    }

    val updates = UpdateChecker(appContext, httpClient)
    val backup = BackupManager(settings, favorites, playlists, history)
    val cacheCleaner = CacheCleaner(appContext, streams, settings)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            settings.settings
                .map { it.proxy }
                .distinctUntilChanged()
                .collect { ProxyController.update(it) }
        }
        scope.launch { runCatching { cacheCleaner.cleanIfNeeded() } }
    }
}
