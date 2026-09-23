package com.example.sennaccess.aplicacion

// Pantalla raíz de la aplicación: actúa como contenedor de navegación global.
// Se muestra desde el arranque y decide qué pantalla pintar según un identificador
// de pantalla en estado. Los dashboards por rol cierran el flujo de navegación.

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
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
import com.example.sennaccess.comun.tema.*
import com.example.sennaccess.administrador.panel.*
import com.example.sennaccess.aprendiz.panel.PanelAprendiz
import com.example.sennaccess.instructor.panel.PanelInstructor
import com.example.sennaccess.datos.repositorios.RepositorioAutenticacion
import com.example.sennaccess.datos.sesion.RolSeguro
import com.example.sennaccess.datos.sesion.GestorSesion
import com.example.sennaccess.biometria.AlmacenHuella
import com.example.sennaccess.autenticacion.verificacion.AlmacenEnlaceDobleFactor
import kotlinx.coroutines.launch
import com.example.sennaccess.autenticacion.acceso.PantallaAcceso
import com.example.sennaccess.autenticacion.recuperacion.PantallaRecuperacionClave
import com.example.sennaccess.autenticacion.registro.PantallaRegistro
import com.example.sennaccess.autenticacion.recuperacion.PantallaRestablecerClave

class ActividadPrincipal : AppCompatActivity() {
    private val deepLinkChallenge = androidx.compose.runtime.mutableStateOf<String?>(null)

    private fun retoDesdeIntent(intent: android.content.Intent?): String? {
        val uri = intent?.data ?: return null
        if (intent.action != android.content.Intent.ACTION_VIEW) return null
        return uri.getQueryParameter("challenge_id")
            ?: uri.getQueryParameter("two_factor_id")
            ?: uri.getQueryParameter("challengeId")
            ?: uri.getQueryParameter("id")
    }

    private fun decDesdeIntent(intent: android.content.Intent?): String? {
        val uri = intent?.data ?: return null
        if (intent.action != android.content.Intent.ACTION_VIEW) return null
        return uri.getQueryParameter("dec") ?: uri.getQueryParameter("decision")
    }

