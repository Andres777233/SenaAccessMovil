package com.example.sennaccess.ui

// Vista de novedades compartida por Instructor y Admin: lista las novedades
// (el instructor solo las suyas vía /my-novedades; el admin ve el listado
// completo), permite reportar una nueva (POST /api/novedades) solo al instructor
// y eliminar (DELETE /api/novedades/{id}) únicamente al admin.

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.data.Novedad
import com.example.sennaccess.data.NovedadRequest
import com.example.sennaccess.data.NovedadRepository
import com.example.sennaccess.data.SessionManager
import com.example.sennaccess.data.mock.MockData
import com.example.sennaccess.ui.ios.GlassCornerRadius
import com.example.sennaccess.ui.ios.IosCollapsibleHeader
import com.example.sennaccess.ui.ios.glassSurface
import com.example.sennaccess.ui.ios.pressScale
import com.example.sennaccess.ui.theme.ErrorRed
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.OrangeAmber
import com.example.sennaccess.ui.theme.SenaGreen
import kotlinx.coroutines.launch

/**
 * Vista de Novedades compartida para INSTRUCTOR y ADMIN.
 * Muestra el listado de novedades y permite reportar una nueva (solo instructor).
 *
 * - [estado] == null → solo datos de ejemplo (la API no expone novedades).
 * - [estado] != null → carga desde la API con respaldo a mocks.
 * Respeta colores SENA y estilo glassmorphism iOS.
 */
