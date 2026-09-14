// Pantalla de inicio de sesión: permite autenticarse con correo y contraseña
// y navegar al registro o a la recuperación de contraseña.
// Además ofrece ingreso con huella real (biometría local): el sistema pide el
// dedo y solo entonces se descifran las credenciales guardadas en el dispositivo
// (HuellaCredentialStore) para hacer el login contra el backend.
// Al autenticarse devuelve el rol al MainActivity para redirigir al dashboard.
// Paquete donde está este archivo
package com.example.sennaccess

// Importamos herramientas que vamos a usar en la interfaz
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.example.sennaccess.R
import com.example.sennaccess.data.HuellaCredentialStore
import com.example.sennaccess.data.DispositivoStore
import com.example.sennaccess.data.SessionManager
import com.example.sennaccess.ui.BiometricAuth
import com.example.sennaccess.ui.campoVisible
import androidx.biometric.BiometricPrompt
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import com.example.sennaccess.ui.theme.verdeMarca
import com.example.sennaccess.ui.LoginViewModel
import com.example.sennaccess.ui.LoginUiState
import com.example.sennaccess.ui.verificacion2fa.Verificacion2FaScreen
import com.example.sennaccess.ui.ios.AuthField
import com.example.sennaccess.ui.ios.ErrorBox
import com.example.sennaccess.ui.ios.GlowOutlinedButton
import com.example.sennaccess.ui.ios.GlowSpheres
import com.example.sennaccess.ui.ios.IosGlassCard
import com.example.sennaccess.ui.ios.PrimaryNeonButton
import com.example.sennaccess.ui.ios.ThemeToggleButton


