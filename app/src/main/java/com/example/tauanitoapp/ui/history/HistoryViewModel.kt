package com.example.tauanitoapp.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tauanitoapp.data.model.DeviceHistory
import com.example.tauanitoapp.data.repository.SensorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HistoryUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val history: DeviceHistory? = null,
    val isDownloading: Boolean = false,
    val downloadData: ByteArray? = null
)

class HistoryViewModel(application: Application, private val deviceId: String) : AndroidViewModel(application) {

    private val repository = SensorRepository(application)
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val data = repository.getDeviceHistory(deviceId)
                _uiState.value = _uiState.value.copy(isLoading = false, history = data)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Errore caricamento storico"
                )
            }
        }
    }

    fun downloadCsv() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloading = true)
            try {
                val data = repository.downloadDeviceCsv(deviceId)
                _uiState.value = _uiState.value.copy(isDownloading = false, downloadData = data)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isDownloading = false, errorMessage = "Errore download CSV")
            }
        }
    }

    fun onDownloadConsumed() {
        _uiState.value = _uiState.value.copy(downloadData = null)
    }
}
