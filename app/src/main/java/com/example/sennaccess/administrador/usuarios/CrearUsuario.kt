package com.example.sennaccess.administrador.usuarios

// Formulario para crear un usuario del ADMINISTRADOR (sub-pantalla).
// Captura los datos personales del nuevo usuario; el campo de rol se llena
// con el catalogo real desde GET /admin/roles. Al enviar, muestra confirmacion.

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

// El dropdown de roles consume GET /admin/roles a traves del PanelAdministradorViewModel.

@Composable
fun ContenidoCrearUsuario(
    roles: EstadoCarga<List<Rol>>,
    onReintentarRoles: () -> Unit,
    onNavigate: (PantallaAdmin) -> Unit
) {
    var nombres by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var identificacion by remember { mutableStateOf("") }
    var programa by remember { mutableStateOf("") }
    var ficha by remember { mutableStateOf("") }
    var jornada by remember { mutableStateOf("Tarde") }
    var jornadaSabado by remember { mutableStateOf("Mañana") }
    var contrasena by remember { mutableStateOf("") }
    var documentoTipo by remember { mutableStateOf("CC") }
    var telefono by remember { mutableStateOf("") }
    var rolSeleccionado by remember { mutableStateOf<Rol?>(null) }
    var dropdownAbierto by remember { mutableStateOf(false) }
    var creado by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }
    var errorMsj by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val colors = ColoresAppLocal.current
    val scrollState = rememberScrollState()

    // Envía el formulario a POST /admin/users; en éxito muestra el overlay de creado.
    fun crearUsuario() {
        if (guardando) return
        val rol = rolSeleccionado ?: run { errorMsj = "Seleccione un rol"; return }
        if (contrasena.length < 8) { errorMsj = "La contraseña debe tener mínimo 8 caracteres"; return }
        if (correo.trim().isBlank()) { errorMsj = "El correo es obligatorio"; return }
        if (identificacion.trim().isBlank()) { errorMsj = "La identificación es obligatoria"; return }
        if (nombres.trim().isBlank() || apellidos.trim().isBlank()) { errorMsj = "Nombres y apellidos son obligatorios"; return }
        if (ficha.isNotBlank() && ficha.trim().toIntOrNull() == null) { errorMsj = "La ficha debe ser numérica"; return }
        guardando = true
        errorMsj = null
        scope.launch {
            try {
                val token = GestorSesion.token
                if (token == null) { guardando = false; errorMsj = "Sesión expirada. Inicia sesión de nuevo."; return@launch }
                // Portero local (id 0): se resuelve contra el servidor para usar
                // su id real; si el WEB aún no lo tiene, se avisa sin enviar nada.
                var rolId = rol.id_rol
                if (rolId == null || rolId == 0) {
                    rolId = try {
                        RepositorioUsuarios().getRoles(token)
                            .firstOrNull { it.rol_name.equals("admin", ignoreCase = true) }?.id_rol
                    } catch (_: Exception) { null }
                    if (rolId == null) {
                        guardando = false
                        errorMsj = "El servidor aún no tiene el rol Admin: publica el proyecto WEB (migrate --seed) e inténtalo de nuevo."
                        return@launch
                    }
                }
                RepositorioUsuarios().crearUsuario(
                    token = token,
                    body = PeticionUsuario(
                        user_identification = identificacion.trim(),
                        user_name = nombres.trim(),
                        user_lastname = apellidos.trim(),
                        user_email = correo.trim(),
                        user_password = contrasena,
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
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cabecera editorial del formulario.
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("NUEVO USUARIO", color = verdeMarca(), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
                Text("Registrar usuario", color = colors.textPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Text("Completa la ficha de la persona y su acceso al sistema.", color = colors.textSecondary, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier.fillMaxWidth().superficiePlana(cornerRadius = RadioSena.lg).padding(16.dp)
            ) {
                EtiquetaSeccionUsuario("Datos personales")
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = nombres, onValueChange = { nombres = it }, label = { Text("Nombres") }, modifier = Modifier.weight(1f).campoVisible(), colors = campoCrearColors(), shape = RoundedCornerShape(16.dp))
                    OutlinedTextField(value = apellidos, onValueChange = { apellidos = it }, label = { Text("Apellidos") }, modifier = Modifier.weight(1f).campoVisible(), colors = campoCrearColors(), shape = RoundedCornerShape(16.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = correo, onValueChange = { correo = it }, label = { Text("Correo Electronico") }, modifier = Modifier.weight(1f).campoVisible(), colors = campoCrearColors(), shape = RoundedCornerShape(16.dp))
                    OutlinedTextField(value = identificacion, onValueChange = { identificacion = it }, label = { Text("Numero de Identificacion") }, modifier = Modifier.weight(1f).campoVisible(), colors = campoCrearColors(), shape = RoundedCornerShape(16.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                val tiposDoc = listOf(
                    "CC: Cédula de Ciudadanía",
                    "CE: Cédula de Extranjería",
                    "TI: Tarjeta de Identidad",
                    "PAS: Pasaporte"
                )
                val docActual = tiposDoc.firstOrNull { it.startsWith(documentoTipo) } ?: tiposDoc.first()
                com.example.sennaccess.comun.diseno.DesplegableSena(
                    valor = docActual,
                    opciones = tiposDoc,
                    onElegir = { documentoTipo = it.substringBefore(":").trim() },
                    label = "Tipo de Documento"
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = telefono, onValueChange = { telefono = it }, label = { Text("Telefono de contacto (opcional)") }, modifier = Modifier.fillMaxWidth().campoVisible(), colors = campoCrearColors(), shape = RoundedCornerShape(16.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = programa, onValueChange = { programa = it }, label = { Text("Programa de Formacion") }, modifier = Modifier.weight(1f).campoVisible(), colors = campoCrearColors(), shape = RoundedCornerShape(16.dp))
                    OutlinedTextField(value = ficha, onValueChange = { ficha = it }, label = { Text("Ficha") }, modifier = Modifier.weight(1f).campoVisible(), colors = campoCrearColors(), shape = RoundedCornerShape(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier.fillMaxWidth().superficiePlana(cornerRadius = RadioSena.lg).padding(16.dp)
            ) {
                EtiquetaSeccionUsuario("Jornada")
                Spacer(modifier = Modifier.height(8.dp))
                com.example.sennaccess.comun.diseno.DesplegableSena(
                    valor = jornada,
                    opciones = com.example.sennaccess.comun.Jornadas.TODAS,
                    onElegir = { jornada = it },
                    label = "Jornada (lunes a viernes)"
                )
                Spacer(modifier = Modifier.height(12.dp))
                com.example.sennaccess.comun.diseno.DesplegableSena(
                    valor = jornadaSabado,
                    opciones = com.example.sennaccess.comun.Jornadas.TODAS,
                    onElegir = { jornadaSabado = it },
                    label = "Jornada de los sábados"
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier.fillMaxWidth().superficiePlana(cornerRadius = RadioSena.lg).padding(16.dp)
            ) {
                EtiquetaSeccionUsuario("Acceso y rol")
                Spacer(modifier = Modifier.height(8.dp))
                var mostrarClave by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = contrasena,
                    onValueChange = { contrasena = it },
                    label = { Text("Contraseña") },
                    modifier = Modifier.fillMaxWidth().campoVisible(),
                    colors = campoCrearColors(),
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
                Spacer(modifier = Modifier.height(12.dp))

                // Dropdown de roles alimentado por GET /admin/roles. Se abre tocando
                // cualquier parte del campo. Portero siempre visible: si el backend
                // aún no lo expone se ofrece local y su id se resuelve al guardar.
                EstadoContenido(estado = roles, onReintentar = onReintentarRoles) { listaRoles ->
                    val rolesVisibles = remember(listaRoles) {
                        if (listaRoles.none { it.rol_name.equals("admin", ignoreCase = true) })
                            listaRoles + Rol(id_rol = 0, rol_name = "Admin")
                        else listaRoles
                    }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = rolSeleccionado?.rol_name ?: "Seleccionar rol",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Rol - toca para elegir") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = campoCrearColors(),
                            shape = RoundedCornerShape(16.dp),
                            enabled = false,
                            trailingIcon = {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = verdeMarca())
                            }
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { dropdownAbierto = true })
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
                    // Aviso cuando el servidor aún no tiene el rol Admin: sin él,
                    // asignar portería es imposible (el id 4 del servidor es Invitado).
                    if (listaRoles.none { it.rol_name.equals("admin", ignoreCase = true) }) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "El servidor aún no tiene el rol Admin: créalo en el proyecto WEB (tabla roles + permisos de portería) y aparecerá aquí.",
                            color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                if (errorMsj != null) {
                    Text(errorMsj!!, color = RojoError, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Button(
                    onClick = { crearUsuario() },
                    enabled = !guardando,
                    modifier = Modifier.fillMaxWidth().height(52.dp).escalaPresion(pressedScale = 0.97f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = Color.Black)
                ) { Text(if (guardando) "Guardando..." else "Crear", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { onNavigate(PantallaAdmin.USUARIOS) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, colors.textSecondary)
                ) { Text("Cancelar", color = colors.textSecondary, fontWeight = FontWeight.Bold) }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (creado) {
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
private fun campoCrearColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = verdeMarca(), unfocusedBorderColor = ColoresAppLocal.current.textSecondary,
    focusedLabelColor = verdeMarca(), unfocusedLabelColor = ColoresAppLocal.current.textSecondary,
    cursorColor = verdeMarca(), focusedTextColor = ColoresAppLocal.current.textPrimary, unfocusedTextColor = ColoresAppLocal.current.textPrimary
)

// Etiqueta de sección en formularios de usuarios: eyebrow verde + jerarquía.
@Composable
private fun EtiquetaSeccionUsuario(texto: String) {
    Text(
        text = texto.uppercase(),
        color = verdeMarca(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.6.sp
    )
}
