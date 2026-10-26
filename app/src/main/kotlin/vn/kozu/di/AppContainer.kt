package vn.kozu.di

import android.content.Context
import vn.kozu.data.download.DownloadManager
import vn.kozu.data.local.AccountStore
import vn.kozu.data.local.DownloadsStore
import vn.kozu.data.local.FavoritesStore
import vn.kozu.data.local.HistoryStore
import vn.kozu.data.local.PlaylistStore
import vn.kozu.data.local.SettingsStore
import vn.kozu.data.mirror.MirrorTester
import vn.kozu.data.mirror.StreamResolver
import vn.kozu.data.remote.osu.OsuApiFactory
import vn.kozu.data.remote.osu.OsuAuth
import vn.kozu.data.repository.AccountRepository
import vn.kozu.data.repository.BeatmapRepository
import vn.kozu.data.repository.DiscoverFeed
import vn.kozu.data.repository.LibrarySync
import vn.kozu.player.PlaybackController

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

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
    val discover = DiscoverFeed(catalog, settings, accountStore)

    val streams = StreamResolver(appContext, httpClient, downloadsStore, settings)
    val downloads = DownloadManager(appContext, httpClient, settings, downloadsStore, auth)
    val mirrorTester = MirrorTester(httpClient)
    val sync = LibrarySync(catalog, favorites, playlists, history, downloadsStore, downloads, settings)

    val playback: PlaybackController by lazy { PlaybackController(appContext, history, streams) }
}
