package com.webtech.kamuskorea.ui.screens.ebook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.webtech.kamuskorea.data.Ebook
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log


class EbookViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _ebooks = MutableStateFlow<List<Ebook>>(emptyList())
    val ebooks = _ebooks.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        fetchEbooks()
    }

    private fun fetchEbooks() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val db = FirebaseFirestore.getInstance()
                val snapshot = db.collection("ebooks")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val ebookList = snapshot.documents.mapNotNull { doc ->
                    // Menggunakan mapNotNull untuk keamanan ekstra, agar jika ada data null,
                    // aplikasi tidak crash dan item tersebut dilewati.
                    doc.toObject(Ebook::class.java)?.copy(id = doc.id)
                }
                _ebooks.value = ebookList

                // --- LOG UNTUK DEBUGGING ---
                // Log ini akan memberitahu kita apakah pengambilan data berhasil.
                Log.d("EbookFirestore", "Fetch berhasil. Jumlah ebook yang diambil: ${ebookList.size}")
                if (ebookList.isNotEmpty()) {
                    // Log ini untuk memastikan data di dalamnya tidak korup.
                    Log.d("EbookFirestore", "Judul ebook pertama yang terambil: ${ebookList[0].title}")
                }
                // --- AKHIR DARI LOG ---

            } catch (e: Exception) {
                // --- LOG JIKA TERJADI ERROR ---
                // Jika error PERMISSION_DENIED masih terjadi, log ini akan muncul.
                Log.e("EbookFirestore", "Fetch gagal karena error:", e)
                _ebooks.value = emptyList() // Kosongkan daftar jika gagal
            } finally {
                _isLoading.value = false
            }
        }
    }
}