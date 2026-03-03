package com.example.tauanitoapp.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tauanitoapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    refreshIntervalLabel: String,
    onThemeToggle: (Boolean) -> Unit,
    onRefreshIntervalChange: (String) -> Unit,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Sfondo coerente con l'app
        Image(
            painter = painterResource(R.drawable.immaginesfondo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        
        // Overlay dinamico: Scuro in Dark Mode, Chiaro in Light Mode
        val overlayColor = if (isDarkMode) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.7f)
        val contentColor = if (isDarkMode) Color.White else Color.Black
        val cardColor = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(overlayColor)
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Impostazioni", color = contentColor) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = contentColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sezione Tema
                SettingsCard(title = "Aspetto", isDarkMode = isDarkMode, containerColor = cardColor) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = contentColor)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Tema Scuro", color = contentColor)
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = onThemeToggle
                        )
                    }
                }

                // Sezione Aggiornamento
                SettingsCard(title = "Dati", isDarkMode = isDarkMode, containerColor = cardColor) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = contentColor)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Frequenza Aggiornamento", color = contentColor)
                        }
                        
                        val options = listOf("30 secondi", "5 minuti", "30 minuti")
                        
                        options.forEach { option ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (option == refreshIntervalLabel),
                                    onClick = { onRefreshIntervalChange(option) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFF1565C0),
                                        unselectedColor = contentColor.copy(alpha = 0.6f)
                                    )
                                )
                                Text(
                                    text = option,
                                    color = contentColor,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = "Tauanito App v1.0.3",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = contentColor.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    isDarkMode: Boolean,
    containerColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = if (isDarkMode) Color(0xFF4CAF50) else Color(0xFF1B5E20),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}
