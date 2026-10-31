package vn.kozu.game.mania.previewstage.storage

import java.io.File
import java.io.IOException

/**
 * Bố cục thư mục của module:
 *
 *   <root>/downloads/        nơi thả file .osz
 *   <root>/songs/<songId>/   nhạc đã giải nén
 *   <root>/kozu.db           thông tin bài + độ khó
 *   <root>/score.db          điểm người chơi
 */
class ModulePaths(val root: File) {
    val downloads = File(root, "downloads")
    val songs = File(root, "songs")
    val importState = File(root, "import-state")
    val kozuDb = File(root, "kozu.db")
    val scoreDb = File(root, "score.db")

    fun songDir(songId: Long) = File(songs, songId.toString())

    /** Tạo các thư mục còn thiếu. (2 file db sẽ được SQLite tạo khi mở lần đầu.) */
    fun ensureLayout() {
        for (dir in listOf(root, downloads, songs, importState)) {
            if (!dir.isDirectory && !dir.mkdirs() && !dir.isDirectory) {
                throw IOException("Could not create directory ${dir.absolutePath}")
            }
        }
        // Dọn thư mục tạm còn sót lại do lần chạy trước bị ngắt giữa chừng.
        songs.listFiles { f -> f.isDirectory && f.name.startsWith(TMP_PREFIX) }?.forEach { it.deleteRecursively() }
    }

    companion object {
        const val TMP_PREFIX = ".tmp_"
    }
}
