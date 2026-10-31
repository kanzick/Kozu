package vn.kozu.game.mania.previewstage.storage

import vn.kozu.game.mania.previewstage.model.DifficultyRecord
import vn.kozu.game.mania.previewstage.model.ImportFailure
import vn.kozu.game.mania.previewstage.osu.FileResolver
import vn.kozu.game.mania.previewstage.osu.OsuBeatmap
import vn.kozu.game.mania.previewstage.osu.OsuParser
import vn.kozu.game.mania.previewstage.osu.OszExtractor
import java.io.File
import java.io.IOException
import java.util.UUID

class ImportSummary(
    val imported: List<Long>,
    val skipped: List<Long>,
    val failed: List<ImportFailure>,
)

/**
 * Scans downloads/ once: each .osz is extracted to songs/<songId>/ and recorded in kozu.db.
 * Extraction happens in a temporary directory before it is renamed, so interruptions do not leave partial directories.
 */
class SongImporter(
    private val paths: ModulePaths,
    private val kozu: KozuDatabase,
    private val onlyMania: Boolean,
    private val reimportExisting: Boolean,
    private val maxExtractBytes: Long,
) {
    private sealed interface Outcome {
        data class Imported(val songId: Long) : Outcome
        data class Skipped(val songId: Long) : Outcome
    }

    fun scanDownloads(): ImportSummary {
        val oszFiles = paths.downloads
            .listFiles { f -> f.isFile && f.extension.equals("osz", ignoreCase = true) }
            ?.sortedBy { it.name }
            .orEmpty()

        val imported = ArrayList<Long>()
        val skipped = ArrayList<Long>()
        val failed = ArrayList<ImportFailure>()
        oszFiles.forEach { osz -> processArchive(osz, imported, skipped, failed) }
        return ImportSummary(imported, skipped, failed)
    }

    fun importArchive(osz: File): ImportSummary {
        require(osz.isFile && osz.extension.equals("osz", ignoreCase = true)) {
            "The map archive could not be found"
        }
        val imported = ArrayList<Long>()
        val skipped = ArrayList<Long>()
        val failed = ArrayList<ImportFailure>()
        processArchive(osz, imported, skipped, failed)
        return ImportSummary(imported, skipped, failed)
    }

    private fun processArchive(
        osz: File,
        imported: MutableList<Long>,
        skipped: MutableList<Long>,
        failed: MutableList<ImportFailure>,
    ) {
        try {
            val result = if (!reimportExisting && isAlreadyImported(osz)) {
                Outcome.Skipped(cachedSongId(osz) ?: throw IOException("Could not read the imported map record"))
            } else {
                importOne(osz)
            }
            when (result) {
                is Outcome.Imported -> {
                    imported += result.songId
                    saveImportState(osz, result.songId)
                }
                is Outcome.Skipped -> {
                    skipped += result.songId
                    saveImportState(osz, result.songId)
                }
            }
            if (osz.exists() && !osz.delete()) {
                throw IOException("The map was imported, but its .osz file could not be removed from downloads")
            }
        } catch (e: Exception) {
            failed += ImportFailure(osz.name, e.message ?: e.javaClass.simpleName)
        }
    }

    private fun importOne(osz: File): Outcome {
        val tmp = File(paths.songs, ModulePaths.TMP_PREFIX + UUID.randomUUID())
        try {
            OszExtractor.extract(osz, tmp, maxExtractBytes)

            val parsed = tmp.walkTopDown()
                .filter { it.isFile && it.extension.equals("osu", ignoreCase = true) }
                .sortedBy { it.path }
                .mapNotNull { file ->
                    val beatmap = runCatching {
                        OsuParser.parse(file.readText(Charsets.UTF_8))
                    }.getOrNull()
                    if (beatmap == null || (onlyMania && beatmap.mode != OsuParser.MODE_MANIA)) {
                        if (!file.delete()) throw IOException("Could not remove non-Mania chart ${file.path}")
                        null
                    } else {
                        file to beatmap
                    }
                }
                .toList()
            if (parsed.isEmpty())             throw IOException("No valid Mania charts were found in the .osz archive")

            val songId = resolveSongId(parsed.map { it.second }, osz)
            val target = paths.songDir(songId)
            if (!reimportExisting &&
                target.isDirectory &&
                kozu.hasCompleteSongFiles(songId, target) &&
                hasSameContents(tmp, target)
            ) return Outcome.Skipped(songId)

            val records = parsed.map { (file, beatmap) -> toRecord(songId, tmp, file, beatmap) }

            if (target.exists() && (!target.deleteRecursively() || target.exists())) {
                throw IOException("Could not replace the existing chart folder ${target.path}")
            }
            if (!tmp.renameTo(target)) throw IOException("Could not move the imported chart folder into place")
            kozu.replaceSong(songId, osz.name, records)
            return Outcome.Imported(songId)
        } finally {
            if (tmp.exists()) tmp.deleteRecursively()
        }
    }

    private fun hasSameContents(first: File, second: File): Boolean {
        fun entries(root: File): List<String> = root.walkTopDown()
            .drop(1)
            .map { file ->
                file.relativeTo(root).invariantSeparatorsPath +
                    if (file.isDirectory) "/" else ""
            }
            .sorted()
            .toList()

        val firstEntries = entries(first)
        if (firstEntries != entries(second)) return false

        return firstEntries
            .filterNot { it.endsWith("/") }
            .all { relativePath ->
                val a = File(first, relativePath)
                val b = File(second, relativePath)
                a.length() == b.length() && sameBytes(a, b)
            }
    }

    private fun isAlreadyImported(osz: File): Boolean {
        val songId = cachedSongId(osz) ?: return false
        val saved = runCatching { importStateFile(osz).readLines(Charsets.UTF_8) }.getOrNull()
            ?: return false
        if (saved.size != 3 || saved[1].toLongOrNull() != osz.length() ||
            saved[2].toLongOrNull() != osz.lastModified()
        ) return false
        return kozu.hasCompleteSongFiles(songId, paths.songDir(songId))
    }

    private fun cachedSongId(osz: File): Long? =
        runCatching { importStateFile(osz).readLines(Charsets.UTF_8).firstOrNull()?.toLongOrNull() }
            .getOrNull()

    private fun saveImportState(osz: File, songId: Long) {
        importStateFile(osz).writeText(
            "$songId\n${osz.length()}\n${osz.lastModified()}",
            Charsets.UTF_8
        )
    }

    private fun importStateFile(osz: File): File =
        File(paths.importState, "${osz.name.hashCode().toUInt().toString(16)}.txt")

    private fun sameBytes(first: File, second: File): Boolean {
        val firstBuffer = ByteArray(8192)
        val secondBuffer = ByteArray(8192)
        first.inputStream().buffered().use { a ->
            second.inputStream().buffered().use { b ->
                while (true) {
                    val aCount = a.read(firstBuffer)
                    val bCount = b.read(secondBuffer)
                    if (aCount != bCount) return false
                    if (aCount < 0) return true
                    for (i in 0 until aCount) {
                        if (firstBuffer[i] != secondBuffer[i]) return false
                    }
                }
            }
        }
    }

    /** Prefers BeatmapSetID from the .osu file, then the leading number in the filename ("2610887_linear..."), and finally the filename hash. */
    private fun resolveSongId(beatmaps: List<OsuBeatmap>, osz: File): Long {
        beatmaps.firstOrNull { it.beatmapSetId > 0 }?.let { return it.beatmapSetId }
        Regex("""^(\d{1,15})(?=\D|$)""").find(osz.name)?.let { return it.groupValues[1].toLong() }
        return osz.name.hashCode().toLong() and 0x7fffffffL
    }

    private fun toRecord(songId: Long, root: File, osuFile: File, b: OsuBeatmap): DifficultyRecord {
        val dir = osuFile.parentFile ?: root
        val audio = if (b.audioFilename.isNotBlank()) FileResolver.resolve(dir, b.audioFilename) else null
        val image = b.backgroundFilename?.let { FileResolver.resolve(dir, it) }
        fun rel(f: File) = f.relativeTo(root).invariantSeparatorsPath
        return DifficultyRecord(
            songId = songId,
            osuFile = rel(osuFile),
            audioFilename = b.audioFilename,
            audioLeadIn = b.audioLeadIn,
            previewTime = b.previewTime,
            countdown = b.countdown,
            sampleSet = b.sampleSet,
            stackLeniency = b.stackLeniency,
            mode = b.mode,
            letterboxInBreaks = b.letterboxInBreaks,
            widescreenStoryboard = b.widescreenStoryboard,
            specialStyle = b.specialStyle,
            title = b.title, titleUnicode = b.titleUnicode,
            artist = b.artist, artistUnicode = b.artistUnicode,
            creator = b.creator, version = b.version,
            source = b.source, tags = b.tags,
            beatmapId = b.beatmapId, beatmapSetId = b.beatmapSetId,
            hpDrainRate = b.hpDrainRate, circleSize = b.circleSize,
            overallDifficulty = b.overallDifficulty, approachRate = b.approachRate,
            sliderMultiplier = b.sliderMultiplier, sliderTickRate = b.sliderTickRate,
            backgroundFilename = b.backgroundFilename,
            audioPath = audio?.let(::rel),
            audioExists = audio != null,
            backgroundPath = image?.let(::rel),
            backgroundExists = image != null,
        )
    }
}
