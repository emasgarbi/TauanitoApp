package com.example.tauanitoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tauanitoapp.ui.login.LoginRoute
import com.example.tauanitoapp.ui.sensors.SensorRoute
import com.example.tauanitoapp.ui.welcome.WelcomeScreen
import com.example.tauanitoapp.ui.theme.TauanitoAppTheme

private const val ROUTE_WELCOME = "welcome"
private const val ROUTE_LOGIN = "login"
private const val ROUTE_SENSORS = "sensors"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TauanitoApp()
        }
    }
}

@Composable
fun TauanitoApp() {
    TauanitoAppTheme {
        val navController = rememberNavController()

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
                        onLogout = {
                            navController.navigate(ROUTE_LOGIN) {
                                popUpTo(ROUTE_SENSORS) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TauanitoAppPreview() {
    TauanitoApp()
}
