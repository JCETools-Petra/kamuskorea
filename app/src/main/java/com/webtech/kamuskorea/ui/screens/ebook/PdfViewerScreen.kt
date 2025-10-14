package com.webtech.kamuskorea.ui.screens.ebook

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rizzi.bouquet.ResourceType
import com.rizzi.bouquet.rememberVerticalPdfReaderState
import com.rizzi.bouquet.VerticalPDFReader
import java.io.File // <-- INI BARIS YANG MEMPERBAIKI ERROR

@Composable
fun PdfViewerScreen(filePath: String?) {
    // 1. Menangani kasus di mana path file tidak ada (null)
    if (filePath == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Gagal memuat file PDF.")
        }
        return
    }

    // 2. Membuat objek File dari path yang diberikan
    val pdfFile = File(filePath)

    // 3. Memeriksa apakah file benar-benar ada di perangkat
    if (!pdfFile.exists()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("File PDF tidak ditemukan di lokasi yang ditentukan.")
        }
        return
    }

    // 4. Menggunakan library Bouquet untuk mengingat state dari PDF reader
    val pdfState = rememberVerticalPdfReaderState(
        resource = ResourceType.File(pdfFile),
        isZoomEnable = true
    )

    // 5. Menampilkan PDF reader dan loading indicator
    Box(modifier = Modifier.fillMaxSize()) {
        VerticalPDFReader(
            state = pdfState,
            modifier = Modifier.fillMaxSize()
        )

        // Tampilkan loading indicator di tengah layar selama PDF sedang dimuat
        if (!pdfState.isLoaded) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}
