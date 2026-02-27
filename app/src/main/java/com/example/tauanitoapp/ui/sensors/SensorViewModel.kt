package com.example.tauanitoapp.ui.sensors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tauanitoapp.data.CustomerDeviceMap
import com.example.tauanitoapp.data.model.Device
import com.example.tauanitoapp.data.repository.SensorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SensorUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val devices: List<Device> = emptyList(),
    val searchQuery: String = "",
    val selectedCustomer: String? = null
) {
    val filteredDevices: List<Device>
        get() {
            var result = devices
            if (searchQuery.isNotBlank())
                result = result.filter { it.name.contains(searchQuery, ignoreCase = true) }
            if (selectedCustomer != null)
                result = result.filter { device ->
                    CustomerDeviceMap.matchesCustomer(selectedCustomer, device.id, device.name)
                }
            return result
        }
}

class SensorViewModel(
    private val repository: SensorRepository = SensorRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SensorUiState())
    val uiState: StateFlow<SensorUiState> = _uiState

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val devices = repository.getDevices()
                _uiState.value = _uiState.value.copy(isLoading = false, devices = devices)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Errore caricamento dispositivi"
                )
            }
        }
    }

    fun onSearchChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun onCustomerSelect(customer: String?) {
        _uiState.value = _uiState.value.copy(selectedCustomer = customer)
    }
}
