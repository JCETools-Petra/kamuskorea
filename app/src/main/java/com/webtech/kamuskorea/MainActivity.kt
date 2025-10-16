// File: app/src/main/java/com/webtech/kamuskorea/MainActivity.kt

package com.webtech.kamuskorea

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.webtech.kamuskorea.data.UserRepository
import com.webtech.kamuskorea.ui.navigation.Screen
import com.webtech.kamuskorea.ui.screens.*
import com.webtech.kamuskorea.ui.screens.auth.LoginScreen
import com.webtech.kamuskorea.ui.screens.auth.RegisterScreen
import com.webtech.kamuskorea.ui.screens.profile.ProfileScreen
import com.webtech.kamuskorea.ui.screens.settings.SettingsScreen
import com.webtech.kamuskorea.ui.screens.settings.SettingsViewModel
import com.webtech.kamuskorea.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

// Tambahkan anotasi ini untuk mengaktifkan Hilt
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Inject UserRepository menggunakan Hilt
    @Inject
    lateinit var userRepository: UserRepository

    // Inject FirebaseAuth
    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContent {
            // Kita akan mengambil isPremium langsung dari userRepository
            val isPremium by userRepository.isPremium.collectAsState()

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
    // ViewModel sekarang diambil menggunakan hiltViewModel()
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
        val context = LocalContext.current
        val application = context.applicationContext as Application

        LaunchedEffect(navController) {
            navController.currentBackStackEntryFlow.collect { backStackEntry ->
                currentTitle = when (backStackEntry.destination.route) {
                    Screen.Home.route -> "Menu Utama"
                    Screen.Dictionary.route -> "Kamus"
                    Screen.Ebook.route -> "E-Book"
                    Screen.Quiz.route -> "Latihan"
                    Screen.Memorization.route -> "Hafalan"
                    Screen.Profile.route -> "Profil & Langganan"
                    Screen.Settings.route -> "Pengaturan"
                    else -> "Kamus Korea"
                }
            }
        }

        val menuItems = listOf(
            NavItem("Kamus", Icons.Default.MenuBook, Screen.Dictionary),
            NavItem("E-Book", Icons.Default.AutoStories, Screen.Ebook),
            NavItem("Hafalan", Icons.Default.Bookmark, Screen.Memorization),
            NavItem("Latihan", Icons.Default.Quiz, Screen.Quiz)
        )

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val isAuthScreen = currentRoute == Screen.Login.route || currentRoute == Screen.Register.route

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !isAuthScreen,
            drawerContent = {
                ModalDrawerSheet {
                    // ... (Isi DrawerContent tetap sama seperti kodemu, tidak perlu diubah)
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
                        }) {
                            Text("Profil & Langganan")
                        }
                    }
                    Divider()
                    NavigationDrawerItem(icon = { Icon(Icons.Default.Home, contentDescription = "Home") }, label = { Text("Menu Utama") }, selected = Screen.Home.route == currentRoute, onClick = { scope.launch { drawerState.close() }; navController.navigate(Screen.Home.route) }, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding))
                    menuItems.forEach { item ->
                        NavigationDrawerItem(icon = { Icon(item.icon, contentDescription = item.label) }, label = { Text(item.label) }, selected = item.screen.route == currentRoute, onClick = { scope.launch { drawerState.close() }; navController.navigate(item.screen.route) { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } }, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Divider()
                    NavigationDrawerItem(icon = { Icon(Icons.Default.Settings, contentDescription = "Pengaturan") }, label = { Text("Pengaturan") }, selected = currentRoute == Screen.Settings.route, onClick = { scope.launch { drawerState.close() }; navController.navigate(Screen.Settings.route) }, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding))
                    NavigationDrawerItem(icon = { Icon(Icons.Default.Logout, contentDescription = "Logout") }, label = { Text("Logout") }, selected = false, onClick = { scope.launch { drawerState.close() }; val googleSignInClient = GoogleSignIn.getClient(application, GoogleSignInOptions.DEFAULT_SIGN_IN); googleSignInClient.signOut().addOnCompleteListener { firebaseAuth.signOut(); navController.navigate(Screen.Login.route) { popUpTo(0) } } }, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding))
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        ) {
            Scaffold(
                topBar = {
                    if (!isAuthScreen) {
                        TopAppBar(title = { Text(currentTitle) }, navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, contentDescription = "Menu") } })
                    }
                }
            ) { innerPadding ->
                NavHost(navController = navController, startDestination = startDestination, modifier = Modifier.padding(innerPadding)) {
                    composable(Screen.Login.route) { LoginScreen(onLoginSuccess = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } } }, onNavigateToRegister = { navController.navigate(Screen.Register.route) }) }
                    composable(Screen.Register.route) { RegisterScreen(onNavigateToLogin = { navController.popBackStack() }) }
                    composable(Screen.Home.route) { HomeScreen(navController = navController) }
                    composable(Screen.Dictionary.route) { DictionaryScreen() }

                    // Teruskan isPremium ke layar yang membutuhkan
                    composable(Screen.Ebook.route) { EbookScreen(isPremium = isPremium, onNavigateToProfile = { navController.navigate(Screen.Profile.route) }) }
                    composable(Screen.Memorization.route) { MemorizationScreen(isPremium = isPremium, onNavigateToProfile = { navController.navigate(Screen.Profile.route) }) }
                    composable(Screen.Quiz.route) { QuizScreen(isPremium = isPremium, onNavigateToProfile = { navController.navigate(Screen.Profile.route) }) }

                    // ProfileScreen dan SettingsScreen bisa mengambil ViewModel-nya sendiri
                    composable(Screen.Profile.route) { ProfileScreen() }
                    composable(Screen.Settings.route) { SettingsScreen() }
                }
            }
        }
    }
}