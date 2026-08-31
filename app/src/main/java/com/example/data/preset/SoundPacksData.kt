package com.example.data.preset

import android.content.Context
import com.example.audio.WavAudioGenerator
import com.example.audio.WavAudioGenerator.SAMPLE_RATE
import com.example.data.model.CategoryEntity
import com.example.data.model.SoundClipEntity
import java.io.File
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

data class SoundPackItem(
    val key: String,
    val title: String,
    val emoji: String,
    val colorHex: String,
    val durationMs: Long,
    val isLooping: Boolean = false,
    val generator: () -> ShortArray
)

data class SoundPackDefinition(
    val packId: String,
    val title: String,
    val emoji: String,
    val description: String,
    val colorHex: String,
    val categoryName: String,
    val items: List<SoundPackItem>
)

object SoundPacksData {

    fun getAvailablePacks(): List<SoundPackDefinition> = listOf(
        SoundPackDefinition(
            packId = "retro_arcade",
            title = "8-Bit Retro Arcade",
            emoji = "🕹️",
            description = "Classic arcade coin drops, power-ups, lasers, jumps, and game sound effects",
            colorHex = "#FF5722",
            categoryName = "Retro 8-Bit",
            items = listOf(
                SoundPackItem("arcade_jump", "8-Bit Jump", "🦘", "#FF5722", 400L) { generate8BitJump() },
                SoundPackItem("arcade_powerup", "Power Up", "🍄", "#4CAF50", 800L) { generate8BitPowerUp() },
                SoundPackItem("arcade_blaster", "Plasma Blaster", "⚡", "#00BCD4", 450L) { generate8BitBlaster() },
                SoundPackItem("arcade_coin_1up", "1-Up Fanfare", "⭐", "#FFC107", 1200L) { generate8Bit1Up() },
                SoundPackItem("arcade_warp", "Warp Zone", "🌀", "#9C27B0", 700L) { generate8BitWarp() }
            )
        ),
        SoundPackDefinition(
            packId = "streamer_memes",
            title = "Streamer Meme Essentials",
            emoji = "🎤",
            description = "High-energy reaction sound effects: Dramatic Dun, Rimshot punchline, Buzzer, and Airhorn",
            colorHex = "#E91E63",
            categoryName = "Streamer FX",
            items = listOf(
                SoundPackItem("dramatic_dun", "Dramatic Dun-Dun", "😱", "#E91E63", 1600L) { generateDramaticDun() },
                SoundPackItem("rimshot", "Ba-Dum-Tss", "🥁", "#FF9800", 900L) { generateRimshot() },
                SoundPackItem("buzzer_wrong", "Buzzer (Wrong)", "❌", "#F44336", 850L) { generateBuzzer() },
                SoundPackItem("bruh_bass", "Bruh Sub Hit", "🗿", "#795548", 1100L) { generateBruhSub() },
                SoundPackItem("fanfare_tada", "Tada Fanfare", "🎉", "#FFEB3B", 1400L) { generateTada() }
            )
        ),
        SoundPackDefinition(
            packId = "lofi_beat",
            title = "Lo-Fi & Trap Beat Kit",
            emoji = "🥁",
            description = "Warm 808 sub bass, snappy rim shot, trap closed hi-hats, vinyl crackle, and acoustic snaps",
            colorHex = "#6750A4",
            categoryName = "Beat Kit",
            items = listOf(
                SoundPackItem("sub_808_deep", "Deep 808 Sub", "🔊", "#6750A4", 1200L) { generateDeep808() },
                SoundPackItem("acoustic_snap", "Finger Snap", "🫰", "#0288D1", 300L) { generateFingerSnap() },
                SoundPackItem("lofi_shaker", "Lo-Fi Shaker", "🌾", "#00897B", 1500L, isLooping = true) { generateLoFiShaker() },
                SoundPackItem("vinyl_crackle", "Vinyl Crackle Loop", "📻", "#8D6E63", 2500L, isLooping = true) { generateVinylCrackle() },
                SoundPackItem("rhodes_chord", "Chill Rhodes Chime", "🎹", "#AB47BC", 2200L) { generateRhodesChord() }
            )
        ),
        SoundPackDefinition(
            packId = "scifi_cyberpunk",
            title = "Sci-Fi & Cyberpunk Deck",
            emoji = "🚀",
            description = "Futuristic teleport beams, forcefields, quantum glitches, mech steps, and neon energy blasts",
            colorHex = "#00E5FF",
            categoryName = "Sci-Fi Cyber",
            items = listOf(
                SoundPackItem("teleport_beam", "Teleport Beam", "🌌", "#00E5FF", 1200L) { generateTeleport() },
                SoundPackItem("forcefield_hum", "Forcefield Hum", "🛡️", "#7C4DFF", 2000L, isLooping = true) { generateForcefield() },
                SoundPackItem("quantum_glitch", "Quantum Glitch", "👾", "#FF1744", 650L) { generateGlitch() },
                SoundPackItem("mech_footstep", "Mech Stomp", "🤖", "#607D8B", 800L) { generateMechStomp() },
                SoundPackItem("energy_charge", "Energy Charge-Up", "⚡", "#FFEA00", 1500L) { generateEnergyCharge() }
            )
        )
    )

