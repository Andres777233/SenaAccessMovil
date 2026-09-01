package com.example.sennaccess.admin

// Formulario para crear un usuario del ADMINISTRADOR (sub-pantalla).
// Captura los datos personales del nuevo usuario; el campo de rol se llena
// con el catalogo real desde GET /admin/roles. Al enviar, muestra confirmacion.

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
 * Formulario para crear usuario del ADMINISTRADOR (sub-pantalla).
 * El dropdown de roles consume GET /admin/roles a traves del AdminDashboardViewModel.
 */
@Composable
fun CrearUsuarioContent(
    roles: CargaUiState<List<Role>>,
    onReintentarRoles: () -> Unit,
    onNavigate: (AdminScreen) -> Unit
) {
    // Estado local de cada campo del formulario y bandera de exito (creado).
    var nombres by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var identificacion by remember { mutableStateOf("") }
    var programa by remember { mutableStateOf("") }
    var ficha by remember { mutableStateOf("") }
    var jornada by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var documentoTipo by remember { mutableStateOf("CC") }
    var telefono by remember { mutableStateOf("") }
    var rolSeleccionado by remember { mutableStateOf<Role?>(null) }
    var dropdownAbierto by remember { mutableStateOf(false) }
    var creado by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }
    var errorMsj by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val colors = LocalAppColors.current
    val scrollState = rememberScrollState()

    // Envía el formulario a POST /admin/users; en éxito muestra el overlay de creado.
    fun crearUsuario() {
        val rol = rolSeleccionado ?: run { errorMsj = "Seleccione un rol"; return }
        val rolId = rol.id_rol ?: run { errorMsj = "Rol inválido"; return }
        if (contrasena.isBlank()) { errorMsj = "La contraseña es obligatoria"; return }
        guardando = true
        errorMsj = null
        scope.launch {
            try {
                val token = SessionManager.token ?: return@launch
                UsuarioRepository().crearUsuario(
                    token = token,
                    body = UserRequest(
                        user_identification = identificacion.trim(),
                        user_name = nombres.trim(),
                        user_lastname = apellidos.trim(),
                        user_email = correo.trim(),
                        user_password = contrasena,
                        user_coursenumber = ficha.trim().toIntOrNull(),
                        user_program = programa.trim(),
                        user_documento_tipo = documentoTipo,
                        user_telefono = telefono.trim().ifBlank { null },
                        fk_id_rol = rolId
                    )
                )
                guardando = false
                creado = true
            } catch (e: Exception) {
                guardando = false
                errorMsj = e.message ?: "No se pudo crear el usuario"
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Encabezado de la pantalla de nuevo usuario.
            IosCollapsibleHeader(
                title = "Nuevo Usuario",
                subtitle = "Registrar un usuario en el sistema",
                scrollOffset = scrollState.value.toFloat()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Contenedor de vidrio con los campos organizados en filas de dos columnas.
            AdminGlassContainer {
                // Fila 1: nombres y apellidos.
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = nombres, onValueChange = { nombres = it }, label = { Text("Nombres") }, modifier = Modifier.weight(1f), colors = campoCrearColors())
                    OutlinedTextField(value = apellidos, onValueChange = { apellidos = it }, label = { Text("Apellidos") }, modifier = Modifier.weight(1f), colors = campoCrearColors())
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Fila 2: correo y numero de identificacion.
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = correo, onValueChange = { correo = it }, label = { Text("Correo Electronico") }, modifier = Modifier.weight(1f), colors = campoCrearColors())
                    OutlinedTextField(value = identificacion, onValueChange = { identificacion = it }, label = { Text("Numero de Identificacion") }, modifier = Modifier.weight(1f), colors = campoCrearColors())
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
                        colors = campoCrearColors(),
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
                OutlinedTextField(value = telefono, onValueChange = { telefono = it }, label = { Text("Telefono de contacto (opcional)") }, modifier = Modifier.fillMaxWidth(), colors = campoCrearColors())
                Spacer(modifier = Modifier.height(12.dp))
                // Fila 3: programa de formacion y ficha.
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = programa, onValueChange = { programa = it }, label = { Text("Programa de Formacion") }, modifier = Modifier.weight(1f), colors = campoCrearColors())
                    OutlinedTextField(value = ficha, onValueChange = { ficha = it }, label = { Text("Ficha") }, modifier = Modifier.weight(1f), colors = campoCrearColors())
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Campo de jornada a ancho completo.
                OutlinedTextField(value = jornada, onValueChange = { jornada = it }, label = { Text("Jornada") }, modifier = Modifier.fillMaxWidth(), colors = campoCrearColors())
                Spacer(modifier = Modifier.height(12.dp))
                // Campo de contraseña obligatoria para el nuevo usuario.
                OutlinedTextField(value = contrasena, onValueChange = { contrasena = it }, label = { Text("Contraseña") }, modifier = Modifier.fillMaxWidth(), colors = campoCrearColors())
                Spacer(modifier = Modifier.height(12.dp))

                // Dropdown de roles alimentado por GET /admin/roles.
                EstadoContenido(estado = roles, onReintentar = onReintentarRoles) { listaRoles ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = rolSeleccionado?.rol_name ?: "Seleccionar rol",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Rol") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = campoCrearColors(),
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

                Spacer(modifier = Modifier.height(32.dp))
                // Mensaje de error de validación o red, si lo hay.
                if (errorMsj != null) {
                    Text(errorMsj!!, color = ErrorRed, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                }
                // Boton Crear: valida y envía el formulario a la API.
                Button(
                    onClick = { crearUsuario() },
                    enabled = !guardando,
                    modifier = Modifier.fillMaxWidth().height(50.dp).pressScale(pressedScale = 0.97f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = colors.textPrimary)
                ) { Text(if (guardando) "Guardando..." else "Crear", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                Spacer(modifier = Modifier.height(12.dp))
                // Boton Cancelar: vuelve al panel sin guardar.
                OutlinedButton(
                    onClick = { onNavigate(AdminScreen.PANEL) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, colors.textSecondary)
                ) { Text("Cancelar", color = colors.textSecondary, fontWeight = FontWeight.Bold) }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Tras crear, se superpone un overlay de exito sobre el formulario.
        if (creado) {
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
                    Text("¡Usuario Creado!", color = SenaGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "El usuario ha sido creado exitosamente.",
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

// Paleta de colores comun para los campos del formulario (verde al enfocar).
@Composable
private fun campoCrearColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SenaGreen, unfocusedBorderColor = LocalAppColors.current.textSecondary,
    focusedLabelColor = SenaGreen, unfocusedLabelColor = LocalAppColors.current.textSecondary,
    cursorColor = SenaGreen, focusedTextColor = LocalAppColors.current.textPrimary, unfocusedTextColor = LocalAppColors.current.textPrimary
)
