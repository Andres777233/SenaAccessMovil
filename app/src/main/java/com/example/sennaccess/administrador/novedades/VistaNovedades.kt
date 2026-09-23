package com.example.sennaccess.administrador.novedades

// Vista de novedades compartida por Instructor y Admin: lista las novedades
// (el instructor solo las suyas vía /my-novedades; el admin ve el listado
// completo), permite reportar una nueva (POST /api/novedades) solo al instructor
// y eliminar (DELETE /api/novedades/{id}) únicamente al admin.

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.datos.modelos.Novedad
import com.example.sennaccess.administrador.novedades.AlmacenNovedadesLeidas
import com.example.sennaccess.datos.modelos.PeticionNovedad
import com.example.sennaccess.datos.repositorios.RepositorioNovedades
import com.example.sennaccess.datos.sesion.GestorSesion
import com.example.sennaccess.datos.simulados.DatosSimulados
import com.example.sennaccess.comun.diseno.FiltroSena
import com.example.sennaccess.comun.fechaRelativa
import com.example.sennaccess.comun.diseno.RadioVidrio
import com.example.sennaccess.comun.diseno.CabeceraPlegable
import com.example.sennaccess.comun.diseno.superficieVidrio
import com.example.sennaccess.comun.diseno.escalaPresion
import com.example.sennaccess.comun.tema.RojoError
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.NaranjaAmbar
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import kotlinx.coroutines.launch
import com.example.sennaccess.comun.EstadoCarga
import com.example.sennaccess.comun.EstadoContenido
import com.example.sennaccess.comun.VistaVacia

