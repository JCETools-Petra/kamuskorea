package com.webtech.kamuskorea.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.webtech.kamuskorea.data.local.Word
import com.webtech.kamuskorea.data.local.WordDao
import com.webtech.kamuskorea.data.network.ApiService
import com.webtech.kamuskorea.ui.datastore.SettingsDataStore // Import Companion Object Key
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KamusSyncRepository @Inject constructor(
    private val apiService: ApiService,
    private val wordDao: WordDao,
    private val dataStore: DataStore<Preferences> // Inject DataStore<Preferences>
) {
    suspend fun checkAndSyncDatabase() {
        // Jalankan operasi IO di background thread
        withContext(Dispatchers.IO) {
            try {
                // 1. Dapatkan versi lokal dari DataStore
                val localVersion = dataStore.data.map { preferences ->
                    preferences[SettingsDataStore.KAMUS_DB_VERSION_KEY] ?: 0
                }.first()
                Log.d("KamusSyncRepository", "Versi lokal saat ini: $localVersion")

                // 2. Panggil API untuk cek versi terbaru
                Log.d("KamusSyncRepository", "Memanggil API getKamusUpdates...")
                val response = apiService.getKamusUpdates(localVersion)

                if (response.isSuccessful) {
                    val updateData = response.body()
                    if (updateData == null) {
                        Log.e("KamusSyncRepository", "Respons update kamus kosong (body null)")
                        return@withContext
                    }

                    val latestVersion = updateData.latestVersion
                    val wordsToUpdate = updateData.words

                    Log.d("KamusSyncRepository", "Versi server: $latestVersion. Jumlah kata baru/update: ${wordsToUpdate.size}")

                    // 3. Jika versi server lebih baru
                    if (latestVersion > localVersion) {
                        if (wordsToUpdate.isNotEmpty()) {
                            // 4. Konversi data API ke data Room
                            val wordEntities = wordsToUpdate.map { wordApi ->
                                // --- DIPERBAIKI: Kembali ke konstruktor V1 ---
                                Word(
                                    id = 0,
                                    koreanWord = wordApi.korean,
                                    romanization = wordApi.romanization,
                                    indonesianTranslation = wordApi.indonesian
                                )
                                // -------------------------------------------
                            }

                            // 5. Masukkan/Update ke database Room
                            Log.d("KamusSyncRepository", "Memulai upsert ${wordEntities.size} kata ke Room DB...")
                            wordDao.upsertAll(wordEntities)
                            Log.i("KamusSyncRepository", "Upsert ${wordEntities.size} kata selesai.")

                        } else {
                            Log.i("KamusSyncRepository", "Versi server lebih baru ($latestVersion) tapi tidak ada data kata baru.")
                        }

                        // 6. Simpan versi baru ke DataStore (meskipun wordsToUpdate kosong, versi tetap update)
                        dataStore.edit { preferences ->
                            preferences[SettingsDataStore.KAMUS_DB_VERSION_KEY] = latestVersion
                        }
                        Log.i("KamusSyncRepository", "Versi kamus lokal disimpan ke: $latestVersion")

                    } else {
                        Log.i("KamusSyncRepository", "Database kamus sudah terbaru (versi $localVersion).")
                    }

                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("KamusSyncRepository", "Error cek update kamus API: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                Log.e("KamusSyncRepository", "Exception saat sinkronisasi kamus", e)
            }
        } // akhir withContext(Dispatchers.IO)
    }
}