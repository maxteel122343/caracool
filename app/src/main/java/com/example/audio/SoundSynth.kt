package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sin

object SoundSynth {
    private const val SAMPLE_RATE = 44100

    /**
     * Plays a pleasant synthesized melodic arpeggio for unlocking.
     */
    suspend fun playChime() = withContext(Dispatchers.Default) {
        // C5, E5, G5, C6 sweet ascending chime
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
        val noteDurSec = 0.09
        val totalSamples = (SAMPLE_RATE * (notes.size * noteDurSec + 0.15)).toInt()
        val buffer = ShortArray(totalSamples)

        for (i in notes.indices) {
            val freq = notes[i]
            val startSample = (i * noteDurSec * SAMPLE_RATE).toInt()
            val numSamples = (noteDurSec * 1.5 * SAMPLE_RATE).toInt()

            for (s in 0 until numSamples) {
                val index = startSample + s
                if (index < totalSamples) {
                    val t = s.toDouble() / SAMPLE_RATE
                    val envelope = (1.0 - (s.toDouble() / numSamples)).coerceIn(0.0, 1.0)
                    val sampleVal = (sin(2.0 * Math.PI * freq * t) * envelope * 24000).toInt()
                    buffer[index] = (buffer[index] + sampleVal).coerceIn(-32768, 32767).toShort()
                }
            }
        }
        playPcmBuffer(buffer)
    }

    /**
     * Cyber futurist unlock tone
     */
    suspend fun playCyberTone() = withContext(Dispatchers.Default) {
        val durationSec = 0.35
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(totalSamples)

        for (s in 0 until totalSamples) {
            val t = s.toDouble() / SAMPLE_RATE
            val freq = 400.0 + 1200.0 * (s.toDouble() / totalSamples)
            val envelope = (1.0 - (s.toDouble() / totalSamples))
            val wave = sin(2.0 * Math.PI * freq * t)
            val sub = sin(2.0 * Math.PI * (freq * 0.5) * t) * 0.4
            buffer[s] = ((wave + sub) * envelope * 22000).toInt().coerceIn(-32768, 32767).toShort()
        }
        playPcmBuffer(buffer)
    }

    /**
     * Bubbly pop sound
     */
    suspend fun playPopTone() = withContext(Dispatchers.Default) {
        val durationSec = 0.2
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(totalSamples)

        for (s in 0 until totalSamples) {
            val t = s.toDouble() / SAMPLE_RATE
            val freq = 800.0 * (1.0 - 0.6 * (s.toDouble() / totalSamples))
            val envelope = (1.0 - (s.toDouble() / totalSamples))
            val wave = sin(2.0 * Math.PI * freq * t)
            buffer[s] = (wave * envelope * 26000).toInt().coerceIn(-32768, 32767).toShort()
        }
        playPcmBuffer(buffer)
    }

    /**
     * Lock sound: descending subtle tone
     */
    suspend fun playLockTone() = withContext(Dispatchers.Default) {
        val durationSec = 0.22
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(totalSamples)

        for (s in 0 until totalSamples) {
            val t = s.toDouble() / SAMPLE_RATE
            val freq = 650.0 - 300.0 * (s.toDouble() / totalSamples)
            val envelope = (1.0 - (s.toDouble() / totalSamples))
            val wave = sin(2.0 * Math.PI * freq * t)
            buffer[s] = (wave * envelope * 20000).toInt().coerceIn(-32768, 32767).toShort()
        }
        playPcmBuffer(buffer)
    }

    private fun playPcmBuffer(buffer: ShortArray) {
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
        } catch (_: Exception) {}
    }
}
