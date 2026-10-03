package com.example.sennaccess.instructor.ambientes

// Pantalla del instructor: lista de ambientes donde enseña ("Mis Ambientes").
// Desde aquí puede abrir cada ambiente para gestionar estudiantes y proyectar el QR.

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
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
import com.example.sennaccess.datos.modelos.Ambiente
import com.example.sennaccess.datos.repositorios.RepositorioAmbientes
import com.example.sennaccess.datos.sesion.GestorSesion
import com.example.sennaccess.comun.EstadoCarga
import com.example.sennaccess.comun.CajaCargando
import com.example.sennaccess.comun.CajaError
import com.example.sennaccess.comun.VistaVacia
import com.example.sennaccess.comun.diseno.RadioVidrio
import com.example.sennaccess.comun.diseno.superficiePlana
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.diseno.EntradaSuave
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import kotlinx.coroutines.launch

@Composable
fun VistaMisAmbientes(
    onBack: (() -> Unit)? = null,
    onAmbienteClick: (Ambiente) -> Unit
) {
    val colors = ColoresAppLocal.current
    val scope = rememberCoroutineScope()
    val repo = remember { RepositorioAmbientes() }
    val token = GestorSesion.token

    var estado by remember { mutableStateOf<EstadoCarga<List<Ambiente>>>(EstadoCarga.Loading) }

    suspend fun cargar() {
        if (token == null) { estado = EstadoCarga.Error("Sin sesión"); return }
        try {
            estado = EstadoCarga.Loading
            val lista = repo.getMisAmbientes(token)
            estado = EstadoCarga.Success(lista)
        } catch (e: Exception) { estado = EstadoCarga.Error(e.message ?: "Error") }
    }

    LaunchedEffect(Unit) { cargar() }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()) {
        // Cabecera editorial: eyebrow + título + conteo de salones.
        val total = (estado as? EstadoCarga.Success)?.datos?.size ?: 0
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "AULAS ASIGNADAS${if (total > 0) " • $total" else ""}",
                color = verdeMarca(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Mis Ambientes", color = colors.textPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = { scope.launch { cargar() } }, modifier = Modifier.size(44.dp)) { Icon(Icons.Default.Refresh, null, tint = colors.textSecondary) }
            }
            Text("Toca un salón para gestionar sus estudiantes y permisos.", color = colors.textSecondary, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))

        when (val s = estado) {
            is EstadoCarga.Loading -> CajaCargando()
            is EstadoCarga.Error -> CajaError(s.mensaje, onReintentar = { scope.launch { cargar() } })
            is EstadoCarga.Success -> {
                if (s.datos.isEmpty()) {
                    VistaVacia(
                        icono = Icons.Default.MeetingRoom,
                        titulo = "Aún no tienes ambientes asignados",
                        mensaje = "El admin debe asignarte a un ambiente (CCyS, Ciudad Jardín, etc.). Una vez asignado, podrás gestionar los aprendices de ese salón y proyectar su QR."
                    )
                } else {
                    // Tarjetas separadas con aire: cada ambiente respira solo.
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                        s.datos.forEachIndexed { i, amb ->
                            EntradaSuave(indice = i.coerceAtMost(4)) {
                                MisAmbienteCard(amb, onClick = { onAmbienteClick(amb) })
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun MisAmbienteCard(amb: Ambiente, onClick: () -> Unit) {
    val colors = ColoresAppLocal.current
    // Tarjeta editorial por ambiente: franja superior verde + cuerpo en plana.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .superficiePlana(cornerRadius = RadioVidrio)
            .clickable(onClick = onClick)
    ) {
        // Franja de identidad: icono + nombre + jornada píldora + chevron.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(VerdeSena.copy(0.15f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.MeetingRoom, null, tint = verdeMarca(), modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (!amb.ambiente_jornada.isNullOrBlank()) {
                    Box(modifier = Modifier.clip(CircleShape).background(VerdeSena.copy(0.15f)).padding(horizontal = 10.dp, vertical = 3.dp)) {
                        Text(amb.ambiente_jornada!!.uppercase(), color = verdeMarca(), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.0.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(amb.ambiente_nombre ?: "Ambiente", color = colors.textPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, maxLines = 1)
                Text(amb.ambiente_ubicacion ?: "Sin ubicación", color = colors.textSecondary, fontSize = 12.sp, maxLines = 1)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Abrir ambiente", tint = verdeMarca(), modifier = Modifier.size(26.dp))
        }
        HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = 16.dp))
        // Pie con ocupación: conteo + capacidad + barra de progreso.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Default.Groups, null, tint = verdeMarca(), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            val n = amb.aprendices_count ?: amb.aprendices?.size ?: 0
            Text("$n aprendices", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (amb.ambiente_capacidad != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Text("de ${amb.ambiente_capacidad}", color = colors.textSecondary, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text("GESTIONAR", color = verdeMarca(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        }
    }
}
