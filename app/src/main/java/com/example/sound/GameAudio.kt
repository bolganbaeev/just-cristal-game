package com.example.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

class GameAudio(context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var isMuted: Boolean = false

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun isMuted(): Boolean = isMuted

    fun playShoot() {
        if (isMuted) return
        scope.launch {
            generateSweep(startFreq = 800.0, endFreq = 260.0, durationMs = 60, volume = 0.35f)
        }
    }

    fun playShotgun() {
        if (isMuted) return
        scope.launch {
            generateNoiseBurst(durationMs = 90, volume = 0.45f)
        }
    }

    fun playZombieHit() {
        if (isMuted) return
        scope.launch {
            generateSweep(startFreq = 220.0, endFreq = 90.0, durationMs = 50, volume = 0.3f)
        }
    }

    fun playZombieDeath() {
        if (isMuted) return
        scope.launch {
            generateSweep(startFreq = 300.0, endFreq = 70.0, durationMs = 120, volume = 0.4f)
        }
    }

    fun playCrystalHit() {
        vibrateDamage()
        if (isMuted) return
        scope.launch {
            generateDualTone(freq1 = 880.0, freq2 = 1174.0, durationMs = 150, volume = 0.5f)
        }
    }

    fun playShardPickup() {
        if (isMuted) return
        scope.launch {
            generateArpeggio(listOf(523.0, 659.0, 784.0), noteDurationMs = 35, volume = 0.35f)
        }
    }

    fun playExplosion() {
        vibrateHeavy()
        if (isMuted) return
        scope.launch {
            generateNoiseBurst(durationMs = 200, volume = 0.6f)
        }
    }

    fun playWaveClear() {
        if (isMuted) return
        scope.launch {
            generateArpeggio(listOf(440.0, 554.0, 659.0, 880.0), noteDurationMs = 80, volume = 0.5f)
        }
    }

    fun vibrateDamage() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(45)
            }
        } catch (_: Exception) {}
    }

    fun vibrateHeavy() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(90, 200))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(90)
            }
        } catch (_: Exception) {}
    }

    private fun generateSweep(startFreq: Double, endFreq: Double, durationMs: Int, volume: Float) {
        val sampleRate = 22050
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(10)
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val currentFreq = startFreq + (endFreq - startFreq) * progress
            val angle = 2.0 * Math.PI * currentFreq * (i.toDouble() / sampleRate)
            val envelope = (1.0 - progress) * volume
            val sample = (sin(angle) * envelope * Short.MAX_VALUE).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        playPcm(buffer, sampleRate)
    }

    private fun generateDualTone(freq1: Double, freq2: Double, durationMs: Int, volume: Float) {
        val sampleRate = 22050
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(10)
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val a1 = 2.0 * Math.PI * freq1 * (i.toDouble() / sampleRate)
            val a2 = 2.0 * Math.PI * freq2 * (i.toDouble() / sampleRate)
            val envelope = (1.0 - progress) * (volume / 2.0)
            val sample = ((sin(a1) + sin(a2)) * envelope * Short.MAX_VALUE).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        playPcm(buffer, sampleRate)
    }

    private fun generateArpeggio(freqs: List<Double>, noteDurationMs: Int, volume: Float) {
        val sampleRate = 22050
        val samplesPerNote = (sampleRate * (noteDurationMs / 1000.0)).toInt()
        val totalSamples = samplesPerNote * freqs.size
        val buffer = ShortArray(totalSamples)

        for (n in freqs.indices) {
            val freq = freqs[n]
            for (i in 0 until samplesPerNote) {
                val progress = i.toDouble() / samplesPerNote
                val angle = 2.0 * Math.PI * freq * (i.toDouble() / sampleRate)
                val envelope = (1.0 - progress * 0.4) * volume
                val sample = (sin(angle) * envelope * Short.MAX_VALUE).toInt()
                buffer[n * samplesPerNote + i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
        playPcm(buffer, sampleRate)
    }

    private fun generateNoiseBurst(durationMs: Int, volume: Float) {
        val sampleRate = 22050
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(10)
        val buffer = ShortArray(numSamples)

        var lastSample = 0.0
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val white = Random.nextDouble(-1.0, 1.0)
            // Low-pass filter for explosion thud
            lastSample = (lastSample + (0.15 * white)) / 1.15
            val envelope = (1.0 - progress) * volume
            val sample = (lastSample * envelope * Short.MAX_VALUE).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        playPcm(buffer, sampleRate)
    }

    private fun playPcm(buffer: ShortArray, sampleRate: Int) {
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            track.setNotificationMarkerPosition(buffer.size)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack?) {
                    t?.release()
                }
                override fun onPeriodicNotification(t: AudioTrack?) {}
            })
        } catch (_: Exception) {}
    }
}