    private fun guardarDeepLink(intent: android.content.Intent?) {
        val id = retoDesdeIntent(intent) ?: return
        val dec = decDesdeIntent(intent)
        deepLinkChallenge.value = id
        AlmacenEnlaceDobleFactor.guardar(id, dec)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        guardarDeepLink(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        com.example.sennaccess.datos.sesion.GuardianVersion.aplicarSiActualizo(this)
        GestorSesion.enlazar(applicationContext)
        guardarDeepLink(intent)
        setContent {
            var isDark by rememberSaveable { mutableStateOf(true) }

            val appColors = if (isDark) coloresOscuros() else coloresClaros()

            CompositionLocalProvider(ColoresAppLocal provides appColors) {
                TemaSennaccess(darkTheme = isDark) {
                    var currentScreen by rememberSaveable { mutableStateOf("splash") }
                    val pilaRetroceso = remember { ArrayDeque<String>() }
                    fun irA(destino: String) {
                        if (currentScreen == destino) return
                        if (destino == "aprendiz_dashboard" || destino == "instructor_dashboard" || destino == "admin") {
                            pilaRetroceso.clear()
                        } else {
                            pilaRetroceso.addLast(currentScreen)
                            if (pilaRetroceso.size > 20) pilaRetroceso.removeFirst()
                        }
                        currentScreen = destino
                    }
                    fun retroceder(): Boolean {
                        if (pilaRetroceso.isNotEmpty()) {
                            currentScreen = pilaRetroceso.removeLast()
                            return true
                        }
                        val anterior = when (currentScreen) {
                            "login" -> "landing"
                            "register" -> "login"
                            "recovery" -> "landing"
                            "reset" -> "login"
                            else -> null
                        }
                        if (anterior != null) {
                            currentScreen = anterior
                            return true
                        }
                        return false
                    }
                    val enRaiz = currentScreen == "splash" || currentScreen == "landing"
                    BackHandler(enabled = !enRaiz) {
                        retroceder()
                    }

                    // Guardia de autenticación: un dashboard sin token nunca se pinta.
                    // Cubre proceso restaurado y sesión expirada por 401.
                    LaunchedEffect(currentScreen) {
                        if (GestorSesion.token == null && (currentScreen == "aprendiz_dashboard" || currentScreen == "instructor_dashboard" || currentScreen == "admin")) {
                            pilaRetroceso.clear()
                            currentScreen = "landing"
                        }
                    }

                    val retoLink = deepLinkChallenge.value
                    LaunchedEffect(retoLink) {
                        if (retoLink == null) return@LaunchedEffect
                        if (GestorSesion.token != null) {
                            when (RolSeguro.normalizar(GestorSesion.userRole)) {
                                "aprendiz" -> irA("aprendiz_dashboard")
                                "instructor" -> irA("instructor_dashboard")
                                "admin" -> irA("admin")
                                else -> irA("login")
                            }
                        } else {
                            irA("login")
                        }
                    }

                    val scope = rememberCoroutineScope()
                    val cerrarSesion: () -> Unit = {
                        // Limpieza local inmediata: sesión, huella del usuario anterior y pila.
                        val tokenPrevio = GestorSesion.token
                        GestorSesion.clear()
                        AlmacenHuella.borrarTodo(applicationContext)
                        AlmacenEnlaceDobleFactor.limpiar()
                        pilaRetroceso.clear()
                        currentScreen = "landing"
                        scope.launch {
                            try {
                                if (tokenPrevio != null) RepositorioAutenticacion().logout(tokenPrevio)
                            } catch (_: Exception) { /* best-effort: lo local ya quedó limpio */ }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(appColors.background)
                    ) {
                        Crossfade(
                            targetState = currentScreen,
                            animationSpec = tween(300),
                            label = "screenCrossfade"
                        ) { screen ->
                        when (screen) {
                            "splash" -> PantallaCarga(
                                onFinished = { irA("landing") },
                                isDark = isDark
                            )
                            "landing" -> PantallaBienvenida(
                                onNavigateToLogin = { irA("login") },
                                onNavigateToRegister = { irA("register") },
                                isDark = isDark,
                                onToggleTheme = { isDark = !isDark }
                            )
                            "login" -> PantallaAcceso(
                                onNavigateToRegister = { irA("register") },
                                onNavigateToRecovery = { irA("recovery") },
                                onLoginSuccess = { role ->
                                    when (RolSeguro.normalizar(role)) {
                                        "aprendiz" -> irA("aprendiz_dashboard")
                                        "instructor" -> irA("instructor_dashboard")
                                        "admin" -> irA("admin")
                                        else -> Unit
                                    }
                                },
                                isDark = isDark,
                                onToggleTheme = { isDark = !isDark },
                                retoInicialId = if (GestorSesion.token == null) deepLinkChallenge.value else null,
                                onRetoConsumido = {
                                    deepLinkChallenge.value = null
                                    if (GestorSesion.token == null) AlmacenEnlaceDobleFactor.limpiar()
                                }
                            )
                            "register" -> PantallaRegistro(
                                onBackToLogin = { retroceder() },
                                isDark = isDark,
                                onToggleTheme = { isDark = !isDark }
                            )
                            "recovery" -> PantallaRecuperacionClave(
                                onBackToLogin = { irA("landing") },
                                onNavigateToReset = { irA("reset") },
                                isDark = isDark,
                                onToggleTheme = { isDark = !isDark }
                            )
                            "reset" -> PantallaRestablecerClave(
                                onBackToLogin = { irA("login") },
                                isDark = isDark,
                                onToggleTheme = { isDark = !isDark }
                            )
                            "aprendiz_dashboard" -> PanelAprendiz(
                                onCerrarSesion = cerrarSesion,
                                isDark = isDark,
                                onToggleTheme = { isDark = !isDark }
                            )
                            "instructor_dashboard" -> PanelInstructor(
                                onCerrarSesion = cerrarSesion,
                                isDark = isDark,
                                onToggleTheme = { isDark = !isDark }
                            )
                            "admin" -> PanelAdministrador(
                                onCerrarSesion = cerrarSesion,
                                isDark = isDark,
                                onToggleTheme = { isDark = !isDark }
                            )
                        }
                        }
                    }
                }
            }
        }
    }
}
