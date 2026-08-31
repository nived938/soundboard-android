package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Data structures for Cloud Sync, backup and export/import packages.
 */
data class CloudSyncBundle(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val appIdentifier: String = "com.aistudio.soundboard",
    val categories: List<CategoryExportItem>,
    val sounds: List<SoundClipExportItem>
) {
    fun toJsonString(): String {
        val root = JSONObject()
        root.put("version", version)
        root.put("exportedAt", exportedAt)
        root.put("appIdentifier", appIdentifier)

        val catArray = JSONArray()
        categories.forEach { cat ->
            val obj = JSONObject()
            obj.put("name", cat.name)
            obj.put("icon", cat.icon)
            obj.put("colorHex", cat.colorHex)
            obj.put("sortOrder", cat.sortOrder)
            catArray.put(obj)
        }
        root.put("categories", catArray)

        val soundArray = JSONArray()
        sounds.forEach { s ->
            val obj = JSONObject()
            obj.put("title", s.title)
            obj.put("emoji", s.emoji)
            obj.put("colorHex", s.colorHex)
            obj.put("categoryName", s.categoryName)
            obj.put("presetKey", s.presetKey ?: "")
            obj.put("durationMs", s.durationMs.coerceAtMost(10000L))
            obj.put("isLooping", s.isLooping)
            obj.put("volume", s.volume.toDouble())
            obj.put("pitch", s.pitch.toDouble())
            obj.put("playbackSpeed", s.playbackSpeed.toDouble())
            obj.put("isFavorite", s.isFavorite)
            if (!s.base64AudioData.isNullOrEmpty()) {
                obj.put("base64AudioData", s.base64AudioData)
            }
            soundArray.put(obj)
        }
        root.put("sounds", soundArray)

        return root.toString(2)
    }

    companion object {
        fun fromJsonString(json: String): CloudSyncBundle {
            val root = JSONObject(json)
            val version = root.optInt("version", 1)
            val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())
            val appIdentifier = root.optString("appIdentifier", "com.aistudio.soundboard")

            val categories = mutableListOf<CategoryExportItem>()
            val catArray = root.optJSONArray("categories")
            if (catArray != null) {
                for (i in 0 until catArray.length()) {
                    val obj = catArray.getJSONObject(i)
                    categories.add(
                        CategoryExportItem(
                            name = obj.getString("name"),
                            icon = obj.optString("icon", "🎵"),
                            colorHex = obj.optString("colorHex", "#00F0FF"),
                            sortOrder = obj.optInt("sortOrder", i)
                        )
                    )
                }
            }

            val sounds = mutableListOf<SoundClipExportItem>()
            val soundArray = root.optJSONArray("sounds")
            if (soundArray != null) {
                for (i in 0 until soundArray.length()) {
                    val obj = soundArray.getJSONObject(i)
                    sounds.add(
                        SoundClipExportItem(
                            title = obj.getString("title"),
                            emoji = obj.optString("emoji", "🔊"),
                            colorHex = obj.optString("colorHex", "#00F0FF"),
                            categoryName = obj.optString("categoryName", "Memes & FX"),
                            presetKey = obj.optString("presetKey").takeIf { it.isNotEmpty() },
                            durationMs = obj.optLong("durationMs", 1000L).coerceAtMost(10000L),
                            isLooping = obj.optBoolean("isLooping", false),
                            volume = obj.optDouble("volume", 1.0).toFloat(),
                            pitch = obj.optDouble("pitch", 1.0).toFloat(),
                            playbackSpeed = obj.optDouble("playbackSpeed", 1.0).toFloat(),
                            isFavorite = obj.optBoolean("isFavorite", false),
                            base64AudioData = obj.optString("base64AudioData").takeIf { it.isNotEmpty() }
                        )
                    )
                }
            }

            return CloudSyncBundle(
                version = version,
                exportedAt = exportedAt,
                appIdentifier = appIdentifier,
                categories = categories,
                sounds = sounds
            )
        }
    }
}

data class CategoryExportItem(
    val name: String,
    val icon: String,
    val colorHex: String,
    val sortOrder: Int
)

data class SoundClipExportItem(
    val title: String,
    val emoji: String,
    val colorHex: String,
    val categoryName: String,
    val presetKey: String?,
    val durationMs: Long,
    val isLooping: Boolean,
    val volume: Float,
    val pitch: Float,
    val playbackSpeed: Float,
    val isFavorite: Boolean,
    val base64AudioData: String? = null
)
