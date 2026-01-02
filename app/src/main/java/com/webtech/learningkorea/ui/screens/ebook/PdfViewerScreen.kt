package com.webtech.learningkorea.ui.screens.ebook

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.github.barteksc.pdfviewer.PDFView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.URL

@Composable
fun PdfViewerScreen(
    navController: NavController,
    pdfUrl: String,
    title: String
) {
    var pdfFile by remember { mutableStateOf<File?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pdfUrl) {
        coroutineScope.launch {
            try {
                pdfFile = downloadPdf(context, pdfUrl)
            } catch (e: IOException) {
                error = "Gagal memuat E-Book. Periksa koneksi internet Anda."
            } finally {
                isLoading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            error != null -> {
                Text(
                    text = error!!,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            }
            pdfFile != null -> {
                PdfView(file = pdfFile!!)
            }
            else -> {
                Text(
                    "E-Book tidak ditemukan.",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun PdfView(file: File) {
    AndroidView(
        factory = { context ->
            PDFView(context, null).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                fromFile(file)
                    .defaultPage(0)
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .enableDoubletap(true)
                    .load()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

private suspend fun downloadPdf(context: Context, url: String): File {
    return withContext(Dispatchers.IO) {
        val fileName = URL(url).path.substringAfterLast('/')
        val file = File(context.cacheDir, fileName)

        if (!file.exists()) {
            URL(url).openStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        file
    }
}