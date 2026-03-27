package com.example.tauanitoapp.ui.kiosk

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tauanitoapp.data.model.Device
import com.example.tauanitoapp.data.model.SensorReading
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

// ── Palette ───────────────────────────────────────────────────────────────────
private val BgTop       = Color(0xFF07111F)
private val BgBottom    = Color(0xFF0C1E35)
private val CardBg      = Color(0xFF0F2236)
private val CardBorder  = Color(0xFF1A3A5C)
private val AccentCyan  = Color(0xFF00E5FF)

private fun co2Color(ppm: Float) = when {
    ppm < 600  -> Color(0xFF4CAF50)
    ppm < 800  -> Color(0xFF8BC34A)
    ppm < 1000 -> Color(0xFFFFC107)
    ppm < 1200 -> Color(0xFFFF9800)
    else       -> Color(0xFFF44336)
}
private fun co2Label(ppm: Float) = when {
    ppm < 600  -> "Eccellente"
    ppm < 800  -> "Buona"
    ppm < 1000 -> "Discreta"
    ppm < 1200 -> "Scarsa"
    else       -> "Pessima"
}
private fun co2Fill(ppm: Float)  = ((ppm - 400f) / 1600f).coerceIn(0f, 1f)

// Normalizzazioni per ogni sensore
private fun sensorFill(name: String, value: Float): Float = when {
    name.contains("CO2",   ignoreCase = true) -> co2Fill(value)
    name.contains("Temp",  ignoreCase = true) -> (value / 50f).coerceIn(0f, 1f)
    name.contains("Umid",  ignoreCase = true) -> (value / 100f).coerceIn(0f, 1f)
    name.contains("Press", ignoreCase = true) -> ((value - 950f) / 100f).coerceIn(0f, 1f)
    name.contains("iaq",   ignoreCase = true) -> (value / 500f).coerceIn(0f, 1f)
    else -> (value / 100f).coerceIn(0f, 1f)
}
private fun sensorColor(name: String, value: Float): Color = when {
    name.contains("CO2",   ignoreCase = true) -> co2Color(value)
    name.contains("Temp",  ignoreCase = true) -> when {
        value < 16 || value > 30 -> Color(0xFFFF9800)
        value in 18f..26f        -> Color(0xFF4CAF50)
        else                     -> Color(0xFFFFC107)
    }
    name.contains("Umid",  ignoreCase = true) -> when {
        value < 30 || value > 70 -> Color(0xFFFF9800)
        else                     -> Color(0xFF64B5F6)
    }
    name.contains("Press", ignoreCase = true) -> Color(0xFFCE93D8)
    name.contains("iaq",   ignoreCase = true) -> when {
        value <= 50  -> Color(0xFF4CAF50)
        value <= 100 -> Color(0xFF8BC34A)
        value <= 150 -> Color(0xFFFFC107)
        value <= 200 -> Color(0xFFFF9800)
        else         -> Color(0xFFF44336)
    }
    else -> AccentCyan
}

// ── Entry point ───────────────────────────────────────────────────────────────
@Composable
fun KioskRoute(deviceId: String, onExit: () -> Unit) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    val vm: KioskViewModel = viewModel(factory = KioskViewModel.Factory(application, deviceId))
    val state by vm.uiState.collectAsState()

    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (context as Activity).window
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val ctrl = WindowInsetsControllerCompat(window, view)
        ctrl.hide(WindowInsetsCompat.Type.systemBars())
        ctrl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            ctrl.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    KioskScreen(
        state         = state,
        onRefresh     = vm::refresh,
        onCheckPin    = vm::checkPin,
        onExit        = onExit
    )
}

