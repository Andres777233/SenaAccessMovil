package com.example.sennaccess.admin

// Perfil del ADMINISTRADOR (contenido de pestaña).
// Muestra los datos personales provenientes de la API con respaldo a datos de
// ejemplo, y permite editar nombre/correo y cambiar la contraseña.

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.data.UsuarioApi
import com.example.sennaccess.data.SessionManager
import com.example.sennaccess.data.UpdateProfileRequest
import com.example.sennaccess.data.UsuarioRepository
import com.example.sennaccess.ui.AvatarPerfil
import com.example.sennaccess.ui.CargaUiState
import com.example.sennaccess.ui.EstadoContenido
import com.example.sennaccess.ui.MiHuellaSection
import com.example.sennaccess.ui.theme.ErrorRed
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import com.example.sennaccess.ui.ios.GlassCornerRadius
import com.example.sennaccess.ui.ios.IosCollapsibleHeader
import com.example.sennaccess.ui.ios.glassSurface
import com.example.sennaccess.ui.ios.pressScale
import kotlinx.coroutines.launch

/**
 * Perfil del ADMINISTRADOR (contenido de pestaña).
 * Los datos (nombre/correo) llegan desde la API (GET /user) con respaldo a ejemplo.
 */
@Composable
fun PerfilContent(
    perfil: CargaUiState<UsuarioApi>,
    onBack: () -> Unit,
    onReintentar: () -> Unit
) {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    // Campos de nueva contraseña y confirmación (sección de seguridad).
    var passNueva by remember { mutableStateOf("") }
    var passConfirm by remember { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }
    var errorMensaje by remember { mutableStateOf<String?>(null) }
    var guardado by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Contenedor desplazable centrado con el contenido del perfil.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Encabezado de la pantalla de perfil.
        IosCollapsibleHeader(
            title = "Perfil",
            subtitle = "Información personal y seguridad",
            scrollOffset = scrollState.value.toFloat()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // EstadoContenido gestiona carga/error/éxito; los campos se inicializan
        // con los datos del usuario cargado desde la API.
        EstadoContenido(estado = perfil, onReintentar = onReintentar) { usuario ->
            // Estados locales de edición, inicializados con los datos del perfil.
            var nombre by remember(usuario) { mutableStateOf(usuario.nombreCompleto) }
            var email by remember(usuario) { mutableStateOf(usuario.user_email ?: "") }

            // Avatar circular con brillo verde que identifica al usuario.
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .shadow(15.dp, CircleShape, spotColor = SenaGreen, ambientColor = SenaGreen.copy(alpha = 0.3f))
            ) {
                AvatarPerfil(fotoPath = usuario.profile_photo_path, nombre = usuario.nombreCompleto, tamano = 100.dp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Nombre y rol mostrados bajo el avatar.
            Text(nombre, color = colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Administrador", color = SenaGreen, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(24.dp))
            // Sección de información personal y seguridad con campos editables
            // (nombre, correo, contraseñas) y botón de guardado.
            SeccionPerfil("Informacion Personal y Seguridad") {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), colors = perfilColors())
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Correo") }, modifier = Modifier.fillMaxWidth(), colors = perfilColors())
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = passNueva, onValueChange = { passNueva = it }, label = { Text("Nueva contrasena") }, modifier = Modifier.fillMaxWidth(), colors = perfilColors())
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = passConfirm, onValueChange = { passConfirm = it }, label = { Text("Confirmar contrasena") }, modifier = Modifier.fillMaxWidth(), colors = perfilColors())
                Spacer(modifier = Modifier.height(16.dp))
                if (errorMensaje != null) {
                    Text(errorMensaje!!, color = ErrorRed, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                }
                Button(
                    onClick = {
                        if (guardando) return@Button
                        if (passNueva != passConfirm) {
                            errorMensaje = "Las contraseñas no coinciden."
                            return@Button
                        }
                        if (passNueva.isNotBlank() && passNueva.length < 6) {
                            errorMensaje = "La contraseña debe tener al menos 6 caracteres."
                            return@Button
                        }
                        if (nombre.isBlank() || email.isBlank()) {
                            errorMensaje = "Nombre y correo son obligatorios."
                            return@Button
                        }
                        errorMensaje = null
                        guardando = true
                        scope.launch {
                            try {
                                UsuarioRepository().updateMyProfile(
                                    SessionManager.token!!,
                                    UpdateProfileRequest(
                                        user_identification = usuario.user_identification ?: "",
                                        user_name = nombre.trim(),
                                        user_lastname = usuario.user_lastname ?: "",
                                        user_email = email.trim(),
                                        user_password = passNueva.ifBlank { null },
                                        user_coursenumber = usuario.user_coursenumber ?: 0,
                                        user_program = usuario.user_program ?: ""
                                    )
                                )
                                guardando = false
                                passNueva = ""
                                passConfirm = ""
                                guardado = true
                            } catch (e: retrofit2.HttpException) {
                                guardando = false
                                errorMensaje = try {
                                    val json = org.json.JSONObject(e.response()?.errorBody()?.string() ?: "{}")
                                    json.optString("message").ifBlank { "Error ${e.code()}" }
                                } catch (_: Exception) { "Error al guardar: verifica los datos." }
                            } catch (e: Exception) {
                                guardando = false
                                errorMensaje = "No se pudo conectar al servidor."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(45.dp).pressScale(pressedScale = 0.97f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = colors.textPrimary)
                ) { Text(if (guardando) "Guardando..." else "Guardar cambios", fontWeight = FontWeight.Bold) }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Gestión de la huella dactilar local: registrar, ver estado y eliminar.
            MiHuellaSection()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Confirmación de guardado del perfil.
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
                    onClick = { guardado = false },
                    colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                ) { Text("Okey", fontWeight = FontWeight.ExtraBold) }
            }
        )
    }
}

// Contenedor de sección con fondo de vidrio y título propio.
@Composable
private fun SeccionPerfil(titulo: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = GlassCornerRadius)
            .padding(16.dp)
    ) {
        Text(titulo, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

// Paleta de colores común para los campos del perfil.
@Composable
private fun perfilColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SenaGreen, unfocusedBorderColor = LocalAppColors.current.textSecondary,
    focusedLabelColor = SenaGreen, unfocusedLabelColor = LocalAppColors.current.textSecondary,
    cursorColor = SenaGreen, focusedTextColor = LocalAppColors.current.textPrimary, unfocusedTextColor = LocalAppColors.current.textPrimary
)
