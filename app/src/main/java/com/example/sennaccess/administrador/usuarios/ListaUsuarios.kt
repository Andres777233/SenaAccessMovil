package com.example.sennaccess.administrador.usuarios

// Contenido de la pestaña USUARIOS del ADMINISTRADOR (gestión de usuarios).
// Muestra un menú de categorías (Instructores/Aprendices), una lista filtrable
// con búsqueda, tarjetas de usuario y acciones de crear/editar/eliminar.
// Navega a crear/editar usuario y vuelve al panel tras eliminar.

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

import com.example.sennaccess.datos.modelos.UsuarioApi
import com.example.sennaccess.perfil.FotoPerfil
import com.example.sennaccess.comun.EstadoCarga
import com.example.sennaccess.comun.VistaVacia
import com.example.sennaccess.comun.campoVisible
import com.example.sennaccess.comun.detalleHttp
import com.example.sennaccess.comun.diseno.BuscadorSena
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import com.example.sennaccess.administrador.usuarios.ModeloUsuarios
import com.example.sennaccess.comun.diseno.RadioVidrio
import com.example.sennaccess.comun.diseno.CabeceraPlegable
import com.example.sennaccess.comun.diseno.superficieVidrio
import com.example.sennaccess.comun.diseno.escalaPresion
import com.example.sennaccess.administrador.panel.PantallaAdmin
// cada una con su propia vista conectada a la API (GET /admin/users).
@Composable
fun ContenidoUsuarios(
    onNavigate: (PantallaAdmin) -> Unit,
    onEditarUsuario: (UsuarioApi) -> Unit,
    viewModel: ModeloUsuarios = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.cargarUsuarios() }

    var vista by rememberSaveable { mutableStateOf("") }
    var busqueda by remember { mutableStateOf("") }
    var usuarioAEliminar by remember { mutableStateOf<UsuarioApi?>(null) }
    var borrando by remember { mutableStateOf(false) }
    var errorEliminar by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        when (vista) {
            "INSTRUCTORES" -> VistaListaUsuarios(
                titulo = "Instructores",
                icono = Icons.Default.School,
                estado = uiState,
                rol = "Instructor",
                busqueda = busqueda,
                onBusqueda = { busqueda = it },
                onBack = { vista = ""; busqueda = "" },
                onReintentar = viewModel::cargarUsuarios,
                onAgregarUsuario = { onNavigate(PantallaAdmin.CREAR_USUARIO) },
                onEditar = { onEditarUsuario(it) },
                onBorrar = { usuarioAEliminar = it; errorEliminar = null }
            )
            "APRENDICES" -> VistaListaUsuarios(
                titulo = "Aprendices",
                icono = Icons.Default.Person,
                estado = uiState,
                rol = "Aprendiz",
                busqueda = busqueda,
                onBusqueda = { busqueda = it },
                onBack = { vista = ""; busqueda = "" },
                onReintentar = viewModel::cargarUsuarios,
                onAgregarUsuario = { onNavigate(PantallaAdmin.CREAR_USUARIO) },
                onEditar = { onEditarUsuario(it) },
                onBorrar = { usuarioAEliminar = it; errorEliminar = null }
            )
            else -> MenuUsuarios(
                estado = uiState,
                onInstructores = { vista = "INSTRUCTORES"; busqueda = "" },
                onAprendices = { vista = "APRENDICES"; busqueda = "" }
            )
        }

        if (usuarioAEliminar != null) {
            usuarioAEliminar?.let { actual ->
                EliminarUsuarioDialog(
                    usuario = actual,
                    error = errorEliminar,
                    onConfirmar = {
                        val id = actual.id_usuario ?: return@EliminarUsuarioDialog
                        if (borrando) return@EliminarUsuarioDialog
                        borrando = true
                        errorEliminar = null
                        scope.launch {
                            try {
                                viewModel.eliminarUsuario(id)
                                borrando = false
                                usuarioAEliminar = null
                            } catch (e: retrofit2.HttpException) {
                                borrando = false
                                errorEliminar = detalleHttp(e)
                            } catch (e: Exception) {
                                borrando = false
                                errorEliminar = "No se pudo conectar al servidor."
                            }
                        }
                    },
                    onCancelar = { if (!borrando) { usuarioAEliminar = null; errorEliminar = null } },
                    borrando = borrando
                )
            }
        }
    }
}

