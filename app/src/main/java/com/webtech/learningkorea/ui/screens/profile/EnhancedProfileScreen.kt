package com.webtech.learningkorea.ui.screens.profile

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.webtech.learningkorea.R
import java.text.SimpleDateFormat
import java.util.*

data class Achievement(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val isUnlocked: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val productDetails by viewModel.productDetails.collectAsState()
    val hasActiveSubscription by viewModel.hasActiveSubscription.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()
    val displayName by viewModel.displayName.collectAsState()
    val profilePictureUrl by viewModel.profilePictureUrl.collectAsState()
    val dateOfBirth by viewModel.dateOfBirth.collectAsState()
    val userEmail = FirebaseAuth.getInstance().currentUser?.email

    var showEditDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadProfilePicture(it) }
    }

    // Date picker state
    val showDatePicker = remember { mutableStateOf(false) }

    if (showDatePicker.value) {
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

    LaunchedEffect(updateStatus) {
        updateStatus?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.resetUpdateStatus()
        }
    }

    val achievements = listOf(
        Achievement(
            icon = Icons.Default.EmojiEvents,
            title = "Pemula",
            description = "Membuka aplikasi pertama kali",
            isUnlocked = true
        ),
        Achievement(
            icon = Icons.Default.LocalFireDepartment,
            title = "Streak 7 Hari",
            description = "Belajar 7 hari berturut-turut",
            isUnlocked = false
        ),
        Achievement(
            icon = Icons.Default.Star,
            title = "Master Kosa Kata",
            description = "Menguasai 100 kata",
            isUnlocked = false
        ),
        Achievement(
            icon = Icons.Default.School,
            title = "Quiz Champion",
            description = "Menyelesaikan 10 quiz dengan skor perfect",
            isUnlocked = false
        )
    )

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        AsyncImage(
                            model = profilePictureUrl,
                            contentDescription = "Foto Profil",
                            placeholder = painterResource(id = R.drawable.ic_default_profile),
                            error = painterResource(id = R.drawable.ic_default_profile),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .border(4.dp, Color.White, CircleShape)
                                .clickable { imagePickerLauncher.launch("image/*") }
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Edit Photo",
                                tint = Color.White,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = displayName.ifBlank { "Pengguna" },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (!userEmail.isNullOrBlank()) {
                        Text(
                            text = userEmail,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Profile Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Informasi Profil",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, "Edit Profile")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    ProfileInfoRow(
                        icon = Icons.Outlined.Person,
                        label = "Nama",
                        value = displayName.ifBlank { "Belum diatur" }
                    )

                    ProfileInfoRow(
                        icon = Icons.Outlined.Cake,
                        label = "Tanggal Lahir",
                        value = dateOfBirth.ifBlank { "Belum diatur" }
                    )

                    ProfileInfoRow(
                        icon = Icons.Outlined.Email,
                        label = "Email",
                        value = userEmail ?: "-"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subscription Card
            SubscriptionStatusCard(
                hasActiveSubscription = hasActiveSubscription,
                productDetails = productDetails,
                onSubscribeClick = {
                    activity?.let { viewModel.launchBilling(it) }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Achievements Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Pencapaian",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        achievements.forEach { achievement ->
                            AchievementBadge(achievement)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Edit Profile Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Profil") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { viewModel.onDisplayNameChange(it) },
                        label = { Text("Nama") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = dateOfBirth.ifBlank { "Pilih tanggal" },
                        onValueChange = {},
                        label = { Text("Tanggal Lahir") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker.value = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker.value = true }) {
                                Icon(Icons.Default.CalendarMonth, "Pilih tanggal")
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateProfileDetails()
                    showEditDialog = false
                }) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun ProfileInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SubscriptionStatusCard(
    hasActiveSubscription: Boolean,
    productDetails: com.android.billingclient.api.ProductDetails?,
    onSubscribeClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasActiveSubscription)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (hasActiveSubscription) Icons.Default.WorkspacePremium else Icons.Default.Star,
                    contentDescription = null,
                    tint = if (hasActiveSubscription) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    if (hasActiveSubscription) "Status Premium" else "Upgrade ke Premium",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (hasActiveSubscription) {
                Text(
                    "🎉 Anda adalah pengguna Premium!",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Nikmati akses tanpa batas ke semua fitur",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            } else {
                Text(
                    "Buka semua fitur premium:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                PremiumFeature("✓ Semua E-Book gratis")
                PremiumFeature("✓ Latihan soal tanpa batas")
                PremiumFeature("✓ Fitur hafalan cerdas")
                PremiumFeature("✓ Tanpa iklan")

                Spacer(modifier = Modifier.height(16.dp))

                productDetails?.let { details ->
                    val price = details.subscriptionOfferDetails?.firstOrNull()
                        ?.pricingPhases?.pricingPhaseList?.firstOrNull()
                        ?.formattedPrice ?: "N/A"

                    Button(
                        onClick = onSubscribeClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Berlangganan $price/bulan")
                    }
                } ?: CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
fun PremiumFeature(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
fun AchievementBadge(achievement: Achievement) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = if (achievement.isUnlocked)
                Color(0xFFFFD700).copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                achievement.icon,
                contentDescription = null,
                tint = if (achievement.isUnlocked)
                    Color(0xFFFFD700)
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.padding(12.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            achievement.title,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 4.dp),
            color = if (achievement.isUnlocked)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}