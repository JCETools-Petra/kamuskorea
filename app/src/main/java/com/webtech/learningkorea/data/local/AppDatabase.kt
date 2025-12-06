package com.webtech.learningkorea.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope

@Database(
    entities = [Word::class, FavoriteWord::class, FavoriteVocabulary::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wordDao(): WordDao
    abstract fun favoriteWordDao(): FavoriteWordDao
    abstract fun favoriteVocabularyDao(): FavoriteVocabularyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from version 1 to 2: Add favorite_words table
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS favorite_words (
                        word_id INTEGER PRIMARY KEY NOT NULL,
                        korean_word TEXT NOT NULL,
                        romanization TEXT NOT NULL,
                        indonesian_translation TEXT NOT NULL,
                        added_at INTEGER NOT NULL
                    )
                """)
            }
        }

        // Migration from version 2 to 3: Add favorite_vocabulary table
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS favorite_vocabulary (
                        vocabulary_id INTEGER PRIMARY KEY NOT NULL,
                        korean_word TEXT NOT NULL,
                        indonesian_meaning TEXT NOT NULL,
                        chapter_number INTEGER NOT NULL,
                        chapter_title_indonesian TEXT NOT NULL,
                        added_at INTEGER NOT NULL
                    )
                """)
            }
        }

        fun getDatabase(
            context: Context,
            coroutineScope: CoroutineScope
        ): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kamus_korea.db"
                )
                    .createFromAsset("database/kamus_korea.db")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    // FIX: Add fallback strategy for migration failures
                    // This will recreate the database from asset if migration fails
                    // Note: User data in favorite tables will be lost, but dictionary data is preserved
                    .fallbackToDestructiveMigrationFrom(1, 2)  // Safe for old versions
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                            super.onDestructiveMigration(db)
                            android.util.Log.w("AppDatabase",
                                "⚠️ Database migration failed - recreating from asset. " +
                                "User favorites may be lost.")
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
