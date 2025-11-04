package com.webtech.kamuskorea.ui.screens.dictionary

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webtech.kamuskorea.data.local.WordDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * TEST VIEWMODEL
 *
 * Gunakan ini untuk testing manual pencarian database
 * Bisa dipanggil dari MainActivity atau screen manapun
 */
@HiltViewModel
class DatabaseTestViewModel @Inject constructor(
    private val wordDao: WordDao
) : ViewModel() {

    private val TAG = "DatabaseTest"

    init {
        Log.d(TAG, "========================================")
        Log.d(TAG, "DATABASE TEST STARTED")
        Log.d(TAG, "========================================")
        runAllTests()
    }

    private fun runAllTests() {
        viewModelScope.launch {
            test1_CheckTotalWords()
            test2_SearchKorean()
            test3_SearchRomanization()
            test4_SearchIndonesian()
            test5_SearchPartial()
            test6_SearchCaseInsensitive()
        }
    }

    private suspend fun test1_CheckTotalWords() {
        try {
            val words = wordDao.getAllWords().first()
            Log.d(TAG, "")
            Log.d(TAG, "TEST 1: Check Total Words")
            Log.d(TAG, "Result: ${words.size} words")

            if (words.isEmpty()) {
                Log.e(TAG, "❌ FAILED: Database is empty!")
            } else {
                Log.d(TAG, "✅ PASSED")
                Log.d(TAG, "Sample data:")
                words.take(5).forEach { word ->
                    Log.d(TAG, "  - ${word.koreanWord} [${word.romanization}] = ${word.indonesianTranslation}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ EXCEPTION in test1", e)
        }
    }

    private suspend fun test2_SearchKorean() {
        try {
            val query = "%안녕%"
            val results = wordDao.searchWords(query).first()

            Log.d(TAG, "")
            Log.d(TAG, "TEST 2: Search Korean (안녕)")
            Log.d(TAG, "Query: $query")
            Log.d(TAG, "Results: ${results.size}")

            if (results.isEmpty()) {
                Log.e(TAG, "❌ FAILED: No results for Korean search")
            } else {
                Log.d(TAG, "✅ PASSED")
                results.forEach { word ->
                    Log.d(TAG, "  - ${word.koreanWord} [${word.romanization}]")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ EXCEPTION in test2", e)
        }
    }

    private suspend fun test3_SearchRomanization() {
        try {
            val query = "%annyeong%"
            val results = wordDao.searchWords(query).first()

            Log.d(TAG, "")
            Log.d(TAG, "TEST 3: Search Romanization (annyeong)")
            Log.d(TAG, "Query: $query")
            Log.d(TAG, "Results: ${results.size}")

            if (results.isEmpty()) {
                Log.e(TAG, "❌ FAILED: No results for romanization search")
            } else {
                Log.d(TAG, "✅ PASSED")
                results.forEach { word ->
                    Log.d(TAG, "  - ${word.koreanWord} [${word.romanization}]")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ EXCEPTION in test3", e)
        }
    }

    private suspend fun test4_SearchIndonesian() {
        try {
            val query = "%halo%"
            val results = wordDao.searchWords(query).first()

            Log.d(TAG, "")
            Log.d(TAG, "TEST 4: Search Indonesian (halo)")
            Log.d(TAG, "Query: $query")
            Log.d(TAG, "Results: ${results.size}")

            if (results.isEmpty()) {
                Log.e(TAG, "❌ FAILED: No results for Indonesian search")
            } else {
                Log.d(TAG, "✅ PASSED")
                results.forEach { word ->
                    Log.d(TAG, "  - ${word.koreanWord} = ${word.indonesianTranslation}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ EXCEPTION in test4", e)
        }
    }

    private suspend fun test5_SearchPartial() {
        try {
            val query = "%a%"
            val results = wordDao.searchWords(query).first()

            Log.d(TAG, "")
            Log.d(TAG, "TEST 5: Search Partial (a)")
            Log.d(TAG, "Query: $query")
            Log.d(TAG, "Results: ${results.size}")

            if (results.isEmpty()) {
                Log.e(TAG, "❌ FAILED: No results for partial search")
            } else {
                Log.d(TAG, "✅ PASSED")
                Log.d(TAG, "First 3 results:")
                results.take(3).forEach { word ->
                    Log.d(TAG, "  - ${word.koreanWord} [${word.romanization}]")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ EXCEPTION in test5", e)
        }
    }

    private suspend fun test6_SearchCaseInsensitive() {
        try {
            val queryLower = "%annyeong%"
            val queryUpper = "%ANNYEONG%"
            val queryMixed = "%AnNyEoNg%"

            val resultsLower = wordDao.searchWords(queryLower).first()
            val resultsUpper = wordDao.searchWords(queryUpper).first()
            val resultsMixed = wordDao.searchWords(queryMixed).first()

            Log.d(TAG, "")
            Log.d(TAG, "TEST 6: Case Insensitive Search")
            Log.d(TAG, "Lower: ${resultsLower.size} results")
            Log.d(TAG, "Upper: ${resultsUpper.size} results")
            Log.d(TAG, "Mixed: ${resultsMixed.size} results")

            if (resultsLower.size == resultsUpper.size && resultsUpper.size == resultsMixed.size) {
                Log.d(TAG, "✅ PASSED: Case insensitive working")
            } else {
                Log.e(TAG, "❌ FAILED: Case sensitivity issue")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ EXCEPTION in test6", e)
        }
    }

    fun runManualTest(testQuery: String) {
        viewModelScope.launch {
            try {
                val query = "%${testQuery}%"
                val results = wordDao.searchWords(query).first()

                Log.d(TAG, "")
                Log.d(TAG, "MANUAL TEST")
                Log.d(TAG, "Query: $query")
                Log.d(TAG, "Results: ${results.size}")

                if (results.isEmpty()) {
                    Log.d(TAG, "No results found")
                } else {
                    results.forEach { word ->
                        Log.d(TAG, "  - ${word.koreanWord} [${word.romanization}] = ${word.indonesianTranslation}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ EXCEPTION in manual test", e)
            }
        }
    }
}

/**
 * CARA MENGGUNAKAN:
 *
 * 1. Tambahkan di MainActivity atau screen manapun:
 *
 *    private val testViewModel: DatabaseTestViewModel by viewModels()
 *
 * 2. Test otomatis akan jalan saat viewModel dibuat (di init)
 *
 * 3. Untuk test manual, panggil:
 *
 *    testViewModel.runManualTest("kata_yang_dicari")
 *
 * 4. Lihat hasil di Logcat dengan filter: DatabaseTest
 *
 * EXPECTED OUTPUT:
 *
 * ========================================
 * DATABASE TEST STARTED
 * ========================================
 *
 * TEST 1: Check Total Words
 * Result: 100 words
 * ✅ PASSED
 *
 * TEST 2: Search Korean (안녕)
 * Query: %안녕%
 * Results: 3
 * ✅ PASSED
 *
 * ... dst
 */