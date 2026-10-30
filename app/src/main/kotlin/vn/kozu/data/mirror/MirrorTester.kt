package vn.kozu.data.mirror

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import vn.kozu.domain.model.Mirror
import vn.kozu.domain.model.Mirrors
import java.io.IOException
import java.util.concurrent.TimeUnit

data class MirrorTestResult(
    val mirrorId: String,
    val reachable: Boolean,
    val supportsRange: Boolean,
    val previewOk: Boolean?,
    val millis: Long,
    val note: String
)

class MirrorTester(client: OkHttpClient) {

    private val probe = client.newBuilder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun test(mirror: Mirror): MirrorTestResult = withContext(Dispatchers.IO) {
        val started = System.currentTimeMillis()
        var reachable = false
        var range = false
        var note = ""
        try {
            val builder = Request.Builder()
                .url(mirror.downloadFor(Mirrors.PROBE_SET_ID))
                .header("Range", "bytes=0-0")
                .header("User-Agent", "Kozu/2.1 (Android; https://github.com/kanzick/Kozu)")
            if (mirror.referer.isNotBlank()) builder.header("Referer", mirror.referer)
            probe.newCall(builder.build()).execute().use { response ->
                when (response.code) {
                    206 -> {
                        reachable = true
                        range = true
                        note = "Download ok, partial streaming supported"
                    }
                    200 -> {
                        reachable = true
                        note = "Download ok, no partial streaming"
                    }
                    else -> note = "HTTP ${response.code}"
                }
            }
        } catch (e: IOException) {
            note = e.message ?: "Unreachable"
        }
        val previewOk = mirror.previewFor(Mirrors.PROBE_SET_ID)?.let {
            StreamResolver.probeAudio(probe, it, mirror.referer)
        }
        MirrorTestResult(mirror.id, reachable, range, previewOk, System.currentTimeMillis() - started, note)
    }

    suspend fun testOfficialPreview(): Boolean = withContext(Dispatchers.IO) {
        StreamResolver.probeAudio(probe, Mirrors.OFFICIAL_PREVIEW.replace("{id}", Mirrors.PROBE_SET_ID.toString()), "")
    }
}
