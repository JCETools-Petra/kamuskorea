package com.webtech.kamuskorea.ui.screens.dictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webtech.kamuskorea.data.KamusSyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KamusSyncViewModel @Inject constructor(
    private val kamusSyncRepository: KamusSyncRepository
) : ViewModel() {

    fun syncDatabase() {
        viewModelScope.launch {
            kamusSyncRepository.checkAndSyncDatabase()
        }
    }
}