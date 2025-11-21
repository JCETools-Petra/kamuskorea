package com.webtech.kamuskorea

import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.webtech.kamuskorea.data.UserRepository
import com.webtech.kamuskorea.ui.localization.LocalizationProvider
import com.webtech.kamuskorea.ui.localization.LocalStrings
import com.webtech.kamuskorea.ui.navigation.Screen
import com.webtech.kamuskorea.ui.screens.*
import com.webtech.kamuskorea.ui.screens.assessment.*
import com.webtech.kamuskorea.ui.screens.auth.ForgotPasswordScreen
import com.webtech.kamuskorea.ui.screens.auth.LoginScreen
import com.webtech.kamuskorea.ui.screens.auth.RegisterScreen
import com.webtech.kamuskorea.ui.screens.dictionary.KamusSyncViewModel
import com.webtech.kamuskorea.ui.screens.ebook.PdfViewerScreen
import com.webtech.kamuskorea.ads.AdManager
import com.webtech.kamuskorea.notification.NotificationManager
import com.webtech.kamuskorea.notifications.NotificationChannels
import com.webtech.kamuskorea.notifications.NotificationScheduler
import com.webtech.kamuskorea.gamification.XpSyncWorker
import com.webtech.kamuskorea.ui.screens.onboarding.OnboardingScreen
import androidx.work.*
import java.util.concurrent.TimeUnit
import com.webtech.kamuskorea.ui.screens.profile.ProfileScreen
import com.webtech.kamuskorea.ui.screens.settings.ModernSettingsScreen
import com.webtech.kamuskorea.ui.screens.settings.SettingsViewModel
import com.webtech.kamuskorea.ui.screens.splash.AnimatedSplashScreen
import com.webtech.kamuskorea.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.webtech.kamuskorea.ui.screens.dictionary.SimpleDictionaryScreen
import com.webtech.kamuskorea.gamification.GamificationRepository
import com.webtech.kamuskorea.ui.components.GamificationEventHandler
import com.webtech.kamuskorea.ui.components.LevelUpDialog

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    @Inject
    lateinit var adManager: AdManager

    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    private val kamusSyncViewModel: KamusSyncViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    // Permission launcher untuk POST_NOTIFICATIONS (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "✅ Notification permission granted")
        } else {
            Log.d("MainActivity", "⚠️ Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        kamusSyncViewModel.syncDatabase()

        // Preload ads untuk performa yang lebih baik
        Log.d("MainActivity", "🎯 Preloading ads...")
        adManager.preloadAd(this)

        // Create notification channels (required for Android 8.0+)
        Log.d("MainActivity", "📱 Creating notification channels...")
        NotificationChannels.createNotificationChannels(this)

        // Request notification permission untuk Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("MainActivity", "📢 Requesting notification permission...")
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                Log.d("MainActivity", "✅ Notification permission already granted")
            }
        }

        // Schedule daily notifications (learning reminder & streak check)
        Log.d("MainActivity", "⏰ Scheduling daily notifications...")
        notificationScheduler.scheduleAllNotifications()

        // Schedule XP sync to server (every 15 minutes)
        Log.d("MainActivity", "🎮 Scheduling XP background sync...")
        scheduleXpSync()

        // Subscribe ke FCM topic untuk menerima broadcast notifications
        Log.d("MainActivity", "🔔 Subscribing to push notifications...")
        notificationManager.subscribeToAllUsersNotifications()

        // Optional: Get FCM token untuk debugging
        notificationManager.getToken { token ->
            Log.d("MainActivity", "🔑 FCM Token: $token")
            // TODO: Jika ingin kirim token ke server, lakukan di sini
        }

        setContent {
            val isPremium by userRepository.isPremium.collectAsState(initial = false)
            val textScale by settingsViewModel.textScale.collectAsState()
            val language by settingsViewModel.language.collectAsState()

            var showSplash by remember { mutableStateOf(true) }

            // State untuk melacak status login secara reaktif
            var isLoggedIn by remember { mutableStateOf(firebaseAuth.currentUser != null) }

            // Listener yang akan otomatis memperbarui `isLoggedIn`
            // saat status autentikasi Firebase berubah (login atau logout)
            DisposableEffect(firebaseAuth) {
                val authListener = FirebaseAuth.AuthStateListener { auth ->
                    isLoggedIn = auth.currentUser != null
                }
                firebaseAuth.addAuthStateListener(authListener)

                // Hapus listener saat composable tidak lagi digunakan
                onDispose {
                    firebaseAuth.removeAuthStateListener(authListener)
                }
            }

            val textScaleMultiplier = settingsViewModel.getTextScaleMultiplier(textScale)

            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = LocalDensity.current.density,
                    fontScale = LocalDensity.current.fontScale * textScaleMultiplier
                )
            ) {
                LocalizationProvider(languageCode = language) {
                    if (showSplash) {
                        AnimatedSplashScreen(
                            onTimeout = { showSplash = false }
                        )
                    } else {
                        // Logika "Penjaga"
                        // Tentukan Composable mana yang akan ditampilkan berdasarkan status login

                        // Dapatkan tema saat ini di sini agar bisa diteruskan ke kedua alur
                        val currentTheme by settingsViewModel.currentTheme.collectAsState()
                        val darkModeSetting by settingsViewModel.darkMode.collectAsState()
                        val systemDarkTheme = isSystemInDarkTheme()

                        // Determine actual dark theme based on user preference
                        val useDarkTheme = when (darkModeSetting) {
                            "light" -> false
                            "dark" -> true
                            else -> systemDarkTheme // "system" or default
                        }
                        val colors = getColors(currentTheme, useDarkTheme)

                        KamusKoreaTheme(darkTheme = useDarkTheme, dynamicColor = false, colorScheme = colors) {
                            if (isLoggedIn) {
                                // --- PENGGUNA SUDAH LOGIN ---
                                // Tampilkan aplikasi utama (dengan menu, scaffold, dll.)
                                MainApp(
                                    firebaseAuth = firebaseAuth,
                                    isPremium = isPremium,
                                    settingsViewModel = settingsViewModel,
                                    adManager = adManager,
                                    activity = this@MainActivity
                                )
                            } else {
                                // --- PENGGUNA BELUM LOGIN ---
                                // Tampilkan alur otentikasi (tanpa menu, tanpa scaffold)
                                AuthApp()
                            }
                        }
                    }
                }
            }
        }
    }

    // Fungsi helper untuk mendapatkan skema warna
    @Composable
    private fun getColors(currentTheme: String, useDarkTheme: Boolean): ColorScheme {
        return when (currentTheme) {
            "Forest" -> if (useDarkTheme) ForestDarkColorScheme else ForestLightColorScheme
            "Ocean" -> if (useDarkTheme) OceanDarkColorScheme else OceanLightColorScheme
            "Sunset" -> if (useDarkTheme) SunsetDarkColorScheme else SunsetLightColorScheme
            "Lavender" -> if (useDarkTheme) LavenderDarkColorScheme else LavenderLightColorScheme
            "Cherry" -> if (useDarkTheme) CherryDarkColorScheme else CherryLightColorScheme
            "Midnight" -> if (useDarkTheme) MidnightDarkColorScheme else MidnightLightColorScheme
            "Mint" -> if (useDarkTheme) MintDarkColorScheme else MintLightColorScheme
            "Autumn" -> if (useDarkTheme) AutumnDarkColorScheme else AutumnLightColorScheme
            "Coral" -> if (useDarkTheme) CoralDarkColorScheme else CoralLightColorScheme
            else -> if (useDarkTheme) DarkColorScheme else LightColorScheme
        }
    }

    /**
     * Schedule XP sync worker (every 15 minutes)
     */
    private fun scheduleXpSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(false)
            .build()

        val xpSyncRequest = PeriodicWorkRequestBuilder<XpSyncWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            XpSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            xpSyncRequest
        )
    }

    override fun onResume() {
        super.onResume()
        if (firebaseAuth.currentUser != null) {
            Log.d("MainActivity", "🔄 App resumed, checking premium status...")
            userRepository.checkPremiumStatus()
        }
    }

    override fun onStart() {
        super.onStart()
        if (firebaseAuth.currentUser != null) {
            Log.d("MainActivity", "▶️ App started, checking premium status...")
            userRepository.checkPremiumStatus()
        }
    }
}

