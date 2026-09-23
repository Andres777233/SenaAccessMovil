package com.example.sennaccess.aprendiz.panel

// Pantalla principal del Aprendiz: orquesta las 4 vistas del dashboard
// (Resumen, Historial, Comprobantes, Perfil) sobre un layout
// glassmorphism iOS con barra superior, contenido dinámico y dock flotante.

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.datos.modelos.Ingreso
import com.example.sennaccess.datos.modelos.EquipoIngreso
import com.example.sennaccess.datos.modelos.Notificacion
import com.example.sennaccess.datos.sesion.GestorSesion
import com.example.sennaccess.datos.modelos.UsuarioApi
import com.example.sennaccess.datos.simulados.DatosSimulados
import com.example.sennaccess.excusas.VistaMisExcusas
import com.example.sennaccess.comun.diseno.TarjetaSena
import com.example.sennaccess.comun.diseno.CeldaSena
import com.example.sennaccess.perfil.FotoPerfil
import com.example.sennaccess.comun.EstadoCarga
import com.example.sennaccess.comun.CajaCargando
import com.example.sennaccess.perfil.VistaEditarPerfil
import com.example.sennaccess.comun.CajaError
import com.example.sennaccess.comun.EstadoContenido
import com.example.sennaccess.comun.VistaVacia
import com.example.sennaccess.perfil.FilaDato
import com.example.sennaccess.comun.HistorialPorDias
import com.example.sennaccess.biometria.MiSeccionHuella
import com.example.sennaccess.administrador.novedades.VistaNotificaciones
import com.example.sennaccess.perfil.CabeceraPerfil
import com.example.sennaccess.comun.fechaLegible
import com.example.sennaccess.comun.fechaRelativa
import com.example.sennaccess.comun.horaCorta
import com.example.sennaccess.autenticacion.verificacion.VistaConfigDobleFactor
import com.example.sennaccess.autenticacion.verificacion.VistaPendientesDobleFactor
import com.example.sennaccess.comun.tema.RojoError
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.NaranjaAmbar
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import com.example.sennaccess.comun.diseno.BarraNavegacion
import com.example.sennaccess.comun.diseno.ElementoNavegacion
import com.example.sennaccess.comun.diseno.BotonBordeBrillante
import com.example.sennaccess.comun.diseno.EsferasBrillo
import com.example.sennaccess.comun.diseno.CabeceraPlegable
import com.example.sennaccess.comun.diseno.MenuDesplegableVidrio
import com.example.sennaccess.comun.diseno.BarraSuperiorVidrio
import com.example.sennaccess.comun.diseno.BotonPrimarioNeon
import com.example.sennaccess.comun.diseno.BotonCambiarTema
import com.example.sennaccess.comun.diseno.superficieVidrio
import com.example.sennaccess.comun.diseno.RadioVidrio
import com.example.sennaccess.comun.diseno.TipoInsignia
import com.example.sennaccess.comun.diseno.InsigniaSena
import com.example.sennaccess.comun.diseno.RutasSena
import com.example.sennaccess.comun.diseno.EspaciadoSena
import com.example.sennaccess.comun.diseno.claveNavegacion
import com.example.sennaccess.comun.diseno.escalaPresion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelAprendiz(onCerrarSesion: () -> Unit, isDark: Boolean = true, onToggleTheme: () -> Unit = {}) {
    var currentView by rememberSaveable { mutableStateOf("DASHBOARD") }
    val colors = ColoresAppLocal.current
    // Instancia ligada al usuario en sesión: otro login nunca reutiliza el perfil en memoria.
    val viewModel: PanelAprendizViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        key = "panel-aprendiz-${GestorSesion.userId ?: "anon"}"
    )
    val scope = rememberCoroutineScope()
    var refrescando by remember { mutableStateOf(false) }

    fun cargarActual() {
        when (currentView) {
            "DASHBOARD" -> viewModel.cargarResumen()
            "HISTORIAL" -> viewModel.cargarHistorial()
            "COMPROBANTES" -> viewModel.cargarComprobantes()
            "PERFIL", "EDITAR_PERFIL" -> viewModel.cargarPerfil()
            "NOTIFICACIONES" -> viewModel.cargarNotificaciones()
        }
    }

    LaunchedEffect(currentView) { cargarActual() }
    // Recarga el perfil al entrar al panel: corrige sesión anterior en el mismo proceso.
    LaunchedEffect(Unit) { viewModel.cargarPerfil() }

    // El botón atrás del sistema retrocede dentro del panel en vez de no hacer nada.
    BackHandler(enabled = currentView != "DASHBOARD") {
        currentView = when (currentView) {
            "EDITAR_PERFIL", "VERIFICACION_2FA" -> "PERFIL"
            else -> "DASHBOARD"
        }
    }

    val resumen by viewModel.resumen.collectAsState()
    val historial by viewModel.historial.collectAsState()
    val comprobantes by viewModel.comprobantes.collectAsState()
    val perfil by viewModel.perfil.collectAsState()
    val notificaciones by viewModel.notificaciones.collectAsState()

    val noLeidas = (notificaciones as? EstadoCarga.Success<List<Notificacion>>)?.datos
        ?.count { it.is_read != true } ?: 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(colors.background)
    ) {
        EsferasBrillo(isDark = isDark)

        Column(modifier = Modifier.fillMaxSize()) {
            BarraAprendiz(
                onLogout = onCerrarSesion,
                onPerfil = { currentView = "PERFIL" },
                onEditarPerfil = { currentView = "EDITAR_PERFIL" },

                onNotificaciones = { currentView = "NOTIFICACIONES" },
                noLeidas = noLeidas,
                isDark = isDark,
                onToggleTheme = onToggleTheme
            )

            PullToRefreshBox(
                isRefreshing = refrescando,
                onRefresh = {
                    scope.launch {
                        refrescando = true
                        cargarActual()
                        delay(600)
                        refrescando = false
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = EspaciadoSena.screenH,
                        end = EspaciadoSena.screenH,
                        top = EspaciadoSena.screenV,
                        bottom = EspaciadoSena.dockClearance
                    )
            ) {
                when (currentView) {
                    "DASHBOARD" -> VistaResumen(
                        estado = resumen,
                        historialEstado = historial,
                        perfilEstado = perfil,
                        onReintentar = viewModel::cargarResumen,
                        onVerHistorial = { currentView = "HISTORIAL" }
                    )
                    "HISTORIAL" -> VistaHistorial(historial, onReintentar = viewModel::cargarHistorial)
                    "COMPROBANTES" -> VistaComprobantes(comprobantes, onReintentar = viewModel::cargarComprobantes)
                    "MIS_EXCUSAS" -> VistaMisExcusas(onBack = { currentView = "DASHBOARD" })
                    "PERFIL" -> PerfilAprendizView(
                        perfil,
                        onBack = { currentView = "DASHBOARD" },
                        onReintentar = viewModel::cargarPerfil,
                        onEditar = { currentView = "EDITAR_PERFIL" },
                        onConfigurar2Fa = { currentView = "VERIFICACION_2FA" }
                    )
                    "VERIFICACION_2FA" -> VistaConfigDobleFactor(onBack = { currentView = "PERFIL" })
                    "EDITAR_PERFIL" -> VistaEditarPerfil(
                        estado = perfil,
                        onBack = { currentView = "PERFIL" },
                        onGuardado = {
                            currentView = "PERFIL"
                            viewModel.cargarPerfil()
                        },
                        onReintentar = viewModel::cargarPerfil
                    )
                    "NOTIFICACIONES" -> VistaNotificaciones(
                        estado = notificaciones,
                        onReintentar = viewModel::cargarNotificaciones,
                        onMarcarLeida = viewModel::marcarLeida,
                        onMarcarTodasLeidas = viewModel::marcarTodasLeidas,
                        onBack = { currentView = "DASHBOARD" }
                    )

                }
            }
        }

        BarraNavegacion(
            items = listOf(
                ElementoNavegacion("DASHBOARD", Icons.Default.Home, "Inicio"),
                ElementoNavegacion("MIS_EXCUSAS", Icons.Default.Assignment, "Excusas"),
                ElementoNavegacion("HISTORIAL", Icons.Default.History, "Historial"),
                ElementoNavegacion("COMPROBANTES", Icons.Default.Devices, "Equipos")
            ),
            selectedKey = claveNavegacion(currentView, "DASHBOARD"),
            onSelect = { currentView = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        VistaPendientesDobleFactor()
    }
}

@Composable
fun BarraAprendiz(
    onLogout: () -> Unit,
    onPerfil: (() -> Unit)? = null,
    onEditarPerfil: (() -> Unit)? = null,

    onNotificaciones: (() -> Unit)? = null,
    noLeidas: Int = 0,
    isDark: Boolean = true,
    onToggleTheme: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    val colors = ColoresAppLocal.current
    val nombre = GestorSesion.userName ?: "Usuario"
    val email = GestorSesion.userEmail ?: ""

    BarraSuperiorVidrio {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("SENA ", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("ACCESS", color = verdeMarca(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.border(1.dp, VerdeSena.copy(0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("APRENDIZ", color = verdeMarca(), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onNotificaciones != null) {
                    Box {
                        IconButton(onClick = onNotificaciones) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Notificaciones",
                                tint = colors.textPrimary
                            )
                        }
                        if (noLeidas > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(RojoError),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (noLeidas > 99) "99+" else noLeidas.toString(),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                BotonCambiarTema(isDark = isDark, onToggleTheme = onToggleTheme)
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.Menu, null, tint = colors.textPrimary)
                    }
                    MenuDesplegableVidrio(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.clip(CircleShape).clickable {
                                        showMenu = false
                                        (onEditarPerfil ?: onPerfil)?.invoke()
                                    }
                                ) {
                                    FotoPerfil(fotoPath = GestorSesion.userPhoto, nombre = nombre, tamano = 48.dp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(nombre, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(email, color = colors.textSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                        HorizontalDivider(color = colors.border)
                        if (onPerfil != null) {
                            DropdownMenuItem(
                                text = { Text("Perfil", color = colors.textPrimary) },
                                leadingIcon = { Icon(Icons.Default.Person, null, tint = verdeMarca()) },
                                onClick = { showMenu = false; onPerfil() }
                            )
                        }

                        DropdownMenuItem(
                            text = { Text("Cerrar sesion", color = Color.Red) },
                            leadingIcon = { Icon(Icons.Default.Logout, null, tint = Color.Red) },
                            onClick = { showMenu = false; onLogout() }
                        )
                    }
                }
            }
    }
}

// --- VISTA 1: DASHBOARD (RESUMEN) ---
@Composable
fun VistaResumen(
    estado: EstadoCarga<ResumenAprendiz>,
    historialEstado: EstadoCarga<List<Ingreso>> = EstadoCarga.Success(emptyList()),
    perfilEstado: EstadoCarga<UsuarioApi> = EstadoCarga.Loading,
    onReintentar: () -> Unit,
    onVerHistorial: () -> Unit = {}
) {
    val colors = ColoresAppLocal.current
    val scrollState = rememberScrollState()
    val perfilUsuario = (perfilEstado as? EstadoCarga.Success<UsuarioApi>)?.datos
    val nombreBienvenida = perfilUsuario?.nombreCompleto
        ?: GestorSesion.userName ?: DatosSimulados.aprendizDemo.nombreCompleto
    val fotoPath = perfilUsuario?.profile_photo_path
    val ficha = perfilUsuario?.user_coursenumber
    val programa = perfilUsuario?.user_program

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        CabeceraPlegable(
            title = "Dashboard Aprendiz",
            subtitle = "Bienvenido, $nombreBienvenida",
            scrollOffset = scrollState.value.toFloat()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TarjetaBienvenida(
            fotoPath = fotoPath,
            nombre = nombreBienvenida,
            rol = "APRENDIZ",
            ficha = ficha,
            programa = programa
        )

        Spacer(modifier = Modifier.height(12.dp))

        Spacer(modifier = Modifier.height(8.dp))

        TituloSeccion(titulo = "TU ESTADO ACTUAL")
        Spacer(modifier = Modifier.height(8.dp))
        when (historialEstado) {
            is EstadoCarga.Loading -> CajaCargando()
            is EstadoCarga.Error -> CajaError(historialEstado.mensaje, onReintentar)
            is EstadoCarga.Success -> {
                val lista = historialEstado.datos
                if (lista.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().superficieVidrio(cornerRadius = RadioVidrio).padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Info, null, tint = colors.textSecondary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Sin movimientos recientes", color = colors.textSecondary, fontSize = 13.sp)
                        }
                    }
                } else {
                    TarjetaEstadoAcceso(ingreso = lista.first())
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        TituloSeccion(titulo = "ACTIVIDAD RECIENTE", accionTexto = "Ver todo", onAccion = onVerHistorial)
        Spacer(modifier = Modifier.height(8.dp))
        when (historialEstado) {
            is EstadoCarga.Loading -> CajaCargando()
            is EstadoCarga.Error -> CajaError(historialEstado.mensaje, onReintentar)
            is EstadoCarga.Success -> {
                val lista = historialEstado.datos
                if (lista.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().superficieVidrio(cornerRadius = RadioVidrio).padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Aún no tienes ingresos registrados", color = colors.textSecondary, fontSize = 13.sp)
                    }
                } else {
                    lista.take(3).forEach { item ->
                        TarjetaActividad(item)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

// --- VISTA 2: HISTORIAL DE ACCESOS ---
@Composable
fun VistaHistorial(estado: EstadoCarga<List<Ingreso>>, onReintentar: () -> Unit) {
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        CabeceraPlegable(
            title = "Historial",
            subtitle = "Tus entradas y salidas por día",
            scrollOffset = listState.firstVisibleItemScrollOffset.toFloat()
        )
        ContenedorTabla(title = "Mi Historial de Accesos", subtitle = "Últimos 3 días • calendario para otro día") {
            EstadoContenido(estado = estado, onReintentar = onReintentar) { items ->
                HistorialPorDias(items = items, mostrarUsuario = false, textoVacio = "Aún no tienes ingresos registrados")
            }
        }
    }
}

// --- VISTA 3: COMPROBANTES DE EQUIPO (SOLO LECTURA) ---
@Composable
fun VistaComprobantes(
    estado: EstadoCarga<List<EquipoIngreso>>,
    onReintentar: () -> Unit
) {
    val colors = ColoresAppLocal.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        CabeceraPlegable(
            title = "Comprobantes",
            subtitle = "Registros de tus dispositivos ingresados al centro",
            scrollOffset = scrollState.value.toFloat()
        )
        Spacer(modifier = Modifier.height(16.dp))
        ContenedorTabla(title = "Mis Comprobantes de Equipo", subtitle = "Registros de tus dispositivos ingresados al centro") {
            EstadoContenido(estado = estado, onReintentar = onReintentar) { items ->
                if (items.isEmpty()) {
                    VistaVacia(
                        icono = Icons.Default.Devices,
                        titulo = "No tienes equipos registrados",
                        mensaje = "Cuando el administrador registre un equipo a tu nombre, lo verás aquí."
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(EspaciadoSena.sm)) {
                        items.forEach { eq ->
                            TarjetaSena {
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    CeldaSena(texto = eq.equipo_type ?: "Equipo", peso = 1.1f)
                                    CeldaSena(texto = eq.marcaModelo, peso = 1.6f)
                                }
                                Spacer(modifier = Modifier.height(EspaciadoSena.xs))
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    CeldaSena(texto = "Serial: ${eq.equipo_serial ?: "—"}", peso = 1.6f)
                                    CeldaSena(texto = fechaLegible(eq.entry_datetime), peso = 1.2f)
                                    InsigniaSena(texto = "INGRESADO", tipo = TipoInsignia.EXITO, modifier = Modifier.weight(0.9f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- COMPONENTES DASHBOARD (banner, estado, previews) ---
@Composable
fun TarjetaBienvenida(
    fotoPath: String?,
    nombre: String,
    rol: String,
    ficha: Int?,
    programa: String?
) {
    val colors = ColoresAppLocal.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .superficieVidrio(cornerRadius = RadioVidrio)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FotoPerfil(fotoPath = fotoPath, nombre = nombre, tamano = 52.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Hola,", color = colors.textSecondary, fontSize = 11.sp)
                Text(nombre, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                val detalle = buildString {
                    if (ficha != null && ficha > 0) append("Ficha $ficha")
                    if (!programa.isNullOrBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append(programa)
                    }
                }
                if (detalle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(detalle, color = colors.textSecondary, fontSize = 11.sp, maxLines = 2)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .border(1.dp, VerdeSena.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .background(VerdeSena.copy(alpha = 0.12f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(rol, color = verdeMarca(), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TituloSeccion(titulo: String, accionTexto: String? = null, onAccion: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(titulo, color = verdeMarca(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        if (accionTexto != null && onAccion != null) {
            Text(
                accionTexto,
                color = verdeMarca(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onAccion)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun TarjetaEstadoAcceso(ingreso: Ingreso) {
    val colors = ColoresAppLocal.current
    val esSalida = ingreso.ingreso_type.equals("Salida", ignoreCase = true)
    val estaDentro = !esSalida
    val colorEstado = if (estaDentro) verdeMarca() else NaranjaAmbar
    val bgEstado = if (estaDentro) verdeMarca().copy(alpha = 0.15f) else NaranjaAmbar.copy(alpha = 0.15f)
    val icono = if (estaDentro) Icons.Default.Login else Icons.Default.Logout
    val titulo = if (estaDentro) "Estás dentro del centro" else "Estás fuera del centro"
    val subtitulo = "${fechaLegible(ingreso.ingreso_datetime)} • ${ingreso.ingreso_place ?: "CCyS"}"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .superficieVidrio(cornerRadius = RadioVidrio)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(bgEstado),
                contentAlignment = Alignment.Center
            ) {
                Icon(icono, null, tint = colorEstado, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitulo, color = colors.textSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(fechaRelativa(ingreso.ingreso_datetime), color = colorEstado, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .border(1.dp, colorEstado.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(if (estaDentro) "DENTRO" else "FUERA", color = colorEstado, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TarjetaActividad(item: Ingreso) {
    val colors = ColoresAppLocal.current
    val esSalida = item.ingreso_type.equals("Salida", ignoreCase = true)
    val colorTipo = if (esSalida) NaranjaAmbar else verdeMarca()
    val icono = if (esSalida) Icons.Default.Logout else Icons.Default.Login
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .superficieVidrio(cornerRadius = 16.dp)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(colorTipo.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icono, null, tint = colorTipo, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(if (esSalida) "Salida" else "Entrada", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(item.ingreso_place ?: "CCyS", color = colors.textSecondary, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(horaCorta(item.ingreso_datetime), color = colorTipo, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(fechaRelativa(item.ingreso_datetime), color = colors.textSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun TarjetaEquipoDashboard(eq: EquipoIngreso) {
    val colors = ColoresAppLocal.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .superficieVidrio(cornerRadius = 16.dp)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(VerdeSena.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Devices, null, tint = verdeMarca(), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(eq.equipo_type ?: "Equipo", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(eq.marcaModelo, color = colors.textSecondary, fontSize = 11.sp, maxLines = 1)
                Text(eq.equipo_serial ?: "—", color = colors.textSecondary, fontSize = 11.sp)
            }
            InsigniaSena(texto = "Ingresado", tipo = TipoInsignia.EXITO)
        }
    }
}

// --- COMPONENTES REUTILIZABLES ---

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier) {
    val colors = ColoresAppLocal.current
    Box(
        modifier = modifier
            .escalaPresion(pressedScale = 0.96f)
            .superficieVidrio(cornerRadius = RadioVidrio)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = verdeMarca(), modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(label, color = colors.textSecondary, fontSize = 12.sp)
                Text(value, color = colors.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ContenedorTabla(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = ColoresAppLocal.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .superficieVidrio(cornerRadius = RadioVidrio)
            .padding(EspaciadoSena.md)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.subtitleText)
        Spacer(modifier = Modifier.height(EspaciadoSena.sm))
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
fun PerfilAprendizView(estado: EstadoCarga<UsuarioApi>, onBack: () -> Unit, onReintentar: () -> Unit, onEditar: () -> Unit, onConfigurar2Fa: () -> Unit) {
    val colors = ColoresAppLocal.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .imePadding()
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = colors.textPrimary) }
        Spacer(modifier = Modifier.height(8.dp))

        EstadoContenido(estado = estado, onReintentar = onReintentar) { usuario ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .superficieVidrio(cornerRadius = RadioVidrio)
                    .padding(20.dp)
            ) {
                CabeceraPerfil(
                    fotoPath = usuario.profile_photo_path,
                    nombre = usuario.nombreCompleto,
                    rol = "Aprendiz"
                )
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = colors.border)
                Spacer(modifier = Modifier.height(4.dp))
                FilaDato(Icons.Default.Email, "Correo", usuario.user_email ?: "—")
                FilaDato(Icons.Default.Badge, "Documento", usuario.user_identification ?: "—")
                if (usuario.user_coursenumber != null) {
                    FilaDato(Icons.Default.Numbers, "Ficha", usuario.user_coursenumber.toString())
                }
                if (!usuario.user_program.isNullOrBlank()) {
                    FilaDato(Icons.Default.School, "Programa", usuario.user_program!!)
                }
                Spacer(modifier = Modifier.height(16.dp))
                BotonPrimarioNeon(
                    text = "EDITAR PERFIL",
                    icon = Icons.Default.Edit,
                    onClick = onEditar,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                BotonBordeBrillante(
                    text = "VERIFICACIÓN EN DOS PASOS",
                    icon = Icons.Default.Shield,
                    onClick = onConfigurar2Fa,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            MiSeccionHuella()
            Spacer(modifier = Modifier.imePadding().height(96.dp))
        }
    }
}
