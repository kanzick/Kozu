package vn.kozu.game.mania.previewstage.storage

import java.io.File
import java.io.IOException

class ModulePaths(val root: File) {
    val downloads = File(root, "downloads")
    val songs = File(root, "songs")
    val importState = File(root, "import-state")
    val kozuDb = File(root, "kozu.db")
    val scoreDb = File(root, "score.db")

    fun songDir(songId: Long) = File(songs, songId.toString())

    fun ensureLayout() {
        for (dir in listOf(root, downloads, songs, importState)) {
            if (!dir.isDirectory && !dir.mkdirs() && !dir.isDirectory) {
                throw IOException("Could not create directory ${dir.absolutePath}")
            }
        }

        songs.listFiles { f -> f.isDirectory && f.name.startsWith(TMP_PREFIX) }?.forEach { it.deleteRecursively() }
    }

    companion object {
        const val TMP_PREFIX = ".tmp_"
    }
}
