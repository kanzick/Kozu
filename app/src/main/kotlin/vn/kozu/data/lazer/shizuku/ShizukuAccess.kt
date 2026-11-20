package vn.kozu.data.lazer.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.RemoteException
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku
import vn.kozu.data.lazer.ILazerFileService
import vn.kozu.data.lazer.LAZER_DATABASE_NAME
import vn.kozu.data.lazer.LazerAccessException
import vn.kozu.data.lazer.LazerAccessProblem
import vn.kozu.data.lazer.LazerDataSource
import vn.kozu.data.lazer.copyStreamTo
import vn.kozu.data.lazer.lazerFilePath

class ShizukuConnection internal constructor(
    val service: ILazerFileService,
    private val args: Shizuku.UserServiceArgs,
    private val connection: ServiceConnection,
) {
    fun release() {
        try {
            Shizuku.unbindUserService(args, connection, true)
        } catch (_: Throwable) {
        }
    }
}

enum class ShizukuState {
    MISSING,
    STOPPED,
    UNSUPPORTED,
    NEEDS_PERMISSION,
    READY,
}

object ShizukuAccess {
    const val MANAGER_PACKAGE = "moe.shizuku.privileged.api"
    const val DOWNLOAD_URL = "https://shizuku.rikka.app/download/"
    private const val REQUEST_CODE = 4217
    private const val SERVICE_TIMEOUT_MS = 8_000L

    @Volatile
    private var userServiceUnavailable = false

    fun isInstalled(context: Context): Boolean =
        try {
            context.packageManager.getPackageInfo(MANAGER_PACKAGE, 0)
            true
        } catch (_: Exception) {
            false
        }

