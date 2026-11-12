package vn.kozu.game.mania.gamestage

import android.content.Context
import android.util.Log
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import vn.kozu.BuildConfig
import vn.kozu.game.mania.gamestage.scoring.GameResult
import vn.kozu.game.mania.gamestage.settings.GameSettings
import vn.kozu.game.mania.previewstage.model.DifficultyRecord
import vn.kozu.game.mania.previewstage.model.ScoreRecord
import vn.kozu.game.mania.previewstage.storage.ScoreDatabase

data class GlobalScore(val place: Long, val userId: String, val score: ScoreRecord)
data class GlobalLeaderboard(
    val userId: String,
    val rows: List<GlobalScore>,
    val pendingCount: Int = 0,
    val profileWarning: Boolean = false,
)
data class RemoteSubmissionResult(
    val isPersonalBest: Boolean,
    val place: Long?,
    val bestScore: Long?,
    val totalPlayers: Long,
)

internal class ScoreSync(context: Context) {
    private val preferences = context.getSharedPreferences("score_sync_session", Context.MODE_PRIVATE)
    private val http = OkHttpClient()
    private val lock = Mutex()
    private val url = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
    private val apiKey = BuildConfig.SUPABASE_ANON_KEY.trim()
    val configured: Boolean get() = url.isNotEmpty() && apiKey.isNotEmpty()

    @Volatile var lastSubmission: RemoteSubmissionResult? = null
        private set
    @Volatile var profileWarning: Boolean = false
        private set

    suspend fun sync(db: ScoreDatabase, playerName: String) = lock.withLock {
        requireConfigured()
        val pending = db.pendingSubmissions()
        if (pending.isEmpty() && !preferences.contains("user_id")) return@withLock
        val session = session()
        syncName(session, playerName)
        for ((id, payload) in pending) {
            val response = try {
                JSONObject(request("POST", "/rest/v1/rpc/submit_score", payload, session.accessToken))
            } catch (e: HttpFailure) {
                if (e.status !in 400..499 || e.status == 401 || e.status == 429) throw e
                Log.w(TAG, "Score submission rejected; keeping it for retry", e)
                continue
            }
            lastSubmission = RemoteSubmissionResult(
                isPersonalBest = response.getBoolean("is_personal_best"),
                place = response.nullableLong("place"),
                bestScore = response.nullableLong("best_score"),
                totalPlayers = response.getLong("total_players"),
            )
            db.removeSubmission(id)
        }
    }

    suspend fun leaderboard(songId: Long, difficulty: String): GlobalLeaderboard = lock.withLock {
        requireConfigured()
        val session = session()
        val payload = JSONObject()
            .put("p_song_id", songId)
            .put("p_difficulty", difficulty)
            .put("p_limit", 50)
            .put("p_offset", 0)
        val response = JSONArray(request("POST", "/rest/v1/rpc/get_leaderboard", payload.toString(), session.accessToken))
        val rows = (0 until response.length()).map { index ->
            val row = response.getJSONObject(index)
            GlobalScore(
                place = row.getLong("place"),
                userId = row.getString("user_id"),
                score = ScoreRecord(
                    playerName = row.getString("display_name"),
                    songId = songId,
                    difficulty = difficulty,
                    score = row.getLong("score"),
                    accuracy = row.getDouble("accuracy"),
                    rank = row.getString("grade"),
                    trueCombo = row.nullableLong("true_combo")?.toInt(),
                    maxCombo = row.nullableLong("max_combo")?.toInt(),
                    playedAt = Instant.parse(row.getString("achieved_at")).toEpochMilli(),
                ),
            )
        }
        GlobalLeaderboard(session.userId, rows)
    }

    private fun syncName(session: Session, playerName: String) {
        val name = playerName.trim()
        if (name.codePointCount(0, name.length) !in 2..24) {
            profileWarning = true
            Log.w(TAG, "Display name must contain 2 to 24 characters")
            return
        }
        // The default name is shared by installations; retain the unique generated profile name.
        if ((name == GameSettings.DEFAULT_PLAYER_NAME && !preferences.contains("synced_name")) ||
            preferences.getString("synced_name", null) == name
        ) {
            profileWarning = false
            return
        }
        try {
            val updated = request(
                "PATCH",
                "/rest/v1/profiles?id=eq.${session.userId}",
                JSONObject().put("display_name", name).toString(),
                session.accessToken,
            )
            if (JSONArray(updated).length() != 1) throw IOException("Supabase profile was not updated")
            if (!preferences.edit().putString("synced_name", name).commit()) {
                throw IOException("Could not save synced display name")
            }
            profileWarning = false
        } catch (e: IOException) {
            profileWarning = true
            Log.w(TAG, "Could not update Supabase display name", e)
        }
    }

