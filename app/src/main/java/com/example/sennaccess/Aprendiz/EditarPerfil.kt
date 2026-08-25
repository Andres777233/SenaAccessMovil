package com.example.sennaccess.aprendiz

// Vista de edición del perfil propio, compartida por Aprendiz e Instructor.
// Formulario de vidrio prellenado con los datos de la API (PUT /my-profile):
// identificación, nombres, apellidos, correo, ficha, programa y contraseña
// opcional (solo se cambia si viene llena). Al guardar refresca el perfil.

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sennaccess.data.SessionManager
import com.example.sennaccess.data.UpdateProfileRequest
import com.example.sennaccess.data.UsuarioApi
import com.example.sennaccess.data.UsuarioRepository
import com.example.sennaccess.ui.CargaUiState
import com.example.sennaccess.ui.EstadoContenido
import com.example.sennaccess.ui.ios.GlassCornerRadius
import com.example.sennaccess.ui.ios.IosCollapsibleHeader
import com.example.sennaccess.ui.ios.glassSurface
import com.example.sennaccess.ui.ios.pressScale
import com.example.sennaccess.ui.theme.ErrorRed
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

// Vista de edición de perfil: recibe el estado del perfil (para prellenar los
// campos) y los callbacks de retroceso y de perfil actualizado.
@Composable
fun EditarPerfilView(
    estado: CargaUiState<UsuarioApi>,
    onBack: () -> Unit,
    onGuardado: () -> Unit,
    onReintentar: () -> Unit
) {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Estados de los campos, iniciados con los datos del usuario cargado.
    var identificacion by remember { mutableStateOf("") }
    var nombres by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var ficha by remember { mutableStateOf("") }
    var programa by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // Estado de la operación de guardado.
    var guardando by remember { mutableStateOf(false) }
    var errorMensaje by remember { mutableStateOf<String?>(null) }
    var guardado by remember { mutableStateOf(false) }

    // Foto de perfil: uri recién elegida de la galería (prioriza) o la que ya tenía
    // el usuario en el servidor. El selector abre la galería con GetContent image/*.
    val contexto = LocalContext.current
    var fotoUri by remember { mutableStateOf<Uri?>(null) }
    val selectorFoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) fotoUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = colors.textPrimary) }
        }
        Spacer(modifier = Modifier.height(8.dp))

        EstadoContenido(estado = estado, onReintentar = onReintentar) { usuario ->
            // Los campos se rellenan la primera vez que llegan los datos del perfil.
            LaunchedEffect(usuario) {
                identificacion = usuario.user_identification ?: ""
                nombres = usuario.user_name ?: ""
                apellidos = usuario.user_lastname ?: ""
                correo = usuario.user_email ?: ""
                ficha = usuario.user_coursenumber?.toString() ?: ""
                programa = usuario.user_program ?: ""
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassSurface(cornerRadius = GlassCornerRadius)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar: muestra la foto elegida, si no la del servidor y si no el
                // icono por defecto. Toca la imagen para abrir la galería.
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(SenaGreen.copy(alpha = 0.15f))
                        .pressScale(pressedScale = 0.95f),
                    contentAlignment = Alignment.Center
                ) {
                    val urlServidor = SessionManager.fotoUrl(usuario.profile_photo_path)
                    when {
                        fotoUri != null -> AsyncImage(
                            model = fotoUri, contentDescription = null,
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                        )
                        urlServidor != null -> AsyncImage(
                            model = urlServidor, contentDescription = null,
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                        )
                        else -> Icon(Icons.Default.Person, null, tint = SenaGreen, modifier = Modifier.size(50.dp))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                androidx.compose.material3.TextButton(onClick = { selectorFoto.launch("image/*") }) {
                    Text("CAMBIAR FOTO", color = SenaGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(usuario.nombreCompleto, color = colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))

                campoPerfil(identificacion, { identificacion = it }, "Número de Identificación")
                Spacer(modifier = Modifier.height(12.dp))
                campoPerfil(nombres, { nombres = it }, "Nombres")
                Spacer(modifier = Modifier.height(12.dp))
                campoPerfil(apellidos, { apellidos = it }, "Apellidos")
                Spacer(modifier = Modifier.height(12.dp))
                campoPerfil(correo, { correo = it }, "Correo Electrónico")
                Spacer(modifier = Modifier.height(12.dp))
                campoPerfil(ficha, { ficha = it }, "Número de Ficha")
                Spacer(modifier = Modifier.height(12.dp))
                campoPerfil(programa, { programa = it }, "Programa de Formación")
                Spacer(modifier = Modifier.height(12.dp))

                // Contraseña nueva (opcional): si se deja vacía no se modifica.
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Nueva contraseña (opcional)", color = colors.textSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = colors.textSecondary
                            )
                        }
                    },
                    colors = campoPerfilColors()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirmar contraseña", color = colors.textSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = campoPerfilColors()
                )

                // Error de validación o de la API.
                if (errorMensaje != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(errorMensaje!!, color = ErrorRed, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botón guardar: valida, envía PUT /my-profile y avisa al terminar.
                Button(
                    onClick = {
                        if (guardando) return@Button
                        if (password != confirmPassword) {
                            errorMensaje = "Las contraseñas no coinciden."
                            return@Button
                        }
                        if (password.isNotBlank() && password.length < 6) {
                            errorMensaje = "La contraseña debe tener al menos 6 caracteres."
                            return@Button
                        }
                        val fichaNum = ficha.trim().toIntOrNull()
                        if (fichaNum == null) {
                            errorMensaje = "El número de ficha debe ser numérico."
                            return@Button
                        }
                        errorMensaje = null
                        guardando = true
                        scope.launch {
                            try {
                                // Con foto elegida se envía multipart (campo "image");
                                // sin foto se usa el PUT JSON de siempre.
                                val perfilActualizado = if (fotoUri != null) {
                                    val bytes = contexto.contentResolver.openInputStream(fotoUri!!)?.use { it.readBytes() }
                                    if (bytes == null) throw IllegalStateException("No se pudo leer la imagen.")
                                    val mime = contexto.contentResolver.getType(fotoUri!!) ?: "image/jpeg"
                                    val parteFoto = okhttp3.MultipartBody.Part.createFormData(
                                        "image", "perfil.jpg", bytes.toRequestBody(mime.toMediaType())
                                    )
                                    UsuarioRepository().actualizarConFoto(
                                        SessionManager.token!!,
                                        parteFoto,
                                        identificacion.trim(),
                                        nombres.trim(),
                                        apellidos.trim(),
                                        correo.trim(),
                                        fichaNum,
                                        programa.trim()
                                    )
                                } else {
                                    UsuarioRepository().updateMyProfile(
                                        SessionManager.token!!,
                                        UpdateProfileRequest(
                                            user_identification = identificacion.trim(),
                                            user_name = nombres.trim(),
                                            user_lastname = apellidos.trim(),
                                            user_email = correo.trim(),
                                            user_password = password.ifBlank { null },
                                            user_coursenumber = fichaNum,
                                            user_program = programa.trim()
                                        )
                                    )
                                }
                                SessionManager.savePhoto(perfilActualizado.profile_photo_path)
                                guardando = false
                                guardado = true
                            } catch (e: retrofit2.HttpException) {
                                guardando = false
                                errorMensaje = try {
                                    val json = org.json.JSONObject(e.response()?.errorBody()?.string() ?: "{}")
                                    val errores = json.optJSONObject("errors")
                                    if (errores != null && errores.length() > 0) {
                                        val primerCampo = errores.keys().next()
                                        errores.optJSONArray(primerCampo)?.getString(0)
                                            ?: errores.optString(primerCampo)
                                    } else {
                                        json.optString("message").ifBlank { "Error ${e.code()}" }
                                    }
                                } catch (_: Exception) {
                                    "Error al guardar: verifica los datos."
                                }
                            } catch (e: Exception) {
                                guardando = false
                                errorMensaje = "No se pudo conectar al servidor."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp).pressScale(pressedScale = 0.97f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black)
                ) { Text(if (guardando) "GUARDANDO..." else "GUARDAR CAMBIOS", fontWeight = FontWeight.Bold, fontSize = 15.sp) }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, colors.textSecondary)
                ) { Text("CANCELAR", color = colors.textSecondary, fontWeight = FontWeight.Bold) }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Confirmación de guardado: refresca el perfil y vuelve a la vista anterior.
    if (guardado) {
        AlertDialog(
            onDismissRequest = { guardado = false },
            containerColor = colors.cardBackground.copy(alpha = 0.98f),
            shape = RoundedCornerShape(24.dp),
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SenaGreen, modifier = Modifier.size(40.dp)) },
            title = { Text("Perfil actualizado", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Tus datos se guardaron correctamente.", color = colors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = { guardado = false; onGuardado() },
                    colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                ) { Text("Okey", fontWeight = FontWeight.ExtraBold) }
            }
        )
    }
}

// Campo de texto del formulario de perfil con el estilo de la app.
@Composable
private fun campoPerfil(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = LocalAppColors.current.textSecondary) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = campoPerfilColors()
    )
}

// Paleta de colores común para los campos del formulario (verde al enfocar).
@Composable
private fun campoPerfilColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SenaGreen,
    unfocusedBorderColor = LocalAppColors.current.textSecondary.copy(alpha = 0.5f),
    focusedLabelColor = SenaGreen,
    unfocusedLabelColor = LocalAppColors.current.textSecondary,
    cursorColor = SenaGreen,
    focusedTextColor = LocalAppColors.current.textPrimary,
    unfocusedTextColor = LocalAppColors.current.textPrimary
)