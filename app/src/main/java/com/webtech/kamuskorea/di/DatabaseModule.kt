package com.webtech.kamuskorea.di

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.webtech.kamuskorea.data.local.AppDatabase
import com.webtech.kamuskorea.data.local.WordDao
import com.webtech.kamuskorea.data.local.FavoriteWordDao
import com.webtech.kamuskorea.data.local.FavoriteVocabularyDao
import com.webtech.kamuskorea.data.local.VocabularyDatabase
import com.webtech.kamuskorea.data.local.VocabularyDao
import com.webtech.kamuskorea.ui.datastore.dataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // Migration from version 1 to 2: Add favorite_words table
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            Log.d("DatabaseModule", "Running MIGRATION_1_2: Creating favorite_words table")
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
            Log.d("DatabaseModule", "Running MIGRATION_2_3: Creating favorite_vocabulary table")
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

    // Migration from version 1 to 3 (for fresh installs that skip version 2)
    private val MIGRATION_1_3 = object : Migration(1, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            Log.d("DatabaseModule", "Running MIGRATION_1_3: Creating both favorite tables")
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS favorite_words (
                    word_id INTEGER PRIMARY KEY NOT NULL,
                    korean_word TEXT NOT NULL,
                    romanization TEXT NOT NULL,
                    indonesian_translation TEXT NOT NULL,
                    added_at INTEGER NOT NULL
                )
            """)
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

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        Log.d("DatabaseModule", "Creating AppDatabase...")

        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "kamus_database"
        )
            .createFromAsset("database/kamus_korea.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    Log.d("DatabaseModule", "========== DATABASE CREATED ==========")

                    try {
                        val cursor = db.query("SELECT COUNT(*) FROM words")
                        if (cursor.moveToFirst()) {
                            val count = cursor.getInt(0)
                            Log.d("DatabaseModule", "✅ Database contains $count words")

                            if (count == 0) {
                                Log.e("DatabaseModule", "❌ WARNING: Database is empty!")
                            } else {
                                val sampleCursor = db.query("SELECT korean_word, romanization, indonesian_translation FROM words LIMIT 5")
                                var index = 1
                                Log.d("DatabaseModule", "Sample words:")
                                while (sampleCursor.moveToNext()) {
                                    val korean = sampleCursor.getString(0)
                                    val roman = sampleCursor.getString(1)
                                    val indo = sampleCursor.getString(2)
                                    Log.d("DatabaseModule", "$index. $korean [$roman] = $indo")
                                    index++
                                }
                                sampleCursor.close()
                            }
                        }
                        cursor.close()
                    } catch (e: Exception) {
                        Log.e("DatabaseModule", "❌ Error checking database", e)
                    }
                    Log.d("DatabaseModule", "=====================================")
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    Log.d("DatabaseModule", "========== DATABASE OPENED ==========")

                    try {
                        val cursor = db.query("SELECT COUNT(*) FROM words")
                        if (cursor.moveToFirst()) {
                            val count = cursor.getInt(0)
                            Log.d("DatabaseModule", "Current word count: $count")
                        }
                        cursor.close()
                    } catch (e: Exception) {
                        Log.e("DatabaseModule", "Error counting words", e)
                    }
                    Log.d("DatabaseModule", "====================================")
                }
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideWordDao(appDatabase: AppDatabase): WordDao {
        Log.d("DatabaseModule", "Providing WordDao...")
        return appDatabase.wordDao()
    }

    @Provides
    @Singleton
    fun provideFavoriteWordDao(appDatabase: AppDatabase): FavoriteWordDao {
        Log.d("DatabaseModule", "Providing FavoriteWordDao...")
        return appDatabase.favoriteWordDao()
    }

    @Provides
    @Singleton
    fun provideFavoriteVocabularyDao(appDatabase: AppDatabase): FavoriteVocabularyDao {
        Log.d("DatabaseModule", "Providing FavoriteVocabularyDao...")
        return appDatabase.favoriteVocabularyDao()
    }

    @Provides
    @Singleton
    fun provideVocabularyDatabase(@ApplicationContext context: Context): VocabularyDatabase {
        Log.d("DatabaseModule", "Creating VocabularyDatabase...")

        return Room.databaseBuilder(
            context.applicationContext,
            VocabularyDatabase::class.java,
            "hafalan_database"
        )
            .createFromAsset("database/hafalan.db")
            .fallbackToDestructiveMigration()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    Log.d("DatabaseModule", "========== VOCABULARY DATABASE CREATED ==========")

                    try {
                        val cursor = db.query("SELECT COUNT(*) FROM Vocabulary")
                        if (cursor.moveToFirst()) {
                            val count = cursor.getInt(0)
                            Log.d("DatabaseModule", "✅ Vocabulary database contains $count words")
                        }
                        cursor.close()
                    } catch (e: Exception) {
                        Log.e("DatabaseModule", "❌ Error checking vocabulary database", e)
                    }
                    Log.d("DatabaseModule", "=================================================")
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    Log.d("DatabaseModule", "========== VOCABULARY DATABASE OPENED ==========")

                    try {
                        val cursor = db.query("SELECT COUNT(*) FROM Vocabulary")
                        if (cursor.moveToFirst()) {
                            val count = cursor.getInt(0)
                            Log.d("DatabaseModule", "Current vocabulary count: $count")
                        }
                        cursor.close()
                    } catch (e: Exception) {
                        Log.e("DatabaseModule", "Error counting vocabulary", e)
                    }
                    Log.d("DatabaseModule", "================================================")
                }
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideVocabularyDao(vocabularyDatabase: VocabularyDatabase): VocabularyDao {
        Log.d("DatabaseModule", "Providing VocabularyDao...")
        return vocabularyDatabase.vocabularyDao()
    }

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
}