    private fun session(): Session {
        val access = preferences.getString("access_token", null)
        val refresh = preferences.getString("refresh_token", null)
        val userId = preferences.getString("user_id", null)
        if (access != null && refresh != null && userId != null) {
            if (preferences.getLong("expires_at", 0) > System.currentTimeMillis() + 60_000) {
                return Session(access, userId)
            }
            return saveSession(JSONObject(request(
                "POST", "/auth/v1/token?grant_type=refresh_token",
                JSONObject().put("refresh_token", refresh).toString(), null,
            )))
        }
        if (access != null || refresh != null || userId != null) {
            throw IOException("Incomplete Supabase session; refusing to replace the existing identity")
        }
        return signInAnonymously()
    }

    private fun signInAnonymously(): Session =
        saveSession(JSONObject(request("POST", "/auth/v1/signup", "{}", null)))

    private fun saveSession(json: JSONObject): Session {
        val access = json.getString("access_token")
        val refresh = json.getString("refresh_token")
        val userId = json.getJSONObject("user").getString("id")
        val expiresAt = System.currentTimeMillis() + json.getLong("expires_in") * 1000
        if (!preferences.edit().putString("access_token", access).putString("refresh_token", refresh)
                .putString("user_id", userId).putLong("expires_at", expiresAt).commit()
        ) throw IOException("Could not save Supabase session")
        return Session(access, userId)
    }

    private fun requireConfigured() {
        if (!configured) throw IOException("Supabase URL and publishable/anon key are not configured")
        if (url.toHttpUrlOrNull()?.isHttps != true) throw IOException("Supabase URL must use HTTPS")
    }

    private fun request(method: String, path: String, body: String, accessToken: String?): String {
        val builder = Request.Builder()
            .url(url + path)
            .header("apikey", apiKey)
            .method(method, body.toRequestBody(JSON))
        if (accessToken != null) builder.header("Authorization", "Bearer $accessToken")
        if (method == "PATCH") builder.header("Prefer", "return=representation")
        val request = builder.build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw HttpFailure(response.code)
            return response.body.string()
        }
    }

    private data class Session(val accessToken: String, val userId: String)
    private class HttpFailure(val status: Int) : IOException("Supabase request failed (HTTP $status)")

    companion object {
        private const val TAG = "ScoreSync"
        private val JSON = "application/json".toMediaType()

        fun submission(id: String, score: ScoreRecord, difficulty: DifficultyRecord, result: GameResult, speed: Double): String =
            JSONObject()
                .put("p_client_play_id", id)
                .put("p_song_id", score.songId)
                .put("p_difficulty", score.difficulty)
                .put("p_score", score.score)
                .put("p_accuracy", score.accuracy)
                .put("p_grade", score.rank)
                .put("p_true_combo", score.trueCombo ?: JSONObject.NULL)
                .put("p_max_combo", score.maxCombo ?: JSONObject.NULL)
                .put("p_played_at", Instant.ofEpochMilli(score.playedAt).toString())
                .put("p_song", JSONObject()
                    .put("title", difficulty.title).put("title_unicode", difficulty.titleUnicode)
                    .put("artist", difficulty.artist).put("artist_unicode", difficulty.artistUnicode)
                    .put("creator", difficulty.creator))
                .put("p_chart", JSONObject()
                    .put("beatmap_id", difficulty.beatmapId).put("keys", difficulty.circleSize.toInt())
                    .put("star_rating", difficulty.starRating).put("overall_difficulty", difficulty.overallDifficulty)
                    .put("note_count", result.totalNotes))
                .put("p_details", JSONObject()
                    .put("judgements", JSONObject().put("perfect", result.perfect).put("great", result.great)
                        .put("good", result.good).put("ok", result.ok).put("meh", result.meh).put("miss", result.miss))
                    .put("scroll_speed", speed).put("app_version", BuildConfig.VERSION_NAME))
                .toString()
    }
}

private fun JSONObject.nullableLong(name: String): Long? =
    if (isNull(name)) null else getLong(name)
