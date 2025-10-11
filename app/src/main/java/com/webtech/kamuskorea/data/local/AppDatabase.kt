package com.webtech.kamuskorea.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.Executors

@Database(entities = [Word::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wordDao(): WordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kamus_database"
                )
                    // --- BAGIAN YANG DITAMBAHKAN UNTUK MENGISI DATA AWAL ---
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Lakukan insert data di background thread agar tidak mengganggu UI
                            Executors.newSingleThreadExecutor().execute {
                                // Contoh data awal
                                val wordsToInsert = listOf(
                                    Word(korean = "안녕하세요", romanization = "annyeonghaseyo", indonesian = "Halo / Selamat pagi/siang/sore"),
                                    Word(korean = "감사합니다", romanization = "kamsahamnida", indonesian = "Terima kasih"),
                                    Word(korean = "사랑", romanization = "sarang", indonesian = "Cinta"),
                                    Word(korean = "사람", romanization = "saram", indonesian = "Orang"),
                                    Word(korean = "네", romanization = "ne", indonesian = "Ya"),
                                    Word(korean = "아니요", romanization = "aniyo", indonesian = "Tidak / Bukan")
                                )

                                // Karena DAO belum bisa diakses di sini, kita gunakan SQL mentah
                                wordsToInsert.forEach {
                                    db.execSQL("INSERT INTO words (korean_word, romanization, indonesian_translation) VALUES ('${it.korean}', '${it.romanization}', '${it.indonesian}')")
                                }
                            }
                        }
                    })
                    // --- AKHIR DARI BAGIAN YANG DITAMBAHKAN ---
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}