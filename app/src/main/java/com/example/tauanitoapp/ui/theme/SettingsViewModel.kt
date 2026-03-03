package com.example.tauanitoapp.ui.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.tauanitoapp.utils.SecurePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    private val _refreshIntervalMs = MutableStateFlow(300000L)
    val refreshIntervalMs: StateFlow<Long> = _refreshIntervalMs

    // Carica lo stato reale dalle preferenze (che ora saranno resettate a FALSE)
    private val _isBiometricEnabled = MutableStateFlow(SecurePreferences.isBiometricEnabled(application))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }

    fun setRefreshInterval(label: String) {
        _refreshIntervalMs.value = when (label) {
            "30 secondi" -> 30000L
            "5 minuti"   -> 300000L
            "30 minuti"  -> 1800000L
            else         -> 300000L
        }
    }

    fun getRefreshIntervalLabel(): String {
        return when (_refreshIntervalMs.value) {
            30000L    -> "30 secondi"
            1800000L  -> "30 minuti"
            else      -> "5 minuti"
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        SecurePreferences.setBiometricEnabled(getApplication(), enabled)
        if (!enabled) {
            SecurePreferences.clearCredentials(getApplication())
        }
        _isBiometricEnabled.value = enabled
    }
}
