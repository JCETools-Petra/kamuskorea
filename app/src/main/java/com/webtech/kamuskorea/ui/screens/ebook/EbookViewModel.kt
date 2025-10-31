package com.webtech.kamuskorea.ui.screens.ebook

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// Import FirebaseAuth dan await tidak lagi diperlukan di file ini
// import com.google.firebase.auth.FirebaseAuth
// import kotlinx.coroutines.tasks.await
import com.webtech.kamuskorea.data.Ebook
import com.webtech.kamuskorea.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EbookViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _ebooks = MutableStateFlow<List<Ebook>>(emptyList())
    val ebooks: StateFlow<List<Ebook>> = _ebooks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        fetchEbooks()
    }

    fun fetchEbooks() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // --- PERBAIKAN DI SINI ---
                // Logika token manual dihapus. Interceptor menanganinya.
                Log.d("EbookViewModel", "Memanggil API getEbooks...")

                // Panggil API tanpa argumen token
                val response = apiService.getEbooks()
                // --- AKHIR PERBAIKAN ---

                if (response.isSuccessful) {
                    val apiEbooks = response.body() ?: emptyList()
                    Log.d("EbookViewModel", "API Ebooks sukses, jumlah: ${apiEbooks.size}")
                    // Konversi dari EbookApiResponse ke Ebook (data class lokal)
                    _ebooks.value = apiEbooks.map { apiBook ->
                        Ebook(
                            id = apiBook.id.toString(),
                            title = apiBook.title,
                            description = apiBook.description,
                            coverImageUrl = apiBook.coverImageUrl,
                            pdfUrl = apiBook.pdfUrl, // Akan kosong jika premium & user non-premium
                            order = apiBook.order,
                            isPremium = apiBook.isPremium
                        )
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("EbookViewModel", "Error fetching ebooks API: ${response.code()} - $errorBody")
                    _errorMessage.value = "Gagal memuat ebook: ${response.message()}"
                }
            } catch (e: Exception) {
                Log.e("EbookViewModel", "Exception fetching ebooks", e)
                _errorMessage.value = "Terjadi kesalahan: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}