package vn.kozu

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import vn.kozu.data.local.SettingsStore
import vn.kozu.domain.model.ThemeMode
import vn.kozu.ui.KozuApp

/**
 * Main activity of the Kozu application.
 * Manages Compose UI initialization, edge-to-edge layout, high refresh rate configuration,
 * and handles incoming OAuth deep links from osu!.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        applyLaunchTheme()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        preferHighRefreshRate()
        if (savedInstanceState == null) handleOAuthIntent(intent)
        setContent { KozuApp() }
    }

    private fun applyLaunchTheme() {
        val systemDark =
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val dark = when (SettingsStore.initialThemeMode(this)) {
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

    /**
     * Handles deep link OAuth redirects from osu! authentication (kozu://oauth).
     */
    private fun handleOAuthIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "kozu" || data.host != "oauth") return
        val container = (application as KozuApplication).container
        container.accounts.onCallback(
            data.getQueryParameter("code"),
            data.getQueryParameter("state"),
            data.getQueryParameter("error")
        )
        intent.data = null
    }

    /**
     * Configures the window to prefer the highest display refresh rate available (e.g. 90Hz/120Hz+).
     */
    private fun preferHighRefreshRate() {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        }
        val highest = display?.supportedModes?.maxOfOrNull { it.refreshRate } ?: return
        if (highest > 0f) {
            window.attributes = window.attributes.apply { preferredRefreshRate = highest }
        }
    }
}
