package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class MetronomeManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var metronomeJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _bpm = MutableStateFlow(120)
    val bpm: StateFlow<Int> = _bpm.asStateFlow()

    private val _currentBeat = MutableStateFlow(1) // 1, 2, 3, 4
    val currentBeat: StateFlow<Int> = _currentBeat.asStateFlow()

    private val _beatsPerBar = MutableStateFlow(4)
    val beatsPerBar: StateFlow<Int> = _beatsPerBar.asStateFlow()

    var volume: Float = 0.8f

    // Tap tempo timestamps
    private val tapTimestamps = mutableListOf<Long>()

    private val sampleRate = 22050
    private val highClick: ShortArray by lazy { generateClick(freq = 1500.0, durSec = 0.035) }
    private val lowClick: ShortArray by lazy { generateClick(freq = 900.0, durSec = 0.03) }

    private fun generateClick(freq: Double, durSec: Double): ShortArray {
        val numSamples = (sampleRate * durSec).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 90.0)
            val s = sin(2 * PI * freq * t) * env
            buffer[i] = (s * 28000).toInt().toShort()
        }
        return buffer
    }

    private fun playClickSound(isAccent: Boolean) {
        try {
            val pcm = if (isAccent) highClick else lowClick
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
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
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.setVolume(volume.coerceIn(0f, 1f))
            audioTrack.write(pcm, 0, pcm.size)
            audioTrack.play()
            // Clean up after playing
            scope.launch {
                delay(100)
                try {
                    audioTrack.release()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun setBpm(newBpm: Int) {
        _bpm.value = newBpm.coerceIn(40, 240)
    }

    fun setBeatsPerBar(beats: Int) {
        _beatsPerBar.value = beats.coerceIn(2, 8)
    }

    fun togglePlay() {
        if (_isPlaying.value) {
            stop()
        } else {
            start()
        }
    }

    fun start() {
        if (_isPlaying.value) return
        _isPlaying.value = true
        _currentBeat.value = 1

        metronomeJob?.cancel()
        metronomeJob = scope.launch {
            var beat = 1
            while (isActive) {
                val currentBpm = _bpm.value
                val intervalMs = (60_000L / currentBpm)
                val isAccent = (beat == 1)

                _currentBeat.value = beat
                playClickSound(isAccent)

                delay(intervalMs)
                beat = if (beat >= _beatsPerBar.value) 1 else beat + 1
            }
        }
    }

    fun stop() {
        _isPlaying.value = false
        metronomeJob?.cancel()
        metronomeJob = null
        _currentBeat.value = 1
    }

    fun registerTapTempo(): Int {
        val now = System.currentTimeMillis()
        if (tapTimestamps.isNotEmpty() && (now - tapTimestamps.last()) > 2500) {
            tapTimestamps.clear()
        }

        tapTimestamps.add(now)
        if (tapTimestamps.size > 5) {
            tapTimestamps.removeAt(0)
        }

        if (tapTimestamps.size >= 2) {
            var totalInterval = 0L
            for (i in 1 until tapTimestamps.size) {
                totalInterval += (tapTimestamps[i] - tapTimestamps[i - 1])
            }
            val avgIntervalMs = totalInterval / (tapTimestamps.size - 1)
            if (avgIntervalMs > 0) {
                val computedBpm = (60_000.0 / avgIntervalMs).toInt().coerceIn(40, 240)
                _bpm.value = computedBpm
                return computedBpm
            }
        }
        return _bpm.value
    }

    fun release() {
        stop()
    }
}
