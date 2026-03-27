package com.example.tauanitoapp.ui.insights

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tauanitoapp.R
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.component.shape.shader.fromBrush
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.component.shape.shader.DynamicShaders
import com.patrykandpatrick.vico.core.component.text.TextComponent
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

private val GreenGradientStart = Color(0xFF2E7D32)
private val GreenGradientEnd   = Color(0xFF1B5E20)
private val WarningColor = Color(0xFFFFB74D) // Orange for warnings
private val ErrorColor = Color(0xFFEF5350)   // Red for anomalies
private val SuccessColor = Color(0xFF66BB6A) // Green for good health

@Composable
fun InsightsRoute(
    deviceId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application;
    
    val factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InsightsViewModel(application, deviceId) as T
        }
    }
    val viewModel: InsightsViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()

    InsightsScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::loadInsights,
        onToggleSensor = viewModel::toggleSensorSelection,
        onToggleDevice = viewModel::toggleDeviceComparison,
        onExportPdf = {
            exportInsightsToPdf(context, state)
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    state: InsightsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onToggleSensor: (String) -> Unit,
    onToggleDevice: (String) -> Unit,
    onExportPdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
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
                        text = "Intelligenza Tauanito",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onExportPdf, enabled = state.insights.isNotEmpty()) {
                        Icon(Icons.Default.Share, contentDescription = "Esporta PDF", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.Cyan)
                }
            } else if (state.errorMessage != null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.errorMessage, color = Color.White)
                    Button(onClick = onRetry) { Text("Riprova") }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. AI Summary Section
                    item {
                        SummaryCard(
                            summary = state.summary ?: "Analisi in corso...",
                            healthScore = state.healthScore
                        )
                    }

                    // 2. Intelligent Comparison Section
                    item {
                        ComparisonSection(
                            devices = state.comparisonDevices,
                            selectedIds = state.selectedComparisonDevices,
                            onToggle = onToggleDevice
                        )
                    }

                    // 3. Sensor Analysis Section
                    item {
                        SensorSelectionRow(
                            allSensors = state.insights.map { it.sensorName },
                            selectedSensors = state.selectedSensors,
                            onToggle = onToggleSensor
                        )
                    }

                    val filteredInsights = state.insights.filter { state.selectedSensors.contains(it.sensorName) }
                    
                    if (filteredInsights.isNotEmpty()) {
                        item {
                            ComparisonChart(filteredInsights)
                        }

                        items(filteredInsights) { insight ->
                            InsightCard(insight)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComparisonSection(
    devices: List<ComparisonDevice>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Confronto Gestione (Fleet Benchmarking)",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(devices) { device ->
                val isSelected = selectedIds.contains(device.deviceId)
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggle(device.deviceId) },
                    enabled = true,
                    label = { 
                        Text(
                            device.deviceName, 
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 12.sp
                        ) 
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.Cyan,
                        containerColor = Color.White.copy(alpha = 0.1f)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = Color.White.copy(alpha = 0.3f),
                        selectedBorderColor = Color.Cyan
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Mini card di confronto rapido per i selezionati
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            devices.filter { selectedIds.contains(it.deviceId) }.take(3).forEach { device ->
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(device.deviceName, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, maxLines = 1)
                        Text(
                            "${device.healthScore}%", 
                            color = if (device.healthScore > 80) SuccessColor else WarningColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text("Salute", color = Color.White.copy(alpha = 0.4f), fontSize = 8.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(summary: String, healthScore: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.dp, Brush.horizontalGradient(listOf(Color.Cyan.copy(alpha = 0.5f), Color.Magenta.copy(alpha = 0.5f)))),
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_view), // Placeholder for an AI-like icon
                        contentDescription = null,
                        tint = Color.Cyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tauanito AI INSIGHTS",
                        color = Color.Cyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
                
                Badge(containerColor = Color.Cyan.copy(alpha = 0.2f)) {
                    Text("LIVE", color = Color.Cyan, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Health Score Indicator with "Pulse" effect
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(70.dp)) {
                    CircularProgressIndicator(
                        progress = healthScore / 100f,
                        color = when {
                            healthScore > 80 -> SuccessColor
                            healthScore > 50 -> WarningColor
                            else -> ErrorColor
                        },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 4.dp
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$healthScore",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "SCORE",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(20.dp))
                
                Column {
                    Text(
                        text = summary,
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Analisi basata sui trend storici del dispositivo.",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}

@Composable
fun SensorSelectionRow(
    allSensors: List<String>,
    selectedSensors: Set<String>,
    onToggle: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(
            text = "Confronta Sensori:",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        if (allSensors.isEmpty()) {
            Text(
                "Nessun dato numerico per il confronto.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        } else {
            // Using a simple grid-like layout for checkboxes
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                allSensors.forEach { sensor ->
                    val isSelected = selectedSensors.contains(sensor)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onToggle(sensor) }.padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggle(sensor) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color.Cyan,
                                uncheckedColor = Color.White.copy(alpha = 0.5f),
                                checkmarkColor = Color.Black
                            )
                        )
                        Text(
                            text = sensor,
                            color = if (isSelected) Color.Cyan else Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ComparisonChart(insights: List<InsightData>) {
    // Verifichiamo se ci sono rilevazioni di più giorni
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val uniqueDays = remember(insights) {
        insights.flatMap { it.timestamps }.map { dateFormat.format(Date(it)) }.distinct().size
    }

    Card(
        modifier = Modifier.fillMaxWidth().height(if (uniqueDays < 2) 270.dp else 250.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Andamento Temporale", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                if (uniqueDays < 2) {
                    Text(
                        "Andamento orario",
                        color = Color.Cyan.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                } else {
                    Text(
                        "$uniqueDays giorni",
                        color = SuccessColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (uniqueDays < 2) {
                Text(
                    "💡 Per vedere l'andamento di più giorni, verifica che il dispositivo abbia trasmesso dati in più giornate",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            Spacer(Modifier.height(8.dp))

            // Trova il timestamp minimo tra tutti i sensori per normalizzare l'asse X
            val globalMinTs = remember(insights) {
                insights.flatMap { it.timestamps }.minOrNull() ?: 0L
            }

            val entryModel = remember(insights, globalMinTs) {
                if (insights.isEmpty()) return@remember null
                
                val series = insights.map { insight ->
                    insight.values.zip(insight.timestamps).map { (value, ts) ->
                        // X = minuti passati dal primo campionamento assoluto
                        val x = (ts - globalMinTs) / 60000f
                        FloatEntry(x, value)
                    }
                }
                ChartEntryModelProducer(series).getModel()
            }

            if (entryModel != null) {
                // Determiniamo se abbiamo più giorni o solo uno
                val maxTs = insights.flatMap { it.timestamps }.maxOrNull() ?: 0L
                val timeSpanMs = maxTs - globalMinTs
                val hasSingleDay = uniqueDays < 2

                // Se abbiamo un solo giorno, mostriamo l'orario
                // Se abbiamo più giorni, mostriamo solo la data
                val timeFormatter = remember(globalMinTs, hasSingleDay) {
                    val pattern = if (hasSingleDay) "HH:mm" else "dd/MM"
                    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                    val formatter: (Float) -> String = { value ->
                        val ts = globalMinTs + (value.toLong() * 60000L)
                        sdf.format(Date(ts))
                    }
                    formatter
                }

                Chart(
                    chart = lineChart(
                        lines = insights.mapIndexed { index, _ ->
                            LineChart.LineSpec(
                                lineColor = getChartColor(index).toArgb(),
                                lineBackgroundShader = DynamicShaders.fromBrush(
                                    Brush.verticalGradient(
                                        listOf(getChartColor(index).copy(alpha = 0.4f), Color.Transparent)
                                    )
                                )
                            )
                        }
                    ),
                    model = entryModel,
                    startAxis = rememberStartAxis(
                        label = rememberTextComponent(color = Color.White, textSize = 10.sp),
                        valueFormatter = { value, _ -> "%.1f".format(value) }
                    ),
                    bottomAxis = rememberBottomAxis(
                        label = rememberTextComponent(color = Color.White, textSize = 10.sp),
                        valueFormatter = { value, _ -> timeFormatter(value) },
                        guideline = null
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private fun getChartColor(index: Int): Color {
    val colors = listOf(Color.Cyan, Color.Yellow, Color.Magenta, Color.Green, Color.Red, Color.Blue)
    return colors[index % colors.size]
}

@Composable
fun rememberTextComponent(
    color: Color = Color.Black,
    textSize: TextUnit = 12.sp,
): TextComponent = remember(color, textSize) {
    TextComponent.Builder().apply {
        this.color = color.toArgb()
        this.textSizeSp = textSize.value
    }.build()
}

@Composable
fun InsightCard(insight: InsightData) {
    val cardColor = if (insight.isAnomaly) ErrorColor.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.15f)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (insight.isAnomaly) {
                        Icon(Icons.Default.Warning, contentDescription = "Anomalia", tint = Color.White, modifier = Modifier.padding(end = 8.dp))
                    } else {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(end = 8.dp))
                    }
                    Text(insight.sensorName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                
                Badge(
                    containerColor = if (insight.trend?.contains("crescita") == true) SuccessColor else if (insight.trend?.contains("calo") == true) WarningColor else Color.Gray
                ) {
                    Text(insight.trend?.replace(Regex("[^a-zA-Z ]"), "")?.trim() ?: "Stabile", modifier = Modifier.padding(4.dp), color = Color.White)
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Prediction Section
            Text(
                text = "Previsione",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = insight.prediction ?: "Dati insufficienti",
                color = Color.White,
                fontSize = 14.sp
            )
            
            Spacer(Modifier.height(8.dp))
            
            // Advice Section (The "Intelligent" Part)
            if (insight.advice != null) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.ThumbUp, 
                        contentDescription = "Consiglio", 
                        tint = GreenGradientStart,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = insight.advice,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
            
            if (insight.unit != null) {
                 Spacer(Modifier.height(8.dp))
                 Text(
                    text = "Unità: ${insight.unit}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = { content() }
    )
}

fun exportInsightsToPdf(context: Context, state: InsightsUiState) {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas
    val paint = android.graphics.Paint()
    
    var yPos = 40f
    
    paint.textSize = 20f
    paint.isFakeBoldText = true
    canvas.drawText("Report Analisi Tauanito - ${state.history?.deviceName ?: ""}", 40f, yPos, paint)
    yPos += 30f
    
    paint.textSize = 12f
    paint.isFakeBoldText = false
    canvas.drawText("Data generazione: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}", 40f, yPos, paint)
    yPos += 40f
    
    // Add Summary to PDF
    paint.textSize = 14f
    paint.isFakeBoldText = true
    canvas.drawText("Riepilogo Generale (Health Score: ${state.healthScore}/100)", 40f, yPos, paint)
    yPos += 20f
    paint.textSize = 12f
    paint.isFakeBoldText = false
    // Simple text wrapping for summary could be added here, but keeping it simple for now
    canvas.drawText(state.summary ?: "", 40f, yPos, paint)
    yPos += 40f
    
    state.insights.forEach { insight ->
        if (yPos > 750f) {
            return@forEach 
        }
        
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText(insight.sensorName, 40f, yPos, paint)
        yPos += 20f
        
        paint.textSize = 12f
        paint.isFakeBoldText = false
        canvas.drawText("Trend: ${insight.trend}", 50f, yPos, paint)
        yPos += 15f
        canvas.drawText("Previsione: ${insight.prediction}", 50f, yPos, paint)
        yPos += 15f
        if (insight.advice != null) {
            canvas.drawText("Consiglio: ${insight.advice}", 50f, yPos, paint)
            yPos += 15f
        }
        yPos += 25f
    }
    
    pdfDocument.finishPage(page)
    
    val file = File(context.cacheDir, "report_tauanito.pdf")
    try {
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()
        
        val uri = FileProvider.getUriForFile(context, "com.example.tauanitoapp.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Condividi Report PDF"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
