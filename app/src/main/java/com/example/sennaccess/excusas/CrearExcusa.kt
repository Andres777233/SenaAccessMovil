package com.example.sennaccess.excusas

// Pantalla del instructor para dar permiso de salida con PIN.
// Flujo: busca al aprendiz (filtro por ficha + buscador, orden alfabético) →
// motivo → genera PIN de 4 dígitos (15 min, un solo uso) para portería.
// Sin selector de ambiente: cada aprendiz ya trae el salón al que pertenece.

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.datos.repositorios.RepositorioAmbientes
import com.example.sennaccess.datos.modelos.Excusa
import com.example.sennaccess.datos.repositorios.RepositorioExcusas
import com.example.sennaccess.datos.sesion.GestorSesion
import com.example.sennaccess.datos.modelos.UsuarioApi
import com.example.sennaccess.perfil.FotoPerfil
import com.example.sennaccess.comun.campoVisible
import com.example.sennaccess.comun.detalleHttp
import com.example.sennaccess.comun.diseno.FiltroSena
import com.example.sennaccess.comun.diseno.BuscadorSena
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import com.example.sennaccess.comun.tema.RojoError
import com.example.sennaccess.comun.diseno.superficieVidrio
import kotlinx.coroutines.launch

private data class CandidatoExcusa(val aprendiz: UsuarioApi, val ambienteId: Int, val ambienteNombre: String)

