package vn.kozu.game.mania.gamestage.audio

import android.media.AudioAttributes
import android.media.SoundPool
import kotlinx.coroutines.delay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/** Phát hitsound độ trễ thấp bằng SoundPool. */
class HitSoundPlayer {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(32)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val lock = Any()
    private val loaded = HashMap<String, Int>()
    private var pending = 0

    /** Nạp các file; trả về map đường dẫn -> sound id (file nạp lỗi bị bỏ qua). */
    suspend fun load(files: Collection<File>): Map<String, Int> {
        val unique = synchronized(lock) { files.distinctBy { it.absolutePath }.filter { it.absolutePath !in loaded } }
        if (unique.isNotEmpty()) {
            val signal = CompletableDeferred<Unit>()
            synchronized(lock) {
                pending = unique.size
            }
            val idToPath = HashMap<Int, String>()
            withContext(Dispatchers.IO) {
                pool.setOnLoadCompleteListener { _, id, status ->
                    synchronized(lock) {
                        idToPath[id]?.let { path -> if (status != 0) loaded.remove(path) }
                        if (--pending <= 0) signal.complete(Unit)
                    }
                }
                synchronized(lock) {
                    for (f in unique) {
                        val id = pool.load(f.absolutePath, 1)
                        if (id == 0) {
                            pending--
                        } else {
                            loaded[f.absolutePath] = id
                            idToPath[id] = f.absolutePath
                        }
                    }
                    if (pending <= 0) signal.complete(Unit)
                }
            }
            if (withTimeoutOrNull(5000) { signal.await() } == null) {
                throw IllegalStateException("Timed out while loading gameplay hit sounds")
            }
        }
        return synchronized(lock) { HashMap(loaded) }
    }

    suspend fun warmUp() {
        val soundId = synchronized(lock) { loaded.values.firstOrNull() } ?: return
        val streamId = pool.play(soundId, 0f, 0f, 0, 0, 1f)
        if (streamId != 0) {
            delay(AUDIO_WARM_UP_MS)
            pool.stop(streamId)
        }
    }

    fun play(soundId: Int, volume: Float) {
        if (volume <= 0f) return
        pool.play(soundId, volume, volume, 1, 0, 1f)
    }

    fun release() = pool.release()

    private companion object {
        const val AUDIO_WARM_UP_MS = 40L
    }
}
