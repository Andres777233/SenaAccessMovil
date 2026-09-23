package com.example.sennaccess.instructor.ambientes

// Detalle de un ambiente del instructor: cabecera con ocupación, acciones
// lado a lado (agregar / permiso de salida) y estudiantes por ficha con foto.

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Approval
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.datos.modelos.Ambiente
import com.example.sennaccess.datos.repositorios.RepositorioAmbientes
import com.example.sennaccess.datos.sesion.GestorSesion
import com.example.sennaccess.datos.modelos.UsuarioApi
import com.example.sennaccess.perfil.FotoPerfil
import com.example.sennaccess.comun.EstadoCarga
import com.example.sennaccess.comun.CajaCargando
import com.example.sennaccess.comun.CajaError
import com.example.sennaccess.comun.detalleHttp
import com.example.sennaccess.comun.diseno.FiltroSena
import com.example.sennaccess.comun.diseno.RadioVidrio
import com.example.sennaccess.comun.diseno.superficieVidrio
import com.example.sennaccess.comun.tema.RojoError
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaDetalleAmbiente(
    ambiente: Ambiente,
    onBack: () -> Unit,
    onProyectarQr: ((Ambiente) -> Unit)? = null,
    onAutorizarSalida: () -> Unit
) {
    val colors = ColoresAppLocal.current
    val scope = rememberCoroutineScope()
    val repo = remember { RepositorioAmbientes() }
    val token = GestorSesion.token

    var aprendices by remember { mutableStateOf<List<UsuarioApi>>(emptyList()) }
    var estado by remember { mutableStateOf<EstadoCarga<List<UsuarioApi>>>(EstadoCarga.Loading) }
    var mostrarAgregar by remember { mutableStateOf(false) }
    var aprendicesDisponibles by remember { mutableStateOf<List<UsuarioApi>>(emptyList()) }
    var seleccionado by remember { mutableStateOf<UsuarioApi?>(null) }
    var errorAgregar by remember { mutableStateOf<String?>(null) }
    var agregando by remember { mutableStateOf(false) }
    var aprendizAEliminar by remember { mutableStateOf<UsuarioApi?>(null) }
    var quitando by remember { mutableStateOf(false) }
    var errorQuitar by remember { mutableStateOf<String?>(null) }
    var fichaKeySel by remember { mutableStateOf<Int?>(null) }

    suspend fun cargarAprendices() {
        val ambId = ambiente.id_ambiente
        if (token == null || ambId == null) { estado = EstadoCarga.Error("Sin datos"); return }
        try {
            estado = EstadoCarga.Loading
            val lista = repo.getMisAprendices(token, ambId)
            aprendices = lista
            estado = EstadoCarga.Success(lista)
        } catch (e: retrofit2.HttpException) { estado = EstadoCarga.Error(detalleHttp(e)) }
        catch (e: Exception) { estado = EstadoCarga.Error("Fallo de conexión: ${e.message}") }
    }
    suspend fun cargarDisponibles() {
        val t = token ?: return
        try {
            val repoUser = com.example.sennaccess.datos.repositorios.RepositorioUsuarios()
            val lista = repoUser.getUsers(t)
            val idsYa = aprendices.mapNotNull { it.id_usuario }.toSet()
            aprendicesDisponibles = lista.filter { it.role?.rol_name.equals("Aprendiz", true) && it.id_usuario !in idsYa }
        } catch (_: Exception) { aprendicesDisponibles = emptyList() }
    }

    LaunchedEffect(ambiente.id_ambiente) { cargarAprendices() }

    val capacidad = ambiente.ambiente_capacidad ?: 0
    val ocupacion = if (capacidad > 0) (aprendices.size.toFloat() / capacidad).coerceIn(0f, 1f) else 0f

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.ArrowBack, null, tint = colors.textPrimary) }
            Column(modifier = Modifier.weight(1f)) {
                Text(ambiente.ambiente_nombre ?: "Ambiente", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val meta = listOfNotNull(
                    ambiente.ambiente_ubicacion?.takeIf { it.isNotBlank() },
                    ambiente.ambiente_jornada?.takeIf { it.isNotBlank() }
                ).joinToString(" • ")
                if (meta.isNotBlank()) Text(meta, color = colors.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = { scope.launch { cargarAprendices() } }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Refresh, null, tint = colors.textSecondary) }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.fillMaxWidth().superficieVidrio(cornerRadius = RadioVidrio).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(VerdeSena.copy(0.18f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MeetingRoom, null, tint = verdeMarca(), modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (capacidad > 0) "${aprendices.size} de $capacidad estudiantes" else "${aprendices.size} estudiantes",
                        color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp
                    )
                    if (capacidad > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { ocupacion },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                            color = verdeMarca(),
                            trackColor = colors.border
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${(ocupacion * 100).toInt()}% del cupo", color = colors.textSecondary, fontSize = 11.sp)
                    } else {
                        Text("Sin límite de cupo configurado", color = colors.textSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { mostrarAgregar = true; scope.launch { cargarDisponibles() } },
                modifier = Modifier.weight(1.2f).height(50.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = verdeMarca()),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(verdeMarca()))
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("AGREGAR", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
            }
            OutlinedButton(
                onClick = onAutorizarSalida,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = verdeMarca()),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(verdeMarca()))
            ) {
                Icon(Icons.Default.Approval, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("PERMISO", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text("ESTUDIANTES (${aprendices.size})", color = verdeMarca(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(modifier = Modifier.height(8.dp))

        when (val s = estado) {
            is EstadoCarga.Loading -> CajaCargando()
            is EstadoCarga.Error -> CajaError(s.mensaje, onReintentar = { scope.launch { cargarAprendices() } })
            is EstadoCarga.Success -> {
                if (aprendices.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().superficieVidrio(cornerRadius = RadioVidrio).padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Person, null, tint = colors.textSecondary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Este ambiente aún no tiene estudiantes", color = colors.textPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text("Agrega los aprendices con el botón AGREGAR.", color = colors.textSecondary, fontSize = 12.sp)
                        }
                    }
                } else {
                    val fichas = remember(aprendices) {
                        aprendices.groupBy { it.user_coursenumber }.entries
                            .sortedWith(compareBy({ it.key ?: Int.MAX_VALUE }))
                    }
                    val fichaActiva = fichas.firstOrNull { it.key == fichaKeySel } ?: fichas.firstOrNull()

                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(fichas, key = { it.key?.toString() ?: "sin-ficha" }) { (num, lista) ->
                            FiltroSena(
                                texto = "${if (num != null) "Ficha $num" else "Sin ficha"} • ${lista.size}",
                                seleccionado = num == fichaActiva?.key,
                                onClick = { fichaKeySel = num }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    val estudiantesFicha = fichaActiva?.value ?: emptyList()
                    estudiantesFicha.forEach { ap ->
                        Row(modifier = Modifier.fillMaxWidth().superficieVidrio(cornerRadius = 12.dp).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            FotoPerfil(fotoPath = ap.profile_photo_path, nombre = ap.nombreCompleto, tamano = 40.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ap.nombreCompleto, color = colors.textPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(ap.user_email ?: "", color = colors.textSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { aprendizAEliminar = ap }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Delete, null, tint = RojoError, modifier = Modifier.size(20.dp)) }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }

    if (mostrarAgregar) {
        AlertDialog(
            onDismissRequest = { mostrarAgregar = false; errorAgregar = null; seleccionado = null },
            containerColor = colors.cardBackground.copy(alpha = 0.98f),
            shape = RoundedCornerShape(28.dp),
            title = { Text("Agregar aprendiz", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (aprendicesDisponibles.isEmpty()) {
                        Text("No hay aprendices disponibles", color = colors.textSecondary, fontSize = 13.sp)
                    } else {
                        Text("Aprendices disponibles (${aprendicesDisponibles.size})", color = colors.textSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        var expandir by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).superficieVidrio(cornerRadius = 10.dp).padding(8.dp)) {
                            Column {
                                Text(seleccionado?.nombreCompleto ?: "Selecciona aprendiz", color = if (seleccionado == null) colors.textSecondary else colors.textPrimary, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { expandir = !expandir }.padding(8.dp))
                                if (expandir) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                                        items(aprendicesDisponibles, key = { it.id_usuario ?: it.user_email ?: it.hashCode().toString() }) { ap ->
                                            Text("${ap.nombreCompleto} • ${ap.user_identification ?: ""}", color = colors.textPrimary, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().clickable { seleccionado = ap; expandir = false }.padding(8.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (errorAgregar != null) { Spacer(modifier = Modifier.height(8.dp)); errorAgregar?.let { Text(it, color = RojoError, fontSize = 12.sp) } }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val ap = seleccionado ?: run { errorAgregar = "Selecciona un aprendiz"; return@Button }
                    val t = token ?: run { errorAgregar = "Sin sesión"; return@Button }
                    val ambId = ambiente.id_ambiente ?: run { errorAgregar = "Ambiente sin ID"; return@Button }
                    val uid = ap.id_usuario ?: run { errorAgregar = "Aprendiz sin ID"; return@Button }
                    if (agregando) return@Button
                    agregando = true; errorAgregar = null
                    scope.launch {
                        try {
                            repo.addMisAprendiz(t, ambId, uid)
                            agregando = false; mostrarAgregar = false; seleccionado = null; cargarAprendices(); cargarDisponibles()
                        } catch (e: retrofit2.HttpException) { agregando = false; errorAgregar = detalleHttp(e) }
                        catch (e: Exception) { agregando = false; errorAgregar = "Fallo de conexión: ${e.message}" }
                    }
                }, enabled = !agregando && seleccionado != null, colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = Color.Black), shape = RoundedCornerShape(28.dp)) { if (agregando) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Agregar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { mostrarAgregar = false }) { Text("Cancelar") } }
        )
    }

    aprendizAEliminar?.let { ap ->
        // Se limpia el error anterior cada vez que se abre el diálogo.
        LaunchedEffect(ap) { errorQuitar = null; quitando = false }
        AlertDialog(
            onDismissRequest = { if (!quitando) aprendizAEliminar = null },
            containerColor = colors.cardBackground.copy(alpha = 0.98f),
            shape = RoundedCornerShape(28.dp),
            title = { Text("¿Quitar aprendiz?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Se quitará a ${ap.nombreCompleto} de \"${ambiente.ambiente_nombre}\". No se elimina su cuenta, solo la matrícula en este salón.", color = colors.textSecondary)
                    if (errorQuitar != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorQuitar!!, color = RojoError, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (quitando) return@Button
                    val t = token ?: return@Button
                    val ambId = ambiente.id_ambiente ?: return@Button
                    val uid = ap.id_usuario ?: return@Button
                    quitando = true
                    scope.launch {
                        try { repo.removeMisAprendiz(t, ambId, uid); aprendizAEliminar = null; cargarAprendices() }
                        catch (e: retrofit2.HttpException) { errorQuitar = detalleHttp(e); cargarAprendices() }
                        catch (e: Exception) { errorQuitar = "Fallo de conexión: ${e.message}" }
                        quitando = false
                    }
                }, enabled = !quitando, colors = ButtonDefaults.buttonColors(containerColor = RojoError, contentColor = Color.Black), shape = RoundedCornerShape(28.dp)) {
                    if (quitando) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Quitar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { aprendizAEliminar = null }) { Text("Cancelar") } }
        )
    }
}
