package vn.kozu.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Authenticator
import okhttp3.ConnectionPool
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import vn.kozu.domain.model.ProxySettings
import vn.kozu.domain.model.ProxyType
import java.io.IOException
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

sealed interface ProxyTestResult {
    data class Success(val latencyMs: Long, val httpCode: Int) : ProxyTestResult
    data class Failure(val message: String) : ProxyTestResult
}

object ProxyController {

    @Volatile
    private var current = ProxySettings()

    @Volatile
    private var candidate: ProxySettings? = null

    private val ready = CountDownLatch(1)

    val connectionPool = ConnectionPool()

    val selector: ProxySelector = object : ProxySelector() {
        override fun select(uri: URI?): List<Proxy> {
            awaitReady()
            return selectFor(current, uri)
        }

        override fun connectFailed(uri: URI?, address: SocketAddress?, failure: IOException?) = Unit
    }

    val authenticator = Authenticator { _, response -> authenticate(current, response) }

    init {
        runCatching { java.net.Authenticator.setDefault(SocksAuthenticator) }
    }

    fun update(settings: ProxySettings) {
        val normalized = settings.normalized()
        val changed = normalized != current
        current = normalized
        ready.countDown()
        if (changed) connectionPool.evictAll()
    }

    fun apply(builder: OkHttpClient.Builder): OkHttpClient.Builder = builder
        .connectionPool(connectionPool)
        .proxySelector(selector)
        .proxyAuthenticator(authenticator)

    suspend fun test(base: OkHttpClient, proposed: ProxySettings, url: String = TEST_URL): ProxyTestResult =
        withContext(Dispatchers.IO) {
            val settings = proposed.normalized()
            if (!settings.isActive) return@withContext ProxyTestResult.Failure("Enter a valid host and port first")
            candidate = settings
            val client = base.newBuilder()
                .connectionPool(ConnectionPool(0, 1, TimeUnit.SECONDS))
                .proxySelector(object : ProxySelector() {
                    override fun select(uri: URI?): List<Proxy> = selectFor(settings, uri)
                    override fun connectFailed(uri: URI?, address: SocketAddress?, failure: IOException?) = Unit
                })
                .proxyAuthenticator { _, response -> authenticate(settings, response) }
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(6, TimeUnit.SECONDS)
                .callTimeout(10, TimeUnit.SECONDS)
                .build()
            val started = System.nanoTime()
            try {
                client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    val elapsed = (System.nanoTime() - started) / 1_000_000L
                    if (response.code == 407) {
                        ProxyTestResult.Failure("The proxy rejected the credentials")
                    } else {
                        ProxyTestResult.Success(elapsed, response.code)
                    }
                }
            } catch (e: Exception) {
                ProxyTestResult.Failure(describe(e))
            } finally {
                client.connectionPool.evictAll()
                candidate = null
            }
        }

    fun describe(error: Throwable): String {
        val message = error.message?.takeIf { it.isNotBlank() }
        return when (error) {
            is java.net.SocketTimeoutException -> "Timed out while reaching the proxy"
            is java.net.UnknownHostException -> "Could not resolve the proxy host"
            is java.net.ConnectException -> "Connection refused by the proxy"
            else -> message ?: error.javaClass.simpleName
        }
    }

    private fun awaitReady() {
        if (ready.count > 0L) runCatching { ready.await(1500, TimeUnit.MILLISECONDS) }
    }

    private fun selectFor(settings: ProxySettings, uri: URI?): List<Proxy> {
        if (!settings.isActive) return NO_PROXY
        val host = uri?.host.orEmpty()
        if (host.isEmpty() || host == "localhost" || host == "127.0.0.1" || host == "::1" || host == "[::1]") {
            return NO_PROXY
        }
        val type = if (settings.type == ProxyType.SOCKS5) Proxy.Type.SOCKS else Proxy.Type.HTTP
        return listOf(Proxy(type, InetSocketAddress.createUnresolved(settings.host, settings.port)))
    }

    private fun authenticate(settings: ProxySettings, response: okhttp3.Response): Request? {
        if (response.code != 407 || !settings.isActive || settings.type != ProxyType.HTTP) return null
        if (settings.username.isEmpty()) return null
        if (response.request.header("Proxy-Authorization") != null) return null
        return response.request.newBuilder()
            .header("Proxy-Authorization", Credentials.basic(settings.username, settings.password))
            .build()
    }

    private fun ProxySettings.normalized() = copy(
        host = host.trim(),
        username = username.trim()
    )

    private object SocksAuthenticator : java.net.Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication? {
            val port = requestingPort
            val protocol = requestingProtocol.orEmpty()
            val prompt = requestingPrompt.orEmpty()
            val socks = protocol.contains("SOCKS", ignoreCase = true) || prompt.contains("SOCKS", ignoreCase = true)
            if (!socks) return null
            val match = listOfNotNull(candidate, current).firstOrNull {
                it.isActive && it.type == ProxyType.SOCKS5 && it.username.isNotEmpty() &&
                    it.port == port
            } ?: return null
            return PasswordAuthentication(match.username, match.password.toCharArray())
        }
    }

    private const val TEST_URL = "https://osu.ppy.sh/"
    private val NO_PROXY = listOf(Proxy.NO_PROXY)
}
