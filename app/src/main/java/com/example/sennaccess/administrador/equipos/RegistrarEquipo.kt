package com.example.sennaccess.administrador.equipos

// Vista de registro de un equipo (portátil) desde el panel del administrador.
// Formulario de vidrio de un solo scroll que revela secciones según lo que elige
// el usuario: datos del portátil, si lleva accesorios, cuántos y el detalle de cada
// uno (marca, color e inalámbrico cuando aplica). En modo admin (adminMode = true)
// añade un selector de dueño (instructor/aprendiz) y registra vía POST
// /admin/equipment con fk_id_usuario; al guardar llama a la API y al terminar
// regresa a la lista de equipos.

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.datos.modelos.Accesorio
import com.example.sennaccess.datos.repositorios.RepositorioEquipos
import com.example.sennaccess.datos.modelos.PeticionEquipoIngreso
import com.example.sennaccess.datos.sesion.RolSeguro
import com.example.sennaccess.datos.sesion.GestorSesion
import com.example.sennaccess.datos.modelos.UsuarioApi
import com.example.sennaccess.datos.repositorios.RepositorioUsuarios
import com.example.sennaccess.comun.campoVisible
import com.example.sennaccess.comun.diseno.RadioVidrio
import com.example.sennaccess.comun.diseno.CabeceraPlegable
import com.example.sennaccess.comun.diseno.superficieVidrio
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import kotlinx.coroutines.launch

enum class TipoAccesorio(val etiqueta: String) {
    MOUSE("Mouse"), TECLADO("Teclado"), AUDIFONOS("Audífonos")
}

class BorradorAccesorio(val tipo: TipoAccesorio) {
    var marca by mutableStateOf("")
    var color by mutableStateOf("")
    var inalambrico by mutableStateOf<Boolean?>(null)
}

