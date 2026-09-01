package com.example.sennaccess.excusas

// Pantalla del admin (portería) para validar el PIN de una excusa y autorizar la salida.

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.data.Excusa
import com.example.sennaccess.data.ExcusaRepository
import com.example.sennaccess.data.SessionManager
import com.example.sennaccess.ui.detalleHttp
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import com.example.sennaccess.ui.theme.ErrorRed
import com.example.sennaccess.ui.ios.GlassCornerRadius
import com.example.sennaccess.ui.ios.glassSurface
import kotlinx.coroutines.launch

@Composable
fun ValidarExcusaView(onBack: () -> Unit) {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    val repo = remember { ExcusaRepository() }
    val token = SessionManager.token

    var pin by remember { mutableStateOf("") }
    var validando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var exito by remember { mutableStateOf<Excusa?>(null) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var pendientes by remember { mutableStateOf<List<Excusa>>(emptyList()) }

    suspend fun cargarPendientes() {
        if (token == null) return
        try { pendientes = repo.todasAdmin(token).filter { it.estado == "pendiente" } } catch (_: Exception) {}
    }

    LaunchedEffect(Unit) { cargarPendientes() }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = SenaGreen) }
            Text("Validar salida (PIN)", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { scope.launch { cargarPendientes() } }) { Icon(Icons.Default.Refresh, null, tint = colors.textSecondary) }
        }
        Text("El aprendiz entrega el PIN de 4 dígitos que le dio el instructor. Al validar se registra la Salida en su historial.", color = colors.textSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = { v -> pin = v.filter { it.isDigit() }.take(4) },
            label = { Text("PIN de excusa") },
            placeholder = { Text("ej. 1232") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (error != null) { Text(error!!, color = ErrorRed, fontSize = 13.sp); Spacer(modifier = Modifier.height(8.dp)) }

        exito?.let { ex ->
            Box(modifier = Modifier.fillMaxWidth().glassSurface(cornerRadius = GlassCornerRadius).padding(16.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = SenaGreen, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(mensaje ?: "Salida autorizada", color = SenaGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(ex.aprendiz?.nombreCompleto ?: "Aprendiz #${ex.fk_id_aprendiz}", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Documento: ${ex.aprendiz?.user_identification ?: "—"} • ${ex.aprendiz?.user_email ?: ""}", color = colors.textSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("${ex.ambiente?.ambiente_nombre ?: ""} • ${ex.motivo ?: ""}", color = colors.textSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Instructor: ${ex.instructor?.nombreCompleto ?: "#${ex.fk_id_instructor}"}", color = colors.textSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Autoriza la salida del aprendiz y actualiza el historial (Salida).", color = colors.textSecondary, fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { exito = null; pin = ""; error = null; mensaje = null; scope.launch { cargarPendientes() } }, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = SenaGreen.copy(0.18f), contentColor = SenaGreen)) { Text("Validar otro PIN") }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = {
                if (pin.length != 4) { error = "Ingresa los 4 dígitos del PIN"; return@Button }
                if (token == null) { error = "Sin sesión"; return@Button }
                validando = true; error = null; exito = null
                scope.launch {
                    try {
                        val resp = repo.validar(token, pin)
                        validando = false
                        exito = resp.excusa
                        mensaje = resp.message
                        pin = ""
                        cargarPendientes()
                    } catch (e: retrofit2.HttpException) { validando = false; error = detalleHttp(e) }
                    catch (e: Exception) { validando = false; error = "Fallo de conexión: ${e.message}" }
                }
            },
            enabled = !validando && pin.length == 4,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black)
        ) { if (validando) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp) else Text("VALIDAR Y REGISTRAR SALIDA", fontWeight = FontWeight.Bold) }

        Spacer(modifier = Modifier.height(20.dp))

        Text("EXCUSAS PENDIENTES (${pendientes.size})", color = SenaGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(modifier = Modifier.height(8.dp))
        if (pendientes.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().glassSurface(cornerRadius = 12.dp).padding(16.dp), contentAlignment = Alignment.Center) {
                Text("No hay excusas pendientes.", color = colors.textSecondary, fontSize = 13.sp)
            }
        } else {
            pendientes.forEach { ex ->
                Box(modifier = Modifier.fillMaxWidth().glassSurface(cornerRadius = 12.dp).padding(12.dp)) {
                    Column {
                        Text(ex.aprendiz?.nombreCompleto ?: "Aprendiz #${ex.fk_id_aprendiz}", color = colors.textPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        Text("${ex.ambiente?.ambiente_nombre ?: ""} • ${ex.motivo ?: ""}", color = colors.textSecondary, fontSize = 11.sp)
                        Text("Instructor: ${ex.instructor?.nombreCompleto ?: "#${ex.fk_id_instructor}"} • expira ${ex.expira_en ?: ""}", color = colors.textSecondary, fontSize = 10.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
