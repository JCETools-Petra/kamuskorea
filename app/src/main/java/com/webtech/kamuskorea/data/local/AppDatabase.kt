package com.webtech.kamuskorea.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
// HAPUS IMPORT MIGRATION
import kotlinx.coroutines.CoroutineScope

// --- KEMBALIKAN VERSION KE 1 ---
@Database(entities = [Word::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wordDao(): WordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // --- HAPUS MIGRATION_1_2 ---

        fun getDatabase(
            context: Context,
            coroutineScope: CoroutineScope // (coroutineScope tidak dipakai lagi, tapi biarkan saja)
        ): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kamus_korea.db"
                )
                    .createFromAsset("database/kamus_korea.db") // Salin file v1 dari asset
                    // --- HAPUS .addMigrations() ---
                    // --- HAPUS .addCallback() ---
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // --- HAPUS SEMUA KODE DatabaseCallback ---
    }
}