@Composable
fun VistaRegistrarEquipo(onBack: () -> Unit, onRegistrado: () -> Unit, adminMode: Boolean = false) {
    val colors = ColoresAppLocal.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var marca by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var serial by remember { mutableStateOf("") }
    var llevaAccesorios by remember { mutableStateOf<Boolean?>(null) }
    var cantidad by remember { mutableStateOf<Int?>(null) }
    var accesorioUnico by remember { mutableStateOf<TipoAccesorio?>(null) }
    val drafts = remember { mutableStateMapOf<TipoAccesorio, BorradorAccesorio>() }

    var menuDuenoAbierto by remember { mutableStateOf(false) }
    var dueno by remember { mutableStateOf<UsuarioApi?>(null) }
    var usuariosDisponibles by remember { mutableStateOf<List<UsuarioApi>>(emptyList()) }
    var cargandoDuenos by remember { mutableStateOf(false) }
    var errorDuenos by remember { mutableStateOf<String?>(null) }

    // En modo admin se cargan los usuarios con rol Instructor/Aprendiz (GET /admin/users).
    LaunchedEffect(adminMode) {
        if (!adminMode) return@LaunchedEffect
        cargandoDuenos = true
        val token = GestorSesion.token
        if (token != null) {
            try {
                usuariosDisponibles = RepositorioUsuarios().getUsers(token)
                    .filter { it.esRol("Instructor") || it.esRol("Aprendiz") }
                if (usuariosDisponibles.isNotEmpty()) dueno = usuariosDisponibles.first()
            } catch (e: Exception) {
                errorDuenos = "No se pudieron cargar los usuarios."
            }
        } else {
            errorDuenos = "No hay sesión activa."
        }
        cargandoDuenos = false
    }

    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var exito by remember { mutableStateOf(false) }

    val tiposSeleccionados: List<TipoAccesorio> = when {
        llevaAccesorios != true || cantidad == null -> emptyList()
        cantidad == 1 -> listOfNotNull(accesorioUnico)
        cantidad == 2 -> listOf(TipoAccesorio.MOUSE, TipoAccesorio.TECLADO)
        else -> listOf(TipoAccesorio.MOUSE, TipoAccesorio.TECLADO, TipoAccesorio.AUDIFONOS)
    }

    val datosValidos = marca.isNotBlank() && color.isNotBlank() &&
        serial.length == 5 && serial.all { it.isDigit() } &&
        (!adminMode || dueno != null)
    val accesoriosValidos = if (llevaAccesorios == true) {
        tiposSeleccionados.isNotEmpty() && tiposSeleccionados.all { tipo ->
            val borrador = drafts[tipo]
            borrador != null && borrador.marca.isNotBlank() && borrador.color.isNotBlank() &&
                (borrador.tipo == TipoAccesorio.TECLADO || borrador.inalambrico != null)
        }
    } else true

    if (exito) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Equipo registrado", color = verdeMarca(), fontWeight = FontWeight.Bold) },
            text = { Text("El comprobante de ingreso del equipo se guardó correctamente.", color = colors.textPrimary) },
            confirmButton = {
                TextButton(onClick = onRegistrado) { Text("Okey", color = verdeMarca(), fontWeight = FontWeight.Bold) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .imePadding()
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = colors.textPrimary) }

        CabeceraPlegable(
            title = "Registrar Equipo",
            subtitle = "Comprobante de ingreso de un portátil",
            scrollOffset = scrollState.value.toFloat()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .superficieVidrio(cornerRadius = RadioVidrio)
                .padding(20.dp)
        ) {
            // ---- Datos del portátil ----
            TituloSeccion("DATOS DEL PORTÁTIL")
            CampoFormulario(value = marca, onValueChange = { marca = it }, label = "Marca del portátil")
            Spacer(modifier = Modifier.height(10.dp))
            CampoFormulario(value = color, onValueChange = { color = it }, label = "Color")
            Spacer(modifier = Modifier.height(10.dp))
            CampoFormulario(
                value = serial,
                onValueChange = { serial = it.filter { c -> c.isDigit() }.take(5) },
                label = "Últimos 5 dígitos del serial",
                numero = true
            )

            // ---- Dueño del equipo (solo admin) ----
            if (adminMode) {
                Spacer(modifier = Modifier.height(20.dp))
                TituloSeccion("DUEÑO DEL EQUIPO")
                when {
                    cargandoDuenos -> CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = verdeMarca(),
                        strokeWidth = 2.dp
                    )
                    usuariosDisponibles.isEmpty() -> Text(
                        errorDuenos ?: "No hay instructores ni aprendices registrados.",
                        color = Color(0xFFE53935),
                        fontSize = 13.sp
                    )
                    else -> Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = dueno?.nombreCompleto ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Dueño del equipo", color = colors.textSecondary, fontSize = 14.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = camposEquipoColors(),
                            trailingIcon = {
                                IconButton(onClick = { menuDuenoAbierto = !menuDuenoAbierto }) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = verdeMarca())
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = menuDuenoAbierto,
                            onDismissRequest = { menuDuenoAbierto = false },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            usuariosDisponibles.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u.nombreCompleto, color = colors.textPrimary) },
                                    onClick = { dueno = u; menuDuenoAbierto = false }
                                )
                            }
                        }
                    }
                }
            }

            // ---- ¿Lleva accesorios? ----
            Spacer(modifier = Modifier.height(20.dp))
            TituloSeccion("ACCESORIOS")
            Text("¿El portátil lleva accesorios?", color = colors.textSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            FilaSelector(
                opciones = listOf("Sí", "No"),
                seleccionada = when (llevaAccesorios) { true -> 0; false -> 1; else -> -1 },
                onSeleccion = { idx ->
                    llevaAccesorios = idx == 0
                    cantidad = null
                    accesorioUnico = null
                }
            )

            // ---- Cantidad de accesorios ----
            if (llevaAccesorios == true) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("¿Cuántos accesorios lleva?", color = colors.textSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                FilaSelector(
                    opciones = listOf("1", "2", "3"),
                    seleccionada = cantidad?.let { it - 1 } ?: -1,
                    onSeleccion = { idx ->
                        cantidad = idx + 1
                        accesorioUnico = null
                    }
                )

                // ---- Elegir cuál si lleva solo uno ----
                if (cantidad == 1) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("¿Cuál accesorio lleva?", color = colors.textSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    FilaSelector(
                        opciones = TipoAccesorio.entries.map { it.etiqueta },
                        seleccionada = accesorioUnico?.let { tipo -> TipoAccesorio.entries.indexOf(tipo) } ?: -1,
                        onSeleccion = { idx -> accesorioUnico = TipoAccesorio.entries[idx] }
                    )
                }

                // ---- Detalle de cada accesorio seleccionado ----
                tiposSeleccionados.forEach { tipo ->
                    Spacer(modifier = Modifier.height(16.dp))
                    SeccionAccesorio(draft = drafts.getOrPut(tipo) { BorradorAccesorio(tipo) })
                }
            }

            // ---- Mensaje de error ----
            if (error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(error!!, color = Color(0xFFE53935), fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
            }

            // ---- Guardar ----
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (guardando) return@Button
                    error = null
                    guardando = true
                    scope.launch {
                        try {
                            val token = GestorSesion.token
                                ?: throw IllegalStateException("No hay sesión activa")
                            val accesorios = tiposSeleccionados.map { tipo ->
                                val d = drafts[tipo] ?: BorradorAccesorio(tipo)
                                Accesorio(
                                    tipo = tipo.etiqueta,
                                    marca = d.marca.ifBlank { null },
                                    color = d.color.ifBlank { null },
                                    inalambrico = d.inalambrico
                                )
                            }
                            val request = PeticionEquipoIngreso(
                                equipo_type = "Portátil",
                                equipo_brand = marca.trim(),
                                equipo_color = color.trim(),
                                equipo_serial = serial,
                                fk_id_usuario = if (adminMode) dueno?.id_usuario else null,
                                equipo_accesorios = accesorios.ifEmpty { null }
                            )
                            if (adminMode) {
                                RepositorioEquipos().registrar(token, request)
                            } else if (RolSeguro.normalizar(GestorSesion.userRole) == "aprendiz") {
                                RepositorioEquipos().registrarPropio(token, request)
                            } else {
                                RepositorioEquipos().registrar(token, request)
                            }
                            exito = true
                        } catch (e: Exception) {
                            error = "No se pudo registrar el equipo: ${e.message ?: "error de conexión"}"
                        } finally {
                            guardando = false
                        }
                    }
                },
                enabled = datosValidos && accesoriosValidos && !guardando,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = Color.Black),
                shape = RoundedCornerShape(28.dp)
            ) {
                if (guardando) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Text("REGISTRAR EQUIPO", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
private fun TituloSeccion(texto: String) {
    Text(texto, color = verdeMarca(), fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp)
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun CampoFormulario(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    numero: Boolean = false
) {
    val colors = ColoresAppLocal.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = colors.textSecondary, fontSize = 14.sp) },
        modifier = Modifier.fillMaxWidth().campoVisible(),
        singleLine = true,
        keyboardOptions = if (numero) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = verdeMarca(),
            unfocusedBorderColor = colors.divider,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            focusedContainerColor = colors.surfaceVariant.copy(alpha = 0.5f),
            unfocusedContainerColor = colors.surfaceVariant.copy(alpha = 0.5f)
        )
    )
}

