package com.example.sennaccess.ambientes

// Detalle de un ambiente para el instructor: lista aprendices, agregar/quitar,
// y atajos a QR del aula y a autorizar salida.

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Approval
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
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
import com.example.sennaccess.data.SessionManager
import com.example.sennaccess.data.UsuarioApi
import com.example.sennaccess.ui.CargaUiState
import com.example.sennaccess.ui.CargandoBox
import com.example.sennaccess.ui.ErrorBox
import com.example.sennaccess.ui.ios.GlassCornerRadius
import com.example.sennaccess.ui.ios.glassSurface
import com.example.sennaccess.ui.theme.ErrorRed
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import kotlinx.coroutines.launch

@Composable
fun AmbienteDetalleView(
    ambiente: Ambiente,
    onBack: () -> Unit,
    onProyectarQr: (Ambiente) -> Unit,
    onAutorizarSalida: () -> Unit
) {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    val repo = remember { AmbienteRepository() }
    val token = SessionManager.token

    var aprendices by remember { mutableStateOf<List<UsuarioApi>>(emptyList()) }
    var estado by remember { mutableStateOf<CargaUiState<List<UsuarioApi>>>(CargaUiState.Loading) }
    var mostrarAgregar by remember { mutableStateOf(false) }
    var aprendicesDisponibles by remember { mutableStateOf<List<UsuarioApi>>(emptyList()) }
    var seleccionado by remember { mutableStateOf<UsuarioApi?>(null) }
    var menuDisponibles by remember { mutableStateOf(false) }
    var errorAgregar by remember { mutableStateOf<String?>(null) }
    var agregando by remember { mutableStateOf(false) }
    var aprendizAEliminar by remember { mutableStateOf<UsuarioApi?>(null) }

    suspend fun cargarAprendices() {
        if (token == null || ambiente.id_ambiente == null) { estado = CargaUiState.Error("Sin datos"); return }
        try {
            estado = CargaUiState.Loading
            val lista = repo.getMisAprendices(token, ambiente.id_ambiente!!)
            aprendices = lista
            estado = CargaUiState.Success(lista)
        } catch (e: Exception) { estado = CargaUiState.Error(e.message ?: "Error") }
    }
    suspend fun cargarDisponibles() {
        if (token == null) return
        try {
            val repoUser = com.example.sennaccess.data.UsuarioRepository()
            val lista = repoUser.getUsers(token)
            val idsYa = aprendices.mapNotNull { it.id_usuario }.toSet()
            aprendicesDisponibles = lista.filter { it.role?.rol_name.equals("Aprendiz", true) && it.id_usuario !in idsYa }
        } catch (_: Exception) {}
    }

    LaunchedEffect(ambiente.id_ambiente) { cargarAprendices() }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = colors.textPrimary) }
            Text(ambiente.ambiente_nombre ?: "Ambiente", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { scope.launch { cargarAprendices() } }) { Icon(Icons.Default.Refresh, null, tint = colors.textSecondary) }
        }
        Text("${ambiente.ambiente_ubicacion ?: ""} • ${ambiente.ambiente_jornada ?: ""} • cap. ${ambiente.ambiente_capacidad ?: "—"}", color = colors.textSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // Atajos jornada.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { onProyectarQr(ambiente) }, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black)) {
                Icon(Icons.Default.QrCode2, null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(6.dp)); Text("QR", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Button(onClick = onAutorizarSalida, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = SenaGreen.copy(0.18f), contentColor = SenaGreen)) {
                Icon(Icons.Default.Approval, null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(6.dp)); Text("Permiso salida", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Lista de aprendices.
        Text("APRENDICES (${aprendices.size})", color = SenaGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { mostrarAgregar = true; scope.launch { cargarDisponibles() } },
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SenaGreen.copy(0.18f), contentColor = SenaGreen)
        ) { Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(6.dp)); Text("AGREGAR APRENDIZ", fontWeight = FontWeight.Bold) }
        Spacer(modifier = Modifier.height(12.dp))

        when (estado) {
            is CargaUiState.Loading -> CargandoBox()
            is CargaUiState.Error -> ErrorBox((estado as CargaUiState.Error).mensaje, onReintentar = { scope.launch { cargarAprendices() } })
            is CargaUiState.Success -> {
                if (aprendices.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().glassSurface(cornerRadius = GlassCornerRadius).padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("No hay aprendices en este ambiente. Agrega los estudiantes de tu ficha.", color = colors.textSecondary, fontSize = 13.sp)
                    }
                } else {
                    aprendices.forEach { ap ->
                        Row(modifier = Modifier.fillMaxWidth().glassSurface(cornerRadius = 12.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(SenaGreen.copy(0.12f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, null, tint = SenaGreen, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ap.nombreCompleto, color = colors.textPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                Text("${ap.user_email ?: ""} • CC ${ap.user_identification ?: "—"}", color = colors.textSecondary, fontSize = 11.sp)
                            }
                            IconButton(onClick = { aprendizAEliminar = ap }) { Icon(Icons.Default.Delete, null, tint = ErrorRed, modifier = Modifier.size(18.dp)) }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }

    // Diálogo agregar.
    if (mostrarAgregar) {
        AlertDialog(
            onDismissRequest = { mostrarAgregar = false; errorAgregar = null; seleccionado = null },
            containerColor = colors.cardBackground.copy(alpha = 0.98f),
            shape = RoundedCornerShape(24.dp),
            title = { Text("Agregar aprendiz", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (aprendicesDisponibles.isEmpty()) {
                        Text("No hay aprendices disponibles", color = colors.textSecondary, fontSize = 13.sp)
                    } else {
                        Text("Aprendices disponibles (${aprendicesDisponibles.size})", color = colors.textSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        var expandir by remember { mutableStateOf(false) }
                        // Simple dropdown mimicked with list in dialog scroll
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).glassSurface(cornerRadius = 10.dp).padding(8.dp)) {
                            Column {
                                Text(seleccionado?.nombreCompleto ?: "Selecciona aprendiz", color = if (seleccionado == null) colors.textSecondary else colors.textPrimary, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { expandir = !expandir }.padding(8.dp))
                                if (expandir) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                                        items(aprendicesDisponibles.size) { idx ->
                                            val ap = aprendicesDisponibles[idx]
                                            Text("${ap.nombreCompleto} • ${ap.user_identification ?: ""}", color = colors.textPrimary, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().clickable { seleccionado = ap; expandir = false }.padding(8.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (errorAgregar != null) { Spacer(modifier = Modifier.height(8.dp)); Text(errorAgregar!!, color = ErrorRed, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val ap = seleccionado ?: run { errorAgregar = "Selecciona un aprendiz"; return@Button }
                    val t = token ?: run { errorAgregar = "Sin sesión"; return@Button }
                    val ambId = ambiente.id_ambiente ?: return@Button
                    if (agregando) return@Button
                    agregando = true; errorAgregar = null
                    scope.launch {
                        try {
                            repo.addMisAprendiz(t, ambId, ap.id_usuario!!)
                            agregando = false; mostrarAgregar = false; seleccionado = null; cargarAprendices(); cargarDisponibles()
                        } catch (e: retrofit2.HttpException) { agregando = false; errorAgregar = "Error ${e.code()}: ${e.message()}" }
                        catch (e: Exception) { agregando = false; errorAgregar = "Fallo de conexión: ${e.message}" }
                    }
                }, enabled = !agregando && seleccionado != null, colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black), shape = RoundedCornerShape(12.dp)) { if (agregando) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Agregar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { mostrarAgregar = false }) { Text("Cancelar") } }
        )
    }

    // Confirmar eliminación.
    aprendizAEliminar?.let { ap ->
        AlertDialog(
            onDismissRequest = { aprendizAEliminar = null },
            containerColor = colors.cardBackground.copy(alpha = 0.98f),
            shape = RoundedCornerShape(24.dp),
            title = { Text("¿Quitar aprendiz?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Se quitará a ${ap.nombreCompleto} de \"${ambiente.ambiente_nombre}\". No se elimina su cuenta, solo la matrícula en este salón.", color = colors.textSecondary) },
            confirmButton = {
                Button(onClick = {
                    val t = token ?: return@Button
                    val ambId = ambiente.id_ambiente ?: return@Button
                    val uid = ap.id_usuario ?: return@Button
                    scope.launch {
                        try { repo.removeMisAprendiz(t, ambId, uid); aprendizAEliminar = null; cargarAprendices() } catch (e: Exception) {}
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White), shape = RoundedCornerShape(12.dp)) { Text("Quitar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { aprendizAEliminar = null }) { Text("Cancelar") } }
        )
    }
}
