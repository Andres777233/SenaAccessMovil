package com.example.sennaccess.admin.ambientes

// Pantalla de administración de ambientes (solo admin).
// Permite crear ambientes, asignar instructores (multi, un instructor en varios ambientes)
// y gestionar la matrícula de aprendices por ambiente. El instructor luego también
// puede gestionar aprendices de sus propios ambientes.

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.data.Ambiente
import com.example.sennaccess.data.AmbienteRepository
import com.example.sennaccess.data.AmbienteRequest
import com.example.sennaccess.data.SessionManager
import com.example.sennaccess.data.UsuarioApi
import com.example.sennaccess.ui.CargaUiState
import com.example.sennaccess.ui.CargandoBox
import com.example.sennaccess.ui.ErrorBox
import com.example.sennaccess.ui.ios.GlassCornerRadius
import com.example.sennaccess.ui.ios.glassSurface
import com.example.sennaccess.ui.ios.IosCollapsibleHeader
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import com.example.sennaccess.ui.theme.ErrorRed
import kotlinx.coroutines.launch

@Composable
fun AmbientesAdminView(onBack: () -> Unit) {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    val repo = remember { AmbienteRepository() }
    val token = SessionManager.token

    var ambientes by remember { mutableStateOf<List<Ambiente>>(emptyList()) }
    var estado by remember { mutableStateOf<CargaUiState<List<Ambiente>>>(CargaUiState.Loading) }
    var instructores by remember { mutableStateOf<List<UsuarioApi>>(emptyList()) }
    var mostrarCrear by remember { mutableStateOf(false) }
    var ambienteAEditar by remember { mutableStateOf<Ambiente?>(null) }
    var ambienteAEliminar by remember { mutableStateOf<Ambiente?>(null) }
    var errorEliminar by remember { mutableStateOf<String?>(null) }
    var eliminando by remember { mutableStateOf(false) }

    suspend fun cargar() {
        if (token == null) { estado = CargaUiState.Error("Sin sesión"); return }
        try {
            estado = CargaUiState.Loading
            val lista = repo.getAmbientes(token)
            ambientes = lista
            estado = CargaUiState.Success(lista)
        } catch (e: Exception) {
            estado = CargaUiState.Error(e.message ?: "Error")
        }
    }
    suspend fun cargarInstructores() {
        if (token == null) return
        try {
            val repoUser = com.example.sennaccess.data.UsuarioRepository()
            val lista = repoUser.getUsers(token)
            instructores = lista.filter { it.role?.rol_name.equals("Instructor", true) }
        } catch (_: Exception) {}
    }

    LaunchedEffect(Unit) { cargar(); cargarInstructores() }

    if (mostrarCrear || ambienteAEditar != null) {
        AmbienteFormDialog(
            ambiente = ambienteAEditar,
            instructoresDisponibles = instructores,
            onDismiss = { mostrarCrear = false; ambienteAEditar = null },
            onGuardado = {
                mostrarCrear = false; ambienteAEditar = null
                scope.launch { cargar() }
            }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = colors.textPrimary) }
            Text("Ambientes", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(start = 4.dp))
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { scope.launch { cargar() } }) { Icon(Icons.Default.Refresh, null, tint = colors.textSecondary) }
        }
        IosCollapsibleHeader(title = "Ambientes", subtitle = "Crea ambientes y asigna instructores (uno en varios salones, ej. CCyS y Ciudad Jardín)", scrollOffset = 0f)
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { mostrarCrear = true },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("CREAR AMBIENTE", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))

        when (val s = estado) {
            is CargaUiState.Loading -> CargandoBox()
            is CargaUiState.Error -> ErrorBox(s.mensaje, onReintentar = { scope.launch { cargar() } })
            is CargaUiState.Success -> {
                if (s.datos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().glassSurface(cornerRadius = GlassCornerRadius).padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.MeetingRoom, null, tint = colors.textSecondary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No hay ambientes", color = colors.textPrimary, fontWeight = FontWeight.Bold)
                            Text("Crea el primero para asignar instructores y estudiantes.", color = colors.textSecondary, fontSize = 12.sp)
                        }
                    }
                } else {
                    s.datos.forEach { amb ->
                        AmbienteAdminCard(
                            ambiente = amb,
                            onEditar = { ambienteAEditar = amb },
                            onEliminar = { ambienteAEliminar = amb }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }

    // Diálogo de eliminación.
    ambienteAEliminar?.let { amb ->
        AlertDialog(
            onDismissRequest = { ambienteAEliminar = null; errorEliminar = null },
            containerColor = colors.cardBackground.copy(alpha = 0.98f),
            shape = RoundedCornerShape(24.dp),
            icon = { Icon(Icons.Default.Delete, null, tint = ErrorRed, modifier = Modifier.size(36.dp)) },
            title = { Text("Eliminar ambiente", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("¿Seguro que deseas eliminar \"${amb.ambiente_nombre}\" y sus asignaciones?", color = colors.textSecondary)
                    if (errorEliminar != null) { Spacer(modifier = Modifier.height(8.dp)); Text(errorEliminar!!, color = ErrorRed, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (eliminando) return@Button
                    val id = amb.id_ambiente ?: return@Button
                    val t = token ?: return@Button
                    eliminando = true
                    scope.launch {
                        try {
                            repo.deleteAmbiente(t, id)
                            eliminando = false; ambienteAEliminar = null; errorEliminar = null; cargar()
                        } catch (e: retrofit2.HttpException) { eliminando = false; errorEliminar = "Error ${e.code()}: ${e.message()}" }
                        catch (e: Exception) { eliminando = false; errorEliminar = "Fallo de conexión" }
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White), shape = RoundedCornerShape(12.dp)) { Text(if (eliminando) "Eliminando..." else "Eliminar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { ambienteAEliminar = null }) { Text("Cancelar", color = colors.textSecondary) } }
        )
    }
}

@Composable
private fun AmbienteAdminCard(ambiente: Ambiente, onEditar: () -> Unit, onEliminar: () -> Unit) {
    val colors = LocalAppColors.current
    Box(modifier = Modifier.fillMaxWidth().glassSurface(cornerRadius = GlassCornerRadius).padding(14.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(SenaGreen.copy(0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MeetingRoom, null, tint = SenaGreen, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(ambiente.ambiente_nombre ?: "Sin nombre", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!ambiente.ambiente_ubicacion.isNullOrBlank()) Text(ambiente.ambiente_ubicacion!!, color = colors.textSecondary, fontSize = 12.sp)
                        if (!ambiente.ambiente_jornada.isNullOrBlank()) { Spacer(modifier = Modifier.width(8.dp)); Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(SenaGreen.copy(0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text(ambiente.ambiente_jornada!!, color = SenaGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold) } }
                    }
                }
                IconButton(onClick = onEditar) { Icon(Icons.Default.Edit, null, tint = colors.textSecondary) }
                IconButton(onClick = onEliminar) { Icon(Icons.Default.Delete, null, tint = ErrorRed) }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Person, null, tint = SenaGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                val nombres = ambiente.instructores?.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.nombreCompleto } ?: "Sin instructor asignado"
                Text(nombres, color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Groups, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("${ambiente.aprendices_count ?: ambiente.aprendices?.size ?: 0} aprendices", color = colors.textSecondary, fontSize = 12.sp)
                if (ambiente.ambiente_capacidad != null) { Spacer(modifier = Modifier.width(8.dp)); Text("• cap. ${ambiente.ambiente_capacidad}", color = colors.textSecondary, fontSize = 12.sp) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmbienteFormDialog(ambiente: Ambiente?, instructoresDisponibles: List<UsuarioApi>, onDismiss: () -> Unit, onGuardado: () -> Unit) {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    val repo = remember { AmbienteRepository() }
    val token = SessionManager.token

    var nombre by remember { mutableStateOf(ambiente?.ambiente_nombre ?: "") }
    var capacidad by remember { mutableStateOf(ambiente?.ambiente_capacidad?.toString() ?: "") }
    var ubicacion by remember { mutableStateOf(ambiente?.ambiente_ubicacion ?: "") }
    var jornada by remember { mutableStateOf(ambiente?.ambiente_jornada ?: "Tarde") }
    var horaInicio by remember { mutableStateOf(ambiente?.hora_inicio ?: "13:00") }
    var horaFin by remember { mutableStateOf(ambiente?.hora_fin ?: "18:00") }
    var seleccionados by remember { mutableStateOf(ambiente?.instructores?.mapNotNull { it.id_usuario }?.toSet() ?: emptySet()) }
    var error by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }
    val jornadas = listOf("Mañana", "Tarde", "Noche")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.cardBackground.copy(alpha = 0.98f),
        shape = RoundedCornerShape(24.dp),
        title = { Text(if (ambiente == null) "Crear ambiente" else "Editar ambiente", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre *") }, placeholder = { Text("Ambiente 101") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = capacidad, onValueChange = { capacidad = it }, label = { Text("Capacidad") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = ubicacion, onValueChange = { ubicacion = it }, label = { Text("Ubicación") }, placeholder = { Text("CCyS / Ciudad Jardín") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Spacer(modifier = Modifier.height(8.dp))
                var menuJornada by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = menuJornada, onExpandedChange = { menuJornada = !menuJornada }) {
                    OutlinedTextField(value = jornada, onValueChange = {}, readOnly = true, label = { Text("Jornada") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuJornada) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = menuJornada, onDismissRequest = { menuJornada = false }) {
                        jornadas.forEach { j -> DropdownMenuItem(text = { Text(j) }, onClick = { jornada = j; menuJornada = false }, leadingIcon = { Icon(Icons.Default.Check, null, tint = if (jornada == j) SenaGreen else Color.Transparent, modifier = Modifier.size(18.dp)) }) }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = horaInicio, onValueChange = { horaInicio = it }, label = { Text("Hora inicio") }, placeholder = { Text("13:00") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = horaFin, onValueChange = { horaFin = it }, label = { Text("Hora fin") }, placeholder = { Text("18:00") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Instructores asignados (uno puede estar en varios ambientes)", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                if (instructoresDisponibles.isEmpty()) {
                    Text("No hay instructores disponibles", color = colors.textSecondary, fontSize = 12.sp)
                } else {
                    instructoresDisponibles.forEach { inst ->
                        val sel = seleccionados.contains(inst.id_usuario)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { val id = inst.id_usuario ?: return@clickable; seleccionados = if (sel) seleccionados - id else seleccionados + id }.padding(6.dp)) {
                            Checkbox(checked = sel, onCheckedChange = { v -> val id = inst.id_usuario ?: return@Checkbox; seleccionados = if (v) seleccionados + id else seleccionados - id })
                            Text(inst.nombreCompleto, color = colors.textPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Text(inst.user_email ?: "", color = colors.textSecondary, fontSize = 11.sp)
                        }
                    }
                }
                if (error != null) { Spacer(modifier = Modifier.height(8.dp)); Text(error!!, color = ErrorRed, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (nombre.isBlank()) { error = "El nombre es obligatorio"; return@Button }
                if (token == null) { error = "Sin sesión"; return@Button }
                guardando = true
                error = null
                val req = AmbienteRequest(
                    ambiente_nombre = nombre.trim(),
                    ambiente_capacidad = capacidad.toIntOrNull(),
                    ambiente_ubicacion = ubicacion.trim().ifBlank { null },
                    ambiente_estado = "Activo",
                    ambiente_jornada = jornada,
                    hora_inicio = horaInicio.trim().ifBlank { null },
                    hora_fin = horaFin.trim().ifBlank { null },
                    instructores = seleccionados.toList()
                )
                scope.launch {
                    try {
                        if (ambiente == null) repo.createAmbiente(token, req) else repo.updateAmbiente(token, ambiente.id_ambiente!!, req)
                        guardando = false; onGuardado()
                    } catch (e: retrofit2.HttpException) { guardando = false; error = "Error ${e.code()}: ${e.message()}" }
                    catch (e: Exception) { guardando = false; error = "Fallo de conexión: ${e.message}" }
                }
            }, enabled = !guardando, colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black), shape = RoundedCornerShape(12.dp)) { if (guardando) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Guardar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
