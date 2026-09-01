package com.example.sennaccess.admin

// Contenido de la pestaña USUARIOS del ADMINISTRADOR (gestión de usuarios).
// Muestra un menú de categorías (Instructores/Aprendices), una lista filtrable
// con búsqueda, tarjetas de usuario y acciones de crear/editar/eliminar.
// Navega a crear/editar usuario y vuelve al panel tras eliminar.

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

import com.example.sennaccess.data.UsuarioApi
import com.example.sennaccess.ui.CargaUiState
import com.example.sennaccess.ui.EstadoContenido
import com.example.sennaccess.ui.EstadoVacio
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import com.example.sennaccess.ui.UsuariosViewModel
import com.example.sennaccess.ui.ios.GlassCornerRadius
import com.example.sennaccess.ui.ios.IosCollapsibleHeader
import com.example.sennaccess.ui.ios.glassSurface
import com.example.sennaccess.ui.ios.pressScale

/**
 * Gestión de usuarios del ADMINISTRADOR (contenido de la pestaña USUARIOS).
 *
 * Muestra un menú con dos categorías separadas (Instructores / Aprendices),
 * cada una con su propia vista conectada a la API (GET /admin/users).
 * En modo demo (sin sesión) o si la API falla, el ViewModel usa datos de ejemplo.
 */
@Composable
fun UsuariosContent(
    onNavigate: (AdminScreen) -> Unit,
    onEditarUsuario: (UsuarioApi) -> Unit,
    viewModel: UsuariosViewModel = viewModel()
) {
    // Estado de carga/error/datos de la lista de usuarios (desde la API).
    val uiState by viewModel.uiState.collectAsState()

    // Al entrar en la pestaña se dispara la carga inicial de usuarios.
    LaunchedEffect(Unit) { viewModel.cargarUsuarios() }

    // Vista activa del menú de categorías: "", "INSTRUCTORES" o "APRENDICES".
    var vista by rememberSaveable { mutableStateOf("") } // "", "INSTRUCTORES", "APRENDICES"
    // Texto de búsqueda por nombre; se limpia al cambiar de categoría.
    var busqueda by remember { mutableStateOf("") }
    // Usuario pendiente de confirmar eliminación (activa el overlay).
    var usuarioAEliminar by remember { mutableStateOf<UsuarioApi?>(null) }
    var borrando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        // Enrutado interno: según la categoría elegida se muestra su lista o el menú raíz.
        when (vista) {
            // Vista de lista de instructores, filtrada por el rol "Instructor".
            "INSTRUCTORES" -> VistaListaUsuarios(
                titulo = "Instructores",
                icono = Icons.Default.School,
                estado = uiState,
                rol = "Instructor",
                busqueda = busqueda,
                onBusqueda = { busqueda = it },
                onBack = { vista = ""; busqueda = "" },
                onReintentar = viewModel::cargarUsuarios,
                onAgregarUsuario = { onNavigate(AdminScreen.CREAR_USUARIO) },
                onEditar = { onEditarUsuario(it) },
                onBorrar = { usuarioAEliminar = it }
            )
            // Vista de lista de aprendices, filtrada por el rol "Aprendiz".
            "APRENDICES" -> VistaListaUsuarios(
                titulo = "Aprendices",
                icono = Icons.Default.Person,
                estado = uiState,
                rol = "Aprendiz",
                busqueda = busqueda,
                onBusqueda = { busqueda = it },
                onBack = { vista = ""; busqueda = "" },
                onReintentar = viewModel::cargarUsuarios,
                onAgregarUsuario = { onNavigate(AdminScreen.CREAR_USUARIO) },
                onEditar = { onEditarUsuario(it) },
                onBorrar = { usuarioAEliminar = it }
            )
            // Menú raíz: elige entre las dos categorías disponibles.
            else -> MenuUsuarios(
                onInstructores = { vista = "INSTRUCTORES"; busqueda = "" },
                onAprendices = { vista = "APRENDICES"; busqueda = "" }
            )
        }

        // Diálogo de confirmación antes de eliminar: pregunta explícitamente
        // antes de llamar al DELETE y refrescar la lista.
        if (usuarioAEliminar != null) {
            EliminarUsuarioDialog(
                usuario = usuarioAEliminar!!,
                onConfirmar = {
                    val id = usuarioAEliminar!!.id_usuario ?: return@EliminarUsuarioDialog
                    borrando = true
                    scope.launch {
                        try {
                            viewModel.eliminarUsuario(id)
                        } catch (e: Exception) {
                            // Si falla la red se mantiene la lista sin cambios.
                        }
                        borrando = false
                        usuarioAEliminar = null
                    }
                },
                onCancelar = { usuarioAEliminar = null },
                borrando = borrando
            )
        }
    }
}

// Menú de categorías: punto de entrada a la lista de instructores o aprendices.
@Composable
private fun MenuUsuarios(
    onInstructores: () -> Unit,
    onAprendices: () -> Unit
) {
    // Columna con las dos opciones de categoría en tarjetas táctiles.
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(8.dp))
        CategoriaUsuarioRow(
            titulo = "Instructores",
            icono = Icons.Default.School,
            onClick = onInstructores
        )
        Spacer(modifier = Modifier.height(14.dp))
        CategoriaUsuarioRow(
            titulo = "Aprendices",
            icono = Icons.Default.Person,
            onClick = onAprendices
        )
    }
}

