// Pantalla de inicio de sesión: permite autenticarse con correo y contraseña
// y navegar al registro o a la recuperación de contraseña.
// Además ofrece ingreso con huella real (biometría local): el sistema pide el
// dedo y solo entonces se descifran las credenciales guardadas en el dispositivo
// (AlmacenHuella) para hacer el login contra el backend.
// Al autenticarse devuelve el rol al ActividadPrincipal para redirigir al dashboard.
// Paquete donde está este archivo
package com.example.sennaccess.autenticacion.acceso

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
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
import com.example.sennaccess.biometria.AlmacenHuella
import com.example.sennaccess.datos.sesion.AlmacenDispositivo
import com.example.sennaccess.datos.sesion.GestorSesion
import com.example.sennaccess.biometria.AutenticacionBiometrica
import com.example.sennaccess.comun.campoVisible
import androidx.biometric.BiometricPrompt
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import com.example.sennaccess.autenticacion.acceso.AccesoViewModel
import com.example.sennaccess.autenticacion.acceso.EstadoAcceso
import com.example.sennaccess.autenticacion.invitado.FormularioInvitado
import com.example.sennaccess.autenticacion.invitado.CodigoQrInvitado
import com.example.sennaccess.datos.modelos.RespuestaQrInvitado
import com.example.sennaccess.autenticacion.verificacion.PantallaVerificacion
import com.example.sennaccess.comun.diseno.CampoAcceso
import com.example.sennaccess.comun.diseno.CajaError
import com.example.sennaccess.comun.diseno.superficiePlana
import com.example.sennaccess.comun.diseno.BotonBordeBrillante
import com.example.sennaccess.comun.diseno.EsferasBrillo
import com.example.sennaccess.comun.diseno.TarjetaVidrio
import com.example.sennaccess.comun.diseno.BotonPrimarioNeon
import com.example.sennaccess.comun.diseno.BotonCambiarTema
@Composable
fun PantallaAcceso(

    onNavigateToRegister: () -> Unit,
    onNavigateToRecovery: () -> Unit,
    onLoginSuccess: (String) -> Unit,
    isDark: Boolean = true,
    onToggleTheme: () -> Unit = {},
    retoInicialId: String? = null,
    onRetoConsumido: () -> Unit = {},
    viewModel: AccesoViewModel = viewModel()
) {
    val colors = ColoresAppLocal.current
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val deviceId = remember { AlmacenDispositivo.obtenerId(context) }

    var huellaError by remember { mutableStateOf<String?>(null) }
    var huellaOcupado by remember { mutableStateOf(false) }
    var loginDesdeHuella by remember { mutableStateOf(false) }
    var askSaveBiometric by remember { mutableStateOf(false) }
    var reto2FaById by remember { mutableStateOf<String?>(null) }
    var errorRol by remember { mutableStateOf<String?>(null) }

    var verInvitado by remember { mutableStateOf(false) }
    var guestQr by remember { mutableStateOf<RespuestaQrInvitado?>(null) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        val estado = uiState
        if (estado is EstadoAcceso.Success) {
            val res = estado.response
            if (res.two_factor_required == true && res.two_factor_id != null) {
                askSaveBiometric = false
                reto2FaById = res.two_factor_id
                viewModel.reset()
            }
        }
    }

    LaunchedEffect(retoInicialId) {
        if (!retoInicialId.isNullOrBlank() && reto2FaById == null) {
            askSaveBiometric = false
            viewModel.reset()
            reto2FaById = retoInicialId
            onRetoConsumido()
        }
    }

    LaunchedEffect(uiState) {
        val estado = uiState
        if (estado is EstadoAcceso.Success) {
            estado.response.user?.user_email?.takeIf { it.isNotBlank() }?.let {
                GestorSesion.guardarCorreoUsado(context, it.trim())
                // Guarda la clave del ingreso exitoso para autorrellenarla
                // cuando se elija ese correo en este mismo teléfono.
                if (password.isNotEmpty()) GestorSesion.guardarClavePara(context, it.trim(), password)
            }
        }
        // Fix bug huella: solo se desvincula si el backend rechazó las credenciales
        // (401). Un fallo de red NO borra la huella guardada.
        if (estado is EstadoAcceso.Error && loginDesdeHuella) {
            loginDesdeHuella = false
            val msg = estado.message.lowercase()
            val credencialesInvalidas = msg.contains("401") || msg.contains("no autorizado") ||
                msg.contains("unauthorized") || msg.contains("credencial") || msg.contains("contraseña")
            if (credencialesInvalidas) {
                AlmacenHuella.borrar(context)
                huellaError = "Tus credenciales guardadas dejaron de ser válidas. Ingresa con tu contraseña y registra tu huella otra vez."
            } else {
                huellaError = estado.message
            }
        } else if (estado !is EstadoAcceso.Loading) {
            loginDesdeHuella = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        EsferasBrillo(isDark = isDark)

        val qrGenerado = guestQr
        if (qrGenerado != null) {
            CodigoQrInvitado(
                invitado = qrGenerado,
                onVolver = {
                    guestQr = null
                    verInvitado = true
                }
            )
        } else if (verInvitado) {
            FormularioInvitado(
                onVolver = {
                    verInvitado = false
                    guestQr = null
                },
                onQrGenerado = { res ->
                    verInvitado = false
                    guestQr = res
                }
            )
        } else if (reto2FaById == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
        BotonCambiarTema(
            isDark = isDark,
            onToggleTheme = onToggleTheme,
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
        )

        TarjetaVidrio(modifier = Modifier.fillMaxWidth(0.95f).padding(vertical = 20.dp)) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 28.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_sena),
                    contentDescription = "Logo SENA",
                    modifier = Modifier
                        .size(76.dp)
                        .padding(bottom = 10.dp)
                )
                Text(
                    text = buildAnnotatedString {
                        append("Sena ")
                        withStyle(
                            style = SpanStyle(
                                color = verdeMarca(),
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append("Access")
                        }
                    },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(VerdeSena.copy(alpha = 0.14f))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "ACCESO CCyS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.6.sp,
                        color = verdeMarca()
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                FormularioAccesoNormal(
                    email = email,
                    password = password,

                    onEmailChange = {
                        email = it
                        // Si se completa un correo conocido, autorrellena su clave.
                        val clave = GestorSesion.obtenerClavePara(context, it)
                        if (!clave.isNullOrEmpty() && password.isEmpty()) password = clave
                    },

                    onPasswordChange = {
                        password = it
                    },

                    onRegisterClick = onNavigateToRegister,
                    onRecoveryClick = onNavigateToRecovery,
                    viewModel = viewModel,
                    uiState = uiState,
                    deviceId = deviceId,

                        onBiometricLogin = {
                            if (!huellaOcupado) {
                                huellaError = null
                                errorRol = null
                                when {
                                    !AutenticacionBiometrica.isAvailable(context, forCrypto = true) ->
                                        huellaError = "Tu dispositivo no tiene huella configurada. Regístrala en Ajustes del sistema."
                                    !AlmacenHuella.hayGuardada(context) ->
                                        huellaError = "Aún no tienes una huella registrada. Inicia sesión con tu contraseña y acepta registrarla."
                                    else -> {
                                        val cipher = AlmacenHuella.prepararDescifrado(context)
                                        val activity = AutenticacionBiometrica.activityDe(context)
                                        if (cipher == null || activity == null) {
                                            huellaError = "Tu huella se desvinculó. Inicia sesión con tu contraseña y regístrala de nuevo."
                                        } else {
                                            huellaOcupado = true
                                            try {
                                            AutenticacionBiometrica.authenticate(
                                                activity = activity,
                                                title = "Ingresa con tu huella",
                                                subtitle = "Confirma tu identidad para entrar a SenaAccess",
                                                cryptoObject = BiometricPrompt.CryptoObject(cipher),
                                                onSuccess = { result ->
                                                    huellaOcupado = false
                                                    val seguro = result.cryptoObject?.cipher
                                                    if (seguro == null) {
                                                        huellaError = "No se pudo verificar tu huella. Inténtalo de nuevo."
                                                        return@authenticate
                                                    }
                                                    val credenciales = try {
                                                        AlmacenHuella.leer(context, seguro)
                                                    } catch (e: Exception) {
                                                        huellaError = "No se pudieron leer tus credenciales. Inténtalo de nuevo."
                                                        return@authenticate
                                                    }
                                                    loginDesdeHuella = true
                                                    viewModel.login(credenciales.first, credenciales.second, deviceId.ifBlank { null })
                                                },
                                                onError = { motivo ->
                                                    huellaOcupado = false
                                                    huellaError = motivo
                                                },
                                                onFailed = { aviso ->
                                                    huellaError = aviso
                                                },
                                                onCancel = { huellaOcupado = false }
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
                        huellaError = huellaError,
                        onEntrarInvitado = { verInvitado = true }
                    )

                if (uiState is EstadoAcceso.Success) {
                    val res = (uiState as EstadoAcceso.Success).response
                    val rolNormalizado = com.example.sennaccess.datos.sesion.RolSeguro.normalizar(res.role)
                    if (rolNormalizado == null) {
                        LaunchedEffect(res) {
                            errorRol = "Tu cuenta no tiene un rol válido. Contacta al administrador."
                            com.example.sennaccess.datos.sesion.GestorSesion.clear()
                            viewModel.reset()
                        }
                    } else {
                    var yaTieneHuella by remember(res) { mutableStateOf(AlmacenHuella.hayGuardada(context)) }
                    var navegando by remember(res) { mutableStateOf(false) }
                    // La huella pertenece a una cuenta concreta: si el login actual es de
                    // otro correo, se desvincula y se ofrece registrarla de nuevo.
                    val duenoHuella = remember(res) { AlmacenHuella.obtenerDueno(context) }
                    val correoSesion = (res.user?.user_email ?: email.trim()).trim()
                    val huellaEsDeEstaCuenta = !yaTieneHuella || duenoHuella.isNullOrBlank() ||
                        duenoHuella.equals(correoSesion, ignoreCase = true)

                    if (yaTieneHuella && huellaEsDeEstaCuenta) {
                        LaunchedEffect(res) {
                            if (!navegando) {
                                navegando = true
                                viewModel.reset()
                                onLoginSuccess(res.role ?: "")
                            }
                        }
                    } else if (!askSaveBiometric) {
                        LaunchedEffect(res) {
                            if (yaTieneHuella && !huellaEsDeEstaCuenta) {
                                AlmacenHuella.borrar(context)
                                AlmacenHuella.borrarLlave()
                                yaTieneHuella = false
                                huellaError = "Se desvinculó la huella de la cuenta anterior. Regístrala de nuevo para este usuario."
                            }
                            askSaveBiometric = true
                        }
                    } else {
                        AlertDialog(
                            onDismissRequest = {
                                askSaveBiometric = false
                                viewModel.reset()
                                onLoginSuccess(res.role ?: "")
                            },
                            containerColor = colors.cardBackground.copy(alpha = 0.98f),
                            shape = RoundedCornerShape(24.dp),
                            icon = { Icon(Icons.Default.Fingerprint, contentDescription = null, tint = VerdeSena, modifier = Modifier.size(40.dp)) },
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
                                        val activity = AutenticacionBiometrica.activityDe(context)
                                        if (activity == null) {
                                            viewModel.reset()
                                            onLoginSuccess(res.role ?: "")
                                            return@Button
                                        }
                                        try {
                                            val cipher = AlmacenHuella.prepararCifradoSeguro()
                                            huellaOcupado = true
                                            AutenticacionBiometrica.authenticate(
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
                                                            AlmacenHuella.guardar(
                                                                context,
                                                                seguro,
                                                                res.user?.user_email ?: email.trim(),
                                                                password
                                                            )
                                                        }
                                                    } catch (e: Exception) {
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
                                                },
                                                onCancel = {
                                                    huellaOcupado = false
                                                    viewModel.reset()
                                                    onLoginSuccess(res.role ?: "")
                                                }
                                            )
                                        } catch (e: Throwable) {
                                            huellaOcupado = false
                                            huellaError = "No se pudo preparar la huella. Inténtalo desde tu perfil."
                                            viewModel.reset()
                                            onLoginSuccess(res.role ?: "")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = Color.Black),
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
                    }
                    }
                }
                if (errorRol != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CajaError(texto = errorRol!!)
                }
                }
            }
            }
        }
        }
        reto2FaById?.let { challengeId ->
            PantallaVerificacion(
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioAccesoNormal(

    email: String,
    password: String,

    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,

    onRegisterClick: () -> Unit,
    onRecoveryClick: () -> Unit,
    viewModel: AccesoViewModel,
    uiState: EstadoAcceso,
    deviceId: String = "",

    onBiometricLogin: () -> Unit,
    huellaLoading: Boolean = false,
    huellaError: String? = null,

    onEntrarInvitado: () -> Unit = {}
) {

    val colors = ColoresAppLocal.current

    var formError by remember { mutableStateOf<String?>(null) }

    val isLoading = uiState is EstadoAcceso.Loading

    val contextHistorial = LocalContext.current
    var expandirCorreos by remember { mutableStateOf(false) }
    var correoEnfocado by remember { mutableStateOf(false) }
    val correosGuardados = remember { GestorSesion.obtenerCorreosUsados(contextHistorial) }
    val sugerenciasCorreo = remember(email, correosGuardados) {
        if (correosGuardados.isEmpty()) emptyList()
        else if (email.isBlank()) correosGuardados
        else correosGuardados.filter { it.contains(email, ignoreCase = true) && !it.equals(email, ignoreCase = true) }
    }
    ExposedDropdownMenuBox(
        expanded = expandirCorreos && sugerenciasCorreo.isNotEmpty() && correoEnfocado,
        onExpandedChange = { expandirCorreos = it }
    ) {
CampoAcceso(
                value = email,
                onValueChange = { nuevo ->
                    // Al borrar (texto más corto o vacío) se cierra el menú para
                    // no interrumpir letra por letra; al escribir se mantiene.
                    if (nuevo.length < email.length) expandirCorreos = false
                    onEmailChange(nuevo)
                },
                label = "Correo electrónico",
                modifier = Modifier
                    .menuAnchor()
                    .onFocusChanged { correoEnfocado = it.isFocused }
                    .clip(RoundedCornerShape(16.dp)),
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        ExposedDropdownMenu(
            expanded = expandirCorreos && sugerenciasCorreo.isNotEmpty() && correoEnfocado,
            onDismissRequest = { expandirCorreos = false },
            shape = RoundedCornerShape(16.dp)
        ) {
            sugerenciasCorreo.forEach { correo ->
                DropdownMenuItem(
                    text = { Text(correo) },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = verdeMarca()) },
                    onClick = {
                        onEmailChange(correo)
                        expandirCorreos = false
                        // Autorrelleno: si esa cuenta ya ingresó en este teléfono,
                        // se restaura su contraseña guardada al elegir el correo.
                        GestorSesion.obtenerClavePara(contextHistorial, correo)?.let { clave ->
                            if (clave.isNotEmpty()) onPasswordChange(clave)
                        }
                        // Esa cuenta ya estuvo aquí: si tiene huella registrada se
                        // entra con el dedo (descifra la clave guardada cifrada)
                        // sin escribir nada a mano. Sin huella, solo queda el
                        // correo y la contraseña se escribe una vez.
                        try {
                            if (AlmacenHuella.hayGuardada(contextHistorial) &&
                                AlmacenHuella.obtenerDueno(contextHistorial).equals(correo, ignoreCase = true)
                            ) {
                                onBiometricLogin()
                            }
                        } catch (_: Exception) { }
                    }
                )
            }
        }
    }
    // Al enfocar el campo con historial, se ofrecen las sugerencias una vez.
    LaunchedEffect(correoEnfocado) {
        if (correoEnfocado && sugerenciasCorreo.isNotEmpty()) expandirCorreos = true
    }

    Spacer(modifier = Modifier.height(16.dp))

    CampoAcceso(
        value = password,
        onValueChange = onPasswordChange,
        label = "Contraseña",
        keyboardType = KeyboardType.Password,
        imeAction = ImeAction.Done,
        isPassword = true
    )

    Spacer(modifier = Modifier.height(24.dp))

    BotonPrimarioNeon(
        text = "INGRESAR",
        icon = Icons.AutoMirrored.Filled.Login,
        onClick = {
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

    val errorMsg = formError ?: (uiState as? EstadoAcceso.Error)?.message
    if (errorMsg != null) {
        Spacer(modifier = Modifier.height(12.dp))
        CajaError(texto = errorMsg)
    }

    Spacer(modifier = Modifier.height(12.dp))
    BotonBordeBrillante(
        text = "INGRESAR CON HUELLA",
        icon = Icons.Default.Fingerprint,
        onClick = onBiometricLogin,
        enabled = !huellaLoading,
        modifier = Modifier.fillMaxWidth()
    )
    if (huellaError != null) {
        Spacer(modifier = Modifier.height(12.dp))
        CajaError(texto = huellaError)
    }

    Spacer(modifier = Modifier.height(20.dp))

    TextButton(onClick = onEntrarInvitado) {
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(color = colors.textSecondary)) {
                    append("¿Eres visitante? ")
                }
                withStyle(style = SpanStyle(color = verdeMarca(), fontWeight = FontWeight.Bold)) {
                    append("Entrar como invitado")
                }
            },
            fontSize = 14.sp
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    TextButton(onClick = onRegisterClick) {
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(color = colors.textSecondary)) {
                    append("¿No estás registrado? ")
                }
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
                withStyle(style = SpanStyle(color = colors.textSecondary)) {
                    append("¿Olvidaste tu contraseña? ")
                }
                withStyle(style = SpanStyle(color = verdeMarca(), fontWeight = FontWeight.Bold)) {
                    append("Recuperar")
                }
            },
            fontSize = 14.sp
        )
    }
}
