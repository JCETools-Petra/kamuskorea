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
import androidx.compose.material.icons.filled.* // Import all filled icons
import androidx.compose.material.icons.outlined.AccountCircle // Import specific outlined icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.* // M3 imports
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth // Import FirebaseAuth
import com.webtech.kamuskorea.R
import com.webtech.kamuskorea.ui.theme.KamusKoreaTheme // Import your theme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity // Safely cast to Activity

    // Get states from ViewModel
    val productDetails by viewModel.productDetails.collectAsState()
    val hasActiveSubscription by viewModel.hasActiveSubscription.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()
    val displayName by viewModel.displayName.collectAsState()
    val profilePictureUrl by viewModel.profilePictureUrl.collectAsState()
    val dateOfBirth by viewModel.dateOfBirth.collectAsState()
    val userEmail = FirebaseAuth.getInstance().currentUser?.email // Get email directly

    // Local states for editing
    var editedDisplayName by remember(displayName) { mutableStateOf(displayName) }
    var editedDateOfBirth by remember(dateOfBirth) { mutableStateOf(dateOfBirth) }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadProfilePicture(it) }
    }

    // Date Picker Dialog
    val calendar = Calendar.getInstance()
    // Set initial date from state if valid, otherwise use today
    LaunchedEffect(dateOfBirth) {
        if (dateOfBirth.matches("""^\d{4}-\d{2}-\d{2}$""".toRegex())) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                calendar.time = sdf.parse(dateOfBirth) ?: Date()
            } catch (e: Exception) {
                // Keep default calendar instance if parsing fails
            }
        }
    }
    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedCalendar = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            editedDateOfBirth = sdf.format(selectedCalendar.time)
            viewModel.onDateOfBirthChange(editedDateOfBirth) // Update ViewModel state
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

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
                .verticalScroll(rememberScrollState()) // Make the whole screen scrollable
                .padding(16.dp), // Add overall padding
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp) // Spacing between sections
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
                editedDisplayName = editedDisplayName,
                editedDateOfBirth = editedDateOfBirth,
                onDisplayNameChange = {
                    editedDisplayName = it
                    viewModel.onDisplayNameChange(it)
                },
                onDobClick = { datePickerDialog.show() },
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
            // Edit Icon with better contrast and padding
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "Edit Foto",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .offset(x = 4.dp, y = 4.dp) // Adjust position slightly
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(6.dp)
                    .clickable(onClick = onEditClick) // Make icon clickable too
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = displayName.ifBlank { "Pengguna" }, // Handle blank name
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        if (!email.isNullOrBlank()) {
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant // Use a secondary text color
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
            OutlinedTextField(
                value = editedDateOfBirth,
                onValueChange = {}, // ReadOnly
                label = { Text("Tanggal Lahir") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDobClick), // Make the whole field clickable
                readOnly = true,
                leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = "Tanggal Lahir") },
                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = "Pilih Tanggal") }
            )
            Button(
                onClick = onSaveClick,
                modifier = Modifier.align(Alignment.End) // Align button to the right
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
    productDetails: com.android.billingclient.api.ProductDetails?, // Use specific type
    onSubscribeClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            // Use different background if premium
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
                        tint = MaterialTheme.colorScheme.primary // Use primary color for premium icon
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Anda adalah Pengguna Premium ✨",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                // Anda bisa menambahkan info tanggal kadaluarsa di sini jika tersedia dari API/ViewModel
                // Text("Berlaku hingga: $expiryDate", style = MaterialTheme.typography.bodySmall)

            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.MoneyOff, // Icon for free status
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
                        // Gunakan nama produk dari Play Store jika tersedia
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
                        // Beri warna berbeda untuk tombol subscribe
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Berlangganan ($price/bulan)")
                    }
                } ?: run {
                    // Show loading or placeholder if details not loaded yet
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

// --- Preview Function (Optional) ---
@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    KamusKoreaTheme { // Gunakan tema Anda
        // Anda mungkin perlu membuat instance ViewModel palsu atau menyediakan data dummy di sini
        // untuk melihat preview yang lebih representatif.
        // Untuk saat ini, kita tampilkan saja strukturnya.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ProfileHeader(
                profilePictureUrl = null, // Gunakan null atau URL placeholder
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
                hasActiveSubscription = false, // Coba ganti true/false
                productDetails = null, // Beri null atau data ProductDetails dummy
                onSubscribeClick = {}
            )
        }
    }
}