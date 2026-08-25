package com.example.sennaccess.aprendiz

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
import com.example.sennaccess.data.Accesorio
import com.example.sennaccess.data.EquipoRepository
import com.example.sennaccess.data.IngresoEquipoRequest
import com.example.sennaccess.data.SessionManager
import com.example.sennaccess.data.UsuarioApi
import com.example.sennaccess.data.UsuarioRepository
import com.example.sennaccess.ui.ios.GlassCornerRadius
import com.example.sennaccess.ui.ios.IosCollapsibleHeader
import com.example.sennaccess.ui.ios.glassSurface
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import kotlinx.coroutines.launch

// Tipos de accesorio disponibles para el portátil.
enum class TipoAccesorio(val etiqueta: String) {
    MOUSE("Mouse"), TECLADO("Teclado"), AUDIFONOS("Audífonos")
}

// Borrador de un accesorio mientras se llena el formulario: usa estado observable
// para que cada tecla/recomposición actualice el detalle en la tarjeta.
class AccesorioDraft(val tipo: TipoAccesorio) {
    var marca by mutableStateOf("")
    var color by mutableStateOf("")
    var inalambrico by mutableStateOf<Boolean?>(null)
}

@Composable
fun RegistrarEquipoView(onBack: () -> Unit, onRegistrado: () -> Unit, adminMode: Boolean = false) {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Estado del formulario: datos del portátil y opciones de accesorios.
    var marca by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var serial by remember { mutableStateOf("") }
    var llevaAccesorios by remember { mutableStateOf<Boolean?>(null) }
    var cantidad by remember { mutableStateOf<Int?>(null) }
    var accesorioUnico by remember { mutableStateOf<TipoAccesorio?>(null) }
    val drafts = remember { mutableStateMapOf<TipoAccesorio, AccesorioDraft>() }

    // Modo admin: selector del dueño del equipo (instructores y aprendices).
    var menuDuenoAbierto by remember { mutableStateOf(false) }
    var dueno by remember { mutableStateOf<UsuarioApi?>(null) }
    var usuariosDisponibles by remember { mutableStateOf<List<UsuarioApi>>(emptyList()) }
    var cargandoDuenos by remember { mutableStateOf(false) }
    var errorDuenos by remember { mutableStateOf<String?>(null) }

    // En modo admin se cargan los usuarios con rol Instructor/Aprendiz (GET /admin/users).
    LaunchedEffect(adminMode) {
        if (!adminMode) return@LaunchedEffect
        cargandoDuenos = true
        val token = SessionManager.token
        if (token != null) {
            try {
                usuariosDisponibles = UsuarioRepository().getUsers(token)
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

    // Accesorios correspondientes a la cantidad elegida (1 elige uno; 2 y 3 fijos).
    val tiposSeleccionados: List<TipoAccesorio> = when {
        llevaAccesorios != true || cantidad == null -> emptyList()
        cantidad == 1 -> listOfNotNull(accesorioUnico)
        cantidad == 2 -> listOf(TipoAccesorio.MOUSE, TipoAccesorio.TECLADO)
        else -> listOf(TipoAccesorio.MOUSE, TipoAccesorio.TECLADO, TipoAccesorio.AUDIFONOS)
    }

    // Valida que todos los campos requeridos estén completos antes de guardar.
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

    // Diálogo de éxito al terminar el registro.
    if (exito) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = colors.cardBackground,
            title = { Text("Equipo registrado", color = SenaGreen, fontWeight = FontWeight.Bold) },
            text = { Text("El comprobante de ingreso del equipo se guardó correctamente.", color = colors.textPrimary) },
            confirmButton = {
                TextButton(onClick = onRegistrado) { Text("Okey", color = SenaGreen, fontWeight = FontWeight.Bold) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = colors.textPrimary) }

        IosCollapsibleHeader(
            title = "Registrar Equipo",
            subtitle = "Comprobante de ingreso de un portátil",
            scrollOffset = scrollState.value.toFloat()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassSurface(cornerRadius = GlassCornerRadius)
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
                        color = SenaGreen,
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
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = SenaGreen)
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = menuDuenoAbierto,
                            onDismissRequest = { menuDuenoAbierto = false },
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
                    SeccionAccesorio(draft = drafts.getOrPut(tipo) { AccesorioDraft(tipo) })
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
                    error = null
                    guardando = true
                    scope.launch {
                        try {
                            val token = SessionManager.token
                                ?: throw IllegalStateException("No hay sesión activa")
                            val accesorios = tiposSeleccionados.map { tipo ->
                                val d = drafts[tipo] ?: AccesorioDraft(tipo)
                                Accesorio(
                                    tipo = tipo.etiqueta,
                                    marca = d.marca.ifBlank { null },
                                    color = d.color.ifBlank { null },
                                    inalambrico = d.inalambrico
                                )
                            }
                            val request = IngresoEquipoRequest(
                                equipo_type = "Portátil",
                                equipo_brand = marca.trim(),
                                equipo_color = color.trim(),
                                equipo_serial = serial,
                                fk_id_usuario = if (adminMode) dueno?.id_usuario else null,
                                equipo_accesorios = accesorios.ifEmpty { null }
                            )
                            // El admin registra vía /admin/equipment con dueño asignado;
                            // el resto de roles (legacy) vía /my-equipment o /admin/equipment.
                            if (adminMode) {
                                EquipoRepository().registrar(token, request)
                            } else if (SessionManager.userRole == "Aprendiz") {
                                EquipoRepository().registrarPropio(token, request)
                            } else {
                                EquipoRepository().registrar(token, request)
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
                colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
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

// Título de una sección dentro del formulario (p. ej. DATOS DEL PORTÁTIL).
@Composable
private fun TituloSeccion(texto: String) {
    Text(texto, color = SenaGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp)
    Spacer(modifier = Modifier.height(8.dp))
}

// Campo de texto reutilizable con el estilo de vidrio de la app.
@Composable
private fun CampoFormulario(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    numero: Boolean = false
) {
    val colors = LocalAppColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = colors.textSecondary, fontSize = 14.sp) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = if (numero) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SenaGreen,
            unfocusedBorderColor = colors.divider,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            focusedContainerColor = colors.surfaceVariant.copy(alpha = 0.5f),
            unfocusedContainerColor = colors.surfaceVariant.copy(alpha = 0.5f)
        )
    )
}

// Paleta de colores común para los campos del formulario de equipo.
@Composable
private fun camposEquipoColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SenaGreen,
    unfocusedBorderColor = LocalAppColors.current.textSecondary.copy(alpha = 0.5f),
    focusedLabelColor = SenaGreen,
    unfocusedLabelColor = LocalAppColors.current.textSecondary,
    cursorColor = SenaGreen,
    focusedTextColor = LocalAppColors.current.textPrimary,
    unfocusedTextColor = LocalAppColors.current.textPrimary,
    focusedContainerColor = LocalAppColors.current.surfaceVariant.copy(alpha = 0.5f),
    unfocusedContainerColor = LocalAppColors.current.surfaceVariant.copy(alpha = 0.5f)
)

// Fila de opciones seleccionables (Sí/No, cantidades o tipos de accesorio).
@Composable
private fun FilaSelector(
    opciones: List<String>,
    seleccionada: Int,
    onSeleccion: (Int) -> Unit
) {
    val colors = LocalAppColors.current
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
                    selectedContainerColor = SenaGreen,
                    selectedLabelColor = Color.Black,
                    containerColor = colors.surfaceVariant.copy(alpha = 0.4f),
                    labelColor = colors.textSecondary
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// Tarjeta con el detalle de un accesorio: marca, color y (si aplica) inalámbrico.
@Composable
private fun SeccionAccesorio(draft: AccesorioDraft) {
    val colors = LocalAppColors.current
    val usaInalambrico = draft.tipo != TipoAccesorio.TECLADO
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = 16.dp)
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
