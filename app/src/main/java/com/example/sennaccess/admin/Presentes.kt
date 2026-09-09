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
import com.example.sennaccess.ui.fechaLegible
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
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

    // Combinar ambientes y presentes en una lista de AmbienteConPresencia
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
            // Agregar 5 ambientes de ejemplo si la lista real es corta (menos de 3)
            val ejemplos = if (reales.size < 3) {
                listOf(
                    crearAmbienteEjemplo("301", "CCyS", "Mañana", listOf("Andrés Vargas" to 3142101, "Laura Medina" to 3142101)),
                    crearAmbienteEjemplo("302", "Ciudad Jardín", "Tarde", listOf("Carlos Pérez" to 3142102)),
                    crearAmbienteEjemplo("303", "CCyS", "Noche", emptyList()),
                    crearAmbienteEjemplo("304", "Ciudad Jardín", "Mañana", listOf("Ana Gómez" to 3142103)),
                    crearAmbienteEjemplo("305", "CCyS", "Tarde", listOf("Luis Fernández" to 3142104))
                )
            } else emptyList()
            (reales + ejemplos).filter { it.total > 0 || it.presentes.isNotEmpty() }
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

        OutlinedTextField(
            value = busqueda,
            onValueChange = { busqueda = it },
            placeholder = { Text("Buscar por Ficha", color = colors.textSecondary) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, null, tint = colors.textSecondary) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SenaGreen,
                unfocusedBorderColor = colors.borderLight,
                cursorColor = SenaGreen,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            )
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
                    Icon(Icons.Default.MeetingRoom, null, tint = SenaGreen, modifier = Modifier.size(22.dp))
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
                    Text("$dentro / $total", color = if (dentro > 0) SenaGreen else colors.textSecondary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Dentro", color = colors.textSecondary, fontSize = 10.sp)
                }
            }
            if (total > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = dentro.toFloat() / total,
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = SenaGreen,
                    trackColor = colors.borderLight
                )
            }

            if (expandido) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = colors.border)
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
                Icon(Icons.Default.CheckCircle, null, tint = SenaGreen, modifier = Modifier.size(16.dp))
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
                        color = SenaGreen,
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

// Función auxiliar para crear ambientes de ejemplo con miembros ficticios
private fun crearAmbienteEjemplo(nombre: String, ubicacion: String, jornada: String, aprendices: List<Pair<String, Int>>): AmbienteConPresencia {
    val usuarios = aprendices.mapIndexed { idx, (nom, ficha) ->
        com.example.sennaccess.data.UsuarioApi(
            id_usuario = 1000 + idx,
            user_name = nom.split(" ").firstOrNull(),
            user_lastname = nom.split(" ").lastOrNull(),
            user_email = "${nom.lowercase().replace(" ", ".")}@sena.edu.co",
            user_identification = "123456${idx}",
            role = com.example.sennaccess.data.Role(1, "Aprendiz"),
            user_coursenumber = ficha,
            user_program = "Tecnólogo",
            profile_photo_path = null,
            user_documento_tipo = "CC",
            user_telefono = null
        )
    }
    val ambiente = com.example.sennaccess.data.Ambiente(
        id_ambiente = 1000 + nombre.hashCode(),
        ambiente_nombre = nombre,
        ambiente_ubicacion = ubicacion,
        ambiente_jornada = jornada,
        ambiente_capacidad = 30,
        instructores = emptyList(),
        aprendices = usuarios,
        fk_id_instructor = null,
        hora_inicio = null,
        hora_fin = null,
        ambiente_estado = "Activo",
        aprendices_count = usuarios.size
    )
    val presentes = when (nombre) {
        "301" -> usuarios.take(2).map { u ->
            com.example.sennaccess.data.Presente(
                id_usuario = u.id_usuario,
                user_name = u.user_name,
                user_lastname = u.user_lastname,
                rol = "Aprendiz",
                entrada_hora = "2026-09-06 08:00:00"
            )
        }
        "302" -> usuarios.take(1).map { u ->
            com.example.sennaccess.data.Presente(
                id_usuario = u.id_usuario,
                user_name = u.user_name,
                user_lastname = u.user_lastname,
                rol = "Aprendiz",
                entrada_hora = "2026-09-06 09:00:00"
            )
        }
        else -> emptyList()
    }
    return AmbienteConPresencia(ambiente, presentes, usuarios.size)
}