@Composable
fun VistaNovedades(
    estado: EstadoCarga<List<Novedad>>? = null,
    onReintentar: () -> Unit = {}
) {
    val colors = ColoresAppLocal.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var mostrandoFormulario by remember { mutableStateOf(false) }
    var titulo by remember { mutableStateOf("") }
    var detalle by remember { mutableStateOf("") }
    var ambiente by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var errorEnvio by remember { mutableStateOf<String?>(null) }
    var enviada by remember { mutableStateOf(false) }

    var novedadAEliminar by remember { mutableStateOf<Novedad?>(null) }
    var eliminando by remember { mutableStateOf(false) }
    var errorEliminar by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    var leidas by remember { mutableStateOf(AlmacenNovedadesLeidas.obtenerLeidas(context)) }
    var filtroLectura by remember { mutableStateOf(0) }

    var seleccionadas by remember { mutableStateOf<Set<Int>>(emptySet()) }
    val modoSeleccion = seleccionadas.isNotEmpty()
    var pideBorrarLote by remember { mutableStateOf(false) }
    var borrandoLote by remember { mutableStateOf(false) }
    var idsVisibles by remember { mutableStateOf<Set<Int>>(emptySet()) }

    // al instructor/aprendiz en DELETE /novedades/{id}).
    fun puedeEliminar(): Boolean =
        GestorSesion.token != null && GestorSesion.userRole?.equals("admin", ignoreCase = true) == true

    val puedeReportar = GestorSesion.userRole?.equals("Instructor", ignoreCase = true) == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        CabeceraPlegable(
            title = "Novedades",
            subtitle = "Avisos y reportes del centro de formación",
            scrollOffset = scrollState.value.toFloat()
        )

        Spacer(modifier = Modifier.height(16.dp))

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
                        val token = GestorSesion.token
                        if (token == null) {
                            errorEnvio = "Sesión expirada. Inicia sesión de nuevo."
                        } else {
                            enviando = true
                            scope.launch {
                                try {
                                    RepositorioNovedades().crear(
                                        token,
                                        PeticionNovedad(
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
                    .escalaPresion(pressedScale = 0.97f),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("REPORTAR NOVEDAD", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FiltroSena(texto = "Todas", seleccionado = filtroLectura == 0, onClick = { filtroLectura = 0 })
            FiltroSena(texto = "Nuevas", seleccionado = filtroLectura == 1, onClick = { filtroLectura = 1 })
            FiltroSena(texto = "Leídas", seleccionado = filtroLectura == 2, onClick = { filtroLectura = 2 })
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (modoSeleccion) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("${seleccionadas.size} elegidas", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = { seleccionadas = idsVisibles }) { Text("Todas", color = verdeMarca(), fontSize = 12.sp) }
                IconButton(onClick = { pideBorrarLote = true }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Borrar elegidas", tint = RojoError, modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = { seleccionadas = emptySet() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Salir de selección", tint = colors.textSecondary, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            "NOVEDADES RECIENTES",
            color = colors.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (estado == null) {
            DatosSimulados.novedades.forEach { n ->
                TarjetaNovedad(n, onEliminar = null)
                Spacer(modifier = Modifier.height(12.dp))
            }
        } else {
            EstadoContenido(estado = estado, onReintentar = onReintentar) { items ->
                if (items.isEmpty()) {
                    VistaVacia(
                        icono = Icons.Default.ReportProblem,
                        titulo = "No hay novedades registradas",
                        mensaje = "Los avisos y reportes del centro aparecerán aquí."
                    )
                } else {
                    val visibles = items.filter { n ->
                        val id = n.id_novedad
                        val leida = id != null && leidas.contains(id.toString())
                        filtroLectura == 0 || (filtroLectura == 1 && !leida) || (filtroLectura == 2 && leida)
                    }
                    if (visibles.isEmpty()) {
                        VistaVacia(
                            icono = Icons.Default.ReportProblem,
                            titulo = if (filtroLectura == 2) "Nada por aquí" else "Todo al día",
                            mensaje = if (filtroLectura == 2) "Aún no marcas ninguna como leída." else "No tienes novedades nuevas."
                        )
                    } else {
                        LaunchedEffect(visibles) { idsVisibles = visibles.mapNotNull { it.id_novedad }.toSet() }
                        visibles.forEach { n ->
                            val id = n.id_novedad
                            val leida = id != null && leidas.contains(id.toString())
                            TarjetaNovedad(
                                n = n,
                                onEliminar = if (puedeEliminar()) {
                                    { novedadAEliminar = n }
                                } else null,
                                leida = leida,
                                seleccionada = id != null && seleccionadas.contains(id),
                                modoSeleccion = modoSeleccion,
                                onMarcarLeida = id?.let { { AlmacenNovedadesLeidas.marcarLeida(context, id); leidas = AlmacenNovedadesLeidas.obtenerLeidas(context) } },
                                onToggleSeleccion = if (puedeEliminar() && id != null) {
                                    {
                                        seleccionadas = if (seleccionadas.contains(id)) seleccionadas - id else seleccionadas + id
                                    }
                                } else null
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    if (pideBorrarLote) {
        AlertDialog(
            onDismissRequest = { if (!borrandoLote) pideBorrarLote = false },
            containerColor = colors.cardBackground.copy(alpha = 0.98f),
            shape = RoundedCornerShape(28.dp),
            title = { Text("Borrar ${seleccionadas.size} novedades", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Esta acción no se puede deshacer.", color = colors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        val t = GestorSesion.token ?: run { pideBorrarLote = false; return@Button }
                        borrandoLote = true
                        scope.launch {
                            var fallos = 0
                            for (id in seleccionadas) {
                                try { RepositorioNovedades().eliminar(t, id) } catch (_: Exception) { fallos++ }
                            }
                            borrandoLote = false
                            pideBorrarLote = false
                            seleccionadas = emptySet()
                            if (fallos > 0) errorEliminar = "No se pudieron borrar $fallos."
                            onReintentar()
                        }
                    },
                    enabled = !borrandoLote,
                    colors = ButtonDefaults.buttonColors(containerColor = RojoError, contentColor = Color.Black),
                    shape = RoundedCornerShape(28.dp)
                ) { Text(if (borrandoLote) "Borrando..." else "Borrar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { pideBorrarLote = false }, enabled = !borrandoLote) { Text("Cancelar", color = colors.textSecondary) } }
        )
    }

    novedadAEliminar?.let { n ->
        AlertDialog(
            onDismissRequest = { novedadAEliminar = null },
            containerColor = colors.cardBackground.copy(alpha = 0.98f),
            shape = RoundedCornerShape(24.dp),
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = RojoError, modifier = Modifier.size(36.dp)) },
            title = { Text("Eliminar novedad", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("¿Seguro que deseas eliminar esta novedad?", color = colors.textSecondary)
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
                        val id = n.id_novedad ?: return@Button
                        eliminando = true
                        scope.launch {
                            try {
                                RepositorioNovedades().eliminar(token, id)
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
                    colors = ButtonDefaults.buttonColors(containerColor = RojoError, contentColor = Color.Black),
                    shape = RoundedCornerShape(28.dp)
                ) { Text(if (eliminando) "Eliminando..." else "Eliminar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { novedadAEliminar = null }) { Text("Cancelar", color = colors.textSecondary) }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TarjetaNovedad(
    n: Novedad,
    onEliminar: (() -> Unit)?,
    leida: Boolean = false,
    seleccionada: Boolean = false,
    modoSeleccion: Boolean = false,
    onMarcarLeida: (() -> Unit)? = null,
    onToggleSeleccion: (() -> Unit)? = null
) {
    val colors = ColoresAppLocal.current
    val colorAcento = if (leida) verdeMarca() else NaranjaAmbar
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .superficieVidrio(cornerRadius = RadioVidrio)
            .combinedClickable(
                onClick = { if (modoSeleccion) onToggleSeleccion?.invoke() },
                onLongClick = { if (!modoSeleccion) onToggleSeleccion?.invoke() }
            )
            .background(
                if (seleccionada) verdeMarca().copy(alpha = 0.12f) else Color.Transparent,
                RoundedCornerShape(RadioVidrio)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (modoSeleccion) {
            Checkbox(
                checked = seleccionada,
                onCheckedChange = { onToggleSeleccion?.invoke() },
                colors = CheckboxDefaults.colors(checkedColor = VerdeSena, checkmarkColor = Color.Black)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(colorAcento.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ReportProblem, contentDescription = null, tint = colorAcento, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(n.novedad_title ?: "Novedad", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(fechaRelativa(n.novedad_datetime), color = colors.textSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(n.novedad_body ?: "—", color = colors.textSecondary, fontSize = 12.sp, lineHeight = 16.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .border(1.dp, colorAcento.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(if (leida) "LEÍDA" else "NUEVA", color = colorAcento, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (!leida && onMarcarLeida != null) {
                IconButton(onClick = onMarcarLeida, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Marcar como leída", tint = verdeMarca(), modifier = Modifier.size(24.dp))
                }
            }
            if (onEliminar != null) {
                IconButton(onClick = onEliminar, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar novedad", tint = RojoError, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

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
    val colors = ColoresAppLocal.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .superficieVidrio(cornerRadius = RadioVidrio)
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
            Text(errorMensaje, color = RojoError, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onEnviar,
                modifier = Modifier.weight(1f).height(48.dp).escalaPresion(pressedScale = 0.97f),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = Color.Black)
            ) { Text(if (enviando) "ENVIANDO..." else "ENVIAR", fontWeight = FontWeight.Bold) }
            OutlinedButton(
                onClick = onCancelar,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, colors.textSecondary)
            ) { Text("CANCELAR", color = colors.textPrimary) }
        }
    }
}

@Composable
private fun TarjetaNovedadEnviada(onAceptar: () -> Unit) {
    val colors = ColoresAppLocal.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .superficieVidrio(cornerRadius = RadioVidrio)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = verdeMarca(), modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Novedad Reportada", color = verdeMarca(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
            modifier = Modifier.fillMaxWidth().height(48.dp).escalaPresion(pressedScale = 0.97f),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = Color.Black)
        ) { Text("ACEPTAR", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun novedadCamposColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = verdeMarca(),
    unfocusedBorderColor = ColoresAppLocal.current.textSecondary.copy(alpha = 0.5f),
    focusedLabelColor = verdeMarca(),
    unfocusedLabelColor = ColoresAppLocal.current.textSecondary,
    cursorColor = verdeMarca(),
    focusedTextColor = ColoresAppLocal.current.textPrimary,
    unfocusedTextColor = ColoresAppLocal.current.textPrimary
)
