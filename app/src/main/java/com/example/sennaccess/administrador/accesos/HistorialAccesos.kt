package com.example.sennaccess.administrador.accesos

// Contenido de la pestaña HISTORIAL del ADMINISTRADOR.
// Muestra el registro de ingresos con buscador por nombre, filtro Entrada/Salida
// y selector de fecha (Hoy/Ayer/Calendario). El rango viaja al ViewModel
// (GET /admin/ingresos?desde=&hasta=) y al export para que el archivo
// corresponda a lo filtrado en pantalla.

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.core.content.FileProvider
import com.example.sennaccess.datos.repositorios.RepositorioIngresos
import com.example.sennaccess.datos.sesion.GestorSesion
import com.example.sennaccess.comun.EstadoCarga
import com.example.sennaccess.comun.EstadoContenido
import com.example.sennaccess.comun.VistaVacia
import com.example.sennaccess.comun.horaCorta
import com.example.sennaccess.comun.detalleHttp
import com.example.sennaccess.comun.tema.RojoError
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import com.example.sennaccess.comun.diseno.FiltroSena
import com.example.sennaccess.comun.diseno.BuscadorSena
import com.example.sennaccess.comun.diseno.RadioVidrio
import com.example.sennaccess.comun.diseno.CabeceraPlegable
import com.example.sennaccess.comun.diseno.superficieVidrio
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import com.example.sennaccess.administrador.panel.ContenedorVidrioAdmin
import com.example.sennaccess.administrador.panel.DatosHistorial
import com.example.sennaccess.administrador.panel.RegistroAcceso

