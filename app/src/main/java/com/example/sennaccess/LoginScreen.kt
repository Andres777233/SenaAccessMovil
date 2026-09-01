// Pantalla de inicio de sesión: permite autenticarse con correo y contraseña
// y navegar al registro o a la recuperación de contraseña.
// Además ofrece ingreso con huella real (biometría local): el sistema pide el
// dedo y solo entonces se descifran las credenciales guardadas en el dispositivo
// (HuellaCredentialStore) para hacer el login contra el backend.
// Al autenticarse devuelve el rol al MainActivity para redirigir al dashboard.
// Paquete donde está este archivo
package com.example.sennaccess.ui.theme

// Importamos herramientas que vamos a usar en la interfaz
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import com.example.sennaccess.R
import com.example.sennaccess.data.HuellaCredentialStore
import com.example.sennaccess.data.SessionManager
import com.example.sennaccess.ui.BiometricAuth
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.LoginViewModel
import com.example.sennaccess.ui.LoginUiState
import com.example.sennaccess.ui.ios.GlowSpheres
import com.example.sennaccess.ui.ios.glassSurface
import com.example.sennaccess.ui.ios.GlassCornerRadiusLg
import com.example.sennaccess.ui.ios.pressScale
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode


// ESTA ES LA PANTALLA PRINCIPAL DEL LOGIN
@Composable
fun LoginScreen(

    // Funciones para navegar a otras pantallas
    onNavigateToRegister: () -> Unit,
    onNavigateToRecovery: () -> Unit,
    onLoginSuccess: (String) -> Unit,
    isDark: Boolean = true,
    onToggleTheme: () -> Unit = {},
    viewModel: LoginViewModel = viewModel()
) {
    val colors = LocalAppColors.current
    // Estado del ViewModel observado en la UI: maneja Loading / Error / Success.
    val uiState by viewModel.uiState.collectAsState()

    // Contexto para el diálogo biométrico del sistema y el almacén de credenciales.
    val context = LocalContext.current

    // Mensaje de error del botón INGRESAR CON HUELLA (biometría local).
    var huellaError by remember { mutableStateOf<String?>(null) }
    // Verdadero mientras el diálogo biométrico del sistema está en curso.
    var huellaOcupado by remember { mutableStateOf(false) }
    // Marca que el login en curso proviene de la huella: si el backend rechaza
    // las credenciales guardadas (contraseña cambiada), se borran y se avisa.
    var loginDesdeHuella by remember { mutableStateOf(false) }
    // Controla el diálogo de opt-in para registrar la huella tras un login manual.
    var askSaveBiometric by remember { mutableStateOf(false) }

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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
        IconButton(
            onClick = onToggleTheme,
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
        ) {
            Icon(
                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = if (isDark) "Modo claro" else "Modo oscuro",
                tint = colors.textPrimary
            )
        }

        // Tarjeta principal del login
        GlassCard {

            // Column sirve para poner elementos uno debajo del otro
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 32.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),

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
                                color = SenaGreen,
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
                    color = SenaGreen.copy(alpha = 0.8f)
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

                        // Ingreso con huella (biometría local): el sistema pide el dedo
                        // y solo entonces se descifran las credenciales guardadas para
                        // hacer el login normal contra el backend.
                        onBiometricLogin = {
                            if (!huellaOcupado) {
                                huellaError = null
                                when {
                                    !BiometricAuth.isAvailable(context) ->
                                        huellaError = "Tu dispositivo no tiene huella configurada. Regístrala en Ajustes del sistema."
                                    !HuellaCredentialStore.hayGuardada(context) ->
                                        huellaError = "Aún no tienes una huella registrada. Inicia sesión con tu contraseña y acepta registrarla."
                                    else -> {
                                        val cipher = HuellaCredentialStore.prepararDescifrado(context)
                                        val activity = context as? FragmentActivity
                                        if (cipher == null || activity == null) {
                                            huellaError = "Tu huella se desvinculó. Inicia sesión con tu contraseña y regístrala de nuevo."
                                        } else {
                                            huellaOcupado = true
                                            BiometricAuth.authenticate(
                                                activity = activity,
                                                title = "Ingresa con tu huella",
                                                subtitle = "Confirma tu identidad para entrar a SenaAccess",
                                                cryptoObject = BiometricPrompt.CryptoObject(cipher),
                                                onSuccess = { result ->
                                                    huellaOcupado = false
                                                    try {
                                                        val credenciales = HuellaCredentialStore.leer(
                                                            context,
                                                            result.cryptoObject!!.cipher!!
                                                        )
                                                        loginDesdeHuella = true
                                                        viewModel.login(credenciales.first, credenciales.second)
                                                    } catch (e: Exception) {
                                                        HuellaCredentialStore.borrar(context)
                                                        huellaError = "No se pudieron leer tus credenciales. Inicia sesión y registra tu huella de nuevo."
                                                    }
                                                },
                                                onError = { motivo ->
                                                    huellaOcupado = false
                                                    if (!motivo.contains("cancel", ignoreCase = true)) {
                                                        huellaError = motivo
                                                    }
                                                }
                                            )
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
                // el botón INGRESAR CON HUELLA la próxima vez.
                if (uiState is LoginUiState.Success) {
                    val res = (uiState as LoginUiState.Success).response
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
                                        val activity = context as? FragmentActivity
                                        if (activity == null) {
                                            viewModel.reset()
                                            onLoginSuccess(res.role ?: "")
                                            return@Button
                                        }
                                        try {
                                            val cipher = HuellaCredentialStore.prepararCifrado()
                                            huellaOcupado = true
                                            BiometricAuth.authenticate(
                                                activity = activity,
                                                title = "Registra tu huella",
                                                subtitle = "Toca el sensor para proteger tus credenciales",
                                                cryptoObject = BiometricPrompt.CryptoObject(cipher),
                                                onSuccess = { result ->
                                                    huellaOcupado = false
                                                    try {
                                                        HuellaCredentialStore.guardar(
                                                            context,
                                                            result.cryptoObject!!.cipher!!,
                                                            res.user?.user_email ?: email.trim(),
                                                            password
                                                        )
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
                                                }
                                            )
                                        } catch (e: Throwable) {
                                            huellaOcupado = false
                                            viewModel.reset()
                                            onLoginSuccess(res.role ?: "")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black),
                                    shape = RoundedCornerShape(12.dp),
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
                    }
                }
            }
            }
        }
        }




// ESTA FUNCIÓN CREA UNA TARJETA CON ESTILO iOS (VIDRIO ESMERILADO)
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            // ancho de la tarjeta
            .fillMaxWidth(0.95f)
            // espacio vertical
            .padding(vertical = 20.dp)
            // vidrio esmerilado iOS (blur real en API31+, translúcido abajo)
            .glassSurface(cornerRadius = GlassCornerRadiusLg, elevated = true),
        content = content
    )
}


