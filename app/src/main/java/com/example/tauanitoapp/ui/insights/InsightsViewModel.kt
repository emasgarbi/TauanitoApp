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
    val prediction: String?  // e.g., "Previsto aumento del 5%"
)

data class InsightsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val history: DeviceHistory? = null,
    val insights: List<InsightData> = emptyList(),
    val selectedSensors: Set<String> = emptySet()
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
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    history = data,
                    insights = processedInsights,
                    selectedSensors = processedInsights.map { it.sensorName }.toSet()
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

        // Group by sensor name and parse values
        records.forEach { record ->
            val timestamp = try {
                dateFormat.parse(record.timestamp)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
            if (timestamp == 0L) return@forEach

            record.readings.forEach { reading ->
                // Pulisce il valore: toglie unità di misura se presenti nella stringa, spazi e converte virgola in punto
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
            // Sort by timestamp (asc)
            val sortedData = dataPoints.sortedBy { it.first }
            val timestamps = sortedData.map { it.first }
            val values = sortedData.map { it.second }
            
            val trend = calculateTrend(values)
            val prediction = calculatePrediction(values, trend)

            InsightData(
                sensorName = name,
                values = values,
                timestamps = timestamps,
                unit = unitMap[name],
                trend = trend,
                prediction = prediction
            )
        }
    }

    private fun calculateTrend(values: List<Float>): String {
        if (values.size < 2) return "Dati insufficienti"
        
        // Simple linear trend using first and last few points or regression
        // Let's use the difference between the average of the first half and second half
        val mid = values.size / 2
        val firstHalfAvg = values.take(mid).average()
        val secondHalfAvg = values.drop(mid).average()
        
        val diff = secondHalfAvg - firstHalfAvg
        val threshold = firstHalfAvg * 0.02 // 2% change threshold for "stable"

        return when {
            diff > threshold -> "In crescita ↗️"
            diff < -threshold -> "In calo ↘️"
            else -> "Stabile ➡️"
        }
    }

    private fun calculatePrediction(values: List<Float>, trend: String): String {
        if (values.size < 3) return "Dati insufficienti per previsione"
        
        // Simple extrapolation based on recent rate of change
        val last = values.last()
        val secondLast = values[values.size - 2]
        val change = last - secondLast
        
        return if (trend.contains("crescita")) {
            "Previsto ulteriore aumento"
        } else if (trend.contains("calo")) {
            "Prevista ulteriore diminuzione"
        } else {
            "Prevista stabilità"
        }
    }
}
