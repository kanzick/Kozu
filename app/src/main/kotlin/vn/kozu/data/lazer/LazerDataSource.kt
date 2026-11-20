package vn.kozu.data.lazer

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class LazerAccessProblem {
    SHIZUKU_MISSING,
    SHIZUKU_STOPPED,
    SHIZUKU_SERVICE,
    SHIZUKU_UNSUPPORTED,
    SHIZUKU_DENIED,
    DATA_NOT_FOUND,
    NOT_LAZER,
    DATABASE,
    STORAGE_FULL,
}

class LazerAccessException(
    val problem: LazerAccessProblem,
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message ?: problem.name, cause)

interface LazerDataSource : AutoCloseable {
    suspend fun copyDatabase(
        target: File,
        onProgress: (Long) -> Unit = {},
    )

    suspend fun openFile(hash: String): InputStream?

    override fun close() {}
}

internal const val LAZER_DATABASE_NAME = "client.realm"
private const val PROGRESS_STEP = 2L * 1024L * 1024L

internal fun lazerFilePath(hash: String): String? {
    if (hash.length < 2 || !hash.all { it.isLetterOrDigit() }) return null
    return "files/${hash.substring(0, 1)}/${hash.substring(0, 2)}/$hash"
}

internal fun copyStreamTo(
    input: InputStream,
    target: File,
    onProgress: (Long) -> Unit = {},
) {
    target.parentFile?.mkdirs()
    try {
        target.outputStream().buffered(512 * 1024).use { output ->
            val buffer = ByteArray(512 * 1024)
            var total = 0L
            var reported = 0L
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                output.write(buffer, 0, count)
                total += count
                if (total - reported >= PROGRESS_STEP) {
                    reported = total
                    onProgress(total)
                }
            }
            onProgress(total)
        }
    } catch (e: IOException) {
        target.delete()
        val message = e.message.orEmpty()
        if (message.contains("ENOSPC", ignoreCase = true) || message.contains("No space", ignoreCase = true)) {
            throw LazerAccessException(LazerAccessProblem.STORAGE_FULL, cause = e)
        }
        throw e
    }
}

class DirectLazerSource(
    private val root: File,
) : LazerDataSource {
    override suspend fun copyDatabase(
        target: File,
        onProgress: (Long) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val database = File(root, LAZER_DATABASE_NAME)
        if (!database.isFile) throw LazerAccessException(LazerAccessProblem.DATA_NOT_FOUND)
        database.inputStream().use { copyStreamTo(it, target, onProgress) }
    }

    override suspend fun openFile(hash: String): InputStream? =
        withContext(Dispatchers.IO) {
            val relative = lazerFilePath(hash) ?: return@withContext null
            val file = File(root, relative)
            if (file.isFile) runCatching { file.inputStream() }.getOrNull() else null
        }

    companion object {
        fun detect(): DirectLazerSource? {
            val external = android.os.Environment.getExternalStorageDirectory()
            val root = File(external, "Android/data/$LAZER_PACKAGE/files")
            val database = File(root, LAZER_DATABASE_NAME)
            return if (database.isFile && database.canRead()) DirectLazerSource(root) else null
        }
    }
}

class TreeLazerSource private constructor(
    private val context: Context,
    private val tree: Uri,
    private val baseId: String,
) : LazerDataSource {
    private fun documentUri(relative: String): Uri {
        val id = if (baseId.endsWith(":") || baseId.endsWith("/")) "$baseId$relative" else "$baseId/$relative"
        return DocumentsContract.buildDocumentUriUsingTree(tree, id)
    }

    private fun open(uri: Uri): InputStream? =
        try {
            context.contentResolver.openInputStream(uri)
        } catch (_: Exception) {
            null
        }

    override suspend fun copyDatabase(
        target: File,
        onProgress: (Long) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val input = open(documentUri(LAZER_DATABASE_NAME)) ?: throw LazerAccessException(LazerAccessProblem.NOT_LAZER)
        input.use { copyStreamTo(it, target, onProgress) }
    }

    override suspend fun openFile(hash: String): InputStream? =
        withContext(Dispatchers.IO) {
            val relative = lazerFilePath(hash) ?: return@withContext null
            open(documentUri(relative))
        }

    companion object {
        suspend fun create(
            context: Context,
            tree: Uri,
        ): TreeLazerSource =
            withContext(Dispatchers.IO) {
                val treeId = DocumentsContract.getTreeDocumentId(tree)
                val direct = TreeLazerSource(context, tree, treeId)
                if (direct.hasDatabase()) return@withContext direct
                for (child in direct.childFolders(treeId)) {
                    val candidate = TreeLazerSource(context, tree, child)
                    if (candidate.hasDatabase()) return@withContext candidate
                    for (grandChild in candidate.childFolders(child)) {
                        val deeper = TreeLazerSource(context, tree, grandChild)
                        if (deeper.hasDatabase()) return@withContext deeper
                    }
                }
                throw LazerAccessException(LazerAccessProblem.NOT_LAZER)
            }
    }

    private fun hasDatabase(): Boolean {
        val stream = open(documentUri(LAZER_DATABASE_NAME)) ?: return false
        stream.close()
        return true
    }

    private fun childFolders(parentId: String): List<String> {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
        val folders = ArrayList<String>()
        try {
            context.contentResolver
                .query(
                    children,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                    ),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        if (cursor.getString(1) == DocumentsContract.Document.MIME_TYPE_DIR) {
                            cursor.getString(0)?.let(folders::add)
                        }
                    }
                }
        } catch (_: Exception) {
        }
        return folders
    }
}