// ── Schermata principale ──────────────────────────────────────────────────────
@Composable
fun KioskScreen(
    state: KioskUiState,
    onRefresh: () -> Unit,
    onCheckPin: (String) -> Boolean,
    onExit: () -> Unit
) {
    var showExitDialog by remember { mutableStateOf(false) }
    // Flag separato: evita di chiamare onExit() durante la recomposizione del dialog
    var exitConfirmed  by remember { mutableStateOf(false) }

    // Orologio live
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); nowMs = System.currentTimeMillis() } }

    // Esegue onExit() solo dopo che il dialog è stato rimosso dalla composizione
    LaunchedEffect(exitConfirmed) {
        if (exitConfirmed) {
            delay(80)   // attende la chiusura del dialog
            onExit()
        }
    }

    BackHandler { /* bloccato in chiosco */ }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            KioskHeader(device = state.device, nowMs = nowMs, isLoading = state.isLoading, onRefresh = onRefresh)

            when {
                state.device != null -> KioskBody(device = state.device)
                state.errorMessage != null -> Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(state.errorMessage, color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp, textAlign = TextAlign.Center)
                }
                else -> Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentCyan, modifier = Modifier.size(48.dp))
                }
            }

            KioskFooter(lastUpdated = state.lastUpdated)
        }

        // Pulsante Esci — angolo in alto a sinistra, semi-trasparente
        TextButton(
            onClick = { showExitDialog = true },
            modifier = Modifier
                .align(Alignment.TopStart)
                .systemBarsPadding()
                .padding(8.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.35f))
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Esci dal chiosco",
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("Esci", fontSize = 12.sp)
        }
    }

    if (showExitDialog) {
        ExitKioskDialog(
            onDismiss  = { showExitDialog = false },
            onCheckPin = onCheckPin,
            onConfirmed = { showExitDialog = false; exitConfirmed = true }
        )
    }
}

