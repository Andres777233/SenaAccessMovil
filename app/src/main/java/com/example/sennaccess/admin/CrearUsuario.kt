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
import com.example.sennaccess.ui.detalleHttp
import com.example.sennaccess.ui.campoVisible
import com.example.sennaccess.ui.theme.ErrorRed
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import com.example.sennaccess.ui.theme.verdeMarca
import com.example.sennaccess.ui.ios.GlassCornerRadius
import com.example.sennaccess.ui.ios.IosCollapsibleHeader
import com.example.sennaccess.ui.ios.glassSurface
import com.example.sennaccess.ui.ios.pressScale
import kotlinx.coroutines.launch

// Formulario para crear usuario del ADMINISTRADOR (sub-pantalla).
// El dropdown de roles consume GET /admin/roles a traves del AdminDashboardViewModel.

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
        if (contrasena.length < 8) { errorMsj = "La contraseña debe tener mínimo 8 caracteres"; return }
        if (correo.trim().isBlank()) { errorMsj = "El correo es obligatorio"; return }
        if (identificacion.trim().isBlank()) { errorMsj = "La identificación es obligatoria"; return }
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
            } catch (e: retrofit2.HttpException) {
                guardando = false
                errorMsj = detalleHttp(e)
            } catch (e: Exception) {
                guardando = false
                errorMsj = "No se pudo conectar al servidor."
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                // El contenido se encoge sobre el teclado para no quedar tapado.
                .imePadding(),
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
                    OutlinedTextField(value = nombres, onValueChange = { nombres = it }, label = { Text("Nombres") }, modifier = Modifier.weight(1f).campoVisible(), colors = campoCrearColors())
                    OutlinedTextField(value = apellidos, onValueChange = { apellidos = it }, label = { Text("Apellidos") }, modifier = Modifier.weight(1f).campoVisible(), colors = campoCrearColors())
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Fila 2: correo y numero de identificacion.
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = correo, onValueChange = { correo = it }, label = { Text("Correo Electronico") }, modifier = Modifier.weight(1f).campoVisible(), colors = campoCrearColors())
                    OutlinedTextField(value = identificacion, onValueChange = { identificacion = it }, label = { Text("Numero de Identificacion") }, modifier = Modifier.weight(1f).campoVisible(), colors = campoCrearColors())
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
                        label = { Text("Tipo de Documento - toca para elegir") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        colors = campoCrearColors(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = docDropdownAbierto) }
                    )
                    ExposedDropdownMenu(
                        expanded = docDropdownAbierto,
                        onDismissRequest = { docDropdownAbierto = false }
                    ) {
                        tiposDoc.forEach { (codigo, significado) ->
                            DropdownMenuItem(
                                text = { Text("$codigo: $significado", color = colors.textPrimary) },
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
                OutlinedTextField(value = telefono, onValueChange = { telefono = it }, label = { Text("Telefono de contacto (opcional)") }, modifier = Modifier.fillMaxWidth().campoVisible(), colors = campoCrearColors())
                Spacer(modifier = Modifier.height(12.dp))
                // Fila 3: programa de formacion y ficha.
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = programa, onValueChange = { programa = it }, label = { Text("Programa de Formacion") }, modifier = Modifier.weight(1f).campoVisible(), colors = campoCrearColors())
                    OutlinedTextField(value = ficha, onValueChange = { ficha = it }, label = { Text("Ficha") }, modifier = Modifier.weight(1f).campoVisible(), colors = campoCrearColors())
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Campo de jornada a ancho completo.
                OutlinedTextField(value = jornada, onValueChange = { jornada = it }, label = { Text("Jornada") }, modifier = Modifier.fillMaxWidth().campoVisible(), colors = campoCrearColors())
                Spacer(modifier = Modifier.height(12.dp))
                // Campo de contraseña obligatoria para el nuevo usuario.
                OutlinedTextField(value = contrasena, onValueChange = { contrasena = it }, label = { Text("Contraseña") }, modifier = Modifier.fillMaxWidth().campoVisible(), colors = campoCrearColors())
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
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = verdeMarca())
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
                    modifier = Modifier.fillMaxWidth().height(52.dp).pressScale(pressedScale = 0.97f),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black)
                ) { Text(if (guardando) "Guardando..." else "Crear", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                Spacer(modifier = Modifier.height(12.dp))
                // Boton Cancelar: vuelve a la lista de usuarios sin guardar.
                OutlinedButton(
                    onClick = { onNavigate(AdminScreen.USUARIOS) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, colors.textSecondary)
                ) { Text("Cancelar", color = colors.textSecondary, fontWeight = FontWeight.Bold) }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Tras crear, overlay de exito: OK lo cierra, limpia el formulario y se queda
        // en esta misma pantalla (antes mandaba al dashboard y tocaba volver a entrar).
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
                    Icon(Icons.Default.CheckCircle, null, tint = verdeMarca(), modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("¡Usuario Creado!", color = verdeMarca(), fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "El usuario ha sido creado exitosamente.",
                        color = colors.textPrimary, fontSize = 18.sp, textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            creado = false
                            nombres = ""; apellidos = ""; correo = ""; identificacion = ""
                            programa = ""; ficha = ""; jornada = ""; contrasena = ""
                            telefono = ""; documentoTipo = "CC"; rolSeleccionado = null
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp).pressScale(pressedScale = 0.97f),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black)
                    ) { Text("OK", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                }
            }
        }
    }
}

// Paleta de colores comun para los campos del formulario (verde al enfocar).
@Composable
private fun campoCrearColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = verdeMarca(), unfocusedBorderColor = LocalAppColors.current.textSecondary,
    focusedLabelColor = verdeMarca(), unfocusedLabelColor = LocalAppColors.current.textSecondary,
    cursorColor = verdeMarca(), focusedTextColor = LocalAppColors.current.textPrimary, unfocusedTextColor = LocalAppColors.current.textPrimary
)
