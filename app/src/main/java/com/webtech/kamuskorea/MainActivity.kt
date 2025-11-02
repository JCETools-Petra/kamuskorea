package com.webtech.kamuskorea

import android.app.Application
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import com.webtech.kamuskorea.ui.screens.auth.LoginScreen
import com.webtech.kamuskorea.ui.screens.auth.RegisterScreen
import com.webtech.kamuskorea.ui.screens.dictionary.KamusSyncViewModel
import com.webtech.kamuskorea.ui.screens.ebook.PdfViewerScreen
import com.webtech.kamuskorea.ui.screens.onboarding.OnboardingScreen
import com.webtech.kamuskorea.ui.screens.profile.ProfileScreen
import com.webtech.kamuskorea.ui.screens.settings.ModernSettingsScreen
import com.webtech.kamuskorea.ui.screens.settings.SettingsViewModel
import com.webtech.kamuskorea.ui.screens.splash.AnimatedSplashScreen
import com.webtech.kamuskorea.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.navigation.navArgument
import com.webtech.kamuskorea.ui.screens.auth.ForgotPasswordScreen
import com.webtech.kamuskorea.ui.screens.auth.ResetPasswordScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    private val kamusSyncViewModel: KamusSyncViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        kamusSyncViewModel.syncDatabase()

        setContent {
            val isPremium by userRepository.isPremium.collectAsState(initial = false)
            val textScale by settingsViewModel.textScale.collectAsState()
            val language by settingsViewModel.language.collectAsState()

            var showSplash by remember { mutableStateOf(true) }

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
                        MainApp(
                            firebaseAuth = firebaseAuth,
                            isPremium = isPremium
                        )
                    }
                }
            }
        }
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