@Composable
fun VistaCrearExcusa(
    onBack: () -> Unit,
    ambienteIdInicial: Int? = null
) {
    val colors = ColoresAppLocal.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val token = GestorSesion.token

    var candidatos by remember { mutableStateOf<List<CandidatoExcusa>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var ambienteContexto by remember { mutableStateOf<String?>(null) }
    var aprendizSel by remember { mutableStateOf<CandidatoExcusa?>(null) }
    var busqueda by remember { mutableStateOf("") }
    var fichaSel by remember { mutableStateOf<Int?>(null) }
    var motivo by rememberSaveable { mutableStateOf("") }
    var creando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var errorCarga by remember { mutableStateOf<String?>(null) }
    var excusaCreada by remember { mutableStateOf<Excusa?>(null) }
    val ahora = recordarAhoraCada30s()

    val ambRepo = remember { RepositorioAmbientes() }
    val excRepo = remember { RepositorioExcusas() }

    suspend fun cargar() {
        val t = token ?: run { errorCarga = "Sin sesión"; cargando = false; return }
        cargando = true
        errorCarga = null
        try {
            val ambientes = ambRepo.getMisAmbientes(t)
            val utiles = if (ambienteIdInicial != null) ambientes.filter { it.id_ambiente == ambienteIdInicial } else ambientes
            ambienteContexto = if (ambienteIdInicial != null) utiles.firstOrNull()?.ambiente_nombre else null
            val vistos = mutableSetOf<Int>()
            val todos = mutableListOf<CandidatoExcusa>()
            for (amb in utiles) {
                val ambId = amb.id_ambiente ?: continue
                val lista = try { ambRepo.getMisAprendices(t, ambId) } catch (_: Exception) { emptyList() }
                for (ap in lista) {
                    val uid = ap.id_usuario ?: continue
                    if (vistos.add(uid)) todos.add(CandidatoExcusa(ap, ambId, amb.ambiente_nombre ?: "Ambiente $ambId"))
                }
            }
            candidatos = todos.sortedBy { it.aprendiz.nombreCompleto.lowercase() }
        } catch (e: retrofit2.HttpException) { errorCarga = detalleHttp(e) }
        catch (e: Exception) { errorCarga = "No se pudieron cargar aprendices: ${e.message}" }
        cargando = false
    }

    LaunchedEffect(ambienteIdInicial) { cargar() }

    val fichas = remember(candidatos) {
        candidatos.mapNotNull { it.aprendiz.user_coursenumber }.distinct().sorted()
    }
    val q = busqueda.trim()
    val filtrados = candidatos.filter { c ->
        (fichaSel == null || c.aprendiz.user_coursenumber == fichaSel) &&
            (q.isBlank() || c.aprendiz.nombreCompleto.contains(q, ignoreCase = true) ||
                (c.aprendiz.user_identification?.contains(q, ignoreCase = true) == true) ||
                (c.aprendiz.user_email?.contains(q, ignoreCase = true) == true))
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.ArrowBack, null, tint = verdeMarca()) }
            Column(modifier = Modifier.weight(1f)) {
                Text("Permiso de salida", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    ambienteContexto ?: "Se genera un PIN de 4 dígitos (15 min, un solo uso) que el aprendiz entrega en portería.",
                    color = colors.textSecondary, fontSize = 12.sp
                )
            }
        }
        if (errorCarga != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(errorCarga!!, color = RojoError, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))

        val creada = excusaCreada
        if (creada != null) {
            val nombrePin = creada.aprendiz?.nombreCompleto ?: aprendizSel?.aprendiz?.nombreCompleto ?: "Aprendiz"
            val docPin = creada.aprendiz?.user_identification ?: aprendizSel?.aprendiz?.user_identification ?: "—"
            val fichaPin = creada.aprendiz?.user_coursenumber ?: aprendizSel?.aprendiz?.user_coursenumber
            val ambPin = creada.ambiente?.ambiente_nombre ?: aprendizSel?.ambienteNombre ?: ""
            Box(modifier = Modifier.fillMaxWidth().superficieVidrio(cornerRadius = 28.dp).padding(20.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CheckCircle, null, tint = verdeMarca(), modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("PIN DE SALIDA", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        creada.pin ?: "—",
                        color = colors.textPrimary,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        FotoPerfil(fotoPath = creada.aprendiz?.profile_photo_path ?: aprendizSel?.aprendiz?.profile_photo_path, nombre = nombrePin, tamano = 44.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(nombrePin, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                buildString {
                                    append("CC $docPin")
                                    if (fichaPin != null && fichaPin > 0) append(" • Ficha $fichaPin")
                                    if (ambPin.isNotBlank()) append(" • $ambPin")
                                },
                                color = colors.textSecondary, fontSize = 12.sp
                            )
                            if (!creada.motivo.isNullOrBlank()) Text("Motivo: ${creada.motivo}", color = colors.textSecondary, fontSize = 12.sp)
                        }
                    }
                    if (creada.expira_en != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(textoVigencia(creada.expira_en, ahora), color = verdeMarca(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { clipboard.setText(AnnotatedString(creada.pin ?: "")) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = verdeMarca()),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(verdeMarca()))
                        ) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("COPIAR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { excusaCreada = null; motivo = ""; aprendizSel = null; busqueda = ""; fichaSel = null; error = null },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = Color.Black)
                        ) { Text("NUEVO PERMISO", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().superficieVidrio(cornerRadius = 16.dp).padding(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("¿QUÉ SIGUE?", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    PasoPermiso(numero = "1", texto = "Dicta o comparte el PIN al aprendiz antes de que venza.")
                    PasoPermiso(numero = "2", texto = "El aprendiz lo presenta en portería con su documento.")
                    PasoPermiso(numero = "3", texto = "El admin lo valida y la salida queda en el historial.")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (error != null) { Text(error!!, color = RojoError, fontSize = 12.sp); Spacer(modifier = Modifier.height(8.dp)) }
        } else {

        if (aprendizSel != null) {
            val c = aprendizSel!!
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().superficieVidrio(cornerRadius = 16.dp).padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                FotoPerfil(fotoPath = c.aprendiz.profile_photo_path, nombre = c.aprendiz.nombreCompleto, tamano = 44.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(c.aprendiz.nombreCompleto, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("${c.ambienteNombre} • ${c.aprendiz.user_identification ?: ""}", color = colors.textSecondary, fontSize = 11.sp)
                }
                IconButton(onClick = { aprendizSel = null }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cambiar aprendiz", tint = colors.textSecondary)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            Text("APRENDIZ *", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(modifier = Modifier.height(8.dp))
            BuscadorSena(valor = busqueda, onValor = { busqueda = it }, placeholder = "Buscar por nombre, documento o correo...")
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 8.dp), modifier = Modifier.fillMaxWidth()) {
                item(key = "todas") {
                    FiltroSena(texto = "Todas • ${candidatos.size}", seleccionado = fichaSel == null, onClick = { fichaSel = null })
                }
                items(fichas, key = { "f-$it" }) { f ->
                    val n = candidatos.count { it.aprendiz.user_coursenumber == f }
                    FiltroSena(texto = "Ficha $f • $n", seleccionado = fichaSel == f, onClick = { fichaSel = f })
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (cargando) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = verdeMarca(), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cargando aprendices...", color = colors.textSecondary, fontSize = 12.sp)
                }
            } else if (filtrados.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().superficieVidrio(cornerRadius = 12.dp).padding(14.dp)) {
                    Text(if (candidatos.isEmpty()) "No tienes aprendices asignados en tus ambientes." else "Sin coincidencias para \"$q\".", color = colors.textSecondary, fontSize = 12.sp)
                }
            } else {
                Text("${filtrados.size} aprendices • orden alfabético", color = colors.textSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filtrados.take(30).forEach { c ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).superficieVidrio(cornerRadius = 14.dp)
                                .clickable { aprendizSel = c; error = null }.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            FotoPerfil(fotoPath = c.aprendiz.profile_photo_path, nombre = c.aprendiz.nombreCompleto, tamano = 40.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(c.aprendiz.nombreCompleto, color = colors.textPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                Text("Ficha ${c.aprendiz.user_coursenumber ?: "—"} • ${c.aprendiz.user_identification ?: ""}", color = colors.textSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
                if (filtrados.size > 30) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Mostrando 30 de ${filtrados.size}: escribe en el buscador o filtra por ficha.", color = colors.textSecondary, fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(value = motivo, onValueChange = { motivo = it }, label = { Text("Motivo *") }, placeholder = { Text("ej. calamidad familiar, cita médica urgente 4:15") }, modifier = Modifier.fillMaxWidth().campoVisible(), minLines = 2)
        Spacer(modifier = Modifier.height(12.dp))

        if (error != null) { Text(error!!, color = RojoError, fontSize = 12.sp); Spacer(modifier = Modifier.height(8.dp)) }

        Button(
            onClick = {
                val c = aprendizSel ?: run { error = "Elige un aprendiz de la lista"; return@Button }
                if (motivo.isBlank()) { error = "El motivo es obligatorio"; return@Button }
                val t = token ?: run { error = "Sin sesión"; return@Button }
                val apId = c.aprendiz.id_usuario ?: run { error = "El aprendiz no tiene ID válido"; return@Button }
                creando = true; error = null
                scope.launch {
                    try {
                        val ex = excRepo.crear(t, apId, c.ambienteId, motivo.trim())
                        creando = false; excusaCreada = ex
                    } catch (e: retrofit2.HttpException) { creando = false; error = detalleHttp(e) }
                    catch (e: Exception) { creando = false; error = "Fallo de conexión: ${e.message}" }
                }
            },
            enabled = !creando && aprendizSel != null && motivo.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = Color.Black, disabledContainerColor = VerdeSena.copy(0.3f))
        ) { if (creando) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp) else Text("GENERAR PIN", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
        Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PasoPermiso(numero: String, texto: String) {
    val colors = ColoresAppLocal.current
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.size(24.dp).clip(CircleShape).background(verdeMarca().copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(numero, color = verdeMarca(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(texto, color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f).padding(top = 3.dp))
    }
}
