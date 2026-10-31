package vn.kozu.game.mania.previewstage.storage

import java.io.File
import java.io.IOException

/**
 * Module directory layout:
 *
 *   <root>/downloads/        .osz files to import
 *   <root>/songs/<songId>/   extracted songs
 *   <root>/kozu.db           song and difficulty metadata
 *   <root>/score.db          player scores
 */
class ModulePaths(val root: File) {
    val downloads = File(root, "downloads")
    val songs = File(root, "songs")
    val importState = File(root, "import-state")
    val kozuDb = File(root, "kozu.db")
    val scoreDb = File(root, "score.db")

    fun songDir(songId: Long) = File(songs, songId.toString())

    /** Creates missing directories. SQLite creates the two database files when they are first opened. */
    fun ensureLayout() {
        for (dir in listOf(root, downloads, songs, importState)) {
            if (!dir.isDirectory && !dir.mkdirs() && !dir.isDirectory) {
                throw IOException("Could not create directory ${dir.absolutePath}")
            }
        }
        // Clean up temporary directories left behind by an interrupted previous run.
        songs.listFiles { f -> f.isDirectory && f.name.startsWith(TMP_PREFIX) }?.forEach { it.deleteRecursively() }
    }

    companion object {
        const val TMP_PREFIX = ".tmp_"
    }
}
