package com.example.sennaccess

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
import com.example.sennaccess.ui.theme.*
import com.example.sennaccess.admin.*
import com.example.sennaccess.aprendiz.AprendizDashboard
import com.example.sennaccess.aprendiz.InstructorDashboard
import com.example.sennaccess.data.AuthRepository
import com.example.sennaccess.data.RolSeguro
import com.example.sennaccess.data.SessionManager
import com.example.sennaccess.ui.verificacion2fa.DeepLink2FaStore
import kotlinx.coroutines.launch

// Activity principal: no contiene UI propia, solo el enrutador entre pantallas.
// Extiende AppCompatActivity porque el BiometricPrompt requiere un FragmentActivity.
class MainActivity : AppCompatActivity() {
    // Reto 2FA llegado por deep link (links "Sí/No soy yo" del correo): abrir la
    // app con el enlace debe llevar al overlay de verificación en vez de dejar al
    // usuario en una pestaña web con "éxito" mientras la app sigue esperando.
    private val deepLinkChallenge = androidx.compose.runtime.mutableStateOf<String?>(null)

    // Extrae el id del reto desde un VIEW intent: acepta challenge_id, two_factor_id
    // o challengeId como query param en scheme senaaccess:// o https del backend.
    // También lee la pista dec (aprobar/denegar) del correo: solo es informativa,
    // jamás auto-aprueba sin sesión (el diálogo "¿Eres tú?" decide con token).
    private fun retoDesdeIntent(intent: android.content.Intent?): String? {
        val uri = intent?.data ?: return null
        if (intent.action != android.content.Intent.ACTION_VIEW) return null
        return uri.getQueryParameter("challenge_id")
            ?: uri.getQueryParameter("two_factor_id")
            ?: uri.getQueryParameter("challengeId")
            ?: uri.getQueryParameter("id")
    }

    // Pista dec del botón del correo (aprobar/denegar) o null si no viene.
    private fun decDesdeIntent(intent: android.content.Intent?): String? {
        val uri = intent?.data ?: return null
        if (intent.action != android.content.Intent.ACTION_VIEW) return null
        return uri.getQueryParameter("dec") ?: uri.getQueryParameter("decision")
    }

    // Guarda el deep link en el estado local (flujo login) y en el store global
    // (flujo dashboard con sesión, lo consume "¿Eres tú?").
    private fun guardarDeepLink(intent: android.content.Intent?) {
        val id = retoDesdeIntent(intent) ?: return
        val dec = decDesdeIntent(intent)
        deepLinkChallenge.value = id
        DeepLink2FaStore.guardar(id, dec)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        guardarDeepLink(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // PIN de excusa y código 2FA no salen en recientes ni screenshots.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        // Versión nueva: arranca desde cero (sin roles ni cuentas de la anterior).
        com.example.sennaccess.data.VersionGuard.aplicarSiActualizo(this)
        guardarDeepLink(intent)
        setContent {
            var isDark by rememberSaveable { mutableStateOf(true) }

            val appColors = if (isDark) darkAppColors() else lightAppColors()

            CompositionLocalProvider(LocalAppColors provides appColors) {
                SennaccessTheme(darkTheme = isDark) {
                    // Identificador de la pantalla activa; cada cambio dispara la transición.
                    var currentScreen by rememberSaveable { mutableStateOf("splash") }
                    // Pila de retroceso para que el boton del sistema vuelva atras.
                    val pilaRetroceso = remember { ArrayDeque<String>() }
                    // Avanza guardando el origen; los dashboards limpian la pila.
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
                    // Retrocede una pantalla; false si ya esta en la raiz.
                    fun retroceder(): Boolean {
                        if (pilaRetroceso.isNotEmpty()) {
                            currentScreen = pilaRetroceso.removeLast()
                            return true
                        }
                        // Mapeo directo por si la pila esta vacia tras recomposicion.
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
                    // El boton del sistema retrocede; en dashboards se consume para no cerrar la app.
                    val enRaiz = currentScreen == "splash" || currentScreen == "landing"
                    BackHandler(enabled = !enRaiz) {
                        retroceder()
                    }

                    // Deep link 2FA del correo (solo deep-link directo): sin sesión va al
                    // login y abre el overlay en espera; con sesión va al dashboard de
                    // su rol y el diálogo "¿Eres tú?" lo consume (nunca auto-aprueba).
                    val retoLink = deepLinkChallenge.value
                    LaunchedEffect(retoLink) {
                        if (retoLink == null) return@LaunchedEffect
                        if (SessionManager.token != null) {
                            when (RolSeguro.normalizar(SessionManager.userRole)) {
                                "aprendiz" -> irA("aprendiz_dashboard")
                                "instructor" -> irA("instructor_dashboard")
                                "admin" -> irA("admin")
                                else -> irA("login")
                            }
                        } else {
                            irA("login")
                        }
                    }

                    // Cierre de sesión: avisa al servidor (registra la "Salida") de forma
                    // best-effort y luego limpia la sesión local y regresa al landing.
                    val scope = rememberCoroutineScope()
                    val cerrarSesion: () -> Unit = {
                        scope.launch {
                            try {
                                SessionManager.token?.let { AuthRepository().logout(it) }
                            } catch (_: Exception) { /* la sesión local se limpia igual */ }
                            SessionManager.clear()
                            pilaRetroceso.clear()
                            currentScreen = "landing"
                        }
                    }

                    // Contenedor raíz con el color de fondo según el tema activo.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(appColors.background)
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
                                onFinished = { irA("landing") },
                                isDark = isDark
                            )
                            "landing" -> LandingScreen(
                                onNavigateToLogin = { irA("login") },
                                onNavigateToRegister = { irA("register") },
                                isDark = isDark,
                                onToggleTheme = { isDark = !isDark }
                            )
                            "login" -> LoginScreen(
                                onNavigateToRegister = { irA("register") },
                                onNavigateToRecovery = { irA("recovery") },
                                // Mapeo rol -> pantalla con lista cerrada: un rol vacío o
                                // desconocido NUNCA entra como admin (antes el else caía a
                                // admin y tras activar el 2FA se ingresaba con privilegios).
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
                                // Si la app se abrió desde el correo sin sesión, entra directo
                                // al reto en espera (con sesión el reto lo toma el dashboard).
                                retoInicialId = if (SessionManager.token == null) deepLinkChallenge.value else null,
                                onRetoConsumido = {
                                    deepLinkChallenge.value = null
                                    if (SessionManager.token == null) DeepLink2FaStore.limpiar()
                                }
                            )
                            "register" -> RegisterScreen(
                                onBackToLogin = { retroceder() },
                                isDark = isDark,
                                onToggleTheme = { isDark = !isDark }
                            )
                            "recovery" -> PasswordRecoveryScreen(
                                onBackToLogin = { irA("landing") },
                                onNavigateToReset = { irA("reset") },
                                isDark = isDark,
                                onToggleTheme = { isDark = !isDark }
                            )
                            "reset" -> PasswordResetScreen(
                                onBackToLogin = { irA("login") },
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
