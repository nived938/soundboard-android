package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String = "🎵",
    val colorHex: String = "#00F0FF",
    val sortOrder: Int = 0,
    val isDefault: Boolean = false
)
