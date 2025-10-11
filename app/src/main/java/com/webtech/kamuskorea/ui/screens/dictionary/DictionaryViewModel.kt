package com.webtech.kamuskorea.ui.screens.dictionary

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.webtech.kamuskorea.data.local.AppDatabase
import com.webtech.kamuskorea.data.local.Word
import com.webtech.kamuskorea.data.local.WordDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class DictionaryViewModel(private val wordDao: WordDao) : ViewModel() {

    // State untuk menampung query pencarian dari pengguna
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // State untuk menampung daftar kata yang akan ditampilkan di UI
    val words: StateFlow<List<Word>> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                wordDao.getAllWords()
            } else {
                wordDao.searchWords(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Fungsi yang dipanggil UI saat pengguna mengetik di search bar
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    // Ini adalah "Factory" untuk membuat instance ViewModel kita,
    // karena ViewModel ini butuh WordDao saat dibuat.
    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(DictionaryViewModel::class.java)) {
                        val dao = AppDatabase.getDatabase(application).wordDao()
                        return DictionaryViewModel(dao) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}