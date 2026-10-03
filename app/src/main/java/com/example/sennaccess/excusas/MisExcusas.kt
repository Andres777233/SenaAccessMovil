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
import com.example.sennaccess.comun.diseno.superficiePlana
import com.example.sennaccess.comun.diseno.RadioSena
import com.example.sennaccess.comun.diseno.EntradaSuave
import com.example.sennaccess.comun.fechaLegible
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
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
    val ahora = recordarAhoraCadaSegundo()

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
        // Cabecera editorial de excusas.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = colors.textPrimary) }
            Column(modifier = Modifier.weight(1f)) {
                Text("PERMISOS DE SALIDA", color = verdeMarca(), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
                Text("Mis excusas", color = colors.textPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            }
            IconButton(onClick = { scope.launch { cargar() } }) { Icon(Icons.Default.Refresh, null, tint = colors.textSecondary) }
            if (recargando) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        }
        Text("Tu instructor genera el permiso; muestra el QR o el PIN en portería para salir.", color = colors.textSecondary, fontSize = 12.sp)
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
                                Text("PENDIENTES • ${pendientes.size} — muestra el PIN en portería", color = verdeMarca(), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                            }
                            items(pendientes, key = { "p-${it.id_excusa}" }) { ex ->
                                EntradaSuave(indice = 0) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().superficiePlana(cornerRadius = RadioSena.lg).padding(16.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("PERMISO VIGENTE", color = verdeMarca(), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(ex.ambiente?.ambiente_nombre ?: "Ambiente #${ex.fk_id_ambiente}", color = colors.textPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                            Text(ex.motivo ?: "", color = colors.textSecondary, fontSize = 12.sp, maxLines = 2)
                                        }
                                        InsigniaExcusa(ex.estado)
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    // Hero del PIN + QR: número protagonista y QR para portería.
                                    Box(
                                        modifier = Modifier.fillMaxWidth().clickable { clipboard.setText(AnnotatedString(ex.pin ?: "")) }.padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(ex.pin ?: "—", color = colors.textPrimary, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 6.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Icon(Icons.Default.ContentCopy, null, tint = verdeMarca(), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        CodigoQrExcusa(excusa = ex, lado = 170.dp, mostrarPin = false)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(textoVigencia(ex.expira_en, ahora), color = colors.textSecondary, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = colors.divider)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Instructor: ${ex.instructor?.nombreCompleto ?: "#${ex.fk_id_instructor}"}", color = colors.textSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                                }
                                }
                            }
                        }
                        if (otras.isNotEmpty()) {
                            item(key = "titulo-hist") {
                                Text("HISTORIAL • ${otras.size}", color = colors.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                            }
                            items(otras, key = { "h-${it.id_excusa}" }) { ex ->
                                Column(
                                    modifier = Modifier.fillMaxWidth().superficiePlana(cornerRadius = RadioSena.lg).padding(14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(ex.ambiente?.ambiente_nombre ?: "Ambiente #${ex.fk_id_ambiente}", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(textoCreacionExcusa(ex), color = colors.textSecondary, fontSize = 11.sp)
                                        }
                                        InsigniaExcusa(ex.estado)
                                    }
                                    if (ex.usado_en != null) Text("Usada: ${fechaLegible(ex.usado_en)}", color = colors.textSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
