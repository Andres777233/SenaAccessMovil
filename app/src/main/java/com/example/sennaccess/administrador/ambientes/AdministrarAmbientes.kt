package com.example.sennaccess.administrador.ambientes

// Pantalla de administración de ambientes (solo admin).
// Permite crear ambientes, asignar instructores (multi, un instructor en varios ambientes)
// y gestionar la matrícula de aprendices por ambiente. El instructor luego también
// puede gestionar aprendices de sus propios ambientes.

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.datos.modelos.Ambiente
import com.example.sennaccess.datos.repositorios.RepositorioAmbientes
import com.example.sennaccess.datos.modelos.PeticionAmbiente
import com.example.sennaccess.datos.sesion.GestorSesion
import com.example.sennaccess.datos.modelos.UsuarioApi
import com.example.sennaccess.comun.EstadoCarga
import com.example.sennaccess.comun.CajaCargando
import com.example.sennaccess.comun.CajaError
import com.example.sennaccess.comun.campoVisible
import com.example.sennaccess.comun.diseno.RadioVidrio
import com.example.sennaccess.comun.diseno.RadioSena
import com.example.sennaccess.comun.diseno.superficiePlana
import com.example.sennaccess.comun.diseno.escalaPresion
import com.example.sennaccess.comun.diseno.EntradaSuave
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import com.example.sennaccess.comun.tema.RojoError
import kotlinx.coroutines.launch

