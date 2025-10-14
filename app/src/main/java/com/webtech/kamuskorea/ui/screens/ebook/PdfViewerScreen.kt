package com.webtech.kamuskorea.ui.screens.ebook

import android.net.Uri // <-- Import Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.net.toUri // <-- Or import the extension function
import com.rizzi.bouquet.ResourceType
import com.rizzi.bouquet.VerticalPDFReader
import com.rizzi.bouquet.rememberVerticalPdfReaderState
import java.io.File as JFile

@Composable
fun PdfViewerScreen(filePath: String?) {
    if (filePath == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Gagal memuat file PDF.")
        }
        return
    }

    val pdfFile = JFile(filePath)

    if (!pdfFile.exists()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("File PDF tidak ditemukan di lokasi yang ditentukan.")
        }
        return
    }

    // Convert the File to a Uri before passing it to the state holder
    val fileUri = pdfFile.toUri() // Or use Uri.fromFile(pdfFile)

    val pdfState = rememberVerticalPdfReaderState(
        // Pass the Uri to ResourceType.Local
        resource = ResourceType.Local(fileUri),
        isZoomEnable = true
    )

    Box(modifier = Modifier.fillMaxSize()) {
        VerticalPDFReader(
            state = pdfState,
            modifier = Modifier.fillMaxSize()
        )

        // The loading state is handled internally by the library now,
        // but you can keep this for a better user experience.
        if (!pdfState.isLoaded) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}
