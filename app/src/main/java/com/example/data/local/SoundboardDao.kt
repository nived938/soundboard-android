package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CategoryEntity
import com.example.data.model.SequenceEntity
import com.example.data.model.SoundClipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SoundboardDao {

    // --- Category Queries ---

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, id ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>): List<Long>

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    // --- Sound Queries ---

    @Query("SELECT * FROM sound_clips ORDER BY isFavorite DESC, sortOrder ASC, id ASC")
    fun getAllSounds(): Flow<List<SoundClipEntity>>

    @Query("SELECT * FROM sound_clips WHERE categoryId = :categoryId ORDER BY isFavorite DESC, sortOrder ASC, id ASC")
    fun getSoundsByCategory(categoryId: Long): Flow<List<SoundClipEntity>>

    @Query("SELECT * FROM sound_clips WHERE isFavorite = 1 ORDER BY sortOrder ASC, id ASC")
    fun getFavoriteSounds(): Flow<List<SoundClipEntity>>

    @Query("SELECT * FROM sound_clips WHERE id = :id LIMIT 1")
    suspend fun getSoundById(id: Long): SoundClipEntity?

    @Query("SELECT * FROM sound_clips WHERE title LIKE '%' || :query || '%' ORDER BY isFavorite DESC, sortOrder ASC, id ASC")
    fun searchSounds(query: String): Flow<List<SoundClipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSound(sound: SoundClipEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSounds(sounds: List<SoundClipEntity>): List<Long>

    @Update
    suspend fun updateSound(sound: SoundClipEntity)

    @Delete
    suspend fun deleteSound(sound: SoundClipEntity)

    @Query("DELETE FROM sound_clips WHERE id = :id")
    suspend fun deleteSoundById(id: Long)

    @Query("UPDATE sound_clips SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE sound_clips SET isLooping = :isLooping WHERE id = :id")
    suspend fun setLooping(id: Long, isLooping: Boolean)

    @Query("UPDATE sound_clips SET volume = :volume, pitch = :pitch WHERE id = :id")
    suspend fun updateAudioSettings(id: Long, volume: Float, pitch: Float)

    @Query("UPDATE sound_clips SET playCount = playCount + 1 WHERE id = :id")
    suspend fun incrementPlayCount(id: Long)

    @Query("UPDATE sound_clips SET trimStartMs = :startMs, trimEndMs = :endMs, activeFx = :activeFx WHERE id = :id")
    suspend fun updateTrimAndFx(id: Long, startMs: Long, endMs: Long, activeFx: String)

    @Query("SELECT COUNT(*) FROM sound_clips")
    suspend fun getSoundCount(): Int

    @Query("SELECT SUM(playCount) FROM sound_clips")
    suspend fun getTotalPlays(): Int?

    @Query("DELETE FROM sound_clips")
    suspend fun clearAllSounds()

    @Query("DELETE FROM categories")
    suspend fun clearAllCategories()

    // --- Sequence / Beat Combo Queries ---

    @Query("SELECT * FROM sound_sequences ORDER BY id DESC")
    fun getAllSequences(): Flow<List<SequenceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSequence(sequence: SequenceEntity): Long

    @Delete
    suspend fun deleteSequence(sequence: SequenceEntity)
}
