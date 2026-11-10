package vn.kozu.data.repository

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import vn.kozu.data.local.AccountStore
import vn.kozu.data.remote.osu.OsuApi
import vn.kozu.data.remote.osu.OsuAuth
import vn.kozu.data.remote.osu.UserDto
import vn.kozu.domain.model.OsuProfile

class AccountRepository(
    private val auth: OsuAuth,
    private val api: OsuApi,
    private val store: AccountStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val profile: Flow<OsuProfile?> = store.profile
    val loggedIn: Flow<Boolean> = auth.loggedIn

    val isSupporter: Flow<Boolean> =
        combine(auth.loggedIn, store.profile) { signedIn, profile ->
            signedIn && profile?.isSupporter == true
        }.distinctUntilChanged()

    @Volatile private var lastAutoRefresh = 0L

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val events: SharedFlow<String> = _events.asSharedFlow()

    suspend fun beginLogin(): String? = auth.beginLogin()

    fun onCallback(
        code: String?,
        state: String?,
        error: String?,
    ) {
        scope.launch { handleCallback(code, state, error) }
    }

    suspend fun handleCallback(
        code: String?,
        state: String?,
        error: String?,
    ) {
        if (!error.isNullOrBlank() || code.isNullOrBlank()) {
            _events.tryEmit("osu! sign-in was cancelled")
            return
        }
        auth.completeLogin(code, state).fold(
            onSuccess = {
                refreshProfile().fold(
                    onSuccess = { _events.tryEmit("Signed in as ${it.username}") },
                    onFailure = { _events.tryEmit("Signed in, but the profile could not be loaded") },
                )
            },
            onFailure = { _events.tryEmit(it.message ?: "osu! sign-in failed") },
        )
    }

    suspend fun refreshProfile(forceRankHistory: Boolean = false): Result<OsuProfile> {
        return try {
            val bearer = auth.userBearer() ?: return Result.failure(IllegalStateException("Not signed in"))
            val authorization = "Bearer $bearer"
            val user = api.me(authorization)
            val basicProfile = user.toProfile()
            val cachedProfile = store.profile.first()
            val previousHistory = cachedProfile?.rankHistory.orEmpty()
            val now = System.currentTimeMillis()
            val historyIsFresh =
                !forceRankHistory &&
                    now - store.rankHistoryFetchedAt.first() < RANK_HISTORY_REFRESH_INTERVAL_MS
            var historyRefreshed = false
            val rankHistory =
                if (historyIsFresh) {
                    previousHistory
                } else {
                    try {
                        api
                            .user(authorization, basicProfile.id, basicProfile.mode)
                            .rankHistory
                            ?.data
                            .orEmpty()
                            .filter { it > 0 }
                            .takeLast(90)
                            .also { historyRefreshed = true }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _events.tryEmit("osu! profile updated, but rank history could not be refreshed")
                        previousHistory
                    }
                }
            val profile = basicProfile.copy(rankHistory = rankHistory)
            store.saveProfile(profile)
            if (historyRefreshed) store.markRankHistoryFetched(now)
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Result.failure(IllegalStateException("No connection to osu!", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshIfSignedIn(force: Boolean = false) {
        if (!force && System.currentTimeMillis() - lastAutoRefresh < AUTO_REFRESH_INTERVAL_MS) return
        if (!auth.loggedIn.first()) return
        refreshProfile(forceRankHistory = force).fold(
            onSuccess = { lastAutoRefresh = System.currentTimeMillis() },
            onFailure = {
                _events.tryEmit(it.message ?: "osu! profile could not be refreshed")
            },
        )
    }

    suspend fun logout() {
        auth.logout()
        lastAutoRefresh = 0L
    }

    suspend fun cachedProfile(): OsuProfile? = store.profile.first()

    private fun UserDto.toProfile(): OsuProfile {
        val stats = statistics
        val grades = stats?.gradeCounts
        val level = stats?.level
        return OsuProfile(
            id = id,
            username = username.orEmpty(),
            avatarUrl = avatarUrl.orEmpty(),
            coverUrl = cover?.url ?: cover?.customUrl.orEmpty(),
            countryCode = countryCode ?: country?.code.orEmpty(),
            countryName = country?.name.orEmpty(),
            isSupporter = isSupporter ?: false,
            joinDate = joinDate.orEmpty().take(10),
            followers = followerCount ?: 0,
            mode = playmode ?: "osu",
            globalRank = stats?.globalRank ?: 0,
            countryRank = stats?.countryRank ?: 0,
            pp = stats?.pp ?: 0.0,
            accuracy = stats?.hitAccuracy ?: 0.0,
            playCount = stats?.playCount ?: 0,
            playTimeSeconds = stats?.playTime ?: 0L,
            level = (level?.current ?: 0) + (level?.progress ?: 0) / 100.0,
            rankedScore = stats?.rankedScore ?: 0L,
            gradeSS = (grades?.ss ?: 0) + (grades?.ssh ?: 0),
            gradeS = (grades?.s ?: 0) + (grades?.sh ?: 0),
            gradeA = grades?.a ?: 0,
            gradeSSH = grades?.ssh ?: 0,
            gradeSH = grades?.sh ?: 0,
        )
    }

    private companion object {
        const val AUTO_REFRESH_INTERVAL_MS = 30L * 60L * 1000L
        const val RANK_HISTORY_REFRESH_INTERVAL_MS = 6L * 60L * 60L * 1000L
    }
}
