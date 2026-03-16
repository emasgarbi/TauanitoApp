package com.example.tauanitoapp.ui.insights

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.view.View
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.toArgb
import com.example.tauanitoapp.R
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.component.shape.shader.fromBrush
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.component.shape.shader.DynamicShaders
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import com.patrykandpatrick.vico.core.component.text.TextComponent
import androidx.compose.ui.unit.TextUnit

private val GreenGradientStart = Color(0xFF2E7D32)
private val GreenGradientEnd   = Color(0xFF1B5E20)
private val GreenBorderColor   = Color(0xFF81C784).copy(alpha = 0.6f)

@Composable
fun InsightsRoute(
    deviceId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    
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
                        text = "Analisi Avanzata",
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
                    CircularProgressIndicator(color = Color.White)
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
                    } else {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Seleziona almeno un sensore per visualizzare l'analisi", color = Color.White.copy(alpha = 0.7f))
                            }
                        }
                    }
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Sensori da confrontare:",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        if (allSensors.isEmpty()) {
            Text(
                "Nessun dato numerico rilevato per questo dispositivo.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allSensors.forEach { sensor ->
                    val isSelected = selectedSensors.contains(sensor)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggle(sensor) },
                        label = { Text(sensor) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenGradientStart,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ComparisonChart(insights: List<InsightData>) {
    Card(
        modifier = Modifier.fillMaxWidth().height(250.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Andamento Temporale", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            
            val entryModel = remember(insights) {
                if (insights.isEmpty()) return@remember null
                
                val series = insights.map { insight ->
                    insight.values.mapIndexed { index, value ->
                        FloatEntry(index.toFloat(), value)
                    }
                }
                ChartEntryModelProducer(series).getModel()
            }

            if (entryModel != null) {
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
                        valueFormatter = { value, _ -> 
                            value.toInt().toString()
                        }
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(insight.sensorName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(insight.trend ?: "", color = Color.White)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Previsione: ${insight.prediction ?: "Dati insufficienti"}",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp
            )
            if (insight.unit != null) {
                Text(
                    text = "Unità: ${insight.unit}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
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
