package com.example.sennaccess.admin

// Formulario para actualizar los datos de un usuario existente (rol ADMIN).
// Recibe el objeto UsuarioApi obtenido desde GET /admin/users y prellena
// los campos editables; al enviar muestra confirmación y navega de vuelta.

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.data.Role
import com.example.sennaccess.data.SessionManager
import com.example.sennaccess.data.UserRequest
import com.example.sennaccess.data.UsuarioApi
import com.example.sennaccess.data.UsuarioRepository
import com.example.sennaccess.ui.CargaUiState
import com.example.sennaccess.ui.EstadoContenido
import com.example.sennaccess.ui.theme.ErrorRed
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import com.example.sennaccess.ui.ios.GlassCornerRadius
import com.example.sennaccess.ui.ios.IosCollapsibleHeader
import com.example.sennaccess.ui.ios.glassSurface
import com.example.sennaccess.ui.ios.pressScale
import kotlinx.coroutines.launch

/**
 * Formulario para actualizar usuario del ADMINISTRADOR (sub-pantalla).
 * Prellena los campos con los datos reales del GET /admin/users.
 */
@Composable
fun ActualizarUsuarioContent(
    usuario: UsuarioApi,
    roles: CargaUiState<List<Role>>,
    onReintentarRoles: () -> Unit,
    onNavigate: (AdminScreen) -> Unit
) {
    // Estado local de los campos editables, inicializados desde el usuario del GET.
    var nombres by remember { mutableStateOf(usuario.user_name ?: "") }
    var apellidos by remember { mutableStateOf(usuario.user_lastname ?: "") }
    var correo by remember { mutableStateOf(usuario.user_email ?: "") }
    var numeroId by remember { mutableStateOf(usuario.user_identification ?: "") }
    var ficha by remember { mutableStateOf(usuario.user_coursenumber?.toString() ?: "") }
    var programa by remember { mutableStateOf(usuario.user_program ?: "") }
    var documentoTipo by remember { mutableStateOf(usuario.user_documento_tipo ?: "CC") }
    var telefono by remember { mutableStateOf(usuario.user_telefono ?: "") }
    var contrasena by remember { mutableStateOf("") }
    var rolSeleccionado by remember { mutableStateOf(usuario.role) }
    var dropdownAbierto by remember { mutableStateOf(false) }
    var actualizado by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }
    var errorMsj by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val colors = LocalAppColors.current
    val scrollState = rememberScrollState()

    // Envía los cambios a PUT /admin/users/{id}; en éxito muestra el overlay.
    fun guardarCambios() {
        val rolId = rolSeleccionado?.id_rol ?: run { errorMsj = "Seleccione un rol"; return }
        guardando = true
        errorMsj = null
        scope.launch {
            try {
                val token = SessionManager.token ?: return@launch
                UsuarioRepository().actualizarUsuario(
                    token = token,
                    id = usuario.id_usuario ?: return@launch,
                    body = UserRequest(
                        user_identification = numeroId.trim(),
                        user_name = nombres.trim(),
                        user_lastname = apellidos.trim(),
                        user_email = correo.trim(),
                        user_password = contrasena.ifBlank { null },
                        user_coursenumber = ficha.trim().toIntOrNull(),
                        user_program = programa.trim(),
                        user_documento_tipo = documentoTipo,
                        user_telefono = telefono.trim().ifBlank { null },
                        fk_id_rol = rolId
                    )
                )
                guardando = false
                actualizado = true
            } catch (e: Exception) {
                guardando = false
                errorMsj = e.message ?: "No se pudo actualizar el usuario"
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Encabezado de la pantalla de edicion.
            IosCollapsibleHeader(
                title = "Actualizar Usuario",
                subtitle = "Edicion de datos del usuario",
                scrollOffset = scrollState.value.toFloat()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Contenedor de vidrio; encabeza con el nombre del usuario en edicion.
            AdminGlassContainer {
                Text(
                    "Actualizar Datos De ${usuario.nombreCompleto}",
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                // Dos columnas de campos: izquierda (nombres, correo, ficha) y
                // derecha (apellidos, ID, programa).
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(value = nombres, onValueChange = { nombres = it }, label = { Text("Nombres") }, modifier = Modifier.fillMaxWidth(), colors = campoColors())
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(value = correo, onValueChange = { correo = it }, label = { Text("Correo Electronico") }, modifier = Modifier.fillMaxWidth(), colors = campoColors())
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(value = ficha, onValueChange = { ficha = it }, label = { Text("Ficha") }, modifier = Modifier.fillMaxWidth(), colors = campoColors())
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(value = apellidos, onValueChange = { apellidos = it }, label = { Text("Apellidos") }, modifier = Modifier.fillMaxWidth(), colors = campoColors())
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(value = numeroId, onValueChange = { numeroId = it }, label = { Text("Numero De Identificacion") }, modifier = Modifier.fillMaxWidth(), colors = campoColors())
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(value = programa, onValueChange = { programa = it }, label = { Text("Programa de Formacion") }, modifier = Modifier.fillMaxWidth(), colors = campoColors())
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Tipo de documento con su significado entre paréntesis (dropdown).
                var docDropdownAbierto by remember { mutableStateOf(false) }
                val tiposDoc = listOf(
                    "CC" to "Cédula de Ciudadanía",
                    "CE" to "Cédula de Extranjería",
                    "TI" to "Tarjeta de Identidad",
                    "PAS" to "Pasaporte"
                )
                val docSeleccionado = tiposDoc.firstOrNull { it.first == documentoTipo } ?: tiposDoc.first()
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = "${docSeleccionado.first}: ${docSeleccionado.second}",
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = { Text("Tipo de Documento") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = campoColors(),
                        trailingIcon = {
                            IconButton(onClick = { docDropdownAbierto = true }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = SenaGreen)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = docDropdownAbierto,
                        onDismissRequest = { docDropdownAbierto = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tiposDoc.forEach { (codigo, significado) ->
                            DropdownMenuItem(
                                text = { Text("$codigo: $significado", color = colors.textPrimary) },
                                onClick = {
                                    documentoTipo = codigo
                                    docDropdownAbierto = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = telefono, onValueChange = { telefono = it }, label = { Text("Telefono de contacto (opcional)") }, modifier = Modifier.fillMaxWidth(), colors = campoColors())
                Spacer(modifier = Modifier.height(12.dp))
                // Dropdown de rol prellenado con el rol actual del usuario.
                EstadoContenido(estado = roles, onReintentar = onReintentarRoles) { listaRoles ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = rolSeleccionado?.rol_name ?: "Seleccionar rol",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Rol") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = campoColors(),
                            trailingIcon = {
                                IconButton(onClick = { dropdownAbierto = true }) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = SenaGreen)
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = dropdownAbierto,
                            onDismissRequest = { dropdownAbierto = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listaRoles.forEach { rol ->
                                DropdownMenuItem(
                                    text = { Text(rol.rol_name ?: "Rol", color = colors.textPrimary) },
                                    onClick = {
                                        rolSeleccionado = rol
                                        dropdownAbierto = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Campo opcional: solo se envía si se escribe una contraseña nueva.
                OutlinedTextField(value = contrasena, onValueChange = { contrasena = it }, label = { Text("Nueva contraseña (opcional)") }, modifier = Modifier.fillMaxWidth(), colors = campoColors())
                Spacer(modifier = Modifier.height(32.dp))
                // Mensaje de error de validación o red, si lo hay.
                if (errorMsj != null) {
                    Text(errorMsj!!, color = ErrorRed, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                }
                // Acciones: cancelar (vuelve al panel) y actualizar (envía a la API).
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { onNavigate(AdminScreen.PANEL) },
                        modifier = Modifier.weight(1f).height(50.dp).pressScale(pressedScale = 0.97f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colors.textSecondary)
                    ) { Text("CANCELAR", color = colors.textSecondary, fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = { guardarCambios() },
                        enabled = !guardando,
                        modifier = Modifier.weight(1f).height(50.dp).pressScale(pressedScale = 0.97f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = colors.textPrimary)
                    ) { Text(if (guardando) "GUARDANDO..." else "ACTUALIZAR", fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Tras actualizar, overlay de exito sobre el formulario.
        if (actualizado) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .glassSurface(cornerRadius = GlassCornerRadius)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = SenaGreen, modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("¡Actualizacion Exitosa!", color = SenaGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Los datos de ${usuario.nombreCompleto} han sido actualizados correctamente.",
                        color = colors.textPrimary, fontSize = 18.sp, textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { onNavigate(AdminScreen.PANEL) },
                        modifier = Modifier.fillMaxWidth().height(50.dp).pressScale(pressedScale = 0.97f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = colors.textPrimary)
                    ) { Text("Volver al panel", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                }
            }
        }
    }
}

// Paleta de colores comun de los campos del formulario de actualizacion.
@Composable
private fun campoColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SenaGreen, unfocusedBorderColor = LocalAppColors.current.textSecondary,
    focusedLabelColor = SenaGreen, unfocusedLabelColor = LocalAppColors.current.textSecondary,
    cursorColor = SenaGreen, focusedTextColor = LocalAppColors.current.textPrimary, unfocusedTextColor = LocalAppColors.current.textPrimary
)
