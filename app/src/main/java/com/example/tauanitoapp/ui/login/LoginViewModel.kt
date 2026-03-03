package com.example.tauanitoapp.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tauanitoapp.data.repository.SensorRepository
import com.example.tauanitoapp.utils.SecurePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val email:        String  = "",
    val password:     String  = "",
    val isLoading:    Boolean = false,
    val isLoggedIn:   Boolean = false,
    val errorMessage: String? = null
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SensorRepository(application)
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun onEmailChange(newValue: String) {
        _uiState.value = _uiState.value.copy(email = newValue, errorMessage = null)
    }

    fun onPasswordChange(newValue: String) {
        _uiState.value = _uiState.value.copy(password = newValue, errorMessage = null)
    }

    fun login() {
        val email = _uiState.value.email
        val password = _uiState.value.password

        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Inserisci email e password")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                repository.login(email, password)
                SecurePreferences.saveCredentials(getApplication(), email, password)
                _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true)
            } catch (e: Exception) {
                val msg = when {
                    e is java.net.UnknownHostException -> "Impossibile raggiungere il server. Verifica la connessione internet."
                    e.message?.contains("timeout", ignoreCase = true) == true -> "Connessione lenta. Riprova."
                    e.message?.contains("Email o password") == true -> e.message!!
                    else -> "Errore di connessione. Riprova."
                }
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = msg)
            }
        }
    }

    fun loginWithBiometrics(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val devices = repository.getDevices()
                if (devices.isNotEmpty() || repository.isSessionValid()) {
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true)
                    onSuccess()
                    return@launch
                }

                val email = SecurePreferences.getSavedEmail(getApplication())
                val password = SecurePreferences.getSavedPassword(getApplication())
                if (email != null && password != null) {
                    repository.login(email, password)
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true)
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Credenziali non trovate")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Sessione scaduta: inserisci password")
            }
        }
    }
}
