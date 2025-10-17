package com.webtech.kamuskorea.ui.screens.profile

import android.app.Activity
import android.app.DatePickerDialog
import android.net.Uri
import android.widget.DatePicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.webtech.kamuskorea.R
import java.util.*

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val productDetails by viewModel.productDetails.collectAsState()
    val hasActiveSubscription by viewModel.hasActiveSubscription.collectAsState()
    val context = LocalContext.current
    val firebaseAuth = FirebaseAuth.getInstance()
    val currentUser = firebaseAuth.currentUser
    var displayName by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var birthDate by remember { mutableStateOf("Belum diatur") }
    var newProfilePicUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        newProfilePicUri = uri
        // Anda bisa langsung mengunggah gambar di sini atau menampilkan pratinjau
        // viewModel.uploadProfilePicture(uri)
    }

    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            birthDate = "$dayOfMonth/${month + 1}/$year"
        }, year, month, day
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = newProfilePicUri?.let { rememberAsyncImagePainter(it) } ?: painterResource(id = R.drawable.ic_default_profile),
                            contentDescription = "Foto Profil",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("Nama") },
                                trailingIcon = { Icon(Icons.Default.Edit, contentDescription = "Edit Nama") }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = currentUser?.email ?: "Tidak ada email",
                        onValueChange = {},
                        label = { Text("Email") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = birthDate,
                        onValueChange = {},
                        label = { Text("Tanggal Lahir") },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { datePickerDialog.show() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Status Langganan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (hasActiveSubscription) {
                        Text(
                            "Status: Pengguna Premium",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 18.sp
                        )
                    } else {
                        Text(
                            "Status: Pengguna Gratis",
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        productDetails?.let { details ->
                            Button(
                                onClick = { viewModel.purchase(context as Activity) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val price = details.subscriptionOfferDetails?.firstOrNull()
                                    ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                                    ?: ""
                                Text("Langganan Sekarang ($price)")
                            }
                        } ?: run {
                            CircularProgressIndicator()
                            Text(
                                "Memuat informasi langganan...",
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}