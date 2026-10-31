package vn.kozu.game.mania.gamestage.session

import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import vn.kozu.game.mania.gamestage.audio.GameAudio
import vn.kozu.game.mania.gamestage.audio.HitSoundPlayer
import vn.kozu.game.mania.gamestage.audio.SampleLookup
import vn.kozu.game.mania.gamestage.chart.ChartNote
import vn.kozu.game.mania.gamestage.chart.ManiaChart
import vn.kozu.game.mania.gamestage.engine.GameCore
import vn.kozu.game.mania.gamestage.engine.JudgementEngine
import vn.kozu.game.mania.gamestage.media.BackgroundLoader
import vn.kozu.game.mania.gamestage.scoring.GameResult
import vn.kozu.game.mania.gamestage.scoring.Judgement
import vn.kozu.game.mania.gamestage.settings.GameSettings
import vn.kozu.game.mania.previewstage.model.DifficultyRecord
import vn.kozu.game.mania.previewstage.model.SongEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume

/**
 * A play session connecting [GameCore] (clock + judgement + score) to music, hitsounds, the background, and HUD.
 * All functions except [prepare] are called on the main thread.
 */
class GameSession(
    val song: SongEntry,
    val difficulty: DifficultyRecord,
    val chart: ManiaChart,
    val settings: GameSettings,
    private val defaultHitFile: File,
) : JudgementEngine.Listener {

    enum class Phase { LOADING, READY, PLAYING, PAUSED, FINISHED, FAILED }

    val core = GameCore(chart, settings, listener = this)
    private val audio = GameAudio()
    private val hitSounds = HitSoundPlayer()

    var phase by mutableStateOf(Phase.LOADING); private set
    var error by mutableStateOf<String?>(null); private set
    var background by mutableStateOf<ImageBitmap?>(null); private set
    var preparationLog by mutableStateOf<List<String>>(emptyList()); private set

    // HUD: updated only after a judgement, so it does not recompose every frame.
    var score by mutableLongStateOf(0L); private set
    var combo by mutableIntStateOf(0); private set
    var accuracy by mutableDoubleStateOf(1.0); private set
    var lastJudgement by mutableStateOf<Judgement?>(null); private set
    var judgementSeq by mutableIntStateOf(0); private set

    var result: GameResult? = null; private set

    private var noteSounds: Array<IntArray> = emptyArray()
    private var noteVolumes: FloatArray = FloatArray(0)
    private var lastAnchor: Any? = null
    private var audioRequestNano = 0L
    private var released = false

    suspend fun prepare() {
        try {
            logPreparation("Chart: ${difficulty.title} — ${difficulty.version}")
            logPreparation("Mode: Mania • ${chart.keys} keys • ${chart.notes.size} notes")
            logPreparation("Audio file: ${difficulty.audioFilename}")

            val audioFile = difficulty.audioFile(song.folder) ?: throw IOException("Audio file '${difficulty.audioFilename}' could not be found")
            logPreparation("Loading background image…")
            val backgroundFile = difficulty.backgroundFile(song.folder)
            background = withContext(Dispatchers.IO) {
                backgroundFile?.let {
                    runCatching { BackgroundLoader.load(it, 1280, 720) }
                        .onFailure { error -> Log.w(TAG, "Could not load background for ${difficulty.osuFile}", error) }
                        .getOrNull()
                }
            }
            logPreparation(if (background != null) "Background image ready" else "No background image")

            logPreparation("Preparing hit sounds…")
            val loadedSounds = buildSounds()
            logPreparation("Hit sounds ready: $loadedSounds files")

            logPreparation("Preparing audio…")
            val err = suspendCancellableCoroutine<String?> { cont ->
                audio.prepare(audioFile, settings.musicVolume / 100f) { if (cont.isActive) cont.resume(it) }
            }
            if (err != null) throw IOException(err)
            logPreparation("Audio ready")
            logPreparation("Preparation complete")
            phase = Phase.READY
        } catch (e: CancellationException) {
            release()
            throw e
        } catch (e: Exception) {
            logPreparation("Preparation failed: ${e.message ?: e.javaClass.simpleName}")
            error = e.message ?: e.javaClass.simpleName
            phase = Phase.FAILED
            release()
        }
    }

    fun start() {
        if (phase != Phase.READY) return
        core.begin(withAudio = true)
        phase = Phase.PLAYING
    }

    fun pause() {
        if (phase != Phase.PLAYING) return
        core.clock.pause()
        audio.pause()
        phase = Phase.PAUSED
    }

    fun resume() {
        if (phase != Phase.PAUSED) return
        lastAnchor = audio.anchor
        core.clock.resume()
        audio.resume()
        phase = Phase.PLAYING
    }

    private fun logPreparation(message: String) {
        preparationLog = preparationLog + message
    }

    private suspend fun buildSounds(): Int {
        val baseDir = File(song.folder, difficulty.osuFile).parentFile ?: song.folder
        val lookup = withContext(Dispatchers.IO) { SampleLookup(baseDir) }
        val resolved = HashMap<String, File?>()
        fun resolve(name: String): File? = resolved.getOrPut(name) {
            lookup.find(name) ?: if (name.contains("hitnormal") || name.contains('.')) defaultHitFile else null
        }
        val perNote = chart.notes.map { note -> note.sample.sampleNames().mapNotNull { resolve(it) } }
        val ids = hitSounds.load(perNote.flatten())
        hitSounds.warmUp()
        noteSounds = Array(chart.notes.size) { i -> perNote[i].mapNotNull { ids[it.absolutePath] }.toIntArray() }
        val master = settings.hitsoundVolume / 100f
        noteVolumes = FloatArray(chart.notes.size) { i -> chart.notes[i].sample.volume / 100f * master }
        return ids.size
    }

    // ------------------------------------------------------------ engine callbacks

    override fun onHit(note: ChartNote) {
        if (note.id >= noteSounds.size) return
        val volume = noteVolumes[note.id]
        for (id in noteSounds[note.id]) hitSounds.play(id, volume)
    }

    override fun onJudgement(note: ChartNote, judgement: Judgement, errorMs: Double) {
        score = core.tracker.score
        combo = core.tracker.combo
        accuracy = core.tracker.accuracy
        lastJudgement = judgement
        judgementSeq++
    }

    // ------------------------------------------------------------ per frame / input

    /** Called once per frame; returns chart time for rendering. */
    fun frame(): Double {
        if (phase != Phase.PLAYING) return core.chartTimeNow()
        val a = audio.anchor
        if (a != null && a !== lastAnchor) {
            lastAnchor = a
            val firstLock = !core.clock.isAudioLocked
            core.clock.onAudioAnchor(a)
            if (firstLock && a.precise && audioRequestNano != 0L) {
                // Measure MediaPlayer startup latency so the next start() call can be issued that much earlier.
                val audioZeroNano = a.systemNano - (a.mediaTimeMs * 1e6).toLong()
                learnedLatencyMs = ((audioZeroNano - audioRequestNano) / 1e6).coerceIn(0.0, 500.0)
            }
        }
        if (audioRequestNano == 0L && core.clock.shouldStartAudio(learnedLatencyMs)) {
            audioRequestNano = System.nanoTime()
            audio.start()
        }
        if (core.clock.isWaitingForAudio &&
            (audio.failure != null || System.nanoTime() - audioRequestNano > 3_000_000_000L)
        ) core.clock.giveUpOnAudio()

        val t = core.frame()
        if (core.isFinished(t)) finish()
        return t
    }

    /** Converts a touch event's uptimeMillis to System.nanoTime (Android's shared monotonic clock). */
    fun eventNano(uptimeMillis: Long): Long =
        System.nanoTime() - (SystemClock.uptimeMillis() - uptimeMillis) * 1_000_000L

    fun press(column: Int, uptimeMillis: Long) {
        if (phase == Phase.PLAYING) core.press(column, eventNano(uptimeMillis))
    }

    fun release(column: Int, uptimeMillis: Long) {
        if (phase == Phase.PLAYING) core.release(column, eventNano(uptimeMillis))
    }

    private fun finish() {
        if (phase != Phase.PLAYING) return
        result = core.result()
        phase = Phase.FINISHED
        release()
    }

    /** Aborts the session without a result. */
    fun abort() {
        if (phase == Phase.PLAYING || phase == Phase.PAUSED || phase == Phase.LOADING || phase == Phase.READY) {
            phase = Phase.FAILED
        }
        release()
    }

    fun release() {
        if (released) return
        released = true
        audio.release()
        hitSounds.release()
    }

    private companion object {
        const val TAG = "GameSession"
        @Volatile var learnedLatencyMs = 0.0
    }
}
