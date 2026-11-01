package vn.kozu.game.mania.previewstage

import vn.kozu.game.mania.previewstage.osu.FileResolver
import vn.kozu.game.mania.previewstage.osu.OsuParser
import vn.kozu.game.mania.previewstage.osu.OszExtractor
import vn.kozu.game.mania.previewstage.model.SongEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import org.junit.Test

class PreviewStageCoreTest {
    private val sample = """
        osu file format v14

        [General]
        AudioFilename: Audio.MP3
        PreviewTime: 70313
        Mode: 3

        [Metadata]
        Title:bloo cat, bloo
        Version:^ -. -^   meow, woof   u^ w^ u
        BeatmapSetID:2610887

        [Difficulty]
        CircleSize:4
        OverallDifficulty:6.8

        [Events]
        //Background and Video events
        0,0,"love.jpg",0,0
    """.trimIndent()

    @Test
    fun parsesSections() {
        val b = OsuParser.parse(sample)
        assertEquals("Audio.MP3", b.audioFilename)
        assertEquals(70313, b.previewTime)
        assertEquals(3, b.mode)
        assertEquals("bloo cat, bloo", b.title)
        assertEquals("^ -. -^   meow, woof   u^ w^ u", b.version)
        assertEquals(2610887L, b.beatmapSetId)
        assertEquals(4.0, b.circleSize, 0.0)
        assertEquals("love.jpg", b.backgroundFilename)
    }

    @Test
    fun resolvesCaseInsensitive() {
        val dir = createTempDirectory().toFile()
        File(dir, "song").mkdir()
        File(dir, "song/audio.mp3").writeText("x")
        assertNotNull(FileResolver.resolve(dir, "Song/Audio.MP3"))
        assertNull(FileResolver.resolve(dir, "../etc/passwd"))
        dir.deleteRecursively()
    }

    @Test
    fun extractsAllFilesWithoutFlatteningFolders() {
        val dir = createTempDirectory().toFile()
        val zip = File(dir, "song.osz")
        ZipOutputStream(zip.outputStream()).use {
            it.putNextEntry(ZipEntry("folder/")); it.closeEntry()
            it.putNextEntry(ZipEntry("folder/map.osu")); it.write("map".toByteArray()); it.closeEntry()
            it.putNextEntry(ZipEntry("folder/audio/song.mp3")); it.write("audio".toByteArray()); it.closeEntry()
            it.putNextEntry(ZipEntry("background.jpg")); it.write("image".toByteArray()); it.closeEntry()
        }

        val output = File(dir, "song")
        assertEquals(3, OszExtractor.extract(zip, output))
        assertEquals("map", File(output, "folder/map.osu").readText())
        assertEquals("audio", File(output, "folder/audio/song.mp3").readText())
        assertEquals("image", File(output, "background.jpg").readText())
        dir.deleteRecursively()
    }

    @Test
    fun blocksZipSlip() {
        val dir = createTempDirectory().toFile()
        val zip = File(dir, "evil.osz")
        ZipOutputStream(zip.outputStream()).use {
            it.putNextEntry(ZipEntry("../evil.txt")); it.write(1); it.closeEntry()
        }
        try {
            OszExtractor.extract(zip, File(dir, "out"))
            fail("phải ném IOException")
        } catch (e: IOException) {
            assertTrue(!File(dir, "evil.txt").exists())
        }
        dir.deleteRecursively()
    }

    @Test
    fun catalogUpsertUpdatesInMemoryEntryWithoutDuplicatingSong() {
        val catalog = KozuCatalog()
        val original = SongEntry(2, File("songs/2"), emptyList())
        val replacement = SongEntry(2, File("songs/2-updated"), emptyList())
        val additional = SongEntry(1, File("songs/1"), emptyList())
        catalog.replace(listOf(original))

        catalog.upsert(replacement)
        catalog.upsert(additional)

        assertEquals(listOf(1L, 2L), catalog.all().map { it.songId })
        assertEquals(replacement, catalog.find(2))
        assertEquals(additional, catalog.find(1))
    }
}
