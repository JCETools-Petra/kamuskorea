package com.webtech.kamuskorea.ui.screens.ebook

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import com.rizzi.bouquet.ResourceType
import com.rizzi.bouquet.VerticalPDFReader
import com.rizzi.bouquet.rememberVerticalPdfReaderState
import java.io.File as JFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    filePath: String?,
    onNavigateBack: () -> Unit // Menambahkan parameter untuk aksi kembali
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kembali ke E-book") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { // Memanggil aksi saat tombol diklik
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Kembali ke daftar E-Book"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (filePath == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Gagal memuat file PDF.")
                }
                return@Box
            }

            val pdfFile = JFile(filePath)

            if (!pdfFile.exists()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("File PDF tidak ditemukan di lokasi yang ditentukan.")
                }
                return@Box
            }

            val fileUri = pdfFile.toUri()

            val pdfState = rememberVerticalPdfReaderState(
                resource = ResourceType.Local(fileUri),
                isZoomEnable = true
            )

            VerticalPDFReader(
                state = pdfState,
                modifier = Modifier.fillMaxSize()
            )

            if (!pdfState.isLoaded) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}