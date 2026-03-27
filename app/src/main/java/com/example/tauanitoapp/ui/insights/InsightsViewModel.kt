package com.example.tauanitoapp.ui.insights

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tauanitoapp.data.model.DeviceHistory
import com.example.tauanitoapp.data.model.HistoryRecord
import com.example.tauanitoapp.data.model.SensorReading
import com.example.tauanitoapp.data.repository.SensorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class InsightData(
    val sensorName: String,
    val values: List<Float>,
    val timestamps: List<Long>, // Unix timestamps for X-axis
    val unit: String?,
    val trend: String?,      // e.g., "In crescita", "In calo", "Stabile"
    val prediction: String?, // e.g., "Previsto aumento del 5%"
    val advice: String?,     // Actionable advice based on analysis
    val isAnomaly: Boolean = false // Flag if recent data looks like an anomaly
)

data class ComparisonDevice(
    val deviceId: String,
    val deviceName: String,
    val readings: List<SensorReading>,
    val healthScore: Int
)

data class InsightsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val history: DeviceHistory? = null,
    val insights: List<InsightData> = emptyList(),
    val selectedSensors: Set<String> = emptySet(),
    val comparisonDevices: List<ComparisonDevice> = emptyList(),
    val selectedComparisonDevices: Set<String> = emptySet(),
    val summary: String? = null,
    val healthScore: Int = 100
)

class InsightsViewModel(application: Application, private val deviceId: String) : AndroidViewModel(application) {

    private val repository = SensorRepository(application)
    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState

    init {
        loadInsights()
    }

