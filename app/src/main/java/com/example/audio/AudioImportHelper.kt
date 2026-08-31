package com.example.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

data class ImportedAudioResult(
    val file: File,
    val durationMs: Long,
    val originalFileName: String
)

object AudioImportHelper {

    fun importAudioFromUri(context: Context, uri: Uri): ImportedAudioResult? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val rawDuration = durationStr?.toLongOrNull() ?: 3000L
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "Imported Clip"
            retriever.release()

            // Strict limit: 10 seconds maximum per audio file
            val clampedDuration = rawDuration.coerceAtMost(10000L)

            val importedDir = File(context.filesDir, "imported_sounds")
            if (!importedDir.exists()) importedDir.mkdirs()

            val ext = if (uri.toString().endsWith(".wav", ignoreCase = true)) "wav" else "m4a"
            val destFile = File(importedDir, "import_${System.currentTimeMillis()}.$ext")

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (destFile.exists() && destFile.length() > 0) {
                ImportedAudioResult(
                    file = destFile,
                    durationMs = clampedDuration,
                    originalFileName = title
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("AudioImportHelper", "Failed to import audio from $uri: ${e.message}", e)
            null
        }
    }
}