// Composable ini HANYA menangani alur Login dan Register
// Tidak ada Drawer, tidak ada Scaffold.
// Onboarding dinonaktifkan - langsung ke Login
@Composable
fun AuthApp() {
    val navController = rememberNavController()

    // Skip onboarding - langsung ke login screen
    val startDestination = Screen.Login.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                // Saat login sukses, listener Firebase akan otomatis
                // mengganti state di MainActivity, jadi kita tidak perlu navigasi di sini
                onLoginSuccess = { },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(onNavigateToLogin = { navController.popBackStack() })
        }
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}


data class NavItem(
    val label: String,
    val icon: ImageVector,
    val screen: Screen
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    firebaseAuth: FirebaseAuth,
    isPremium: Boolean,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    adManager: AdManager,
    activity: ComponentActivity
) {
    val strings = LocalStrings.current

    val navController = rememberNavController()

    // startDestination sekarang selalu Home,
    // karena MainApp hanya dipanggil jika pengguna sudah login.
    val startDestination = Screen.Home.route

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentTitle by remember { mutableStateOf("") }
    val application = LocalContext.current.applicationContext as Application

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Gamification event handler - use HomeViewModel to access gamification events
    val homeViewModel: HomeViewModel = hiltViewModel()
    val snackbarHostState = remember { SnackbarHostState() }

    // Level up dialog state
    var showLevelUpDialog by remember { mutableStateOf(false) }
    var levelUpValue by remember { mutableIntStateOf(1) }

    // Handle gamification events (XP, level ups, achievements)
    GamificationEventHandler(
        gamificationEvents = homeViewModel.gamificationEvents,
        snackbarHostState = snackbarHostState,
        onLevelUp = { newLevel ->
            levelUpValue = newLevel
            showLevelUpDialog = true
        }
    )

    // Level up celebration dialog
    LevelUpDialog(
        isVisible = showLevelUpDialog,
        newLevel = levelUpValue,
        onDismiss = { showLevelUpDialog = false }
    )

    LaunchedEffect(currentRoute) {
        currentTitle = when {
            currentRoute == Screen.Home.route -> strings.home
            currentRoute == Screen.Dictionary.route -> strings.dictionary
            currentRoute == Screen.Favorites.route -> strings.favorites
            currentRoute == Screen.Ebook.route -> strings.ebook
            currentRoute == Screen.Quiz.route -> strings.quiz
            currentRoute == Screen.Memorization.route -> strings.memorization
            currentRoute == Screen.Profile.route -> strings.profile
            currentRoute == Screen.Settings.route -> strings.settings
            currentRoute?.startsWith("pdf_viewer/") == true ->
                navBackStackEntry?.arguments?.getString("title") ?: strings.ebook
            currentRoute?.startsWith("assessment/") == true -> "Latihan & Ujian"
            else -> strings.appName
        }
    }

    val menuItems = listOf(
        NavItem(strings.dictionary, Icons.AutoMirrored.Outlined.MenuBook, Screen.Dictionary),
        NavItem(strings.favorites, Icons.Outlined.Favorite, Screen.Favorites),
        NavItem(strings.ebook, Icons.Outlined.AutoStories, Screen.Ebook),
        NavItem(strings.memorization, Icons.Outlined.Bookmark, Screen.Memorization),
        NavItem(strings.quiz, Icons.Outlined.Quiz, Screen.Quiz)
    )

    val isPdfViewerScreen = currentRoute?.startsWith("pdf_viewer/") == true
    val isHomeScreen = currentRoute == Screen.Home.route
    val isAssessmentFlow = currentRoute?.startsWith("assessment/") == true || currentRoute == Screen.Quiz.route
    val isTakingAssessment = currentRoute?.startsWith("assessment/take/") == true

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isPdfViewerScreen,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = firebaseAuth.currentUser?.photoUrl,
                        contentDescription = "Profile Photo",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.ic_default_profile),
                        error = painterResource(id = R.drawable.ic_default_profile)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val displayName = firebaseAuth.currentUser?.displayName
                    val email = firebaseAuth.currentUser?.email
                    Text(
                        text = if (!displayName.isNullOrBlank()) displayName else email ?: "User",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Profile.route)
                    }) { Text(strings.profile) }
                }
                HorizontalDivider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = strings.home) },
                    label = { Text(strings.home) },
                    selected = Screen.Home.route == currentRoute,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Home.route)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                menuItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = item.screen.route == currentRoute,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id)
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = strings.settings) },
                    label = { Text(strings.settings) },
                    selected = currentRoute == Screen.Settings.route,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Settings.route)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = strings.logout) },
                    label = { Text(strings.logout) },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            val googleSignInClient = GoogleSignIn.getClient(
                                application,
                                GoogleSignInOptions.DEFAULT_SIGN_IN
                            )
                            googleSignInClient.signOut().addOnCompleteListener {
                                firebaseAuth.signOut()
                            }
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                // Sembunyikan TopAppBar saat mengerjakan ujian
                if (!isHomeScreen && !isTakingAssessment) {
                    TopAppBar(
                        title = { Text(currentTitle) },
                        navigationIcon = {
                            if (isPdfViewerScreen || isAssessmentFlow && currentRoute != Screen.Quiz.route) {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            } else {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                            }
                        }
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) {
                    ModernHomeScreen(navController = navController, isPremium = isPremium)
                }
                composable(Screen.Dictionary.route) {
                    SimpleDictionaryScreen(viewModel = hiltViewModel(), isPremium = isPremium)
                }
                composable(Screen.Favorites.route) {
                    FavoritesScreen(isPremium = isPremium)
                }
                composable(Screen.Memorization.route) {
                    MemorizationScreen(
                        isPremium = isPremium,
                        onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                        adManager = adManager
                    )
                }

                // Assessment Navigation Graph
                navigation(
                    startDestination = Screen.Quiz.route,
                    route = "assessment_graph"
                ) {
                    composable(Screen.Quiz.route) {
                        AssessmentMainScreen(
                            onNavigateToQuiz = {
                                navController.navigate("assessment/quiz/list")
                            },
                            onNavigateToExam = {
                                navController.navigate("assessment/exam/list")
                            },
                            onNavigateToHistory = {
                                navController.navigate("assessment/history")
                            },
                            isPremium = isPremium
                        )
                    }
                    composable("assessment/quiz/list") {
                        val assessmentBackStackEntry = remember(it) {
                            navController.getBackStackEntry("assessment_graph")
                        }
                        val assessmentViewModel: AssessmentViewModel = hiltViewModel(assessmentBackStackEntry)
                        AssessmentListScreen(
                            type = "quiz",
                            isPremium = isPremium,
                            onNavigateToAssessment = { assessmentId, durationMinutes ->
                                navController.navigate("assessment/take/$assessmentId/Quiz/$durationMinutes")
                            },
                            onNavigateToPremium = {
                                navController.navigate(Screen.PremiumLock.route)
                            },
                            viewModel = assessmentViewModel
                        )
                    }
                    composable("assessment/exam/list") {
                        val assessmentBackStackEntry = remember(it) {
                            navController.getBackStackEntry("assessment_graph")
                        }
                        val assessmentViewModel: AssessmentViewModel = hiltViewModel(assessmentBackStackEntry)
                        AssessmentListScreen(
                            type = "exam",
                            isPremium = isPremium,
                            onNavigateToAssessment = { assessmentId, durationMinutes ->
                                navController.navigate("assessment/take/$assessmentId/Exam/$durationMinutes")
                            },
                            onNavigateToPremium = {
                                navController.navigate(Screen.PremiumLock.route)
                            },
                            viewModel = assessmentViewModel
                        )
                    }
                    composable(
                        route = "assessment/take/{assessmentId}/{title}/{durationMinutes}",
                        arguments = listOf(
                            navArgument("assessmentId") { type = NavType.IntType },
                            navArgument("title") { type = NavType.StringType },
                            navArgument("durationMinutes") { type = NavType.IntType }
                        )
                    ) { backStackEntry ->
                        val assessmentId = backStackEntry.arguments?.getInt("assessmentId") ?: 0
                        val title = backStackEntry.arguments?.getString("title") ?: "Assessment"
                        val durationMinutes = backStackEntry.arguments?.getInt("durationMinutes") ?: 10
                        val assessmentBackStackEntry = remember(backStackEntry) {
                            navController.getBackStackEntry("assessment_graph")
                        }
                        val assessmentViewModel: AssessmentViewModel = hiltViewModel(assessmentBackStackEntry)

                        // Show interstitial ad before starting quiz/exam (for non-premium users)
                        var adDismissed by remember { mutableStateOf(false) }

                        LaunchedEffect(assessmentId) {
                            Log.d("MainActivity", "🎯 Quiz/Exam start - isPremium: $isPremium, adDismissed: $adDismissed")
                            if (!isPremium && !adDismissed) {
                                Log.d("MainActivity", "📺 Attempting to show quiz start ad...")
                                adManager.showInterstitialOnQuizStart(
                                    activity = activity,
                                    onAdDismissed = {
                                        Log.d("MainActivity", "✅ Quiz start ad dismissed or skipped")
                                        adDismissed = true
                                    }
                                )
                            } else {
                                adDismissed = true
                            }
                        }

                        if (adDismissed || isPremium) {
                            TakeAssessmentScreen(
                                assessmentId = assessmentId,
                                assessmentTitle = title,
                                durationMinutes = durationMinutes,
                                onFinish = {
                                    // Show interstitial ad after quiz/exam completion (for non-premium users)
                                    if (!isPremium) {
                                        adManager.showInterstitialOnQuizComplete(
                                            activity = activity,
                                            onAdDismissed = {
                                                navController.navigate("assessment/result") {
                                                    popUpTo(backStackEntry.destination.route!!) { inclusive = true }
                                                }
                                            }
                                        )
                                    } else {
                                        navController.navigate("assessment/result") {
                                            popUpTo(backStackEntry.destination.route!!) { inclusive = true }
                                        }
                                    }
                                },
                                onExit = {
                                    assessmentViewModel.resetAssessment()
                                    navController.navigate(Screen.Quiz.route) {
                                        popUpTo(Screen.Quiz.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                viewModel = assessmentViewModel
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    composable("assessment/result") {
                        val assessmentBackStackEntry = remember(it) {
                            navController.getBackStackEntry("assessment_graph")
                        }
                        val viewModel: AssessmentViewModel = hiltViewModel(assessmentBackStackEntry)
                        val result by viewModel.assessmentResult.collectAsState()
                        result?.let { assessmentResult ->
                            AssessmentResultScreen(
                                result = assessmentResult,
                                onBackToList = {
                                    viewModel.resetResult()
                                    navController.navigate(Screen.Quiz.route) {
                                        popUpTo("assessment_graph") { inclusive = true }
                                    }
                                },
                                onRetry = {
                                    viewModel.resetResult()
                                    navController.popBackStack()
                                },
                                isPremium = isPremium
                            )
                        } ?: Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    composable("assessment/history") {
                        val assessmentBackStackEntry = remember(it) {
                            navController.getBackStackEntry("assessment_graph")
                        }
                        val assessmentViewModel: AssessmentViewModel = hiltViewModel(assessmentBackStackEntry)
                        AssessmentHistoryScreen(
                            onBack = { navController.popBackStack() },
                            viewModel = assessmentViewModel
                        )
                    }
                }

                composable(Screen.Settings.route) {
                    ModernSettingsScreen(viewModel = hiltViewModel())
                }
                composable(Screen.Ebook.route) {
                    EbookScreen(
                        viewModel = hiltViewModel(),
                        onNavigateToPdf = { pdfUrl, title ->
                            val encodedUrl = Uri.encode(pdfUrl)
                            navController.navigate("pdf_viewer/$encodedUrl/$title")
                        },
                        onNavigateToPremiumLock = {
                            navController.navigate(Screen.PremiumLock.route)
                        },
                        isPremium = isPremium
                    )
                }
                composable(Screen.PremiumLock.route) {
                    PremiumLockScreen(
                        onNavigateToProfile = {
                            navController.navigate(Screen.Profile.route) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    )
                }
                composable(Screen.Profile.route) {
                    ProfileScreen(viewModel = hiltViewModel())
                }
                composable(Screen.Leaderboard.route) {
                    com.webtech.kamuskorea.ui.screens.leaderboard.LeaderboardScreen(
                        viewModel = hiltViewModel(),
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.Achievements.route) {
                    com.webtech.kamuskorea.ui.screens.achievements.AchievementsScreen(
                        viewModel = hiltViewModel(),
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = "pdf_viewer/{pdfUrl}/{title}",
                    arguments = listOf(
                        navArgument("pdfUrl") { type = NavType.StringType },
                        navArgument("title") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val pdfUrl = backStackEntry.arguments?.getString("pdfUrl")?.let { Uri.decode(it) }
                    val title = backStackEntry.arguments?.getString("title")
                    if (pdfUrl != null && title != null) {
                        var adDismissed by remember { mutableStateOf(false) }

                        LaunchedEffect(pdfUrl) {
                            Log.d("MainActivity", "📄 PDF open - isPremium: $isPremium, adDismissed: $adDismissed")
                            if (!isPremium && !adDismissed) {
                                Log.d("MainActivity", "📺 Attempting to show PDF open ad...")
                                adManager.showInterstitialOnPdfOpen(
                                    activity = activity,
                                    onAdDismissed = {
                                        Log.d("MainActivity", "✅ PDF open ad dismissed or skipped")
                                        adDismissed = true
                                    }
                                )
                            } else {
                                adDismissed = true
                            }
                        }

                        if (adDismissed || isPremium) {
                            PdfViewerScreen(
                                navController = navController,
                                pdfUrl = pdfUrl,
                                title = title
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Error: Invalid E-Book data.")
                        }
                    }
                }
            }
        }
    }
}