    fun loadInsights() {
        if (deviceId.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "ID Dispositivo mancante")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                // 1. Carica lo storico del device corrente
                val history = repository.getDeviceHistory(deviceId)
                val processedInsights = processHistoryData(history.records)
                
                // 2. Carica l'elenco di tutti i device per il confronto
                val allDevices = try { repository.getDevices() } catch (e: Exception) { emptyList() }
                val comparisonList = allDevices.map { dev ->
                    ComparisonDevice(
                        deviceId = dev.id,
                        deviceName = dev.name,
                        readings = dev.readings,
                        healthScore = calculateInstantHealthScore(dev.readings)
                    )
                }

                val summary = generateAdvancedSummary(processedInsights, comparisonList)
                val healthScore = calculateHealthScore(processedInsights)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    history = history,
                    insights = processedInsights,
                    selectedSensors = if (_uiState.value.selectedSensors.isEmpty()) 
                        processedInsights.map { it.sensorName }.take(3).toSet() 
                        else _uiState.value.selectedSensors,
                    comparisonDevices = comparisonList,
                    selectedComparisonDevices = setOf(deviceId),
                    summary = summary,
                    healthScore = healthScore
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Errore caricamento insights"
                )
            }
        }
    }

    fun toggleSensorSelection(sensorName: String) {
        val current = _uiState.value.selectedSensors.toMutableSet()
        if (current.contains(sensorName)) {
            current.remove(sensorName)
        } else {
            current.add(sensorName)
        }
        _uiState.value = _uiState.value.copy(selectedSensors = current)
    }

    fun toggleDeviceComparison(id: String) {
        val current = _uiState.value.selectedComparisonDevices.toMutableSet()
        if (current.contains(id)) {
            if (current.size > 1) current.remove(id) // Mantieni almeno uno
        } else {
            current.add(id)
        }
        _uiState.value = _uiState.value.copy(selectedComparisonDevices = current)
    }

    private fun processInstantReadings(readings: List<SensorReading>): List<InsightData> {
        // Analisi di base per device non correnti (solo istantanea)
        return readings.map { r ->
            InsightData(r.name, listOf(r.value.replace(",",".").toFloatOrNull() ?: 0f), emptyList(), r.unit, "Istantaneo", null, null)
        }
    }

    private fun calculateInstantHealthScore(readings: List<SensorReading>): Int {
        var score = 100
        readings.forEach { r ->
            val valF = r.value.replace(",",".").toFloatOrNull() ?: return@forEach
            if (r.name.contains("Temp", true) && valF > 45) score -= 20
            if (r.name.contains("Volt", true) && valF < 3.4) score -= 30
            if (r.name.contains("Batt", true) && valF < 20) score -= 25
        }
        return score.coerceIn(0, 100)
    }

    private fun generateAdvancedSummary(insights: List<InsightData>, others: List<ComparisonDevice>): String {
        val currentDev = others.find { it.deviceId == deviceId }
        val avgHealth = if (others.isNotEmpty()) others.map { it.healthScore }.average() else 100.0
        
        val healthTrend = if ((currentDev?.healthScore ?: 100) > avgHealth) {
            "sopra la media della tua flotta"
        } else {
            "sotto la media della gestione"
        }

        val anomalies = insights.count { it.isAnomaly }
        
        return if (anomalies > 0) {
            "Rilevate $anomalies anomalie. Questo Tauanito è $healthTrend. Si consiglia un controllo incrociato con i dispositivi più stabili."
        } else {
            "Andamento regolare. Il dispositivo è $healthTrend (${currentDev?.healthScore ?: 100} vs ${avgHealth.toInt()} avg). Nessuna criticità rilevata dai sensori selezionati."
        }
    }

    private fun filterByFixedHour(dataPoints: List<Pair<Long, Float>>): List<Pair<Long, Float>> {
        if (dataPoints.isEmpty()) return emptyList()

        val calendar = Calendar.getInstance()

        // 1. Raggruppiamo per giorno in modo robusto usando Calendar (azzera ore/min/sec)
        val dayGroups = dataPoints.groupBy { (timestamp, _) ->
            calendar.timeInMillis = timestamp
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }

        Log.d("InsightsVM", "Giorni unici trovati (Calendar): ${dayGroups.size}")

        // 2. Identifichiamo l'ora del giorno "più densa" tra tutti i dati per scegliere una fascia oraria coerente
        val hourGroups = dataPoints.groupBy { (timestamp, _) ->
            calendar.timeInMillis = timestamp
            calendar.get(Calendar.HOUR_OF_DAY)
        }
        val targetHour = hourGroups.maxByOrNull { it.value.size }?.key ?: 12 // Default a mezzogiorno se non chiaro

        // 3. Per ogni giorno, prendiamo SOLO LA PRIMA rilevazione più vicina all'ora target
        val filtered = dayGroups.mapNotNull { (dayStart, readings) ->
            // Cerchiamo la lettura che più si avvicina a (dayStart + targetHour)
            val targetTimestamp = dayStart + (targetHour * 3600000L)
            
            val bestMatch = readings.minByOrNull { (timestamp, _) ->
                Math.abs(timestamp - targetTimestamp)
            }
            
            if (bestMatch != null) {
                val timeStr = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(bestMatch.first))
                Log.d("InsightsVM", "  Giorno ${SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(dayStart))} -> scelto $timeStr")
            }
            bestMatch
        }.sortedBy { it.first }

        return filtered
    }

    private fun processHistoryData(records: List<HistoryRecord>): List<InsightData> {
        val sensorMap = mutableMapOf<String, MutableList<Pair<Long, Float>>>()
        val unitMap = mutableMapOf<String, String?>()

        // Formato data sito: "27.02.2026 - 12:39"
        val dateFormats = listOf(
            SimpleDateFormat("dd.MM.yyyy - HH:mm", Locale.getDefault()),
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()),
            SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        )

        // 1. Convertiamo i record in una lista con timestamp numerico e ordiniamo cronologicamente
        val sortedRecords = records.mapNotNull { record ->
            var timestamp = 0L
            for (format in dateFormats) {
                try {
                    timestamp = format.parse(record.timestamp)?.time ?: 0L
                    if (timestamp != 0L) break
                } catch (e: Exception) {}
            }
            if (timestamp == 0L) null else record to timestamp
        }.sortedBy { it.second }

        // 2. Processiamo i dati ordinati
        sortedRecords.forEach { (record, timestamp) ->
            record.readings.forEach { reading ->
                // Normalizziamo il nome del sensore
                val normalizedName = reading.name.split("(")[0].split("%")[0].split("[")[0].trim()

                // Estrazione numerica più aggressiva: prendiamo il primo numero valido trovato
                // Utile se il valore è "100% (4.2V)" -> prende 100
                val cleanValue = reading.value
                    .replace(",", ".")
                    .replace("[^0-9.-]".toRegex(), " ")
                    .trim()
                    .split(" ")
                    .firstOrNull { it.isNotEmpty() }

                val floatValue = cleanValue?.toFloatOrNull()
                if (floatValue != null) {
                    sensorMap.getOrPut(normalizedName) { mutableListOf() }.add(timestamp to floatValue)
                    unitMap[normalizedName] = reading.unit
                }
            }
        }

        // 3. Filtriamo per mantenere solo rilevazioni a orario fisso ma di giorni diversi
        val filteredSensorMap = sensorMap.mapValues { (sensorName, dataPoints) ->
            Log.d("InsightsVM", "Sensore '$sensorName' - Prima del filtro: ${dataPoints.size} punti")
            val filtered = filterByFixedHour(dataPoints)
            Log.d("InsightsVM", "Sensore '$sensorName' - Dopo il filtro: ${filtered.size} punti")
            filtered
        }

        return filteredSensorMap.map { (name, dataPoints) ->
            val sortedData = dataPoints.sortedBy { it.first }

            // Creiamo coppie (minuti_da_inizio, valore)
            val timestamps = sortedData.map { it.first }
            val values = sortedData.map { it.second }

            Log.d("InsightsVM", "InsightData per '$name': ${values.size} valori finali")
            
            val trend = calculateTrend(values)
            val isAnomaly = detectAnomaly(values)
            val prediction = calculatePrediction(values, trend)
            val advice = generateAdvice(name, values, trend, isAnomaly)

            InsightData(
                sensorName = name,
                values = values,
                timestamps = timestamps, // Manteniamo i timestamp Unix originali
                unit = unitMap[name],
                trend = trend,
                prediction = prediction,
                advice = advice,
                isAnomaly = isAnomaly
            )
        }
    }

    private fun detectAnomaly(values: List<Float>): Boolean {
        if (values.size < 5) return false
        val last = values.last()
        val previous = values.dropLast(1)
        val avg = previous.average().toFloat()
        val stdDev = Math.sqrt(previous.map { (it - avg).toDouble() * (it - avg) }.average()).toFloat()
        
        // Se l'ultimo valore è a più di 3 deviazioni standard dalla media, è un'anomalia
        return Math.abs(last - avg) > (3 * stdDev) && stdDev > 0.1
    }

    private fun calculateTrend(values: List<Float>): String {
        if (values.size < 2) return "Stabile"
        
        val mid = values.size / 2
        val firstHalfAvg = values.take(mid).average()
        val secondHalfAvg = values.drop(mid).average()
        
        val diffPercent = if (firstHalfAvg != 0.0) (secondHalfAvg - firstHalfAvg) / Math.abs(firstHalfAvg) else 0.0
        
        return when {
            diffPercent > 0.05 -> "In forte crescita 📈"
            diffPercent > 0.01 -> "In lieve crescita ↗️"
            diffPercent < -0.05 -> "In forte calo 📉"
            diffPercent < -0.01 -> "In lieve calo ↘️"
            else -> "Stabile ➡️"
        }
    }

    private fun calculatePrediction(values: List<Float>, trend: String): String {
        if (values.size < 3) return "Dati insufficienti"
        
        val last = values.last()
        val avgChange = values.zipWithNext { a, b -> b - a }.average()
        val nextValue = last + avgChange
        
        return "Atteso ~${"%.1f".format(nextValue)} nel prossimo rilevamento"
    }

    private fun generateAdvice(name: String, values: List<Float>, trend: String, isAnomaly: Boolean): String {
        val last = values.last()
        
        return when {
            isAnomaly -> "Rilevata un'anomalia improvvisa! Verifica l'integrità del sensore o l'ambiente circostante."
            name.contains("Temp", ignoreCase = true) -> {
                if (last > 40) "Temperatura elevata: assicurati che ci sia ventilazione adeguata."
                else if (trend.contains("crescita")) "Trend in aumento: monitora per evitare surriscaldamenti."
                else "Temperatura operativa ottimale."
            }
            name.contains("Batt", ignoreCase = true) || name.contains("Volt", ignoreCase = true) -> {
                if (last < 3.5) "Batteria quasi scarica: pianifica una ricarica o sostituzione a breve."
                else if (trend.contains("calo")) "Consumo rilevato: l'autonomia sta diminuendo costantemente."
                else "Livello energetico stabile."
            }
            name.contains("Umid", ignoreCase = true) -> {
                if (last > 70) "Umidità alta: rischio condensa. Considera di deumidificare."
                else "Livello di umidità nella norma."
            }
            else -> "Il sistema sembra funzionare regolarmente secondo i parametri analizzati."
        }
    }

    private fun generateGeneralSummary(insights: List<InsightData>): String {
        if (insights.isEmpty()) return "Nessun dato sufficiente per un'analisi globale."
        
        val anomalies = insights.count { it.isAnomaly }
        val criticals = insights.count { it.advice?.contains("elevata", true) == true || it.advice?.contains("scarica", true) == true }
        
        return when {
            anomalies > 0 -> "Attenzione: Rilevate $anomalies anomalie nei sensori. Il sistema richiede un controllo manuale."
            criticals > 0 -> "Stato Critico: Alcuni parametri sono fuori soglia. Consulta i suggerimenti qui sotto."
            else -> "Tutto sotto controllo. I sensori mostrano un andamento regolare e non sono previste criticità immediate."
        }
    }

    private fun calculateHealthScore(insights: List<InsightData>): Int {
        if (insights.isEmpty()) return 100
        
        var score = 100
        
        // Deduct points for anomalies
        val anomalies = insights.count { it.isAnomaly }
        score -= (anomalies * 15)
        
        // Deduct points for negative trends or critical values
        insights.forEach { insight ->
            val advice = insight.advice ?: ""
            if (advice.contains("elevata", true) || advice.contains("scarica", true)) {
                score -= 10
            } else if (insight.trend?.contains("calo") == true || insight.trend?.contains("crescita") == true) {
                // Slight penalty for unstable trends if they aren't critical
                score -= 2
            }
        }
        
        return score.coerceIn(0, 100)
    }
}
