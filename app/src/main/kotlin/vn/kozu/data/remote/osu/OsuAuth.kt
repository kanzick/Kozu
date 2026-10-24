package vn.kozu.data.remote.osu

import android.os.SystemClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import vn.kozu.data.local.SettingsStore


class OsuAuth(
    private val api: OsuApi,
    private val settings: SettingsStore) {
    private val mutex = Mutex()
    private var token: String? = null
    private var tokenOwner: String? = null
    private var expiresAtMs = 0L


    suspend fun bearer(force: Boolean = false): String? {
        return mutex.withLock {
            val current = settings.settings.first()
            if (!current.hasCredentials) return@withLock null

            val owner = "${current.osuClientId}:${current.osuClientSecret}"
            val now = SystemClock.elapsedRealtime()
            val cached = token
            if (!force && cached != null && owner == tokenOwner && now < expiresAtMs) {
                return@withLock cached
            }

            val response = api.requestToken(current.osuClientId, current.osuClientSecret)
            val value = response.accessToken ?: error("osu! did not return a token")
            token = value
            tokenOwner = owner
            expiresAtMs = now + (response.expiresIn - 60L).coerceAtLeast(60L) * 1000L
            value
        }
    }
}
