// Contenido de la pestaña PRESENTES del ADMINISTRADOR.
// Muestra los ambientes (salones físicos) con sus aprendices/instructores y quiénes están DENTRO/FUERA.
package com.example.sennaccess.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.data.Ambiente
import com.example.sennaccess.data.Ingreso
import com.example.sennaccess.data.Presente
import com.example.sennaccess.ui.CargaUiState
import com.example.sennaccess.ui.ErrorBox
import com.example.sennaccess.ui.EstadoVacio
import com.example.sennaccess.ui.SkeletonList
import com.example.sennaccess.ui.ds.SenaSearchField
import com.example.sennaccess.ui.fechaLegible
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import com.example.sennaccess.ui.theme.verdeMarca
import com.example.sennaccess.ui.ios.GlassCornerRadius
import com.example.sennaccess.ui.ios.IosCollapsibleHeader
import com.example.sennaccess.ui.ios.glassSurface

// Ambiente con estado de presencia deducido.
data class AmbienteConPresencia(
    val ambiente: Ambiente,
    val presentes: List<Presente>, // Personas de este ambiente que están dentro
    val total: Int               // Total de personas (aprendices + instructores) en el ambiente
) {
    val fichas: List<Int> get() = (ambiente.aprendices ?: emptyList()).mapNotNull { it.user_coursenumber }.distinct()
}

@Composable
fun PresentesView(
    ambientesEstado: CargaUiState<List<Ambiente>>,
    presentesEstado: CargaUiState<List<Presente>>,
    ingresosEstado: CargaUiState<List<Ingreso>>,
    onReintentar: () -> Unit
) {
    val colors = LocalAppColors.current
    var busqueda by remember { mutableStateOf("") }

    // Última fecha de entrada por usuario, deducida del historial global de ingresos.
    val ultimaEntradaPorUsuario: Map<Int?, String> = if (ingresosEstado is CargaUiState.Success) {
        ingresosEstado.datos.groupBy { it.fk_id_user }.mapValues { (_, lista) ->
            val entradas = lista.filter { it.ingreso_type.equals("Entrada", ignoreCase = true) }
                .mapNotNull { it.ingreso_datetime }
            val candidatas = if (entradas.isNotEmpty()) entradas else lista.mapNotNull { it.ingreso_datetime }
            candidatas.maxOrNull() ?: ""
        }
    } else emptyMap()

    // Combinar ambientes y presentes en una lista de AmbienteConPresencia.
    // Solo datos reales del servidor: sin ejemplos demo mezclados en producción.
    val ambientesConPresencia = when {
        ambientesEstado is CargaUiState.Success && presentesEstado is CargaUiState.Success -> {
            val presentesMap = presentesEstado.datos.groupBy { it.id_usuario }
            val reales = ambientesEstado.datos.map { amb ->
                val miembros = (amb.aprendices ?: emptyList()) + (amb.instructores ?: emptyList())
                val presentesEnAmbiente = miembros.mapNotNull { usuario ->
                    presentesMap[usuario.id_usuario]?.firstOrNull()
                }
                AmbienteConPresencia(
                    ambiente = amb,
                    presentes = presentesEnAmbiente,
                    total = miembros.size
                )
            }
            reales.filter { it.total > 0 || it.presentes.isNotEmpty() }
        }
        else -> emptyList()
    }

    // Filtrar solo por ficha (grupo de estudiantes), no por nombre de ambiente.
    val filtrados = ambientesConPresencia.filter { item ->
        busqueda.isBlank() || item.fichas.any { it.toString().contains(busqueda) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        IosCollapsibleHeader(
            title = "Ambientes",
            subtitle = "Presencia por salón",
            scrollOffset = 0f
        )
        Spacer(modifier = Modifier.height(16.dp))

        SenaSearchField(
            valor = busqueda,
            onValor = { busqueda = it },
            placeholder = "Buscar por Ficha"
        )
        Spacer(modifier = Modifier.height(16.dp))

        when {
            ambientesEstado is CargaUiState.Loading || presentesEstado is CargaUiState.Loading -> {
                SkeletonList(count = 3)
            }
            ambientesEstado is CargaUiState.Error -> ErrorBox(ambientesEstado.mensaje, onReintentar)
            presentesEstado is CargaUiState.Error -> ErrorBox(presentesEstado.mensaje, onReintentar)
            filtrados.isEmpty() -> EstadoVacio(
                icono = Icons.Default.MeetingRoom,
                titulo = "Sin ambientes",
                mensaje = "No hay ambientes con aprendices o instructores asignados."
            )
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filtrados) { item ->
                    TarjetaAmbiente(item, ultimaEntradaPorUsuario)
                }
            }
        }
    }
}

