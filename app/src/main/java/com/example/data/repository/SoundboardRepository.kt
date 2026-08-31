package com.example.data.repository

import android.content.Context
import com.example.audio.WavAudioGenerator
import com.example.data.local.SoundboardDao
import com.example.data.local.SoundboardDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.SoundClipEntity
import com.example.data.sync.CloudSyncInfo
import com.example.data.sync.CloudSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

class SoundboardRepository(
    private val context: Context,
    private val database: SoundboardDatabase = SoundboardDatabase.getDatabase(context)
) {
    private val dao: SoundboardDao = database.soundboardDao()
    val cloudSyncManager = CloudSyncManager(context, dao)

    val allCategories: Flow<List<CategoryEntity>> = dao.getAllCategories()
    val allSounds: Flow<List<SoundClipEntity>> = dao.getAllSounds()
    val favoriteSounds: Flow<List<SoundClipEntity>> = dao.getFavoriteSounds()
    val allSequences: Flow<List<com.example.data.model.SequenceEntity>> = dao.getAllSequences()
    val cloudSyncInfo: StateFlow<CloudSyncInfo> = cloudSyncManager.syncInfo

    suspend fun incrementPlayCount(soundId: Long) = withContext(Dispatchers.IO) {
        dao.incrementPlayCount(soundId)
    }

    suspend fun updateTrimAndFx(soundId: Long, startMs: Long, endMs: Long, activeFx: String) = withContext(Dispatchers.IO) {
        dao.updateTrimAndFx(soundId, startMs, endMs, activeFx)
    }

    suspend fun getTotalPlays(): Int = withContext(Dispatchers.IO) {
        dao.getTotalPlays() ?: 0
    }

    suspend fun insertSequence(sequence: com.example.data.model.SequenceEntity): Long = withContext(Dispatchers.IO) {
        dao.insertSequence(sequence)
    }

    suspend fun deleteSequence(sequence: com.example.data.model.SequenceEntity) = withContext(Dispatchers.IO) {
        dao.deleteSequence(sequence)
    }

    suspend fun installSoundPack(pack: com.example.data.preset.SoundPackDefinition) = withContext(Dispatchers.IO) {
        com.example.data.preset.SoundPacksData.installSoundPack(context, pack) { category, sounds ->
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                val catId = dao.insertCategory(category)
                val updatedSounds = sounds.map { it.copy(categoryId = catId) }
                dao.insertSounds(updatedSounds)
            }
        }
    }

    fun getSoundsByCategory(categoryId: Long): Flow<List<SoundClipEntity>> {
        return dao.getSoundsByCategory(categoryId)
    }

    fun searchSounds(query: String): Flow<List<SoundClipEntity>> {
        return dao.searchSounds(query)
    }

    suspend fun ensureDefaultPresetsPopulated() = withContext(Dispatchers.IO) {
        val categoryCount = dao.getCategoryCount()
        if (categoryCount == 0) {
            // Populate default categories
            val defaultCategories = listOf(
                CategoryEntity(name = "Memes & FX", icon = "💥", colorHex = "#FF3366", sortOrder = 0, isDefault = true),
                CategoryEntity(name = "Beat Machine", icon = "🥁", colorHex = "#00F0FF", sortOrder = 1, isDefault = true),
                CategoryEntity(name = "Gaming & Chiptune", icon = "🎮", colorHex = "#A855F7", sortOrder = 2, isDefault = true),
                CategoryEntity(name = "Studio & DJ", icon = "🎧", colorHex = "#10B981", sortOrder = 3, isDefault = true)
            )

            val catIdMap = mutableMapOf<String, Long>()
            defaultCategories.forEach { cat ->
                val id = dao.insertCategory(cat)
                catIdMap[cat.name] = id
            }

            // Ensure WAV audio files exist
            val presetFiles = WavAudioGenerator.ensurePresetAudioFiles(context)

            // Populate default sounds
            val presets = WavAudioGenerator.getPresets()
            val soundEntities = presets.mapIndexed { index, preset ->
                val categoryId = catIdMap[preset.defaultCategory] ?: (catIdMap.values.firstOrNull() ?: 1L)
                val audioPath = presetFiles[preset.key] ?: ""
                SoundClipEntity(
                    title = preset.title,
                    emoji = preset.emoji,
                    colorHex = preset.colorHex,
                    categoryId = categoryId,
                    audioFilePath = audioPath,
                    presetKey = preset.key,
                    durationMs = preset.defaultDurationMs.coerceAtMost(10000L),
                    isLooping = preset.isDefaultLoop,
                    volume = 1.0f,
                    pitch = 1.0f,
                    playbackSpeed = 1.0f,
                    sortOrder = index,
                    isFavorite = index < 4 // Pin first few as favorites
                )
            }

            dao.insertSounds(soundEntities)
        } else {
            // Still ensure audio files exist if user has presets
            WavAudioGenerator.ensurePresetAudioFiles(context)
        }
    }

    suspend fun insertSound(sound: SoundClipEntity): Long = withContext(Dispatchers.IO) {
        // Strictly enforce 10s maximum limit
        val clampedSound = sound.copy(durationMs = sound.durationMs.coerceAtMost(10000L))
        dao.insertSound(clampedSound)
    }

    suspend fun updateSound(sound: SoundClipEntity) = withContext(Dispatchers.IO) {
        val clampedSound = sound.copy(durationMs = sound.durationMs.coerceAtMost(10000L))
        dao.updateSound(clampedSound)
    }

    suspend fun deleteSound(sound: SoundClipEntity) = withContext(Dispatchers.IO) {
        // Delete audio file if it's a custom recording/import (not a preset)
        if (sound.presetKey.isNullOrEmpty()) {
            try {
                val file = File(sound.audioFilePath)
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                // Ignore
            }
        }
        dao.deleteSound(sound)
    }

    suspend fun toggleFavorite(soundId: Long, current: Boolean) = withContext(Dispatchers.IO) {
        dao.setFavorite(soundId, !current)
    }

    suspend fun toggleLoop(soundId: Long, current: Boolean) = withContext(Dispatchers.IO) {
        dao.setLooping(soundId, !current)
    }

    suspend fun updateAudioSettings(soundId: Long, volume: Float, pitch: Float) = withContext(Dispatchers.IO) {
        dao.updateAudioSettings(soundId, volume, pitch)
    }

    // --- Category Management ---

    suspend fun insertCategory(category: CategoryEntity): Long = withContext(Dispatchers.IO) {
        dao.insertCategory(category)
    }

    suspend fun updateCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        dao.updateCategory(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        dao.deleteCategory(category)
    }

    // --- Cloud Sync ---

    suspend fun syncSoundboard(categories: List<CategoryEntity>, sounds: List<SoundClipEntity>): Result<String> {
        return cloudSyncManager.syncToCloud(categories, sounds)
    }

    suspend fun restoreSoundboard(syncCode: String, overwriteExisting: Boolean): Result<Int> {
        return cloudSyncManager.restoreFromCloud(syncCode, overwriteExisting)
    }

    fun setCloudSyncCode(code: String) {
        cloudSyncManager.setSyncCode(code)
    }
}
