package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sound_sequences")
data class SequenceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val emoji: String = "🎹",
    val colorHex: String = "#6750A4",
    val eventsJson: String, // Comma or pipe-separated: "soundId:offsetMs,soundId2:offsetMs2"
    val totalDurationMs: Long = 4000L,
    val isLooping: Boolean = false,
    val bpm: Int = 120,
    val createdAt: Long = System.currentTimeMillis()
)

data class SequenceStep(
    val soundId: Long,
    val offsetMs: Long,
    val soundTitle: String = "",
    val emoji: String = "🔊",
    val colorHex: String = "#6750A4"
)
