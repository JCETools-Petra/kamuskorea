package com.webtech.kamuskorea.ui.screens

import android.app.Application
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.webtech.kamuskorea.data.local.Word
import com.webtech.kamuskorea.ui.screens.dictionary.DictionaryViewModel
import kotlinx.coroutines.delay

@Composable
fun DictionaryScreen(
    viewModel: DictionaryViewModel = viewModel(
        factory = DictionaryViewModel.provideFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val words by viewModel.words.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    // State baru untuk mode bahasa: "korea" atau "indonesia"
    var languageMode by remember { mutableStateOf("korea") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Baris untuk tombol toggle dan search bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tombol untuk mengubah mode bahasa
            OutlinedButton(
                onClick = {
                    languageMode = if (languageMode == "korea") "indonesia" else "korea"
                }
            ) {
                Text(if (languageMode == "korea") "KO -> ID" else "ID -> KO")
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier.weight(1f), // Agar mengisi sisa ruang
                label = { Text("Cari...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                singleLine = true
            )
        }

        // Daftar Kata
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(words) { word ->
                // Kirim state languageMode ke setiap WordItem
                WordItem(word = word, languageMode = languageMode)
            }
        }
    }
}

@Composable
fun WordItem(word: Word, languageMode: String) {
    // State untuk setiap kartu: mengingat apakah sudah terungkap atau belum.
    var isRevealed by remember { mutableStateOf(false) }

    // --- BAGIAN BARU DIMULAI DI SINI ---
    // LaunchedEffect akan "mengamati" state isRevealed.
    // Setiap kali isRevealed berubah, kode di dalamnya akan dieksekusi.
    LaunchedEffect(key1 = isRevealed) {
        // Kita hanya ingin timer berjalan JIKA kartu dalam kondisi terbuka (true)
        if (isRevealed) {
            // delay() adalah fungsi suspend dari coroutine, tidak akan memblokir UI.
            // 5000L berarti 5000 milidetik = 5 detik.
            delay(5000L)
            // Setelah 5 detik, paksa state kembali menjadi tertutup (false).
            isRevealed = false
        }
    }
    // --- AKHIR DARI BAGIAN BARU ---


    // State untuk animasi (kode ini tidak berubah)
    val rotationAngle by animateFloatAsState(
        targetValue = if (isRevealed) -10f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "rotation"
    )
    val alphaValue by animateFloatAsState(
        targetValue = if (isRevealed) 0f else 1f,
        animationSpec = tween(durationMillis = 300, delayMillis = 100),
        label = "alpha"
    )
    val scaleValue by animateFloatAsState(
        targetValue = if (isRevealed) 1.1f else 1f,
        animationSpec = tween(durationMillis = 400),
        label = "scale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        // Ganti menjadi Box biasa, modifier-nya tetap sama
        Box(
            modifier = Modifier
                .clickable {
                    // Logika klik diubah: hanya izinkan membuka jika sedang tertutup
                    // untuk menghindari reset timer jika diklik berkali-kali.
                    if (!isRevealed) {
                        isRevealed = true
                    }
                }
        ) {
            // --- LAPISAN BAWAH (YANG TERLIHAT SETELAH STIKER DIROBEK) ---
            val revealedContent: @Composable () -> Unit = {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (languageMode == "korea") {
                        Text(text = word.indonesian, fontSize = 18.sp)
                    } else {
                        Text(text = word.korean, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "[${word.romanization}]",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            revealedContent()

            // --- LAPISAN ATAS (STIKER YANG AKAN DIANIMASIKAN) ---
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = alphaValue
                        rotationZ = rotationAngle
                        scaleX = scaleValue
                        scaleY = scaleValue
                        transformOrigin = TransformOrigin(0.9f, 0.1f)
                    },
                color = MaterialTheme.colorScheme.surfaceVariant,
                content = {
                    val stickerContent: @Composable () -> Unit = {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (languageMode == "korea") {
                                Text(text = word.korean, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "[${word.romanization}]",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(text = word.indonesian, fontSize = 18.sp)
                            }
                        }
                    }
                    stickerContent()
                }
            )
        }
    }
}