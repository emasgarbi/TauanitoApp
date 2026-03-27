package com.example.tauanitoapp.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tauanitoapp.data.model.DeviceHistory
import com.example.tauanitoapp.data.repository.SensorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HistoryUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val downloadError: String? = null,
    val history: DeviceHistory? = null,
    val isDownloading: Boolean = false,
    val showFilePicker: Boolean = false
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

    fun startDownloadProcess() {
        _uiState.value = _uiState.value.copy(showFilePicker = true, downloadError = null)
    }

    fun onFilePickerLaunched() {
        _uiState.value = _uiState.value.copy(showFilePicker = false)
    }

    fun downloadCsvToUri(uri: android.net.Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloading = true, downloadError = null)
            try {
                val data = repository.downloadDeviceCsv(deviceId)
                val context = getApplication<Application>()
                withContext(kotlinx.coroutines.Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(data)
                    }
                }
                _uiState.value = _uiState.value.copy(isDownloading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDownloading = false, 
                    downloadError = "Errore durante il salvataggio del file"
                )
            }
        }
    }

    fun clearDownloadError() {
        _uiState.value = _uiState.value.copy(downloadError = null)
    }
}
