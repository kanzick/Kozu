package vn.kozu.data.news

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Tracks whether the newest osu! news post has been opened on this device. */
object NewsReadState {
    private const val PREFS_NAME = "kozu_news_read_state"
    private const val KEY_LATEST_POST_ID = "latest_post_id"
    private const val KEY_LAST_SEEN_POST_ID = "last_seen_post_id"
    private const val KEY_LATEST_POST_DATE = "latest_post_date"

    private var initialized = false
    private var latestPostId = 0L
    private var latestPostDate = 0L
    private var lastSeenPostId = 0L

    var hasUnread by mutableStateOf(false)
        private set

    @Synchronized
    fun initialize(context: Context) {
        if (initialized) return

        val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        latestPostId = preferences.getLong(KEY_LATEST_POST_ID, 0L)
        latestPostDate = preferences.getLong(KEY_LATEST_POST_DATE, 0L)
        lastSeenPostId = preferences.getLong(KEY_LAST_SEEN_POST_ID, 0L)
        hasUnread = latestPostId > 0L && latestPostId != lastSeenPostId
        initialized = true
    }

    @Synchronized
    fun updateLatest(context: Context, posts: List<NewsPost>) {
        initialize(context)
        val newest = posts.maxWithOrNull(compareBy<NewsPost> { it.dateMillis }.thenBy { it.id }) ?: return
        // Two feed requests can finish out of order (startup check and opening the page).
        // Never let an older response replace a newer notification target.
        if (
            latestPostId != 0L &&
            (newest.dateMillis < latestPostDate ||
                (newest.dateMillis == latestPostDate && newest.id < latestPostId))
        ) return

        latestPostId = newest.id
        latestPostDate = newest.dateMillis
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LATEST_POST_ID, latestPostId)
            .putLong(KEY_LATEST_POST_DATE, latestPostDate)
            .apply()
        hasUnread = latestPostId > 0L && latestPostId != lastSeenPostId
    }

    @Synchronized
    fun markSeen(context: Context, postId: Long) {
        initialize(context)
        // Opening an older article must not clear the notification for a newer one.
        if (postId <= 0L || postId != latestPostId) return

        lastSeenPostId = postId
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_SEEN_POST_ID, lastSeenPostId)
            .apply()
        hasUnread = false
    }
}
