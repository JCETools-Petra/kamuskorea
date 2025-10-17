// Lokasi file: app/src/main/java/com/webtech/kamuskorea/ui/screens/dictionary/DictionaryViewModel.kt

package com.webtech.kamuskorea.ui.screens.dictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webtech.kamuskorea.data.local.Word
import com.webtech.kamuskorea.data.local.WordDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel // Anotasi ini memberitahu Hilt cara membuat ViewModel ini
class DictionaryViewModel @Inject constructor( // @Inject memberitahu Hilt dependensi apa yang dibutuhkan
    private val wordDao: WordDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Logika untuk mendapatkan kata-kata berdasarkan query pencarian dari flow
    val words: StateFlow<List<Word>> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                // Jika query kosong, tampilkan semua kata
                wordDao.getAllWords()
            } else {
                // Jika ada query, cari kata yang cocok
                wordDao.searchWords(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Optimasi agar flow aktif saat UI terlihat
            initialValue = emptyList() // Nilai awal adalah daftar kosong
        )

    /**
     * Fungsi yang dipanggil dari UI untuk memperbarui query pencarian.
     */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    // companion object dengan provideFactory() DIHAPUS karena tidak diperlukan saat menggunakan Hilt.
}
