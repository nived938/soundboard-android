package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.SequenceEntity
import com.example.data.model.SoundClipEntity

@Database(
    entities = [CategoryEntity::class, SoundClipEntity::class, SequenceEntity::class],
    version = 2,
    exportSchema = false
)
abstract class SoundboardDatabase : RoomDatabase() {

    abstract fun soundboardDao(): SoundboardDao

    companion object {
        @Volatile
        private var INSTANCE: SoundboardDatabase? = null

        fun getDatabase(context: Context): SoundboardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SoundboardDatabase::class.java,
                    "soundboard_database.db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
