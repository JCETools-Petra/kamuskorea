package com.webtech.kamuskorea.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.webtech.kamuskorea.ui.navigation.Screen

@Composable
fun HomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Pilih Menu",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        MenuGrid(navController = navController)
    }
}

@Composable
fun MenuGrid(navController: NavController) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MenuItem(navController = navController, icon = Icons.Default.MenuBook, text = "Kamus", route = Screen.Dictionary.route, modifier = Modifier.weight(1f))
            // PERBAIKAN 1: Mengarahkan E-Book ke Screen.Ebook.route
            MenuItem(navController = navController, icon = Icons.Default.AutoStories, text = "E-Book", route = Screen.Ebook.route, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // PERBAIKAN 2: Mengarahkan Hafalan ke Screen.Memorization.route
            MenuItem(navController = navController, icon = Icons.Default.Bookmark, text = "Hafalan", route = Screen.Memorization.route, modifier = Modifier.weight(1f))
            MenuItem(navController = navController, icon = Icons.Default.Quiz, text = "Latihan", route = Screen.Quiz.route, modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuItem(navController: NavController, icon: ImageVector, text: String, route: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { navController.navigate(route) },
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = text, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        }
    }
}