@Composable
private fun camposEquipoColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = verdeMarca(),
    unfocusedBorderColor = ColoresAppLocal.current.textSecondary.copy(alpha = 0.5f),
    focusedLabelColor = verdeMarca(),
    unfocusedLabelColor = ColoresAppLocal.current.textSecondary,
    cursorColor = verdeMarca(),
    focusedTextColor = ColoresAppLocal.current.textPrimary,
    unfocusedTextColor = ColoresAppLocal.current.textPrimary,
    focusedContainerColor = ColoresAppLocal.current.surfaceVariant.copy(alpha = 0.5f),
    unfocusedContainerColor = ColoresAppLocal.current.surfaceVariant.copy(alpha = 0.5f)
)

@Composable
private fun FilaSelector(
    opciones: List<String>,
    seleccionada: Int,
    onSeleccion: (Int) -> Unit
) {
    val colors = ColoresAppLocal.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        opciones.forEachIndexed { idx, etiqueta ->
            FilterChip(
                selected = idx == seleccionada,
                onClick = { onSeleccion(idx) },
                label = { Text(etiqueta, fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = VerdeSena,
                    selectedLabelColor = Color.Black,
                    containerColor = colors.surfaceVariant.copy(alpha = 0.4f),
                    labelColor = colors.textSecondary
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SeccionAccesorio(draft: BorradorAccesorio) {
    val colors = ColoresAppLocal.current
    val usaInalambrico = draft.tipo != TipoAccesorio.TECLADO
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .superficieVidrio(cornerRadius = 16.dp)
            .padding(16.dp)
    ) {
        Text(draft.tipo.etiqueta.uppercase(), color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(10.dp))
        CampoFormulario(
            value = draft.marca,
            onValueChange = { draft.marca = it },
            label = "Marca del ${draft.tipo.etiqueta.lowercase()}"
        )
        Spacer(modifier = Modifier.height(10.dp))
        CampoFormulario(
            value = draft.color,
            onValueChange = { draft.color = it },
            label = "Color del ${draft.tipo.etiqueta.lowercase()}"
        )
        if (usaInalambrico) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("¿Es inalámbrico?", color = colors.textSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            FilaSelector(
                opciones = listOf("Sí", "No"),
                seleccionada = when (draft.inalambrico) { true -> 0; false -> 1; else -> -1 },
                onSeleccion = { draft.inalambrico = it == 0 }
            )
        }
    }
}
