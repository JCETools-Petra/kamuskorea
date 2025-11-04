package com.webtech.kamuskorea.di

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.webtech.kamuskorea.data.local.AppDatabase
import com.webtech.kamuskorea.data.local.WordDao
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

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        Log.d("DatabaseModule", "Creating AppDatabase...")

        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "kamus_database"
        )
            // ✅ MEMBACA DARI ASSETS
            .createFromAsset("database/kamus_korea.db")
            // Fallback strategy jika database corrupt
            .fallbackToDestructiveMigration()
            // ✅ TAMBAHKAN CALLBACK UNTUK CEK DATABASE
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    Log.d("DatabaseModule", "========== DATABASE CREATED ==========")

                    // Cek jumlah data LANGSUNG pakai SQLite (tanpa coroutine)
                    try {
                        val cursor = db.query("SELECT COUNT(*) FROM words")
                        if (cursor.moveToFirst()) {
                            val count = cursor.getInt(0)
                            Log.d("DatabaseModule", "✅ Database contains $count words")

                            if (count == 0) {
                                Log.e("DatabaseModule", "❌ WARNING: Database is empty!")
                            } else {
                                // Sample beberapa kata
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
                        } else {
                            Log.e("DatabaseModule", "❌ Cannot count words!")
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

                    // Cek jumlah data setiap kali database dibuka
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
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
}