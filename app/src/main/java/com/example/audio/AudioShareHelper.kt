package com.example.audio

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.SoundClipEntity
import java.io.File

object AudioShareHelper {

    fun shareAudioClip(context: Context, sound: SoundClipEntity) {
        try {
            val soundFile = File(sound.audioFilePath)
            val fileToShare = if (soundFile.exists() && soundFile.length() > 0) {
                soundFile
            } else if (!sound.presetKey.isNullOrEmpty()) {
                val presetFiles = WavAudioGenerator.ensurePresetAudioFiles(context)
                val p = presetFiles[sound.presetKey]
                if (p != null) File(p) else null
            } else {
                null
            }

            if (fileToShare == null || !fileToShare.exists()) {
                Toast.makeText(context, "Audio file not found for sharing", Toast.LENGTH_SHORT).show()
                return
            }

            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, fileToShare)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/wav"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Soundboard Clip: ${sound.title}")
                putExtra(Intent.EXTRA_TEXT, "Listen to \"${sound.title}\" sound clip!")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share \"${sound.title}\" audio clip via")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

        } catch (e: Exception) {
            Log.e("AudioShareHelper", "Error sharing audio file: ${e.message}", e)
            Toast.makeText(context, "Could not share sound: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
