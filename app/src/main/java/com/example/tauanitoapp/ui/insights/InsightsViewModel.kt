package com.example.tauanitoapp.ui.insights

import android.app.Application
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

data class InsightsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val history: DeviceHistory? = null,
    val insights: List<InsightData> = emptyList(),
    val selectedSensors: Set<String> = emptySet(),
    val summary: String? = null, // General summary of all sensors
    val healthScore: Int = 100   // 0-100 score representing overall device health
)

class InsightsViewModel(application: Application, private val deviceId: String) : AndroidViewModel(application) {

    private val repository = SensorRepository(application)
    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    init {
        loadInsights()
    }

    fun loadInsights() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val data = repository.getDeviceHistory(deviceId)
                val processedInsights = processHistoryData(data.records)
                val summary = generateGeneralSummary(processedInsights)
                val healthScore = calculateHealthScore(processedInsights)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    history = data,
                    insights = processedInsights,
                    selectedSensors = processedInsights.map { it.sensorName }.toSet(),
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

    private fun processHistoryData(records: List<HistoryRecord>): List<InsightData> {
        val sensorMap = mutableMapOf<String, MutableList<Pair<Long, Float>>>()
        val unitMap = mutableMapOf<String, String?>()

        records.forEach { record ->
            val timestamp = try {
                dateFormat.parse(record.timestamp)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
            if (timestamp == 0L) return@forEach

            record.readings.forEach { reading ->
                val cleanValue = reading.value
                    .replace(reading.unit ?: "", "")
                    .replace(",", ".")
                    .replace("[^0-9.-]".toRegex(), "")
                    .trim()
                
                val floatValue = cleanValue.toFloatOrNull()
                if (floatValue != null) {
                    sensorMap.getOrPut(reading.name) { mutableListOf() }.add(timestamp to floatValue)
                    unitMap[reading.name] = reading.unit
                }
            }
        }

        return sensorMap.map { (name, dataPoints) ->
            val sortedData = dataPoints.sortedBy { it.first }
            val timestamps = sortedData.map { it.first }
            val values = sortedData.map { it.second }
            
            val trend = calculateTrend(values)
            val isAnomaly = detectAnomaly(values)
            val prediction = calculatePrediction(values, trend)
            val advice = generateAdvice(name, values, trend, isAnomaly)

            InsightData(
                sensorName = name,
                values = values,
                timestamps = timestamps,
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
