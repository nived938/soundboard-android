package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sound_clips",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId"), Index("isFavorite")]
)
data class SoundClipEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val emoji: String = "🔊",
    val colorHex: String = "#00F0FF",
    val categoryId: Long,
    val audioFilePath: String, // Absolute path or internal preset key
    val presetKey: String? = null, // Preset identifier if built-in sound
    val durationMs: Long = 1000, // Clamped to max 10,000 ms (10s)
    val isLooping: Boolean = false, // Option to loop playback seamlessly
    val volume: Float = 1.0f, // 0.0f to 1.0f
    val pitch: Float = 1.0f, // 0.5f to 2.0f
    val playbackSpeed: Float = 1.0f, // 0.5f to 2.0f
    val sortOrder: Int = 0,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val activeFx: String = "NONE",
    val createdAt: Long = System.currentTimeMillis()
)
