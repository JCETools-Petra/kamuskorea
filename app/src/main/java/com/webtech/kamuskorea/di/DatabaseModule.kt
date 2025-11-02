package com.webtech.kamuskorea.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
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
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "kamus_database"
        )
            // ✅ MEMBACA DARI ASSETS
            // Pastikan file ada di: app/src/main/assets/database/kamus_korea.db
            .createFromAsset("database/kamus_korea.db")
            // Fallback strategy jika database corrupt
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideWordDao(appDatabase: AppDatabase): WordDao {
        return appDatabase.wordDao()
    }

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
}