// Tarjeta de categoría reutilizable: ícono, título y flecha de navegación.
@Composable
private fun CategoriaUsuarioRow(
    titulo: String,
    icono: ImageVector,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    // Fila táctil con efecto de presión; al tocar dispara la navegación a la categoría.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(pressedScale = 0.96f)
            .glassSurface(cornerRadius = GlassCornerRadius)
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(SenaGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = SenaGreen, modifier = Modifier.size(26.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(titulo, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = SenaGreen, modifier = Modifier.size(28.dp))
    }
}

// Lista de usuarios de un rol concreto: botón crear, buscador y tarjetas.
@Composable
private fun VistaListaUsuarios(
    titulo: String,
    icono: ImageVector,
    estado: CargaUiState<List<UsuarioApi>>,
    rol: String,
    busqueda: String,
    onBusqueda: (String) -> Unit,
    onBack: () -> Unit,
    onReintentar: () -> Unit,
    onAgregarUsuario: () -> Unit,
    onEditar: (UsuarioApi) -> Unit,
    onBorrar: (UsuarioApi) -> Unit
) {
    val colors = LocalAppColors.current
    val scrollState = rememberScrollState()

    // Columna desplazable con todo el contenido de la vista.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Botón de retroceso para volver al menú de categorías.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = SenaGreen)
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        // Encabezado con el título de la categoría actual.
        IosCollapsibleHeader(
            title = titulo,
            subtitle = "Lista de $titulo registrados",
            scrollOffset = scrollState.value.toFloat()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Botón principal para registrar un nuevo usuario en el sistema.
        Button(
            onClick = onAgregarUsuario,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .pressScale(pressedScale = 0.97f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black)
        ) {
            Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("AGREGAR NUEVO USUARIO", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Campo de búsqueda en vivo por nombre dentro de la categoría seleccionada.
        OutlinedTextField(
            value = busqueda,
            onValueChange = onBusqueda,
            placeholder = { Text("Buscar $titulo por nombre...", color = colors.textSecondary) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.textSecondary) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SenaGreen, unfocusedBorderColor = colors.borderLight,
                cursorColor = SenaGreen, focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // EstadoContenido gestiona carga/error/éxito; aquí se filtran los datos.
        EstadoContenido(estado = estado, onReintentar = onReintentar) { todos ->
            // Filtra por el rol de la categoría y aplica la búsqueda por nombre.
            val filtrados = todos
                .filter { it.role?.rol_name.equals(rol, ignoreCase = true) }
                .filter { it.user_name?.contains(busqueda, ignoreCase = true) ?: true }

            if (filtrados.isEmpty()) {
                // Sin resultados para el filtro/búsqueda actual.
                EstadoVacio(
                    icono = icono,
                    titulo = "Sin $titulo registrados",
                    mensaje = if (busqueda.isBlank()) {
                        "Usa el botón para agregar el primer $titulo."
                    } else {
                        "No hay $titulo que coincidan con \"$busqueda\"."
                    }
                )
            } else {
                // Tarjetas de usuarios filtrados en fila horizontal deslizable.
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filtrados) { usuario ->
                        TarjetaUsuario(usuario, onEditar = { onEditar(usuario) }, onBorrar = { onBorrar(usuario) })
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// Diálogo de confirmación real: pregunta si se desea eliminar al usuario y
// permite cancelar antes de ejecutar el DELETE /admin/users/{id}.
@Composable
private fun EliminarUsuarioDialog(
    usuario: UsuarioApi,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    borrando: Boolean = false
) {
    val colors = LocalAppColors.current
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = colors.cardBackground.copy(alpha = 0.98f),
        shape = RoundedCornerShape(24.dp),
        icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(36.dp)) },
        title = { Text("Eliminar usuario", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "¿Seguro que deseas eliminar a ${usuario.nombreCompleto}? " +
                    "Esta acción no se puede deshacer.",
                color = colors.textSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                enabled = !borrando,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) { Text(if (borrando) "Eliminando..." else "Eliminar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar", color = colors.textSecondary) }
        }
    )
}

// Tarjeta compacta de usuario con datos principales y acciones editar/eliminar.
@Composable
fun TarjetaUsuario(usuario: UsuarioApi, onEditar: () -> Unit, onBorrar: () -> Unit) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .width(220.dp)
            .glassSurface(cornerRadius = GlassCornerRadius)
    ) {
        // Contenido centrado: avatar, nombre, rol y datos de identificación.
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(60.dp).clip(CircleShape).background(SenaGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Person, contentDescription = null, tint = SenaGreen, modifier = Modifier.size(40.dp)) }
            Spacer(modifier = Modifier.height(8.dp))
            Text(usuario.nombreCompleto, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(usuario.role?.rol_name ?: "", color = SenaGreen, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text("CC: ${usuario.user_identification ?: ""}", color = colors.textSecondary, fontSize = 11.sp)
            Text("Ficha ${usuario.user_coursenumber ?: 0}", color = colors.textSecondary, fontSize = 11.sp)
            Text(usuario.user_program ?: "", color = colors.textSecondary, fontSize = 11.sp)
            Text(usuario.user_email ?: "", color = colors.textSecondary, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(8.dp))
            // Acciones de la tarjeta: editar (verde) y eliminar (rojo).
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onEditar,
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, SenaGreen)
                ) { Icon(Icons.Default.Edit, null, tint = SenaGreen, modifier = Modifier.size(16.dp)) }
                OutlinedButton(
                    onClick = onBorrar,
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.Red)
                ) { Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(16.dp)) }
            }
        }
    }
}