@Composable
fun VistaAdministrarAmbientes(onBack: () -> Unit) {
    val colors = ColoresAppLocal.current
    val scope = rememberCoroutineScope()
    val repo = remember { RepositorioAmbientes() }
    val token = GestorSesion.token

    var ambientes by remember { mutableStateOf<List<Ambiente>>(emptyList()) }
    var estado by remember { mutableStateOf<EstadoCarga<List<Ambiente>>>(EstadoCarga.Loading) }
    var instructores by remember { mutableStateOf<List<UsuarioApi>>(emptyList()) }
    var mostrarCrear by remember { mutableStateOf(false) }
    var ambienteAEditar by remember { mutableStateOf<Ambiente?>(null) }
    var ambienteAEliminar by remember { mutableStateOf<Ambiente?>(null) }
    var errorEliminar by remember { mutableStateOf<String?>(null) }
    var eliminando by remember { mutableStateOf(false) }

    suspend fun cargar() {
        if (token == null) { estado = EstadoCarga.Error("Sin sesión"); return }
        try {
            estado = EstadoCarga.Loading
            val lista = repo.getAmbientes(token)
            ambientes = lista
            estado = EstadoCarga.Success(lista)
        } catch (e: Exception) {
            estado = EstadoCarga.Error(e.message ?: "Error")
        }
    }
    suspend fun cargarInstructores() {
        if (token == null) return
        try {
            val repoUser = com.example.sennaccess.datos.repositorios.RepositorioUsuarios()
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

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.ArrowBack, null, tint = colors.textPrimary) }
            Column(modifier = Modifier.weight(1f)) {
                Text("Ambientes", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Salones y sus instructores asignados", color = colors.textSecondary, fontSize = 12.sp)
            }
            IconButton(onClick = { scope.launch { cargar() } }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Refresh, null, tint = colors.textSecondary) }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { mostrarCrear = true },
            modifier = Modifier.fillMaxWidth().height(52.dp).escalaPresion(pressedScale = 0.97f),
            colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = Color.Black),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("CREAR AMBIENTE", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))

        when (val s = estado) {
            is EstadoCarga.Loading -> CajaCargando()
            is EstadoCarga.Error -> CajaError(s.mensaje, onReintentar = { scope.launch { cargar() } })
            is EstadoCarga.Success -> {
                if (s.datos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().superficiePlana(cornerRadius = RadioVidrio).padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.MeetingRoom, null, tint = colors.textSecondary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No hay ambientes", color = colors.textPrimary, fontWeight = FontWeight.Bold)
                            Text("Crea el primero para asignar instructores y estudiantes.", color = colors.textSecondary, fontSize = 12.sp)
                        }
                    }
                } else {
                    // Tarjetas separadas con aire: cada ambiente en su propia
                    // plana con 14dp entre sí para que no se confundan.
                    Text("AMBIENTES • ${s.datos.size}", color = colors.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                        s.datos.forEachIndexed { i, amb ->
                            EntradaSuave(indice = i.coerceAtMost(4)) {
                                AmbienteAdminCard(
                                    ambiente = amb,
                                    onEditar = { ambienteAEditar = amb },
                                    onEliminar = { ambienteAEliminar = amb }
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }

    ambienteAEliminar?.let { amb ->
        AlertDialog(
            onDismissRequest = { ambienteAEliminar = null; errorEliminar = null },
            containerColor = colors.cardBackground.copy(alpha = 0.98f),
            shape = RoundedCornerShape(24.dp),
            icon = { Icon(Icons.Default.Delete, null, tint = RojoError, modifier = Modifier.size(36.dp)) },
            title = { Text("Eliminar ambiente", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("¿Seguro que deseas eliminar \"${amb.ambiente_nombre}\" y sus asignaciones?", color = colors.textSecondary)
                    if (errorEliminar != null) { Spacer(modifier = Modifier.height(8.dp)); Text(errorEliminar!!, color = RojoError, fontSize = 12.sp) }
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
                }, colors = ButtonDefaults.buttonColors(containerColor = RojoError, contentColor = Color.Black), shape = RoundedCornerShape(28.dp)) { Text(if (eliminando) "Eliminando..." else "Eliminar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { ambienteAEliminar = null }) { Text("Cancelar", color = colors.textSecondary) } }
        )
    }
}

@Composable
private fun AmbienteAdminCard(ambiente: Ambiente, onEditar: () -> Unit, onEliminar: () -> Unit) {
    val colors = ColoresAppLocal.current
    // Tarjeta individual en plana: identidad + instructores + ocupación.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .superficiePlana(cornerRadius = RadioVidrio)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(VerdeSena.copy(0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MeetingRoom, null, tint = verdeMarca(), modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(ambiente.ambiente_nombre ?: "Sin nombre", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!ambiente.ambiente_ubicacion.isNullOrBlank()) Text(ambiente.ambiente_ubicacion!!, color = colors.textSecondary, fontSize = 12.sp, maxLines = 1)
                        val jornadaAmb = com.example.sennaccess.comun.Jornadas.normalizar(ambiente.ambiente_jornada)
                        if (jornadaAmb != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.clip(CircleShape).background(VerdeSena.copy(0.15f)).padding(horizontal = 9.dp, vertical = 3.dp)) {
                                Text(jornadaAmb, color = verdeMarca(), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            com.example.sennaccess.comun.Jornadas.textoHorario(jornadaAmb)?.let { franja ->
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(franja, color = colors.textSecondary, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
                IconButton(onClick = onEditar, modifier = Modifier.size(44.dp)) { Icon(Icons.Default.Edit, contentDescription = "Editar ambiente", tint = verdeMarca(), modifier = Modifier.size(20.dp)) }
                IconButton(onClick = onEliminar, modifier = Modifier.size(44.dp)) { Icon(Icons.Default.Delete, contentDescription = "Eliminar ambiente", tint = RojoError, modifier = Modifier.size(20.dp)) }
            }
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = colors.divider)
            Spacer(modifier = Modifier.height(10.dp))
            // Instructores responsables: nombre en letra grande + jornada.
            val jornadaAmb = com.example.sennaccess.comun.Jornadas.normalizar(ambiente.ambiente_jornada)
            val asignados = ambiente.instructores.orEmpty()
            if (asignados.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Person, null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sin instructor asignado", color = colors.textSecondary, fontSize = 14.sp)
                }
            } else {
                asignados.forEach { inst ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Icon(Icons.Default.Person, null, tint = verdeMarca(), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            inst.nombreCompleto,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        if (jornadaAmb != null) {
                            Box(modifier = Modifier.clip(CircleShape).background(VerdeSena.copy(0.15f)).padding(horizontal = 9.dp, vertical = 3.dp)) {
                                Text(jornadaAmb, color = verdeMarca(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Groups, null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("${ambiente.aprendices_count ?: ambiente.aprendices?.size ?: 0} aprendices", color = colors.textSecondary, fontSize = 13.sp)
                if (ambiente.ambiente_capacidad != null) { Spacer(modifier = Modifier.width(8.dp)); Text("• cap. ${ambiente.ambiente_capacidad}", color = colors.textSecondary, fontSize = 13.sp) }
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmbienteFormDialog(ambiente: Ambiente?, instructoresDisponibles: List<UsuarioApi>, onDismiss: () -> Unit, onGuardado: () -> Unit) {
    val colors = ColoresAppLocal.current
    val scope = rememberCoroutineScope()
    val repo = remember { RepositorioAmbientes() }
    val token = GestorSesion.token

    var nombre by remember { mutableStateOf(ambiente?.ambiente_nombre ?: "") }
    var capacidad by remember { mutableStateOf(ambiente?.ambiente_capacidad?.toString() ?: "") }
    var ubicacion by remember { mutableStateOf(ambiente?.ambiente_ubicacion ?: "") }
    // Jornada normalizada: si el registro viejo trae otra cosa, se deduce de la
    // hora de inicio (o queda Tarde). El horario lo define la jornada.
    var jornada by remember {
        mutableStateOf(
            com.example.sennaccess.comun.Jornadas.normalizar(ambiente?.ambiente_jornada)
                ?: com.example.sennaccess.comun.Jornadas.normalizar(
                    run {
                        val h = com.example.sennaccess.comun.Jornadas.horaCorta(ambiente?.hora_inicio)
                        when {
                            h == null -> null
                            h < "12:00" -> "Mañana"
                            h < "18:00" -> "Tarde"
                            else -> "Noche"
                        }
                    }
                )
                ?: "Tarde"
        )
    }
    var seleccionados by remember { mutableStateOf(ambiente?.instructores?.mapNotNull { it.id_usuario }?.toSet() ?: emptySet()) }
    var error by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }

    // Horario fijo por jornada (no editable): mañana 6 a.m.–1 p.m., tarde
    // 1–6 p.m., noche 6–10 p.m.
    val horarioJornada = com.example.sennaccess.comun.Jornadas.textoHorario(jornada) ?: ""

    // Vista completa de edición: ocupa casi toda la pantalla como una
    // pantalla aparte, no un diálogo pequeño.
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.9f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Cabecera de la vista con cerrar.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (ambiente == null) "NUEVO AMBIENTE" else "EDITAR AMBIENTE",
                            color = verdeMarca(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (ambiente == null) "Crear ambiente" else "Editar \"${ambiente?.ambiente_nombre ?: ""}\"",
                            color = colors.textPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            maxLines = 1
                        )
                        Text("Completa la ficha del salón y sus instructores", color = colors.textSecondary, fontSize = 12.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) { Icon(Icons.Default.Close, contentDescription = "Cerrar editor", tint = colors.textSecondary) }
                }
                HorizontalDivider(color = colors.divider)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .imePadding()
                ) {
                EtiquetaAmbiente("Identidad")
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre *") }, placeholder = { Text("Ambiente 101") }, modifier = Modifier.fillMaxWidth().campoVisible(), singleLine = true, shape = RoundedCornerShape(16.dp))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = capacidad, onValueChange = { capacidad = it }, label = { Text("Capacidad") }, modifier = Modifier.fillMaxWidth().campoVisible(), singleLine = true, shape = RoundedCornerShape(16.dp))
                Spacer(modifier = Modifier.height(8.dp))
                com.example.sennaccess.comun.diseno.DesplegableSena(
                    valor = ubicacion.ifBlank { "Elige la sede…" },
                    opciones = com.example.sennaccess.comun.UbicacionesSena.SEDES,
                    onElegir = { ubicacion = it },
                    label = "Ubicación / sede *"
                )
                Spacer(modifier = Modifier.height(12.dp))
                EtiquetaAmbiente("Horario")
                Spacer(modifier = Modifier.height(6.dp))
                com.example.sennaccess.comun.diseno.DesplegableSena(
                    valor = jornada,
                    opciones = com.example.sennaccess.comun.Jornadas.TODAS,
                    onElegir = { jornada = it },
                    label = "Jornada"
                )
                Spacer(modifier = Modifier.height(8.dp))
                // El horario lo fija la jornada: solo se muestra, no se edita.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .superficiePlana(cornerRadius = RadioSena.md)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = verdeMarca(),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Horario de la jornada", color = colors.textSecondary, fontSize = 11.sp)
                        Text(horarioJornada, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "La jornada define la hora de inicio y fin; no se edita a mano.",
                    color = colors.textSecondary, fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                EtiquetaAmbiente("Instructores")
                Spacer(modifier = Modifier.height(2.dp))
                Text("Uno puede estar en varios ambientes", color = colors.textSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                if (instructoresDisponibles.isEmpty()) {
                    Text("No hay instructores disponibles", color = colors.textSecondary, fontSize = 12.sp)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .superficiePlana(cornerRadius = RadioSena.md)
                            .padding(vertical = 4.dp)
                    ) {
                        instructoresDisponibles.forEachIndexed { i, inst ->
                            if (i > 0) HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = 10.dp))
                            val sel = seleccionados.contains(inst.id_usuario)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { val id = inst.id_usuario ?: return@clickable; seleccionados = if (sel) seleccionados - id else seleccionados + id }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Checkbox(
                                    checked = sel,
                                    onCheckedChange = { v -> val id = inst.id_usuario ?: return@Checkbox; seleccionados = if (v) seleccionados + id else seleccionados - id },
                                    colors = CheckboxDefaults.colors(checkedColor = VerdeSena, checkmarkColor = Color.Black)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(inst.nombreCompleto, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    if (!inst.user_email.isNullOrBlank()) Text(inst.user_email!!, color = colors.textSecondary, fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
                if (error != null) { Spacer(modifier = Modifier.height(8.dp)); Text(error!!, color = RojoError, fontSize = 12.sp) }
                }
                HorizontalDivider(color = colors.divider)
                // Pie de la vista: Cancelar + Guardar a 52dp.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Cancelar", fontWeight = FontWeight.Bold) }
                    Button(onClick = {
                        if (guardando) return@Button
                        if (nombre.isBlank()) { error = "El nombre es obligatorio"; return@Button }
                        if (ubicacion.isBlank() || ubicacion == "Elige la sede…") { error = "Elige la ubicación / sede del ambiente"; return@Button }
                        if (token == null) { error = "Sin sesión"; return@Button }
                        val capacidadNum = capacidad.trim().toIntOrNull()
                        if (capacidad.isNotBlank() && (capacidadNum == null || capacidadNum <= 0)) { error = "La capacidad debe ser un número mayor a 0"; return@Button }
                        guardando = true
                        error = null
                        val (horaInicio, horaFin) = com.example.sennaccess.comun.Jornadas.horasPara(jornada)
                        val req = PeticionAmbiente(
                            ambiente_nombre = nombre.trim(),
                            ambiente_capacidad = capacidadNum,
                            ambiente_ubicacion = ubicacion.trim().ifBlank { null },
                            // Conserva el estado real del salón: editar instructores
                            // no debe devolver un ambiente inactivo a "Activo".
                            ambiente_estado = ambiente?.ambiente_estado ?: "Activo",
                            ambiente_jornada = jornada,
                            hora_inicio = horaInicio,
                            hora_fin = horaFin,
                            instructores = seleccionados.toList()
                        )
                        scope.launch {
                            try {
                                if (ambiente == null) repo.createAmbiente(token, req) else repo.updateAmbiente(token, ambiente.id_ambiente!!, req)
                                guardando = false; onGuardado()
                            } catch (e: retrofit2.HttpException) {
                                guardando = false
                                error = com.example.sennaccess.comun.detalleHttp(e)
                            } catch (e: Exception) {
                                guardando = false
                                error = "No se pudo conectar: ${e.message ?: "error de red"}"
                            }
                        }
                    }, enabled = !guardando, colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = Color.Black), shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f).height(52.dp).escalaPresion(pressedScale = 0.97f)) { if (guardando) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp) else Text(if (ambiente == null) "CREAR AMBIENTE" else "GUARDAR CAMBIOS", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// Etiqueta de sección en el diálogo de ambiente: eyebrow verde + jerarquía.
@Composable
private fun EtiquetaAmbiente(texto: String) {
    Text(
        text = texto.uppercase(),
        color = verdeMarca(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.6.sp
    )
}
