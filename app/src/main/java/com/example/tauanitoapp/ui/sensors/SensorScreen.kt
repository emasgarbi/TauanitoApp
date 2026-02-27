package com.example.tauanitoapp.ui.sensors

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tauanitoapp.data.model.BatteryLevel
import com.example.tauanitoapp.data.model.Device
import com.example.tauanitoapp.data.model.SensorReading

// ── Colori ──────────────────────────────────────────────────────────────────
private val ColorTitle       = Color(0xFF1565C0) // blu titolo
private val ColorDeviceName  = Color(0xFF198754) // verde nome device
private val ColorTemperatura = Color(0xFFFFC107) // giallo/ambra
private val ColorUmidita     = Color(0xFF1565C0) // blu
private val ColorPressione   = Color(0xFF7B1FA2) // viola
private val ColorCO2         = Color(0xFFB0BEC5) // grigio chiaro (leggibile su sfondo scuro)
private val ColorIaq         = Color(0xFFECEFF1) // bianco grigio (leggibile su sfondo scuro)
private val ColorBattFull    = Color(0xFF4CAF50)
private val ColorBattHalf    = Color(0xFFFFB300)
private val ColorBattLow     = Color(0xFFFF5722)
private val ColorBattEmpty   = Color(0xFFF44336)

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
    "OPENGROUPITALIA", "Sanixair", "SAVIO", "Signify", "Strobilo",
    "Taua", "TEST-ING", "Weishaupt"
)

// ── Route ────────────────────────────────────────────────────────────────────
@Composable
fun SensorRoute(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: SensorViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()

    SensorScreen(
        state            = state,
        onRetry          = viewModel::refresh,
        onLogout         = onLogout,
        onSearchChange   = viewModel::onSearchChange,
        onCustomerSelect = viewModel::onCustomerSelect,
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        // ── Barra superiore ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text       = "Tauanito",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color      = ColorTitle
            )
            TextButton(onClick = onLogout) {
                Text("Logout", color = MaterialTheme.colorScheme.error)
            }
        }

        // ── Riga filtri: cerca device (sx) + filtra per cliente (dx) ─────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // Campo di testo ricerca device (metà larghezza)
            OutlinedTextField(
                value         = state.searchQuery,
                onValueChange = onSearchChange,
                modifier      = Modifier.weight(1f),
                label         = { Text("Cerca device") },
                leadingIcon   = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine    = true,
                shape         = MaterialTheme.shapes.medium
            )

            // Menù a tendina clienti (metà larghezza)
            var dropdownExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded          = dropdownExpanded,
                onExpandedChange  = { dropdownExpanded = it },
                modifier          = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value         = state.selectedCustomer ?: "Tutti",
                    onValueChange = {},
                    readOnly      = true,
                    label         = { Text("Cliente") },
                    trailingIcon  = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                    },
                    modifier      = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    singleLine    = true,
                    shape         = MaterialTheme.shapes.medium
                )
                ExposedDropdownMenu(
                    expanded          = dropdownExpanded,
                    onDismissRequest  = { dropdownExpanded = false }
                ) {
                    // Voce "Tutti"
                    DropdownMenuItem(
                        text    = { Text("Tutti") },
                        onClick = { onCustomerSelect(null); dropdownExpanded = false }
                    )
                    CUSTOMERS.forEach { customer ->
                        DropdownMenuItem(
                            text    = { Text(customer) },
                            onClick = { onCustomerSelect(customer); dropdownExpanded = false }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Contenuto ────────────────────────────────────────────────────────
        when {
            state.isLoading && state.devices.isEmpty() -> {
                Column(
                    modifier              = Modifier.fillMaxSize(),
                    verticalArrangement   = Arrangement.Center,
                    horizontalAlignment   = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Text(
                        text     = "Caricamento dispositivi...",
                        modifier = Modifier.padding(top = 12.dp),
                        style    = MaterialTheme.typography.bodyMedium
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
                        strokeWidth = 2.dp
                    )
                }

                val list = state.filteredDevices
                if (list.isEmpty()) {
                    Text(
                        text     = "Nessun device trovato.",
                        modifier = Modifier.padding(top = 16.dp),
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier            = Modifier.fillMaxSize(),
                        contentPadding      = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(list) { device ->
                            DeviceCard(device = device)
                        }
                    }
                }
            }
        }
    }
}

// ── Card singolo device ───────────────────────────────────────────────────────
@Composable
private fun DeviceCard(device: Device) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape     = MaterialTheme.shapes.medium,
        colors    = CardDefaults.cardColors(containerColor = Color(0xFF025669))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // Nome device + indicatore batteria
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = device.name,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = ColorDeviceName,
                    modifier   = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BatteryIndicator(level = device.batteryLevel, voltage = device.voltage)
            }

            // Timestamp
            if (!device.timestamp.isNullOrBlank()) {
                Text(
                    text     = device.timestamp,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 3.dp)
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Letture sensori
            device.readings.forEach { reading ->
                SensorRow(reading = reading)
            }
        }
    }
}

// ── Riga sensore con colore ───────────────────────────────────────────────────
@Composable
private fun SensorRow(reading: SensorReading) {
    val labelColor = when {
        reading.name.contains("Temp",  ignoreCase = true) -> ColorTemperatura
        reading.name.contains("Umid",  ignoreCase = true) -> ColorUmidita
        reading.name.contains("Press", ignoreCase = true) -> ColorPressione
        reading.name.contains("CO2",   ignoreCase = true) -> ColorCO2
        reading.name.contains("iaq",   ignoreCase = true) -> ColorIaq
        else -> Color.White
    }
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text       = reading.name,
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color      = labelColor
        )
        Text(
            text       = if (reading.unit != null) "${reading.value} ${reading.unit}"
                         else reading.value,
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color      = Color.White
        )
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
