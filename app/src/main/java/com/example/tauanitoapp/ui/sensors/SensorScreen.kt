package com.example.tauanitoapp.ui.sensors

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tauanitoapp.R
import com.example.tauanitoapp.data.model.BatteryLevel
import com.example.tauanitoapp.data.model.Device
import com.example.tauanitoapp.data.model.SensorReading

// ── Colori ──────────────────────────────────────────────────────────────────
private val ColorTitle       = Color(0xFF1565C0) // blu titolo
private val ColorDeviceName  = Color(0xFF198754) // verde nome device
private val ColorTemperatura = Color(0xFFFFC107) // giallo/ambra
private val ColorUmidita     = Color(0xFF1565C0) // blu
private val ColorPressione   = Color(0xFF7B1FA2) // viola
private val ColorCO2         = Color(0xFFB0BEC5) // grigio chiaro
private val ColorIaq         = Color(0xFFECEFF1) // bianco grigio
private val ColorBattFull    = Color(0xFF4CAF50)
private val ColorBattHalf    = Color(0xFFFFB300)
private val ColorBattLow     = Color(0xFFFF5722)
private val ColorBattEmpty   = Color(0xFFF44336)

// ── Testo con ombra leggibile dinamica ───────────────────────────────────────
@Composable
private fun OutlinedText(
    text: String,
    color: Color,
    style: TextStyle,
    fontWeight: FontWeight? = null,
    isDarkMode: Boolean = true,
    modifier: Modifier = Modifier
) {
    Text(
        text       = text,
        color      = color,
        style      = style.copy(
            shadow = if (isDarkMode) Shadow(
                color      = Color.Black,
                offset     = Offset(0f, 2f),
                blurRadius = 6f
            ) else Shadow(
                // Ombra "fantasma" quasi invisibile per distaccare il testo in Light Mode
                color      = Color.Black.copy(alpha = 0.15f),
                offset     = Offset(0f, 1f),
                blurRadius = 3f
            )
        ),
        fontWeight = fontWeight,
        modifier   = modifier
    )
}

// ── Lista clienti ────────────────────────────────────────────────────────────
private val CUSTOMERS = listOf(
    "ADA RICERCA", "Aeroporto Marconi", "Agritec", "Agrofertil", "Agrofertil Tecnico",
    "Aldo Romani", "ALISEA", "AMMAGAMMA", "Aria Suolo Acqua ASA Srl", "Arienti Condominio",
    "Ariestech", "ATTA", "Aura System", "Bacchi", "Bellini Tiziana S.r.l. a socio unico",
    "BIOGEOTEK", "Bsg tech", "Caelum", "Comune di Bologna", "CONFINDUSTRIA",
    "CTE MOLISE", "DEL CORSO", "Designtech", "DT4 Benefit Srlb", "Emanuele Gatti",
    "Evoluzione servizi", "Fanti Luca", "FILAIR", "Gaetano Ingenito", "Gaetano Settimo",
    "Gianfranco Silvestri", "Giorgio Prodi", "Heltyair", "Involve Group", "IPLUS",
    "IQC", "ITA", "Jonixair", "Logimatic", "Mario Sboarina",
    "Montanari Galletti", "Nuovamacut", "NUOVASORMU", "OBS", "OLTRE SRL",
    "OPENGROUPITALIA", "Sala Borsa", "Sanixair", "SAVIO", "Signify", "Strobilo",
    "Taua", "TEST-ING", "Weishaupt"
)

// ── Route ────────────────────────────────────────────────────────────────────
@Composable
fun SensorRoute(
    viewModel: SensorViewModel = viewModel(),
    onLogout: () -> Unit,
    onDeviceClick: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    // startAutoRefresh chiama refreshData() subito all'avvio, poi ogni N minuti.
    // Non serve un refresh() separato: evita due richieste HTTP concorrenti al primo caricamento.
    LaunchedEffect(Unit) {
        viewModel.startAutoRefresh()
    }

    SensorScreen(
        state            = state,
        onRetry          = viewModel::refresh,
        onLogout         = onLogout,
        onSearchChange   = viewModel::onSearchChange,
        onCustomerSelect = viewModel::onCustomerSelect,
        onDeviceClick    = onDeviceClick,
        onOpenDrawer     = onOpenDrawer,
        modifier         = modifier
    )
}

