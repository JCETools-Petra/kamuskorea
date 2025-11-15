package com.webtech.kamuskorea.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope

@Database(entities = [Vocabulary::class], version = 1, exportSchema = false)
abstract class VocabularyDatabase : RoomDatabase() {

    abstract fun vocabularyDao(): VocabularyDao

    companion object {
        @Volatile
        private var INSTANCE: VocabularyDatabase? = null

        fun getDatabase(
            context: Context,
            coroutineScope: CoroutineScope
        ): VocabularyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VocabularyDatabase::class.java,
                    "hafalan.db"
                )
                    .createFromAsset("database/hafalan.db") // Load dari assets
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
