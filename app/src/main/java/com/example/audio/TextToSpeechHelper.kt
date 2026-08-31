package com.example.audio

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume

class TextToSpeechHelper(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                isInitialized = true
            } else {
                Log.e("TextToSpeechHelper", "TTS Init failed with status: $status")
            }
        }
    }

    fun speakPreview(text: String, pitch: Float = 1.0f, speechRate: Float = 1.0f) {
        if (!isInitialized || text.isBlank()) return
        tts?.apply {
            setPitch(pitch.coerceIn(0.5f, 2.0f))
            setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))
            speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_preview_${System.currentTimeMillis()}")
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Synthesizes the text to a WAV audio file on disk and returns the absolute file path.
     */
    suspend fun synthesizeToFile(
        text: String,
        pitch: Float = 1.0f,
        speechRate: Float = 1.0f
    ): String? = suspendCancellableCoroutine { continuation ->
        if (!isInitialized || text.isBlank() || tts == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        try {
            val ttsDir = File(context.filesDir, "tts_sounds")
            if (!ttsDir.exists()) ttsDir.mkdirs()

            val outputFile = File(ttsDir, "tts_${System.currentTimeMillis()}.wav")
            val utteranceId = "tts_gen_${System.currentTimeMillis()}"

            tts?.apply {
                setPitch(pitch.coerceIn(0.5f, 2.0f))
                setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))

                setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}

                    override fun onDone(id: String?) {
                        if (id == utteranceId && outputFile.exists() && outputFile.length() > 0) {
                            continuation.resume(outputFile.absolutePath)
                        } else {
                            continuation.resume(null)
                        }
                    }

                    override fun onError(id: String?) {
                        continuation.resume(null)
                    }
                })

                val params = Bundle()
                val result = synthesizeToFile(text, params, outputFile, utteranceId)
                if (result != TextToSpeech.SUCCESS) {
                    continuation.resume(null)
                }
            }
        } catch (e: Exception) {
            Log.e("TextToSpeechHelper", "Error synthesizing TTS: ${e.message}", e)
            continuation.resume(null)
        }
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            // Ignore
        }
    }
}
