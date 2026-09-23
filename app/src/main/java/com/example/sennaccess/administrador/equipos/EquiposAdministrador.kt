package com.example.sennaccess.administrador.equipos

// Contenido del INVENTARIO DE EQUIPOS del ADMINISTRADOR.
// Tabla de vidrio con todos los equipos registrados en el centro (GET
// /admin/equipment), mostrando propietario, tipo, marca/modelo y serial.
// El admin es el único que registra equipos (botón "REGISTRAR EQUIPO" abre el
// formulario con selector de dueño, POST /admin/equipment con fk_id_usuario) y
// elimina registros (DELETE /admin/equipment/{id}) con confirmación.

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.administrador.equipos.VistaRegistrarEquipo
import com.example.sennaccess.aprendiz.panel.ContenedorTabla
import com.example.sennaccess.datos.repositorios.RepositorioEquipos
import com.example.sennaccess.datos.modelos.EquipoIngreso
import com.example.sennaccess.datos.sesion.GestorSesion
import com.example.sennaccess.comun.EstadoCarga
import com.example.sennaccess.comun.EstadoContenido
import com.example.sennaccess.comun.VistaVacia
import com.example.sennaccess.comun.diseno.RadioVidrio
import com.example.sennaccess.comun.diseno.CabeceraPlegable
import com.example.sennaccess.comun.diseno.TipoInsignia
import com.example.sennaccess.comun.diseno.InsigniaSena
import com.example.sennaccess.comun.diseno.CeldaSena
import com.example.sennaccess.comun.diseno.BuscadorSena
import com.example.sennaccess.comun.diseno.EspaciadoSena
import com.example.sennaccess.comun.diseno.escalaPresion
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = colors.textPrimary) }
        }

        CabeceraPlegable(
            title = "Inventario de Equipos",
            subtitle = "Equipos registrados en el centro",
            scrollOffset = scrollState.value.toFloat()
        )

        Spacer(modifier = Modifier.height(EspaciadoSena.md))

        Button(
            onClick = { mostrandoRegistro = true },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .escalaPresion(pressedScale = 0.97f),
            colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = Color.Black),
            shape = RoundedCornerShape(28.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("REGISTRAR EQUIPO", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        Spacer(modifier = Modifier.height(EspaciadoSena.md))

        BuscadorSena(
            valor = busqueda,
            onValor = { busqueda = it },
            placeholder = "Buscar por propietario, tipo o serial..."
        )

        Spacer(modifier = Modifier.height(EspaciadoSena.md))

        ContenedorTabla(title = "Equipos del Centro", subtitle = "Registros de ingreso de equipos") {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                CeldaSena(texto = "PROPIETARIO", peso = 2f, encabezado = true)
                CeldaSena(texto = "EQUIPO", peso = 1f, encabezado = true)
                CeldaSena(texto = "MARCA", peso = 1.5f, encabezado = true)
                CeldaSena(texto = "SERIAL", peso = 1.2f, encabezado = true)
                Spacer(modifier = Modifier.width(48.dp))
            }
            HorizontalDivider(color = colors.border)

            EstadoContenido(estado = estado, onReintentar = onReintentar) { items ->
                val filtrados = items.filter { eq ->
                    busqueda.isBlank() ||
                        eq.user?.nombreCompleto?.contains(busqueda, ignoreCase = true) == true ||
                        eq.equipo_type?.contains(busqueda, ignoreCase = true) == true ||
                        eq.marcaModelo.contains(busqueda, ignoreCase = true) ||
                        eq.equipo_serial?.contains(busqueda, ignoreCase = true) == true
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
                    filtrados.forEach { eq ->
                        HorizontalDivider(color = colors.border)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(2f).padding(horizontal = EspaciadoSena.xs)) {
                                Text(eq.user?.nombreCompleto ?: "—", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(eq.user?.user_email ?: "", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            CeldaSena(texto = eq.equipo_type ?: "Equipo", peso = 1f)
                            CeldaSena(texto = eq.marcaModelo, peso = 1.5f)
                            CeldaSena(texto = eq.equipo_serial ?: "—", peso = 1.2f)
                            Spacer(modifier = Modifier.width(EspaciadoSena.xs))
                            IconButton(
                                onClick = { equipoAEliminar = eq },
                                modifier = Modifier.size(48.dp).escalaPresion(pressedScale = 0.9f)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar equipo", tint = RojoError, modifier = Modifier.size(20.dp))
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
