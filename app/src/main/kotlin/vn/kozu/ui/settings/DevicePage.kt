package vn.kozu.ui.settings

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import android.os.StatFs
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.kozu.data.update.UpdateChecker
import java.io.File

internal data class DeviceEntry(
    val icon: ImageVector,
    val title: String,
    val value: String,
    val description: String? = null
)

internal data class DeviceReport(
    val playback: List<DeviceEntry>,
    val decoders: List<DeviceEntry>,
    val memory: List<DeviceEntry>,
    val storage: List<DeviceEntry>,
    val app: List<DeviceEntry>
)

@Composable
fun DevicePage(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val report by produceState<DeviceReport?>(null, refresh) {
        value = withContext(Dispatchers.Default) { readDeviceReport(context) }
    }

    SettingsScaffold(title = "Device Capabilities", onBack = onBack) {
        SettingsSection(null) {
            action(Setting.REFRESH_DEVICE, "Refresh") { refresh++ }
        }
        report?.let { data ->
            SettingsSection("Playback path") { data.playback.forEach { info(it.icon, it.title, it.value, it.description) } }
            SettingsSection("Audio decoders") { data.decoders.forEach { info(it.icon, it.title, it.value, it.description) } }
            SettingsSection("Memory") { data.memory.forEach { info(it.icon, it.title, it.value, it.description) } }
            SettingsSection("Storage") { data.storage.forEach { info(it.icon, it.title, it.value, it.description) } }
            SettingsSection("App & device") { data.app.forEach { info(it.icon, it.title, it.value, it.description) } }
        }
    }
}

private fun yesNo(value: Boolean) = if (value) "Yes" else "No"

internal fun readDeviceReport(context: Context): DeviceReport {
    val appContext = context.applicationContext
    return DeviceReport(
        playback = playbackEntries(appContext),
        decoders = decoderEntries(),
        memory = memoryEntries(appContext),
        storage = storageEntries(appContext),
        app = appEntries(appContext)
    )
}

private fun playbackEntries(context: Context): List<DeviceEntry> {
    val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val packages = context.packageManager
    val sampleRate = audio.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
    val frames = audio.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
    val latencyMs = if (sampleRate != null && frames != null) {
        val rate = sampleRate.toIntOrNull()
        val count = frames.toIntOrNull()
        if (rate != null && rate > 0 && count != null) "%.1f ms per buffer".format(count * 1000f / rate) else null
    } else {
        null
    }
    val floatSupported = try {
        AudioTrack.getMinBufferSize(48_000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT) > 0
    } catch (_: Exception) {
        false
    }
    val devices = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
    val current = devices.minByOrNull { devicePriority(it.type) }
    return listOf(
        DeviceEntry(Icons.Rounded.Speed, "Output sample rate", sampleRate?.let { "$it Hz" } ?: "Unknown"),
        DeviceEntry(Icons.Rounded.Tune, "Frames per buffer", frames ?: "Unknown", latencyMs),
        DeviceEntry(
            Icons.Rounded.Bolt,
            "Low-latency audio",
            yesNo(packages.hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY))
        ),
        DeviceEntry(
            Icons.Rounded.Bolt,
            "Pro audio",
            yesNo(packages.hasSystemFeature(PackageManager.FEATURE_AUDIO_PRO))
        ),
        DeviceEntry(Icons.Rounded.Memory, "Hardware offload", offloadSupport(audio), "Offloaded decoding of compressed audio."),
        DeviceEntry(Icons.Rounded.HighQuality, "Float PCM output", yesNo(floatSupported), "Needed for Hi-Fi mode."),
        DeviceEntry(
            Icons.Rounded.Headphones,
            "Current output",
            current?.let { deviceName(it) } ?: "None",
            devices.takeIf { it.size > 1 }?.joinToString(", ") { deviceName(it) }
        )
    )
}

private fun offloadSupport(audio: AudioManager): String {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return "Unknown"
    val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()
    val supported = listOf(AudioFormat.ENCODING_MP3, AudioFormat.ENCODING_AAC_LC).filter { encoding ->
        try {
            val format = AudioFormat.Builder()
                .setEncoding(encoding)
                .setSampleRate(44_100)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build()
            AudioManager.isOffloadedPlaybackSupported(format, attributes)
        } catch (_: Exception) {
            false
        }
    }
    return if (supported.isEmpty()) "No" else "Yes"
}

private fun devicePriority(type: Int): Int = when (type) {
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> 0
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> 1
    AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_ACCESSORY -> 2
    AudioDeviceInfo.TYPE_HDMI -> 3
    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> 4
    AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> 5
    else -> 6
}

