package com.example.sennaccess.administrador.usuarios

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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.datos.modelos.Rol
import com.example.sennaccess.datos.sesion.GestorSesion
import com.example.sennaccess.datos.modelos.PeticionUsuario
import com.example.sennaccess.datos.modelos.UsuarioApi
import com.example.sennaccess.datos.repositorios.RepositorioUsuarios
import com.example.sennaccess.comun.EstadoCarga
import com.example.sennaccess.comun.EstadoContenido
import com.example.sennaccess.comun.detalleHttp
import com.example.sennaccess.comun.campoVisible
import com.example.sennaccess.comun.tema.RojoError
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import com.example.sennaccess.comun.diseno.RadioVidrio
import com.example.sennaccess.comun.diseno.CabeceraPlegable
import com.example.sennaccess.comun.diseno.superficieVidrio
import com.example.sennaccess.comun.diseno.escalaPresion
import kotlinx.coroutines.launch
import com.example.sennaccess.administrador.panel.ContenedorVidrioAdmin
import com.example.sennaccess.administrador.panel.PantallaAdmin

// Prellena los campos con los datos reales del GET /admin/users.

@Composable
fun ContenidoActualizarUsuario(
    usuario: UsuarioApi,
    roles: EstadoCarga<List<Rol>>,
    onReintentarRoles: () -> Unit,
    onNavigate: (PantallaAdmin) -> Unit
) {
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
    val colors = ColoresAppLocal.current
    val scrollState = rememberScrollState()

    // Envía los cambios a PUT /admin/users/{id}; en éxito muestra el overlay.
    fun guardarCambios() {
        if (guardando) return
        val rolId = rolSeleccionado?.id_rol ?: run { errorMsj = "Seleccione un rol"; return }
        if (contrasena.isNotBlank() && contrasena.length < 8) { errorMsj = "La contraseña debe tener mínimo 8 caracteres"; return }
        if (correo.trim().isBlank()) { errorMsj = "El correo es obligatorio"; return }
        if (ficha.isNotBlank() && ficha.trim().toIntOrNull() == null) { errorMsj = "La ficha debe ser numérica"; return }
        guardando = true
        errorMsj = null
        scope.launch {
            try {
                val token = GestorSesion.token
                if (token == null) { guardando = false; errorMsj = "Sesión expirada. Inicia sesión de nuevo."; return@launch }
                val idUsuario = usuario.id_usuario
                if (idUsuario == null) { guardando = false; errorMsj = "Usuario sin ID."; return@launch }
                RepositorioUsuarios().actualizarUsuario(
                    token = token,
                    id = idUsuario,
                    body = PeticionUsuario(
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
                .imePadding()
        ) {
            CabeceraPlegable(
                title = "Actualizar Usuario",
                subtitle = "Edicion de datos del usuario",
                scrollOffset = scrollState.value.toFloat()
            )

            Spacer(modifier = Modifier.height(12.dp))

            ContenedorVidrioAdmin {
                Text(
                    "Actualizar Datos De ${usuario.nombreCompleto}",
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(value = nombres, onValueChange = { nombres = it }, label = { Text("Nombres") }, modifier = Modifier.fillMaxWidth().campoVisible(), colors = campoColors(), shape = RoundedCornerShape(16.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(value = correo, onValueChange = { correo = it }, label = { Text("Correo Electronico") }, modifier = Modifier.fillMaxWidth().campoVisible(), colors = campoColors(), shape = RoundedCornerShape(16.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(value = ficha, onValueChange = { ficha = it }, label = { Text("Ficha") }, modifier = Modifier.fillMaxWidth().campoVisible(), colors = campoColors(), shape = RoundedCornerShape(16.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(value = apellidos, onValueChange = { apellidos = it }, label = { Text("Apellidos") }, modifier = Modifier.fillMaxWidth().campoVisible(), colors = campoColors(), shape = RoundedCornerShape(16.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(value = numeroId, onValueChange = { numeroId = it }, label = { Text("Numero De Identificacion") }, modifier = Modifier.fillMaxWidth().campoVisible(), colors = campoColors(), shape = RoundedCornerShape(16.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(value = programa, onValueChange = { programa = it }, label = { Text("Programa de Formacion") }, modifier = Modifier.fillMaxWidth().campoVisible(), colors = campoColors(), shape = RoundedCornerShape(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
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
                        colors = campoColors(),
                        shape = RoundedCornerShape(16.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = docDropdownAbierto) }
                    )
                    ExposedDropdownMenu(
                        expanded = docDropdownAbierto,
                        onDismissRequest = { docDropdownAbierto = false },
                        shape = RoundedCornerShape(16.dp)
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
                OutlinedTextField(value = telefono, onValueChange = { telefono = it }, label = { Text("Telefono de contacto (opcional)") }, modifier = Modifier.fillMaxWidth().campoVisible(), colors = campoColors(), shape = RoundedCornerShape(16.dp))
                Spacer(modifier = Modifier.height(12.dp))
                EstadoContenido(estado = roles, onReintentar = onReintentarRoles) { listaRoles ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = rolSeleccionado?.rol_name ?: "Seleccionar rol",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Rol") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = campoColors(),
                            shape = RoundedCornerShape(16.dp),
                            trailingIcon = {
                                IconButton(onClick = { dropdownAbierto = true }) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = verdeMarca())
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = dropdownAbierto,
                            onDismissRequest = { dropdownAbierto = false },
                            shape = RoundedCornerShape(16.dp),
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
                var mostrarClave by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = contrasena,
                    onValueChange = { contrasena = it },
                    label = { Text("Nueva contraseña (opcional)") },
                    modifier = Modifier.fillMaxWidth().campoVisible(),
                    colors = campoColors(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    visualTransformation = if (mostrarClave) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { mostrarClave = !mostrarClave }) {
                            Icon(
                                imageVector = if (mostrarClave) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (mostrarClave) "Ocultar contraseña" else "Mostrar contraseña",
                                tint = colors.textSecondary
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.height(32.dp))
                if (errorMsj != null) {
                    Text(errorMsj!!, color = RojoError, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { onNavigate(PantallaAdmin.USUARIOS) },
                        modifier = Modifier.weight(1f).height(52.dp).escalaPresion(pressedScale = 0.97f),
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(1.dp, colors.textSecondary)
                    ) { Text("CANCELAR", color = colors.textSecondary, fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = { guardarCambios() },
                        enabled = !guardando,
                        modifier = Modifier.weight(1f).height(52.dp).escalaPresion(pressedScale = 0.97f),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = Color.Black)
                    ) { Text(if (guardando) "GUARDANDO..." else "ACTUALIZAR", fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (actualizado) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .superficieVidrio(cornerRadius = RadioVidrio)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = verdeMarca(), modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("¡Actualizacion Exitosa!", color = verdeMarca(), fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Los datos de ${usuario.nombreCompleto} han sido actualizados correctamente.",
                        color = colors.textPrimary, fontSize = 18.sp, textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { actualizado = false },
                        modifier = Modifier.fillMaxWidth().height(52.dp).escalaPresion(pressedScale = 0.97f),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = Color.Black)
                    ) { Text("OK", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                }
            }
        }
    }
}

@Composable
private fun campoColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = verdeMarca(), unfocusedBorderColor = ColoresAppLocal.current.textSecondary,
    focusedLabelColor = verdeMarca(), unfocusedLabelColor = ColoresAppLocal.current.textSecondary,
    cursorColor = verdeMarca(), focusedTextColor = ColoresAppLocal.current.textPrimary, unfocusedTextColor = ColoresAppLocal.current.textPrimary
)
