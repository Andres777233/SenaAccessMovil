package com.example.sennaccess

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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.R
import com.example.sennaccess.data.AuthRepository
import com.example.sennaccess.data.RegisterRequest
import com.example.sennaccess.ui.BiometricAuth
import com.example.sennaccess.ui.ios.AuthBackdrop
import com.example.sennaccess.ui.ios.AuthField
import com.example.sennaccess.ui.ios.AuthLogo
import com.example.sennaccess.ui.ios.ErrorBox
import com.example.sennaccess.ui.ios.GlowOutlinedButton
import com.example.sennaccess.ui.ios.IosGlassCard
import com.example.sennaccess.ui.ios.PrimaryNeonButton
import com.example.sennaccess.ui.ios.ThemeToggleButton
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import com.example.sennaccess.ui.theme.verdeMarca
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun RegisterScreen(onBackToLogin: () -> Unit, isDark: Boolean = true, onToggleTheme: () -> Unit = {}) {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var documentoTipo by remember { mutableStateOf("CC") }
    var identification by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var lastname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var courseNumber by remember { mutableStateOf("") }
    var program by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var enviando by remember { mutableStateOf(false) }
    var errorMensaje by remember { mutableStateOf<String?>(null) }
    var showConfirm by remember { mutableStateOf(false) }

    AuthBackdrop(isDark = isDark) {
        ThemeToggleButton(
            isDark = isDark,
            onToggleTheme = onToggleTheme,
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IosGlassCard {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AuthLogo(modifier = Modifier.size(90.dp).padding(bottom = 12.dp))
                    Text(
                        text = "Sena Access",
                        fontSize = 28.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Crea tu cuenta institucional",
                        fontSize = 14.sp,
                        color = verdeMarca()
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    // --- SECCIÓN 1: INFORMACIÓN PERSONAL ---
                    SectionHeader(icon = Icons.Default.Person, title = "Información Personal")

                    var docDropdownAbierto by remember { mutableStateOf(false) }
                    val tiposDoc = listOf(
                        "CC" to "Cédula de Ciudadanía",
                        "CE" to "Cédula de Extranjería",
                        "TI" to "Tarjeta de Identidad",
                        "PAS" to "Pasaporte"
                    )
                    val docSeleccionado = tiposDoc.firstOrNull { it.first == documentoTipo } ?: tiposDoc.first()
                    @OptIn(ExperimentalMaterial3Api::class)
                    ExposedDropdownMenuBox(
                        expanded = docDropdownAbierto,
                        onExpandedChange = { docDropdownAbierto = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = "${docSeleccionado.first}: ${docSeleccionado.second}",
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            label = { Text("Tipo de Documento", color = colors.textSecondary, fontSize = 14.sp) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = docDropdownAbierto)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = verdeMarca(), unfocusedBorderColor = colors.divider,
                                focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
                                focusedContainerColor = colors.surfaceVariant.copy(alpha = 0.5f), unfocusedContainerColor = colors.surfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = docDropdownAbierto,
                            onDismissRequest = { docDropdownAbierto = false }
                        ) {
                            tiposDoc.forEach { (codigo, significado) ->
                                DropdownMenuItem(
                                    text = { Text("$codigo: $significado", color = colors.textPrimary, fontSize = 14.sp) },
                                    onClick = {
                                        documentoTipo = codigo
                                        docDropdownAbierto = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    AuthField(
                        value = identification,
                        onValueChange = { identification = it },
                        label = "Número de Identificación",
                        keyboardType = KeyboardType.Number
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AuthField(
                        value = telefono,
                        onValueChange = { telefono = it },
                        label = "Teléfono de contacto (opcional)",
                        keyboardType = KeyboardType.Phone
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AuthField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Nombres"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AuthField(
                        value = lastname,
                        onValueChange = { lastname = it },
                        label = "Apellidos"
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- SECCIÓN 2: FORMACIÓN ACADÉMICA ---
                    SectionHeader(icon = Icons.Default.School, title = "Formación Académica")
                    AuthField(
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
                    AuthField(
                        value = courseNumber,
                        onValueChange = { courseNumber = it },
                        label = "Número de Ficha",
                        keyboardType = KeyboardType.Number
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AuthField(
                        value = program,
                        onValueChange = { program = it },
                        label = "Programa de Formación"
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- SECCIÓN 3: SEGURIDAD ---
                    SectionHeader(icon = Icons.Default.Lock, title = "Seguridad de la Cuenta")
                    AuthField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Crear Contraseña",
                        isPassword = true,
                        imeAction = ImeAction.Next
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AuthField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = "Confirmar Contraseña",
                        isPassword = true,
                        imeAction = ImeAction.Done
                    )

                    if (errorMensaje != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        ErrorBox(texto = errorMensaje!!)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // BOTONES
                    PrimaryNeonButton(
                        text = "REGISTRARSE",
                        icon = Icons.Default.HowToReg,
                        loading = enviando,
                        onClick = {
                            if (enviando) return@PrimaryNeonButton
                            if (password != confirmPassword) {
                                errorMensaje = "Las contraseñas no coinciden."
                                return@PrimaryNeonButton
                            }
                            if (password.length < 8) {
                                errorMensaje = "La contraseña debe tener mínimo 8 caracteres."
                                return@PrimaryNeonButton
                            }
                            val correoLimpio = email.trim().lowercase()
                            if (correoLimpio.isBlank()) {
                                errorMensaje = "Escribe tu correo electrónico."
                                return@PrimaryNeonButton
                            }
                            val dominiosOk = listOf("@gmail.com", "@hotmail.com", "@outlook.com", "@soy.sena.edu.co")
                            if (dominiosOk.none { correoLimpio.endsWith(it) }) {
                                errorMensaje = "El correo debe ser @gmail.com, @hotmail.com, @outlook.com o @soy.sena.edu.co."
                                return@PrimaryNeonButton
                            }
                            val ficha = courseNumber.trim().toIntOrNull()
                            if (ficha == null) {
                                errorMensaje = "El número de ficha debe ser un valor numérico."
                                return@PrimaryNeonButton
                            }
                            if (!BiometricAuth.isAvailable(context, forCrypto = true)) {
                                errorMensaje = "Tu dispositivo no tiene huella fuerte registrada. Regístrala en Ajustes para poder crear tu cuenta."
                                return@PrimaryNeonButton
                            }
                            val activity = context as? FragmentActivity
                            if (activity == null) {
                                errorMensaje = "No se pudo iniciar la verificación biométrica."
                                return@PrimaryNeonButton
                            }
                            errorMensaje = null
                            BiometricAuth.authenticate(
                                activity,
                                "Verificación biométrica",
                                "Confirma tu identidad con tu huella para completar el registro",
                                onSuccess = {
                                    enviando = true
                                    scope.launch {
                                        try {
                                            AuthRepository().register(
                                                RegisterRequest(
                                                    user_identification = identification.trim(),
                                                    user_name = name.trim(),
                                                    user_lastname = lastname.trim(),
                                                    user_email = email.trim().lowercase(),
                                                    user_password = password,
                                                    user_password_confirmation = confirmPassword,
                                                    user_coursenumber = ficha,
                                                    user_program = program.trim(),
                                                    user_documento_tipo = documentoTipo,
                                                    user_telefono = telefono.trim().ifBlank { null }
                                                )
                                            )
                                            enviando = false
                                            showConfirm = true
                                        } catch (e: retrofit2.HttpException) {
                                            enviando = false
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
                                            errorMensaje = "No se pudo conectar al servidor."
                                        }
                                    }
                                },
                                onError = { msg ->
                                    errorMensaje = msg
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    GlowOutlinedButton(
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
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Cuenta creada", tint = SenaGreen, modifier = Modifier.size(40.dp)) },
            title = {
                Text("Solicitud enviada", color = colors.textPrimary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
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
                    colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(28.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text("Okey", fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold)
                }
            }
        )
    }
}

// Encabezado de sección: icono + título en mayúsculas para agrupar los campos.
@Composable
fun SectionHeader(icon: ImageVector, title: String) {
    val colors = LocalAppColors.current
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