// ESTA ES LA PANTALLA PRINCIPAL DEL LOGIN
@Composable
fun LoginScreen(

    // Funciones para navegar a otras pantallas
    onNavigateToRegister: () -> Unit,
    onNavigateToRecovery: () -> Unit,
    onLoginSuccess: (String) -> Unit,
    isDark: Boolean = true,
    onToggleTheme: () -> Unit = {},
    // Reto 2FA llegado por deep link (correo): se muestra directo sin login previo.
    retoInicialId: String? = null,
    onRetoConsumido: () -> Unit = {},
    viewModel: LoginViewModel = viewModel()
) {
    val colors = LocalAppColors.current
    // Estado del ViewModel observado en la UI: maneja Loading / Error / Success.
    val uiState by viewModel.uiState.collectAsState()

    // Contexto para el diálogo biométrico del sistema y el almacén de credenciales.
    val context = LocalContext.current
    // Id estable de ESTE teléfono: viaja en cada login para que el backend solo pida
    // 2FA desde dispositivos distintos al original (ver DispositivoStore).
    val deviceId = remember { DispositivoStore.obtenerId(context) }

    // Mensaje de error del botón INGRESAR CON HUELLA (biometría local).
    var huellaError by remember { mutableStateOf<String?>(null) }
    // Verdadero mientras el diálogo biométrico del sistema está en curso.
    var huellaOcupado by remember { mutableStateOf(false) }
    // Marca que el login en curso proviene de la huella: si el backend rechaza
    // las credenciales guardadas (contraseña cambiada), se borran y se avisa.
    var loginDesdeHuella by remember { mutableStateOf(false) }
    // Controla el diálogo de opt-in para registrar la huella tras un login manual.
    var askSaveBiometric by remember { mutableStateOf(false) }
    // Reto de verificación en dos pasos pendiente: si no es null se muestra el
    // overlay de 2FA (código del correo + aprobación desde otro dispositivo).
    var reto2FaById by remember { mutableStateOf<String?>(null) }
    // Error de rol desconocido: por seguridad nunca se navega a un dashboard
    // si el backend no devolvió un rol válido (antes caía a admin por defecto).
    var errorRol by remember { mutableStateOf<String?>(null) }

    // Deriva el reto 2FA desde el estado del ViewModel fuera de la composición:
    // hacerlo dentro del @Composable provocaba recomposiciones y mostraba el
    // diálogo de huella en el mismo frame que el overlay de verificación.
    LaunchedEffect(uiState) {
        val estado = uiState
        if (estado is LoginUiState.Success) {
            val res = estado.response
            if (res.two_factor_required == true && res.two_factor_id != null) {
                askSaveBiometric = false
                reto2FaById = res.two_factor_id
                viewModel.reset()
            }
        }
    }

    // Deep link del correo ("Sí/No soy yo"): si la app se abrió con un
    // challenge_id, se muestra el overlay aunque no hubo login previo aquí.
    LaunchedEffect(retoInicialId) {
        if (!retoInicialId.isNullOrBlank() && reto2FaById == null) {
            askSaveBiometric = false
            viewModel.reset()
            reto2FaById = retoInicialId
            onRetoConsumido()
        }
    }

    // Si el login con huella falla (credenciales guardadas inválidas), borra lo
    // guardado para forzar un nuevo registro en el próximo login manual.
    // Si tiene éxito, guarda el correo usado para el autocompletado del login.
    LaunchedEffect(uiState) {
        val estado = uiState
        if (estado is LoginUiState.Success) {
            estado.response.user?.user_email?.takeIf { it.isNotBlank() }?.let {
                SessionManager.guardarCorreoUsado(context, it.trim())
            }
        }
        if (estado is LoginUiState.Error && loginDesdeHuella) {
            loginDesdeHuella = false
            HuellaCredentialStore.borrar(context)
            huellaError = "Tus credenciales guardadas dejaron de ser válidas. Ingresa con tu contraseña y registra tu huella otra vez."
        } else if (estado !is LoginUiState.Loading) {
            loginDesdeHuella = false
        }
    }

    // Variables para guardar texto del correo y contraseña
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Caja principal que ocupa toda la pantalla
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Luces ambientales detrás del vidrio (acentúan el glassmorphism).
        GlowSpheres(isDark = isDark)

        // Tarjeta de login: se oculta mientras el reto 2FA está activo para que
        // nunca quede compuesta detrás del overlay de verificación (antes el
        // orden del Box pintaba el login encima y se veían superpuestos).
        if (reto2FaById == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
        ThemeToggleButton(
            isDark = isDark,
            onToggleTheme = onToggleTheme,
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
        )

        // Tarjeta principal del login
        IosGlassCard(modifier = Modifier.fillMaxWidth(0.95f).padding(vertical = 20.dp)) {

            // Column sirve para poner elementos uno debajo del otro
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        // El contenido se encoge sobre el teclado para no quedar tapado.
                        .imePadding(),

                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Imagen del logo SENA
                Image(
                    painter = painterResource(R.drawable.logo_sena),
                    contentDescription = "Logo SENA",
                    modifier = Modifier
                        .size(110.dp)
                        .padding(bottom = 12.dp)
                )

                // Texto "Sena Access"
                // buildAnnotatedString sirve para cambiar color a partes del texto
                Text(
                    text = buildAnnotatedString {

                        append("Sena ")

                        // Esta parte pone "Access" verde y en negrita
                        withStyle(
                            style = SpanStyle(
                                color = verdeMarca(),
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append("Access")
                        }
                    },

                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                // Texto pequeño debajo
                Text(
                    "Acceso CCyS",
                    fontSize = 16.sp,
                    color = verdeMarca()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Formulario de login normal
                NormalLoginForm(
                    email = email,
                    password = password,

                    // Guardar cambios del correo
                    onEmailChange = {
                        email = it
                    },

                    // Guardar cambios contraseña
                    onPasswordChange = {
                        password = it
                    },

                    // Navegar pantallas
                    onRegisterClick = onNavigateToRegister,
                    onRecoveryClick = onNavigateToRecovery,
                    viewModel = viewModel,
                    uiState = uiState,
                    deviceId = deviceId,

                        // Ingreso con huella (biometría local): el sistema pide el dedo
                        // y solo entonces se descifran las credenciales guardadas para
                        // hacer el login normal contra el backend.
                        onBiometricLogin = {
                            if (!huellaOcupado) {
                                huellaError = null
                                errorRol = null
                                when {
                                    // Se exige biometría fuerte para descifrar: el PIN no puede
                                    // abrir el CryptoObject y antes eso cerraba la app.
                                    !BiometricAuth.isAvailable(context, forCrypto = true) ->
                                        huellaError = "Tu dispositivo no tiene huella configurada. Regístrala en Ajustes del sistema."
                                    !HuellaCredentialStore.hayGuardada(context) ->
                                        huellaError = "Aún no tienes una huella registrada. Inicia sesión con tu contraseña y acepta registrarla."
                                    else -> {
                                        val cipher = HuellaCredentialStore.prepararDescifrado(context)
                                        // Sin !! ni cast directo: si no hay actividad o cipher, se
                                        // informa en pantalla en vez de lanzar una excepción.
                                        val activity = BiometricAuth.activityDe(context)
                                        if (cipher == null || activity == null) {
                                            huellaError = "Tu huella se desvinculó. Inicia sesión con tu contraseña y regístrala de nuevo."
                                        } else {
                                            huellaOcupado = true
                                            try {
                                            BiometricAuth.authenticate(
                                                activity = activity,
                                                title = "Ingresa con tu huella",
                                                subtitle = "Confirma tu identidad para entrar a SenaAccess",
                                                cryptoObject = BiometricPrompt.CryptoObject(cipher),
                                                onSuccess = { result ->
                                                    huellaOcupado = false
                                                    try {
                                                        val seguro = result.cryptoObject?.cipher
                                                        if (seguro == null) {
                                                            huellaError = "No se pudo verificar tu huella. Inténtalo de nuevo."
                                                            return@authenticate
                                                        }
                                                        val credenciales = HuellaCredentialStore.leer(context, seguro)
                                                        loginDesdeHuella = true
                                                        viewModel.login(credenciales.first, credenciales.second, deviceId.ifBlank { null })
                                                    } catch (e: Exception) {
                                                        HuellaCredentialStore.borrar(context)
                                                        HuellaCredentialStore.borrarLlave()
                                                        huellaError = "No se pudieron leer tus credenciales. Inicia sesión y registra tu huella de nuevo."
                                                    }
                                                },
                                                onError = { motivo ->
                                                    huellaOcupado = false
                                                    huellaError = motivo
                                                },
                                                onFailed = { aviso ->
                                                    huellaError = aviso
                                                }
                                            )
                                            } catch (e: Exception) {
                                                huellaOcupado = false
                                                huellaError = "No se pudo iniciar la huella. Usa tu contraseña."
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        huellaLoading = huellaOcupado,
                        huellaError = huellaError
                    )
                }

                // Éxito del login (manual o con huella): si ya hay una huella registrada
                // navega directo al dashboard; si no, ofrece registrarla para poder usar
                // el botón INGRESAR CON HUELLA la próxima vez. El caso 2FA ya se derivó
                // en el LaunchedEffect superior y no llega aquí (se reseteó a Idle).
                if (uiState is LoginUiState.Success) {
                    val res = (uiState as LoginUiState.Success).response
                    // Guardia de rol: sin rol válido no se navega (MainActivity también
                    // bloquea, pero aquí se muestra el motivo en vez de quedar en negro).
                    val rolNormalizado = com.example.sennaccess.data.RolSeguro.normalizar(res.role)
                    if (rolNormalizado == null) {
                        LaunchedEffect(res) {
                            errorRol = "Tu cuenta no tiene un rol válido. Contacta al administrador."
                            com.example.sennaccess.data.SessionManager.clear()
                            viewModel.reset()
                        }
                    } else {
                    // Se evalúan una vez por intento de login exitoso.
                    var yaTieneHuella by remember(res) { mutableStateOf(HuellaCredentialStore.hayGuardada(context)) }
                    var navegando by remember(res) { mutableStateOf(false) }

                    if (yaTieneHuella) {
                        // Con huella registrada: entra directo, sin preguntar de nuevo.
                        LaunchedEffect(res) {
                            if (!navegando) {
                                navegando = true
                                viewModel.reset()
                                onLoginSuccess(res.role ?: "")
                            }
                        }
                    } else if (!askSaveBiometric) {
                        LaunchedEffect(res) { askSaveBiometric = true }
                    } else {
                        AlertDialog(
                            onDismissRequest = {
                                askSaveBiometric = false
                                viewModel.reset()
                                onLoginSuccess(res.role ?: "")
                            },
                            containerColor = colors.cardBackground.copy(alpha = 0.98f),
                            shape = RoundedCornerShape(24.dp),
                            icon = { Icon(Icons.Default.Fingerprint, contentDescription = null, tint = SenaGreen, modifier = Modifier.size(40.dp)) },
                            title = {
                                Text("¿Registrar tu huella?", color = colors.textPrimary, fontWeight = FontWeight.Bold)
                            },
                            text = {
                                Text(
                                    "Podrás ingresar solo con tu huella la próxima vez. Tus credenciales se guardan cifradas en este teléfono y se desbloquean únicamente con tu huella; nunca viajan en texto plano.",
                                    color = colors.textSecondary
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        askSaveBiometric = false
                                        // Resuelve la actividad aunque el contexto venga envuelto;
                                        // el cast directo devolvía null y rompía el registro.
                                        val activity = BiometricAuth.activityDe(context)
                                        if (activity == null) {
                                            viewModel.reset()
                                            onLoginSuccess(res.role ?: "")
                                            return@Button
                                        }
                                        try {
                                            // Llave segura: si la anterior quedó invalidada se
                                            // regenera en vez de lanzar y cerrar la app.
                                            val cipher = HuellaCredentialStore.prepararCifradoSeguro()
                                            huellaOcupado = true
                                            BiometricAuth.authenticate(
                                                activity = activity,
                                                title = "Registra tu huella",
                                                subtitle = "Toca el sensor para proteger tus credenciales",
                                                cryptoObject = BiometricPrompt.CryptoObject(cipher),
                                                onSuccess = { result ->
                                                    huellaOcupado = false
                                                    try {
                                                        val seguro = result.cryptoObject?.cipher
                                                        if (seguro == null) {
                                                            huellaError = "No se pudo activar la huella. Regístrala desde tu perfil."
                                                        } else {
                                                            HuellaCredentialStore.guardar(
                                                                context,
                                                                seguro,
                                                                res.user?.user_email ?: email.trim(),
                                                                password
                                                            )
                                                        }
                                                    } catch (e: Exception) {
                                                        // Si falla el guardado simplemente continúa:
                                                        // podrá registrarse desde MI HUELLA del perfil.
                                                    }
                                                    viewModel.reset()
                                                    onLoginSuccess(res.role ?: "")
                                                },
                                                onError = { _ ->
                                                    huellaOcupado = false
                                                    viewModel.reset()
                                                    onLoginSuccess(res.role ?: "")
                                                },
                                                onFailed = { aviso ->
                                                    huellaError = aviso
                                                }
                                            )
                                        } catch (e: Throwable) {
                                            huellaOcupado = false
                                            huellaError = "No se pudo preparar la huella. Inténtalo desde tu perfil."
                                            viewModel.reset()
                                            onLoginSuccess(res.role ?: "")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black),
                                    shape = RoundedCornerShape(28.dp),
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                                ) {
                                    Text("SÍ, REGISTRAR", fontWeight = FontWeight.ExtraBold)
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        askSaveBiometric = false
                                        viewModel.reset()
                                        onLoginSuccess(res.role ?: "")
                                    }
                                ) {
                                    Text("NO", color = colors.textSecondary, fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                    } // cierre diálogo ¿Registrar tu huella?
                    } // cierre rama con rol válido
                } // cierre if Success
                // Rol inválido: se informa sin navegar a ningún dashboard.
                if (errorRol != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ErrorBox(texto = errorRol!!)
                }
            }
            }
        } // cierre Box interno del login
        } // cierre if reto2FaById == null: el login no se compone durante el 2FA
        // Overlay de verificación en dos pasos al FINAL del Box raíz para que pinte
        // por encima del login (antes estaba primero y el login lo tapaba, por eso
        // se veían superpuestos). Mientras está activo el login ni se compone.
        reto2FaById?.let { challengeId ->
            Verificacion2FaScreen(
                challengeId = challengeId,
                isDark = isDark,
                onCancel = {
                    reto2FaById = null
                    viewModel.reset()
                },
                onLoginSuccess = { role ->
                    reto2FaById = null
                    viewModel.reset()
                    onLoginSuccess(role)
                }
            )
        }
        }




// FORMULARIO DE LOGIN NORMAL
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NormalLoginForm(

    email: String,
    password: String,

    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,

    onRegisterClick: () -> Unit,
    onRecoveryClick: () -> Unit,
    viewModel: LoginViewModel,
    uiState: LoginUiState,
    // Id de ESTE teléfono para el 2FA por dispositivo (ver DispositivoStore).
    deviceId: String = "",

    // Ingreso con huella (biometría local): lanza el prompt del sistema y muestra estados.
    onBiometricLogin: () -> Unit,
    huellaLoading: Boolean = false,
    huellaError: String? = null
) {

    val colors = LocalAppColors.current

    // Mensaje de error local (campos vacíos)
    var formError by remember { mutableStateOf<String?>(null) }

    // Indicador de carga derivado del estado del ViewModel.
    val isLoading = uiState is LoginUiState.Loading

    // Título
    Text(
        "Iniciar Sesión",
        style = MaterialTheme.typography.titleLarge,
        color = colors.textPrimary
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Campo correo con historial de correos guardados (autocompletado).
    val contextHistorial = LocalContext.current
    var expandirCorreos by remember { mutableStateOf(false) }
    val correosGuardados = remember { SessionManager.obtenerCorreosUsados(contextHistorial) }
    val sugerenciasCorreo = remember(email, correosGuardados) {
        if (correosGuardados.isEmpty()) emptyList()
        else if (email.isBlank()) correosGuardados
        else correosGuardados.filter { it.contains(email, ignoreCase = true) && !it.equals(email, ignoreCase = true) }
    }
    ExposedDropdownMenuBox(
        expanded = expandirCorreos && sugerenciasCorreo.isNotEmpty(),
        onExpandedChange = { expandirCorreos = it }
    ) {
AuthField(
                value = email,
                onValueChange = {
                    onEmailChange(it)
                    expandirCorreos = true
                },
                label = "Correo electrónico",
                modifier = Modifier.menuAnchor(),
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        ExposedDropdownMenu(
            expanded = expandirCorreos && sugerenciasCorreo.isNotEmpty(),
            onDismissRequest = { expandirCorreos = false }
        ) {
            sugerenciasCorreo.forEach { correo ->
                DropdownMenuItem(
                    text = { Text(correo) },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = verdeMarca()) },
                    onClick = {
                        onEmailChange(correo)
                        expandirCorreos = false
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Campo contraseña con toggle accesible y autofill para gestores.
    AuthField(
        value = password,
        onValueChange = onPasswordChange,
        label = "Contraseña",
        keyboardType = KeyboardType.Password,
        imeAction = ImeAction.Done,
        isPassword = true
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Botón ingresar con estado de carga integrado (spinner en el propio botón).
    PrimaryNeonButton(
        text = "INGRESAR",
        icon = Icons.AutoMirrored.Filled.Login,
        onClick = {
            // Validación local: campos vacíos se reportan sin llamar a la API.
            if (email.isBlank() || password.isBlank()) {
                formError = "Ingresa correo y contraseña"
            } else {
                formError = null
                viewModel.login(email, password, deviceId.ifBlank { null })
            }
        },
        loading = isLoading,
        modifier = Modifier.fillMaxWidth()
    )

    // Errores (validación local o error de la API) en cajita semántica.
    val errorMsg = formError ?: (uiState as? LoginUiState.Error)?.message
    if (errorMsg != null) {
        Spacer(modifier = Modifier.height(12.dp))
        ErrorBox(texto = errorMsg)
    }

    // Botón de ingreso con huella: lanza el prompt biométrico del sistema y, al
    // verificar el dedo, descifra las credenciales guardadas e inicia sesión.
    Spacer(modifier = Modifier.height(12.dp))
    GlowOutlinedButton(
        text = "INGRESAR CON HUELLA",
        icon = Icons.Default.Fingerprint,
        onClick = onBiometricLogin,
        enabled = !huellaLoading,
        modifier = Modifier.fillMaxWidth()
    )
    if (huellaError != null) {
        Spacer(modifier = Modifier.height(12.dp))
        ErrorBox(texto = huellaError)
    }

    // Éxito: la navegación y el registro de huella se gestionan en LoginScreen.

    Spacer(modifier = Modifier.height(24.dp))

    TextButton(onClick = onRegisterClick) {
        Text(
            text = buildAnnotatedString {
                // Primera parte: Texto normal en gris
                withStyle(style = SpanStyle(color = colors.textSecondary)) {
                    append("¿No estás registrado? ")
                }
                // Segunda parte: Texto verde sólido y en negrita (Sin brillo)
                withStyle(style = SpanStyle(color = verdeMarca(), fontWeight = FontWeight.Bold)) {
                    append("¡Regístrate aquí!")
                }
            },
            fontSize = 14.sp
        )
    }

    TextButton(onClick = onRecoveryClick) {
        Text(
            text = buildAnnotatedString {
                // Primera parte: Texto normal en gris
                withStyle(style = SpanStyle(color = colors.textSecondary)) {
                    append("¿Olvidaste tu contraseña? ")
                }
                // Segunda parte: Texto verde sólido y en negrita (Sin brillo)
                withStyle(style = SpanStyle(color = verdeMarca(), fontWeight = FontWeight.Bold)) {
                    append("Recuperar")
                }
            },
            fontSize = 14.sp
        )
    }
}
