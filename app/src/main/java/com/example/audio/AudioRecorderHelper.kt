package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
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

data class RecordingState(
    val isRecording: Boolean = false,
    val recordedMs: Long = 0L,
    val maxDurationMs: Long = 10000L, // Strict 10-second limit
    val amplitudes: List<Float> = emptyList(),
    val currentAmplitude: Float = 0f,
    val recordedFilePath: String? = null,
    val isFinished: Boolean = false,
    val error: String? = null
)

class AudioRecorderHelper(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _recordingState = MutableStateFlow(RecordingState())
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    fun startRecording(onFinishedCallback: ((File, Long) -> Unit)? = null) {
        if (_recordingState.value.isRecording) return

        try {
            val recordDir = File(context.filesDir, "custom_recordings")
            if (!recordDir.exists()) recordDir.mkdirs()

            val outputFile = File(recordDir, "rec_${System.currentTimeMillis()}.m4a")
            currentOutputFile = outputFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(192000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                setMaxDuration(10000) // Hardware limit 10 seconds max

                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        stopRecording(onFinishedCallback)
                    }
                }

                prepare()
                start()
            }

            mediaRecorder = recorder
            _recordingState.value = RecordingState(
                isRecording = true,
                recordedMs = 0L,
                maxDurationMs = 10000L,
                amplitudes = emptyList(),
                currentAmplitude = 0f,
                recordedFilePath = outputFile.absolutePath,
                isFinished = false
            )

            // Start amplitude & timer polling
            recordingJob?.cancel()
            recordingJob = scope.launch {
                val startTime = System.currentTimeMillis()
                val ampList = mutableListOf<Float>()

                while (isActive) {
                    val elapsed = System.currentTimeMillis() - startTime
                    if (elapsed >= 10000L) {
                        stopRecording(onFinishedCallback)
                        break
                    }

                    var ampNorm = 0f
                    try {
                        val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                        ampNorm = (maxAmp / 32767f).coerceIn(0f, 1f)
                    } catch (e: Exception) {
                        // Ignore
                    }

                    ampList.add(ampNorm)
                    if (ampList.size > 50) ampList.removeAt(0)

                    _recordingState.value = _recordingState.value.copy(
                        recordedMs = elapsed,
                        amplitudes = ampList.toList(),
                        currentAmplitude = ampNorm
                    )

                    delay(100)
                }
            }

        } catch (e: Exception) {
            Log.e("AudioRecorderHelper", "Failed to start recording: ${e.message}", e)
            _recordingState.value = RecordingState(
                isRecording = false,
                error = "Could not access microphone: ${e.localizedMessage}"
            )
            cleanup()
        }
    }

    fun stopRecording(onFinishedCallback: ((File, Long) -> Unit)? = null) {
        if (!_recordingState.value.isRecording) return

        recordingJob?.cancel()
        val finalFile = currentOutputFile
        val finalDuration = _recordingState.value.recordedMs.coerceIn(100L, 10000L)

        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    Log.w("AudioRecorderHelper", "Recorder stop exception: ${e.message}")
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorderHelper", "Error releasing recorder: ${e.message}")
        } finally {
            mediaRecorder = null
        }

        _recordingState.value = _recordingState.value.copy(
            isRecording = false,
            isFinished = true,
            recordedFilePath = finalFile?.absolutePath,
            recordedMs = finalDuration
        )

        if (finalFile != null && finalFile.exists() && finalFile.length() > 0) {
            onFinishedCallback?.invoke(finalFile, finalDuration)
        }
    }

    fun reset() {
        cleanup()
        _recordingState.value = RecordingState()
    }

    private fun cleanup() {
        recordingJob?.cancel()
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            // Ignore
        }
        mediaRecorder = null
    }
}
