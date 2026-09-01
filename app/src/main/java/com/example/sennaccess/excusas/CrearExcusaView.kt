package com.example.sennaccess.excusas

// Pantalla del instructor para crear una excusa con PIN.
// Flujo: elige ambiente (de sus mis-ambientes) → elige aprendiz de ese ambiente → motivo → genera PIN.

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.data.Ambiente
import com.example.sennaccess.data.AmbienteRepository
import com.example.sennaccess.data.Excusa
import com.example.sennaccess.data.ExcusaRepository
import com.example.sennaccess.data.SessionManager
import com.example.sennaccess.data.UsuarioApi
import com.example.sennaccess.ui.CargaUiState
import com.example.sennaccess.ui.detalleHttp
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import com.example.sennaccess.ui.theme.ErrorRed
import com.example.sennaccess.ui.ios.GlassCornerRadius
import com.example.sennaccess.ui.ios.glassSurface
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearExcusaView(
    onBack: () -> Unit,
    ambienteIdInicial: Int? = null
) {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val token = SessionManager.token

    var ambientes by remember { mutableStateOf<List<Ambiente>>(emptyList()) }
    var ambienteSel by remember { mutableStateOf<Ambiente?>(null) }
    var menuAmbientes by remember { mutableStateOf(false) }
    var cargandoAmbientes by remember { mutableStateOf(false) }

    var aprendices by remember { mutableStateOf<List<UsuarioApi>>(emptyList()) }
    var aprendizSel by remember { mutableStateOf<UsuarioApi?>(null) }
    var menuAprendices by remember { mutableStateOf(false) }
    var cargandoAprendices by remember { mutableStateOf(false) }

    var motivo by remember { mutableStateOf("") }
    var creando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var excusaCreada by remember { mutableStateOf<Excusa?>(null) }
    var listaExcusas by remember { mutableStateOf<List<Excusa>>(emptyList()) }

    val ambRepo = remember { AmbienteRepository() }
    val excRepo = remember { ExcusaRepository() }

    suspend fun cargarAmbientes() {
        if (token == null) return
        cargandoAmbientes = true
        try {
            val lista = ambRepo.getMisAmbientes(token)
            ambientes = lista
            if (ambienteIdInicial != null) ambienteSel = lista.find { it.id_ambiente == ambienteIdInicial } ?: ambienteSel
        } catch (_: Exception) {}
        cargandoAmbientes = false
    }

    suspend fun cargarAprendices(ambienteId: Int) {
        if (token == null) return
        cargandoAprendices = true
        try {
            val lista = ambRepo.getMisAprendices(token, ambienteId)
            aprendices = lista
        } catch (_: Exception) { aprendices = emptyList() }
        cargandoAprendices = false
    }

    suspend fun cargarLista() {
        if (token == null) return
        try { listaExcusas = excRepo.misComoInstructor(token) } catch (_: Exception) {}
    }

    LaunchedEffect(Unit) { cargarAmbientes(); cargarLista() }
    LaunchedEffect(ambienteSel?.id_ambiente) {
        val id = ambienteSel?.id_ambiente ?: return@LaunchedEffect
        aprendizSel = null
        cargarAprendices(id)
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = SenaGreen) }
            Text("Crear excusa", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Text("Selecciona el aprendiz, el ambiente y el motivo. Se genera un PIN de 4 dígitos (vigencia 60 min) que el aprendiz entrega en portería.", color = colors.textSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // Selector ambiente.
        if (cargandoAmbientes) {
            Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(color = SenaGreen, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Cargando ambientes...", color = colors.textSecondary, fontSize = 12.sp) }
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            ExposedDropdownMenuBox(expanded = menuAmbientes, onExpandedChange = { menuAmbientes = !menuAmbientes }) {
                OutlinedTextField(
                    value = ambienteSel?.ambiente_nombre ?: "Selecciona ambiente",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Ambiente *") },
                    leadingIcon = { Icon(Icons.Default.MeetingRoom, null, tint = SenaGreen) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuAmbientes) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = menuAmbientes, onDismissRequest = { menuAmbientes = false }) {
                    ambientes.forEach { amb ->
                        DropdownMenuItem(text = { Text("${amb.ambiente_nombre} • ${amb.ambiente_ubicacion ?: ""}") }, onClick = { ambienteSel = amb; menuAmbientes = false })
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Selector aprendiz.
        if (ambienteSel == null) {
            Text("Selecciona primero un ambiente para ver sus aprendices.", color = colors.textSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))
        } else if (cargandoAprendices) {
            Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(color = SenaGreen, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Cargando aprendices...", color = colors.textSecondary, fontSize = 12.sp) }
            Spacer(modifier = Modifier.height(12.dp))
        } else if (aprendices.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().glassSurface(cornerRadius = 12.dp).padding(12.dp)) { Text("No hay aprendices en ${ambienteSel?.ambiente_nombre}. Añade estudiantes desde la ficha del ambiente.", color = colors.textSecondary, fontSize = 12.sp) }
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            ExposedDropdownMenuBox(expanded = menuAprendices, onExpandedChange = { menuAprendices = !menuAprendices }) {
                OutlinedTextField(
                    value = aprendizSel?.nombreCompleto ?: "Selecciona aprendiz",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Aprendiz *") },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = SenaGreen) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuAprendices) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = menuAprendices, onDismissRequest = { menuAprendices = false }) {
                    aprendices.forEach { ap ->
                        DropdownMenuItem(text = { Text("${ap.nombreCompleto} • ${ap.user_identification ?: ap.user_email ?: ""}") }, onClick = { aprendizSel = ap; menuAprendices = false })
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(value = motivo, onValueChange = { motivo = it }, label = { Text("Motivo *") }, placeholder = { Text("ej. calamidad familiar, cita médica urgente 4:15") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        Spacer(modifier = Modifier.height(12.dp))

        if (error != null) { Text(error!!, color = ErrorRed, fontSize = 12.sp); Spacer(modifier = Modifier.height(8.dp)) }

        // Resultado creación.
        excusaCreada?.let { ex ->
            Box(modifier = Modifier.fillMaxWidth().glassSurface(cornerRadius = GlassCornerRadius).padding(16.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = SenaGreen, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Excusa creada", color = SenaGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${ex.aprendiz?.nombreCompleto ?: "Aprendiz"} • ${ex.ambiente?.ambiente_nombre ?: ""}", color = colors.textSecondary, fontSize = 12.sp)
                    Text("Motivo: ${ex.motivo ?: ""}", color = colors.textPrimary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("PIN (4 dígitos, 60 min, un solo uso):", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).glassSurface(cornerRadius = 12.dp).padding(12.dp)) {
                        Text(ex.pin ?: "—", color = colors.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                    }
                    if (ex.expira_en != null) Text("Expira: ${ex.expira_en}", color = colors.textSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { clipboard.setText(AnnotatedString(ex.pin ?: "")) }, colors = ButtonDefaults.buttonColors(containerColor = SenaGreen.copy(0.18f), contentColor = SenaGreen), shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(6.dp)); Text("Copiar PIN")
                        }
                        Button(onClick = { excusaCreada = null; motivo = ""; aprendizSel = null }, colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black), shape = RoundedCornerShape(10.dp)) { Text("Nueva excusa") }
                    }
                    Text("Entrega este PIN al aprendiz; en portería el admin lo validará para registrar la salida.", color = colors.textSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = {
                val ap = aprendizSel ?: run { error = "Selecciona un aprendiz"; return@Button }
                val amb = ambienteSel ?: run { error = "Selecciona un ambiente"; return@Button }
                if (motivo.isBlank()) { error = "El motivo es obligatorio"; return@Button }
                if (token == null) { error = "Sin sesión"; return@Button }
                val apId = ap.id_usuario ?: return@Button
                val ambId = amb.id_ambiente ?: return@Button
                creando = true; error = null
                scope.launch {
                    try {
                        val ex = excRepo.crear(token, apId, ambId, motivo.trim())
                        creando = false; excusaCreada = ex; cargarLista()
                    } catch (e: retrofit2.HttpException) { creando = false; error = detalleHttp(e) }
                    catch (e: Exception) { creando = false; error = "Fallo de conexión: ${e.message}" }
                }
            },
            enabled = !creando && aprendizSel != null && ambienteSel != null && motivo.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black, disabledContainerColor = SenaGreen.copy(0.3f))
        ) { if (creando) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp) else Text("GENERAR EXCUSA (PIN)", fontWeight = FontWeight.Bold) }

        Spacer(modifier = Modifier.height(20.dp))

        // Historial del instructor.
        Text("MIS EXCUSAS RECIENTES", color = SenaGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(modifier = Modifier.height(8.dp))
        if (listaExcusas.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().glassSurface(cornerRadius = 12.dp).padding(16.dp), contentAlignment = Alignment.Center) { Text("Aún no has creado excusas.", color = colors.textSecondary, fontSize = 13.sp) }
        } else {
            listaExcusas.take(10).forEach { ex ->
                val estadoColor = when (ex.estado) { "pendiente" -> SenaGreen; "usada" -> Color(0xFF2E7D32); "anulada","expirada" -> ErrorRed; else -> colors.textSecondary }
                Box(modifier = Modifier.fillMaxWidth().glassSurface(cornerRadius = 12.dp).padding(12.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ex.aprendiz?.nombreCompleto ?: "Aprendiz #${ex.fk_id_aprendiz}", color = colors.textPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                Text("${ex.ambiente?.ambiente_nombre ?: ""} • ${ex.motivo ?: ""}", color = colors.textSecondary, fontSize = 11.sp)
                            }
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).glassSurface(cornerRadius = 6.dp).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text(ex.estado?.uppercase() ?: "—", color = estadoColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (ex.estado == "pendiente") {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("PIN: ${ex.pin ?: "—"}", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("expira ${ex.expira_en ?: ""}", color = colors.textSecondary, fontSize = 10.sp)
                                Spacer(modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                                    val id = ex.id_excusa ?: return@TextButton
                                    val t = token ?: return@TextButton
                                    scope.launch {
                                        try { excRepo.anular(t, id); cargarLista() } catch (_: Exception) {}
                                    }
                                }) { Text("Anular", color = ErrorRed, fontSize = 12.sp) }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
