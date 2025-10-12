package com.webtech.kamuskorea.ui.screens

import android.app.Application
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.webtech.kamuskorea.data.local.Word
import com.webtech.kamuskorea.ui.screens.dictionary.DictionaryViewModel
import kotlinx.coroutines.delay

@Composable
fun DictionaryScreen(
    viewModel: DictionaryViewModel = viewModel(
        factory = DictionaryViewModel.provideFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val words by viewModel.words.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var languageMode by remember { mutableStateOf("korea") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    languageMode = if (languageMode == "korea") "indonesia" else "korea"
                }
            ) {
                Text(if (languageMode == "korea") "KO -> ID" else "ID -> KO")
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier.weight(1f),
                label = { Text("Cari...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                singleLine = true
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(words) { word ->
                WordItem(word = word, languageMode = languageMode)
            }
        }
    }
}

@Composable
fun WordItem(word: Word, languageMode: String) {
    var isRevealed by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = isRevealed) {
        if (isRevealed) {
            delay(5000L)
            isRevealed = false
        }
    }

    val rotationAngle by animateFloatAsState(
        targetValue = if (isRevealed) -10f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "rotation"
    )
    val alphaValue by animateFloatAsState(
        targetValue = if (isRevealed) 0f else 1f,
        animationSpec = tween(durationMillis = 300, delayMillis = 100),
        label = "alpha"
    )
    val scaleValue by animateFloatAsState(
        targetValue = if (isRevealed) 1.1f else 1f,
        animationSpec = tween(durationMillis = 400),
        label = "scale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .clickable {
                    if (!isRevealed) {
                        isRevealed = true
                    }
                }
        ) {
            // Lapisan Bawah
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                if (languageMode == "korea") {
                    Text(text = word.indonesian, fontSize = 18.sp)
                } else {
                    Text(text = word.korean, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "[${word.romanization}]",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Lapisan Atas (Stiker)
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = alphaValue
                        rotationZ = rotationAngle
                        scaleX = scaleValue
                        scaleY = scaleValue
                        transformOrigin = TransformOrigin(0.9f, 0.1f)
                    },
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    if (languageMode == "korea") {
                        Text(text = word.korean, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "[${word.romanization}]",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(text = word.indonesian, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}