// ── Schermata principale ──────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorScreen(
    state: SensorUiState,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
    onSearchChange: (String) -> Unit,
    onCustomerSelect: (String?) -> Unit,
    onDeviceClick: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val actualDarkMode = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val contentColor = if (actualDarkMode) Color.White else Color.Black
    val filterCardBg = if (actualDarkMode) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)

    Scaffold(
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(modifier = modifier.fillMaxSize().padding(paddingValues)) {
            Image(
                painter            = painterResource(R.drawable.immaginesfondo),
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(if (actualDarkMode) Color.Black.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.75f))
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = "Apri Menù", tint = contentColor)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text       = "Tauanito",
                            style      = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color      = contentColor
                        )
                        Image(
                            painter            = painterResource(R.drawable.tauanito_logo),
                            contentDescription = "Logo Tauanito",
                            modifier           = Modifier.size(42.dp),
                            contentScale       = ContentScale.Fit
                        )
                    }

                    TextButton(
                        onClick = onLogout,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text("Logout", color = contentColor.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (actualDarkMode) 0.dp else 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = filterCardBg
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = state.searchQuery,
                            onValueChange = onSearchChange,
                            modifier = Modifier.weight(1.1f),
                            placeholder = { Text("Cerca...", fontSize = 12.sp, color = contentColor.copy(alpha = 0.5f)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp)) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 12.sp, color = contentColor),
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = contentColor,
                                unfocusedTextColor = contentColor
                            )
                        )

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(contentColor.copy(alpha = 0.2f))
                        )

                        var dropdownExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = dropdownExpanded,
                            onExpandedChange = { dropdownExpanded = it },
                            modifier = Modifier.weight(0.9f)
                        ) {
                            TextField(
                                value = state.selectedCustomer ?: "Clienti",
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("Clienti", fontSize = 12.sp, color = contentColor.copy(alpha = 0.5f)) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp)) },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                                },
                                textStyle = TextStyle(fontSize = 12.sp, color = contentColor),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = contentColor,
                                    unfocusedTextColor = contentColor
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Tutti i clienti") },
                                    onClick = { onCustomerSelect(null); dropdownExpanded = false }
                                )
                                CUSTOMERS.forEach { customer ->
                                    DropdownMenuItem(
                                        text = { Text(customer) },
                                        onClick = { onCustomerSelect(customer); dropdownExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                when {
                    state.isLoading && state.devices.isEmpty() -> {
                        Column(
                            modifier              = Modifier.fillMaxSize(),
                            verticalArrangement   = Arrangement.Center,
                            horizontalAlignment   = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = contentColor)
                            Text(
                                text     = "Caricamento dispositivi...",
                                modifier = Modifier.padding(top = 12.dp),
                                style    = MaterialTheme.typography.bodyMedium,
                                color    = contentColor
                            )
                        }
                    }

                    state.errorMessage != null && state.devices.isEmpty() -> {
                        Column(
                            modifier              = Modifier.fillMaxSize(),
                            verticalArrangement   = Arrangement.Center,
                            horizontalAlignment   = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text  = state.errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(
                                onClick  = onRetry,
                                modifier = Modifier.padding(top = 12.dp)
                            ) { Text("Riprova") }
                        }
                    }

                    else -> {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier    = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(bottom = 8.dp)
                                    .size(24.dp),
                                strokeWidth = 2.dp,
                                color = contentColor
                            )
                        }

                        val list = state.filteredDevices
                        if (list.isEmpty()) {
                            Text(
                                text     = "Nessun device trovato.",
                                modifier = Modifier.padding(top = 16.dp),
                                style    = MaterialTheme.typography.bodyMedium,
                                color    = contentColor.copy(alpha = 0.7f)
                            )
                        } else {
                            LazyColumn(
                                modifier            = Modifier.fillMaxSize(),
                                contentPadding      = PaddingValues(bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(list) { device ->
                                    DeviceCard(
                                        device = device, 
                                        onClick = { onDeviceClick(device.id) },
                                        isDarkMode = actualDarkMode
                                    )
                                }
                            }                        }
                    }
                }
            }
        }
    }
}

// ── Card singolo device ───────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceCard(device: Device, onClick: () -> Unit, isDarkMode: Boolean) {
    val cardOverlay = if (isDarkMode) Color.Black.copy(alpha = 0.50f) else Color.Transparent
    val textPrimary = if (isDarkMode) Color.White else Color.Black
    val deviceNameColor = if (isDarkMode) ColorDeviceName else Color(0xFF1B5E20) // Verde più scuro in Light Mode
    
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkMode) 3.dp else 6.dp),
        shape     = MaterialTheme.shapes.medium,
        colors    = CardDefaults.cardColors(containerColor = Color.Transparent),
        onClick   = onClick
    ) {
        Box {
            // Immagine di sfondo
            Image(
                painter            = painterResource(R.drawable.sfondotauanito),
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.matchParentSize()
            )
            // Overlay dinamico
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(cardOverlay)
            )
            // Contenuto
            Column(modifier = Modifier.padding(14.dp)) {

                // Nome device + indicatore batteria
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    OutlinedText(
                        text       = device.name,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = deviceNameColor,
                        isDarkMode = isDarkMode,
                        modifier   = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BatteryIndicator(level = device.batteryLevel, voltage = device.voltage)
                }

                // Timestamp
                if (!device.timestamp.isNullOrBlank()) {
                    OutlinedText(
                        text       = device.timestamp,
                        style      = MaterialTheme.typography.bodySmall,
                        color      = textPrimary.copy(alpha = 0.7f),
                        isDarkMode = isDarkMode,
                        modifier   = Modifier.padding(top = 3.dp)
                    )
                }

                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = textPrimary.copy(alpha = 0.1f)
                )

                // Letture sensori
                device.readings.forEach { reading ->
                    SensorRow(reading = reading, isDarkMode = isDarkMode)
                }
            }
        }
    }
}

