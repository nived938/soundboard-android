package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.data.model.SoundClipEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class PlaybackStatus(
    val soundId: Long,
    val isPlaying: Boolean,
    val isLooping: Boolean,
    val progressFraction: Float = 0f,
    val currentMs: Int = 0,
    val durationMs: Int = 0
)

class SoundPlaybackManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val activePlayers = ConcurrentHashMap<Long, MediaPlayer>()
    private val _playbackStates = MutableStateFlow<Map<Long, PlaybackStatus>>(emptyMap())
    val playbackStates: StateFlow<Map<Long, PlaybackStatus>> = _playbackStates.asStateFlow()

    private val _activeCount = MutableStateFlow(0)
    val activeCount: StateFlow<Int> = _activeCount.asStateFlow()

    var masterVolume: Float = 1.0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            // Update all active players
            activePlayers.forEach { (id, player) ->
                val soundVolume = soundVolumeMap[id] ?: 1.0f
                val finalVol = soundVolume * field
                try {
                    player.setVolume(finalVol, finalVol)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }

    var hapticsEnabled: Boolean = true

    private val soundVolumeMap = ConcurrentHashMap<Long, Float>()
    private val mutedChannels = ConcurrentHashMap<Long, Boolean>()
    private var progressTrackingJob: Job? = null
    private var fadeJob: Job? = null

    private val _channelVolumes = MutableStateFlow<Map<Long, Float>>(emptyMap())
    val channelVolumes: StateFlow<Map<Long, Float>> = _channelVolumes.asStateFlow()

    private val _mutedState = MutableStateFlow<Set<Long>>(emptySet())
    val mutedState: StateFlow<Set<Long>> = _mutedState.asStateFlow()

    fun setChannelVolume(soundId: Long, vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        soundVolumeMap[soundId] = clamped
        val updated = _channelVolumes.value.toMutableMap()
        updated[soundId] = clamped
        _channelVolumes.value = updated

        val player = activePlayers[soundId]
        if (player != null && !(mutedChannels[soundId] ?: false)) {
            val finalVol = clamped * masterVolume
            try {
                player.setVolume(finalVol, finalVol)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun toggleMute(soundId: Long) {
        val currentlyMuted = mutedChannels[soundId] ?: false
        val newMute = !currentlyMuted
        mutedChannels[soundId] = newMute

        val player = activePlayers[soundId]
        if (player != null) {
            val targetVol = if (newMute) 0f else (soundVolumeMap[soundId] ?: 1f) * masterVolume
            try {
                player.setVolume(targetVol, targetVol)
            } catch (e: Exception) {
                // Ignore
            }
        }
        val set = _mutedState.value.toMutableSet()
        if (newMute) set.add(soundId) else set.remove(soundId)
        _mutedState.value = set
    }

    fun soloChannel(soundId: Long) {
        activePlayers.keys.forEach { id ->
            val shouldMute = (id != soundId)
            mutedChannels[id] = shouldMute
            val player = activePlayers[id]
            if (player != null) {
                val targetVol = if (shouldMute) 0f else (soundVolumeMap[id] ?: 1f) * masterVolume
                try {
                    player.setVolume(targetVol, targetVol)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
        val set = activePlayers.keys.filter { it != soundId }.toSet()
        _mutedState.value = set
    }

    fun fadeAllOut(durationMs: Long = 1500L) {
        fadeJob?.cancel()
        fadeJob = scope.launch {
            val steps = 20
            val interval = durationMs / steps
            val originalMaster = masterVolume

            for (i in steps downTo 0) {
                val fraction = i.toFloat() / steps
                masterVolume = originalMaster * fraction
                delay(interval)
            }
            stopAll()
            masterVolume = originalMaster
        }
    }

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    init {
        startProgressTracker()
    }

    private fun triggerHaptic(strong: Boolean = false) {
        if (!hapticsEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = if (strong) {
                    VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE)
                } else {
                    VibrationEffect.createOneShot(20, 150)
                }
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(if (strong) 45 else 20)
            }
        } catch (e: Exception) {
            // Ignore haptic failures
        }
    }

    fun playSound(sound: SoundClipEntity, overrideLoop: Boolean? = null) {
        val soundId = sound.id
        val shouldLoop = overrideLoop ?: sound.isLooping

        // If sound is already playing, stop or toggle
        if (activePlayers.containsKey(soundId)) {
            val existing = activePlayers[soundId]
            if (existing?.isPlaying == true) {
                // If user clicks playing sound, toggle stop
                stopSound(soundId)
                return
            }
        }

        try {
            triggerHaptic(strong = shouldLoop)

            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .build()
                )

                // Resolve file path
                val file = File(sound.audioFilePath)
                if (file.exists()) {
                    setDataSource(context, Uri.fromFile(file))
                } else if (!sound.presetKey.isNullOrEmpty()) {
                    // Try to regenerate preset if file got cleaned up
                    val presetFiles = WavAudioGenerator.ensurePresetAudioFiles(context)
                    val generatedPath = presetFiles[sound.presetKey]
                    if (generatedPath != null && File(generatedPath).exists()) {
                        setDataSource(context, Uri.fromFile(File(generatedPath)))
                    } else {
                        Log.e("SoundPlaybackManager", "File not found: ${sound.audioFilePath}")
                        return
                    }
                } else {
                    Log.e("SoundPlaybackManager", "File not found: ${sound.audioFilePath}")
                    return
                }

                // Seamless looping
                isLooping = shouldLoop

                prepare()

                // Set Volume
                val vol = (sound.volume * masterVolume).coerceIn(0f, 1f)
                setVolume(vol, vol)
                soundVolumeMap[soundId] = sound.volume

                // Set Pitch and Speed if supported
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        val params = PlaybackParams().apply {
                            pitch = sound.pitch.coerceIn(0.5f, 2.0f)
                            speed = sound.playbackSpeed.coerceIn(0.5f, 2.0f)
                        }
                        playbackParams = params
                    } catch (e: Exception) {
                        Log.w("SoundPlaybackManager", "PlaybackParams not supported: ${e.message}")
                    }
                }

                setOnCompletionListener { mp ->
                    if (!mp.isLooping) {
                        stopSound(soundId)
                    }
                }

                setOnErrorListener { _, what, extra ->
                    Log.e("SoundPlaybackManager", "MediaPlayer error: what=$what, extra=$extra")
                    stopSound(soundId)
                    true
                }
            }

            activePlayers[soundId] = player
            player.start()

            val currentMap = _playbackStates.value.toMutableMap()
            currentMap[soundId] = PlaybackStatus(
                soundId = soundId,
                isPlaying = true,
                isLooping = shouldLoop,
                progressFraction = 0f,
                currentMs = 0,
                durationMs = player.duration.coerceAtMost(10000)
            )
            _playbackStates.value = currentMap
            _activeCount.value = activePlayers.size

        } catch (e: Exception) {
            Log.e("SoundPlaybackManager", "Failed to play sound $soundId: ${e.message}", e)
            stopSound(soundId)
        }
    }

    fun toggleLoop(sound: SoundClipEntity) {
        val soundId = sound.id
        val player = activePlayers[soundId]
        if (player != null && player.isPlaying) {
            val newLoopState = !player.isLooping
            player.isLooping = newLoopState
            val currentStatus = _playbackStates.value[soundId]
            if (currentStatus != null) {
                val updated = _playbackStates.value.toMutableMap()
                updated[soundId] = currentStatus.copy(isLooping = newLoopState)
                _playbackStates.value = updated
            }
            triggerHaptic(strong = newLoopState)
        } else {
            // Start playback in loop mode
            playSound(sound, overrideLoop = true)
        }
    }

    fun stopSound(soundId: Long) {
        try {
            val player = activePlayers.remove(soundId)
            soundVolumeMap.remove(soundId)
            player?.apply {
                try {
                    if (isPlaying) {
                        stop()
                    }
                } catch (e: Exception) {
                    // Ignore
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("SoundPlaybackManager", "Error stopping sound $soundId: ${e.message}")
        } finally {
            val updated = _playbackStates.value.toMutableMap()
            updated.remove(soundId)
            _playbackStates.value = updated
            _activeCount.value = activePlayers.size
        }
    }

    fun stopAll() {
        triggerHaptic(strong = true)
        activePlayers.forEach { (id, player) ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
        activePlayers.clear()
        soundVolumeMap.clear()
        _playbackStates.value = emptyMap()
        _activeCount.value = 0
    }

    private fun startProgressTracker() {
        progressTrackingJob?.cancel()
        progressTrackingJob = scope.launch {
            while (isActive) {
                if (activePlayers.isNotEmpty()) {
                    val updated = _playbackStates.value.toMutableMap()
                    var changed = false
                    activePlayers.forEach { (id, player) ->
                        try {
                            if (player.isPlaying) {
                                val current = player.currentPosition
                                val dur = player.duration.coerceAtLeast(1)
                                val fraction = (current.toFloat() / dur).coerceIn(0f, 1f)
                                val status = updated[id]
                                if (status != null) {
                                    updated[id] = status.copy(
                                        progressFraction = fraction,
                                        currentMs = current,
                                        durationMs = dur
                                    )
                                    changed = true
                                }
                            }
                        } catch (e: Exception) {
                            // Player may have completed or released
                        }
                    }
                    if (changed) {
                        _playbackStates.value = updated
                    }
                }
                delay(60) // ~16 fps progress updates for smooth waveform
            }
        }
    }

    fun release() {
        stopAll()
        progressTrackingJob?.cancel()
    }
}
