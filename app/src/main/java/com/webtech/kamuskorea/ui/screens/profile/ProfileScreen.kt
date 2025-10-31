package com.webtech.kamuskorea.ui.screens.profile

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.webtech.kamuskorea.R
import com.webtech.kamuskorea.ui.theme.KamusKoreaTheme
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar

// Impor untuk DatePicker Material 3
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Get states from ViewModel
    val productDetails by viewModel.productDetails.collectAsState()
    val hasActiveSubscription by viewModel.hasActiveSubscription.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()
    val displayName by viewModel.displayName.collectAsState()
    val profilePictureUrl by viewModel.profilePictureUrl.collectAsState()
    val dateOfBirth by viewModel.dateOfBirth.collectAsState()
    val userEmail = FirebaseAuth.getInstance().currentUser?.email

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadProfilePicture(it) }
    }

    // --- Logika Date Picker Dialog (Material 3) ---
    val showDatePicker = remember { mutableStateOf(false) }

    if (showDatePicker.value) {
        // Coba parse tanggal yang ada (dari ViewModel), jika gagal, gunakan hari ini
        val initialSelectedDateMillis = remember(dateOfBirth) {
            if (dateOfBirth.matches("""^\d{4}-\d{2}-\d{2}$""".toRegex())) {
                try {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateOfBirth)?.time
                } catch (e: Exception) {
                    Calendar.getInstance().timeInMillis
                }
            } else {
                Calendar.getInstance().timeInMillis
            }
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialSelectedDateMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker.value = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        // Konversi milidetik ke tanggal YYYY-MM-DD
                        val selectedCalendar = Calendar.getInstance().apply {
                            timeInMillis = millis
                        }
                        val formattedDate = String.format("%04d-%02d-%02d",
                            selectedCalendar.get(Calendar.YEAR),
                            selectedCalendar.get(Calendar.MONTH) + 1,
                            selectedCalendar.get(Calendar.DAY_OF_MONTH)
                        )

                        viewModel.onDateOfBirthChange(formattedDate)
                    }
                    showDatePicker.value = false
                }) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker.value = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    // --- Akhir Logika Date Picker ---


    // Snackbar Host State
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(updateStatus) {
        updateStatus?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.resetUpdateStatus()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- Profile Header Section ---
            ProfileHeader(
                profilePictureUrl = profilePictureUrl,
                displayName = displayName,
                email = userEmail,
                onEditClick = { imagePickerLauncher.launch("image/*") }
            )

            // --- Edit Profile Section ---
            EditProfileCard(
                editedDisplayName = displayName,
                editedDateOfBirth = dateOfBirth,
                onDisplayNameChange = viewModel::onDisplayNameChange,
                onDobClick = { showDatePicker.value = true },
                onSaveClick = { viewModel.updateProfileDetails() }
            )

            // --- Subscription Section ---
            SubscriptionCard(
                hasActiveSubscription = hasActiveSubscription,
                productDetails = productDetails,
                onSubscribeClick = {
                    activity?.let { act -> viewModel.launchBilling(act) }
                }
            )
        }
    }
}

// --- Composable for Profile Header ---
@Composable
fun ProfileHeader(
    profilePictureUrl: String?,
    displayName: String,
    email: String?,
    onEditClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd) {
            AsyncImage(
                model = profilePictureUrl,
                contentDescription = "Foto Profil",
                placeholder = painterResource(id = R.drawable.ic_default_profile),
                error = painterResource(id = R.drawable.ic_default_profile),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onEditClick)
            )
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "Edit Foto",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .offset(x = 4.dp, y = 4.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(6.dp)
                    .clickable(onClick = onEditClick)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = displayName.ifBlank { "Pengguna" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        if (!email.isNullOrBlank()) {
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// --- Composable for Edit Profile Card ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileCard(
    editedDisplayName: String,
    editedDateOfBirth: String,
    onDisplayNameChange: (String) -> Unit,
    onDobClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Edit Profil", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = editedDisplayName,
                onValueChange = onDisplayNameChange,
                label = { Text("Nama Tampilan") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Outlined.AccountCircle, contentDescription = "Nama") }
            )

            // PERBAIKAN: Gunakan Box dengan clickable untuk membuat field bisa diklik
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDobClick)
            ) {
                OutlinedTextField(
                    value = editedDateOfBirth.ifBlank { "Belum diatur" },
                    onValueChange = {},
                    label = { Text("Tanggal Lahir") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    enabled = false, // Disable untuk mencegah keyboard muncul
                    leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = "Tanggal Lahir") },
                    trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = "Pilih Tanggal") },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Button(
                onClick = onSaveClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Simpan Perubahan")
            }
        }
    }
}

// --- Composable for Subscription Card ---
@Composable
fun SubscriptionCard(
    hasActiveSubscription: Boolean,
    productDetails: com.android.billingclient.api.ProductDetails?,
    onSubscribeClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasActiveSubscription) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Status Langganan", style = MaterialTheme.typography.titleMedium)

            if (hasActiveSubscription) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.WorkspacePremium,
                        contentDescription = "Premium",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Anda adalah Pengguna Premium ✨",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.MoneyOff,
                        contentDescription = "Gratis",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Anda menggunakan versi Gratis",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Show subscription offer
                productDetails?.let { details ->
                    val price = details.subscriptionOfferDetails?.firstOrNull()
                        ?.pricingPhases?.pricingPhaseList?.firstOrNull()
                        ?.formattedPrice ?: "N/A"

                    Text(
                        text = details.name.ifBlank { "Tingkatkan ke Premium" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = details.description.ifBlank { "Akses semua fitur tanpa batas." },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Button(
                        onClick = onSubscribeClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Berlangganan ($price/bulan)")
                    }
                } ?: run {
                    // Show loading
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Memuat info langganan...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// --- Preview Function ---
@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    KamusKoreaTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ProfileHeader(
                profilePictureUrl = null,
                displayName = "Nama Pengguna",
                email = "email@contoh.com",
                onEditClick = {}
            )
            EditProfileCard(
                editedDisplayName = "Nama Pengguna",
                editedDateOfBirth = "2000-01-01",
                onDisplayNameChange = {},
                onDobClick = {},
                onSaveClick = {}
            )
            SubscriptionCard(
                hasActiveSubscription = false,
                productDetails = null,
                onSubscribeClick = {}
            )
        }
    }
}