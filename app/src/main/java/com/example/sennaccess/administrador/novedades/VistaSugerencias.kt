package com.example.sennaccess.administrador.novedades

// Buzón de sugerencias: cualquier rol envía (POST /api/sugerencias, tope de
// 3 por día que informa el servidor) y consulta las propias con su respuesta
// (GET /api/my-sugerencias); el admin además ve la bandeja completa con
// buscador y filtros por estado (GET /api/sugerencias), responde
// (PUT /api/admin/sugerencias/{id}/responder) y elimina con confirmación
// (DELETE /api/admin/sugerencias/{id}).

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.comun.EstadoCarga
import com.example.sennaccess.comun.EstadoContenido
import com.example.sennaccess.comun.VistaVacia
import com.example.sennaccess.comun.detalleHttp
import com.example.sennaccess.comun.diseno.BuscadorSena
import com.example.sennaccess.comun.diseno.CabeceraPlegable
import com.example.sennaccess.comun.diseno.FiltroSena
import com.example.sennaccess.comun.diseno.InsigniaSena
import com.example.sennaccess.comun.diseno.TarjetaSena
import com.example.sennaccess.comun.diseno.TipoInsignia
import com.example.sennaccess.comun.diseno.escalaPresion
import com.example.sennaccess.comun.fechaRelativa
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.RojoError
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import com.example.sennaccess.datos.modelos.PeticionResponderSugerencia
import com.example.sennaccess.datos.modelos.PeticionSugerencia
import com.example.sennaccess.datos.modelos.Sugerencia
import com.example.sennaccess.datos.repositorios.RepositorioSugerencias
import com.example.sennaccess.datos.sesion.GestorSesion
import com.example.sennaccess.datos.sesion.RolSeguro
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.HttpException

// Categorías y estados tal como los define el modelo Sugerencia del backend.
private val categoriasSugerencia = listOf("Sistema", "Ambientes", "Procesos", "Otro")
private val estadosSugerencia = listOf("Pendiente", "En revisión", "Implementada", "Rechazada")