    fun installSoundPack(
        context: Context,
        pack: SoundPackDefinition,
        onSoundReady: (CategoryEntity, List<SoundClipEntity>) -> Unit
    ) {
        val soundDir = File(context.filesDir, "sound_packs")
        if (!soundDir.exists()) soundDir.mkdirs()

        val category = CategoryEntity(
            name = pack.categoryName,
            icon = pack.emoji,
            colorHex = pack.colorHex,
            isDefault = false
        )

        val soundClips = mutableListOf<SoundClipEntity>()
        pack.items.forEach { item ->
            val wavFile = File(soundDir, "${item.key}.wav")
            if (!wavFile.exists() || wavFile.length() < 100) {
                try {
                    val pcm = item.generator()
                    WavAudioGenerator.writeWavFile(wavFile, pcm, SAMPLE_RATE)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            soundClips.add(
                SoundClipEntity(
                    title = item.title,
                    emoji = item.emoji,
                    colorHex = item.colorHex,
                    categoryId = 0L, // will be assigned after category insert
                    audioFilePath = wavFile.absolutePath,
                    presetKey = item.key,
                    durationMs = item.durationMs,
                    isLooping = item.isLooping
                )
            )
        }
        onSoundReady(category, soundClips)
    }

    // --- Sound Synthesis Engines for Packs ---

    private fun generate8BitJump(): ShortArray {
        val dur = 0.35f
        val samples = ShortArray((SAMPLE_RATE * dur).toInt())
        var phase = 0.0
        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 160.0 + 750.0 * (t / dur)
            phase += 2 * PI * freq / SAMPLE_RATE
            val square = if (sin(phase) >= 0) 1.0 else -1.0
            val env = 1.0 - t / dur
            samples[i] = (square * env * 24000).toInt().toShort()
        }
        return samples
    }

    private fun generate8BitPowerUp(): ShortArray {
        val dur = 0.7f
        val samples = ShortArray((SAMPLE_RATE * dur).toInt())
        val notes = listOf(330.0, 392.0, 523.0, 587.0, 659.0, 784.0)
        val noteDur = dur / notes.size
        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            val noteIdx = (t / noteDur).toInt().coerceIn(0, notes.size - 1)
            val freq = notes[noteIdx]
            val square = if (sin(2 * PI * freq * t) >= 0) 1.0 else -1.0
            samples[i] = (square * 22000).toInt().toShort()
        }
        return samples
    }

