package com.example.sennaccess.comun

// Historial por días compartido por aprendiz e instructor.
// En vez de mostrar TODAS las entradas/salidas en una lista infinita:
// vista inicial con los últimos 3 días agrupados + calendario para buscar otro día,
// más buscador por lugar y chips Entradas/Salidas.

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.datos.modelos.Ingreso
import com.example.sennaccess.comun.EstadoCarga
import com.example.sennaccess.comun.EstadoContenido
import com.example.sennaccess.comun.VistaVacia
import com.example.sennaccess.comun.diseno.TipoInsignia
import com.example.sennaccess.comun.diseno.InsigniaSena
import com.example.sennaccess.comun.diseno.CeldaSena
import com.example.sennaccess.comun.diseno.FiltroSena
import com.example.sennaccess.comun.diseno.BuscadorSena
import com.example.sennaccess.comun.diseno.EspaciadoSena
import com.example.sennaccess.comun.fechaLegible
import com.example.sennaccess.comun.tema.ColoresAppLocal
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private val patronFecha = Regex("""(\d{4})-(\d{2})-(\d{2})""")

fun diaClaveHistorial(iso: String?): String {
    if (iso.isNullOrBlank()) return "sin-fecha"
    return patronFecha.find(iso)?.value ?: "sin-fecha"
}

fun tituloDiaHistorial(clave: String): String {
    if (clave == "sin-fecha") return "Sin fecha"
    val hoy = fechaBogotaDias(0)
    val ayer = fechaBogotaDias(-1)
    return when (clave) {
        hoy -> "Hoy"
        ayer -> "Ayer"
        else -> clave.split("-").let { p ->
            if (p.size == 3) "${p[2]}/${p[1]}/${p[0]}" else clave
        }
    }
}

private fun fechaBogotaDias(offset: Int): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("America/Bogota"))
    cal.add(Calendar.DAY_OF_YEAR, offset)
    return String.format(Locale.US, "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialPorDias(
    items: List<Ingreso>,
    mostrarUsuario: Boolean,
    textoVacio: String
) {
    val colors = ColoresAppLocal.current
    var busqueda by remember { mutableStateOf("") }
    var filtroTipo by remember { mutableStateOf(0) }
    var fechaElegida by remember { mutableStateOf<String?>(null) }
    var mostrarCalendario by remember { mutableStateOf(false) }

    val q = busqueda.trim()
    val filtrados = items.filter { item ->
        val tipo = item.ingreso_type ?: "Entrada"
        (filtroTipo == 0 || (filtroTipo == 1 && tipo.equals("Entrada", ignoreCase = true)) || (filtroTipo == 2 && tipo.equals("Salida", ignoreCase = true))) &&
            (q.isBlank() || (item.ingreso_place?.contains(q, ignoreCase = true) == true) ||
                (item.user?.nombreCompleto?.contains(q, ignoreCase = true) == true))
    }
    val grupos = filtrados.groupBy { diaClaveHistorial(it.ingreso_datetime) }
        .toList().sortedByDescending { it.first }
    val gruposVisibles = if (fechaElegida == null) grupos.take(3) else grupos.filter { it.first == fechaElegida }

    Column {
        BuscadorSena(
            valor = busqueda,
            onValor = { busqueda = it },
            placeholder = if (mostrarUsuario) "Buscar por persona o lugar..." else "Buscar por lugar..."
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FiltroSena(texto = "Todos", seleccionado = filtroTipo == 0, onClick = { filtroTipo = 0 })
            FiltroSena(texto = "Entradas", seleccionado = filtroTipo == 1, onClick = { filtroTipo = 1 })
            FiltroSena(texto = "Salidas", seleccionado = filtroTipo == 2, onClick = { filtroTipo = 2 })
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (fechaElegida == null) {
                Text(
                    "Últimos 3 días",
                    color = colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium
                )
            } else {
                FilterChip(
                    selected = true,
                    onClick = { fechaElegida = null },
                    label = { Text(fechaElegida!!) },
                    trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) }
                )
            }
            FilterChip(
                selected = false,
                onClick = { mostrarCalendario = true },
                label = { Text(if (fechaElegida == null) "Calendario" else "Otro día") },
                leadingIcon = { Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp)) }
            )
        }
        if (mostrarCalendario) {
            val pickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { mostrarCalendario = false },
                confirmButton = {
                    TextButton(onClick = {
                        pickerState.selectedDateMillis?.let { ms ->
                            val cal = Calendar.getInstance(TimeZone.getTimeZone("America/Bogota")).apply { timeInMillis = ms }
                            fechaElegida = String.format(Locale.US, "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
                        }
                        mostrarCalendario = false
                    }) { Text("Ver día") }
                },
                dismissButton = { TextButton(onClick = { mostrarCalendario = false }) { Text("Cancelar") } }
            ) { DatePicker(state = pickerState) }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (filtrados.isEmpty()) {
            VistaVacia(icono = Icons.Default.History, titulo = textoVacio, mensaje = "Prueba con otro filtro o fecha.")
        } else if (gruposVisibles.isEmpty()) {
            VistaVacia(
                icono = Icons.Default.History,
                titulo = if (fechaElegida == null) textoVacio else "Sin movimientos ese día",
                mensaje = if (fechaElegida == null) "Tus entradas y salidas aparecerán aquí." else "No entraste el $fechaElegida. Elige otro día."
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                gruposVisibles.forEach { (clave, delDia) ->
                    item(key = "dia-$clave") {
                        Text(
                            "${tituloDiaHistorial(clave).uppercase()} (${delDia.size})",
                            color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    items(delDia, key = { it.id_ingreso ?: it.hashCode() }) { item ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = EspaciadoSena.sm), verticalAlignment = Alignment.CenterVertically) {
                            val tipo = item.ingreso_type ?: "Entrada"
                            val esSalida = tipo.equals("Salida", true)
                            if (mostrarUsuario) {
                                CeldaSena(texto = item.user?.nombreCompleto ?: "Usuario", peso = 1.8f)
                                CeldaSena(texto = fechaLegible(item.ingreso_datetime), peso = 1.6f)
                            } else {
                                CeldaSena(texto = fechaLegible(item.ingreso_datetime), peso = 2f)
                                CeldaSena(texto = item.ingreso_place ?: "CCyS", peso = 1.7f)
                            }
                            InsigniaSena(
                                texto = if (esSalida) "SALIDA" else "INGRESADO",
                                tipo = if (esSalida) TipoInsignia.ALERTA else TipoInsignia.EXITO,
                                modifier = Modifier.weight(if (mostrarUsuario) 1f else 1.1f)
                            )
                            if (mostrarUsuario) CeldaSena(texto = item.ingreso_place ?: "—", peso = 1f)
                        }
                        HorizontalDivider(color = colors.border)
                    }
                }
            }
        }
    }
}