    fun isRunning(): Boolean =
        try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }

    private fun isSupported(): Boolean =
        try {
            !Shizuku.isPreV11()
        } catch (_: Throwable) {
            false
        }

    fun hasPermission(): Boolean =
        try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }

    fun state(context: Context): ShizukuState =
        when {
            !isRunning() -> if (isInstalled(context)) ShizukuState.STOPPED else ShizukuState.MISSING
            !isSupported() -> ShizukuState.UNSUPPORTED
            !hasPermission() -> ShizukuState.NEEDS_PERMISSION
            else -> ShizukuState.READY
        }

    suspend fun requestPermission(): Boolean =
        withContext(Dispatchers.Main) {
            if (hasPermission()) return@withContext true
            suspendCancellableCoroutine { continuation ->
                val listener =
                    object : Shizuku.OnRequestPermissionResultListener {
                        override fun onRequestPermissionResult(
                            requestCode: Int,
                            grantResult: Int,
                        ) {
                            if (requestCode != REQUEST_CODE) return
                            Shizuku.removeRequestPermissionResultListener(this)
                            if (continuation.isActive) continuation.resume(grantResult == PackageManager.PERMISSION_GRANTED)
                        }
                    }
                Shizuku.addRequestPermissionResultListener(listener)
                continuation.invokeOnCancellation { Shizuku.removeRequestPermissionResultListener(listener) }
                try {
                    Shizuku.requestPermission(REQUEST_CODE)
                } catch (_: Throwable) {
                    Shizuku.removeRequestPermissionResultListener(listener)
                    if (continuation.isActive) continuation.resume(false)
                }
            }
        }

    private suspend fun connect(context: Context): ShizukuConnection =
        withContext(Dispatchers.Main) {
            val appContext = context.applicationContext
            val versionCode =
                PackageInfoCompat
                    .getLongVersionCode(appContext.packageManager.getPackageInfo(appContext.packageName, 0))
                    .toInt()
            val args =
                Shizuku
                    .UserServiceArgs(ComponentName(appContext.packageName, LazerFileService::class.java.name))
                    .processNameSuffix("lazer")
                    .daemon(false)
                    .tag("kozu-lazer-files")
                    .version(versionCode)
            withTimeout(SERVICE_TIMEOUT_MS) {
                suspendCancellableCoroutine { continuation ->
                    val connection =
                        object : ServiceConnection {
                            override fun onServiceConnected(
                                name: ComponentName?,
                                binder: IBinder?,
                            ) {
                                if (!continuation.isActive) return
                                if (binder != null && binder.pingBinder()) {
                                    continuation.resume(
                                        ShizukuConnection(ILazerFileService.Stub.asInterface(binder), args, this),
                                    )
                                } else {
                                    continuation.resumeWithException(IllegalStateException("Invalid Shizuku binder"))
                                }
                            }

                            override fun onServiceDisconnected(name: ComponentName?) {}
                        }
                    continuation.invokeOnCancellation {
                        try {
                            Shizuku.unbindUserService(args, connection, true)
                        } catch (_: Throwable) {
                        }
                    }
                    try {
                        Shizuku.bindUserService(args, connection)
                    } catch (e: Throwable) {
                        continuation.resumeWithException(e)
                    }
                }
            }
        }

    private suspend fun openUserService(
        context: Context,
        userId: Int,
    ): LazerDataSource {
        val connection = connect(context)
        val root =
            try {
                withContext(Dispatchers.IO) { connection.service.findRoot(userId) }
            } catch (e: Throwable) {
                connection.release()
                throw e
            }
        if (root.isNullOrBlank()) {
            connection.release()
            throw LazerAccessException(LazerAccessProblem.DATA_NOT_FOUND)
        }
        return ShizukuLazerSource(connection, root)
    }

    suspend fun open(context: Context): LazerDataSource {
        when (state(context)) {
            ShizukuState.MISSING -> throw LazerAccessException(LazerAccessProblem.SHIZUKU_MISSING)
            ShizukuState.STOPPED -> throw LazerAccessException(LazerAccessProblem.SHIZUKU_STOPPED)
            ShizukuState.UNSUPPORTED -> throw LazerAccessException(LazerAccessProblem.SHIZUKU_UNSUPPORTED)
            ShizukuState.NEEDS_PERMISSION ->
                if (!requestPermission()) throw LazerAccessException(LazerAccessProblem.SHIZUKU_DENIED)

            ShizukuState.READY -> Unit
        }
        val userId = Process.myUid() / 100_000
        var serviceFailure: Throwable? = null
        if (!userServiceUnavailable) {
            try {
                return openUserService(context, userId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: LazerAccessException) {
                if (e.problem == LazerAccessProblem.DATA_NOT_FOUND) throw e
                serviceFailure = e
            } catch (e: Throwable) {
                serviceFailure = e
            }
            userServiceUnavailable = true
        }
        try {
            return ShizukuShellSource.open(userId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: LazerAccessException) {
            if (e.problem == LazerAccessProblem.DATA_NOT_FOUND) throw e
            throw serviceError(serviceFailure, e)
        } catch (e: Throwable) {
            throw serviceError(serviceFailure, e)
        }
    }

    private fun serviceError(
        first: Throwable?,
        second: Throwable,
    ): LazerAccessException {
        val parts = listOfNotNull(first?.let { "service: ${describe(it)}" }, "shell: ${describe(second)}")
        return LazerAccessException(LazerAccessProblem.SHIZUKU_SERVICE, parts.joinToString("; "), second)
    }

    private fun describe(error: Throwable): String {
        val root = generateSequence(error) { it.cause?.takeIf { cause -> cause !== it } }.last()
        val message = root.message?.lineSequence()?.firstOrNull()?.take(160).orEmpty()
        return if (message.isBlank()) root.javaClass.simpleName else "${root.javaClass.simpleName}: $message"
    }
}

class ShizukuLazerSource internal constructor(
    private val connection: ShizukuConnection,
    private val root: String,
) : LazerDataSource {
    override suspend fun copyDatabase(
        target: File,
        onProgress: (Long) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val descriptor =
            try {
                connection.service.openFile("$root/$LAZER_DATABASE_NAME")
            } catch (e: RemoteException) {
                throw LazerAccessException(LazerAccessProblem.SHIZUKU_SERVICE, e.message, e)
            } ?: throw LazerAccessException(LazerAccessProblem.DATA_NOT_FOUND)
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { copyStreamTo(it, target, onProgress) }
    }

    override suspend fun openFile(hash: String): InputStream? =
        withContext(Dispatchers.IO) {
            val relative = lazerFilePath(hash) ?: return@withContext null
            try {
                connection.service.openFile("$root/$relative")?.let { ParcelFileDescriptor.AutoCloseInputStream(it) }
            } catch (_: RemoteException) {
                null
            }
        }

    override fun close() = connection.release()
}
