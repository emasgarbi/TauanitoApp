package com.example.tauanitoapp.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tauanitoapp.R
import com.example.tauanitoapp.utils.BiometricHelper
import com.example.tauanitoapp.utils.SecurePreferences

@Composable
fun LoginRoute(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    // Usiamo l'activity come owner per condividere lo stesso SettingsViewModel del MainActivity/Drawer
    val settingsViewModel: com.example.tauanitoapp.ui.theme.SettingsViewModel = if (activity != null) {
        viewModel(activity)
    } else {
        viewModel()
    }
    
    val isBiometricEnabled by settingsViewModel.isBiometricEnabled.collectAsState()

    // AUTO-ATTIVAZIONE: Si attiva solo se l'utente ha scritto l'email corretta e l'opzione è attiva
    LaunchedEffect(state.email, isBiometricEnabled) {
        val savedEmail = SecurePreferences.getSavedEmail(context)
        val savedPassword = SecurePreferences.getSavedPassword(context)
        
        val isEmailMatching = state.email.isNotBlank() && 
                             state.email.trim().equals(savedEmail?.trim(), ignoreCase = true)

        if (isBiometricEnabled && isEmailMatching && activity != null && !savedPassword.isNullOrBlank()) {
            BiometricHelper.showBiometricPrompt(
                activity = activity,
                onSuccess = { viewModel.loginWithBiometrics(onLoginSuccess) },
                onError = { }
            )
        }
    }

    if (state.isLoggedIn) {
        onLoginSuccess()
        return
    }

    LoginScreen(
        state            = state,
        onEmailChange    = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLoginClick     = viewModel::login,
        isBiometricEnabledOption = isBiometricEnabled,
        onBiometricClick = {
            if (activity != null) {
                BiometricHelper.showBiometricPrompt(
                    activity  = activity,
                    onSuccess = { viewModel.loginWithBiometrics(onLoginSuccess) },
                    onError   = { }
                )
            }
        }
    )
}

@Composable
fun LoginScreen(
    state:            LoginUiState,
    onEmailChange:    (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick:     () -> Unit,
    isBiometricEnabledOption: Boolean,
    onBiometricClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter            = painterResource(R.drawable.immaginesfondo),
            contentDescription = null,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.matchParentSize()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.tauanito_logo),
                contentDescription = "Logo",
                modifier = Modifier.height(100.dp).padding(bottom = 16.dp)
            )
            Text(
                text  = "Tauanito Login",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value         = state.email,
                onValueChange = onEmailChange,
                modifier      = Modifier.fillMaxWidth(),
                label         = { Text("Email") },
                leadingIcon   = { Icon(Icons.Default.Email, contentDescription = null, tint = Color.DarkGray) },
                singleLine    = true,
                shape         = RoundedCornerShape(16.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedTextColor        = Color.Black,
                    unfocusedTextColor      = Color.Black,
                    focusedLabelColor       = Color.Black,
                    unfocusedLabelColor     = Color.Black,
                    focusedContainerColor   = Color.White.copy(alpha = 0.9f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.8f),
                    focusedBorderColor      = Color.Black,
                    unfocusedBorderColor    = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value                = state.password,
                onValueChange        = onPasswordChange,
                modifier             = Modifier.fillMaxWidth(),
                label                = { Text("Password") },
                leadingIcon          = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.DarkGray) },
                singleLine           = true,
                visualTransformation = PasswordVisualTransformation(),
                shape                = RoundedCornerShape(16.dp),
                colors               = OutlinedTextFieldDefaults.colors(
                    focusedTextColor        = Color.Black,
                    unfocusedTextColor      = Color.Black,
                    focusedLabelColor       = Color.Black,
                    unfocusedLabelColor     = Color.Black,
                    focusedContainerColor   = Color.White.copy(alpha = 0.9f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.8f),
                    focusedBorderColor      = Color.Black,
                    unfocusedBorderColor    = Color.Gray
                )
            )

            if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text  = state.errorMessage,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick  = onLoginClick,
                    enabled  = !state.isLoading,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape    = RoundedCornerShape(16.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier    = Modifier.height(24.dp),
                            strokeWidth = 2.dp,
                            color       = Color.White
                        )
                    } else {
                        Text("Login", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Pulsante biometrico: visibile immediatamente se l'opzione è attiva e ci sono credenziali salvate
                val currentContext = LocalContext.current
                val savedEmail     = SecurePreferences.getSavedEmail(currentContext)
                val savedPassword  = SecurePreferences.getSavedPassword(currentContext)
                val hasLoggedIn    = SecurePreferences.hasLoggedInOnce(currentContext)
                
                val hasCredentials = hasLoggedIn && !savedEmail.isNullOrBlank() && !savedPassword.isNullOrBlank()
                
                // Mostra il pulsante se l'opzione è attiva, indipendentemente da cosa è scritto nei campi
                if (BiometricHelper.isBiometricAvailable(currentContext) && 
                    hasCredentials &&
                    isBiometricEnabledOption) {
                    IconButton(
                        onClick  = onBiometricClick,
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Login Biometrico",
                            tint     = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}
