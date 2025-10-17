package com.webtech.kamuskorea.ui.screens.ebook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
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
    private val database: FirebaseDatabase,
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
                remoteConfig.fetchAndActivate().await()
                val freeEbookCount = remoteConfig.getLong("free_ebook_count").toInt()

                val ebooksRef = database.getReference("ebooks")
                val dataSnapshot = ebooksRef.get().await()

                val ebookList = dataSnapshot.children.mapNotNull { snapshot ->
                    // Ambil Ebook dari value
                    val ebook = snapshot.getValue(Ebook::class.java)
                    // Ambil ID dari key Firebase dan simpan ke dalam objek Ebook
                    ebook?.copy(id = snapshot.key ?: "")
                }.mapIndexed { index, ebook ->
                    // Tandai ebook sebagai premium jika posisinya melebihi jumlah gratis
                    ebook.copy(isPremium = index >= freeEbookCount)
                }
                _ebooks.value = ebookList
            } catch (e: Exception) {
                // Handle error, misalnya dengan logging atau menampilkan pesan ke pengguna
            }
        }
    }

    fun getEbookById(ebookId: String): Flow<Ebook?> {
        return _ebooks.map { list ->
            list.find { it.id == ebookId }
        }
    }
}