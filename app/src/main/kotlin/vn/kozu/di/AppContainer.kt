package vn.kozu.di

import android.content.Context
import vn.kozu.data.local.FavoritesStore
import vn.kozu.data.local.HistoryStore
import vn.kozu.data.local.PlaylistStore
import vn.kozu.data.local.SettingsStore
import vn.kozu.data.remote.osu.OsuApiFactory
import vn.kozu.data.remote.osu.OsuAuth
import vn.kozu.data.repository.BeatmapRepository
import vn.kozu.data.repository.DiscoverFeed
import vn.kozu.player.PlaybackController

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val settings = SettingsStore(appContext)
    val favorites = FavoritesStore(appContext)
    val playlists = PlaylistStore(appContext)
    val history = HistoryStore(appContext)
    private val api = OsuApiFactory.create()
    private val auth = OsuAuth(api, settings)
    val catalog = BeatmapRepository(api, auth)
    val discover = DiscoverFeed(catalog, settings)
    val playback: PlaybackController by lazy { PlaybackController(appContext, history) }
}
