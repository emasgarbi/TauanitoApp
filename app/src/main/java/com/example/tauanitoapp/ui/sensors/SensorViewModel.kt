package com.example.tauanitoapp.ui.sensors

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tauanitoapp.data.CustomerDeviceMap
import com.example.tauanitoapp.data.model.BatteryLevel
import com.example.tauanitoapp.data.model.Device
import com.example.tauanitoapp.data.repository.SensorRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

data class SensorUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val devices: List<Device> = emptyList(),
    val searchQuery: String = "",
    val selectedCustomer: String? = null,
    val showLowBatteryOnly: Boolean = false,
    val globalHealthScore: Int = 100,
    val globalSummary: String = "Analisi in corso..."
) {
    // ... filteredDevices and other properties
    val filteredDevices: List<Device>
        get() {
            var result = devices
            if (showLowBatteryOnly) {
                result = result.filter { 
                    it.batteryLevel == BatteryLevel.LOW || it.batteryLevel == BatteryLevel.EMPTY 
                }
            }
            if (searchQuery.isNotBlank())
                result = result.filter { it.name.contains(searchQuery, ignoreCase = true) }
            if (selectedCustomer != null)
                result = result.filter { device ->
                    com.example.tauanitoapp.data.CustomerDeviceMap.matchesCustomer(selectedCustomer, device.id, device.name)
                }
            
            return if (showLowBatteryOnly || selectedCustomer != null) {
                result.sortedByDescending { parseTimestamp(it.timestamp) }
            } else {
                result
            }
        }

    private fun parseTimestamp(timestamp: String?): Long {
        if (timestamp.isNullOrBlank()) return 0L
        return try {
            val sdf = SimpleDateFormat("dd.MM.yyyy - HH:mm", Locale.getDefault())
            sdf.parse(timestamp)?.time ?: 0L
        } catch (e: Exception) { 0L }
    }

    val totalDevices: Int get() = devices.size
    val lowBatteryCount: Int get() = devices.count { 
        it.batteryLevel == BatteryLevel.LOW || it.batteryLevel == BatteryLevel.EMPTY 
    }
}

class SensorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SensorRepository(application)
    private val _uiState = MutableStateFlow(SensorUiState())
    val uiState: StateFlow<SensorUiState> = _uiState

    private var refreshJob: Job? = null
    private var currentIntervalMs: Long = 300000L

    fun updateRefreshInterval(intervalMs: Long) {
        currentIntervalMs = intervalMs
        if (_uiState.value.devices.isNotEmpty()) {
            startAutoRefresh()
        }
    }

    fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                refreshData()
                delay(currentIntervalMs)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { refreshData() }
    }

    private suspend fun refreshData() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        try {
            val devices = repository.getDevices()
            val (score, summary) = calculateGlobalAIStatus(devices)
            _uiState.value = _uiState.value.copy(
                isLoading = false, 
                devices = devices,
                globalHealthScore = score,
                globalSummary = summary
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = e.message ?: "Errore caricamento"
            )
        }
    }

    private fun calculateGlobalAIStatus(devices: List<Device>): Pair<Int, String> {
        if (devices.isEmpty()) return 100 to "Nessun dispositivo rilevato."
        
        var score = 100
        val criticalDevices = devices.count { 
            it.batteryLevel == BatteryLevel.LOW || it.batteryLevel == BatteryLevel.EMPTY 
        }
        
        score -= (criticalDevices * 10)
        
        val summary = when {
            criticalDevices > 0 -> "Attenzione: $criticalDevices dispositivi richiedono manutenzione (batteria scarica)."
            devices.size > 0 -> "Sistema monitorato dall'IA: Tutti i dispositivi risultano operativi e stabili."
            else -> "In attesa di dati dai sensori."
        }
        
        return score.coerceIn(0, 100) to summary
    }

    fun onSearchChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun onCustomerSelect(customer: String?) {
        _uiState.value = _uiState.value.copy(selectedCustomer = customer)
    }

    fun toggleLowBatteryFilter() {
        _uiState.value = _uiState.value.copy(showLowBatteryOnly = !_uiState.value.showLowBatteryOnly)
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }
}