@Composable
private fun TarjetaAmbiente(item: AmbienteConPresencia, ultimaEntradaPorUsuario: Map<Int?, String>) {
    val colors = LocalAppColors.current
    var expandido by remember { mutableStateOf(false) }
    val dentro = item.presentes.size
    val total = item.total

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = GlassCornerRadius)
            .clickable { expandido = !expandido },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(GlassCornerRadius)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SenaGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MeetingRoom, null, tint = verdeMarca(), modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.ambiente.ambiente_nombre ?: "Sin nombre",
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!item.ambiente.ambiente_ubicacion.isNullOrBlank()) {
                            Text(item.ambiente.ambiente_ubicacion!!, color = colors.textSecondary, fontSize = 11.sp)
                        }
                        if (!item.ambiente.ambiente_jornada.isNullOrBlank()) {
                            Text(item.ambiente.ambiente_jornada!!, color = colors.textSecondary, fontSize = 11.sp)
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("$dentro / $total", color = if (dentro > 0) verdeMarca() else colors.textSecondary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Dentro", color = colors.textSecondary, fontSize = 10.sp)
                }
            }
            if (total > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = dentro.toFloat() / total,
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = verdeMarca(),
                    trackColor = colors.borderLight
                )
            }

            if (expandido) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = colors.border)
                Spacer(modifier = Modifier.height(8.dp))
                val miembros = (item.ambiente.aprendices ?: emptyList()) + (item.ambiente.instructores ?: emptyList())
                if (miembros.isEmpty()) {
                    Text("Sin miembros asignados", color = colors.textSecondary, fontSize = 12.sp)
                } else {
                    miembros.forEach { usuario ->
                        val presente = item.presentes.find { it.id_usuario == usuario.id_usuario }
                        FilaPersonaAmbiente(usuario, presente, ultimaEntradaPorUsuario[usuario.id_usuario])
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaPersonaAmbiente(
    usuario: com.example.sennaccess.data.UsuarioApi,
    presente: Presente?,
    ultimaEntrada: String?
) {
    val colors = LocalAppColors.current
    val estaDentro = presente != null
    val esInstructor = usuario.esRol("Instructor")
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (estaDentro) SenaGreen.copy(alpha = 0.2f) else colors.borderLight),
            contentAlignment = Alignment.Center
        ) {
            if (estaDentro) {
                Icon(Icons.Default.CheckCircle, null, tint = verdeMarca(), modifier = Modifier.size(16.dp))
            } else {
                Icon(Icons.Default.Circle, null, tint = colors.textSecondary, modifier = Modifier.size(12.dp))
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    usuario.nombreCompleto,
                    color = if (estaDentro) colors.textPrimary else colors.textSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                if (estaDentro) {
                    Text(
                        "Entró: ${fechaLegible(presente!!.entrada_hora)}",
                        color = verdeMarca(),
                        fontSize = 11.sp
                    )
                } else if (!ultimaEntrada.isNullOrBlank()) {
                    Text(
                        "Última: ${fechaLegible(ultimaEntrada)}",
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            val extras = mutableListOf<String>()
            extras.add(usuario.role?.rol_name ?: if (esInstructor) "Instructor" else "")
            usuario.user_coursenumber?.let { extras.add("Ficha $it") }
            Text(extras.filter { it.isNotBlank() }.joinToString(" · "), color = colors.textSecondary, fontSize = 11.sp)
        }
    }
}