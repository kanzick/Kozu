package vn.kozu

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import vn.kozu.ui.KozuApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        preferHighRefreshRate()
        if (savedInstanceState == null) handleOAuthIntent(intent)
        setContent { KozuApp() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthIntent(intent)
    }

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
