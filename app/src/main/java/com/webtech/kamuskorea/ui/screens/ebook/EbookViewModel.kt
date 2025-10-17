package com.webtech.kamuskorea.ui.screens.ebook

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.webtech.kamuskorea.data.Ebook
import com.webtech.kamuskorea.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class EbookViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val remoteConfig: FirebaseRemoteConfig,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _ebooks = mutableStateOf<List<Ebook>>(emptyList())
    val ebooks: State<List<Ebook>> = _ebooks

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    init {
        fetchEbooks()
    }

    private fun fetchEbooks() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // --- PERUBAHAN DI SINI ---
                // Menggunakan nama properti yang benar: 'isPremium'
                val isPremiumUser = userRepository.isPremium.first()

                val snapshot = firestore.collection("ebooks")
                    .orderBy("order", Query.Direction.ASCENDING)
                    .get()
                    .await()

                val ebooksList = snapshot.documents.mapNotNull { document ->
                    val ebook = document.toObject(Ebook::class.java)?.copy(id = document.id)
                    if (ebook != null) {
                        if (!isPremiumUser && ebook.isPremium) {
                            ebook.copy(pdfUrl = "")
                        } else {
                            ebook
                        }
                    } else {
                        null
                    }
                }

                if (ebooksList.isNotEmpty()) {
                    Log.d("EbookViewModel", "Berhasil mengambil ${ebooksList.size} dokumen dari Firestore.")
                    _ebooks.value = ebooksList
                } else {
                    Log.w("EbookViewModel", "Tidak ada dokumen e-book yang ditemukan di Firestore.")
                    _ebooks.value = emptyList()
                }

            } catch (e: Exception) {
                Log.e("EbookViewModel", "Gagal mengambil e-books", e)
                _error.value = "Gagal memuat e-book. Periksa koneksi internet Anda."
            } finally {
                _isLoading.value = false
            }
        }
    }
}