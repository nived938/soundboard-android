package com.example.audio

import com.example.data.model.SequenceEntity
import com.example.data.model.SequenceStep
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

data class SequencePlaybackState(
    val isPlaying: Boolean = false,
    val activeSequenceId: Long? = null,
    val currentMs: Long = 0L,
    val totalMs: Long = 4000L,
    val progressFraction: Float = 0f,
    val activeStepIndex: Int = -1
)

class SequencePlayerManager(
    private val playbackManager: SoundPlaybackManager
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var playbackJob: Job? = null

    // Recording State
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private var recordStartTime: Long = 0L
    private val _recordedSteps = MutableStateFlow<List<SequenceStep>>(emptyList())
    val recordedSteps: StateFlow<List<SequenceStep>> = _recordedSteps.asStateFlow()

    // Playback State
    private val _playbackState = MutableStateFlow(SequencePlaybackState())
    val playbackState: StateFlow<SequencePlaybackState> = _playbackState.asStateFlow()

    fun startRecording() {
        playbackJob?.cancel()
        _isRecording.value = true
        recordStartTime = System.currentTimeMillis()
        _recordedSteps.value = emptyList()
    }

    fun recordPadHit(sound: SoundClipEntity) {
        if (!_isRecording.value) return
        val offset = System.currentTimeMillis() - recordStartTime
        val step = SequenceStep(
            soundId = sound.id,
            offsetMs = offset,
            soundTitle = sound.title,
            emoji = sound.emoji,
            colorHex = sound.colorHex
        )
        _recordedSteps.value = _recordedSteps.value + step
    }

    fun stopRecording(): List<SequenceStep> {
        _isRecording.value = false
        return _recordedSteps.value
    }

    fun playSequence(
        sequence: SequenceEntity,
        allSoundsMap: Map<Long, SoundClipEntity>
    ) {
        stopPlayback()

        val steps = parseSteps(sequence.eventsJson, allSoundsMap)
        if (steps.isEmpty()) return

        val totalDuration = sequence.totalDurationMs.coerceAtLeast(1000L)
        _playbackState.value = SequencePlaybackState(
            isPlaying = true,
            activeSequenceId = sequence.id,
            currentMs = 0L,
            totalMs = totalDuration,
            progressFraction = 0f,
            activeStepIndex = -1
        )

        playbackJob = scope.launch {
            do {
                val startLoopTime = System.currentTimeMillis()
                val triggeredSet = mutableSetOf<Int>()

                while (isActive) {
                    val elapsed = System.currentTimeMillis() - startLoopTime
                    if (elapsed >= totalDuration) break

                    // Trigger due steps
                    steps.forEachIndexed { idx, step ->
                        if (!triggeredSet.contains(idx) && elapsed >= step.offsetMs) {
                            triggeredSet.add(idx)
                            val sound = allSoundsMap[step.soundId]
                            if (sound != null) {
                                playbackManager.playSound(sound)
                            }
                            _playbackState.value = _playbackState.value.copy(
                                activeStepIndex = idx
                            )
                        }
                    }

                    _playbackState.value = _playbackState.value.copy(
                        currentMs = elapsed,
                        progressFraction = (elapsed.toFloat() / totalDuration).coerceIn(0f, 1f)
                    )

                    delay(20)
                }
            } while (isActive && sequence.isLooping)

            stopPlayback()
        }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        _playbackState.value = SequencePlaybackState()
    }

    companion object {
        fun serializeSteps(steps: List<SequenceStep>): String {
            return steps.joinToString(";") { "${it.soundId},${it.offsetMs}" }
        }

        fun parseSteps(
            json: String,
            allSoundsMap: Map<Long, SoundClipEntity>
        ): List<SequenceStep> {
            if (json.isBlank()) return emptyList()
            return try {
                json.split(";").mapNotNull { entry ->
                    val parts = entry.split(",")
                    if (parts.size == 2) {
                        val id = parts[0].toLongOrNull() ?: return@mapNotNull null
                        val offset = parts[1].toLongOrNull() ?: return@mapNotNull null
                        val sound = allSoundsMap[id]
                        SequenceStep(
                            soundId = id,
                            offsetMs = offset,
                            soundTitle = sound?.title ?: "Pad #$id",
                            emoji = sound?.emoji ?: "🔊",
                            colorHex = sound?.colorHex ?: "#6750A4"
                        )
                    } else null
                }.sortedBy { it.offsetMs }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
