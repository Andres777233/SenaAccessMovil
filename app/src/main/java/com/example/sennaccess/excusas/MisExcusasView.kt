package com.example.sennaccess.excusas

// Vista del aprendiz: historial de sus excusas (pin, motivo, estado).

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.sennaccess.data.Excusa
import com.example.sennaccess.data.ExcusaRepository
import com.example.sennaccess.data.SessionManager
import com.example.sennaccess.ui.CargaUiState
import com.example.sennaccess.ui.CargandoBox
import com.example.sennaccess.ui.ErrorBox
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import com.example.sennaccess.ui.theme.ErrorRed
import com.example.sennaccess.ui.ios.GlassCornerRadius
import com.example.sennaccess.ui.ios.glassSurface
import kotlinx.coroutines.launch

@Composable
fun MisExcusasView(onBack: (() -> Unit)? = null) {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    val repo = remember { ExcusaRepository() }
    val token = SessionManager.token
    var estado by remember { mutableStateOf<CargaUiState<List<Excusa>>>(CargaUiState.Loading) }

    suspend fun cargar() {
        if (token == null) { estado = CargaUiState.Error("Sin sesión"); return }
        try {
            estado = CargaUiState.Loading
            val lista = repo.misExcusas(token)
            estado = CargaUiState.Success(lista)
        } catch (e: Exception) { estado = CargaUiState.Error(e.message ?: "Error") }
    }

    LaunchedEffect(Unit) { cargar() }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = SenaGreen) }
            Text("Mis excusas", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { scope.launch { cargar() } }) { Icon(Icons.Default.Refresh, null, tint = colors.textSecondary) }
        }
        Text("Aquí ves las excusas que tu instructor generó para ti. Entrega el PIN en portería para salir.", color = colors.textSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))

        when (val s = estado) {
            is CargaUiState.Loading -> CargandoBox()
            is CargaUiState.Error -> ErrorBox(s.mensaje, onReintentar = { scope.launch { cargar() } })
            is CargaUiState.Success -> {
                val lista = s.datos
                if (lista.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().glassSurface(cornerRadius = GlassCornerRadius).padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("No tienes excusas registradas.", color = colors.textSecondary, fontSize = 13.sp)
                    }
                } else {
                    lista.forEach { ex ->
                        val c = when (ex.estado) { "pendiente" -> SenaGreen; "usada" -> Color(0xFF2E7D32); "expirada","anulada" -> ErrorRed; else -> colors.textSecondary }
                        Box(modifier = Modifier.fillMaxWidth().glassSurface(cornerRadius = 12.dp).padding(14.dp)) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ex.ambiente?.ambiente_nombre ?: "Ambiente #${ex.fk_id_ambiente}", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(ex.motivo ?: "", color = colors.textSecondary, fontSize = 12.sp)
                                    }
                                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).glassSurface(cornerRadius = 6.dp).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text(ex.estado?.uppercase() ?: "—", color = c, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                if (ex.estado == "pendiente") {
                                    Text("PIN: ${ex.pin ?: "—"}", color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                                    Text("Vigencia 60 min • expira ${ex.expira_en ?: ""}", color = colors.textSecondary, fontSize = 11.sp)
                                } else {
                                    Text("PIN: ${ex.pin ?: "—"} • ${ex.expira_en ?: ""}", color = colors.textSecondary, fontSize = 12.sp)
                                    if (ex.usado_en != null) Text("Usada: ${ex.usado_en}", color = colors.textSecondary, fontSize = 11.sp)
                                }
                                Text("Instructor: ${ex.instructor?.nombreCompleto ?: "#${ex.fk_id_instructor}"}", color = colors.textSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
