// Contenido de la pestaña PRESENTES del ADMINISTRADOR.
// Muestra los ambientes (salones físicos) con sus aprendices/instructores y quiénes están DENTRO/FUERA.
package com.example.sennaccess.administrador.accesos

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.datos.modelos.Ambiente
import com.example.sennaccess.datos.modelos.Ingreso
import com.example.sennaccess.datos.modelos.Presente
import com.example.sennaccess.comun.EstadoCarga
import com.example.sennaccess.comun.CajaError
import com.example.sennaccess.comun.VistaVacia
import com.example.sennaccess.comun.ListaEsqueleto
import com.example.sennaccess.comun.diseno.BuscadorSena
import com.example.sennaccess.comun.fechaLegible
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import com.example.sennaccess.comun.diseno.RadioVidrio
import com.example.sennaccess.comun.diseno.CabeceraPlegable
import com.example.sennaccess.comun.diseno.superficieVidrio
data class AmbienteConPresencia(
    val ambiente: Ambiente,
    val presentes: List<Presente>,
    val total: Int
) {
    val fichas: List<Int> get() = (ambiente.aprendices ?: emptyList()).mapNotNull { it.user_coursenumber }.distinct()
}

@Composable
fun VistaPersonasDentro(
    ambientesEstado: EstadoCarga<List<Ambiente>>,
    presentesEstado: EstadoCarga<List<Presente>>,
    ingresosEstado: EstadoCarga<List<Ingreso>>,
    onReintentar: () -> Unit
) {
    val colors = ColoresAppLocal.current
    var busqueda by remember { mutableStateOf("") }

    val ultimaEntradaPorUsuario: Map<Int?, String> = if (ingresosEstado is EstadoCarga.Success) {
        ingresosEstado.datos.groupBy { it.fk_id_user }.mapValues { (_, lista) ->
            val entradas = lista.filter { it.ingreso_type.equals("Entrada", ignoreCase = true) }
                .mapNotNull { it.ingreso_datetime }
            val candidatas = if (entradas.isNotEmpty()) entradas else lista.mapNotNull { it.ingreso_datetime }
            candidatas.maxOrNull() ?: ""
        }
    } else emptyMap()

    val ambientesConPresencia = when {
        ambientesEstado is EstadoCarga.Success && presentesEstado is EstadoCarga.Success -> {
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

    val q = busqueda.trim()
    val filtrados = ambientesConPresencia.filter { item ->
        q.isBlank() || item.fichas.any { it.toString().contains(q) } ||
            item.presentes.any { it.nombreCompleto.contains(q, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CabeceraPlegable(
            title = "Ambientes",
            subtitle = "Presencia por salón",
            scrollOffset = 0f
        )
        Spacer(modifier = Modifier.height(16.dp))

        BuscadorSena(
            valor = busqueda,
            onValor = { busqueda = it },
            placeholder = "Buscar por ficha o nombre"
        )
        Spacer(modifier = Modifier.height(16.dp))

        when {
            ambientesEstado is EstadoCarga.Loading || presentesEstado is EstadoCarga.Loading -> {
                ListaEsqueleto(count = 3)
            }
            ambientesEstado is EstadoCarga.Error -> CajaError(ambientesEstado.mensaje, onReintentar)
            presentesEstado is EstadoCarga.Error -> CajaError(presentesEstado.mensaje, onReintentar)
            filtrados.isEmpty() -> VistaVacia(
                icono = Icons.Default.MeetingRoom,
                titulo = if (q.isNotBlank()) "Sin coincidencias" else "Sin ambientes",
                mensaje = if (q.isNotBlank()) "Nada coincide con «$q». Borra la búsqueda para ver todos."
                else "No hay ambientes con aprendices o instructores asignados."
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
    val colors = ColoresAppLocal.current
    var expandido by remember { mutableStateOf(false) }
    val dentro = item.presentes.size
    val total = item.total

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .superficieVidrio(cornerRadius = RadioVidrio)
            .clickable { expandido = !expandido },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(RadioVidrio)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(VerdeSena.copy(alpha = 0.15f)),
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        if (!item.ambiente.ambiente_ubicacion.isNullOrBlank()) {
                            Text(item.ambiente.ambiente_ubicacion!!, color = colors.textSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (!item.ambiente.ambiente_jornada.isNullOrBlank()) {
                            Text(item.ambiente.ambiente_jornada!!, color = colors.textSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    usuario: com.example.sennaccess.datos.modelos.UsuarioApi,
    presente: Presente?,
    ultimaEntrada: String?
) {
    val colors = ColoresAppLocal.current
    val estaDentro = presente != null
    val esInstructor = usuario.esRol("Instructor")
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (estaDentro) VerdeSena.copy(alpha = 0.2f) else colors.borderLight),
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
            Text(
                usuario.nombreCompleto,
                color = if (estaDentro) colors.textPrimary else colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            if (estaDentro) {
                Text(
                    "Entró: ${fechaLegible(presente!!.entrada_hora)}",
                    color = verdeMarca(),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else if (!ultimaEntrada.isNullOrBlank()) {
                Text(
                    "Última: ${fechaLegible(ultimaEntrada)}",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                val extras = mutableListOf<String>()
                extras.add(usuario.role?.rol_name ?: if (esInstructor) "Instructor" else "")
                usuario.user_coursenumber?.let { extras.add("Ficha $it") }
                Text(
                    extras.filter { it.isNotBlank() }.joinToString(" · "),
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
