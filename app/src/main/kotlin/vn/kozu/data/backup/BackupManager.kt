package vn.kozu.data.backup

import android.content.Context
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import vn.kozu.data.local.BeatmapJson
import vn.kozu.data.local.DownloadedSet
import vn.kozu.data.local.DownloadsStore
import vn.kozu.data.local.FavoritesStore
import vn.kozu.data.local.History
import vn.kozu.data.local.HistoryStore
import vn.kozu.data.local.PlayEvent
import vn.kozu.data.local.PlaylistStore
import vn.kozu.data.local.SettingsStore
import vn.kozu.domain.model.Beatmap
import vn.kozu.domain.model.Playlist
import vn.kozu.game.mania.previewstage.PreviewStageConfig
import vn.kozu.game.mania.previewstage.PreviewStageModule

const val MAX_BACKUP_ARCHIVE_BYTES = 8L * 1024L * 1024L * 1024L

enum class BackupSection(
    val key: String,
    val label: String,
    val description: String,
) {
    SETTINGS("settings", "Settings", "Preferences and appearance. Credentials are never included."),
    FAVORITES("favorites", "Favorites", "Songs you have liked."),
    PLAYLISTS("playlists", "Playlists", "Your playlists and their songs."),
    HISTORY("history", "Listening history", "Plays and daily listening time."),
    MUSIC_DOWNLOADS("music_downloads", "Downloaded music", "Offline beatmaps, audio, artwork and library entries."),
    GAME_DATA("game_data", "Game data", "Imported charts, collections, scores and downloaded maps. Restoring replaces existing game data."),
}

class BackupException(
    message: String,
) : Exception(message)

data class BackupInfo(
    val version: Int,
    val createdAt: Long,
    val counts: Map<BackupSection, Int>,
) {
    val sections: Set<BackupSection> get() = counts.keys
}

