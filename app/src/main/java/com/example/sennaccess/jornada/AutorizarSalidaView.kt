package com.example.sennaccess.jornada

// Pantalla del instructor/admin para emitir el token de salida anticipada.
// El backend escribe el audit trail inmutable (instructor_id, timestamp, motivo)
// y el aprendiz usa el token en POST /jornada/salida-anticipada.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.data.SessionManager
import com.example.sennaccess.data.UsuarioApi
import com.example.sennaccess.ui.CargaUiState
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import com.example.sennaccess.ui.theme.ErrorRed
import com.example.sennaccess.ui.ios.GlassCornerRadius
import com.example.sennaccess.ui.ios.glassSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutorizarSalidaView(
    onBack: () -> Unit,
    viewModel: JornadaViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val colors = LocalAppColors.current
    val clipboard = LocalClipboardManager.current
    val permiso by viewModel.permiso.collectAsState()
    var aprendices by remember { mutableStateOf<List<UsuarioApi>>(emptyList()) }
    var cargandoAprendices by remember { mutableStateOf(false) }
    var aprendizSel by remember { mutableStateOf<UsuarioApi?>(null) }
    var menuAprendices by remember { mutableStateOf(false) }
    var motivo by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    // Carga aprendices para el dropdown (solo admin/instructor con token).
    LaunchedEffect(Unit) {
        val token = SessionManager.token ?: return@LaunchedEffect
        cargandoAprendices = true
        try {
            val repo = com.example.sennaccess.data.UsuarioRepository()
            val lista = repo.getUsers(token)
            aprendices = lista.filter { it.role?.rol_name.equals("Aprendiz", ignoreCase = true) }
        } catch (_: Exception) {}
        cargandoAprendices = false
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = SenaGreen) }
            Text("Autorizar salida", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Text("Emite un permiso de salida anticipada. Queda registrado en auditoría con tu ID, motivo y timestamp.", color = colors.textSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // Selector de aprendiz.
        if (cargandoAprendices) {
            Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(color = SenaGreen, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Cargando aprendices...", color = colors.textSecondary, fontSize = 12.sp) }
            Spacer(modifier = Modifier.height(12.dp))
        } else if (aprendices.isNotEmpty()) {
            ExposedDropdownMenuBox(expanded = menuAprendices, onExpandedChange = { menuAprendices = !menuAprendices }) {
                OutlinedTextField(
                    value = aprendizSel?.nombreCompleto ?: "Selecciona aprendiz",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Aprendiz") },
                    leadingIcon = { Icon(Icons.Default.PersonSearch, null, tint = SenaGreen) },
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
        } else {
            OutlinedTextField(value = aprendizSel?.id_usuario?.toString() ?: "", onValueChange = { v -> aprendizSel = v.toIntOrNull()?.let { UsuarioApi(id_usuario = it) } }, label = { Text("ID de aprendiz") }, leadingIcon = { Icon(Icons.Default.Badge, null, tint = SenaGreen) }, placeholder = { Text("ej. 5") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            Text("Sin lista de aprendices (modo demo o sin permisos). Ingresa el ID manualmente.", color = colors.textSecondary, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedTextField(value = motivo, onValueChange = { motivo = it }, label = { Text("Motivo *") }, placeholder = { Text("ej. cita médica, calamidad") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = pin, onValueChange = { pin = it }, label = { Text("PIN de autorización (si aplica)") }, placeholder = { Text("PIN del instructor/admin") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(modifier = Modifier.height(16.dp))

        // Estado del permiso.
        when (val p = permiso) {
            is CargaUiState.Loading -> Box(modifier = Modifier.fillMaxWidth().glassSurface(cornerRadius = GlassCornerRadius).padding(16.dp), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(color = SenaGreen, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(10.dp)); Text("Emitiendo permiso...", color = colors.textSecondary, fontSize = 13.sp) }
            }
            is CargaUiState.Error -> Box(modifier = Modifier.fillMaxWidth().glassSurface(cornerRadius = 12.dp).padding(12.dp)) {
                Text(p.mensaje, color = ErrorRed, fontSize = 13.sp)
            }
            is CargaUiState.Success -> {
                val resp = p.datos
                val tokenPermiso = resp.permiso_token ?: "—"
                Box(modifier = Modifier.fillMaxWidth().glassSurface(cornerRadius = GlassCornerRadius).padding(16.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = SenaGreen, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(resp.message ?: "Permiso emitido correctamente", color = SenaGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Token (corta vida, un solo uso):", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).glassSurface(cornerRadius = 10.dp).padding(12.dp)) {
                            Text(tokenPermiso, color = colors.textPrimary, fontSize = 13.sp)
                        }
                        if (resp.expira_en != null) Text("Expira: ${resp.expira_en}", color = colors.textSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { clipboard.setText(AnnotatedString(tokenPermiso)) }, colors = ButtonDefaults.buttonColors(containerColor = SenaGreen.copy(0.18f), contentColor = SenaGreen), shape = RoundedCornerShape(10.dp)) {
                                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(6.dp)); Text("Copiar token")
                            }
                            Button(onClick = viewModel::limpiarPermiso, colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black), shape = RoundedCornerShape(10.dp)) { Text("Nuevo permiso") }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Entrégale este token al aprendiz para que lo pegue en 'Salida anticipada'. Queda auditado.", color = colors.textSecondary, fontSize = 11.sp)
                    }
                }
            }
            null -> {}
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val id = aprendizSel?.id_usuario
                if (id == null) return@Button
                viewModel.emitirPermiso(id, motivo.trim(), pin.trim().ifBlank { null })
            },
            enabled = aprendizSel?.id_usuario != null && motivo.isNotBlank() && permiso !is CargaUiState.Loading,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black, disabledContainerColor = SenaGreen.copy(0.3f))
        ) { Text("EMITIR PERMISO", fontWeight = FontWeight.Bold) }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
