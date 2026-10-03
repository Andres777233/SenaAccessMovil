package com.example.sennaccess.excusas

// Pantalla del admin (portería) para validar el PIN de una excusa y autorizar la salida.
// Polling silencioso cada 10s para ver en tiempo real las excusas que crean los instructores.

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.datos.modelos.Excusa
import com.example.sennaccess.datos.repositorios.RepositorioExcusas
import com.example.sennaccess.datos.sesion.GestorSesion
import com.example.sennaccess.datos.modelos.estaPendiente
import com.example.sennaccess.datos.modelos.estadoNormalizado
import androidx.compose.ui.draw.clip
import com.example.sennaccess.comun.detalleHttp
import com.example.sennaccess.comun.campoVisible
import com.example.sennaccess.comun.diseno.TarjetaSena
import com.example.sennaccess.comun.fechaLegible
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import com.example.sennaccess.comun.tema.RojoError
import com.example.sennaccess.comun.diseno.RadioVidrio
import com.example.sennaccess.comun.diseno.RadioSena
import com.example.sennaccess.comun.diseno.superficieVidrio
import com.example.sennaccess.comun.diseno.superficiePlana
import com.example.sennaccess.comun.diseno.escalaPresion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VistaValidarExcusa(onBack: () -> Unit, mostrarCabecera: Boolean = true) {
    val colors = ColoresAppLocal.current
    val scope = rememberCoroutineScope()
    val repo = remember { RepositorioExcusas() }
    val token = GestorSesion.token

    var pin by remember { mutableStateOf("") }
    var validando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var errorCarga by remember { mutableStateOf<String?>(null) }
    var exito by remember { mutableStateOf<Excusa?>(null) }
    var exitoNombre by remember { mutableStateOf<String?>(null) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var pendientes by remember { mutableStateOf<List<Excusa>>(emptyList()) }
    var pideConfirmar by remember { mutableStateOf(false) }
    val ahora = recordarAhoraCada30s()

    suspend fun cargarPendientes(silencioso: Boolean = false) {
        if (token == null) return
        try {
            pendientes = repo.todasAdmin(token).filter { it.estaPendiente }
            errorCarga = null
        } catch (e: retrofit2.HttpException) { if (!silencioso) errorCarga = detalleHttp(e) }
        catch (e: Exception) { if (!silencioso) errorCarga = "No se pudieron cargar pendientes: ${e.message}" }
    }

    LaunchedEffect(Unit) { cargarPendientes() }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000)
            cargarPendientes(silencioso = true)
        }
    }

    fun validar() {
        if (validando) return
        if (pin.length != 4) { error = "Ingresa los 4 dígitos del PIN"; return }
        if (token == null) { error = "Sin sesión"; return }
        validando = true; error = null; exito = null
        scope.launch {
            try {
                val resp = repo.validar(token, pin)
                validando = false
                exito = resp.excusa
                exitoNombre = resp.excusa?.aprendiz?.nombreCompleto ?: resp.aprendiz?.nombreCompleto
                mensaje = resp.message
                pin = ""
                pideConfirmar = false
                cargarPendientes(silencioso = true)
            } catch (e: retrofit2.HttpException) { validando = false; pideConfirmar = false; error = detalleHttp(e) }
            catch (e: Exception) { validando = false; pideConfirmar = false; error = "Fallo de conexión: ${e.message}" }
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()) {
        // Cabecera propia solo cuando se usa fuera de la vista unificada
        // (la unificada ya trae su volver + título + segmentado).
        if (mostrarCabecera) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = colors.textPrimary) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "VALIDAR SALIDA",
                        color = verdeMarca(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp
                    )
                    Text("PIN de 4 dígitos", color = colors.textPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                }
                IconButton(onClick = { scope.launch { cargarPendientes() } }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Refresh, contentDescription = "Recargar pendientes", tint = colors.textSecondary) }
            }
            Text("El aprendiz entrega el PIN que le dio el instructor. Al validar se registra la Salida en su historial.", color = colors.textSecondary, fontSize = 12.sp)
            if (errorCarga != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(errorCarga!!, color = RojoError, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            // Dentro de la unificada: subtítulo compacto + recarga sin flecha.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("PIN de 4 dígitos para autorizar la salida.", color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = { scope.launch { cargarPendientes() } }, modifier = Modifier.size(44.dp)) { Icon(Icons.Default.Refresh, contentDescription = "Recargar pendientes", tint = colors.textSecondary) }
            }
            if (errorCarga != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(errorCarga!!, color = RojoError, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = pin,
            onValueChange = { v -> pin = v.filter { it.isDigit() }.take(4) },
            label = { Text("PIN de excusa (4 dígitos)") },
            placeholder = { Text("• • • •") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().campoVisible(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 28.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Toca una excusa pendiente para autollenar su PIN.",
            color = colors.textSecondary, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (error != null) { Text(error!!, color = RojoError, fontSize = 13.sp); Spacer(modifier = Modifier.height(8.dp)) }

        exito?.let { ex ->
            // Éxito protagonista en vidrio: la confirmación que mira portería.
            Box(modifier = Modifier.fillMaxWidth().superficieVidrio(cornerRadius = 24.dp, elevated = true).padding(16.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = verdeMarca(), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(mensaje ?: "Salida autorizada", color = verdeMarca(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(exitoNombre ?: ex.aprendiz?.nombreCompleto ?: "Aprendiz #${ex.fk_id_aprendiz}", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Documento: ${ex.aprendiz?.user_identification ?: "—"} • ${ex.aprendiz?.user_email ?: ""}", color = colors.textSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(textoCreacionExcusa(ex), color = colors.textSecondary, fontSize = 12.sp)
                    if (ex.usado_en != null) Text("Registrada: ${fechaLegible(ex.usado_en)}", color = colors.textSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Autoriza la salida del aprendiz y actualiza el historial (Salida).", color = colors.textSecondary, fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { exito = null; pin = ""; error = null; mensaje = null; exitoNombre = null; scope.launch { cargarPendientes() } }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = VerdeSena.copy(0.18f), contentColor = verdeMarca())) { Text("Validar otro PIN") }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = { pideConfirmar = true },
            enabled = !validando && pin.length == 4,
            modifier = Modifier.fillMaxWidth().height(52.dp).escalaPresion(pressedScale = 0.97f),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = Color.Black)
        ) { if (validando) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp) else Text("VALIDAR Y REGISTRAR SALIDA", fontWeight = FontWeight.Bold) }

        if (pideConfirmar) {
            AlertDialog(
                onDismissRequest = { if (!validando) pideConfirmar = false },
                title = { Text("¿Validar PIN $pin?") },
                text = { Text("Se registra la Salida del aprendiz en su historial. Verifica su documento antes de confirmar.") },
                confirmButton = { TextButton(onClick = { validar() }, enabled = !validando) { Text(if (validando) "Validando..." else "Sí, validar") } },
                dismissButton = { TextButton(onClick = { pideConfirmar = false }, enabled = !validando) { Text("Cancelar") } }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("EXCUSAS PENDIENTES", color = verdeMarca(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp, modifier = Modifier.fillMaxWidth())
        Text("Toca una para usar su PIN", color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        if (pendientes.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().superficiePlana(cornerRadius = RadioSena.md).padding(16.dp), contentAlignment = Alignment.Center) {
                Text("No hay excusas pendientes.", color = colors.textSecondary, fontSize = 13.sp)
            }
        } else {
            // Tarjetas separadas por pendiente: cada excusa respira sola.
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                pendientes.forEach { ex ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(colors.surface, RoundedCornerShape(16.dp))
                            .clickable(enabled = ex.pin?.length == 4) { ex.pin?.let { pin = it } }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ex.aprendiz?.nombreCompleto ?: "Aprendiz #${ex.fk_id_aprendiz}", color = colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                            Text(textoCreacionExcusa(ex), color = colors.textSecondary, fontSize = 11.sp)
                            Text("PIN ${ex.pin ?: "—"} • ${textoVigencia(ex.expira_en, ahora)}", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
                            Text("Instructor: ${ex.instructor?.nombreCompleto ?: "#${ex.fk_id_instructor}"}", color = colors.textSecondary, fontSize = 10.sp, maxLines = 1)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        InsigniaExcusa(ex.estado)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
