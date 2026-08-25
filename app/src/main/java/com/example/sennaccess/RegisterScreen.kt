package com.example.sennaccess.ui.theme

// Pantalla de registro de nuevas cuentas: captura datos personales, académicos y
// de seguridad organizados en secciones. Al pulsar REGISTRARSE exige primero una
// verificación biométrica (huella) y luego envía los datos al backend
// (POST /api/register); en caso de éxito muestra el aviso de solicitud enviada y
// regresa al login.
// Desde aquí se regresa al login con onBackToLogin.


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.fragment.app.FragmentActivity
import com.example.sennaccess.R
import com.example.sennaccess.data.AuthRepository
import com.example.sennaccess.data.RegisterRequest
import com.example.sennaccess.ui.BiometricAuth
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.ios.GlowSpheres
import com.example.sennaccess.ui.ios.glassSurface
import com.example.sennaccess.ui.ios.GlassCornerRadiusLg
import com.example.sennaccess.ui.ios.pressScale
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun RegisterScreen(onBackToLogin: () -> Unit, isDark: Boolean = true, onToggleTheme: () -> Unit = {}) {
    // Composable principal: formulario de registro organizado en secciones.
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    // Contexto de la actividad para lanzar el diálogo biométrico del sistema.
    val context = LocalContext.current
    // Variables locales solo para que la vista interactúe visualmente (no guarda nada)
    var identification by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var lastname by remember { mutableStateOf("") }

    var email by remember { mutableStateOf("") }
    var courseNumber by remember { mutableStateOf("") }
    var program by remember { mutableStateOf("") }

    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // Estado del envío al backend: en curso, error de validación o confirmación mostrada.
    var enviando by remember { mutableStateOf(false) }
    var errorMensaje by remember { mutableStateOf<String?>(null) }

    // Controla si se muestra el aviso de solicitud enviada al pulsar Registrarse.
    var showConfirm by remember { mutableStateOf(false) }

    // Contenedor raíz con el fondo del tema activo.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        IconButton(
            onClick = onToggleTheme,
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
        ) {
            Icon(
                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = if (isDark) "Modo claro" else "Modo oscuro",
                tint = colors.textPrimary
            )
        }

        // Patrón de fondo traslúcido
        Image(
            painter = rememberAsyncImagePainter("https://www.sena.edu.co/Style%20Library/alayout/images/pattern.png"),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().graphicsLayer(alpha = 0.12f),
            contentScale = ContentScale.Crop
        )

        // Luces ambientales detrás del vidrio
        GlowSpheres(isDark = isDark)

        // Centra la tarjeta de cristal en pantalla.
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Tarjeta de cristal con neón
            GlassCardRegister {
                // Columna desplazable: evita que el formulario se corte en pantallas pequeñas.
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()), // Scroll habilitado
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Cabecera
                    Image(
                        painter = painterResource(R.drawable.logo_sena),
                        contentDescription = "Logo SENA",
                        modifier = Modifier.size(90.dp).padding(bottom = 12.dp)
                    )
                    Text(
                        text = buildAnnotatedString {
                            append("Sena ")
                            withStyle(style = SpanStyle(color = SenaGreen, fontWeight = FontWeight.Bold)) { append("Access") }
                        },
                        fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary
                    )
                    Text("Crea tu cuenta institucional", fontSize = 14.sp, color = SenaGreen.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(32.dp))

                    // --- SECCIÓN 1: INFORMACIÓN PERSONAL ---
                    SectionHeader(icon = Icons.Default.Person, title = "Información Personal")
                    NeonTextField(value = identification, onValueChange = { identification = it }, label = "Número de Identificación")
                    Spacer(modifier = Modifier.height(12.dp))
                    NeonTextField(value = name, onValueChange = { name = it }, label = "Nombres")
                    Spacer(modifier = Modifier.height(12.dp))
                    NeonTextField(value = lastname, onValueChange = { lastname = it }, label = "Apellidos")

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- SECCIÓN 2: FORMACIÓN ACADÉMICA ---
                    SectionHeader(icon = Icons.Default.School, title = "Formación Académica")
                    NeonTextField(value = email, onValueChange = { email = it }, label = "Correo Electrónico Institucional")
                    Spacer(modifier = Modifier.height(12.dp))
                    NeonTextField(value = courseNumber, onValueChange = { courseNumber = it }, label = "Número de Ficha")
                    Spacer(modifier = Modifier.height(12.dp))
                    NeonTextField(value = program, onValueChange = { program = it }, label = "Programa de Formación")

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- SECCIÓN 3: SEGURIDAD ---
                    SectionHeader(icon = Icons.Default.Lock, title = "Seguridad de la Cuenta")
                    NeonTextField(
                        value = password, onValueChange = { password = it }, label = "Crear Contraseña",
                        isPassword = true, showPassword = showPassword, onTogglePassword = { showPassword = !showPassword }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    NeonTextField(
                        value = confirmPassword, onValueChange = { confirmPassword = it }, label = "Confirmar Contraseña",
                        isPassword = true, showPassword = showPassword, onTogglePassword = { showPassword = !showPassword }
                    )

                    // Error de validación (coincidencia de contraseñas o respuesta del backend).
                    if (errorMensaje != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMensaje!!,
                            color = ErrorRed,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // BOTONES
                    PrimaryNeonButtonRegister(
                        text = "REGISTRARSE",
                        icon = Icons.Default.HowToReg,
                        onClick = {
                            if (enviando) return@PrimaryNeonButtonRegister
                            // Validación en cliente: las contraseñas deben coincidir.
                            if (password != confirmPassword) {
                                errorMensaje = "Las contraseñas no coinciden."
                                return@PrimaryNeonButtonRegister
                            }
                            val ficha = courseNumber.trim().toIntOrNull()
                            if (ficha == null) {
                                errorMensaje = "El número de ficha debe ser un valor numérico."
                                return@PrimaryNeonButtonRegister
                            }
                            // El registro exige verificar la huella dactilar primero.
                            if (!BiometricAuth.isAvailable(context)) {
                                errorMensaje = "Tu dispositivo no tiene huella configurada. Regístrala en Ajustes para poder crear tu cuenta."
                                return@PrimaryNeonButtonRegister
                            }
                            val activity = context as? FragmentActivity
                            if (activity == null) {
                                errorMensaje = "No se pudo iniciar la verificación biométrica."
                                return@PrimaryNeonButtonRegister
                            }
                            errorMensaje = null
                            BiometricAuth.authenticate(
                                activity,
                                "Verificación biométrica",
                                "Confirma tu identidad con tu huella para completar el registro",
                                onSuccess = {
                                    // Huella verificada: recién aquí se envía el registro.
                                    enviando = true
                                    scope.launch {
                                        try {
                                            AuthRepository().register(
                                                RegisterRequest(
                                                    user_identification = identification.trim(),
                                                    user_name = name.trim(),
                                                    user_lastname = lastname.trim(),
                                                    user_email = email.trim(),
                                                    user_password = password,
                                                    user_password_confirmation = confirmPassword,
                                                    user_coursenumber = ficha,
                                                    user_program = program.trim()
                                                )
                                            )
                                            enviando = false
                                            showConfirm = true
                                        } catch (e: retrofit2.HttpException) {
                                            enviando = false
                                            errorMensaje = try {
                                                // El backend devuelve 422 con los errores por campo.
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
                    GlowOutlinedButtonRegister(text = "VOLVER AL LOGIN", icon = Icons.AutoMirrored.Filled.Login, onClick = onBackToLogin, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        // Aviso de solicitud enviada: se muestra al registrar correctamente y, con el
        // botón Okey, cierra el aviso y regresa al login.
        if (showConfirm) {
            AlertDialog(
                onDismissRequest = { showConfirm = false },
                containerColor = colors.cardBackground.copy(alpha = 0.98f),
                shape = RoundedCornerShape(24.dp),
                icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SenaGreen, modifier = Modifier.size(40.dp)) },
                title = {
                    Text("Solicitud enviada", color = colors.textPrimary, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "Tu solicitud de registro se ha enviado correctamente. Un administrador del SENA validará tu cuenta y recibirás un correo cuando esté activa.",
                        color = colors.textSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showConfirm = false; onBackToLogin() },
                        colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                    ) {
                        Text("Okey", fontWeight = FontWeight.ExtraBold)
                    }
                }
            )
        }
    }
}

// --- COMPONENTES VISUALES INTERNOS ---

// Encabezado de sección: icono + título en mayúsculas para agrupar los campos.
@Composable
fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = SenaGreen.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title.uppercase(), color = SenaGreen.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

// Campo de texto reutilizable con estilo SENA; soporta modo contraseña con el
// botón de visibilidad para alternar entre texto plano y oculto.
@Composable
fun NeonTextField(
    value: String, onValueChange: (String) -> Unit, label: String,
    isPassword: Boolean = false, showPassword: Boolean = false, onTogglePassword: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, color = colors.textSecondary, fontSize = 14.sp) },
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = {
            if (isPassword) {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null, tint = colors.textSecondary
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SenaGreen, unfocusedBorderColor = colors.divider,
            focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
            focusedContainerColor = colors.surfaceVariant.copy(alpha = 0.5f), unfocusedContainerColor = colors.surfaceVariant.copy(alpha = 0.5f)
        )
    )
}

// Tarjeta de cristal reutilizable para el formulario de registro.
@Composable
fun GlassCardRegister(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier.fillMaxWidth(0.95f).padding(vertical = 20.dp)
            .glassSurface(cornerRadius = GlassCornerRadiusLg, elevated = true),
        content = content
    )
}

// Botón principal (relleno verde) con icono opcional y efecto neón.
@Composable
fun PrimaryNeonButtonRegister(text: String, icon: ImageVector? = null, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.pressScale(pressedScale = 0.97f).shadow(15.dp, RoundedCornerShape(12.dp), spotColor = SenaGreen),
        colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black),
        shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = text, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
        }
    }
}

// Botón secundario con borde verde (acción de respaldo, p. ej. volver al login).
@Composable
fun GlowOutlinedButtonRegister(text: String, icon: ImageVector? = null, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick, modifier = modifier.pressScale(pressedScale = 0.97f),
        border = BorderStroke(2.dp, SenaGreen.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ThemeText),
        shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = SenaGreen)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = text, letterSpacing = 2.sp)
        }
    }
}