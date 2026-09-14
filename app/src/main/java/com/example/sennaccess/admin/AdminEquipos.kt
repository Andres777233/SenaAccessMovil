package com.example.sennaccess.admin

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.aprendiz.RegistrarEquipoView
import com.example.sennaccess.aprendiz.TableContainer
import com.example.sennaccess.data.EquipoRepository
import com.example.sennaccess.data.IngresoEquipo
import com.example.sennaccess.data.SessionManager
import com.example.sennaccess.ui.CargaUiState
import com.example.sennaccess.ui.EstadoContenido
import com.example.sennaccess.ui.EstadoVacio
import com.example.sennaccess.ui.ios.GlassCornerRadius
import com.example.sennaccess.ui.ios.IosCollapsibleHeader
import com.example.sennaccess.ui.ds.BadgeTipo
import com.example.sennaccess.ui.ds.SenaBadge
import com.example.sennaccess.ui.ds.SenaCell
import com.example.sennaccess.ui.ds.SenaSearchField
import com.example.sennaccess.ui.ds.SenaSpacing
import com.example.sennaccess.ui.ios.pressScale
import com.example.sennaccess.ui.theme.ErrorRed
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import com.example.sennaccess.ui.theme.verdeMarca
import kotlinx.coroutines.launch
@Composable
fun AdminEquiposContent(
    estado: CargaUiState<List<IngresoEquipo>>,
    onReintentar: () -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Al pulsar "REGISTRAR EQUIPO" se muestra el formulario en modo admin.
    var mostrandoRegistro by remember { mutableStateOf(false) }

    if (mostrandoRegistro) {
        RegistrarEquipoView(
            adminMode = true,
            onBack = { mostrandoRegistro = false },
            onRegistrado = {
                mostrandoRegistro = false
                onReintentar()
            }
        )
        return
    }

    // Equipo pendiente de eliminar (se confirma con un diálogo antes de borrar).
    var equipoAEliminar by remember { mutableStateOf<IngresoEquipo?>(null) }
    var eliminando by remember { mutableStateOf(false) }
    var errorEliminar by remember { mutableStateOf<String?>(null) }
    // Texto de búsqueda local por propietario, tipo, marca o serial.
    var busqueda by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = colors.textPrimary) }
        }

        IosCollapsibleHeader(
            title = "Inventario de Equipos",
            subtitle = "Equipos registrados en el centro",
            scrollOffset = scrollState.value.toFloat()
        )

        Spacer(modifier = Modifier.height(SenaSpacing.md))

        // Botón para abrir el formulario de registro de un equipo (modo admin).
        Button(
            onClick = { mostrandoRegistro = true },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .pressScale(pressedScale = 0.97f),
            colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black),
            shape = RoundedCornerShape(28.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("REGISTRAR EQUIPO", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        Spacer(modifier = Modifier.height(SenaSpacing.md))

        SenaSearchField(
            valor = busqueda,
            onValor = { busqueda = it },
            placeholder = "Buscar por propietario, tipo o serial..."
        )

        Spacer(modifier = Modifier.height(SenaSpacing.md))

        TableContainer(title = "Equipos del Centro", subtitle = "Registros de ingreso de equipos") {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                SenaCell(texto = "PROPIETARIO", peso = 2f, encabezado = true)
                SenaCell(texto = "EQUIPO", peso = 1f, encabezado = true)
                SenaCell(texto = "MARCA", peso = 1.5f, encabezado = true)
                SenaCell(texto = "SERIAL", peso = 1.2f, encabezado = true)
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
                    EstadoVacio(
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
                            Column(modifier = Modifier.weight(2f)) {
                                Text(eq.user?.nombreCompleto ?: "—", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary, maxLines = 1)
                                Text(eq.user?.user_email ?: "", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 1)
                            }
                            SenaCell(texto = eq.equipo_type ?: "Equipo", peso = 1f)
                            SenaCell(texto = eq.marcaModelo, peso = 1.5f)
                            SenaCell(texto = eq.equipo_serial ?: "—", peso = 1.2f)
                            IconButton(
                                onClick = { equipoAEliminar = eq },
                                modifier = Modifier.size(48.dp).pressScale(pressedScale = 0.9f)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar equipo", tint = ErrorRed, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }

    // Confirmación antes de eliminar un registro de equipo.
    equipoAEliminar?.let { eq ->
        AlertDialog(
            onDismissRequest = { equipoAEliminar = null },
            containerColor = colors.cardBackground.copy(alpha = 0.98f),
            shape = RoundedCornerShape(28.dp),
            icon = { Icon(Icons.Default.Delete, contentDescription = "Eliminar equipo", tint = ErrorRed, modifier = Modifier.size(36.dp)) },
            title = { Text("Eliminar equipo", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "¿Seguro que deseas eliminar el registro de ${eq.equipo_type ?: "equipo"} serial ${eq.equipo_serial ?: "—"}?",
                        color = colors.textSecondary
                    )
                    if (errorEliminar != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorEliminar!!, color = ErrorRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (eliminando) return@Button
                        val token = SessionManager.token ?: return@Button
                        val id = eq.id_ingreso_equipo ?: return@Button
                        eliminando = true
                        scope.launch {
                            try {
                                EquipoRepository().eliminar(token, id)
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
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White),
                    shape = RoundedCornerShape(28.dp)
                ) { Text(if (eliminando) "Eliminando..." else "Eliminar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { equipoAEliminar = null }) { Text("Cancelar", color = colors.textSecondary) }
            }
        )
    }
}