private fun deviceName(device: AudioDeviceInfo): String {
    val type = when (device.type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth call"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired headphones"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired headset"
        AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB audio"
        AudioDeviceInfo.TYPE_HDMI -> "HDMI"
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Speaker"
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Earpiece"
        else -> "Audio output"
    }
    val product = device.productName?.toString().orEmpty()
    return if (product.isBlank() || product == Build.MODEL) type else "$type ($product)"
}

private fun decoderEntries(): List<DeviceEntry> {
    val decoders = try {
        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
            .filter { !it.isEncoder && it.supportedTypes.any { type -> type.startsWith("audio/", ignoreCase = true) } }
    } catch (_: Exception) {
        emptyList()
    }
    val hardware = decoders.count { isHardware(it) }
    val software = decoders.size - hardware
    val formats = listOf(
        "MP3" to "audio/mpeg",
        "AAC" to "audio/mp4a-latm",
        "Opus" to "audio/opus",
        "Vorbis" to "audio/vorbis",
        "FLAC" to "audio/flac"
    )
    return buildList {
        add(
            DeviceEntry(
                Icons.Rounded.GraphicEq,
                "Audio decoders",
                "${decoders.size}",
                "$hardware hardware, $software software"
            )
        )
        formats.forEach { (label, mime) ->
            val matching = decoders.filter { info -> info.supportedTypes.any { it.equals(mime, ignoreCase = true) } }
            val value = when {
                matching.isEmpty() -> "No"
                matching.any { isHardware(it) } -> "Hardware"
                else -> "Software"
            }
            add(DeviceEntry(Icons.Rounded.MusicNote, "$label decoding", value, matching.firstOrNull()?.name))
        }
    }
}

private fun isHardware(info: MediaCodecInfo): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    info.isHardwareAccelerated && !info.isSoftwareOnly
} else {
    val name = info.name.lowercase()
    !name.startsWith("omx.google.") && !name.startsWith("c2.android.") && !name.contains(".sw.")
}

private fun memoryEntries(context: Context): List<DeviceEntry> {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val info = ActivityManager.MemoryInfo().also { manager.getMemoryInfo(it) }
    val runtime = Runtime.getRuntime()
    return listOf(
        DeviceEntry(Icons.Rounded.Memory, "Total RAM", formatBytes(info.totalMem)),
        DeviceEntry(
            Icons.Rounded.Memory,
            "Available RAM",
            formatBytes(info.availMem),
            if (info.lowMemory) "The system reports low memory." else "Low-memory threshold ${formatBytes(info.threshold)}."
        ),
        DeviceEntry(
            Icons.Rounded.Memory,
            "App heap",
            "${formatBytes(runtime.totalMemory() - runtime.freeMemory())} used",
            "Limit ${manager.memoryClass} MB, up to ${manager.largeMemoryClass} MB with large heap."
        )
    )
}

private fun storageEntries(context: Context): List<DeviceEntry> {
    val stats = try {
        StatFs(context.filesDir.absolutePath)
    } catch (_: Exception) {
        null
    }
    val total = stats?.totalBytes ?: 0L
    val free = stats?.availableBytes ?: 0L
    val cache = directorySize(context.cacheDir)
    val files = directorySize(context.filesDir)
    return listOf(
        DeviceEntry(Icons.Rounded.Storage, "Free storage", formatBytes(free), "of ${formatBytes(total)} total"),
        DeviceEntry(Icons.Rounded.Storage, "App data", formatBytes(files), "Downloaded songs and saved files."),
        DeviceEntry(Icons.Rounded.Storage, "App cache", formatBytes(cache), "Streams, artwork and temporary files.")
    )
}

private fun directorySize(directory: File): Long = try {
    directory.walkTopDown().filter { it.isFile }.sumOf { it.length() }
} catch (_: Exception) {
    0L
}

private fun appEntries(context: Context): List<DeviceEntry> {
    val code = try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode.toString() else "${info.versionCode}"
    } catch (_: Exception) {
        "?"
    }
    return listOf(
        DeviceEntry(Icons.Rounded.Info, "Kozu", UpdateChecker.installedVersion(context), "Build $code"),
        DeviceEntry(Icons.Rounded.Android, "Android", Build.VERSION.RELEASE, "API ${Build.VERSION.SDK_INT}"),
        DeviceEntry(Icons.Rounded.DeveloperBoard, "Device", Build.MODEL, Build.MANUFACTURER),
        DeviceEntry(
            Icons.Rounded.DeveloperBoard,
            "CPU architecture",
            Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown",
            Build.SUPPORTED_ABIS.joinToString(", ")
        )
    )
}
