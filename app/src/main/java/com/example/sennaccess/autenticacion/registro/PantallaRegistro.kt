package com.example.sennaccess.autenticacion.registro

// Pantalla de registro de nuevas cuentas: captura datos personales, académicos y
// de seguridad organizados en secciones. Al pulsar REGISTRARSE exige primero una
// verificación biométrica (huella) y luego envía los datos al backend
// (POST /api/register); en caso de éxito muestra el aviso de solicitud enviada y
// regresa al login.
// Desde aquí se regresa al login con onBackToLogin.

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.R
import com.example.sennaccess.datos.repositorios.RepositorioAutenticacion
import com.example.sennaccess.datos.modelos.PeticionRegistro
import com.example.sennaccess.biometria.AutenticacionBiometrica
import com.example.sennaccess.comun.diseno.FondoAcceso
import com.example.sennaccess.comun.diseno.CampoAcceso
import com.example.sennaccess.comun.diseno.LogoAcceso
import com.example.sennaccess.comun.diseno.CajaError
import com.example.sennaccess.comun.diseno.BotonBordeBrillante
import com.example.sennaccess.comun.diseno.TarjetaVidrio
import com.example.sennaccess.comun.diseno.BotonPrimarioNeon
import com.example.sennaccess.comun.diseno.BotonCambiarTema
import com.example.sennaccess.comun.diseno.superficiePlana
import com.example.sennaccess.comun.diseno.DesplegableSena
import com.example.sennaccess.comun.Jornadas
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun PantallaRegistro(onBackToLogin: () -> Unit, isDark: Boolean = true, onToggleTheme: () -> Unit = {}) {
    val colors = ColoresAppLocal.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var documentoTipo by rememberSaveable { mutableStateOf("CC") }
    var identification by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var lastname by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var courseNumber by rememberSaveable { mutableStateOf("") }
    var program by rememberSaveable { mutableStateOf("") }
    var telefono by rememberSaveable { mutableStateOf("") }
    var jornada by rememberSaveable { mutableStateOf(Jornadas.TARDE) }
    var jornadaSabado by rememberSaveable { mutableStateOf(Jornadas.MANANA) }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    var enviando by remember { mutableStateOf(false) }
    var errorMensaje by remember { mutableStateOf<String?>(null) }
    var avisoSuave by remember { mutableStateOf<String?>(null) }
    var showConfirm by remember { mutableStateOf(false) }
    var promptBiometrico by remember { mutableStateOf(false) }

    FondoAcceso(isDark = isDark) {
        BotonCambiarTema(
            isDark = isDark,
            onToggleTheme = onToggleTheme,
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(top = 48.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LogoAcceso(modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CREAR CUENTA",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.8.sp),
                        color = verdeMarca(),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Únete a Sena Access",
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Solo aprendices • activación inmediata",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .superficiePlana(cornerRadius = 24.dp)
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {

                    // --- SECCIÓN 1: INFORMACIÓN PERSONAL ---
                    EncabezadoSeccion(icon = Icons.Default.Person, title = "Información Personal")

                    val tiposDoc = listOf(
                        "CC: Cédula de Ciudadanía",
                        "CE: Cédula de Extranjería",
                        "TI: Tarjeta de Identidad",
                        "PAS: Pasaporte"
                    )
                    val docCodigo = documentoTipo.substringBefore(":").trim().ifBlank { "CC" }
                    val docActual = tiposDoc.firstOrNull { it.startsWith(docCodigo) } ?: tiposDoc.first()
                    DesplegableSena(
                        valor = docActual,
                        opciones = tiposDoc,
                        onElegir = { documentoTipo = it.substringBefore(":").trim() },
                        label = "Tipo de Documento"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CampoAcceso(
                        value = identification,
                        onValueChange = { identification = it },
                        label = "Número de Identificación",
                        keyboardType = KeyboardType.Number
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CampoAcceso(
                        value = telefono,
                        onValueChange = { telefono = it },
                        label = "Teléfono de contacto (opcional)",
                        keyboardType = KeyboardType.Phone
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CampoAcceso(
                        value = name,
                        onValueChange = { name = it },
                        label = "Nombres"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CampoAcceso(
                        value = lastname,
                        onValueChange = { lastname = it },
                        label = "Apellidos"
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- SECCIÓN 2: FORMACIÓN ACADÉMICA ---
                    EncabezadoSeccion(icon = Icons.Default.School, title = "Formación Académica")
                    CampoAcceso(
                        value = email,
                        onValueChange = { email = it },
                        label = "Correo Electrónico",
                        keyboardType = KeyboardType.Email
                    )
                    Text(
                        text = "Usa @gmail.com, @hotmail.com, @outlook.com o @soy.sena.edu.co",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CampoAcceso(
                        value = courseNumber,
                        onValueChange = { courseNumber = it },
                        label = "Número de Ficha",
                        keyboardType = KeyboardType.Number
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CampoAcceso(
                        value = program,
                        onValueChange = { program = it },
                        label = "Programa de Formación"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DesplegableSena(
                        valor = jornada,
                        opciones = Jornadas.TODAS,
                        onElegir = { jornada = it },
                        label = "Jornada (lunes a viernes)"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DesplegableSena(
                        valor = jornadaSabado,
                        opciones = Jornadas.TODAS,
                        onElegir = { jornadaSabado = it },
                        label = "Jornada de los sábados"
                    )
                    Text(
                        text = "Si los sábados estudias en otra jornada, elígela aquí.",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- SECCIÓN 3: SEGURIDAD ---
                    EncabezadoSeccion(icon = Icons.Default.Lock, title = "Seguridad de la Cuenta")
                    CampoAcceso(
                        value = password,
                        onValueChange = { password = it },
                        label = "Crear Contraseña",
                        isPassword = true,
                        imeAction = ImeAction.Next
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CampoAcceso(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = "Confirmar Contraseña",
                        isPassword = true,
                        imeAction = ImeAction.Done
                    )

                    if (errorMensaje != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        CajaError(texto = errorMensaje!!)
                    }
                    if (avisoSuave != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = avisoSuave!!,
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    BotonPrimarioNeon(
                        text = "REGISTRARSE",
                        icon = Icons.Default.HowToReg,
                        loading = enviando,
                        onClick = {
                            if (enviando || promptBiometrico) return@BotonPrimarioNeon
                            if (password != confirmPassword) {
                                errorMensaje = "Las contraseñas no coinciden."
                                return@BotonPrimarioNeon
                            }
                            if (password.length < 8) {
                                errorMensaje = "La contraseña debe tener mínimo 8 caracteres."
                                return@BotonPrimarioNeon
                            }
                            if (identification.trim().isBlank()) {
                                errorMensaje = "Escribe tu número de identificación."
                                return@BotonPrimarioNeon
                            }
                            if (name.trim().isBlank() || lastname.trim().isBlank()) {
                                errorMensaje = "Escribe tus nombres y apellidos."
                                return@BotonPrimarioNeon
                            }
                            if (program.trim().isBlank()) {
                                errorMensaje = "Escribe tu programa de formación."
                                return@BotonPrimarioNeon
                            }
                            val correoLimpio = email.trim().lowercase()
                            if (correoLimpio.isBlank()) {
                                errorMensaje = "Escribe tu correo electrónico."
                                return@BotonPrimarioNeon
                            }
                            val dominiosOk = listOf("@gmail.com", "@hotmail.com", "@outlook.com", "@soy.sena.edu.co")
                            if (dominiosOk.none { correoLimpio.endsWith(it) }) {
                                errorMensaje = "El correo debe ser @gmail.com, @hotmail.com, @outlook.com o @soy.sena.edu.co."
                                return@BotonPrimarioNeon
                            }
                            val ficha = courseNumber.trim().toIntOrNull()
                            if (ficha == null) {
                                errorMensaje = "El número de ficha debe ser un valor numérico."
                                return@BotonPrimarioNeon
                            }
                            if (!AutenticacionBiometrica.isAvailable(context, forCrypto = true)) {
                                errorMensaje = "Tu dispositivo no tiene huella fuerte registrada. Regístrala en Ajustes para poder crear tu cuenta."
                                return@BotonPrimarioNeon
                            }
                            val activity = context as? FragmentActivity
                            if (activity == null) {
                                errorMensaje = "No se pudo iniciar la verificación biométrica."
                                return@BotonPrimarioNeon
                            }
                            errorMensaje = null
                            avisoSuave = null
                            promptBiometrico = true
                            AutenticacionBiometrica.authenticate(
                                activity,
                                "Verificación biométrica",
                                "Confirma tu identidad con tu huella para completar el registro",
                                onSuccess = {
                                    enviando = true
                                    scope.launch {
                                        try {
                                            RepositorioAutenticacion().register(
                                                PeticionRegistro(
                                                    user_identification = identification.trim(),
                                                    user_name = name.trim(),
                                                    user_lastname = lastname.trim(),
                                                    user_email = email.trim().lowercase(),
                                                    user_password = password,
                                                    user_password_confirmation = confirmPassword,
                                                    user_coursenumber = ficha,
                                                    user_program = program.trim(),
                                                    user_documento_tipo = documentoTipo,
                                                    user_telefono = telefono.trim().ifBlank { null },
                                                    user_jornada = jornada,
                                                    user_jornada_sabado = jornadaSabado
                                                )
                                            )
                                            enviando = false
                                            promptBiometrico = false
                                            showConfirm = true
                                        } catch (e: retrofit2.HttpException) {
                                            enviando = false
                                            promptBiometrico = false
                                            errorMensaje = try {
                                                val body = e.response()?.errorBody()?.string()
                                                val json = JSONObject(body ?: "{}")
                                                val errores = json.optJSONObject("errors")
                                                if (errores != null && errores.length() > 0) {
                                                    val primerCampo = errores.keys().next()
                                                    errores.optJSONArray(primerCampo)?.getString(0)
                                                        ?: errores.optString(primerCampo)
                                                } else {
                                                    json.optString("message").ifBlank { "Error ${e.code()}" }
                                                }
                                            } catch (_: Exception) {
                                                "Error al registrar: verifica los datos."
                                            }
                                        } catch (e: Exception) {
                                            enviando = false
                                            promptBiometrico = false
                                            errorMensaje = "No se pudo conectar al servidor."
                                        }
                                    }
                                },
                                onError = { msg ->
                                    promptBiometrico = false
                                    avisoSuave = null
                                    errorMensaje = msg
                                },
                                onCancel = {
                                    promptBiometrico = false
                                    errorMensaje = null
                                    avisoSuave = "Verificación cancelada. Tus datos siguen aquí, pulsa REGISTRARSE de nuevo cuando quieras."
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    BotonBordeBrillante(
                        text = "VOLVER AL LOGIN",
                        icon = Icons.AutoMirrored.Filled.Login,
                        onClick = onBackToLogin,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor = colors.cardBackground.copy(alpha = 0.98f),
            shape = RoundedCornerShape(28.dp),
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Cuenta creada", tint = VerdeSena, modifier = Modifier.size(40.dp)) },
            title = {
                Text("Cuenta creada", color = colors.textPrimary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            },
            text = {
                Text(
                    "¡Cuenta creada correctamente! Ya puedes iniciar sesión con tu correo y contraseña. Te llegó un correo con un enlace para verificar tu cuenta (revisa la bandeja de entrada y de correo no deseado).",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { showConfirm = false; onBackToLogin() },
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = Color.Black),
                    shape = RoundedCornerShape(28.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text("OK", fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold)
                }
            }
        )
    }
}

@Composable
fun EncabezadoSeccion(icon: ImageVector, title: String) {
    val colors = ColoresAppLocal.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = verdeMarca(),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            color = verdeMarca(),
            fontSize = 12.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}
