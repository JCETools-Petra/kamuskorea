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
                val snapshot = db.collection("ebooks")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val ebookList = snapshot.documents.map { doc ->
                    doc.toObject(Ebook::class.java)!!.copy(id = doc.id)
                }
                _ebooks.value = ebookList
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
}