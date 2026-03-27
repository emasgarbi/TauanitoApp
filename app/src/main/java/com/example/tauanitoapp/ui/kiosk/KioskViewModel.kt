package com.example.tauanitoapp.ui.kiosk

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tauanitoapp.data.model.Device
import com.example.tauanitoapp.data.repository.SensorRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class KioskUiState(
    val device: Device? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val lastUpdated: Long = 0L
)

class KioskViewModel(application: Application, private val deviceId: String) : AndroidViewModel(application) {

    private val repository = SensorRepository(application)
    private val prefs = application.getSharedPreferences("kiosk_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(KioskUiState())
    val uiState: StateFlow<KioskUiState> = _uiState

    private var refreshJob: Job? = null

    init {
        refresh()
        startAutoRefresh()
    }

    // ── PIN ──────────────────────────────────────────────────────────────────

    fun getPin(): String = prefs.getString("kiosk_pin", null) ?: ""

    fun isPinConfigured(): Boolean = prefs.contains("kiosk_pin") && getPin().isNotEmpty()

    fun setPin(pin: String) {
        prefs.edit().putString("kiosk_pin", pin).apply()
    }

    fun checkPin(input: String): Boolean = input == getPin()

    // ── Dati ─────────────────────────────────────────────────────────────────

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val devices = repository.getDevices()
                val device = devices.find { it.id == deviceId }
                _uiState.value = _uiState.value.copy(
                    device = device,
                    isLoading = false,
                    lastUpdated = System.currentTimeMillis(),
                    errorMessage = if (device == null) "Dispositivo non trovato" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Errore di rete"
                )
            }
        }
    }

    private fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                delay(30_000L)
                refresh()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }

    class Factory(private val application: Application, private val deviceId: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            KioskViewModel(application, deviceId) as T
    }
}
