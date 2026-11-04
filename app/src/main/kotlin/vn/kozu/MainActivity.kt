package vn.kozu

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
        if (savedInstanceState == null) handleOAuthIntent(intent)
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
        handleOAuthIntent(intent)
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
