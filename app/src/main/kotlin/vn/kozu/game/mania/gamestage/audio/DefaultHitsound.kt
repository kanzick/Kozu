package vn.kozu.game.mania.gamestage.audio

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Hitsound mặc định, tự sinh ra file WAV (16-bit mono PCM) để beatmap không có sample riêng vẫn có tiếng.
 * Thuần JVM.
 */
object DefaultHitsound {
    fun ensure(file: File): File {
        if (file.isFile && file.length() > 44) return file
        file.parentFile?.mkdirs()
        file.writeBytes(generate())
        return file
    }

    fun generate(sampleRate: Int = 44100, durationMs: Int = 60): ByteArray {
        val n = sampleRate * durationMs / 1000
        val pcm = ByteBuffer.allocate(n * 2).order(ByteOrder.LITTLE_ENDIAN)
        var seed = 1234567L
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            seed = seed * 6364136223846793005L + 1442695040888963407L
            val noise = ((seed ushr 40) % 2001) / 1000.0 - 1.0
            val tone = sin(2 * PI * 1800.0 * t) * exp(-t * 90.0)
            val click = noise * exp(-t * 400.0) * 0.6
            val v = ((tone * 0.7 + click) * 0.8).coerceIn(-1.0, 1.0)
            pcm.putShort((v * 32767).toInt().toShort())
        }
        val data = pcm.array()
        val out = ByteArrayOutputStream()
        val h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        h.put("RIFF".toByteArray()).putInt(36 + data.size).put("WAVE".toByteArray())
        h.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
        h.putInt(sampleRate).putInt(sampleRate * 2).putShort(2).putShort(16)
        h.put("data".toByteArray()).putInt(data.size)
        out.write(h.array()); out.write(data)
        return out.toByteArray()
    }
}
