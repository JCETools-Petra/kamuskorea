package com.webtech.kamuskorea.di

import android.content.Context
import androidx.datastore.core.DataStore // <-- TAMBAHKAN IMPORT INI
import androidx.datastore.preferences.core.Preferences // <-- TAMBAHKAN IMPORT INI
import androidx.room.Room
import com.webtech.kamuskorea.data.local.AppDatabase
import com.webtech.kamuskorea.data.local.WordDao
import com.webtech.kamuskorea.ui.datastore.dataStore // <-- TAMBAHKAN IMPORT INI (delegate)
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
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "kamus_korea.db"
        )
            .createFromAsset("database/kamus_korea.db")
            .build()
    }

    @Provides
    @Singleton
    fun provideWordDao(appDatabase: AppDatabase): WordDao {
        return appDatabase.wordDao()
    }

    // --- PERUBAHAN DI SINI ---
    // Menyediakan DataStore<Preferences> secara langsung sebagai Singleton
    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        // Menggunakan delegate 'dataStore' yang didefinisikan di SettingsDataStore.kt
        return context.dataStore
    }
    // Fungsi provideSettingsDataStore dihapus
    // --- AKHIR PERUBAHAN ---
}