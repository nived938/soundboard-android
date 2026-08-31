package com.example.audio

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

object AudioEffectsProcessor {

    enum class SoundFx(val title: String, val emoji: String, val description: String) {
        NONE("Clean (No FX)", "✨", "Original unprocessed audio"),
        ECHO("Tape Echo", "🔊", "Repeating spatial delay reflections"),
        REVERB("Studio Reverb", "🏛️", "Acoustic room ambiance decay"),
        BITCRUSH("8-Bit Arcade", "👾", "Chiptune downsampled grit & crunch"),
        ROBOT("Robotic Ring", "🤖", "Metallic ring modulation voice"),
        WARM_VINYL("Lo-Fi Warmth", "📻", "Low-pass vintage tape filter")
    }

    /**
     * Reads a 16-bit PCM WAV file, applies the selected DSP effect, and saves it to a new WAV file.
     */
    fun applyFxToWavFile(sourceWav: File, targetWav: File, fx: SoundFx): Boolean {
        try {
            if (!sourceWav.exists() || sourceWav.length() < 44) return false

            val (pcmData, sampleRate) = readPcmFromWav(sourceWav) ?: return false

            val processedPcm = when (fx) {
                SoundFx.NONE -> pcmData
                SoundFx.ECHO -> applyEcho(pcmData, sampleRate, delayMs = 180, decay = 0.55f, taps = 3)
                SoundFx.REVERB -> applyReverb(pcmData, sampleRate)
                SoundFx.BITCRUSH -> applyBitcrush(pcmData, bitDepth = 6, downsampleFactor = 3)
                SoundFx.ROBOT -> applyRobotRingMod(pcmData, sampleRate, carrierFreq = 85.0)
                SoundFx.WARM_VINYL -> applyLoFiWarmth(pcmData, sampleRate)
            }

            WavAudioGenerator.writeWavFile(targetWav, processedPcm, sampleRate)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun readPcmFromWav(wavFile: File): Pair<ShortArray, Int>? {
        return try {
            val bytes = wavFile.readBytes()
            if (bytes.size < 44) return null

            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val sampleRate = buffer.getInt(24).coerceIn(8000, 48000)
            val channels = buffer.getShort(22).toInt().coerceIn(1, 2)
            val bitsPerSample = buffer.getShort(34).toInt()

            val dataOffset = 44
            val dataBytes = bytes.size - dataOffset
            val totalSamples = dataBytes / 2

            val pcm = ShortArray(if (channels == 2) totalSamples / 2 else totalSamples)
            val pcmBuffer = ByteBuffer.wrap(bytes, dataOffset, dataBytes).order(ByteOrder.LITTLE_ENDIAN)

            if (channels == 2) {
                // Mix stereo down to mono
                for (i in pcm.indices) {
                    val left = pcmBuffer.short.toInt()
                    val right = pcmBuffer.short.toInt()
                    pcm[i] = ((left + right) / 2).toShort()
                }
            } else {
                for (i in pcm.indices) {
                    pcm[i] = pcmBuffer.short
                }
            }
            Pair(pcm, sampleRate)
        } catch (e: Exception) {
            null
        }
    }

    private fun applyEcho(pcm: ShortArray, sampleRate: Int, delayMs: Int, decay: Float, taps: Int): ShortArray {
        val delaySamples = (sampleRate * (delayMs / 1000.0)).toInt().coerceAtLeast(1)
        val extraLength = delaySamples * taps
        val output = ShortArray(pcm.size + extraLength)

        // Copy direct dry signal
        for (i in pcm.indices) {
            output[i] = pcm[i]
        }

        // Add feedback taps
        var currentDecay = decay
        for (tap in 1..taps) {
            val offset = delaySamples * tap
            for (i in pcm.indices) {
                val outIdx = i + offset
                if (outIdx < output.size) {
                    val mixed = output[outIdx] + (pcm[i] * currentDecay).toInt()
                    output[outIdx] = mixed.coerceIn(-32767, 32767).toShort()
                }
            }
            currentDecay *= decay
        }
        return output
    }

    private fun applyReverb(pcm: ShortArray, sampleRate: Int): ShortArray {
        val delays = intArrayOf(
            (sampleRate * 0.029).toInt(),
            (sampleRate * 0.037).toInt(),
            (sampleRate * 0.043).toInt(),
            (sampleRate * 0.051).toInt()
        )
        val output = ShortArray(pcm.size + (sampleRate * 0.4).toInt())

        for (i in pcm.indices) {
            output[i] = (pcm[i] * 0.7f).toInt().toShort()
        }

        for (d in delays) {
            val gain = 0.35f
            for (i in pcm.indices) {
                val outIdx = i + d
                if (outIdx < output.size) {
                    val mixed = output[outIdx] + (pcm[i] * gain).toInt()
                    output[outIdx] = mixed.coerceIn(-32767, 32767).toShort()
                }
            }
        }
        return output
    }

    private fun applyBitcrush(pcm: ShortArray, bitDepth: Int, downsampleFactor: Int): ShortArray {
        val step = (1 shl (16 - bitDepth)).coerceAtLeast(1)
        val output = ShortArray(pcm.size)

        var heldSample: Short = 0
        for (i in pcm.indices) {
            if (i % downsampleFactor == 0) {
                val raw = pcm[i].toInt()
                val crushed = ((raw / step) * step).coerceIn(-32767, 32767)
                heldSample = crushed.toShort()
            }
            output[i] = heldSample
        }
        return output
    }

    private fun applyRobotRingMod(pcm: ShortArray, sampleRate: Int, carrierFreq: Double): ShortArray {
        val output = ShortArray(pcm.size)
        for (i in pcm.indices) {
            val t = i.toDouble() / sampleRate
            val carrier = sin(2 * PI * carrierFreq * t)
            val mixed = (pcm[i] * carrier).toInt().coerceIn(-32767, 32767)
            output[i] = mixed.toShort()
        }
        return output
    }

    private fun applyLoFiWarmth(pcm: ShortArray, sampleRate: Int): ShortArray {
        val output = ShortArray(pcm.size)
        var lowPass = 0.0
        val alpha = 0.28 // Cutoff ~2.8kHz
        for (i in pcm.indices) {
            val raw = pcm[i].toDouble()
            lowPass += alpha * (raw - lowPass)
            // Soft tube saturation
            val sat = Math.tanh(lowPass / 24000.0) * 28000.0
            output[i] = sat.toInt().coerceIn(-32767, 32767).toShort()
        }
        return output
    }
}
