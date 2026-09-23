package com.example.sennaccess.excusas

// Vista del aprendiz: historial de sus excusas (pin, motivo, estado).
// Polling silencioso cada 15s para que el PIN creado por el instructor
// aparezca en tiempo real sin salir de la pantalla.

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.datos.modelos.Excusa
import com.example.sennaccess.datos.repositorios.RepositorioExcusas
import com.example.sennaccess.datos.sesion.GestorSesion
import com.example.sennaccess.datos.modelos.estaPendiente
import com.example.sennaccess.comun.EstadoCarga
import com.example.sennaccess.comun.CajaCargando
import com.example.sennaccess.comun.CajaError
import com.example.sennaccess.comun.detalleHttp
import com.example.sennaccess.comun.diseno.TarjetaSena
import com.example.sennaccess.comun.fechaLegible
import com.example.sennaccess.comun.tema.ColoresAppLocal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VistaMisExcusas(onBack: (() -> Unit)? = null) {
    val colors = ColoresAppLocal.current
    val scope = rememberCoroutineScope()
    val repo = remember { RepositorioExcusas() }
    val clipboard = LocalClipboardManager.current
    val token = GestorSesion.token
    var estado by remember { mutableStateOf<EstadoCarga<List<Excusa>>>(EstadoCarga.Loading) }
    var recargando by remember { mutableStateOf(false) }
    var idsVistos by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var hayNueva by remember { mutableStateOf(false) }
    val ahora = recordarAhoraCada30s()

    suspend fun cargar(silencioso: Boolean = false) {
        if (token == null) { estado = EstadoCarga.Error("Sin sesión"); return }
        try {
            if (!silencioso) estado = EstadoCarga.Loading else recargando = true
            val lista = repo.misExcusas(token)
            val nuevosPendientes = lista.filter { it.estaPendiente && it.id_excusa != null && it.id_excusa !in idsVistos }
            if (idsVistos.isNotEmpty() && nuevosPendientes.isNotEmpty()) hayNueva = true
            idsVistos = lista.mapNotNull { it.id_excusa }.toSet()
            estado = EstadoCarga.Success(lista)
        } catch (e: retrofit2.HttpException) { if (!silencioso) estado = EstadoCarga.Error(detalleHttp(e)) }
        catch (e: Exception) { if (!silencioso) estado = EstadoCarga.Error("Fallo de conexión: ${e.message}") }
        finally { recargando = false }
    }

    LaunchedEffect(Unit) { cargar() }
    LaunchedEffect(hayNueva) {
        if (hayNueva) {
            delay(8_000)
            hayNueva = false
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            if (token != null) cargar(silencioso = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = colors.textPrimary) }
            Text("Mis excusas", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            IconButton(onClick = { scope.launch { cargar() } }) { Icon(Icons.Default.Refresh, null, tint = colors.textSecondary) }
            Spacer(modifier = Modifier.weight(1f))
            if (recargando) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        }
        Text("Aquí ves las excusas que tu instructor generó para ti. Entrega el PIN en portería para salir.", color = colors.textSecondary, fontSize = 12.sp)
        if (hayNueva) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("● Nueva excusa pendiente", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))

        when (val s = estado) {
            is EstadoCarga.Loading -> CajaCargando()
            is EstadoCarga.Error -> CajaError(s.mensaje, onReintentar = { scope.launch { cargar() } })
            is EstadoCarga.Success -> {
                val lista = s.datos
                if (lista.isEmpty()) {
                    TarjetaSena(modifier = Modifier.fillMaxWidth()) {
                        Text("No tienes excusas registradas.", color = colors.textSecondary, fontSize = 13.sp)
                    }
                } else {
                    val pendientes = lista.filter { it.estaPendiente }
                    val otras = lista.filter { !it.estaPendiente }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        if (pendientes.isNotEmpty()) {
                            item(key = "titulo-pend") {
                                Text("PENDIENTES (${pendientes.size}) — muestra el PIN en portería", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            items(pendientes, key = { "p-${it.id_excusa}" }) { ex ->
                                TarjetaSena(modifier = Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(ex.ambiente?.ambiente_nombre ?: "Ambiente #${ex.fk_id_ambiente}", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(ex.motivo ?: "", color = colors.textSecondary, fontSize = 12.sp)
                                        }
                                        InsigniaExcusa(ex.estado)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { clipboard.setText(AnnotatedString(ex.pin ?: "")) }
                                    ) {
                                        Text("PIN: ${ex.pin ?: "—"}", color = colors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(Icons.Default.ContentCopy, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                                    }
                                    Text(textoVigencia(ex.expira_en, ahora), color = colors.textSecondary, fontSize = 11.sp)
                                    Text("Instructor: ${ex.instructor?.nombreCompleto ?: "#${ex.fk_id_instructor}"}", color = colors.textSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                        if (otras.isNotEmpty()) {
                            item(key = "titulo-hist") {
                                Text("HISTORIAL (${otras.size})", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            items(otras, key = { "h-${it.id_excusa}" }) { ex ->
                                TarjetaSena(modifier = Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(ex.ambiente?.ambiente_nombre ?: "Ambiente #${ex.fk_id_ambiente}", color = colors.textPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                            Text(textoCreacionExcusa(ex), color = colors.textSecondary, fontSize = 11.sp)
                                        }
                                        InsigniaExcusa(ex.estado)
                                    }
                                    if (ex.usado_en != null) Text("Usada: ${fechaLegible(ex.usado_en)}", color = colors.textSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
