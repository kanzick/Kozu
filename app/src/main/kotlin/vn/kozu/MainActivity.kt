package vn.kozu

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
        setContent { KozuApp() }
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
