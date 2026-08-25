package com.example.sennaccess

// Pantalla raíz de la aplicación: actúa como contenedor de navegación global.
// Se muestra desde el arranque y decide qué pantalla pintar según un identificador
// de pantalla en estado. Los dashboards por rol cierran el flujo de navegación.

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.example.sennaccess.ui.theme.*
import com.example.sennaccess.admin.*
import com.example.sennaccess.aprendiz.AprendizDashboard
import com.example.sennaccess.aprendiz.InstructorDashboard
import com.example.sennaccess.data.AuthRepository
import com.example.sennaccess.data.SessionManager
import kotlinx.coroutines.launch

// Activity principal: no contiene UI propia, solo el enrutador entre pantallas.
// Extiende AppCompatActivity porque el BiometricPrompt requiere un FragmentActivity.
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Preferencia de tema (claro/oscuro) conservada ante cambios de configuración.
            var isDark by rememberSaveable { mutableStateOf(true) }
            val appColors = if (isDark) darkAppColors() else lightAppColors()

            CompositionLocalProvider(LocalAppColors provides appColors) {
                SennaccessTheme(darkTheme = isDark) {
                    // Identificador de la pantalla activa; cada cambio dispara la transición.
                    var currentScreen by rememberSaveable { mutableStateOf("splash") }

                    // Cierre de sesión: avisa al servidor (registra la "Salida") de forma
                    // best-effort y luego limpia la sesión local y regresa al landing.
                    val scope = rememberCoroutineScope()
                    val cerrarSesion: () -> Unit = {
                        scope.launch {
                            try {
                                SessionManager.token?.let { AuthRepository().logout(it) }
                            } catch (_: Exception) { /* la sesión local se limpia igual */ }
                            SessionManager.clear()
                            currentScreen = "landing"
                        }
                    }

                    // Contenedor raíz con el color de fondo según el tema activo.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (isDark) Color(0xFF07090D) else Color(0xFFF5F5F5))
                    ) {
                        // Transición suave entre pantallas (Crossfade) sin alterar
                        // rutas, estado ni callbacks de navegación.
                        Crossfade(
                            targetState = currentScreen,
                            animationSpec = tween(300),
                            label = "screenCrossfade"
                        ) { screen ->
                        // Enrutamiento: mapea cada identificador a su pantalla y callbacks.
                        when (screen) {
                            "splash" -> SplashScreen(
                                onFinished = { currentScreen = "landing" },
                                isDark = isDark
                            )
                            "landing" -> LandingScreen(
                                onNavigateToLogin = { currentScreen = "login" },
                                onNavigateToRegister = { currentScreen = "register" },
                                isDark = isDark,
                                onToggleTheme = { isDark = !isDark }
                            )
                            "login" -> LoginScreen(
                                onNavigateToRegister = { currentScreen = "register" },
                                onNavigateToRecovery = { currentScreen = "recovery" },
                                // Mapeo rol -> pantalla: aprendiz, instructor o admin
                                // (rol por defecto en caso de valor desconocido).
                                onLoginSuccess = { role ->
                                    val r = role.trim().lowercase()
                                    currentScreen = when (r) {
                                        "aprendiz" -> "aprendiz_dashboard"
                                        "instructor" -> "instructor_dashboard"
                                        else -> "admin"
                                    }
                                },
                                isDark = isDark,
                                onToggleTheme = { isDark = !isDark }
                            )
                            "register" -> RegisterScreen(
                                onBackToLogin = { currentScreen = "login" },
                                isDark = isDark,
                                onToggleTheme = { isDark = !isDark }
                            )
                            "recovery" -> PasswordRecoveryScreen(
                                onBackToLogin = { currentScreen = "landing" },
                                onNavigateToReset = { currentScreen = "reset" },
                                isDark = isDark,
                                onToggleTheme = { isDark = !isDark }
                            )
                            "reset" -> PasswordResetScreen(
                                onBackToLogin = { currentScreen = "login" },
                                isDark = isDark,
                                onToggleTheme = { isDark = !isDark }
                            )
                            // Dashboards por rol: al cerrar sesión se avisa al servidor
                            // (queda registrada la salida) y se regresa al aterrizaje.
                            "aprendiz_dashboard" -> AprendizDashboard(
                                onCerrarSesion = cerrarSesion,
                                isDark = isDark,
                                onToggleTheme = { isDark = !isDark }
                            )
                            "instructor_dashboard" -> InstructorDashboard(
                                onCerrarSesion = cerrarSesion,
                                isDark = isDark,
                                onToggleTheme = { isDark = !isDark }
                            )
                            "admin" -> AdminDashboard(
                                onCerrarSesion = cerrarSesion,
                                isDark = isDark,
                                onToggleTheme = { isDark = !isDark }
                            )
                        }
                        } // cierre Crossfade
                    }
                }
            }
        }
    }
}