// ── Header ────────────────────────────────────────────────────────────────────
@Composable
private fun KioskHeader(device: Device?, nowMs: Long, isLoading: Boolean, onRefresh: () -> Unit) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFmt = remember {
        if (isLandscape) SimpleDateFormat("EEE d MMM yyyy", Locale.ITALIAN)
        else SimpleDateFormat("EEEE d MMMM yyyy", Locale.ITALIAN)
    }
    val timeFontSz = if (isLandscape) 28.sp else 42.sp
    val nameFontSz = if (isLandscape) 16.sp else 22.sp

    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = device?.name ?: "—",
                color = Color.White, fontSize = nameFontSz, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (!device?.customer.isNullOrBlank()) {
                Text(device!!.customer!!, color = AccentCyan.copy(alpha = 0.7f), fontSize = 11.sp)
            }
            if (!isLandscape) {
                Text(
                    dateFmt.format(Date(nowMs)).replaceFirstChar { it.uppercase() },
                    color = Color.White.copy(alpha = 0.35f), fontSize = 11.sp
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (isLandscape) {
                Text(
                    dateFmt.format(Date(nowMs)).replaceFirstChar { it.uppercase() },
                    color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp
                )
            }
            Text(
                text = timeFmt.format(Date(nowMs)),
                color = Color.White, fontSize = timeFontSz, fontWeight = FontWeight.Thin, letterSpacing = 2.sp
            )
            IconButton(onClick = onRefresh, enabled = !isLoading, modifier = Modifier.size(30.dp)) {
                if (isLoading)
                    CircularProgressIndicator(color = AccentCyan, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else
                    Icon(Icons.Default.Refresh, null, tint = Color.White.copy(alpha = 0.25f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ── Body principale ───────────────────────────────────────────────────────────
@Composable
private fun KioskBody(device: Device) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val readings = device.readings
    val co2Reading = readings.find { it.name.contains("CO2", ignoreCase = true) }
    val otherReadings = readings.filter { !it.name.contains("CO2", ignoreCase = true) }.take(4)

    if (isLandscape) {
        // ── Landscape: CO₂ a sinistra | sensori a destra in griglia 2×2 ──────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // CO₂ compatto a sinistra
            Box(modifier = Modifier.weight(0.45f), contentAlignment = Alignment.Center) {
                if (co2Reading != null) Co2MainDonut(reading = co2Reading, compact = true)
            }
            // Grid 2×2 a destra
            if (otherReadings.isNotEmpty()) {
                Column(
                    modifier = Modifier.weight(0.55f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val rows = otherReadings.chunked(2)
                    rows.forEach { rowReadings ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowReadings.forEach { reading ->
                                Box(modifier = Modifier.weight(1f)) {
                                    SensorDonutCard(reading = reading, modifier = Modifier.fillMaxWidth(), compact = true)
                                }
                            }
                            if (rowReadings.size < 2) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    } else {
        // ── Portrait: CO₂ grande sopra | sensori in riga sotto ───────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (co2Reading != null) Co2MainDonut(reading = co2Reading, compact = false)
            if (otherReadings.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    otherReadings.forEach { reading ->
                        SensorDonutCard(reading = reading, modifier = Modifier.weight(1f).padding(horizontal = 4.dp))
                    }
                    repeat((4 - otherReadings.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

// ── Donut CO₂ grande ──────────────────────────────────────────────────────────
@Composable
private fun Co2MainDonut(reading: SensorReading, compact: Boolean = false) {
    val rawValue = reading.value.replace(",", ".").replace("[^0-9.]".toRegex(), "").toFloatOrNull()
    val ppm      = rawValue ?: 0f
    val color    = co2Color(ppm)
    val fill     = co2Fill(ppm)
    val label    = co2Label(ppm)

    val donutSize  = if (compact) 160.dp else 220.dp
    val glowSize   = if (compact) 190.dp else 260.dp
    val strokeSize = if (compact) 14.dp  else 18.dp
    val ppmFontSz  = if (compact) 48.sp  else 68.sp
    val labelFontSz = if (compact) 11.sp else 13.sp

    val animFill by animateFloatAsState(
        targetValue = fill,
        animationSpec = tween(1200, easing = EaseOutCubic),
        label = "co2fill"
    )
    val inf = rememberInfiniteTransition(label = "glow")
    val glowAlpha by inf.animateFloat(
        initialValue = 0.06f, targetValue = 0.18f,
        animationSpec = infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glowAlpha"
    )

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(glowSize)) {
            drawCircle(color = color.copy(alpha = glowAlpha), radius = size.minDimension / 2f)
        }
        DonutChart(fill = animFill, color = color, trackColor = Color.White.copy(alpha = 0.07f),
            size = donutSize, stroke = strokeSize)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (rawValue != null) "${"%.0f".format(ppm)}" else "—",
                color = color, fontSize = ppmFontSz, fontWeight = FontWeight.ExtraBold, lineHeight = ppmFontSz
            )
            Text("ppm CO₂", color = color.copy(alpha = 0.65f), fontSize = labelFontSz, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = color.copy(alpha = 0.18f),
                border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "Aria $label", color = color, fontSize = labelFontSz, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ── Card sensore con mini donut ───────────────────────────────────────────────
@Composable
private fun SensorDonutCard(reading: SensorReading, modifier: Modifier = Modifier, compact: Boolean = false) {
    val rawValue = reading.value.replace(",", ".").replace("[^0-9.]".toRegex(), "").toFloatOrNull()
    val value    = rawValue ?: 0f
    val color    = sensorColor(reading.name, value)
    val fill     = sensorFill(reading.name, value)

    val animFill by animateFloatAsState(
        targetValue = fill,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "sensorfill"
    )

    val donutSize  = if (compact) 72.dp else 88.dp
    val strokeSize = if (compact) 8.dp  else 10.dp
    val valueFontSz = if (compact) 14.sp else 16.sp
    val vPad = if (compact) 12.dp else 18.dp

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = CardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = vPad, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = reading.name,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(contentAlignment = Alignment.Center) {
                DonutChart(fill = animFill, color = color,
                    trackColor = Color.White.copy(alpha = 0.07f), size = donutSize, stroke = strokeSize)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (rawValue != null) reading.value.trim() else "—",
                        color = color, fontSize = valueFontSz, fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    if (!reading.unit.isNullOrBlank()) {
                        Text(reading.unit, color = color.copy(alpha = 0.5f), fontSize = 9.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// ── Componente Donut generico ─────────────────────────────────────────────────
@Composable
private fun DonutChart(
    fill: Float,
    color: Color,
    trackColor: Color,
    size: Dp,
    stroke: Dp
) {
    Canvas(modifier = Modifier.size(size)) {
        val strokePx   = stroke.toPx()
        val inset      = strokePx / 2f
        val arcSize    = Size(this.size.width - strokePx, this.size.height - strokePx)
        val topLeft    = Offset(inset, inset)
        val startAngle = -90f         // parte dall'alto
        val sweepFull  = 360f
        val sweepFill  = sweepFull * fill

        // Traccia di sfondo
        drawArc(
            color      = trackColor,
            startAngle = startAngle,
            sweepAngle = sweepFull,
            useCenter  = false,
            topLeft    = topLeft,
            size       = arcSize,
            style      = Stroke(width = strokePx, cap = StrokeCap.Round)
        )

        // Arco valore
        if (sweepFill > 0f) {
            drawArc(
                brush      = Brush.sweepGradient(
                    0f    to color.copy(alpha = 0.6f),
                    0.5f  to color,
                    1f    to color.copy(alpha = 0.6f)
                ),
                startAngle = startAngle,
                sweepAngle = sweepFill,
                useCenter  = false,
                topLeft    = topLeft,
                size       = arcSize,
                style      = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
    }
}

// ── Footer ────────────────────────────────────────────────────────────────────
@Composable
private fun KioskFooter(lastUpdated: Long) {
    val fmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(
            text = if (lastUpdated > 0L) "Ultimo aggiornamento: ${fmt.format(Date(lastUpdated))}"
                   else "In aggiornamento...",
            color = Color.White.copy(alpha = 0.25f),
            fontSize = 11.sp
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("TAUANITO", color = Color.White.copy(alpha = 0.12f), fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp)
            Box(Modifier.size(6.dp).background(AccentCyan.copy(alpha = 0.4f), CircleShape))
        }
    }
}

// ── Dialog PIN uscita ─────────────────────────────────────────────────────────
@Composable
fun ExitKioskDialog(
    onDismiss: () -> Unit,
    onCheckPin: (String) -> Boolean,
    onConfirmed: () -> Unit
) {
    var pin   by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(dismissOnClickOutside = false)) {
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("Esci dal Chiosco", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Inserisci il PIN di sicurezza", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)

                // Puntini PIN
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    repeat(4) { i ->
                        Box(
                            Modifier.size(14.dp).background(
                                if (i < pin.length) AccentCyan else Color.White.copy(alpha = 0.15f),
                                CircleShape
                            )
                        )
                    }
                }

                AnimatedError(visible = error)

                NumPad(
                    onDigit  = { d -> if (pin.length < 4) { pin += d; error = false } },
                    onDelete = { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                        Text("Annulla")
                    }
                    Button(
                        onClick = {
                            if (onCheckPin(pin)) onConfirmed()
                            else { error = true; pin = "" }
                        },
                        enabled = pin.length == 4,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                    ) { Text("Sblocca", color = Color.Black, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// ── Dialog impostazione PIN (prima volta) ─────────────────────────────────────
@Composable
fun SetKioskPinDialog(
    onDismiss: () -> Unit,
    onPinSet: (String) -> Unit
) {
    var pin     by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var step    by remember { mutableStateOf(0) }
    var error   by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(dismissOnClickOutside = false)) {
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("Modalità Chiosco", color = AccentCyan, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Text(
                    if (step == 0) "Imposta un PIN a 4 cifre per uscire dalla modalità chiosco"
                    else "Conferma il PIN",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                val current = if (step == 0) pin else confirm
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    repeat(4) { i ->
                        Box(
                            Modifier.size(14.dp).background(
                                if (i < current.length) AccentCyan else Color.White.copy(alpha = 0.15f),
                                CircleShape
                            )
                        )
                    }
                }

                AnimatedError(visible = error, message = "I PIN non corrispondono, riprova")

                NumPad(
                    onDigit = { d ->
                        error = false
                        if (step == 0) { if (pin.length < 4)     pin += d }
                        else           { if (confirm.length < 4) confirm += d }
                    },
                    onDelete = {
                        if (step == 0) { if (pin.isNotEmpty())     pin = pin.dropLast(1) }
                        else           { if (confirm.isNotEmpty()) confirm = confirm.dropLast(1) }
                    }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                        Text("Annulla")
                    }
                    Button(
                        onClick = {
                            if (step == 0 && pin.length == 4) { step = 1 }
                            else if (step == 1 && confirm.length == 4) {
                                if (pin == confirm) onPinSet(pin)
                                else { error = true; confirm = "" }
                            }
                        },
                        enabled = if (step == 0) pin.length == 4 else confirm.length == 4,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                    ) {
                        Text(if (step == 0) "Avanti" else "Attiva",
                            color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Errore animato ────────────────────────────────────────────────────────────
@Composable
private fun AnimatedError(visible: Boolean, message: String = "PIN errato") {
    androidx.compose.animation.AnimatedVisibility(visible = visible) {
        Text(message, color = Color(0xFFEF5350), fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

// ── Tastierino numerico ───────────────────────────────────────────────────────
@Composable
private fun NumPad(onDigit: (String) -> Unit, onDelete: () -> Unit) {
    val rows = listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("","0","⌫"))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    if (key.isEmpty()) Spacer(Modifier.size(68.dp))
                    else FilledTonalButton(
                        onClick = { if (key == "⌫") onDelete() else onDigit(key) },
                        modifier = Modifier.size(68.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = if (key == "⌫") 0.07f else 0.11f)
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(key, color = Color.White, fontSize = if (key == "⌫") 20.sp else 22.sp,
                            fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
