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
import com.example.sennaccess.ui.ios.GlassCornerRadius
import com.example.sennaccess.ui.ios.IosCollapsibleHeader
import com.example.sennaccess.ui.ios.pressScale
import com.example.sennaccess.ui.theme.ErrorRed
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = colors.textPrimary) }
        }

        IosCollapsibleHeader(
            title = "Inventario de Equipos",
            subtitle = "Equipos registrados en el centro",
            scrollOffset = scrollState.value.toFloat()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para abrir el formulario de registro de un equipo (modo admin).
        Button(
            onClick = { mostrandoRegistro = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .pressScale(pressedScale = 0.97f),
            colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("REGISTRAR EQUIPO", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TableContainer(title = "Equipos del Centro", subtitle = "Registros de ingreso de equipos") {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Text("PROPIETARIO", modifier = Modifier.width(140.dp), color = colors.textSecondary, fontSize = 12.sp)
                Text("EQUIPO", modifier = Modifier.width(90.dp), color = colors.textSecondary, fontSize = 12.sp)
                Text("MARCA/MODELO", modifier = Modifier.width(140.dp), color = colors.textSecondary, fontSize = 12.sp)
                Text("SERIAL", modifier = Modifier.width(110.dp), color = colors.textSecondary, fontSize = 12.sp)
            }
            HorizontalDivider(color = colors.border)

            EstadoContenido(estado = estado, onReintentar = onReintentar) { items ->
                if (items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("No hay equipos registrados.", color = colors.textSecondary, fontSize = 14.sp)
                    }
                } else {
                    items.forEach { eq ->
                        HorizontalDivider(color = colors.border)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.width(140.dp)) {
                                Text(eq.user?.nombreCompleto ?: "—", color = colors.textPrimary, fontSize = 13.sp)
                                Text(eq.user?.user_email ?: "", color = colors.textSecondary, fontSize = 10.sp)
                            }
                            Text(eq.equipo_type ?: "Equipo", modifier = Modifier.width(90.dp), color = colors.textPrimary, fontSize = 13.sp)
                            Text(eq.marcaModelo, modifier = Modifier.width(140.dp), color = colors.textPrimary, fontSize = 13.sp)
                            Text(eq.equipo_serial ?: "—", modifier = Modifier.width(110.dp), color = colors.textPrimary, fontSize = 13.sp)
                            IconButton(
                                onClick = { equipoAEliminar = eq },
                                modifier = Modifier.pressScale(pressedScale = 0.9f)
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
            shape = RoundedCornerShape(24.dp),
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(36.dp)) },
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
                    shape = RoundedCornerShape(12.dp)
                ) { Text(if (eliminando) "Eliminando..." else "Eliminar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { equipoAEliminar = null }) { Text("Cancelar", color = colors.textSecondary) }
            }
        )
    }
}
