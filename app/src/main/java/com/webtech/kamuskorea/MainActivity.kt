package com.webtech.kamuskorea

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.webtech.kamuskorea.ui.navigation.Screen
import com.webtech.kamuskorea.ui.screens.DictionaryScreen
import com.webtech.kamuskorea.ui.screens.EbookScreen
import com.webtech.kamuskorea.ui.screens.MemorizationScreen
import com.webtech.kamuskorea.ui.screens.QuizScreen
import com.webtech.kamuskorea.ui.screens.auth.LoginScreen
import com.webtech.kamuskorea.ui.screens.auth.RegisterScreen
import com.webtech.kamuskorea.ui.screens.profile.ProfileScreen
import com.webtech.kamuskorea.ui.screens.profile.ProfileViewModel
import com.webtech.kamuskorea.ui.theme.KamusKoreaTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
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
                Screen.Profile.route -> "Profil & Langganan"
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = firebaseAuth.currentUser?.photoUrl,
                        contentDescription = "Foto Profil",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.ic_default_profile),
                        error = painterResource(id = R.drawable.ic_default_profile)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = firebaseAuth.currentUser?.displayName ?: "Pengguna",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Profile.route)
                    }) {
                        Text("Edit Profil & Langganan")
                    }
                }
                Divider()

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

                Spacer(modifier = Modifier.weight(1f))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Logout, contentDescription = "Logout") },
                    label = { Text("Logout") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        val googleSignInClient = GoogleSignIn.getClient(application, GoogleSignInOptions.DEFAULT_SIGN_IN)
                        googleSignInClient.signOut().addOnCompleteListener {
                            firebaseAuth.signOut()
                            navController.navigate(Screen.Login.route) { popUpTo(0) }
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
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
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
                composable(route = Screen.Login.route) {
                    LoginScreen(
                        onLoginSuccess = { navController.navigate(Screen.Dictionary.route) { popUpTo(Screen.Login.route) { inclusive = true } } },
                        onNavigateToRegister = { navController.navigate(Screen.Register.route) }
                    )
                }
                composable(route = Screen.Register.route) {
                    RegisterScreen(
                        onNavigateToLogin = { navController.popBackStack() }
                    )
                }

                composable(Screen.Dictionary.route) { DictionaryScreen() }
                composable(Screen.Ebook.route) { EbookScreen(isPremium) { navController.navigate(Screen.Profile.route) } }
                composable(Screen.Memorization.route) { MemorizationScreen(isPremium) { navController.navigate(Screen.Profile.route) } }
                composable(Screen.Quiz.route) { QuizScreen(isPremium) { navController.navigate(Screen.Profile.route) } }

                composable(Screen.Profile.route) {
                    ProfileScreen(
                        viewModel = profileViewModel,
                        onLogout = {
                            val googleSignInClient = GoogleSignIn.getClient(application, GoogleSignInOptions.DEFAULT_SIGN_IN)
                            googleSignInClient.signOut().addOnCompleteListener {
                                firebaseAuth.signOut()
                                navController.navigate(Screen.Login.route) { popUpTo(0) }
                            }
                        }
                    )
                }
            }
        }
    }
}