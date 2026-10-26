package vn.kozu.data.remote.osu

import android.net.Uri
import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import vn.kozu.data.local.AccountStore
import vn.kozu.data.local.OsuSession
import vn.kozu.data.local.SettingsStore
import java.util.UUID

class OsuAuth(
    private val api: OsuApi,
    private val settings: SettingsStore,
    private val account: AccountStore
) {
    private val mutex = Mutex()
    private var clientToken: String? = null
    private var clientOwner: String? = null
    private var clientExpiresAt = 0L

    val loggedIn: Flow<Boolean> = account.session.map { it != null }.distinctUntilChanged()

    suspend fun bearer(force: Boolean = false): String? = mutex.withLock {
        userToken(force) ?: clientCredentials(force)
    }

    suspend fun userBearer(): String? = mutex.withLock { userToken(false) }

    suspend fun clientBearer(force: Boolean = false): String? = mutex.withLock { clientCredentials(force) }

    suspend fun beginLogin(): String? {
        val current = settings.settings.first()
        if (!current.hasCredentials) return null
        val state = UUID.randomUUID().toString()
        account.savePendingState(state)
        return Uri.parse("https://osu.ppy.sh/oauth/authorize").buildUpon()
            .appendQueryParameter("client_id", current.osuClientId)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", "identify public")
            .appendQueryParameter("state", state)
            .build()
            .toString()
    }

    suspend fun completeLogin(code: String, state: String?): Result<Unit> {
        val expected = account.consumePendingState()
        if (expected == null || expected != state) {
            return Result.failure(IllegalStateException("Login request expired, please try again"))
        }
        val current = settings.settings.first()
        return try {
            val token = api.exchangeCode(current.osuClientId, current.osuClientSecret, code, REDIRECT_URI)
            saveToken(token, fallbackRefresh = "")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        mutex.withLock { account.clear() }
    }

    private suspend fun userToken(force: Boolean): String? {
        val session = account.session.first() ?: return null
        val now = System.currentTimeMillis()
        if (!force && now < session.expiresAtMs - 60_000L) return session.accessToken
        if (session.refreshToken.isBlank()) return null
        val current = settings.settings.first()
        if (!current.hasCredentials) return null
        return try {
            val token = api.refreshToken(current.osuClientId, current.osuClientSecret, session.refreshToken)
            saveToken(token, fallbackRefresh = session.refreshToken)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun saveToken(token: TokenDto, fallbackRefresh: String): String {
        val access = token.accessToken ?: error("osu! did not return a token")
        account.saveSession(
            OsuSession(
                accessToken = access,
                refreshToken = token.refreshToken ?: fallbackRefresh,
                expiresAtMs = System.currentTimeMillis() + token.expiresIn * 1000L
            )
        )
        return access
    }

    private suspend fun clientCredentials(force: Boolean): String? {
        val current = settings.settings.first()
        if (!current.hasCredentials) return null
        val owner = "${current.osuClientId}:${current.osuClientSecret}"
        val now = SystemClock.elapsedRealtime()
        val cached = clientToken
        if (!force && cached != null && owner == clientOwner && now < clientExpiresAt) return cached

        val response = api.requestToken(current.osuClientId, current.osuClientSecret)
        val value = response.accessToken ?: error("osu! did not return a token")
        clientToken = value
        clientOwner = owner
        clientExpiresAt = now + (response.expiresIn - 60L).coerceAtLeast(60L) * 1000L
        return value
    }

    companion object {
        const val REDIRECT_URI = "kozu://oauth"
    }
}