@Composable
private fun MenuUsuarios(
    estado: EstadoCarga<List<UsuarioApi>>,
    onInstructores: () -> Unit,
    onAprendices: () -> Unit
) {
    val colors = ColoresAppLocal.current
    val datos = (estado as? EstadoCarga.Success)?.datos.orEmpty()
    val nIns = datos.count { it.role?.rol_name.equals("Instructor", ignoreCase = true) }
    val nApr = datos.count { it.role?.rol_name.equals("Aprendiz", ignoreCase = true) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Top
    ) {
        CabeceraPlegable(
            title = "Usuarios",
            subtitle = if (estado is EstadoCarga.Loading) "Cargando personal..." else "${datos.size} registrados • $nIns instructores • $nApr aprendices",
            scrollOffset = 0f
        )
        Spacer(modifier = Modifier.height(12.dp))
        CategoriaUsuarioRow(
            titulo = "Instructores",
            subtitulo = if (estado is EstadoCarga.Loading) "Cargando..." else "$nIns registrados",
            icono = Icons.Default.School,
            onClick = onInstructores
        )
        Spacer(modifier = Modifier.height(14.dp))
        CategoriaUsuarioRow(
            titulo = "Aprendices",
            subtitulo = if (estado is EstadoCarga.Loading) "Cargando..." else "$nApr registrados",
            icono = Icons.Default.Person,
            onClick = onAprendices
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Busca por nombre, documento, correo o ficha dentro de cada grupo.",
            color = colors.textSecondary,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun CategoriaUsuarioRow(
    titulo: String,
    icono: ImageVector,
    onClick: () -> Unit,
    subtitulo: String = ""
) {
    val colors = ColoresAppLocal.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .escalaPresion(pressedScale = 0.96f)
            .superficieVidrio(cornerRadius = RadioVidrio)
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(VerdeSena.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = verdeMarca(), modifier = Modifier.size(26.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            if (subtitulo.isNotBlank()) Text(subtitulo, color = colors.textSecondary, fontSize = 12.sp)
        }
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = verdeMarca(), modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun VistaListaUsuarios(
    titulo: String,
    icono: ImageVector,
    estado: EstadoCarga<List<UsuarioApi>>,
    rol: String,
    busqueda: String,
    onBusqueda: (String) -> Unit,
    onBack: () -> Unit,
    onReintentar: () -> Unit,
    onAgregarUsuario: () -> Unit,
    onEditar: (UsuarioApi) -> Unit,
    onBorrar: (UsuarioApi) -> Unit
) {
    val colors = ColoresAppLocal.current
    var ordenAZ by remember { mutableStateOf(true) }
    var mostrarFiltros by remember { mutableStateOf(false) }
    var filtroTipoDoc by remember { mutableStateOf<String?>(null) }
    var filtroPrograma by remember { mutableStateOf("") }
    var filtroFicha by remember { mutableStateOf("") }
    var borradorTipoDoc by remember { mutableStateOf<String?>(null) }
    var borradorPrograma by remember { mutableStateOf("") }
    var borradorFicha by remember { mutableStateOf("") }
    val nFiltrosActivos = (if (filtroTipoDoc != null) 1 else 0) +
        (if (filtroPrograma.isNotBlank()) 1 else 0) +
        (if (filtroFicha.isNotBlank()) 1 else 0)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = verdeMarca())
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Lista de $titulo registrados", color = colors.textSecondary, fontSize = 12.sp)
            }
            IconButton(
                onClick = onAgregarUsuario,
                modifier = Modifier.size(48.dp).clip(CircleShape).background(VerdeSena)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Agregar usuario", tint = Color.Black, modifier = Modifier.size(22.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) {
                BuscadorSena(
                    valor = busqueda,
                    onValor = onBusqueda,
                    placeholder = "Buscar por nombre, documento, correo o ficha..."
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(contentAlignment = Alignment.TopEnd) {
                FilledTonalButton(
                    onClick = {
                        borradorTipoDoc = filtroTipoDoc
                        borradorPrograma = filtroPrograma
                        borradorFicha = filtroFicha
                        mostrarFiltros = true
                    },
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = colors.surfaceVariant)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = "Filtros", tint = if (nFiltrosActivos > 0) verdeMarca() else colors.textSecondary, modifier = Modifier.size(22.dp))
                }
                if (nFiltrosActivos > 0) {
                    Box(
                        modifier = Modifier.size(20.dp).clip(CircleShape).background(VerdeSena).align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(nFiltrosActivos.toString(), color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            FilterChip(
                selected = ordenAZ,
                onClick = { ordenAZ = true },
                label = { Text("A-Z") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = !ordenAZ,
                onClick = { ordenAZ = false },
                label = { Text("Recientes") }
            )
            if (nFiltrosActivos > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = true,
                    onClick = { filtroTipoDoc = null; filtroPrograma = ""; filtroFicha = "" },
                    label = { Text("Limpiar filtros ($nFiltrosActivos)") },
                    trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) }
                )
            }
        }

        if (mostrarFiltros) {
            AlertDialog(
                onDismissRequest = { mostrarFiltros = false },
                containerColor = colors.cardBackground.copy(alpha = 0.98f),
                shape = RoundedCornerShape(28.dp),
                title = { Text("Filtrar $titulo", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.verticalScroll(rememberScrollState()).campoVisible()
                    ) {
                        Text("Tipo de documento", color = colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = borradorTipoDoc == null, onClick = { borradorTipoDoc = null }, label = { Text("Todos") })
                            listOf("CC", "CE", "TI", "PAS").forEach { t ->
                                FilterChip(selected = borradorTipoDoc == t, onClick = { borradorTipoDoc = t }, label = { Text(t) })
                            }
                        }
                        OutlinedTextField(
                            value = borradorPrograma,
                            onValueChange = { borradorPrograma = it },
                            label = { Text("Programa de formación") },
                            placeholder = { Text("ej. ADSO") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = borradorFicha,
                            onValueChange = { v -> borradorFicha = v.filter { it.isDigit() } },
                            label = { Text("Ficha") },
                            placeholder = { Text("ej. 285321") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            filtroTipoDoc = borradorTipoDoc
                            filtroPrograma = borradorPrograma.trim()
                            filtroFicha = borradorFicha.trim()
                            mostrarFiltros = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = Color.Black),
                        shape = RoundedCornerShape(28.dp)
                    ) { Text("Aplicar", fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        borradorTipoDoc = null; borradorPrograma = ""; borradorFicha = ""
                        filtroTipoDoc = null; filtroPrograma = ""; filtroFicha = ""
                        mostrarFiltros = false
                    }) { Text("Limpiar todo", color = colors.textSecondary) }
                }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        when (estado) {
            is EstadoCarga.Loading -> {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = verdeMarca(), modifier = Modifier.size(32.dp))
                }
            }
            is EstadoCarga.Error -> {
                VistaVacia(
                    icono = Icons.Default.Warning,
                    titulo = "No se pudo cargar",
                    mensaje = estado.mensaje
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onReintentar,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = Color.Black)
                ) { Text("REINTENTAR", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            }
            is EstadoCarga.Success -> {
                val q = busqueda.trim()
                val progQ = filtroPrograma.trim()
                val base = estado.datos.filter { it.role?.rol_name.equals(rol, ignoreCase = true) }
                val filtrados = base
                    .filter { u ->
                        val pasaTexto = if (q.isBlank()) true
                        else (u.user_name?.contains(q, ignoreCase = true) == true ||
                            u.user_lastname?.contains(q, ignoreCase = true) == true ||
                            u.nombreCompleto.contains(q, ignoreCase = true) ||
                            u.user_identification?.contains(q, ignoreCase = true) == true ||
                            u.user_email?.contains(q, ignoreCase = true) == true ||
                            u.user_coursenumber?.toString()?.contains(q) == true ||
                            u.user_program?.contains(q, ignoreCase = true) == true)
                        val pasaDoc = filtroTipoDoc == null || u.user_documento_tipo?.equals(filtroTipoDoc, ignoreCase = true) == true
                        val pasaProg = progQ.isBlank() || (u.user_program?.contains(progQ, ignoreCase = true) == true)
                        val pasaFicha = filtroFicha.isBlank() || (u.user_coursenumber?.toString()?.contains(filtroFicha) == true)
                        pasaTexto && pasaDoc && pasaProg && pasaFicha
                    }
                    .let { lista ->
                        if (ordenAZ) lista.sortedBy { it.nombreCompleto.lowercase() }
                        else lista.sortedByDescending { it.id_usuario ?: 0 }
                    }

                Text(
                    "${filtrados.size} de ${base.size} $titulo",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (filtrados.isEmpty()) {
                    VistaVacia(
                        icono = icono,
                        titulo = "Sin $titulo registrados",
                        mensaje = if (busqueda.isBlank()) {
                            "Usa el botón + para agregar el primero."
                        } else {
                            "No hay $titulo que coincidan con \"$busqueda\"."
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(
                            items = filtrados,
                            key = { u -> u.id_usuario ?: u.user_email ?: u.hashCode().toString() }
                        ) { usuario ->
                            FilaUsuario(
                                usuario,
                                onEditar = { onEditar(usuario) },
                                onBorrar = { onBorrar(usuario) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// permite cancelar antes de ejecutar el DELETE /admin/users/{id}.
@Composable
private fun EliminarUsuarioDialog(
    usuario: UsuarioApi,
    error: String? = null,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    borrando: Boolean = false
) {
    val colors = ColoresAppLocal.current
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = colors.cardBackground.copy(alpha = 0.98f),
        shape = RoundedCornerShape(28.dp),
        icon = { Icon(Icons.Default.Delete, contentDescription = "Eliminar usuario", tint = Color.Red, modifier = Modifier.size(36.dp)) },
        title = { Text("Eliminar usuario", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "¿Seguro que deseas eliminar a ${usuario.nombreCompleto}? " +
                        "Esta acción no se puede deshacer.",
                    color = colors.textSecondary
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error, color = Color.Red, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                enabled = !borrando,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White),
                shape = RoundedCornerShape(28.dp)
            ) { Text(if (borrando) "Eliminando..." else "Eliminar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar", color = colors.textSecondary) }
        }
    )
}

@Composable
fun FilaUsuario(usuario: UsuarioApi, onEditar: () -> Unit, onBorrar: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ColoresAppLocal.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .superficieVidrio(cornerRadius = RadioVidrio)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FotoPerfil(fotoPath = usuario.profile_photo_path, nombre = usuario.nombreCompleto, tamano = 52.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                usuario.nombreCompleto,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val ficha = usuario.user_coursenumber ?: 0
            val detalle = buildString {
                append("${usuario.user_documento_tipo ?: "CC"} ${usuario.user_identification ?: "—"}")
                if (ficha > 0) append(" • Ficha $ficha")
                if (!usuario.user_program.isNullOrBlank()) append(" • ${usuario.user_program}")
            }
            Text(detalle, color = colors.textSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                usuario.user_email ?: "",
                color = colors.textSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onEditar, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Edit, contentDescription = "Editar usuario", tint = verdeMarca(), modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onBorrar, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Eliminar usuario", tint = Color.Red, modifier = Modifier.size(20.dp))
        }
    }
}
