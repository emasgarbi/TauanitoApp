package com.example.tauanitoapp.ui.history

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tauanitoapp.R
import com.example.tauanitoapp.data.model.HistoryRecord
import com.example.tauanitoapp.data.model.SensorReading

// Colori Verde Premium per il bottone
private val GreenGradientStart = Color(0xFF2E7D32)
private val GreenGradientEnd   = Color(0xFF1B5E20)
private val GreenBorderColor   = Color(0xFF81C784).copy(alpha = 0.6f)

@Composable
fun HistoryRoute(
    deviceId: String,
    onBack: () -> Unit,
    onNavigateToInsights: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    
    val factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(application, deviceId) as T
        }
    }
    val viewModel: HistoryViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()

    val saveFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            viewModel.downloadCsvToUri(it)
        }
    }

    LaunchedEffect(state.showFilePicker) {
        if (state.showFilePicker) {
            saveFileLauncher.launch("tauanito_${deviceId}.csv")
            viewModel.onFilePickerLaunched()
        }
    }

    HistoryScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::loadHistory,
        onDownloadCsv = viewModel::startDownloadProcess,
        onClearDownloadError = viewModel::clearDownloadError,
        onNavigateToInsights = { onNavigateToInsights(deviceId) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onDownloadCsv: () -> Unit,
    onClearDownloadError: () -> Unit,
    onNavigateToInsights: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.downloadError) {
        state.downloadError?.let {
            snackbarHostState.showSnackbar(it)
            onClearDownloadError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Image(
                painter = painterResource(R.drawable.immaginesfondo),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            )

            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { 
                        Text(
                            text = state.history?.deviceName ?: "Storico Dati",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                        }
                    },
                    actions = {
                        // ── PULSANTE INSIGHTS ──────────────────────────
                        if (state.history != null) {
                            Surface(
                                onClick = onNavigateToInsights,
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Transparent,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .height(30.dp)
                                    .shadow(3.dp, RoundedCornerShape(8.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(Color(0xFF1976D2), Color(0xFF0D47A1))
                                            )
                                        )
                                        .border(BorderStroke(1.dp, Color(0xFF64B5F6).copy(alpha = 0.6f)), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "INSIGHTS", 
                                        color = Color.White, 
                                        fontSize = 11.sp, 
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        // ── PULSANTE VERDE CSV ──────────────────────────
                        if (state.isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp).padding(end = 12.dp),
                                color = GreenGradientStart,
                                strokeWidth = 3.dp
                            )
                        } else if (state.history != null) {
                            Surface(
                                onClick = onDownloadCsv,
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Transparent,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .height(30.dp)
                                    .widthIn(min = 70.dp)
                                    .shadow(3.dp, RoundedCornerShape(8.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(GreenGradientStart, GreenGradientEnd)
                                            )
                                        )
                                        .border(BorderStroke(1.dp, GreenBorderColor), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.KeyboardArrowDown, 
                                            contentDescription = null, 
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "CSV", 
                                            color = Color.White, 
                                            fontSize = 11.sp, 
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )

                when {
                    state.isLoading && state.history == null -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                    state.errorMessage != null && state.history == null -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(state.errorMessage, color = Color.White)
                            Button(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
                                Text("Riprova")
                            }
                        }
                    }
                    state.history != null -> {
                        if (state.history.records.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Nessuno storico disponibile", color = Color.White)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.history.records) { record ->
                                    HistoryRecordCard(record)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryRecordCard(record: HistoryRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = record.timestamp,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            record.readings.forEach { reading ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(reading.name, color = Color.White, fontSize = 14.sp)
                    Text(
                        text = if (reading.unit != null) "${reading.value} ${reading.unit}" else reading.value,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
