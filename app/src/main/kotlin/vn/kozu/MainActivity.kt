package vn.kozu

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import vn.kozu.data.local.SettingsStore
import vn.kozu.domain.model.ThemeMode
import vn.kozu.ui.KozuApp
import vn.kozu.util.AppLocaleManager

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocaleManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyLaunchTheme()
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        super.onCreate(savedInstanceState)
        applyRefreshRate((application as KozuApplication).container.settings.cachedHighRefreshRate)
        if (savedInstanceState == null) handleIncomingIntent(intent)
        setContent { KozuApp() }
    }

    private fun applyLaunchTheme() {
        val systemDark =
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val dark =
            when (SettingsStore.initialThemeMode(this)) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
        setTheme(if (dark) R.style.Theme_Kozu_Dark else R.style.Theme_Kozu_Light)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (!handleArchiveIntent(intent)) handleOAuthIntent(intent)
    }

    private fun handleArchiveIntent(intent: Intent?): Boolean {
        if (intent == null) return false
        val uris = LinkedHashSet<Uri>()
        when (intent.action) {
            Intent.ACTION_SEND -> {
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let(uris::add)
                collectClipUris(intent, uris)
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let(uris::addAll)
                collectClipUris(intent, uris)
            }

            Intent.ACTION_VIEW -> {
                val data = intent.data
                if (data != null && (data.scheme == "content" || data.scheme == "file")) uris += data
            }
        }
        if (uris.isEmpty()) return false
        (application as KozuApplication).container.lazerImporter.importUris(uris.toList())
        intent.removeExtra(Intent.EXTRA_STREAM)
        intent.clipData = null
        intent.data = null
        intent.action = Intent.ACTION_MAIN
        return true
    }

    private fun collectClipUris(
        intent: Intent,
        into: MutableSet<Uri>,
    ) {
        val clip = intent.clipData ?: return
        for (index in 0 until clip.itemCount) clip.getItemAt(index).uri?.let(into::add)
    }

    private fun handleOAuthIntent(intent: Intent?) {
        val data = intent?.data ?: return
        val container = (application as KozuApplication).container
        if (data.scheme == "https" && data.host == "osu.ppy.sh") {
            val id = beatmapSetIdFrom(data)
            intent.data = null
            if (id != null) openBeatmapSet(id)
            return
        }
        if (data.scheme != "kozu" || data.host != "oauth") return
        container.accounts.onCallback(
            data.getQueryParameter("code"),
            data.getQueryParameter("state"),
            data.getQueryParameter("error"),
        )
        intent.data = null
    }

    private fun beatmapSetIdFrom(uri: android.net.Uri): Long? {
        val segments = uri.pathSegments
        if (segments.size < 2) return null
        if (segments[0] != "beatmapsets" && segments[0] != "s") return null
        return segments[1].takeWhile { it.isDigit() }.toLongOrNull()?.takeIf { it > 0L }
    }

    private fun openBeatmapSet(id: Long) {
        val container = (application as KozuApplication).container
        lifecycleScope.launch {
            val beatmap =
                try {
                    container.catalog.details(id)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                } ?: vn.kozu.domain.model
                    .Beatmap(id = id, title = "Beatmap $id", artist = "")
            container.playback.playQueue(listOf(beatmap), 0)
        }
    }

    fun applyRefreshRate(enabled: Boolean) {
        val display =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            }
        val highest = display?.supportedModes?.maxOfOrNull { it.refreshRate } ?: return
        val target = if (enabled && highest > 0f) highest else 0f
        if (window.attributes.preferredRefreshRate != target) {
            window.attributes = window.attributes.apply { preferredRefreshRate = target }
        }
    }
}