// ── Giudizio qualità aria ─────────────────────────────────────────────────────
private fun sensorComment(name: String, rawValue: String): Pair<String, Color>? {
    val cleanedValue = rawValue.replace(".", "").replace(",", ".")
    val n = cleanedValue.trim().toDoubleOrNull() ?: return null
    return when {
        name.contains("CO2", ignoreCase = true) -> when {
            n <= 600  -> "Ottimale"   to Color(0xFF4CAF50)
            n <= 1000 -> "Buono"      to Color(0xFF8BC34A)
            n <= 1500 -> "Attenzione" to Color(0xFFFFC107)
            n <= 2000 -> "Cattivo"    to Color(0xFFFF9800)
            else      -> "Pericoloso" to Color(0xFFF44336)
        }
        name.contains("iaq", ignoreCase = true) -> when {
            n <= 50  -> "Eccellente"      to Color(0xFF4CAF50)
            n <= 100 -> "Buono"           to Color(0xFF8BC34A)
            n <= 150 -> "Liev. inquinato" to Color(0xFFFFC107)
            n <= 200 -> "Mod. inquinato"  to Color(0xFFFF9800)
            n <= 300 -> "Fort. inquinato" to Color(0xFFFF5722)
            else     -> "Pericoloso"      to Color(0xFFF44336)
        }
        name.contains("Press", ignoreCase = true) -> when {
            n > 1020 -> "Alta"    to Color(0xFF29B6F6)
            n >= 1000 -> "Normale" to Color(0xFF4CAF50)
            else      -> "Bassa"   to Color(0xFFFFB300)
        }
        else -> null
    }
}

// ── Riga sensore con stile Glassmorphism ─────────────────────────────────────
@Composable
private fun SensorRow(reading: SensorReading, isDarkMode: Boolean) {
    val textPrimary = if (isDarkMode) Color.White else Color.Black
    val labelColor = when {
        reading.name.contains("Temp",  ignoreCase = true) -> ColorTemperatura
        reading.name.contains("Umid",  ignoreCase = true) -> ColorUmidita
        reading.name.contains("Press", ignoreCase = true) -> ColorPressione
        reading.name.contains("CO2",   ignoreCase = true) -> if (isDarkMode) ColorCO2 else Color(0xFF455A64)
        reading.name.contains("iaq",   ignoreCase = true) -> if (isDarkMode) ColorIaq else Color(0xFF37474F)
        else -> textPrimary
    }
    val comment = sensorComment(reading.name, reading.value)

    Surface(
        color = if (isDarkMode) Color.White.copy(alpha = 0.03f) else Color.White.copy(alpha = 0.65f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier              = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text       = reading.name,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color      = labelColor
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text       = if (reading.unit != null) "${reading.value} ${reading.unit}"
                                 else reading.value,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color      = textPrimary
                )
                if (comment != null) {
                    Text(
                        text       = comment.first,
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color      = comment.second
                    )
                }
            }
        }
    }
}

// ── Indicatore batteria ────────────────────────────────────────────────────────
@Composable
private fun BatteryIndicator(
    level: BatteryLevel,
    voltage: String?,
    modifier: Modifier = Modifier
) {
    val color = when (level) {
        BatteryLevel.FULL    -> ColorBattFull
        BatteryLevel.HALF    -> ColorBattHalf
        BatteryLevel.LOW     -> ColorBattLow
        BatteryLevel.EMPTY   -> ColorBattEmpty
        BatteryLevel.UNKNOWN -> Color.Gray
    }
    val fillFraction = when (level) {
        BatteryLevel.FULL    -> 0.95f
        BatteryLevel.HALF    -> 0.55f
        BatteryLevel.LOW     -> 0.20f
        BatteryLevel.EMPTY   -> 0.05f
        BatteryLevel.UNKNOWN -> 0.00f
    }

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier              = modifier
    ) {
        Canvas(modifier = Modifier.size(width = 26.dp, height = 13.dp)) {
            val tipW   = 3.dp.toPx()
            val bodyW  = size.width - tipW
            val stroke = 1.5.dp.toPx()
            val pad    = stroke + 1.dp.toPx()

            drawRoundRect(
                color        = color,
                topLeft      = Offset(0f, 0f),
                size         = Size(bodyW, size.height),
                cornerRadius = CornerRadius(3.dp.toPx()),
                style        = Stroke(width = stroke)
            )
            if (fillFraction > 0f) {
                drawRoundRect(
                    color        = color,
                    topLeft      = Offset(pad, pad),
                    size         = Size((bodyW - pad * 2) * fillFraction, size.height - pad * 2),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
            }
            drawRoundRect(
                color        = color,
                topLeft      = Offset(bodyW, size.height * 0.30f),
                size         = Size(tipW - stroke, size.height * 0.40f),
                cornerRadius = CornerRadius(1.dp.toPx())
            )
        }
        if (!voltage.isNullOrBlank()) {
            Text(
                text       = voltage,
                style      = MaterialTheme.typography.labelSmall,
                color      = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
