package com.example.sennaccess.ambientes

// Pantalla del instructor: lista de ambientes donde enseña ("Mis Ambientes").
// Desde aquí puede abrir cada ambiente para gestionar estudiantes y proyectar el QR.

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.data.Ambiente
import com.example.sennaccess.data.AmbienteRepository
import com.example.sennaccess.data.SessionManager
import com.example.sennaccess.ui.CargaUiState
import com.example.sennaccess.ui.CargandoBox
import com.example.sennaccess.ui.ErrorBox
import com.example.sennaccess.ui.EstadoVacio
import com.example.sennaccess.ui.ios.GlassCornerRadius
import com.example.sennaccess.ui.ios.IosCollapsibleHeader
import com.example.sennaccess.ui.ios.glassSurface
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import kotlinx.coroutines.launch

@Composable
fun MisAmbientesView(
    onBack: (() -> Unit)? = null,
    onAmbienteClick: (Ambiente) -> Unit
) {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    val repo = remember { AmbienteRepository() }
    val token = SessionManager.token

    var estado by remember { mutableStateOf<CargaUiState<List<Ambiente>>>(CargaUiState.Loading) }

    suspend fun cargar() {
        if (token == null) { estado = CargaUiState.Error("Sin sesión"); return }
        try {
            estado = CargaUiState.Loading
            val lista = repo.getMisAmbientes(token)
            estado = CargaUiState.Success(lista)
        } catch (e: Exception) { estado = CargaUiState.Error(e.message ?: "Error") }
    }

    LaunchedEffect(Unit) { cargar() }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(Icons.Default.MeetingRoom, null, tint = colors.textPrimary) }
            }
        }
        IosCollapsibleHeader(title = "Mis Ambientes", subtitle = "Salones donde enseñas — toca uno para gestionar estudiantes y QR", scrollOffset = 0f)
        Spacer(modifier = Modifier.height(16.dp))

        when (val s = estado) {
            is CargaUiState.Loading -> CargandoBox()
            is CargaUiState.Error -> ErrorBox(s.mensaje, onReintentar = { scope.launch { cargar() } })
            is CargaUiState.Success -> {
                if (s.datos.isEmpty()) {
                    EstadoVacio(
                        icono = Icons.Default.MeetingRoom,
                        titulo = "Aún no tienes ambientes asignados",
                        mensaje = "El admin debe asignarte a un ambiente (CCyS, Ciudad Jardín, etc.). Una vez asignado, podrás gestionar los aprendices de ese salón y proyectar su QR."
                    )
                } else {
                    s.datos.forEach { amb ->
                        MisAmbienteCard(amb, onClick = { onAmbienteClick(amb) })
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun MisAmbienteCard(amb: Ambiente, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Box(modifier = Modifier.fillMaxWidth().glassSurface(cornerRadius = GlassCornerRadius).clickable(onClick = onClick).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(SenaGreen.copy(0.15f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.MeetingRoom, null, tint = SenaGreen, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(amb.ambiente_nombre ?: "Ambiente", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${amb.ambiente_ubicacion ?: "Sin ubicación"} • ${amb.ambiente_jornada ?: ""}".trim(), color = colors.textSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Groups, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${amb.aprendices_count ?: amb.aprendices?.size ?: 0} aprendices", color = colors.textSecondary, fontSize = 12.sp)
                    if (amb.ambiente_capacidad != null) { Spacer(modifier = Modifier.width(8.dp)); Text("cap. ${amb.ambiente_capacidad}", color = colors.textSecondary, fontSize = 12.sp) }
                }
            }
        }
    }
}
