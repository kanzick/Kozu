package vn.kozu.data.lazer.shizuku

import android.os.ParcelFileDescriptor
import java.io.File
import kotlin.system.exitProcess
import vn.kozu.data.lazer.ILazerFileService

class LazerFileService : ILazerFileService.Stub() {
    override fun destroy() {
        exitProcess(0)
    }

    override fun findRoot(userId: Int): String {
        for (candidate in candidates(userId)) {
            val database = File(candidate, DATABASE_NAME)
            if (database.isFile && database.canRead()) return candidate
        }
        return ""
    }

    override fun openFile(path: String?): ParcelFileDescriptor? {
        if (path.isNullOrBlank()) return null
        return try {
            val file = File(path).canonicalFile
            if (!isAllowed(file) || !file.isFile) {
                null
            } else {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun isAllowed(file: File): Boolean {
        val path = file.path
        return path.contains("/$LAZER_PACKAGE/") &&
            (path.startsWith("/storage/") || path.startsWith("/sdcard/") || path.startsWith("/data/") || path.startsWith("/mnt/"))
    }

    private fun candidates(userId: Int): List<String> =
        listOf(
            "/storage/emulated/$userId/Android/data/$LAZER_PACKAGE/files",
            "/data/media/$userId/Android/data/$LAZER_PACKAGE/files",
            "/data/user/$userId/$LAZER_PACKAGE/files",
            "/data/data/$LAZER_PACKAGE/files",
        )

    private companion object {
        const val DATABASE_NAME = "client.realm"
        const val LAZER_PACKAGE = "sh.ppy.osulazer"
    }
}
