package com.example.sennaccess.jornada

// Pantalla del instructor/admin para generar y proyectar el QR rotativo TOTP del aula.
// Liviana: consume GET /jornada/qr/{ambiente} calculado server-side; sin secretos en el móvil.
// El QR expira cada 30s (TOTP estándar) y se repolea automáticamente.

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.data.Ambiente
import com.example.sennaccess.data.RetrofitClient
import com.example.sennaccess.data.SessionManager
import com.example.sennaccess.ui.CargaUiState
import com.example.sennaccess.ui.CargandoBox
import com.example.sennaccess.ui.ErrorBox
import com.example.sennaccess.ui.generarQrBitmap
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import com.example.sennaccess.ui.ios.GlassCornerRadius
import com.example.sennaccess.ui.ios.glassSurface
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerarQrAulaView(
    onBack: () -> Unit,
    ambienteIdInicial: Int? = null,
    viewModel: JornadaViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val colors = LocalAppColors.current
    val qrEstado by viewModel.qr.collectAsState()
    var ambienteId by remember { mutableStateOf<Int?>(ambienteIdInicial) }
    var ambientes by remember { mutableStateOf<List<Ambiente>>(emptyList()) }
    var cargandoAmbientes by remember { mutableStateOf(false) }
    var menuAmbientes by remember { mutableStateOf(false) }
    var segundosRestantes by remember { mutableStateOf(30) }

    // Carga el catálogo de ambientes para el dropdown (liviano).
    LaunchedEffect(Unit) {
        val token = SessionManager.token ?: return@LaunchedEffect
        cargandoAmbientes = true
        try {
            val lista = RetrofitClient.conServicio { it.getAmbientes("Bearer $token") }
            ambientes = lista
            if (ambienteId == null && lista.isNotEmpty()) ambienteId = lista.first().id_ambiente
        } catch (_: Exception) {}
        cargandoAmbientes = false
    }

    // Carga inicial del QR y polling cada 30s.
    LaunchedEffect(ambienteId) { viewModel.cargarQr(ambienteId) }
    LaunchedEffect(qrEstado, ambienteId) {
        while (true) {
            segundosRestantes = segundosRestantesVentana(30)
            delay(1000)
            if (segundosRestantes <= 1) {
                viewModel.cargarQr(ambienteId)
                delay(1200)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = SenaGreen) }
            Text("QR del Aula", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { viewModel.cargarQr(ambienteId) }) { Icon(Icons.Default.Refresh, null, tint = colors.textSecondary) }
        }
        Text("Proyecta este código en el aula. El aprendiz lo escanea para validar presencia física (expira cada 30s).", color = colors.textSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // Selector de ambiente.
        if (cargandoAmbientes) {
            CargandoBox()
        } else if (ambientes.isNotEmpty()) {
            ExposedDropdownMenuBox(expanded = menuAmbientes, onExpandedChange = { menuAmbientes = !menuAmbientes }) {
                OutlinedTextField(
                    value = ambientes.find { it.id_ambiente == ambienteId }?.ambiente_nombre ?: "Selecciona ambiente",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Ambiente") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuAmbientes) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = menuAmbientes, onDismissRequest = { menuAmbientes = false }) {
                    ambientes.forEach { amb ->
                        DropdownMenuItem(text = { Text(amb.ambiente_nombre ?: "Ambiente ${amb.id_ambiente}") }, onClick = { ambienteId = amb.id_ambiente; menuAmbientes = false })
                    }
                }
            }
        } else {
            OutlinedTextField(value = ambienteId?.toString() ?: "", onValueChange = { ambienteId = it.toIntOrNull() }, label = { Text("ID de ambiente (opcional)") }, placeholder = { Text("ej. 1 — se usa el ambiente actual si está vacío") }, modifier = Modifier.fillMaxWidth())
        }
        Spacer(modifier = Modifier.height(16.dp))

        when (val s = qrEstado) {
            is CargaUiState.Loading -> Box(modifier = Modifier.fillMaxWidth().height(280.dp).glassSurface(cornerRadius = GlassCornerRadius), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = SenaGreen) }
            is CargaUiState.Error -> ErrorBox(s.mensaje, onReintentar = { viewModel.cargarQr(ambienteId) })
            is CargaUiState.Success -> {
                val qr = s.datos
                val contenido = qr.qr_content ?: qr.code?.let { construirContenidoQr(it, qr.ambiente_id ?: ambienteId ?: 1, qr.periodo_s ?: 30) } ?: "SIN-CODIGO"
                val bitmap = remember(contenido) { generarQrBitmap(contenido, 512) }
                Box(modifier = Modifier.fillMaxWidth().glassSurface(cornerRadius = GlassCornerRadius).padding(16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCode2, null, tint = SenaGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(qr.ambiente_nombre ?: "Ambiente ${qr.ambiente_id ?: ambienteId ?: "actual"}", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White).padding(12.dp)) {
                            if (bitmap != null) {
                                Image(bitmap = bitmap.asImageBitmap(), contentDescription = "QR del aula", modifier = Modifier.size(240.dp))
                            } else {
                                Text("No se pudo generar el QR.", color = Color.Red, fontSize = 13.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        // Countdown + code.
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(SenaGreen.copy(0.15f)).border(1.dp, SenaGreen.copy(0.6f), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text("${segundosRestantes}s", color = SenaGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Text(qr.code ?: contenido.take(20), color = colors.textSecondary, fontSize = 12.sp, letterSpacing = 1.2.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Contenido: $contenido", color = colors.textSecondary, fontSize = 10.sp, textAlign = TextAlign.Center)
                        if (qr.expira_en != null) Text("Expira: ${qr.expira_en}", color = colors.textSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("El servidor valida el code por ventana (NTP) ±1 periodo y rechaza replays.", color = colors.textSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { viewModel.cargarQr(ambienteId) }, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = SenaGreen.copy(0.18f), contentColor = SenaGreen)) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refrescar ahora", fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
