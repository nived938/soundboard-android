package com.example.audio

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object WavAudioGenerator {

    const val SAMPLE_RATE = 44100

    data class PresetDefinition(
        val key: String,
        val title: String,
        val emoji: String,
        val defaultCategory: String,
        val colorHex: String,
        val defaultDurationMs: Long,
        val isDefaultLoop: Boolean = false,
        val generator: () -> ShortArray
    )

    fun getPresets(): List<PresetDefinition> = listOf(
        PresetDefinition(
            key = "airhorn",
            title = "Airhorn",
            emoji = "📢",
            defaultCategory = "Memes & FX",
            colorHex = "#FF3366",
            defaultDurationMs = 1200L,
            generator = { generateAirhorn() }
        ),
        PresetDefinition(
            key = "kick_808",
            title = "808 Kick",
            emoji = "🥁",
            defaultCategory = "Beat Machine",
            colorHex = "#00F0FF",
            defaultDurationMs = 700L,
            generator = { generate808Kick() }
        ),
        PresetDefinition(
            key = "snare",
            title = "Studio Snare",
            emoji = "🪘",
            defaultCategory = "Beat Machine",
            colorHex = "#3B82F6",
            defaultDurationMs = 500L,
            generator = { generateSnare() }
        ),
        PresetDefinition(
            key = "hihat",
            title = "Hi-Hat",
            emoji = "💿",
            defaultCategory = "Beat Machine",
            colorHex = "#A855F7",
            defaultDurationMs = 350L,
            generator = { generateHiHat() }
        ),
        PresetDefinition(
            key = "synth_bass",
            title = "Synth Bass Groove",
            emoji = "🎸",
            defaultCategory = "Beat Machine",
            colorHex = "#10B981",
            defaultDurationMs = 2000L,
            isDefaultLoop = true,
            generator = { generateSynthBass() }
        ),
        PresetDefinition(
            key = "laser",
            title = "Laser Zap",
            emoji = "🔫",
            defaultCategory = "Gaming & Chiptune",
            colorHex = "#00F0FF",
            defaultDurationMs = 600L,
            generator = { generateLaser() }
        ),
        PresetDefinition(
            key = "coin",
            title = "Coin Drop",
            emoji = "🪙",
            defaultCategory = "Gaming & Chiptune",
            colorHex = "#FFB300",
            defaultDurationMs = 550L,
            generator = { generateCoin() }
        ),
        PresetDefinition(
            key = "victory",
            title = "Level Up Fanfare",
            emoji = "🏆",
            defaultCategory = "Gaming & Chiptune",
            colorHex = "#EC4899",
            defaultDurationMs = 1800L,
            generator = { generateVictory() }
        ),
        PresetDefinition(
            key = "game_over",
            title = "Game Over",
            emoji = "💀",
            defaultCategory = "Gaming & Chiptune",
            colorHex = "#EF4444",
            defaultDurationMs = 1600L,
            generator = { generateGameOver() }
        ),
        PresetDefinition(
            key = "dj_scratch",
            title = "DJ Scratch",
            emoji = "🎧",
            defaultCategory = "Studio & DJ",
            colorHex = "#7C3AED",
            defaultDurationMs = 900L,
            generator = { generateDjScratch() }
        ),
        PresetDefinition(
            key = "rewind",
            title = "Tape Rewind",
            emoji = "⏪",
            defaultCategory = "Studio & DJ",
            colorHex = "#F97316",
            defaultDurationMs = 1100L,
            generator = { generateRewind() }
        ),
        PresetDefinition(
            key = "epic_horn",
            title = "Cinematic Braam",
            emoji = "📯",
            defaultCategory = "Memes & FX",
            colorHex = "#EAB308",
            defaultDurationMs = 2400L,
            generator = { generateEpicHorn() }
        ),
        PresetDefinition(
            key = "applause",
            title = "Crowd Applause",
            emoji = "👏",
            defaultCategory = "Memes & FX",
            colorHex = "#14B8A6",
            defaultDurationMs = 3000L,
            isDefaultLoop = true,
            generator = { generateApplause() }
        ),
        PresetDefinition(
            key = "siren",
            title = "Emergency Siren",
            emoji = "🚨",
            defaultCategory = "Memes & FX",
            colorHex = "#EF4444",
            defaultDurationMs = 2200L,
            isDefaultLoop = true,
            generator = { generateSiren() }
        ),
        PresetDefinition(
            key = "bell",
            title = "Crystal Bell",
            emoji = "🔔",
            defaultCategory = "Studio & DJ",
            colorHex = "#6366F1",
            defaultDurationMs = 2500L,
            generator = { generateCrystalBell() }
        ),
        PresetDefinition(
            key = "boing",
            title = "Cartoon Boing",
            emoji = "🪀",
            defaultCategory = "Memes & FX",
            colorHex = "#EC4899",
            defaultDurationMs = 800L,
            generator = { generateBoing() }
        )
    )

    fun ensurePresetAudioFiles(context: Context): Map<String, String> {
        val soundDir = File(context.filesDir, "sound_presets")
        if (!soundDir.exists()) {
            soundDir.mkdirs()
        }

        val map = mutableMapOf<String, String>()
        getPresets().forEach { preset ->
            val wavFile = File(soundDir, "${preset.key}.wav")
            if (!wavFile.exists() || wavFile.length() < 100) {
                try {
                    val pcmData = preset.generator()
                    writeWavFile(wavFile, pcmData, SAMPLE_RATE)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            map[preset.key] = wavFile.absolutePath
        }
        return map
    }

    fun writeWavFile(file: File, pcmData: ShortArray, sampleRate: Int) {
        val totalAudioLen = pcmData.size * 2
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = sampleRate * channels * 2

        val header = ByteArray(44)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF header
        buffer.put('R'.code.toByte())
        buffer.put('I'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.putInt(totalDataLen)
        buffer.put('W'.code.toByte())
        buffer.put('A'.code.toByte())
        buffer.put('V'.code.toByte())
        buffer.put('E'.code.toByte())

        // fmt chunk
        buffer.put('f'.code.toByte())
        buffer.put('m'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put(' '.code.toByte())
        buffer.putInt(16) // Subchunk1Size for PCM
        buffer.putShort(1) // AudioFormat 1 = PCM
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort((channels * 2).toShort()) // block align
        buffer.putShort(16) // bits per sample

        // data chunk
        buffer.put('d'.code.toByte())
        buffer.put('a'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put('a'.code.toByte())
        buffer.putInt(totalAudioLen)

        FileOutputStream(file).use { fos ->
            fos.write(header)
            val byteBuffer = ByteBuffer.allocate(pcmData.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (sample in pcmData) {
                byteBuffer.putShort(sample)
            }
            fos.write(byteBuffer.array())
        }
    }

    // --- Audio Synthesis Generators ---

    private fun generateAirhorn(): ShortArray {
        val durationSec = 1.1f
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(numSamples)

        val f1 = 466.16 // Bb4
        val f2 = 587.33 // D5
        val f3 = 700.0  // F5

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            // Air horn flutter/vibrato
            val vib = 1.0 + 0.03 * sin(2 * PI * 35.0 * t)
            // Envelope: fast attack, sustained blast, rapid decay
            val env = when {
                t < 0.04 -> t / 0.04
                t > 0.9 -> (durationSec - t) / 0.2
                else -> 1.0
            }
            // Rich multi-harmonic brass sum
            val s1 = sin(2 * PI * f1 * vib * t) + 0.5 * sin(4 * PI * f1 * vib * t)
            val s2 = sin(2 * PI * f2 * vib * t) + 0.4 * sin(4 * PI * f2 * vib * t)
            val s3 = sin(2 * PI * f3 * vib * t) + 0.3 * sin(4 * PI * f3 * vib * t)
            val combined = (s1 * 0.4 + s2 * 0.35 + s3 * 0.25) * env
            samples[i] = (combined.coerceIn(-1.0, 1.0) * 29000).toInt().toShort()
        }
        return samples
    }

    private fun generate808Kick(): ShortArray {
        val durationSec = 0.65f
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(numSamples)

        var phase = 0.0
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            // Exponential pitch sweep from 170 Hz down to 48 Hz
            val freq = 48.0 + 122.0 * exp(-t * 18.0)
            phase += 2 * PI * freq / SAMPLE_RATE
            // Amplitude envelope
            val env = exp(-t * 5.2)
            // Soft saturation overdrive
            var s = sin(phase) * env * 1.3
            s = Math.tanh(s)
            samples[i] = (s * 30000).toInt().toShort()
        }
        return samples
    }

    private fun generateSnare(): ShortArray {
        val durationSec = 0.45f
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(numSamples)

        var phase = 0.0
        var prevNoise = 0.0
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            // Snare tone body (185Hz drop)
            val toneFreq = 185.0 * exp(-t * 25.0)
            phase += 2 * PI * toneFreq / SAMPLE_RATE
            val tone = sin(phase) * exp(-t * 15.0) * 0.5

            // Snare noise crackle (high-pass filtered random)
            val white = (Math.random() * 2.0 - 1.0)
            val highPassNoise = white - prevNoise * 0.7
            prevNoise = white
            val noiseEnv = exp(-t * 9.0)

            val combined = (tone + highPassNoise * noiseEnv * 0.7)
            samples[i] = (combined.coerceIn(-1.0, 1.0) * 28000).toInt().toShort()
        }
        return samples
    }

    private fun generateHiHat(): ShortArray {
        val durationSec = 0.3f
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(numSamples)

        var prev = 0.0
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val noise = (Math.random() * 2.0 - 1.0)
            // Metallic ring tones + high-pass noise
            val metallic = sin(2 * PI * 6500.0 * t) * 0.2 + sin(2 * PI * 8200.0 * t) * 0.2
            val hp = (noise - prev * 0.85) + metallic
            prev = noise
            val env = exp(-t * 28.0)
            samples[i] = ((hp * env).coerceIn(-1.0, 1.0) * 26000).toInt().toShort()
        }
        return samples
    }

    private fun generateSynthBass(): ShortArray {
        val durationSec = 2.0f
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(numSamples)

        // 4-note repeating bass pattern: A1 (55Hz), C2 (65.4Hz), D2 (73.4Hz), E2 (82.4Hz)
        val pattern = listOf(55.0, 65.4, 73.4, 82.4)
        val noteDur = durationSec / pattern.size

        var phase = 0.0
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val noteIndex = (t / noteDur).toInt().coerceIn(0, pattern.size - 1)
            val freq = pattern[noteIndex]
            val noteT = t % noteDur

            phase += 2 * PI * freq / SAMPLE_RATE
            // Sawtooth + Sub Sine
            val saw = ((phase / PI) % 2.0) - 1.0
            val sub = sin(phase)
            // Filter envelope
            val filterMod = exp(-noteT * 6.0)
            val env = if (noteT < 0.02) noteT / 0.02 else exp(-noteT * 2.5)

            val combined = (saw * (0.4 + 0.4 * filterMod) + sub * 0.5) * env
            samples[i] = (combined.coerceIn(-1.0, 1.0) * 28000).toInt().toShort()
        }
        return samples
    }

    private fun generateLaser(): ShortArray {
        val durationSec = 0.5f
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(numSamples)

        var phase = 0.0
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            // Frequency sweep 2800Hz down to 180Hz exponentially
            val freq = 180.0 + 2620.0 * exp(-t * 14.0)
            phase += 2 * PI * freq / SAMPLE_RATE
            val env = exp(-t * 5.5)
            val s = sin(phase) * env
            samples[i] = (s * 30000).toInt().toShort()
        }
        return samples
    }

    private fun generateCoin(): ShortArray {
        val durationSec = 0.5f
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            // First note B5 (987.77 Hz) for 0.09s, then E6 (1318.51 Hz)
            val freq = if (t < 0.09) 987.77 else 1318.51
            val noteT = if (t < 0.09) t else t - 0.09
            val env = exp(-noteT * 7.0)
            // Pure crystal bell tone with 2nd harmonic
            val s = (sin(2 * PI * freq * t) + 0.25 * sin(4 * PI * freq * t)) * env
            samples[i] = (s.coerceIn(-1.0, 1.0) * 29000).toInt().toShort()
        }
        return samples
    }

    private fun generateVictory(): ShortArray {
        val durationSec = 1.7f
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(numSamples)

        // Arpeggio: C5 (523.25), E5 (659.25), G5 (783.99), C6 (1046.5)
        val notes = listOf(523.25, 659.25, 783.99, 1046.5)
        val noteDur = 0.18

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val noteIdx = (t / noteDur).toInt().coerceAtMost(notes.size - 1)
            val freq = notes[noteIdx]
            val isFinal = noteIdx == notes.size - 1
            val noteT = if (isFinal) t - (noteDur * (notes.size - 1)) else (t % noteDur)

            val env = if (isFinal) exp(-noteT * 1.8) else exp(-noteT * 4.0)
            val square = if (sin(2 * PI * freq * t) >= 0) 0.6 else -0.6
            val sine = sin(2 * PI * freq * t)
            val combined = (square * 0.4 + sine * 0.6) * env
            samples[i] = (combined.coerceIn(-1.0, 1.0) * 27000).toInt().toShort()
        }
        return samples
    }

    private fun generateGameOver(): ShortArray {
        val durationSec = 1.5f
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(numSamples)

        // Descending sad chime: F4 (349.23), D4 (293.66), C4 (261.63), A3 (220.0)
        val notes = listOf(349.23, 293.66, 261.63, 220.0)
        val noteDur = 0.35

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val noteIdx = (t / noteDur).toInt().coerceAtMost(notes.size - 1)
            val freq = notes[noteIdx]
            val noteT = t % noteDur
            val env = exp(-noteT * 3.5)
            val tone = sin(2 * PI * freq * t) * 0.7 + sin(4 * PI * freq * t) * 0.3
            samples[i] = ((tone * env).coerceIn(-1.0, 1.0) * 28000).toInt().toShort()
        }
        return samples
    }

    private fun generateDjScratch(): ShortArray {
        val durationSec = 0.85f
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(numSamples)

        var phase = 0.0
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            // Frequency wobbles forward and backward 400Hz -> 1800Hz -> 200Hz -> 1200Hz
            val scratchMod = sin(2 * PI * 4.0 * t) + sin(2 * PI * 9.0 * t) * 0.5
            val freq = (750.0 + 650.0 * scratchMod).coerceAtLeast(100.0)
            phase += 2 * PI * freq / SAMPLE_RATE

            val noise = (Math.random() * 2.0 - 1.0) * 0.2
            val vinyl = (sin(phase) + noise) * (1.0 - t / durationSec)
            samples[i] = (vinyl.coerceIn(-1.0, 1.0) * 28000).toInt().toShort()
        }
        return samples
    }

    private fun generateRewind(): ShortArray {
        val durationSec = 1.0f
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(numSamples)

        var phase = 0.0
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            // Ascending pitch speed with flutter
            val flutter = 1.0 + 0.15 * sin(2 * PI * 30.0 * t)
            val freq = (250.0 + 2200.0 * (t * t)) * flutter
            phase += 2 * PI * freq / SAMPLE_RATE
            val env = if (t < 0.1) t / 0.1 else if (t > 0.8) (1.0 - t) / 0.2 else 1.0
            val s = sin(phase) * env
            samples[i] = (s * 28000).toInt().toShort()
        }
        return samples
    }

    private fun generateEpicHorn(): ShortArray {
        val durationSec = 2.3f
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(numSamples)

        val f = 65.41 // C2 low braam
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = when {
                t < 0.15 -> t / 0.15
                t > 1.6 -> (durationSec - t) / 0.7
                else -> 1.0
            }
            // Rich multi-octave brass harmonics with subtle low-frequency modulation
            val s1 = sin(2 * PI * f * t)
            val s2 = sin(2 * PI * (f * 2) * t) * 0.7
            val s3 = sin(2 * PI * (f * 3) * t) * 0.5
            val s4 = sin(2 * PI * (f * 4) * t) * 0.3
            var combined = (s1 + s2 + s3 + s4) * env * 0.6
            // Saturation for that trailer impact feel
            combined = Math.tanh(combined * 1.6)
            samples[i] = (combined * 31000).toInt().toShort()
        }
        return samples
    }

    private fun generateApplause(): ShortArray {
        val durationSec = 2.8f
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(numSamples)

        var lpf = 0.0
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = if (t < 0.4) t / 0.4 else if (t > 2.2) (durationSec - t) / 0.6 else 1.0
            // Claps simulated via random impulses overlaid on crowd murmur
            val murmur = (Math.random() * 2.0 - 1.0)
            lpf += (murmur - lpf) * 0.25
            val clapChance = if (Math.random() < 0.015) (Math.random() * 2.0 - 1.0) * 2.0 else 0.0
            val combined = (lpf * 0.6 + clapChance * 0.4) * env
            samples[i] = (combined.coerceIn(-1.0, 1.0) * 26000).toInt().toShort()
        }
        return samples
    }

    private fun generateSiren(): ShortArray {
        val durationSec = 2.1f
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(numSamples)

        var phase = 0.0
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            // 2 Hz oscillation between 650Hz and 950Hz
            val freq = 800.0 + 180.0 * sin(2 * PI * 1.8 * t)
            phase += 2 * PI * freq / SAMPLE_RATE
            val env = if (t < 0.05) t / 0.05 else if (t > 1.9) (durationSec - t) / 0.2 else 1.0
            val s = (sin(phase) + 0.3 * sin(2 * phase)) * env * 0.8
            samples[i] = (s.coerceIn(-1.0, 1.0) * 28000).toInt().toShort()
        }
        return samples
    }

    private fun generateCrystalBell(): ShortArray {
        val durationSec = 2.4f
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(numSamples)

        val fundamental = 880.0 // A5
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val env1 = exp(-t * 2.0)
            val env2 = exp(-t * 3.5)
            val env3 = exp(-t * 5.0)

            val s1 = sin(2 * PI * fundamental * t) * env1
            val s2 = sin(2 * PI * (fundamental * 2.76) * t) * 0.4 * env2
            val s3 = sin(2 * PI * (fundamental * 5.4) * t) * 0.2 * env3

            val combined = s1 + s2 + s3
            samples[i] = (combined.coerceIn(-1.0, 1.0) * 29000).toInt().toShort()
        }
        return samples
    }

    private fun generateBoing(): ShortArray {
        val durationSec = 0.75f
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(numSamples)

        var phase = 0.0
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val vibrato = sin(2 * PI * 22.0 * t) * 70.0
            val freq = (220.0 + 400.0 * (t / durationSec) + vibrato).coerceAtLeast(80.0)
            phase += 2 * PI * freq / SAMPLE_RATE
            val env = exp(-t * 3.5)
            val s = sin(phase) * env
            samples[i] = (s * 29000).toInt().toShort()
        }
        return samples
    }
}
