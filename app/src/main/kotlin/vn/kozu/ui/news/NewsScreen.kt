package vn.kozu.ui.news

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import vn.kozu.ui.components.LocalOverlayBottomInset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import vn.kozu.ui.components.CenteredHeader
import vn.kozu.ui.components.HeaderAction
import vn.kozu.ui.i18n.kozuUiString

private const val NEWS_URL = "https://osu.ppy.sh/home"

/**
 * osu!'s own news, exactly as the website shows it. It is the real page in a WebView, so every
 * post, link, comment thread and login behaves as it does in a browser. Back steps through the
 * pages visited before leaving.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NewsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var web by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var currentUrl by remember { mutableStateOf(NEWS_URL) }

    BackHandler {
        val view = web
        if (view != null && view.canGoBack()) view.goBack() else onBack()
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                // Keep touches on this page from reaching what is underneath.
                .pointerInput(Unit) { detectTapGestures { } },
    ) {
        CenteredHeader(
            title = title.ifBlank { kozuUiString("osu! news") },
            leading = {
                HeaderAction(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = kozuUiString("Back"),
                    onClick = {
                        val view = web
                        if (view != null && view.canGoBack()) view.goBack() else onBack()
                    },
                    startPadding = 0.dp,
                )
            },
            trailing = {
                Row {
                    HeaderAction(
                        icon = Icons.Rounded.Refresh,
                        contentDescription = kozuUiString("Refresh"),
                        onClick = { web?.reload() },
                        startPadding = 0.dp,
                    )
                    HeaderAction(
                        icon = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = kozuUiString("Open in browser"),
                        onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))) } },
                        startPadding = 8.dp,
                    )
                }
            },
        )
        Box(Modifier.fillMaxWidth().height(3.dp)) {
            if (progress in 1..99) {
                LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
            }
        }
        AndroidView(
            modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = LocalOverlayBottomInset.current),
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        builtInZoomControls = false
                        displayZoomControls = false
                        mediaPlaybackRequiresUserGesture = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    }
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webViewClient =
                        object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest,
                            ): Boolean {
                                val uri = request.url
                                val host = uri.host.orEmpty()
                                val inApp =
                                    uri.scheme == "https" &&
                                        (host == "ppy.sh" || host.endsWith(".ppy.sh") || host.endsWith("osu.ppy.sh"))
                                if (inApp) return false
                                // Anything off osu! (YouTube, Twitter, Discord…) opens in the browser.
                                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                                return true
                            }

                            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                url?.let { currentUrl = it }
                                canGoBack = view.canGoBack()
                            }

                            override fun onPageFinished(view: WebView, url: String?) {
                                url?.let { currentUrl = it }
                                canGoBack = view.canGoBack()
                            }
                        }
                    webChromeClient =
                        object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView, newProgress: Int) {
                                progress = newProgress
                            }

                            override fun onReceivedTitle(view: WebView, received: String?) {
                                title = received.orEmpty().substringBefore(" | ").ifBlank { "osu!" }
                            }
                        }
                    loadUrl(NEWS_URL)
                    web = this
                }
            },
            onRelease = {
                web = null
                it.stopLoading()
                it.destroy()
            },
        )
    }
}