// BOTÓN PRINCIPAL VERDE
@Composable
fun PrimaryNeonButton(

    // texto del botón
    text: String,

    // icono opcional
    icon: ImageVector? = null,

    // acción al hacer clic
    onClick: () -> Unit,

    modifier: Modifier = Modifier
) {

    Button(
        onClick = onClick,

        modifier = modifier
            .pressScale(pressedScale = 0.97f)
            .shadow(
                15.dp,
                RoundedCornerShape(12.dp),
                spotColor = SenaGreen
            ),

        colors = ButtonDefaults.buttonColors(
            containerColor = SenaGreen,
            contentColor = Color.Black
        ),

        shape = RoundedCornerShape(12.dp),

        contentPadding = PaddingValues(vertical = 14.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            // Si el botón tiene icono
            if (icon != null) {

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))
            }

            // Texto del botón
            Text(
                text = text.uppercase(),
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
        }
    }
}


// BOTÓN CON BORDE VERDE
@Composable
fun GlowOutlinedButton(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.pressScale(pressedScale = 0.97f),

        // borde verde
        border = BorderStroke(
            2.dp,
            SenaGreen.copy(alpha = 0.5f)
        ),

        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = LocalAppColors.current.textPrimary
        ),

        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            // mostrar icono si existe
            if (icon != null) {

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = SenaGreen
                )

                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = text.uppercase(),
                letterSpacing = 2.sp
            )
        }
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

    // Ingreso con huella (biometría local): lanza el prompt del sistema y muestra estados.
    onBiometricLogin: () -> Unit,
    huellaLoading: Boolean = false,
    huellaError: String? = null
) {

    val colors = LocalAppColors.current

    // Variable para mostrar u ocultar contraseña
    var showPassword by remember {
        mutableStateOf(false)
    }

    // Mensaje de error local (campos vacíos)
    var formError by remember { mutableStateOf<String?>(null) }

    // Indicador de carga derivado del estado del ViewModel.
    val isLoading = uiState is LoginUiState.Loading

    // Título
    Text(
        "Iniciar Sesión",
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
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
        OutlinedTextField(
            value = email,
            onValueChange = {
                onEmailChange(it)
                expandirCorreos = true
            },
            label = { Text("Correo electrónico") },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expandirCorreos && sugerenciasCorreo.isNotEmpty(),
            onDismissRequest = { expandirCorreos = false }
        ) {
            sugerenciasCorreo.forEach { correo ->
                DropdownMenuItem(
                    text = { Text(correo) },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = SenaGreen) },
                    onClick = {
                        onEmailChange(correo)
                        expandirCorreos = false
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Campo contraseña
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,

        label = {
            Text("Contraseña")
        },

        modifier = Modifier.fillMaxWidth(),
        singleLine = true,

        // ocultar o mostrar contraseña
        visualTransformation =
            if (showPassword)
                VisualTransformation.None
            else
                PasswordVisualTransformation(),

        // icono ojito
        trailingIcon = {

            IconButton(
                onClick = {
                    showPassword = !showPassword
                }
            ) {

                Icon(
                    imageVector =
                        if (showPassword)
                            Icons.Default.VisibilityOff
                        else
                            Icons.Default.Visibility,

                    contentDescription = null
                )
            }
        }
    )

    Spacer(modifier = Modifier.height(32.dp))

    // Botón ingresar
    PrimaryNeonButton(
        text = if (isLoading) "CARGANDO..." else "INGRESAR",
        icon = Icons.AutoMirrored.Filled.Login,
        onClick = {
            // Validación local: campos vacíos se reportan sin llamar a la API.
            if (email.isBlank() || password.isBlank()) {
                formError = "Ingresa correo y contraseña"
            } else {
                formError = null
                viewModel.login(email, password)
            }
        },
        modifier = Modifier.fillMaxWidth()
    )

    // Indicador de progreso visible mientras se autentica contra el servidor.
    if (isLoading) {
        Spacer(modifier = Modifier.height(12.dp))
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            color = SenaGreen,
            strokeWidth = 3.dp
        )
    }

    // Errores (validación local o error de la API)
    val errorMsg = formError ?: (uiState as? LoginUiState.Error)?.message
    if (errorMsg != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = errorMsg,
            color = Color.Red,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }

    // Botón de ingreso con huella: lanza el prompt biométrico del sistema y, al
    // verificar el dedo, descifra las credenciales guardadas e inicia sesión.
    Spacer(modifier = Modifier.height(12.dp))
    GlowOutlinedButton(
        text = if (huellaLoading) "VERIFICANDO..." else "INGRESAR CON HUELLA",
        icon = Icons.Default.Fingerprint,
        onClick = onBiometricLogin,
        enabled = !huellaLoading,
        modifier = Modifier.fillMaxWidth()
    )
    if (huellaError != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = huellaError,
            color = Color.Red,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth()
        )
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
                withStyle(style = SpanStyle(color = SenaGreen, fontWeight = FontWeight.Bold)) {
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
                withStyle(style = SpanStyle(color = SenaGreen, fontWeight = FontWeight.Bold)) {
                    append("Recuperar")
                }
            },
            fontSize = 14.sp
        )
    }
}
