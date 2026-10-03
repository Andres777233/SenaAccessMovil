package com.example.sennaccess.administrador.usuarios

// Formulario para actualizar los datos de un usuario existente (rol ADMIN).
// Recibe el objeto UsuarioApi obtenido desde GET /admin/users y prellena
// los campos editables; al enviar muestra confirmación y navega de vuelta.

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.datos.modelos.Rol
import com.example.sennaccess.perfil.FotoPerfil
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
import com.example.sennaccess.comun.diseno.RadioSena
import com.example.sennaccess.comun.diseno.superficieVidrio
import com.example.sennaccess.comun.diseno.superficiePlana
import com.example.sennaccess.comun.diseno.escalaPresion
import kotlinx.coroutines.launch
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
    // Solo el admin edita jornada/ficha/programa/correo (fix seguridad).
    var jornada by remember { mutableStateOf(usuario.user_jornada ?: "Tarde") }
    var jornadaSabado by remember { mutableStateOf(usuario.user_jornada_sabado ?: "Mañana") }
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
        val rol = rolSeleccionado ?: run { errorMsj = "Seleccione un rol"; return }
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
                // Portero local (id 0): id real del servidor o aviso honesto.
                var rolId = rol.id_rol
                if (rolId == null || rolId == 0) {
                    rolId = try {
                        RepositorioUsuarios().getRoles(token)
                            .firstOrNull { it.rol_name.equals("portero", ignoreCase = true) }?.id_rol
                    } catch (_: Exception) { null }
                    if (rolId == null) {
                        guardando = false
                        errorMsj = "El servidor aún no tiene el rol Portero: publica el proyecto WEB (migrate --seed) e inténtalo de nuevo."
                        return@launch
                    }
                }
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
                        user_jornada = jornada.ifBlank { null },
                        user_jornada_sabado = jornadaSabado.ifBlank { null },
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
            // Hero del usuario a editar: avatar + nombre + rol/correo.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .superficiePlana(cornerRadius = RadioSena.lg)
                    .padding(16.dp)
            ) {
                FotoPerfil(fotoPath = usuario.profile_photo_path, nombre = usuario.nombreCompleto, tamano = 72.dp)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("EDITAR USUARIO", color = verdeMarca(), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(usuario.nombreCompleto, color = colors.textPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, maxLines = 2)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val rolTxt = usuario.role?.rol_name?.uppercase()?.takeIf { it.isNotBlank() } ?: "SIN ROL"
                        Box(
                            modifier = Modifier.clip(CircleShape).background(VerdeSena.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(rolTxt, color = verdeMarca(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (!usuario.user_email.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(usuario.user_email!!, color = colors.textSecondary, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth().superficiePlana(cornerRadius = RadioSena.lg).padding(16.dp)
            ) {
                EtiquetaSeccionEditar("Datos personales")
                Spacer(modifier = Modifier.height(8.dp))
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
                Spacer(modifier = Modifier.height(16.dp))
                EtiquetaSeccionEditar("Documento")
                Spacer(modifier = Modifier.height(8.dp))
                val tiposDocActualizar = listOf(
                    "CC: Cédula de Ciudadanía",
                    "CE: Cédula de Extranjería",
                    "TI: Tarjeta de Identidad",
                    "PAS: Pasaporte"
                )
                val docActualActualizar = tiposDocActualizar.firstOrNull { it.startsWith(documentoTipo) } ?: tiposDocActualizar.first()
                com.example.sennaccess.comun.diseno.DesplegableSena(
                    valor = docActualActualizar,
                    opciones = tiposDocActualizar,
                    onElegir = { documentoTipo = it.substringBefore(":").trim() },
                    label = "Tipo de Documento"
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = telefono, onValueChange = { telefono = it }, label = { Text("Telefono de contacto (opcional)") }, modifier = Modifier.fillMaxWidth().campoVisible(), colors = campoColors(), shape = RoundedCornerShape(16.dp))
                Spacer(modifier = Modifier.height(12.dp))
                com.example.sennaccess.comun.diseno.DesplegableSena(
                    valor = jornada.ifBlank { "Tarde" },
                    opciones = com.example.sennaccess.comun.Jornadas.TODAS,
                    onElegir = { jornada = it },
                    label = "Jornada (lunes a viernes)"
                )
                Spacer(modifier = Modifier.height(12.dp))
                com.example.sennaccess.comun.diseno.DesplegableSena(
                    valor = jornadaSabado.ifBlank { "Mañana" },
                    opciones = com.example.sennaccess.comun.Jornadas.TODAS,
                    onElegir = { jornadaSabado = it },
                    label = "Jornada de los sábados"
                )
                Spacer(modifier = Modifier.height(16.dp))
                EtiquetaSeccionEditar("Rol y seguridad")
                Spacer(modifier = Modifier.height(8.dp))
                EstadoContenido(estado = roles, onReintentar = onReintentarRoles) { listaRoles ->
                    val rolesVisibles = remember(listaRoles, usuario.role) {
                        val base = if (listaRoles.none { it.rol_name.equals("portero", ignoreCase = true) })
                            listaRoles + Rol(id_rol = 0, rol_name = "Portero")
                        else listaRoles
                        // El rol actual del usuario siempre visible aunque el
                        // servidor ya no lo liste (evita dropdown vacío).
                        val actual = usuario.role
                        if (actual?.rol_name != null && base.none { it.id_rol == actual.id_rol }) base + actual else base
                    }
                    Box(modifier = Modifier.fillMaxWidth().clickable { dropdownAbierto = !dropdownAbierto }) {
                        OutlinedTextField(
                            value = rolSeleccionado?.rol_name ?: "Seleccionar rol",
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Rol - toca para elegir") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = campoColors(),
                            shape = RoundedCornerShape(16.dp),
                            trailingIcon = {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = verdeMarca())
                            }
                        )
                        // Capa full-touch: toda la barra abre el menú.
                        Box(modifier = Modifier.matchParentSize().clickable { dropdownAbierto = !dropdownAbierto })
                        DropdownMenu(
                            expanded = dropdownAbierto,
                            onDismissRequest = { dropdownAbierto = false },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rolesVisibles.forEach { rol ->
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
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, colors.textSecondary)
                    ) { Text("CANCELAR", color = colors.textSecondary, fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = { guardarCambios() },
                        enabled = !guardando,
                        modifier = Modifier.weight(1f).height(52.dp).escalaPresion(pressedScale = 0.97f),
                        shape = RoundedCornerShape(16.dp),
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

// Etiqueta de sección en el formulario de edición: eyebrow verde + jerarquía.
@Composable
private fun EtiquetaSeccionEditar(texto: String) {
    Text(
        text = texto.uppercase(),
        color = verdeMarca(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.6.sp
    )
}