    private fun generate8BitBlaster(): ShortArray {
        val dur = 0.4f
        val samples = ShortArray((SAMPLE_RATE * dur).toInt())
        var phase = 0.0
        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 1800.0 * exp(-t * 16.0) + 120.0
            phase += 2 * PI * freq / SAMPLE_RATE
            val square = if (sin(phase) >= 0) 1.0 else -1.0
            samples[i] = (square * exp(-t * 8.0) * 26000).toInt().toShort()
        }
        return samples
    }

    private fun generate8Bit1Up(): ShortArray {
        val dur = 0.9f
        val samples = ShortArray((SAMPLE_RATE * dur).toInt())
        val notes = listOf(330.0, 392.0, 659.0, 523.0, 587.0, 784.0)
        val noteDur = dur / notes.size
        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            val noteIdx = (t / noteDur).toInt().coerceIn(0, notes.size - 1)
            val freq = notes[noteIdx]
            val noteT = t % noteDur
            val square = if (sin(2 * PI * freq * t) >= 0) 1.0 else -1.0
            val env = exp(-noteT * 5.0)
            samples[i] = (square * env * 24000).toInt().toShort()
        }
        return samples
    }

    private fun generate8BitWarp(): ShortArray {
        val dur = 0.6f
        val samples = ShortArray((SAMPLE_RATE * dur).toInt())
        var phase = 0.0
        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            val mod = sin(2 * PI * 40.0 * t) * 200.0
            val freq = 500.0 + mod + (1.0 - t / dur) * 600.0
            phase += 2 * PI * freq / SAMPLE_RATE
            val square = if (sin(phase) >= 0) 1.0 else -1.0
            samples[i] = (square * (1.0 - t / dur) * 24000).toInt().toShort()
        }
        return samples
    }

    private fun generateDramaticDun(): ShortArray {
        val dur = 1.5f
        val samples = ShortArray((SAMPLE_RATE * dur).toInt())
        val f = 73.42 // D2
        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            val s1 = sin(2 * PI * f * t)
            val s2 = sin(2 * PI * f * 2 * t) * 0.6
            val s3 = sin(2 * PI * f * 3 * t) * 0.4
            val env = exp(-t * 2.2)
            var mixed = (s1 + s2 + s3) * env
            mixed = Math.tanh(mixed * 2.0)
            samples[i] = (mixed * 29000).toInt().toShort()
        }
        return samples
    }

    private fun generateRimshot(): ShortArray {
        val dur = 0.8f
        val samples = ShortArray((SAMPLE_RATE * dur).toInt())
        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            // 2 hits: hit 1 at 0.0s, hit 2 at 0.25s, crash cymbal at 0.45s
            val s = when {
                t < 0.2 -> sin(2 * PI * 220.0 * t) * exp(-t * 20.0)
                t < 0.45 -> {
                    val t2 = t - 0.25
                    sin(2 * PI * 260.0 * t2) * exp(-t2 * 20.0)
                }
                else -> {
                    val t3 = t - 0.45
                    (Math.random() * 2.0 - 1.0) * exp(-t3 * 7.0)
                }
            }
            samples[i] = (s.coerceIn(-1.0, 1.0) * 28000).toInt().toShort()
        }
        return samples
    }

    private fun generateBuzzer(): ShortArray {
        val dur = 0.75f
        val samples = ShortArray((SAMPLE_RATE * dur).toInt())
        val f = 110.0 // A2 saw
        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            val saw = ((t * f * 2) % 2.0) - 1.0
            val env = if (t > 0.6) (dur - t) / 0.15 else 1.0
            samples[i] = (saw * env * 27000).toInt().toShort()
        }
        return samples
    }

    private fun generateBruhSub(): ShortArray {
        val dur = 1.0f
        val samples = ShortArray((SAMPLE_RATE * dur).toInt())
        var phase = 0.0
        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 55.0 * exp(-t * 2.0)
            phase += 2 * PI * freq / SAMPLE_RATE
            val env = exp(-t * 3.0)
            val s = sin(phase) * env
            samples[i] = (s * 30000).toInt().toShort()
        }
        return samples
    }

    private fun generateTada(): ShortArray {
        val dur = 1.2f
        val samples = ShortArray((SAMPLE_RATE * dur).toInt())
        // G5 -> C6
        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = if (t < 0.25) 783.99 else 1046.5
            val noteT = if (t < 0.25) t else t - 0.25
            val env = exp(-noteT * 3.5)
            val s = (sin(2 * PI * freq * t) + 0.3 * sin(4 * PI * freq * t)) * env
            samples[i] = (s.coerceIn(-1.0, 1.0) * 28000).toInt().toShort()
        }
        return samples
    }

    private fun generateDeep808(): ShortArray {
        val dur = 1.1f
        val samples = ShortArray((SAMPLE_RATE * dur).toInt())
        var phase = 0.0
        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 42.0 + 80.0 * exp(-t * 12.0)
            phase += 2 * PI * freq / SAMPLE_RATE
            val env = exp(-t * 3.2)
            var s = sin(phase) * env * 1.2
            s = Math.tanh(s)
            samples[i] = (s * 31000).toInt().toShort()
        }
        return samples
    }

    private fun generateFingerSnap(): ShortArray {
        val dur = 0.25f
        val samples = ShortArray((SAMPLE_RATE * dur).toInt())
        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            val noise = (Math.random() * 2.0 - 1.0) * exp(-t * 50.0)
            val pop = sin(2 * PI * 1200.0 * t) * exp(-t * 40.0)
            val combined = pop * 0.6 + noise * 0.4
            samples[i] = (combined.coerceIn(-1.0, 1.0) * 28000).toInt().toShort()
        }
        return samples
    }

    private fun generateLoFiShaker(): ShortArray {
        val dur = 1.4f
        val samples = ShortArray((SAMPLE_RATE * dur).toInt())
        val shakeCount = 4
        val shakeDur = dur / shakeCount
        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            val shakeT = t % shakeDur
            val env = exp(-shakeT * 22.0)
            val noise = (Math.random() * 2.0 - 1.0) * env
            samples[i] = (noise * 22000).toInt().toShort()
        }
        return samples
    }

    private fun generateVinylCrackle(): ShortArray {
        val dur = 2.4f
        val samples = ShortArray((SAMPLE_RATE * dur).toInt())
        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            val click = if (Math.random() < 0.003) (Math.random() * 2.0 - 1.0) * 0.8 else 0.0
            val hiss = (Math.random() * 2.0 - 1.0) * 0.08
            samples[i] = ((click + hiss).coerceIn(-1.0, 1.0) * 20000).toInt().toShort()
        }
        return samples
    }

    private fun generateRhodesChord(): ShortArray {
        val dur = 2.0f
        val samples = ShortArray((SAMPLE_RATE * dur).toInt())
        // Cmaj7 chord: C4 (261.63), E4 (329.63), G4 (392.0), B4 (493.88)
        val freqs = listOf(261.63, 329.63, 392.0, 493.88)
        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t * 2.0)
            var tone = 0.0
            for (f in freqs) {
                tone += (sin(2 * PI * f * t) + 0.3 * sin(4 * PI * f * t)) * 0.25
            }
            samples[i] = ((tone * env).coerceIn(-1.0, 1.0) * 28000).toInt().toShort()
        }
        return samples
    }

    private fun generateTeleport(): ShortArray {
        val dur = 1.1f
        val samples = ShortArray((SAMPLE_RATE * dur).toInt())
        var phase = 0.0
        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 300.0 + 1600.0 * (t / dur) + sin(2 * PI * 30.0 * t) * 80.0
            phase += 2 * PI * freq / SAMPLE_RATE
            val env = if (t < 0.1) t / 0.1 else if (t > 0.8) (dur - t) / 0.3 else 1.0
            samples[i] = (sin(phase) * env * 28000).toInt().toShort()
        }
        return samples
    }

    private fun generateForcefield(): ShortArray {
        val dur = 1.9f
        val samples = ShortArray((SAMPLE_RATE * dur).toInt())
        var phase = 0.0
        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 120.0 + sin(2 * PI * 4.0 * t) * 40.0
            phase += 2 * PI * freq / SAMPLE_RATE
            val hum = sin(phase) + 0.3 * sin(2 * phase)
            samples[i] = (hum * 24000).toInt().toShort()
        }
        return samples
    }

    private fun generateGlitch(): ShortArray {
        val dur = 0.6f
        val samples = ShortArray((SAMPLE_RATE * dur).toInt())
        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            val chunk = (t * 24.0).toInt()
            val freq = (chunk * 317) % 1400 + 200.0
            val s = if (sin(2 * PI * freq * t) >= 0) 0.8 else -0.8
            samples[i] = (s * 25000).toInt().toShort()
        }
        return samples
    }

    private fun generateMechStomp(): ShortArray {
        val dur = 0.75f
        val samples = ShortArray((SAMPLE_RATE * dur).toInt())
        var phase = 0.0
        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 60.0 + 240.0 * exp(-t * 20.0)
            phase += 2 * PI * freq / SAMPLE_RATE
            val metal = (Math.random() * 2.0 - 1.0) * exp(-t * 12.0) * 0.4
            val sub = sin(phase) * exp(-t * 5.0)
            val combined = sub + metal
            samples[i] = (combined.coerceIn(-1.0, 1.0) * 29000).toInt().toShort()
        }
        return samples
    }

    private fun generateEnergyCharge(): ShortArray {
        val dur = 1.4f
        val samples = ShortArray((SAMPLE_RATE * dur).toInt())
        var phase = 0.0
        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 80.0 + 1200.0 * (t * t / (dur * dur))
            phase += 2 * PI * freq / SAMPLE_RATE
            val env = if (t < 0.2) t / 0.2 else 1.0
            samples[i] = (sin(phase) * env * 28000).toInt().toShort()
        }
        return samples
    }
}
