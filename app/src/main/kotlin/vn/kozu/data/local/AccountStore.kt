package vn.kozu.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import vn.kozu.domain.model.OsuProfile

data class OsuSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtMs: Long,
)

class AccountStore(
    context: Context,
) {
    private val store = context.applicationContext.accountDataStore
    private val accessKey = stringPreferencesKey("access_token")
    private val refreshKey = stringPreferencesKey("refresh_token")
    private val expiresKey = longPreferencesKey("expires_at")
    private val rankHistoryFetchedKey = longPreferencesKey("rank_history_fetched_at")
    private val profileKey = stringPreferencesKey("profile")
    private val stateKey = stringPreferencesKey("pending_state")

    val session: Flow<OsuSession?> =
        store.data
            .map { prefs ->
                val access = prefs[accessKey]
                if (access.isNullOrBlank()) {
                    null
                } else {
                    OsuSession(access, prefs[refreshKey].orEmpty(), prefs[expiresKey] ?: 0L)
                }
            }.distinctUntilChanged()

    val profile: Flow<OsuProfile?> = store.data.map { decodeProfile(it[profileKey]) }.distinctUntilChanged()
    val rankHistoryFetchedAt: Flow<Long> =
        store.data.map { it[rankHistoryFetchedKey] ?: 0L }.distinctUntilChanged()

    suspend fun currentSignedInProfile(): OsuProfile? {
        val prefs = store.data.first()
        if (prefs[accessKey].isNullOrBlank()) return null
        return decodeProfile(prefs[profileKey])
    }

    suspend fun saveSession(session: OsuSession, clearProfile: Boolean = false) {
        store.edit {
            if (clearProfile) {
                it.remove(profileKey)
                it.remove(rankHistoryFetchedKey)
            }
            it[accessKey] = session.accessToken
            it[refreshKey] = session.refreshToken
            it[expiresKey] = session.expiresAtMs
        }
    }

    suspend fun saveProfileForSession(profile: OsuProfile, accessToken: String): Boolean {
        var saved = false
        store.edit {
            if (it[accessKey] == accessToken) {
                it[profileKey] = encodeProfile(profile)
                saved = true
            }
        }
        return saved
    }

    suspend fun markRankHistoryFetched(atMs: Long) {
        store.edit { it[rankHistoryFetchedKey] = atMs }
    }

    suspend fun savePendingState(state: String) {
        store.edit { it[stateKey] = state }
    }

    suspend fun consumePendingState(): String? {
        var value: String? = null
        store.edit {
            value = it[stateKey]
            it.remove(stateKey)
        }
        return value
    }

    suspend fun clear() {
        store.edit {
            it.remove(accessKey)
            it.remove(refreshKey)
            it.remove(expiresKey)
            it.remove(profileKey)
            it.remove(rankHistoryFetchedKey)
            it.remove(stateKey)
        }
    }

    private fun encodeProfile(p: OsuProfile): String {
        val rankHistory = JSONArray()
        p.rankHistory.takeLast(90).forEach(rankHistory::put)
        return JSONObject()
            .put("id", p.id)
            .put("username", p.username)
            .put("avatar", p.avatarUrl)
            .put("cover", p.coverUrl)
            .put("cc", p.countryCode)
            .put("country", p.countryName)
            .put("supporter", p.isSupporter)
            .put("joined", p.joinDate)
            .put("followers", p.followers)
            .put("mode", p.mode)
            .put("grank", p.globalRank)
            .put("crank", p.countryRank)
            .put("pp", p.pp)
            .put("acc", p.accuracy)
            .put("plays", p.playCount)
            .put("time", p.playTimeSeconds)
            .put("level", p.level)
            .put("score", p.rankedScore)
            .put("ss", p.gradeSS)
            .put("ssh", p.gradeSSH)
            .put("s", p.gradeS)
            .put("sh", p.gradeSH)
            .put("a", p.gradeA)
            .put("rankHistory", rankHistory)
            .toString()
    }

    private fun decodeProfile(json: String?): OsuProfile? {
        if (json.isNullOrBlank()) return null
        return try {
            val o = JSONObject(json)
            OsuProfile(
                id = o.getLong("id"),
                username = o.optString("username"),
                avatarUrl = o.optString("avatar"),
                coverUrl = o.optString("cover"),
                countryCode = o.optString("cc"),
                countryName = o.optString("country"),
                isSupporter = o.optBoolean("supporter"),
                joinDate = o.optString("joined"),
                followers = o.optInt("followers"),
                mode = o.optString("mode", "osu"),
                globalRank = o.optInt("grank"),
                countryRank = o.optInt("crank"),
                pp = o.optDouble("pp"),
                accuracy = o.optDouble("acc"),
                playCount = o.optInt("plays"),
                playTimeSeconds = o.optLong("time"),
                level = o.optDouble("level"),
                rankedScore = o.optLong("score"),
                gradeSS = o.optInt("ss"),
                gradeSSH = o.optInt("ssh"),
                gradeS = o.optInt("s"),
                gradeSH = o.optInt("sh"),
                gradeA = o.optInt("a"),
                rankHistory =
                    o
                        .optJSONArray("rankHistory")
                        ?.let { history ->
                            List(history.length()) { history.optInt(it) }.filter { it > 0 }.takeLast(90)
                        }.orEmpty(),
            )
        } catch (e: JSONException) {
            null
        }
    }
}