@Composable
fun VistaSugerencias(
    estado: EstadoCarga<List<Sugerencia>>? = null,
    onReintentar: () -> Unit = {}
) {
    val colors = ColoresAppLocal.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    // Rol normalizado: solo "admin" ve la bandeja (RolSeguro acepta "administrador").
    val esAdmin = RolSeguro.normalizar(GestorSesion.userRole) == "admin"

    var mostrandoFormulario by remember { mutableStateOf(false) }
    var asunto by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("") }
    var cuerpo by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var errorEnvio by remember { mutableStateOf<String?>(null) }
    var enviada by remember { mutableStateOf(false) }

    var busqueda by remember { mutableStateOf("") }
    var filtroEstado by remember { mutableStateOf("") }

    var estadoInterno by remember { mutableStateOf<EstadoCarga<List<Sugerencia>>>(EstadoCarga.Loading) }
    var recargaKey by remember { mutableIntStateOf(0) }

    var sugerenciaAResponder by remember { mutableStateOf<Sugerencia?>(null) }
    var sugerenciaAEliminar by remember { mutableStateOf<Sugerencia?>(null) }

    // Sin estado externo la vista carga sola: el admin usa la bandeja con
    // filtros y los demás roles solo sus propias sugerencias.
    LaunchedEffect(esAdmin, busqueda, filtroEstado, recargaKey) {
        if (estado != null) return@LaunchedEffect
        delay(400)
        estadoInterno = EstadoCarga.Loading
        val token = GestorSesion.token
        if (token == null) {
            estadoInterno = EstadoCarga.Error("Sesión expirada. Inicia sesión de nuevo.")
            return@LaunchedEffect
        }
        try {
            val repo = RepositorioSugerencias()
            val items = if (esAdmin) {
                repo.getSugerenciasAdmin(
                    token,
                    q = busqueda.ifBlank { null },
                    status = filtroEstado.ifBlank { null }
                )
            } else {
                repo.getMisSugerencias(token)
            }
            estadoInterno = EstadoCarga.Success(items)
        } catch (e: HttpException) {
            estadoInterno = EstadoCarga.Error(detalleHttp(e))
        } catch (e: Exception) {
            estadoInterno = EstadoCarga.Error("Fallo de conexión: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    val estadoEfectivo = estado ?: estadoInterno
    fun refrescar() {
        if (estado != null) onReintentar() else recargaKey++
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        CabeceraPlegable(
            title = "Sugerencias",
            subtitle = if (esAdmin) "Bandeja de ideas de la comunidad" else "Tus ideas para mejorar el centro",
            scrollOffset = scrollState.value.toFloat()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (enviada) {
            TarjetaSugerenciaEnviada(onAceptar = { enviada = false })
        } else if (mostrandoFormulario) {
            FormularioSugerencia(
                asunto = asunto,
                categoria = categoria,
                cuerpo = cuerpo,
                onAsuntoChange = { asunto = it },
                onCategoriaChange = { categoria = it },
                onCuerpoChange = { cuerpo = it },
                enviando = enviando,
                errorMensaje = errorEnvio,
                onEnviar = {
                    val token = GestorSesion.token
                    when {
                        token == null -> errorEnvio = "Sesión expirada. Inicia sesión de nuevo."
                        asunto.isBlank() || cuerpo.isBlank() || categoria.isBlank() ->
                            errorEnvio = "Completa el asunto, la categoría y la descripción."
                        asunto.trim().length > 150 ->
                            errorEnvio = "El asunto no puede pasar de 150 caracteres."
                        cuerpo.trim().length > 5000 ->
                            errorEnvio = "La descripción no puede pasar de 5000 caracteres."
                        !enviando -> {
                            errorEnvio = null
                            enviando = true
                            scope.launch {
                                try {
                                    RepositorioSugerencias().crear(
                                        token,
                                        PeticionSugerencia(
                                            sugerencia_asunto = asunto.trim(),
                                            sugerencia_body = cuerpo.trim(),
                                            sugerencia_categoria = categoria
                                        )
                                    )
                                    enviando = false
                                    asunto = ""
                                    categoria = ""
                                    cuerpo = ""
                                    enviada = true
                                    mostrandoFormulario = false
                                    refrescar()
                                } catch (e: HttpException) {
                                    enviando = false
                                    errorEnvio = detalleHttp(e)
                                } catch (e: Exception) {
                                    enviando = false
                                    errorEnvio = "Fallo de conexión: ${e.message ?: e.javaClass.simpleName}"
                                }
                            }
                        }
                    }
                },
                onCancelar = { mostrandoFormulario = false }
            )
        } else {
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
                Text("NUEVA SUGERENCIA", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (esAdmin) {
            BuscadorSena(
                valor = busqueda,
                onValor = { busqueda = it },
                placeholder = "Buscar por asunto, texto o autor"
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FiltroSena(texto = "Todas", seleccionado = filtroEstado.isEmpty(), onClick = { filtroEstado = "" })
                FiltroSena(texto = "Pendientes", seleccionado = filtroEstado == "Pendiente", onClick = { filtroEstado = "Pendiente" })
                FiltroSena(texto = "En revisión", seleccionado = filtroEstado == "En revisión", onClick = { filtroEstado = "En revisión" })
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FiltroSena(texto = "Implementadas", seleccionado = filtroEstado == "Implementada", onClick = { filtroEstado = "Implementada" })
                FiltroSena(texto = "Rechazadas", seleccionado = filtroEstado == "Rechazada", onClick = { filtroEstado = "Rechazada" })
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Text(
            if (esAdmin) "BANDEJA DE SUGERENCIAS" else "MIS SUGERENCIAS",
            color = colors.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        EstadoContenido(estado = estadoEfectivo, onReintentar = ::refrescar) { items ->
            if (items.isEmpty()) {
                VistaVacia(
                    icono = Icons.Default.Lightbulb,
                    titulo = if (esAdmin) "Bandeja vacía" else "Aún no envías sugerencias",
                    mensaje = if (esAdmin) "No hay sugerencias con esos filtros." else "Cuéntanos cómo mejorar: tu idea llega directo al administrador."
                )
            } else {
                items.forEach { s ->
                    TarjetaSugerencia(
                        s = s,
                        mostrarAutor = esAdmin,
                        onResponder = if (esAdmin) {
                            { sugerenciaAResponder = s }
                        } else null,
                        onEliminar = if (esAdmin) {
                            { sugerenciaAEliminar = s }
                        } else null
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    sugerenciaAResponder?.let { s ->
        DialogoResponderSugerencia(
            sugerencia = s,
            onCerrar = { sugerenciaAResponder = null },
            onRespondida = { refrescar() }
        )
    }

    sugerenciaAEliminar?.let { s ->
        DialogoEliminarSugerencia(
            sugerencia = s,
            onCerrar = { sugerenciaAEliminar = null },
            onEliminada = { refrescar() }
        )
    }
}

@Composable
private fun TarjetaSugerencia(
    s: Sugerencia,
    mostrarAutor: Boolean,
    onResponder: (() -> Unit)?,
    onEliminar: (() -> Unit)?
) {
    val colors = ColoresAppLocal.current
    val estadoTexto = s.sugerencia_status ?: "Pendiente"
    val autor = listOfNotNull(s.user?.user_name, s.user?.user_lastname)
        .joinToString(" ").ifBlank { null }
    TarjetaSena(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Lightbulb,
                contentDescription = null,
                tint = verdeMarca(),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                s.sugerencia_asunto ?: "Sugerencia",
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InsigniaSena(texto = estadoTexto, tipo = tipoPorEstado(estadoTexto))
            val cat = s.sugerencia_categoria
            if (!cat.isNullOrBlank()) {
                InsigniaSena(texto = cat, tipo = TipoInsignia.NEUTRO)
            }
        }
        if (mostrarAutor && autor != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text("Por $autor", color = colors.textSecondary, fontSize = 12.sp)
        }
        Text(
            fechaRelativa(s.created_at),
            color = colors.textSecondary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            s.sugerencia_body ?: "—",
            color = colors.textSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        val respuesta = s.respuesta_admin
        if (!respuesta.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Respuesta del administrador",
                color = verdeMarca(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(respuesta, color = colors.textPrimary, fontSize = 13.sp, lineHeight = 18.sp)
        }
        if (onResponder != null || onEliminar != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (onResponder != null) {
                    OutlinedButton(
                        onClick = onResponder,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = verdeMarca())
                    ) {
                        Icon(Icons.Default.Reply, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Responder", fontWeight = FontWeight.Bold)
                    }
                }
                if (onEliminar != null) {
                    IconButton(onClick = onEliminar, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar sugerencia", tint = RojoError, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

private fun tipoPorEstado(estado: String): TipoInsignia {
    return when (estado) {
        "Implementada" -> TipoInsignia.EXITO
        "En revisión" -> TipoInsignia.ALERTA
        "Rechazada" -> TipoInsignia.ERROR
        else -> TipoInsignia.AVISO
    }
}

@Composable
private fun FormularioSugerencia(
    asunto: String,
    categoria: String,
    cuerpo: String,
    onAsuntoChange: (String) -> Unit,
    onCategoriaChange: (String) -> Unit,
    onCuerpoChange: (String) -> Unit,
    enviando: Boolean,
    errorMensaje: String?,
    onEnviar: () -> Unit,
    onCancelar: () -> Unit
) {
    val colors = ColoresAppLocal.current
    TarjetaSena(modifier = Modifier.fillMaxWidth()) {
        Text("Nueva sugerencia", color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Máximo 3 por día. El administrador la leerá y te responderá aquí mismo.",
            color = colors.textSecondary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
            value = asunto,
            onValueChange = onAsuntoChange,
            label = { Text("Asunto (${asunto.length}/150)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = sugerenciaCamposColors()
        )
        Spacer(modifier = Modifier.height(12.dp))
        DesplegableSugerencia(
            etiqueta = "Categoría",
            valor = categoria,
            opciones = categoriasSugerencia,
            onElegir = onCategoriaChange
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = cuerpo,
            onValueChange = onCuerpoChange,
            label = { Text("Describe tu idea (${cuerpo.length}/5000)") },
            modifier = Modifier.fillMaxWidth().height(130.dp),
            shape = RoundedCornerShape(16.dp),
            colors = sugerenciaCamposColors()
        )
        if (errorMensaje != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(errorMensaje, color = RojoError, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onEnviar,
                enabled = !enviando,
                modifier = Modifier.weight(1f).height(48.dp).escalaPresion(pressedScale = 0.97f),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = Color.Black)
            ) { Text(if (enviando) "ENVIANDO..." else "ENVIAR", fontWeight = FontWeight.Bold) }
            OutlinedButton(
                onClick = onCancelar,
                enabled = !enviando,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary)
            ) { Text("CANCELAR") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DesplegableSugerencia(
    etiqueta: String,
    valor: String,
    opciones: List<String>,
    onElegir: (String) -> Unit
) {
    val colors = ColoresAppLocal.current
    var abierto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = abierto,
        onExpandedChange = { abierto = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = valor.ifBlank { "Toca para elegir" },
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(etiqueta) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            shape = RoundedCornerShape(16.dp),
            colors = sugerenciaCamposColors(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) }
        )
        ExposedDropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false },
            shape = RoundedCornerShape(16.dp)
        ) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion, color = colors.textPrimary) },
                    onClick = {
                        onElegir(opcion)
                        abierto = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
private fun DialogoResponderSugerencia(
    sugerencia: Sugerencia,
    onCerrar: () -> Unit,
    onRespondida: () -> Unit
) {
    val colors = ColoresAppLocal.current
    val scope = rememberCoroutineScope()
    var respuesta by remember { mutableStateOf(sugerencia.respuesta_admin.orEmpty()) }
    var nuevoEstado by remember { mutableStateOf(sugerencia.sugerencia_status ?: "En revisión") }
    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!guardando) onCerrar() },
        containerColor = colors.cardBackground.copy(alpha = 0.98f),
        shape = RoundedCornerShape(28.dp),
        title = { Text("Responder sugerencia", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    sugerencia.sugerencia_asunto ?: "Sugerencia",
                    color = colors.textSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = respuesta,
                    onValueChange = { respuesta = it },
                    label = { Text("Respuesta (${respuesta.length}/5000)") },
                    modifier = Modifier.fillMaxWidth().height(130.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = sugerenciaCamposColors()
                )
                Spacer(modifier = Modifier.height(12.dp))
                DesplegableSugerencia(
                    etiqueta = "Estado",
                    valor = nuevoEstado,
                    opciones = estadosSugerencia,
                    onElegir = { nuevoEstado = it }
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error ?: "", color = RojoError, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val token = GestorSesion.token
                    val id = sugerencia.id_sugerencia
                    when {
                        token == null || id == null -> onCerrar()
                        respuesta.isBlank() -> error = "Escribe la respuesta antes de guardar."
                        respuesta.trim().length > 5000 -> error = "La respuesta no puede pasar de 5000 caracteres."
                        !guardando -> {
                            error = null
                            guardando = true
                            scope.launch {
                                try {
                                    RepositorioSugerencias().responder(
                                        token,
                                        id,
                                        PeticionResponderSugerencia(
                                            respuesta_admin = respuesta.trim(),
                                            sugerencia_status = nuevoEstado
                                        )
                                    )
                                    guardando = false
                                    onCerrar()
                                    onRespondida()
                                } catch (e: HttpException) {
                                    guardando = false
                                    error = detalleHttp(e)
                                } catch (e: Exception) {
                                    guardando = false
                                    error = "Fallo de conexión: ${e.message ?: e.javaClass.simpleName}"
                                }
                            }
                        }
                    }
                },
                enabled = !guardando,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = Color.Black)
            ) { Text(if (guardando) "Guardando..." else "Guardar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onCerrar, enabled = !guardando) { Text("Cancelar", color = colors.textSecondary) }
        }
    )
}

@Composable
private fun DialogoEliminarSugerencia(
    sugerencia: Sugerencia,
    onCerrar: () -> Unit,
    onEliminada: () -> Unit
) {
    val colors = ColoresAppLocal.current
    val scope = rememberCoroutineScope()
    var eliminando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!eliminando) onCerrar() },
        containerColor = colors.cardBackground.copy(alpha = 0.98f),
        shape = RoundedCornerShape(28.dp),
        icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = RojoError, modifier = Modifier.size(36.dp)) },
        title = { Text("Eliminar sugerencia", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("¿Seguro que deseas eliminar esta sugerencia?", color = colors.textSecondary)
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error ?: "", color = RojoError, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val token = GestorSesion.token
                    val id = sugerencia.id_sugerencia
                    if (token == null || id == null || eliminando) return@Button
                    eliminando = true
                    scope.launch {
                        try {
                            RepositorioSugerencias().eliminar(token, id)
                            eliminando = false
                            onCerrar()
                            onEliminada()
                        } catch (e: HttpException) {
                            eliminando = false
                            error = detalleHttp(e)
                        } catch (e: Exception) {
                            eliminando = false
                            error = "Fallo de conexión: ${e.message ?: e.javaClass.simpleName}"
                        }
                    }
                },
                enabled = !eliminando,
                colors = ButtonDefaults.buttonColors(containerColor = RojoError, contentColor = Color.Black),
                shape = RoundedCornerShape(28.dp)
            ) { Text(if (eliminando) "Eliminando..." else "Eliminar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onCerrar, enabled = !eliminando) { Text("Cancelar", color = colors.textSecondary) }
        }
    )
}

@Composable
private fun TarjetaSugerenciaEnviada(onAceptar: () -> Unit) {
    val colors = ColoresAppLocal.current
    TarjetaSena(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = verdeMarca(), modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Sugerencia enviada", color = verdeMarca(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Gracias por tu idea. Podrás ver la respuesta del administrador en esta misma pantalla.",
                color = colors.textSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
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
}

@Composable
private fun sugerenciaCamposColors() = OutlinedTextFieldDefaults.colors(
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