private fun fechaBogota(offsetDias: Int = 0): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("America/Bogota"))
    cal.add(Calendar.DAY_OF_YEAR, offsetDias)
    return String.format(
        Locale.US, "%04d-%02d-%02d",
        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContenidoHistorial(
    historial: EstadoCarga<DatosHistorial>,
    onReintentar: () -> Unit,
    onVerAprendices: () -> Unit = {},
    onVerInstructores: () -> Unit = {},
    onRango: (desde: String, hasta: String) -> Unit = { _, _ -> }
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var busqueda by remember { mutableStateOf("") }
    var filtroTipo by remember { mutableStateOf(0) }
    var presetFecha by remember { mutableStateOf(0) }
    var fechaElegida by remember { mutableStateOf(fechaBogota(0)) }
    var mostrarCalendario by remember { mutableStateOf(false) }

    var exportando by remember { mutableStateOf(false) }
    var errorExport by remember { mutableStateOf<String?>(null) }

    var mostrarHojaExport by remember { mutableStateOf(false) }
    var formatoSel by remember { mutableStateOf("csv") }
    var colsSel by remember { mutableStateOf(listOf("usuario", "email", "identificacion", "tipo", "lugar", "fecha")) }

    val opcionesCols = listOf(
        "usuario" to "Usuario",
        "email" to "Correo",
        "identificacion" to "Identificación",
        "tipo" to "Tipo",
        "lugar" to "Lugar",
        "fecha" to "Fecha y Hora"
    )

    fun rangoActual(): Pair<String, String> = when (presetFecha) {
        1 -> fechaBogota(-1) to fechaBogota(-1)
        2 -> fechaElegida to fechaElegida
        else -> fechaBogota(0) to fechaBogota(0)
    }

    fun etiquetaFecha(): String = when (presetFecha) {
        1 -> "Ayer"
        2 -> fechaElegida
        else -> "Hoy"
    }

    fun etiquetaTipo(): String = when (filtroTipo) {
        1 -> "entradas"
        2 -> "salidas"
        else -> "todo"
    }

    fun exportarHistorial() {
        val token = GestorSesion.token ?: return
        if (exportando || colsSel.isEmpty()) return
        exportando = true
        errorExport = null
        mostrarHojaExport = false
        scope.launch {
            try {
                val (desde, hasta) = rangoActual()
                val body = RepositorioIngresos().exportarHistorial(
                    token,
                    formato = formatoSel,
                    cols = colsSel.joinToString(","),
                    desde = desde,
                    hasta = hasta
                )
                val ext = when (formatoSel) {
                    "xlsx" -> "xlsx"
                    "pdf" -> "pdf"
                    else -> "csv"
                }
                val mime = when (formatoSel) {
                    "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    "pdf" -> "application/pdf"
                    else -> "text/csv"
                }
                val archivo = java.io.File(context.cacheDir, "historial_${desde}_${etiquetaTipo()}.$ext")
                archivo.writeBytes(body.bytes())
                exportando = false
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = mime
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Compartir historial"))
            } catch (e: Exception) {
                exportando = false
                errorExport = if (e is retrofit2.HttpException) {
                    detalleHttp(e)
                } else {
                    "No se pudo exportar: ${e.message ?: e.javaClass.simpleName}"
                }
            }
        }
    }

    var tabRol by remember { mutableStateOf(0) }

    fun coincideFiltro(r: RegistroAcceso): Boolean {
        if (filtroTipo == 1 && !r.tipo.equals("Entrada", ignoreCase = true)) return false
        if (filtroTipo == 2 && !r.tipo.equals("Salida", ignoreCase = true)) return false
        val q = busqueda.trim()
        if (q.isBlank()) return true
        return r.nombre.contains(q, ignoreCase = true) || r.rol.contains(q, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        CabeceraPlegable(
            title = "Historial de Acceso",
            subtitle = "Registro de ingresos al centro • ${etiquetaFecha()}",
            scrollOffset = scrollState.value.toFloat()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) {
                BuscadorSena(
                    valor = busqueda,
                    onValor = { busqueda = it },
                    placeholder = "Buscar por nombre..."
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            IconButton(
                onClick = { mostrarHojaExport = true },
                enabled = !exportando,
                modifier = Modifier.size(48.dp).clip(CircleShape).background(VerdeSena)
            ) {
                if (exportando) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.FileDownload, contentDescription = "Exportar historial", tint = Color.Black, modifier = Modifier.size(22.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(end = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item(key = "t-todos") { FiltroSena(texto = "Todos", seleccionado = filtroTipo == 0, onClick = { filtroTipo = 0 }) }
            item(key = "t-ent") { FiltroSena(texto = "Entradas", seleccionado = filtroTipo == 1, onClick = { filtroTipo = 1 }) }
            item(key = "t-sal") { FiltroSena(texto = "Salidas", seleccionado = filtroTipo == 2, onClick = { filtroTipo = 2 }) }
            item(key = "sep") { Box(modifier = Modifier.width(1.dp).height(24.dp).background(ColoresAppLocal.current.divider)) }
            item(key = "f-hoy") {
                FiltroSena(texto = "Hoy", seleccionado = presetFecha == 0, onClick = {
                    presetFecha = 0
                    val (d, h) = fechaBogota(0) to fechaBogota(0)
                    onRango(d, h)
                })
            }
            item(key = "f-ayer") {
                FiltroSena(texto = "Ayer", seleccionado = presetFecha == 1, onClick = {
                    presetFecha = 1
                    val f = fechaBogota(-1)
                    onRango(f, f)
                })
            }
            item(key = "f-fecha") {
                FiltroSena(
                    texto = if (presetFecha == 2) fechaElegida else "Fecha...",
                    seleccionado = presetFecha == 2,
                    onClick = { mostrarCalendario = true }
                )
            }
        }

        if (mostrarCalendario) {
            val pickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { mostrarCalendario = false },
                confirmButton = {
                    TextButton(onClick = {
                        pickerState.selectedDateMillis?.let { ms ->
                            val cal = Calendar.getInstance(TimeZone.getTimeZone("America/Bogota")).apply { timeInMillis = ms }
                            fechaElegida = String.format(
                                Locale.US, "%04d-%02d-%02d",
                                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)
                            )
                            presetFecha = 2
                            onRango(fechaElegida, fechaElegida)
                        }
                        mostrarCalendario = false
                    }) { Text("Ver día") }
                },
                dismissButton = { TextButton(onClick = { mostrarCalendario = false }) { Text("Cancelar") } }
            ) { DatePicker(state = pickerState) }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (mostrarHojaExport) {
            val sinColumnas = colsSel.isEmpty()
            val formatos = listOf(
                Triple("csv", "CSV", "Hoja de cálculo universal"),
                Triple("xlsx", "Excel", "Para tablas y filtros"),
                Triple("pdf", "PDF", "Para imprimir o archivar")
            )
            ModalBottomSheet(
                onDismissRequest = { if (!exportando) mostrarHojaExport = false },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Exportar historial", color = ColoresAppLocal.current.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        "Se descarga ${etiquetaFecha()} con las columnas elegidas. El archivo solo filtra por día: ignora la búsqueda y el tipo (entradas/salidas) que ves en pantalla.",
                        color = ColoresAppLocal.current.textSecondary, fontSize = 12.sp
                    )
                    Text("Formato", color = ColoresAppLocal.current.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    formatos.forEach { (valor, etiqueta, desc) ->
                        val sel = formatoSel == valor
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().superficieVidrio(cornerRadius = RadioVidrio).padding(12.dp)
                        ) {
                            RadioButton(selected = sel, onClick = { formatoSel = valor })
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(etiqueta, color = ColoresAppLocal.current.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(desc, color = ColoresAppLocal.current.textSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                    Text("Columnas (${colsSel.size}/${opcionesCols.size})", color = ColoresAppLocal.current.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    opcionesCols.forEach { (clave, etiqueta) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = colsSel.contains(clave),
                                onCheckedChange = { marcado ->
                                    colsSel = if (marcado) colsSel + clave else colsSel - clave
                                },
                                colors = CheckboxDefaults.colors(checkedColor = VerdeSena, checkmarkColor = Color.Black)
                            )
                            Text(etiqueta, color = ColoresAppLocal.current.textPrimary, fontSize = 14.sp)
                        }
                    }
                    if (sinColumnas) Text("Selecciona al menos una columna.", color = RojoError, fontSize = 12.sp)
                    Button(
                        onClick = { exportarHistorial() },
                        enabled = !sinColumnas && !exportando,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = Color.Black)
                    ) { Text(if (exportando) "EXPORTANDO..." else "EXPORTAR ${formatoSel.uppercase()}", fontWeight = FontWeight.Bold) }
                    TextButton(onClick = { mostrarHojaExport = false }, enabled = !exportando, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancelar", color = ColoresAppLocal.current.textSecondary)
                    }
                }
            }
        }

        if (errorExport != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(errorExport!!, color = RojoError, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(16.dp))

        EstadoContenido(estado = historial, onReintentar = onReintentar) { data ->
            val ins = data.instructores.filter(::coincideFiltro)
            val apr = data.aprendices.filter(::coincideFiltro)
            val lista: List<RegistroAcceso> = when (tabRol) {
                1 -> ins
                2 -> apr
                else -> (ins + apr).sortedByDescending { it.hora }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FiltroSena(texto = "Todos • ${ins.size + apr.size}", seleccionado = tabRol == 0, onClick = { tabRol = 0 })
                FiltroSena(texto = "Instr. • ${ins.size}", seleccionado = tabRol == 1, onClick = { tabRol = 1 })
                FiltroSena(texto = "Aprend. • ${apr.size}", seleccionado = tabRol == 2, onClick = { tabRol = 2 })
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (lista.isEmpty()) {
                VistaVacia(
                    icono = Icons.Default.History,
                    titulo = "Sin movimientos",
                    mensaje = "Prueba con otro nombre, tipo, fecha o pestaña."
                )
            } else {
                ContenedorVidrioAdmin {
                    Text(
                        "${etiquetaFecha()} • ${lista.size} movimientos${if (busqueda.isBlank()) "" else " • “$busqueda”"}",
                        color = ColoresAppLocal.current.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    lista.take(8).forEachIndexed { i, r ->
                        if (i > 0) HorizontalDivider(color = ColoresAppLocal.current.divider)
                        FilaAccesoCompacta(nombre = r.nombre, detalle = "${r.rol} • ${horaCorta(r.hora)}", tipo = r.tipo)
                    }
                    if (lista.size > 8 || tabRol != 0) {
                        HorizontalDivider(color = ColoresAppLocal.current.divider)
                        when (tabRol) {
                            1 -> PieVerTodos(texto = "Ver todos los instructores (${ins.size})", onClick = onVerInstructores)
                            2 -> PieVerTodos(texto = "Ver todos los aprendices (${apr.size})", onClick = onVerAprendices)
                            else -> Row(modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = onVerInstructores, modifier = Modifier.weight(1f).height(40.dp)) {
                                    Text("Instructores", color = verdeMarca(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                TextButton(onClick = onVerAprendices, modifier = Modifier.weight(1f).height(40.dp)) {
                                    Text("Aprendices", color = verdeMarca(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PieVerTodos(texto: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(40.dp)) {
        Text(texto, color = verdeMarca(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FilaAccesoCompacta(nombre: String, detalle: String, tipo: String) {
    val colors = ColoresAppLocal.current
    val esSalida = tipo.equals("Salida", ignoreCase = true)
    val colorTipo = if (esSalida) Color(0xFFE67E22) else verdeMarca()
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(colorTipo.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(nombre.take(1).uppercase(), color = colorTipo, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(nombre, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(detalle, color = colors.textSecondary, fontSize = 11.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        Box(
            modifier = Modifier.clip(RoundedCornerShape(50)).background(colorTipo.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(if (esSalida) "SALIDA" else "ENTRADA", color = colorTipo, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun TarjetaAccesoAdmin(nombre: String, rol: String, hora: String, tipo: String) {
    val colors = ColoresAppLocal.current
    val esSalida = tipo.equals("Salida", ignoreCase = true)
    val colorTipo = if (esSalida) Color(0xFFE67E22) else verdeMarca()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .superficieVidrio(cornerRadius = 12.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(colorTipo.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(nombre.take(1).uppercase(), color = colorTipo, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(nombre, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text("$rol • ${horaCorta(hora)}", color = colors.textSecondary, fontSize = 11.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        Box(
            modifier = Modifier.clip(RoundedCornerShape(50)).background(colorTipo.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(if (esSalida) "SALIDA" else "ENTRADA", color = colorTipo, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }
    }
}
