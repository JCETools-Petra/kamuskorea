package com.webtech.kamuskorea.ui.screens.ebook

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.github.barteksc.pdfviewer.PDFView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    ebookId: String,
    navController: NavController,
    viewModel: EbookViewModel = hiltViewModel()
) {
    val ebook by viewModel.getEbookById(ebookId).collectAsState(initial = null)
    val context = LocalContext.current
    var pdfFile by remember { mutableStateOf<File?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(ebook) {
        ebook?.let {
            isLoading = true
            error = null
            coroutineScope.launch {
                try {
                    val file = downloadPdf(context, it.pdfUrl)
                    pdfFile = file
                } catch (e: Exception) {
                    Log.e("PdfViewerScreen", "Gagal mengunduh PDF", e)
                    error = "Gagal memuat E-Book. Periksa koneksi internet Anda."
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ebook?.title ?: "Memuat...") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { paddingValues -> // <-- Nama parameternya adalah paddingValues
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues), // <-- Gunakan nama yang sama di sini
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (error != null) {
                Text(text = error!!)
            } else if (pdfFile != null) {
                PdfView(pdfFile!!)
            } else {
                Text("E-Book tidak ditemukan.")
            }
        }
    }
}

@Composable
fun PdfView(file: File) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            PDFView(context, null).apply {
                fromFile(file)
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .enableDoubletap(true)
                    .defaultPage(0)
                    .load()
            }
        }
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