data class NavItem(
    val label: String,
    val icon: ImageVector,
    val screen: Screen
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    firebaseAuth: FirebaseAuth,
    isPremium: Boolean
) {
    val strings = LocalStrings.current
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val currentTheme by settingsViewModel.currentTheme.collectAsState()
    val useDarkTheme = isSystemInDarkTheme()

    val colors = when (currentTheme) {
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

    KamusKoreaTheme(darkTheme = useDarkTheme, dynamicColor = false, colorScheme = colors) {
        val navController = rememberNavController()
        val hasSeenOnboarding = remember { mutableStateOf(false) }
        val startDestination = when {
            !hasSeenOnboarding.value -> Screen.Onboarding.route
            firebaseAuth.currentUser != null -> Screen.Home.route
            else -> Screen.Login.route
        }
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        var currentTitle by remember { mutableStateOf(strings.appName) }
        val application = LocalContext.current.applicationContext as Application

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        LaunchedEffect(currentRoute) {
            currentTitle = when {
                currentRoute == Screen.Home.route -> strings.home
                currentRoute == Screen.Dictionary.route -> strings.dictionary
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
            NavItem(strings.ebook, Icons.Outlined.AutoStories, Screen.Ebook),
            NavItem(strings.memorization, Icons.Outlined.Bookmark, Screen.Memorization),
            NavItem(strings.quiz, Icons.Outlined.Quiz, Screen.Quiz)
        )

        val isAuthScreen = currentRoute == Screen.Login.route || currentRoute == Screen.Register.route
        val isPdfViewerScreen = currentRoute?.startsWith("pdf_viewer/") == true

        // ✅ Cek apakah rute saat ini ada di dalam alur assessment
        val isAssessmentFlow = currentRoute?.startsWith("assessment/") == true || currentRoute == Screen.Quiz.route

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !isAuthScreen && !isPdfViewerScreen,
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
                                    navController.navigate(Screen.Login.route) { popUpTo(0) }
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
                    if (!isAuthScreen) {
                        TopAppBar(
                            title = { Text(currentTitle) },
                            navigationIcon = {
                                // ✅ PERUBAHAN: Tampilkan panah kembali untuk SEMUA alur assessment
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
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(Screen.Login.route) {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            },
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

                    composable(
                        route = Screen.ResetPassword.route,
                        arguments = listOf(
                            navArgument("token") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val token = backStackEntry.arguments?.getString("token") ?: ""
                        ResetPasswordScreen(
                            token = token,
                            onNavigateToLogin = {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Screen.Home.route) {
                        ModernHomeScreen(navController = navController, isPremium = isPremium)
                    }
                    composable(Screen.Dictionary.route) {
                        ModernDictionaryScreen(viewModel = hiltViewModel())
                    }
                    composable(Screen.Memorization.route) {
                        MemorizationScreen(
                            isPremium = isPremium,
                            onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                        )
                    }

                    // ========== ASSESSMENT/QUIZ ROUTES (DIPERBAIKI) ==========

                    // ✅ KITA BUAT NESTED NAVIGATION GRAPH AGAR VIEWMODEL BISA DI-SHARE
                    navigation(
                        startDestination = Screen.Quiz.route,
                        route = "assessment_graph" // Ini adalah rute "induk" baru
                    ) {

                        // Rute ini sekarang adalah "assessment_graph/quiz"
                        // tapi kita ubah startDestination-nya saja
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
                                }
                            )
                        }

                        // List Quiz
                        composable("assessment/quiz/list") {
                            // ✅ Dapatkan VM yang di-scope ke "assessment_graph"
                            val assessmentBackStackEntry = remember(it) {
                                navController.getBackStackEntry("assessment_graph")
                            }
                            val assessmentViewModel: AssessmentViewModel = hiltViewModel(assessmentBackStackEntry)

                            AssessmentListScreen(
                                type = "quiz",
                                isPremium = isPremium,
                                onNavigateToAssessment = { assessmentId ->
                                    navController.navigate("assessment/take/$assessmentId/Quiz")
                                },
                                onNavigateToPremium = {
                                    navController.navigate(Screen.PremiumLock.route)
                                },
                                viewModel = assessmentViewModel // ✅ Kirim VM
                            )
                        }

                        // List Exam
                        composable("assessment/exam/list") {
                            // ✅ Dapatkan VM yang di-scope ke "assessment_graph"
                            val assessmentBackStackEntry = remember(it) {
                                navController.getBackStackEntry("assessment_graph")
                            }
                            val assessmentViewModel: AssessmentViewModel = hiltViewModel(assessmentBackStackEntry)

                            AssessmentListScreen(
                                type = "exam",
                                isPremium = isPremium,
                                onNavigateToAssessment = { assessmentId ->
                                    navController.navigate("assessment/take/$assessmentId/Exam")
                                },
                                onNavigateToPremium = {
                                    navController.navigate(Screen.PremiumLock.route)
                                },
                                viewModel = assessmentViewModel // ✅ Kirim VM
                            )
                        }

                        // Take Assessment
                        composable(
                            route = "assessment/take/{assessmentId}/{title}",
                            arguments = listOf(
                                navArgument("assessmentId") { type = NavType.IntType },
                                navArgument("title") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val assessmentId = backStackEntry.arguments?.getInt("assessmentId") ?: 0
                            val title = backStackEntry.arguments?.getString("title") ?: "Assessment"

                            // ✅ Dapatkan VM yang di-scope ke "assessment_graph"
                            val assessmentBackStackEntry = remember(backStackEntry) {
                                navController.getBackStackEntry("assessment_graph")
                            }
                            val assessmentViewModel: AssessmentViewModel = hiltViewModel(assessmentBackStackEntry)

                            TakeAssessmentScreen(
                                assessmentId = assessmentId,
                                assessmentTitle = title,
                                onFinish = { // ✅ PERUBAHAN: onFinish tanpa parameter
                                    navController.navigate("assessment/result") {
                                        // Pop up sampai layar 'take'
                                        popUpTo(backStackEntry.destination.route!!) { inclusive = true }
                                    }
                                },
                                viewModel = assessmentViewModel // ✅ Kirim VM
                            )
                        }

                        // Assessment Result
                        composable("assessment/result") {
                            // ✅ Dapatkan VM yang di-scope ke "assessment_graph"
                            val assessmentBackStackEntry = remember(it) {
                                navController.getBackStackEntry("assessment_graph")
                            }
                            val viewModel: AssessmentViewModel = hiltViewModel(assessmentBackStackEntry)
                            val result by viewModel.assessmentResult.collectAsState()

                            result?.let { assessmentResult ->
                                AssessmentResultScreen(
                                    result = assessmentResult,
                                    onBackToList = {
                                        viewModel.resetResult() // ✅ Reset state
                                        navController.navigate(Screen.Quiz.route) {
                                            popUpTo("assessment_graph") { inclusive = true } // Kembali ke root graph
                                        }
                                    },
                                    onRetry = {
                                        viewModel.resetResult() // ✅ Reset state
                                        navController.popBackStack() // Kembali ke layar list
                                    }
                                )
                            } ?: Box( // Tidak akan stuck di sini lagi
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        // Assessment History
                        composable("assessment/history") {
                            // ✅ Dapatkan VM yang di-scope ke "assessment_graph"
                            val assessmentBackStackEntry = remember(it) {
                                navController.getBackStackEntry("assessment_graph")
                            }
                            val assessmentViewModel: AssessmentViewModel = hiltViewModel(assessmentBackStackEntry)

                            AssessmentHistoryScreen(
                                onBack = { navController.popBackStack() },
                                viewModel = assessmentViewModel // ✅ Kirim VM
                            )
                        }
                    } // ========== AKHIR DARI navigation("assessment_graph") ==========

                    composable(Screen.Settings.route) {
                        ModernSettingsScreen(viewModel = hiltViewModel())
                    }
                    composable(Screen.Onboarding.route) {
                        OnboardingScreen(
                            onFinish = {
                                hasSeenOnboarding.value = true
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                                }
                            }
                        )
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
                            }
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
                                Text("Error: Invalid E-Book data.")
                            }
                        }
                    }
                }
            }
        }
    }
}