class BackupManager(
    context: Context,
    private val settings: SettingsStore,
    private val favorites: FavoritesStore,
    private val playlists: PlaylistStore,
    private val history: HistoryStore,
    private val downloads: DownloadsStore,
) {
    private val appContext = context.applicationContext
    private val game: PreviewStageModule by lazy {
        PreviewStageModule.get(appContext, PreviewStageConfig(rootDir = PreviewStageModule.defaultRootDir(appContext)))
    }

    suspend fun create(
        sections: Set<BackupSection>,
        output: OutputStream,
    ) = withContext(Dispatchers.IO) {
        createArchive(sections, output)
    }

    private suspend fun createArchive(
        sections: Set<BackupSection>,
        output: OutputStream,
    ) {
        if (sections.isEmpty()) throw BackupException("Select at least one section")

        val downloadedSets = if (BackupSection.MUSIC_DOWNLOADS in sections) {
            downloads.sets.first().filter { downloads.folder(it.beatmap.id).isDirectory }
        } else {
            emptyList()
        }
        val body = JSONObject()
        if (BackupSection.SETTINGS in sections) body.put(BackupSection.SETTINGS.key, encodeSettings(settings.exportPreferences()))
        if (BackupSection.FAVORITES in sections) {
            body.put(BackupSection.FAVORITES.key, BeatmapJson.encodeList(favorites.snapshot()))
        }
        if (BackupSection.PLAYLISTS in sections) body.put(BackupSection.PLAYLISTS.key, encodePlaylists(playlists.snapshot()))
        if (BackupSection.HISTORY in sections) body.put(BackupSection.HISTORY.key, encodeHistory(history.snapshot()))
        if (BackupSection.MUSIC_DOWNLOADS in sections) {
            body.put(BackupSection.MUSIC_DOWNLOADS.key, encodeDownloadedSets(downloadedSets))
        }
        if (BackupSection.GAME_DATA in sections) {
            val songCount = game.paths.songs.listFiles { file -> file.isDirectory && !file.name.startsWith(".tmp_") }?.size ?: 0
            body.put(BackupSection.GAME_DATA.key, JSONObject().put("songs", songCount))
        }

        val manifest = JSONObject()
            .put("app", "kozu")
            .put("version", FORMAT_VERSION)
            .put("createdAt", System.currentTimeMillis())
            .put("sections", body)

        try {
            ZipOutputStream(BufferedOutputStream(output)).use { archive ->
                putBytes(archive, MANIFEST_PATH, manifest.toString(2).toByteArray(Charsets.UTF_8))
                if (BackupSection.MUSIC_DOWNLOADS in sections) {
                    downloadedSets.forEach { set ->
                        addDirectory(archive, downloads.folder(set.beatmap.id), "music/${set.beatmap.id}")
                    }
                }
                if (BackupSection.GAME_DATA in sections) {
                    game.exportDataFiles { root -> addDirectory(archive, root, "game") }
                }
            }
        } catch (error: Exception) {
            if (error is BackupException) throw error
            throw BackupException("Could not create backup: ${error.message ?: "unknown error"}")
        }
    }

    fun inspect(file: File): BackupInfo {
        if (!file.isFile) throw BackupException("The backup file could not be read")
        if (isZip(file)) {
            try {
                ZipFile(file).use { zip ->
                    val manifest = readManifest(zip)
                    return inspectRoot(manifest)
                }
            } catch (error: BackupException) {
                throw error
            } catch (error: Exception) {
                throw BackupException("This is not a valid Kozu backup archive")
            }
        }
        if (file.length() > MAX_LEGACY_CHARS.toLong()) throw BackupException("This legacy backup file is too large")
        return inspectJson(file.readText(Charsets.UTF_8))
    }

    fun inspect(json: String): BackupInfo = inspectRoot(parseRoot(json))

    suspend fun restore(
        json: String,
        sections: Set<BackupSection>,
    ): String = withContext(Dispatchers.IO) { restoreJson(json, sections) }

    suspend fun restore(
        file: File,
        sections: Set<BackupSection>,
    ): String = withContext(Dispatchers.IO) {
        if (!file.isFile) throw BackupException("The backup file could not be read")
        if (!isZip(file)) {
            if (file.length() > MAX_LEGACY_CHARS.toLong()) throw BackupException("This legacy backup file is too large")
            return@withContext restoreJson(file.readText(Charsets.UTF_8), sections)
        }

        val stagingRoot = File(appContext.cacheDir, "kozu-restore-${System.currentTimeMillis()}")
        try {
            ZipFile(file).use { zip ->
                val root = readManifest(zip)
                val body = root.optJSONObject("sections") ?: throw BackupException("This file has no backup data")
                val available = inspectRoot(root).sections
                val selected = sections.intersect(available)
                val lines = restoreJsonSections(body, selected)
                var extractedBytes = 0L

                if (BackupSection.MUSIC_DOWNLOADS in selected) {
                    val sets = decodeDownloadedSets(body.optJSONArray(BackupSection.MUSIC_DOWNLOADS.key) ?: JSONArray())
                    val extracted = File(stagingRoot, "music")
                    extractedBytes += extractPrefix(zip, "music/", extracted, MAX_BACKUP_ARCHIVE_BYTES - extractedBytes)
                    val restored = sets.filter { set ->
                        val source = File(extracted, set.beatmap.id.toString())
                        if (!source.isDirectory) return@filter false
                        replaceDirectory(source, downloads.folder(set.beatmap.id))
                        true
                    }
                    val changed = downloads.importAll(restored)
                    lines += "Downloaded music: ${restored.size} restored, $changed library entries updated"
                }

                if (BackupSection.GAME_DATA in selected) {
                    val gameFolder = File(stagingRoot, "game")
                    extractedBytes += extractPrefix(zip, "game/", gameFolder, MAX_BACKUP_ARCHIVE_BYTES - extractedBytes)
                    game.restoreDataFiles(gameFolder)
                    val count = gameFolder.listFiles()?.size ?: 0
                    lines += "Game data: ${count} top-level items restored"
                }

                if (lines.isEmpty()) throw BackupException("The selected sections are not in this file")
                lines.joinToString("\n")
            }
        } finally {
            stagingRoot.deleteRecursively()
        }
    }

    private suspend fun restoreJson(json: String, sections: Set<BackupSection>): String {
        val root = parseRoot(json)
        val body = root.optJSONObject("sections") ?: throw BackupException("This file has no backup data")
        val lines = restoreJsonSections(body, sections)
        if (lines.isEmpty()) throw BackupException("The selected sections are not in this file")
        return lines.joinToString("\n")
    }

    private suspend fun restoreJsonSections(
        body: JSONObject,
        sections: Set<BackupSection>,
    ): MutableList<String> {
        val lines = mutableListOf<String>()
        sections.forEach { section ->
            when (section) {
                BackupSection.SETTINGS -> {
                    body.optJSONObject(section.key)?.let {
                        val values = decodeSettings(it)
                        settings.importPreferences(values)
                        lines += "Settings: ${values.size} values restored"
                    }
                }

                BackupSection.FAVORITES -> {
                    body.optJSONArray(section.key)?.let {
                        val items = decodeBeatmaps(it)
                        val added = favorites.importAll(items)
                        lines += "Favorites: $added added, ${items.size - added} already saved"
                    }
                }

                BackupSection.PLAYLISTS -> {
                    body.optJSONArray(section.key)?.let {
                        val items = decodePlaylists(it)
                        val changed = playlists.importAll(items)
                        lines += "Playlists: ${items.size} processed, $changed changes"
                    }
                }

                BackupSection.HISTORY -> {
                    body.optJSONObject(section.key)?.let {
                        val data = decodeHistory(it)
                        val added = history.importAll(data)
                        lines += "History: $added plays added"
                    }
                }

                BackupSection.MUSIC_DOWNLOADS,
                BackupSection.GAME_DATA -> Unit
            }
        }
        return lines
    }

    private fun inspectJson(json: String): BackupInfo = inspectRoot(parseRoot(json))

    private fun inspectRoot(root: JSONObject): BackupInfo {
        validateRoot(root)
        val body = root.optJSONObject("sections") ?: throw BackupException("This file has no backup data")
        val counts = LinkedHashMap<BackupSection, Int>()
        BackupSection.entries.forEach { section ->
            when (section) {
                BackupSection.SETTINGS -> body.optJSONObject(section.key)?.let { counts[section] = decodeSettings(it).size }
                BackupSection.FAVORITES -> body.optJSONArray(section.key)?.let { counts[section] = decodeBeatmaps(it).size }
                BackupSection.PLAYLISTS -> body.optJSONArray(section.key)?.let { counts[section] = decodePlaylists(it).size }
                BackupSection.HISTORY -> body.optJSONObject(section.key)?.let { counts[section] = decodeHistory(it).events.size }
                BackupSection.MUSIC_DOWNLOADS -> body.optJSONArray(section.key)?.let { counts[section] = decodeDownloadedSets(it).size }
                BackupSection.GAME_DATA -> body.optJSONObject(section.key)?.let { counts[section] = it.optInt("songs", 0).coerceAtLeast(0) }
            }
        }
        if (counts.isEmpty()) throw BackupException("This file contains no recognised data")
        return BackupInfo(root.optInt("version"), root.optLong("createdAt"), counts)
    }

    private fun parseRoot(json: String): JSONObject {
        if (json.length > MAX_LEGACY_CHARS) throw BackupException("This file is too large to be a Kozu backup")
        val root = try {
            JSONObject(json)
        } catch (error: JSONException) {
            throw BackupException("This is not a valid Kozu backup file")
        }
        validateRoot(root)
        return root
    }

    private fun validateRoot(root: JSONObject) {
        if (root.optString("app") != "kozu") throw BackupException("This file was not created by Kozu")
        val version = root.optInt("version", 0)
        if (version < 1 || version > FORMAT_VERSION) throw BackupException("Unsupported backup version")
    }

    private fun encodeSettings(values: Map<String, Any>): JSONObject {
        val result = JSONObject()
        values.toSortedMap().forEach { (name, value) ->
            val tag = when (value) {
                is Boolean -> "b"
                is Int -> "i"
                is Long -> "l"
                is String -> "s"
                else -> return@forEach
            }
            result.put(name, JSONObject().put("t", tag).put("v", value))
        }
        return result
    }

    private fun decodeSettings(obj: JSONObject): Map<String, Any?> {
        val result = LinkedHashMap<String, Any?>()
        obj.keys().forEach { name ->
            if (!NAME_PATTERN.matches(name)) return@forEach
            val entry = obj.optJSONObject(name) ?: return@forEach
            val tag = entry.optString("t")
            val expected = TYPED_KEYS[name] ?: "s"
            if (tag != expected || !entry.has("v")) return@forEach
            val value: Any = try {
                when (tag) {
                    "b" -> entry.getBoolean("v")
                    "i" -> entry.getInt("v")
                    "l" -> entry.getLong("v")
                    else -> entry.getString("v").take(MAX_STRING)
                }
            } catch (error: JSONException) {
                return@forEach
            }
            result[name] = value
        }
        return result
    }

    private fun decodeBeatmaps(array: JSONArray): List<Beatmap> {
        val result = ArrayList<Beatmap>()
        for (index in 0 until array.length()) {
            val obj = array.optJSONObject(index) ?: continue
            val beatmap = try {
                BeatmapJson.decode(obj)
            } catch (error: JSONException) {
                continue
            }
            if (beatmap.id > 0L) result += beatmap
            if (result.size >= MAX_ITEMS) break
        }
        return result
    }

    private fun encodePlaylists(list: List<Playlist>): JSONArray {
        val array = JSONArray()
        list.forEach { playlist ->
            array.put(
                JSONObject()
                    .put("id", playlist.id)
                    .put("name", playlist.name)
                    .put("createdAt", playlist.createdAt)
                    .put("tracks", BeatmapJson.encodeList(playlist.tracks)),
            )
        }
        return array
    }

    private fun decodePlaylists(array: JSONArray): List<Playlist> {
        val result = ArrayList<Playlist>()
        for (index in 0 until array.length()) {
            val obj = array.optJSONObject(index) ?: continue
            val id = obj.optString("id").trim()
            if (id.isEmpty() || id.length > MAX_STRING) continue
            result += Playlist(
                id = id,
                name = obj.optString("name").take(MAX_STRING),
                tracks = decodeBeatmaps(obj.optJSONArray("tracks") ?: JSONArray()),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            )
            if (result.size >= MAX_ITEMS) break
        }
        return result
    }

    private fun encodeHistory(data: History): JSONObject {
        val events = JSONArray()
        data.events.forEach { events.put(JSONArray().put(it.beatmapId).put(it.at)) }
        val daily = JSONObject()
        data.dailyMs.forEach { (day, ms) -> daily.put(day, ms) }
        return JSONObject()
            .put("tracks", BeatmapJson.encodeList(data.tracks.values.toList()))
            .put("events", events)
            .put("daily", daily)
    }

    private fun decodeHistory(obj: JSONObject): History {
        val tracks = decodeBeatmaps(obj.optJSONArray("tracks") ?: JSONArray()).associateBy { it.id }
        val events = ArrayList<PlayEvent>()
        val array = obj.optJSONArray("events") ?: JSONArray()
        for (index in 0 until minOf(array.length(), MAX_ITEMS)) {
            val pair = array.optJSONArray(index) ?: continue
            if (pair.length() < 2) continue
            val id = pair.optLong(0, 0L)
            val at = pair.optLong(1, 0L)
            if (id > 0L && at > 0L) events += PlayEvent(id, at)
        }
        val daily = LinkedHashMap<String, Long>()
        obj.optJSONObject("daily")?.let { source ->
            source.keys().forEach { day ->
                val ms = source.optLong(day, 0L)
                if (DAY_PATTERN.matches(day) && ms > 0L) daily[day] = ms
            }
        }
        return History(tracks, events, daily)
    }

    private fun encodeDownloadedSets(sets: List<DownloadedSet>): JSONArray {
        val array = JSONArray()
        sets.forEach { set ->
            array.put(
                JSONObject()
                    .put("beatmap", BeatmapJson.encode(set.beatmap))
                    .put("audio", set.audioFile)
                    .put("bg", set.backgroundFile)
                    .put("size", set.sizeBytes)
                    .put("at", set.downloadedAt)
                    .put("source", set.source)
                    .put("outdated", set.outdated),
            )
        }
        return array
    }

    private fun decodeDownloadedSets(array: JSONArray): List<DownloadedSet> = buildList {
        for (index in 0 until minOf(array.length(), MAX_ITEMS)) {
            val obj = array.optJSONObject(index) ?: continue
            val beatmapObj = obj.optJSONObject("beatmap") ?: continue
            val beatmap = try {
                BeatmapJson.decode(beatmapObj)
            } catch (error: JSONException) {
                continue
            }
            if (beatmap.id <= 0L) continue
            add(
                DownloadedSet(
                    beatmap = beatmap,
                    audioFile = safeFileName(obj.optString("audio")),
                    backgroundFile = safeFileName(obj.optString("bg")),
                    sizeBytes = obj.optLong("size").coerceAtLeast(0L),
                    downloadedAt = obj.optLong("at").coerceAtLeast(0L),
                    source = obj.optString("source").take(MAX_STRING),
                    outdated = obj.optBoolean("outdated"),
                ),
            )
        }
    }

    private fun safeFileName(value: String): String {
        val normalized = value.replace('\\', '/').trim()
        if (normalized.startsWith('/') || normalized.split('/').any { it == ".." || it.isEmpty() }) return ""
        return normalized.take(MAX_STRING)
    }

    private fun addDirectory(
        archive: ZipOutputStream,
        directory: File,
        prefix: String,
    ) {
        if (!directory.isDirectory) return
        val cleanPrefix = prefix.trimEnd('/')
        putBytes(archive, "$cleanPrefix/", ByteArray(0))
        directory.walkTopDown()
            .filter { it.isFile }
            .sortedBy { it.relativeTo(directory).invariantSeparatorsPath }
            .forEach { file ->
                val relative = file.relativeTo(directory).invariantSeparatorsPath
                val entryName = "$cleanPrefix/$relative"
                validateArchivePath(entryName)
                archive.putNextEntry(ZipEntry(entryName))
                FileInputStream(file).buffered().use { input -> input.copyTo(archive, BUFFER_SIZE) }
                archive.closeEntry()
            }
    }

    private fun putBytes(archive: ZipOutputStream, name: String, bytes: ByteArray) {
        archive.putNextEntry(ZipEntry(name))
        archive.write(bytes)
        archive.closeEntry()
    }

    private fun readManifest(zip: ZipFile): JSONObject {
        val entry = zip.getEntry(MANIFEST_PATH) ?: throw BackupException("This archive has no Kozu backup manifest")
        if (entry.size > MAX_MANIFEST_BYTES.toLong()) throw BackupException("The backup manifest is too large")
        val bytes = zip.getInputStream(entry).use { input -> input.readBytesLimited(MAX_MANIFEST_BYTES) }
        return parseRoot(bytes.toString(Charsets.UTF_8))
    }

    private fun extractPrefix(
        zip: ZipFile,
        prefix: String,
        destination: File,
        maximumBytes: Long,
    ): Long {
        destination.deleteRecursively()
        if (!destination.mkdirs() && !destination.isDirectory) throw BackupException("Could not prepare the restore folder")
        val rootPath = destination.canonicalPath + File.separator
        var extractedBytes = 0L
        val entries = zip.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (!entry.name.startsWith(prefix)) continue
            val relative = entry.name.removePrefix(prefix).trimEnd('/')
            if (relative.isEmpty()) continue
            validateArchivePath(relative)
            val target = File(destination, relative).canonicalFile
            if (!target.path.startsWith(rootPath)) throw BackupException("The backup contains an unsafe file path")
            if (entry.isDirectory) {
                if (!target.exists() && !target.mkdirs()) throw BackupException("Could not create a restore folder")
                continue
            }
            target.parentFile?.let { if (!it.exists() && !it.mkdirs()) throw BackupException("Could not create a restore folder") }
            zip.getInputStream(entry).buffered().use { input ->
                FileOutputStream(target).buffered().use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        extractedBytes += count
                        if (extractedBytes > maximumBytes) throw BackupException("The backup expands beyond the allowed size")
                        output.write(buffer, 0, count)
                    }
                }
            }
        }
        return extractedBytes
    }

    private fun validateArchivePath(path: String) {
        val normalized = path.replace('\\', '/')
        if (normalized.startsWith('/') || normalized.split('/').any { it == ".." || it.isEmpty() }) {
            throw BackupException("The backup contains an unsafe file path")
        }
    }

    private fun replaceDirectory(
        source: File,
        destination: File,
    ) {
        val parent = destination.parentFile ?: throw BackupException("The download folder is unavailable")
        if (!parent.exists() && !parent.mkdirs()) throw BackupException("Could not prepare the download folder")
        val stamp = System.nanoTime()
        val staging = File(parent, ".${destination.name}.restore-$stamp")
        val previous = File(parent, ".${destination.name}.previous-$stamp")
        var previousMoved = false
        var restoredInstalled = false
        try {
            staging.deleteRecursively()
            source.copyRecursively(staging, overwrite = true)
            if (destination.exists()) {
                if (!destination.renameTo(previous)) throw BackupException("Could not replace an existing downloaded song")
                previousMoved = true
            }
            if (!staging.renameTo(destination)) throw BackupException("Could not restore a downloaded song")
            restoredInstalled = true
            previous.deleteRecursively()
        } catch (error: Exception) {
            if (restoredInstalled) destination.deleteRecursively()
            if (previousMoved && previous.exists() && !previous.renameTo(destination)) {
                error.addSuppressed(BackupException("Could not roll back the previous downloaded song"))
            }
            throw error
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun isZip(file: File): Boolean =
        runCatching {
            FileInputStream(file).use { input -> input.read() == 0x50 && input.read() == 0x4B }
        }.getOrDefault(false)

    private fun java.io.InputStream.readBytesLimited(maximum: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_SIZE)
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            if (output.size() + count > maximum) throw BackupException("The backup manifest is too large")
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private companion object {
        const val FORMAT_VERSION = 2
        const val MAX_LEGACY_CHARS = 20_000_000
        const val MAX_MANIFEST_BYTES = 20 * 1024 * 1024
        const val MAX_ITEMS = 20_000
        const val MAX_STRING = 4_000
        const val BUFFER_SIZE = 64 * 1024
        const val MANIFEST_PATH = "kozu/manifest.json"
        val NAME_PATTERN = Regex("[a-z0-9_]{1,64}")
        val DAY_PATTERN = Regex("\\d{4}-\\d{2}-\\d{2}")

        val TYPED_KEYS: Map<String, String> = buildMap {
            listOf(
                "amoled", "artwork_colors", "wifi_only_streaming", "skip_silence", "equalizer_enabled",
                "keep_full_beatmap", "wifi_only_downloads", "auto_update_downloads", "show_explicit",
                "high_refresh_rate", "performance_mode", "blur_player", "show_file_info", "compact_navigation",
                "show_mix_card", "keep_playing_after_close", "resume_on_headset", "pause_on_headset_disconnect",
                "hifi_mode", "hardware_offload", "remember_shuffle", "show_queue_history", "autoplay",
                "avoid_repeats", "dedupe_queue", "preload_next", "haptic_feedback", "auto_open_player",
                "artwork_swipe", "keep_screen_on", "double_tap_search", "prefer_unicode", "show_mapper",
                "auto_sync_library", "auto_clean_cache", "auto_update_check",
            ).forEach { put(it, "b") }
            listOf(
                "stream_cache_mb", "blur_intensity", "nav_corner_radius", "image_cache_mb", "bass_boost",
                "virtualizer", "loudness", "top_size", "mix_size", "proxy_port", "download_limit_mb",
            ).forEach { put(it, "i") }
            listOf("last_sync_at", "last_update_check").forEach { put(it, "l") }
        }
    }
}
