package com.webtech.kamuskorea.ui.screens.settings

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.provideFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val themeOptions = listOf("Default", "Forest", "Ocean")
    val currentTheme by viewModel.currentTheme.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Pilih Tema Aplikasi",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        items(themeOptions.size) { index ->
            val themeName = themeOptions[index]
            val isSelected = themeName == currentTheme

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.changeTheme(themeName) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = themeName,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Tema Terpilih",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}