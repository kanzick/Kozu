package vn.kozu.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import vn.kozu.data.local.AccountStore
import vn.kozu.data.remote.osu.OsuApi
import vn.kozu.data.remote.osu.OsuAuth
import vn.kozu.data.remote.osu.UserDto
import vn.kozu.domain.model.OsuProfile
import java.io.IOException

class AccountRepository(
    private val auth: OsuAuth,
    private val api: OsuApi,
    private val store: AccountStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val profile: Flow<OsuProfile?> = store.profile
    val loggedIn: Flow<Boolean> = auth.loggedIn

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val events: SharedFlow<String> = _events.asSharedFlow()

    suspend fun beginLogin(): String? = auth.beginLogin()

    fun onCallback(code: String?, state: String?, error: String?) {
        scope.launch { handleCallback(code, state, error) }
    }

    suspend fun handleCallback(code: String?, state: String?, error: String?) {
        if (!error.isNullOrBlank() || code.isNullOrBlank()) {
            _events.tryEmit("osu! sign-in was cancelled")
            return
        }
        auth.completeLogin(code, state).fold(
            onSuccess = {
                refreshProfile().fold(
                    onSuccess = { _events.tryEmit("Signed in as ${it.username}") },
                    onFailure = { _events.tryEmit("Signed in, but the profile could not be loaded") }
                )
            },
            onFailure = { _events.tryEmit(it.message ?: "osu! sign-in failed") }
        )
    }

    suspend fun refreshProfile(): Result<OsuProfile> {
        return try {
            val bearer = auth.userBearer() ?: return Result.failure(IllegalStateException("Not signed in"))
            val user = api.me("Bearer $bearer")
            val profile = user.toProfile()
            store.saveProfile(profile)
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Result.failure(IllegalStateException("No connection to osu!", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        auth.logout()
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
            gradeA = grades?.a ?: 0
        )
    }
}
