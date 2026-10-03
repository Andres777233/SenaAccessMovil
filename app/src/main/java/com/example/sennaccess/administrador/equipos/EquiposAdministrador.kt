package com.example.sennaccess.administrador.equipos

// Contenido del INVENTARIO DE EQUIPOS del ADMINISTRADOR.
// Tabla de vidrio con todos los equipos registrados en el centro (GET
// /admin/equipment), mostrando propietario, tipo, marca/modelo y serial.
// El admin es el único que registra equipos (botón "REGISTRAR EQUIPO" abre el
// formulario con selector de dueño, POST /admin/equipment con fk_id_usuario) y
// elimina registros (DELETE /admin/equipment/{id}) con confirmación.

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
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
import com.example.sennaccess.administrador.equipos.VistaRegistrarEquipo
import com.example.sennaccess.perfil.FotoPerfil
import com.example.sennaccess.datos.repositorios.RepositorioEquipos
import com.example.sennaccess.datos.modelos.EquipoIngreso
import com.example.sennaccess.datos.sesion.GestorSesion
import com.example.sennaccess.comun.EstadoCarga
import com.example.sennaccess.comun.EstadoContenido
import com.example.sennaccess.comun.VistaVacia
import com.example.sennaccess.comun.diseno.RadioVidrio
import com.example.sennaccess.comun.diseno.RadioSena
import com.example.sennaccess.comun.diseno.superficiePlana
import com.example.sennaccess.comun.diseno.EntradaSuave
import com.example.sennaccess.comun.diseno.TipoInsignia
import com.example.sennaccess.comun.diseno.InsigniaSena
import com.example.sennaccess.comun.diseno.CeldaSena
import com.example.sennaccess.comun.diseno.BuscadorSena
import com.example.sennaccess.comun.diseno.EspaciadoSena
import com.example.sennaccess.comun.diseno.escalaPresion
import com.example.sennaccess.comun.diseno.TarjetaSena
import com.example.sennaccess.comun.fechaLegible
import com.example.sennaccess.comun.tema.RojoError
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import kotlinx.coroutines.launch
@Composable
fun ContenidoEquiposAdmin(
    estado: EstadoCarga<List<EquipoIngreso>>,
    onReintentar: () -> Unit,
    onBack: () -> Unit
) {
    val colors = ColoresAppLocal.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var mostrandoRegistro by remember { mutableStateOf(false) }

    if (mostrandoRegistro) {
        VistaRegistrarEquipo(
            adminMode = true,
            onBack = { mostrandoRegistro = false },
            onRegistrado = {
                mostrandoRegistro = false
                onReintentar()
            }
        )
        return
    }

    var equipoAEliminar by remember { mutableStateOf<EquipoIngreso?>(null) }
    var eliminando by remember { mutableStateOf(false) }
    var errorEliminar by remember { mutableStateOf<String?>(null) }
    var busqueda by remember { mutableStateOf("") }
    // Filtro por rol del dueño: todos, aprendices, instructores o invitados.
    var filtroRol by remember { mutableStateOf(0) }
    val rolesFiltro = listOf("Todos", "Aprendices", "Instructores", "Invitados")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Cabecera editorial de inventario.
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = colors.textPrimary) }
                Column(modifier = Modifier.weight(1f)) {
                    Text("INVENTARIO", color = verdeMarca(), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
                    Text("Equipos", color = colors.textPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                }
            }
            Text("Portátiles y equipos registrados en el centro.", color = colors.textSecondary, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = { mostrandoRegistro = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .escalaPresion(pressedScale = 0.97f),
            colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = Color.Black),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("REGISTRAR EQUIPO", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))

        BuscadorSena(
            valor = busqueda,
            onValor = { busqueda = it },
            placeholder = "Buscar por cédula, propietario, tipo o serial..."
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items(rolesFiltro.size) { idx ->
                FilterChip(
                    selected = filtroRol == idx,
                    onClick = { filtroRol = idx },
                    label = { Text(rolesFiltro[idx], fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text("EQUIPOS REGISTRADOS", color = colors.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
        Spacer(modifier = Modifier.height(8.dp))
        run {
            EstadoContenido(estado = estado, onReintentar = onReintentar) { items ->
                val filtrados = items.filter { eq ->
                    val rol = eq.user?.role?.rol_name?.trim()?.lowercase()
                    val pasaRol = when (filtroRol) {
                        1 -> rol == "aprendiz"
                        2 -> rol == "instructor"
                        3 -> rol == "invitado" || rol == "visitante" || rol == "guest"
                        else -> true
                    }
                    val q = busqueda.trim()
                    pasaRol && (q.isBlank() ||
                        (eq.user?.user_identification ?: "").contains(q, ignoreCase = true) ||
                        eq.user?.nombreCompleto?.contains(q, ignoreCase = true) == true ||
                        eq.equipo_type?.contains(q, ignoreCase = true) == true ||
                        eq.marcaModelo.contains(q, ignoreCase = true) ||
                        eq.equipo_serial?.contains(q, ignoreCase = true) == true)
                }
                if (filtrados.isEmpty()) {
                    VistaVacia(
                        icono = Icons.Default.Devices,
                        titulo = if (items.isEmpty()) "No hay equipos registrados" else "Sin coincidencias",
                        mensaje = if (items.isEmpty()) {
                            "Los equipos que se registren en el centro aparecerán aquí."
                        } else {
                            "Ningún equipo coincide con \"$busqueda\"."
                        }
                    )
                } else {
                    // Tarjetas separadas por equipo: propietario + ficha técnica.
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        filtrados.forEachIndexed { i, eq ->
                            EntradaSuave(indice = i.coerceAtMost(4)) {
                                TarjetaEquipoAdmin(eq, onEliminar = { equipoAEliminar = eq })
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }

    equipoAEliminar?.let { eq ->
        AlertDialog(
            onDismissRequest = { equipoAEliminar = null },
            containerColor = colors.cardBackground.copy(alpha = 0.98f),
            shape = RoundedCornerShape(28.dp),
            icon = { Icon(Icons.Default.Delete, contentDescription = "Eliminar equipo", tint = RojoError, modifier = Modifier.size(36.dp)) },
            title = { Text("Eliminar equipo", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "¿Seguro que deseas eliminar el registro de ${eq.equipo_type ?: "equipo"} serial ${eq.equipo_serial ?: "—"}?",
                        color = colors.textSecondary
                    )
                    if (errorEliminar != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorEliminar!!, color = RojoError, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (eliminando) return@Button
                        val token = GestorSesion.token ?: return@Button
                        val id = eq.id_ingreso_equipo ?: return@Button
                        eliminando = true
                        scope.launch {
                            try {
                                RepositorioEquipos().eliminar(token, id)
                                eliminando = false
                                equipoAEliminar = null
                                errorEliminar = null
                                onReintentar()
                            } catch (e: retrofit2.HttpException) {
                                eliminando = false
                                errorEliminar = "Error ${e.code()}. No se pudo eliminar."
                            } catch (e: Exception) {
                                eliminando = false
                                errorEliminar = "No se pudo conectar al servidor."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RojoError, contentColor = Color.Black),
                    shape = RoundedCornerShape(28.dp)
                ) { Text(if (eliminando) "Eliminando..." else "Eliminar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { equipoAEliminar = null }) { Text("Cancelar", color = colors.textSecondary) }
            }
        )
    }
}

// Tarjeta profesional de equipo para admin/portería: cabecera con tipo +
// serial, dueño con avatar y ficha técnica en rejilla. Una tarjeta por equipo.
@Composable
private fun TarjetaEquipoAdmin(eq: EquipoIngreso, onEliminar: () -> Unit) {
    val colors = ColoresAppLocal.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .superficiePlana(cornerRadius = RadioSena.lg)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(46.dp).clip(CircleShape).background(VerdeSena.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Devices, null, tint = verdeMarca(), modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(eq.equipo_type ?: "Equipo", color = colors.textPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, maxLines = 1)
                Text("Serial ${eq.equipo_serial ?: "—"}", color = colors.textSecondary, fontSize = 12.sp, maxLines = 1)
            }
            IconButton(onClick = onEliminar, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar equipo", tint = RojoError, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = colors.divider)
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            FotoPerfil(fotoPath = eq.user?.profile_photo_path, nombre = eq.user?.nombreCompleto ?: "—", tamano = 36.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(eq.user?.nombreCompleto ?: "Sin propietario", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                Text(
                    listOfNotNull(eq.user?.user_identification, eq.user?.role?.rol_name).joinToString(" • "),
                    color = colors.textSecondary, fontSize = 12.sp, maxLines = 1
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(eq.marcaModelo, color = colors.textSecondary, fontSize = 12.sp, maxLines = 2)
    }
}

// Tarjeta detallada de equipo, compartida por instructor y aprendiz.
// Muestra todo lo registrado: tipo, marca, modelo, color, serial, observaciones,
// accesorios con su detalle y fecha de ingreso. El dueño solo se muestra en
// portería/admin (el aprendiz/instructor siempre es él mismo).
@Composable
fun TarjetaEquipoDetallada(eq: EquipoIngreso, mostrarDueno: Boolean = false) {
    val colors = ColoresAppLocal.current
    TarjetaSena {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Devices, contentDescription = null, tint = verdeMarca(), modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                eq.equipo_type ?: "Equipo",
                color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            InsigniaSena(texto = "Ingresado", tipo = TipoInsignia.EXITO)
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (mostrarDueno && eq.user != null) {
            FilaDetalleEquipo("Propietario", eq.user.nombreCompleto)
            FilaDetalleEquipo("Documento dueño", eq.user.user_identification ?: "—")
        }
        FilaDetalleEquipo("Marca", eq.equipo_brand ?: "—")
        FilaDetalleEquipo("Modelo", eq.equipo_model ?: "—")
        FilaDetalleEquipo("Color", eq.equipo_color ?: "—")
        FilaDetalleEquipo("Serial", eq.equipo_serial ?: "—")
        FilaDetalleEquipo("Fecha de ingreso", fechaLegible(eq.entry_datetime))
        if (!eq.equipo_observations.isNullOrBlank()) {
            FilaDetalleEquipo("Observaciones", eq.equipo_observations!!)
        }
        val accesorios = eq.equipo_accesorios.orEmpty()
        if (accesorios.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text("Accesorios (${accesorios.size})", color = verdeMarca(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            accesorios.forEach { acc ->
                val detalleAcc = listOfNotNull(
                    acc.marca?.takeIf { it.isNotBlank() },
                    acc.color?.takeIf { it.isNotBlank() },
                    if (acc.inalambrico == true) "inalámbrico" else null
                ).joinToString(" • ").ifBlank { "registrado" }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Box(
                        modifier = Modifier.size(8.dp).clip(CircleShape).background(VerdeSena.copy(alpha = 0.7f))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${acc.tipo}: $detalleAcc", color = colors.textSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun FilaDetalleEquipo(etiqueta: String, valor: String) {
    val colors = ColoresAppLocal.current
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("$etiqueta: ", color = colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(valor, color = colors.textPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
    }
}
