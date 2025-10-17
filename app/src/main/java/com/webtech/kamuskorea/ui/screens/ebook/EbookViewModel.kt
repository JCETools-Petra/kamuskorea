package com.webtech.kamuskorea.ui.screens.ebook

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.webtech.kamuskorea.data.Ebook
import com.webtech.kamuskorea.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class EbookViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val remoteConfig: FirebaseRemoteConfig,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _ebooks = MutableStateFlow<List<Ebook>>(emptyList())
    val ebooks: StateFlow<List<Ebook>> = _ebooks.asStateFlow()

    val isPremiumUser = userRepository.isPremium.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    init {
        fetchEbooks()
    }

    private fun fetchEbooks() {
        viewModelScope.launch {
            try {
                // 1. Ambil konfigurasi jumlah e-book gratis
                remoteConfig.fetchAndActivate().await()
                val freeEbookCount = remoteConfig.getLong("free_ebook_count").toInt()
                Log.d("EbookViewModel", "Jumlah Ebook Gratis dari Remote Config: $freeEbookCount")

                // 2. Lakukan query ke Firestore untuk mendapatkan dokumen
                val snapshot = firestore.collection("ebooks")
                    .orderBy("order", Query.Direction.ASCENDING)
                    .get()
                    .await()

                Log.d("EbookViewModel", "Berhasil mengambil ${snapshot.size()} dokumen dari Firestore.")

                if (snapshot.isEmpty) {
                    Log.w("EbookViewModel", "Tidak ada dokumen e-book yang ditemukan di Firestore.")
                    _ebooks.value = emptyList() // Pastikan state di-update
                    return@launch
                }

                // 3. Ubah dokumen menjadi objek Ebook dan langsung tandai premium
                val ebookList = snapshot.documents.mapIndexedNotNull { index, document ->
                    try {
                        // Coba konversi dokumen ke objek
                        val ebook = document.toObject(Ebook::class.java)
                        if (ebook != null) {
                            // Jika berhasil, lengkapi dengan ID dan status premium
                            ebook.copy(
                                id = document.id,
                                isPremium = index >= freeEbookCount
                            )
                        } else {
                            Log.e("EbookViewModel", "Dokumen dengan ID ${document.id} tidak bisa diubah menjadi objek Ebook (null).")
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("EbookViewModel", "Error saat mengubah dokumen ${document.id}: ${e.message}")
                        null
                    }
                }

                Log.d("EbookViewModel", "Berhasil memproses ${ebookList.size} e-book.")
                _ebooks.value = ebookList

            } catch (e: Exception) {
                // Ini akan menangkap error jika query gagal (misal: karena index belum ada)
                Log.e("EbookViewModel", "Gagal mengambil ebooks dari Firestore", e)
                _ebooks.value = emptyList()
            }
        }
    }

    fun getEbookById(ebookId: String): Flow<Ebook?> {
        return ebooks.map { list ->
            list.find { it.id == ebookId }
        }
    }
}