package com.example.tauanitoapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.tauanitoapp.data.repository.NotificationRepository
import com.example.tauanitoapp.ui.history.HistoryRoute
import com.example.tauanitoapp.ui.insights.InsightsRoute
import com.example.tauanitoapp.ui.login.LoginRoute
import com.example.tauanitoapp.ui.notifications.NotificationScreen
import com.example.tauanitoapp.ui.sensors.SensorRoute
import com.example.tauanitoapp.ui.sensors.SensorViewModel
import com.example.tauanitoapp.ui.settings.SettingsScreen
import com.example.tauanitoapp.ui.theme.TauanitoAppTheme
import com.example.tauanitoapp.ui.theme.SettingsViewModel
import com.example.tauanitoapp.ui.welcome.WelcomeScreen
import com.example.tauanitoapp.utils.BiometricHelper
import com.example.tauanitoapp.utils.SecurePreferences
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

private const val ROUTE_WELCOME = "welcome"
private const val ROUTE_LOGIN = "login"
private const val ROUTE_SENSORS = "sensors"
private const val ROUTE_HISTORY = "history/{deviceId}"
private const val ROUTE_INSIGHTS = "insights/{deviceId}"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_NOTIFICATIONS = "notifications"

class MainActivity : FragmentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("Tauanito", "Permission for notifications granted")
        } else {
            Log.d("Tauanito", "Permission for notifications denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isPlayServicesAvailable()) {
            try {
                FirebaseApp.initializeApp(this)
                askNotificationPermission()
                getFcmToken()
            } catch (e: Exception) {
                Log.e("TauanitoFirebase", "Errore inizializzazione: ${e.message}")
            }
        } else {
            Log.d("Tauanito", "Google Play Services non disponibile (es. GrapheneOS) — Firebase disabilitato")
        }

        // Se è la PRIMISSIMA VOLTA che l'app viene aperta (dopo installazione o pulizia dati),
        // resettiamo tutto e impostiamo la biometria a OFF per sicurezza.
        if (!SecurePreferences.isInitialized(this)) {
            SecurePreferences.clearCredentials(this)
            SecurePreferences.setBiometricEnabled(this, false)
            SecurePreferences.setInitialized(this) // Segniamo che l'app è stata inizializzata
        }

        enableEdgeToEdge()

        setContent {
            TauanitoApp(this)
        }
    }

    private fun isPlayServicesAvailable(): Boolean {
        return try {
            packageManager.getPackageInfo("com.google.android.gms", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun getFcmToken() {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) return@addOnCompleteListener
                Log.d("Tauanito", "FCM Token: ${task.result}")
            }
        } catch (e: Exception) { }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TauanitoApp(activity: FragmentActivity) {
    val settingsViewModel: SettingsViewModel = viewModel()
    val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
    val refreshIntervalMs by settingsViewModel.refreshIntervalMs.collectAsState()
    val isBiometricEnabled by settingsViewModel.isBiometricEnabled.collectAsState()

    TauanitoAppTheme(darkTheme = isDarkMode) {
        val navController = rememberNavController()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        
        val sensorViewModel: SensorViewModel = viewModel()
        val sensorState by sensorViewModel.uiState.collectAsState()

        val notifications by NotificationRepository.notifications.collectAsState()
        val unreadNotifications = notifications.count { !it.isRead }

        // Gestione visibilità Drawer basata sulla rotta corrente
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        
        // Il Drawer è attivo (swipe abilitato) solo nelle rotte interne
        val isDrawerGesturesEnabled = currentRoute != ROUTE_WELCOME && currentRoute != ROUTE_LOGIN

        LaunchedEffect(refreshIntervalMs) {
            sensorViewModel.updateRefreshInterval(refreshIntervalMs)
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = isDrawerGesturesEnabled, // DISABILITA SWIPE SE NON LOGGATI
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Tauanito Menù",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Divider()
                    
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("Dashboard") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(ROUTE_SENSORS)
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        label = { 
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Accesso Biometrico")
                                Switch(
                                    checked = isBiometricEnabled,
                                    onCheckedChange = { enabled ->
                                        if (enabled) {
                                            if (BiometricHelper.isBiometricAvailable(activity)) {
                                                BiometricHelper.showBiometricPrompt(
                                                    activity = activity,
                                                    onSuccess = { settingsViewModel.toggleBiometric(true) },
                                                    onError = { }
                                                )
                                            }
                                        } else {
                                            settingsViewModel.toggleBiometric(false)
                                        }
                                    },
                                    modifier = Modifier.scale(0.7f)
                                )
                            }
                        },
                        selected = false,
                        onClick = { }
                    )
                    
                    NavigationDrawerItem(
                        icon = { 
                            Icon(
                                Icons.Default.Warning, 
                                contentDescription = null,
                                tint = if (sensorState.showLowBatteryOnly) Color.Red else LocalContentColor.current
                            ) 
                        },
                        label = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Batteria Scarica")
                                if (sensorState.lowBatteryCount > 0) {
                                    Badge(modifier = Modifier.padding(start = 8.dp)) {
                                        Text(sensorState.lowBatteryCount.toString())
                                    }
                                }
                            }
                        },
                        selected = sensorState.showLowBatteryOnly,
                        onClick = {
                            sensorViewModel.toggleLowBatteryFilter()
                            scope.launch { drawerState.close() }
                            navController.navigate(ROUTE_SENSORS)
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                        label = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Centro Notifiche")
                                if (unreadNotifications > 0) {
                                    Badge(modifier = Modifier.padding(start = 8.dp)) {
                                        Text(unreadNotifications.toString())
                                    }
                                }
                            }
                        },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(ROUTE_NOTIFICATIONS)
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Impostazioni") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(ROUTE_SETTINGS)
                        }
                    )
                }
            }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = ROUTE_WELCOME,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(ROUTE_WELCOME) {
                        WelcomeScreen(
                            onEnterClick = {
                                navController.navigate(ROUTE_LOGIN)
                            }
                        )
                    }

                    composable(ROUTE_LOGIN) {
                        LoginRoute(
                            onLoginSuccess = {
                                navController.navigate(ROUTE_SENSORS) {
                                    popUpTo(ROUTE_LOGIN) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(ROUTE_SENSORS) {
                        SensorRoute(
                            viewModel = sensorViewModel,
                            onLogout = {
                                navController.navigate(ROUTE_LOGIN) {
                                    popUpTo(ROUTE_SENSORS) { inclusive = true }
                                }
                            },
                            onDeviceClick = { deviceId ->
                                navController.navigate("history/$deviceId")
                            },
                            onOpenDrawer = {
                                scope.launch { drawerState.open() }
                            }
                        )
                    }

                    composable(ROUTE_HISTORY) { backStackEntry ->
                        val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
                        HistoryRoute(
                            deviceId = deviceId,
                            onBack = { navController.popBackStack() },
                            onNavigateToInsights = { id ->
                                navController.navigate("insights/$id")
                            }
                        )
                    }

                    composable(ROUTE_INSIGHTS) { backStackEntry ->
                        val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
                        InsightsRoute(
                            deviceId = deviceId,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(ROUTE_SETTINGS) {
                        SettingsScreen(
                            isDarkMode = isDarkMode,
                            refreshIntervalLabel = settingsViewModel.getRefreshIntervalLabel(),
                            onThemeToggle = { settingsViewModel.setDarkMode(it) },
                            onRefreshIntervalChange = { settingsViewModel.setRefreshInterval(it) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(ROUTE_NOTIFICATIONS) {
                        NotificationScreen(
                            isDarkMode = isDarkMode,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TauanitoAppPreview() {
}