@Composable
fun NovedadesView(
    estado: CargaUiState<List<Novedad>>? = null,
    onReintentar: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Estado local del flujo de reporte: visibilidad del formulario, textos y envío.
    var mostrandoFormulario by remember { mutableStateOf(false) }
    var titulo by remember { mutableStateOf("") }
    var detalle by remember { mutableStateOf("") }
    var ambiente by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var errorEnvio by remember { mutableStateOf<String?>(null) }
    var enviada by remember { mutableStateOf(false) }

    // Novedad pendiente de eliminar (se confirma con un diálogo antes de borrar).
    var novedadAEliminar by remember { mutableStateOf<Novedad?>(null) }
    var eliminando by remember { mutableStateOf(false) }
    var errorEliminar by remember { mutableStateOf<String?>(null) }

    // El usuario solo puede borrar novedades si es admin (el backend responde 403
    // al instructor/aprendiz en DELETE /novedades/{id}).
    fun puedeEliminar(): Boolean =
        SessionManager.token != null && SessionManager.userRole?.equals("admin", ignoreCase = true) == true

    // Solo el instructor reporta novedades; el admin solo ve el historial.
    val puedeReportar = SessionManager.userRole?.equals("Instructor", ignoreCase = true) == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        IosCollapsibleHeader(
            title = "Novedades",
            subtitle = "Avisos y reportes del centro de formación",
            scrollOffset = scrollState.value.toFloat()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // El área superior alterna entre confirmación de envío, formulario y botón.
        if (enviada) {
            TarjetaNovedadEnviada(onAceptar = { enviada = false })
        } else if (mostrandoFormulario) {
            FormularioNovedad(
                titulo = titulo,
                detalle = detalle,
                ambiente = ambiente,
                onTituloChange = { titulo = it },
                onDetalleChange = { detalle = it },
                onAmbienteChange = { ambiente = it },
                enviando = enviando,
                errorMensaje = errorEnvio,
                onEnviar = {
                    if (!enviando && titulo.isNotBlank() && detalle.isNotBlank() && ambiente.isNotBlank()) {
                        errorEnvio = null
                        val token = SessionManager.token
                        if (token == null) {
                            // Sin sesión no hay API: se muestra la confirmación local.
                            enviada = true
                            mostrandoFormulario = false
                        } else {
                            enviando = true
                            scope.launch {
                                try {
                                    NovedadRepository().crear(
                                        token,
                                        NovedadRequest(
                                            novedad_ambiente = ambiente.trim(),
                                            novedad_title = titulo.trim(),
                                            novedad_body = detalle.trim()
                                        )
                                    )
                                    enviando = false
                                    titulo = ""
                                    detalle = ""
                                    ambiente = ""
                                    enviada = true
                                    mostrandoFormulario = false
                                    onReintentar()
                                } catch (e: retrofit2.HttpException) {
                                    enviando = false
                                    errorEnvio = "Error ${e.code()}. Verifica los datos."
                                } catch (e: Exception) {
                                    enviando = false
                                    errorEnvio = "No se pudo conectar al servidor."
                                }
                            }
                        }
                    } else if (titulo.isBlank() || detalle.isBlank() || ambiente.isBlank()) {
                        errorEnvio = "Completa todos los campos."
                    }
                },
                onCancelar = { mostrandoFormulario = false }
            )
        } else if (puedeReportar) {
            Button(
                onClick = { mostrandoFormulario = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .pressScale(pressedScale = 0.97f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("REPORTAR NOVEDAD", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "NOVEDADES RECIENTES",
            color = colors.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Sin estado se pintan novedades de ejemplo; con estado se consume la API
        // a través de EstadoContenido (Loading/Error/Success con reintento).
        if (estado == null) {
            MockData.novedades.forEach { n ->
                TarjetaNovedad(n, onEliminar = null)
                Spacer(modifier = Modifier.height(12.dp))
            }
        } else {
            EstadoContenido(estado = estado, onReintentar = onReintentar) { items ->
                if (items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        Text("No hay novedades registradas.", color = colors.textSecondary, fontSize = 14.sp)
                    }
                } else {
                    items.forEach { n ->
                        TarjetaNovedad(
                            n,
                            onEliminar = if (puedeEliminar()) {
                                { novedadAEliminar = n }
                            } else null
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    // Confirmación antes de eliminar una novedad (solo admin).
    novedadAEliminar?.let { n ->
        AlertDialog(
            onDismissRequest = { novedadAEliminar = null },
            containerColor = colors.cardBackground.copy(alpha = 0.98f),
            shape = RoundedCornerShape(24.dp),
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(36.dp)) },
            title = { Text("Eliminar novedad", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("¿Seguro que deseas eliminar esta novedad?", color = colors.textSecondary)
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
                        val id = n.id_novedad ?: return@Button
                        eliminando = true
                        scope.launch {
                            try {
                                NovedadRepository().eliminar(token, id)
                                eliminando = false
                                novedadAEliminar = null
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
                TextButton(onClick = { novedadAEliminar = null }) { Text("Cancelar", color = colors.textSecondary) }
            }
        )
    }
}

// Tarjeta glassmorphism que muestra una novedad con su detalle y estado pendiente;
// si onEliminar no es null, añade un botón para borrarla (autor o admin).
@Composable
private fun TarjetaNovedad(n: Novedad, onEliminar: (() -> Unit)?) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(pressedScale = 0.98f)
            .glassSurface(cornerRadius = GlassCornerRadius)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(OrangeAmber.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ReportProblem, contentDescription = null, tint = OrangeAmber, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(n.novedad_title ?: "Novedad", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(n.novedad_datetime ?: "—", color = colors.textSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(n.novedad_body ?: "—", color = colors.textSecondary, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        if (onEliminar != null) {
            IconButton(onClick = onEliminar) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar novedad", tint = ErrorRed, modifier = Modifier.size(20.dp))
            }
        }
        Box(
            modifier = Modifier
                .border(1.dp, OrangeAmber.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text("PENDIENTE", color = OrangeAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// Formulario de reporte con campos de ambiente, título, descripción y acciones.
@Composable
private fun FormularioNovedad(
    titulo: String,
    detalle: String,
    ambiente: String,
    onTituloChange: (String) -> Unit,
    onDetalleChange: (String) -> Unit,
    onAmbienteChange: (String) -> Unit,
    enviando: Boolean,
    errorMensaje: String?,
    onEnviar: () -> Unit,
    onCancelar: () -> Unit
) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = GlassCornerRadius)
            .padding(18.dp)
    ) {
        Text("Reportar Novedad", color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
            value = ambiente,
            onValueChange = onAmbienteChange,
            label = { Text("Ambiente") },
            modifier = Modifier.fillMaxWidth(),
            colors = novedadCamposColors()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = titulo,
            onValueChange = onTituloChange,
            label = { Text("Titulo de la novedad") },
            modifier = Modifier.fillMaxWidth(),
            colors = novedadCamposColors()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = detalle,
            onValueChange = onDetalleChange,
            label = { Text("Descripcion") },
            modifier = Modifier.fillMaxWidth().height(110.dp),
            colors = novedadCamposColors()
        )
        if (errorMensaje != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(errorMensaje, color = ErrorRed, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onEnviar,
                modifier = Modifier.weight(1f).height(48.dp).pressScale(pressedScale = 0.97f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black)
            ) { Text(if (enviando) "ENVIANDO..." else "ENVIAR", fontWeight = FontWeight.Bold) }
            OutlinedButton(
                onClick = onCancelar,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, colors.textSecondary)
            ) { Text("CANCELAR", color = colors.textPrimary) }
        }
    }
}

// Confirmación visual que se muestra tras registrar una novedad.
@Composable
private fun TarjetaNovedadEnviada(onAceptar: () -> Unit) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = GlassCornerRadius)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = SenaGreen, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Novedad Reportada", color = SenaGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Tu reporte fue registrado. Solo tu y el administrador podran verlo.",
            color = colors.textSecondary,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onAceptar,
            modifier = Modifier.fillMaxWidth().height(48.dp).pressScale(pressedScale = 0.97f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black)
        ) { Text("ACEPTAR", fontWeight = FontWeight.Bold) }
    }
}

// Esquema de colores SENA para los campos del formulario.
@Composable
private fun novedadCamposColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SenaGreen,
    unfocusedBorderColor = LocalAppColors.current.textSecondary.copy(alpha = 0.5f),
    focusedLabelColor = SenaGreen,
    unfocusedLabelColor = LocalAppColors.current.textSecondary,
    cursorColor = SenaGreen,
    focusedTextColor = LocalAppColors.current.textPrimary,
    unfocusedTextColor = LocalAppColors.current.textPrimary
)
