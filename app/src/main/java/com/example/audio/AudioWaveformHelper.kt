package com.example.audio

import java.io.File
import kotlin.math.abs
import kotlin.math.max

object AudioWaveformHelper {

    /**
     * Extracts a downsampled normalized amplitude list (0.0f .. 1.0f) for smooth waveform UI drawing.
     */
    fun extractWaveformAmplitudes(wavFile: File, pointCount: Int = 64): List<Float> {
        try {
            val (pcmData, _) = AudioEffectsProcessor.readPcmFromWav(wavFile) ?: return defaultPoints(pointCount)
            if (pcmData.isEmpty()) return defaultPoints(pointCount)

            val chunkSize = max(1, pcmData.size / pointCount)
            val result = mutableListOf<Float>()
            var maxPeak = 1

            for (i in 0 until pointCount) {
                val start = i * chunkSize
                val end = minOf(start + chunkSize, pcmData.size)
                var peak = 0
                for (j in start until end) {
                    val sampleAbs = abs(pcmData[j].toInt())
                    if (sampleAbs > peak) peak = sampleAbs
                }
                if (peak > maxPeak) maxPeak = peak
                result.add(peak.toFloat())
            }

            return result.map { (it / maxPeak).coerceIn(0.08f, 1.0f) }
        } catch (e: Exception) {
            return defaultPoints(pointCount)
        }
    }

    private fun defaultPoints(count: Int): List<Float> {
        return (0 until count).map { 0.15f }
    }

    /**
     * Trims a WAV file between startMs and endMs, writing to targetWav.
     */
    fun trimWavFile(sourceWav: File, targetWav: File, startMs: Long, endMs: Long): Boolean {
        try {
            val (pcmData, sampleRate) = AudioEffectsProcessor.readPcmFromWav(sourceWav) ?: return false
            val startSample = (sampleRate * (startMs / 1000.0)).toInt().coerceIn(0, pcmData.size)
            val endSample = (sampleRate * (endMs / 1000.0)).toInt().coerceIn(startSample + 100, pcmData.size)

            val trimmedLength = endSample - startSample
            if (trimmedLength <= 0) return false

            val trimmedPcm = ShortArray(trimmedLength)
            System.arraycopy(pcmData, startSample, trimmedPcm, 0, trimmedLength)

            // Apply tiny 5ms fade-in and fade-out to prevent clicks/pops
            val fadeSamples = (sampleRate * 0.005).toInt().coerceAtMost(trimmedLength / 2)
            for (i in 0 until fadeSamples) {
                val fade = i.toFloat() / fadeSamples
                trimmedPcm[i] = (trimmedPcm[i] * fade).toInt().toShort()
                trimmedPcm[trimmedLength - 1 - i] = (trimmedPcm[trimmedLength - 1 - i] * fade).toInt().toShort()
            }

            WavAudioGenerator.writeWavFile(targetWav, trimmedPcm, sampleRate)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
