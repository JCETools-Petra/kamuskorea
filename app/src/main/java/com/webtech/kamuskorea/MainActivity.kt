package com.webtech.kamuskorea

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.webtech.kamuskorea.ui.navigation.Screen
import com.webtech.kamuskorea.ui.screens.*
import com.webtech.kamuskorea.ui.screens.auth.LoginScreen
import com.webtech.kamuskorea.ui.screens.auth.RegisterScreen
import com.webtech.kamuskorea.ui.screens.profile.ProfileScreen
import com.webtech.kamuskorea.ui.screens.profile.ProfileViewModel
import com.webtech.kamuskorea.ui.theme.KamusKoreaTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            KamusKoreaTheme {
                MainApp()
            }
        }
    }
}

// Data class untuk item navigasi
data class NavItem(
    val label: String,
    val icon: ImageVector,
    val screen: Screen
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val navController = rememberNavController()
    val firebaseAuth = FirebaseAuth.getInstance()
    val startDestination = if (firebaseAuth.currentUser != null) Screen.Dictionary.route else Screen.Login.route

    val application = LocalContext.current.applicationContext as Application
    val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.provideFactory(application))
    val isPremium by profileViewModel.hasActiveSubscription.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var currentTitle by remember { mutableStateOf("Kamus") }

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { backStackEntry ->
            currentTitle = when (backStackEntry.destination.route) {
                Screen.Dictionary.route -> "Kamus"
                Screen.Ebook.route -> "E-Book"
                Screen.Quiz.route -> "Latihan"
                Screen.Memorization.route -> "Hafalan"
                Screen.Profile.route -> "Profil"
                else -> "Kamus Korea" // Fallback title
            }
        }
    }

    val menuItems = listOf(
        NavItem("Kamus", Icons.Default.MenuBook, Screen.Dictionary),
        NavItem("E-Book", Icons.Default.AutoStories, Screen.Ebook),
        NavItem("Hafalan", Icons.Default.Bookmark, Screen.Memorization),
        NavItem("Latihan", Icons.Default.Quiz, Screen.Quiz),
        NavItem("Profil", Icons.Default.Person, Screen.Profile)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isAuthScreen = currentRoute == Screen.Login.route || currentRoute == Screen.Register.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Nonaktifkan gestur swipe untuk membuka drawer di halaman login/register
        gesturesEnabled = !isAuthScreen,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                menuItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = item.screen.route == currentRoute,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                // Hanya tampilkan TopAppBar jika bukan di halaman otentikasi
                if (!isAuthScreen) {
                    TopAppBar(
                        title = { Text(currentTitle) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            // --- SATU NAVHOST UNTUK SEMUA HALAMAN ---
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding)
            ) {
                // Halaman Otentikasi
                composable(route = Screen.Login.route) {
                    LoginScreen(
                        onLoginSuccess = { navController.navigate(Screen.Dictionary.route) { popUpTo(Screen.Login.route) { inclusive = true } } },
                        onNavigateToRegister = { navController.navigate(Screen.Register.route) }
                    )
                }
                composable(route = Screen.Register.route) {
                    RegisterScreen(
                        // onRegisterSuccess dihapus
                        onNavigateToLogin = {
                            // Cukup kembali ke halaman login
                            navController.popBackStack()
                        }
                    )
                }

                // Halaman Utama Aplikasi
                composable(Screen.Dictionary.route) { DictionaryScreen() }
                composable(Screen.Ebook.route) { EbookScreen(isPremium) { navController.navigate(Screen.Profile.route) } }
                composable(Screen.Memorization.route) { MemorizationScreen(isPremium) { navController.navigate(Screen.Profile.route) } }
                composable(Screen.Quiz.route) { QuizScreen(isPremium) { navController.navigate(Screen.Profile.route) } }
                composable(Screen.Profile.route) {
                    ProfileScreen(
                        onLogout = {
                            firebaseAuth.signOut()
                            navController.navigate(Screen.Login.route) { popUpTo(0) }
                        },
                        viewModel = profileViewModel
                    )
                }
            }
        }
    }
}