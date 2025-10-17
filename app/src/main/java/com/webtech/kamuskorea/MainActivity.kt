package com.webtech.kamuskorea

import android.app.Application
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
import com.webtech.kamuskorea.ui.navigation.Screen
import com.webtech.kamuskorea.ui.screens.*
import com.webtech.kamuskorea.ui.screens.auth.LoginScreen
import com.webtech.kamuskorea.ui.screens.auth.RegisterScreen
import com.webtech.kamuskorea.ui.screens.ebook.PdfViewerScreen
import com.webtech.kamuskorea.ui.screens.profile.ProfileScreen
import com.webtech.kamuskorea.ui.screens.settings.SettingsScreen
import com.webtech.kamuskorea.ui.screens.settings.SettingsViewModel
import com.webtech.kamuskorea.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContent {
            val isPremium by userRepository.isPremium.collectAsState(initial = false)
            MainApp(
                firebaseAuth = firebaseAuth,
                isPremium = isPremium
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
    isPremium: Boolean
) {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val currentTheme by settingsViewModel.currentTheme.collectAsState()
    val useDarkTheme = isSystemInDarkTheme()

    val colors = when (currentTheme) {
        "Forest" -> if (useDarkTheme) ForestDarkColorScheme else ForestLightColorScheme
        "Ocean" -> if (useDarkTheme) OceanDarkColorScheme else OceanLightColorScheme
        else -> if (useDarkTheme) DarkColorScheme else LightColorScheme
    }

    KamusKoreaTheme(darkTheme = useDarkTheme, dynamicColor = false, colorScheme = colors) {

        val navController = rememberNavController()
        val startDestination = if (firebaseAuth.currentUser != null) Screen.Home.route else Screen.Login.route
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        var currentTitle by remember { mutableStateOf("Kamus Korea") }
        val application = LocalContext.current.applicationContext as Application

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        LaunchedEffect(currentRoute) {
            currentTitle = when {
                currentRoute == Screen.Home.route -> "Menu Utama"
                currentRoute == Screen.Dictionary.route -> "Kamus"
                currentRoute == Screen.Ebook.route -> "E-Book"
                currentRoute == Screen.Quiz.route -> "Latihan"
                currentRoute == Screen.Memorization.route -> "Hafalan"
                currentRoute == Screen.Profile.route -> "Profil & Langganan"
                currentRoute == Screen.Settings.route -> "Pengaturan"
                currentRoute?.startsWith("pdf_viewer/") == true ->
                    navBackStackEntry?.arguments?.getString("title") ?: "Baca E-Book"
                else -> "Kamus Korea"
            }
        }

        val menuItems = listOf(
            NavItem("Kamus", Icons.AutoMirrored.Outlined.MenuBook, Screen.Dictionary),
            NavItem("E-Book", Icons.Outlined.AutoStories, Screen.Ebook),
            NavItem("Hafalan", Icons.Outlined.Bookmark, Screen.Memorization),
            NavItem("Latihan", Icons.Outlined.Quiz, Screen.Quiz)
        )

        val isAuthScreen = currentRoute == Screen.Login.route || currentRoute == Screen.Register.route
        val isPdfViewerScreen = currentRoute?.startsWith("pdf_viewer/") == true

        ModalNavigationDrawer(
            drawerState = drawerState,
            // --- INI BAGIAN YANG DIPERBAIKI ---
            gesturesEnabled = !isAuthScreen && !isPdfViewerScreen,
            drawerContent = {
                ModalDrawerSheet {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        AsyncImage(
                            model = firebaseAuth.currentUser?.photoUrl,
                            contentDescription = "Foto Profil",
                            modifier = Modifier.size(80.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(id = R.drawable.ic_default_profile),
                            error = painterResource(id = R.drawable.ic_default_profile)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        val displayName = firebaseAuth.currentUser?.displayName
                        val email = firebaseAuth.currentUser?.email
                        Text(text = if (!displayName.isNullOrBlank()) displayName else email ?: "Pengguna", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Screen.Profile.route)
                        }) { Text("Profil & Langganan") }
                    }
                    HorizontalDivider()
                    NavigationDrawerItem(icon = { Icon(Icons.Default.Home, contentDescription = "Home") }, label = { Text("Menu Utama") }, selected = Screen.Home.route == currentRoute, onClick = { scope.launch { drawerState.close() }; navController.navigate(Screen.Home.route) }, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding))
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
                    NavigationDrawerItem(icon = { Icon(Icons.Default.Settings, contentDescription = "Pengaturan") }, label = { Text("Pengaturan") }, selected = currentRoute == Screen.Settings.route, onClick = { scope.launch { drawerState.close() }; navController.navigate(Screen.Settings.route) }, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding))
                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout") },
                        label = { Text("Logout") },
                        selected = false,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                val googleSignInClient = GoogleSignIn.getClient(application, GoogleSignInOptions.DEFAULT_SIGN_IN)
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
                                if (isPdfViewerScreen) {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
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
                    composable(Screen.Login.route) { LoginScreen(onLoginSuccess = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } } }, onNavigateToRegister = { navController.navigate(Screen.Register.route) }) }
                    composable(Screen.Register.route) { RegisterScreen(onNavigateToLogin = { navController.popBackStack() }) }
                    composable(Screen.Home.route) { HomeScreen(navController = navController) }
                    composable(Screen.Dictionary.route) { DictionaryScreen(viewModel = hiltViewModel()) }
                    composable(Screen.Memorization.route) { MemorizationScreen(isPremium = isPremium, onNavigateToProfile = { navController.navigate(Screen.Profile.route) }) }
                    composable(Screen.Quiz.route) { QuizScreen(isPremium = isPremium, onNavigateToProfile = { navController.navigate(Screen.Profile.route) }) }
                    composable(Screen.Settings.route) { SettingsScreen(viewModel = hiltViewModel()) }

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
                        ProfileScreen(
                            viewModel = hiltViewModel()
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
                            PdfViewerScreen(
                                navController = navController,
                                pdfUrl = pdfUrl,
                                title = title
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Error: Data E-Book tidak valid.")
                            }
                        }
                    }
                }
            }
        }
    }
}