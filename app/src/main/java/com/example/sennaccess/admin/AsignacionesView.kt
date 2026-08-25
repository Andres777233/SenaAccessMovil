package com.example.sennaccess.admin

// Pantalla de ASIGNACIONES aprendiz→instructor del admin.
// Lista las asignaciones actuales (GET /admin/aprendiz-instructores), permite
// crear una nueva (POST) y eliminar (DELETE) con confirmación.

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.aprendiz.TableContainer
import com.example.sennaccess.data.AprendizInstructor
import com.example.sennaccess.data.AsignacionRequest
import com.example.sennaccess.data.UsuarioApi
import com.example.sennaccess.ui.CargaUiState
import com.example.sennaccess.ui.EstadoContenido
import com.example.sennaccess.ui.ios.IosCollapsibleHeader
import com.example.sennaccess.ui.ios.pressScale
import com.example.sennaccess.ui.theme.ErrorRed
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen

@Composable
fun AsignacionesView(
    estado: CargaUiState<List<AprendizInstructor>>,
    instructores: List<UsuarioApi>,
    aprendices: List<UsuarioApi>,
    onReintentar: () -> Unit,
    onCrear: (AsignacionRequest) -> Unit,
    onEliminar: (Int) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalAppColors.current
    val scrollState = rememberScrollState()
    var mostrandoFormulario by remember { mutableStateOf(false) }
    var asignacionAEliminar by remember { mutableStateOf<AprendizInstructor?>(null) }

    if (mostrandoFormulario) {
        FormularioAsignacion(
            instructores = instructores,
            aprendices = aprendices,
            onCrear = { onCrear(it); mostrandoFormulario = false },
            onCancelar = { mostrandoFormulario = false }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = colors.textPrimary) }
        }

        IosCollapsibleHeader(
            title = "Asignaciones",
            subtitle = "Asignar aprendices a instructores",
            scrollOffset = scrollState.value.toFloat()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { mostrandoFormulario = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .pressScale(pressedScale = 0.97f),
            colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("NUEVA ASIGNACIÓN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TableContainer(title = "Asignaciones Actuales", subtitle = "Aprendices asignados a instructores") {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Text("APRENDIZ", modifier = Modifier.width(150.dp), color = colors.textSecondary, fontSize = 12.sp)
                Text("INSTRUCTOR", modifier = Modifier.width(150.dp), color = colors.textSecondary, fontSize = 12.sp)
                Text("JORNADA", modifier = Modifier.width(80.dp), color = colors.textSecondary, fontSize = 12.sp)
            }
            HorizontalDivider(color = colors.border)

            EstadoContenido(estado = estado, onReintentar = onReintentar) { items ->
                if (items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("No hay asignaciones registradas.", color = colors.textSecondary, fontSize = 14.sp)
                    }
                } else {
                    items.forEach { asig ->
                        HorizontalDivider(color = colors.border)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                asig.aprendiz?.nombreCompleto ?: "—",
                                modifier = Modifier.width(150.dp),
                                color = colors.textPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                asig.instructor?.nombreCompleto ?: "—",
                                modifier = Modifier.width(150.dp),
                                color = colors.textPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                asig.jornada ?: "—",
                                modifier = Modifier.width(80.dp),
                                color = colors.textPrimary,
                                fontSize = 13.sp
                            )
                            IconButton(
                                onClick = { asignacionAEliminar = asig },
                                modifier = Modifier.pressScale(pressedScale = 0.9f)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = ErrorRed, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }

    asignacionAEliminar?.let { asig ->
        AlertDialog(
            onDismissRequest = { asignacionAEliminar = null },
            containerColor = colors.cardBackground.copy(alpha = 0.98f),
            shape = RoundedCornerShape(24.dp),
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(36.dp)) },
            title = { Text("Eliminar asignación", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "¿Eliminar la asignación de ${asig.aprendiz?.nombreCompleto ?: "—"} a ${asig.instructor?.nombreCompleto ?: "—"}?",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { asig.id_asignacion?.let(onEliminar); asignacionAEliminar = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Eliminar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { asignacionAEliminar = null }) { Text("Cancelar", color = colors.textSecondary) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormularioAsignacion(
    instructores: List<UsuarioApi>,
    aprendices: List<UsuarioApi>,
    onCrear: (AsignacionRequest) -> Unit,
    onCancelar: () -> Unit
) {
    val colors = LocalAppColors.current
    val scrollState = rememberScrollState()
    var aprendizSeleccionado by remember { mutableStateOf<UsuarioApi?>(null) }
    var instructorSeleccionado by remember { mutableStateOf<UsuarioApi?>(null) }
    var jornada by remember { mutableStateOf("") }
    var expandirAprendiz by remember { mutableStateOf(false) }
    var expandirInstructor by remember { mutableStateOf(false) }
    var expandirJornada by remember { mutableStateOf(false) }
    val jornadas = listOf("Mañana", "Tarde", "Noche")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text("Nueva Asignación", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(20.dp))

        Text("APRENDIZ", color = colors.textSecondary, fontSize = 11.sp, letterSpacing = 1.5.sp)
        Spacer(modifier = Modifier.height(6.dp))
        ExposedDropdownMenuBox(expanded = expandirAprendiz, onExpandedChange = { expandirAprendiz = it }) {
            OutlinedTextField(
                value = aprendizSeleccionado?.nombreCompleto ?: "",
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Seleccionar aprendiz", color = colors.textSecondary) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedBorderColor = SenaGreen,
                    unfocusedBorderColor = colors.border
                )
            )
            ExposedDropdownMenu(expanded = expandirAprendiz, onDismissRequest = { expandirAprendiz = false }) {
                aprendices.forEach { ap ->
                    DropdownMenuItem(
                        text = { Text(ap.nombreCompleto ?: "", color = colors.textPrimary) },
                        onClick = { aprendizSeleccionado = ap; expandirAprendiz = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("INSTRUCTOR", color = colors.textSecondary, fontSize = 11.sp, letterSpacing = 1.5.sp)
        Spacer(modifier = Modifier.height(6.dp))
        ExposedDropdownMenuBox(expanded = expandirInstructor, onExpandedChange = { expandirInstructor = it }) {
            OutlinedTextField(
                value = instructorSeleccionado?.nombreCompleto ?: "",
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Seleccionar instructor", color = colors.textSecondary) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedBorderColor = SenaGreen,
                    unfocusedBorderColor = colors.border
                )
            )
            ExposedDropdownMenu(expanded = expandirInstructor, onDismissRequest = { expandirInstructor = false }) {
                instructores.forEach { inst ->
                    DropdownMenuItem(
                        text = { Text(inst.nombreCompleto ?: "", color = colors.textPrimary) },
                        onClick = { instructorSeleccionado = inst; expandirInstructor = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("JORNADA (opcional)", color = colors.textSecondary, fontSize = 11.sp, letterSpacing = 1.5.sp)
        Spacer(modifier = Modifier.height(6.dp))
        ExposedDropdownMenuBox(expanded = expandirJornada, onExpandedChange = { expandirJornada = it }) {
            OutlinedTextField(
                value = jornada,
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Seleccionar jornada", color = colors.textSecondary) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedBorderColor = SenaGreen,
                    unfocusedBorderColor = colors.border
                )
            )
            ExposedDropdownMenu(expanded = expandirJornada, onDismissRequest = { expandirJornada = false }) {
                jornadas.forEach { j ->
                    DropdownMenuItem(
                        text = { Text(j, color = colors.textPrimary) },
                        onClick = { jornada = j; expandirJornada = false }
                    )
                }
                if (jornada.isNotEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Sin jornada", color = colors.textSecondary) },
                        onClick = { jornada = ""; expandirJornada = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onCancelar,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text("CANCELAR", color = colors.textSecondary) }
            Button(
                onClick = {
                    val apId = aprendizSeleccionado?.id_usuario ?: return@Button
                    val instId = instructorSeleccionado?.id_usuario ?: return@Button
                    onCrear(AsignacionRequest(
                        fk_id_aprendiz = apId,
                        fk_id_instructor = instId,
                        jornada = jornada.ifEmpty { null }
                    ))
                },
                modifier = Modifier.weight(1f).height(48.dp).pressScale(pressedScale = 0.97f),
                colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("ASIGNAR", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}
