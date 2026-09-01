package com.example.sennaccess.aprendiz

// Pantalla principal del Instructor: orquesta las 4 vistas del dashboard
// (Resumen, Control de Ingresos, Equipos y Perfil) con barra superior de
// vidrio, contenido dinámico y dock flotante, reutilizando los componentes
// StatCard y TableContainer del dashboard del Aprendiz.

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.sennaccess.data.Ingreso
import com.example.sennaccess.data.IngresoEquipo
import com.example.sennaccess.data.Notificacion
import com.example.sennaccess.data.SessionManager
import com.example.sennaccess.data.UsuarioApi
import com.example.sennaccess.data.mock.MockData
import com.example.sennaccess.ambientes.AmbienteDetalleView
import com.example.sennaccess.ambientes.MisAmbientesView
import com.example.sennaccess.data.Ambiente
import com.example.sennaccess.excusas.CrearExcusaView
import com.example.sennaccess.jornada.GenerarQrAulaView
import com.example.sennaccess.ui.AcercaDeView
import com.example.sennaccess.ui.CargaUiState
import com.example.sennaccess.ui.EstadoContenido
import com.example.sennaccess.ui.EstadoVacio
import com.example.sennaccess.ui.FilaDato
import com.example.sennaccess.ui.MiHuellaSection
import com.example.sennaccess.ui.NotificacionesView
import com.example.sennaccess.ui.NovedadesView
import com.example.sennaccess.ui.AvatarPerfil
import com.example.sennaccess.ui.CargandoBox
import com.example.sennaccess.ui.ErrorBox
import com.example.sennaccess.ui.PerfilHeader
import com.example.sennaccess.ui.fechaLegible
import com.example.sennaccess.ui.fechaRelativa
import com.example.sennaccess.ui.horaCorta
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.OrangeAmber
import com.example.sennaccess.ui.theme.SenaGreen
import com.example.sennaccess.ui.theme.ErrorRed
import com.example.sennaccess.ui.ios.GlassDock
import com.example.sennaccess.ui.ios.GlassDockItem
import com.example.sennaccess.ui.ios.GlowSpheres
import com.example.sennaccess.ui.ios.IosCollapsibleHeader
import com.example.sennaccess.ui.ios.IosGlassDropdownMenu
import com.example.sennaccess.ui.ios.IosGlassTopBar
import com.example.sennaccess.ui.ios.glassSurface
import com.example.sennaccess.ui.ios.GlassCornerRadius
import com.example.sennaccess.ui.ios.pressScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructorDashboard(onCerrarSesion: () -> Unit, isDark: Boolean = true, onToggleTheme: () -> Unit = {}) {
    // Pestaña activa; rememberSaveable conserva su valor al girar la pantalla.
    var currentView by rememberSaveable  { mutableStateOf("DASHBOARD") }
    // Ambiente seleccionado dentro de AMBIENTES (gestión de estudiantes/QR).
    var ambienteSeleccionado by remember { mutableStateOf<Ambiente?>(null) }
    var qrAmbiente by remember { mutableStateOf<Ambiente?>(null) }
    var mostrarAutorizar by remember { mutableStateOf(false) }
    val colors = LocalAppColors.current
    val viewModel: InstructorDashboardViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val scope = rememberCoroutineScope()
    // Estado del indicador de pull-to-refresh.
    var refrescando by remember { mutableStateOf(false) }

    // Recarga los datos de la pestaña activa; lo usan el cambio de pestaña y el refresco.
    fun cargarActual() {
        when (currentView) {
            "DASHBOARD" -> viewModel.cargarResumen()
            "NOVEDADES" -> viewModel.cargarNovedades()
            "HISTORIAL" -> viewModel.cargarHistorial()
            "MIS_EQUIPOS" -> viewModel.cargarEquipos()
            "AMBIENTES" -> {}
        }
    }

    LaunchedEffect(currentView) { cargarActual() }

    // Suscripción a los StateFlow del ViewModel: recomponen la vista al cambiar su estado.
    val resumen by viewModel.resumen.collectAsState()
    val historial by viewModel.historial.collectAsState()
    val equipos by viewModel.equipos.collectAsState()
    val perfil by viewModel.perfil.collectAsState()
    val novedades by viewModel.novedades.collectAsState()
    val notificaciones by viewModel.notificaciones.collectAsState()

    // No leídas para el badge de la campana (0 si el estado no trae datos).
    val noLeidas = (notificaciones as? CargaUiState.Success<List<Notificacion>>)?.datos
        ?.count { it.is_read != true } ?: 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(colors.background)
    ) {
        Image(
            painter = rememberAsyncImagePainter("https://www.sena.edu.co/Style%20Library/alayout/images/pattern.png"),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().graphicsLayer(alpha = 0.15f),
            contentScale = ContentScale.Crop
        )

        GlowSpheres(isDark = isDark)

        Column(modifier = Modifier.fillMaxSize()) {
            InstructorTopBar(
                onLogout = onCerrarSesion,
                onPerfil = { currentView = "PERFIL" },
                onEditarPerfil = { currentView = "EDITAR_PERFIL" },
                onAcercaDe = { currentView = "ACERCA_DE" },
                onNotificaciones = { currentView = "NOTIFICACIONES" },
                noLeidas = noLeidas,
                isDark = isDark,
                onToggleTheme = onToggleTheme
            )

            // Contenido envuelto en PullToRefreshBox: deslizar hacia abajo recarga la pestaña.
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
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp)
            ) {
                // Renderiza únicamente la vista de la pestaña seleccionada; cada
                // vista recibe su CargaUiState y la acción de reintento del ViewModel.
                when (currentView) {
                    "DASHBOARD" -> InstructorResumenView(
                        estado = resumen,
                        historialEstado = historial,
                        perfilEstado = perfil,
                        onReintentar = viewModel::cargarResumen,
                        onVerHistorial = { currentView = "HISTORIAL" }
                    )
                    "AMBIENTES" -> {
                        when {
                            qrAmbiente != null -> GenerarQrAulaView(onBack = { qrAmbiente = null }, ambienteIdInicial = qrAmbiente?.id_ambiente)
                            mostrarAutorizar -> CrearExcusaView(onBack = { mostrarAutorizar = false }, ambienteIdInicial = ambienteSeleccionado?.id_ambiente)
                            ambienteSeleccionado != null -> AmbienteDetalleView(
                                ambiente = ambienteSeleccionado!!,
                                onBack = { ambienteSeleccionado = null },
                                onProyectarQr = { amb -> qrAmbiente = amb },
                                onAutorizarSalida = { mostrarAutorizar = true }
                            )
                            else -> MisAmbientesView(onAmbienteClick = { ambienteSeleccionado = it })
                        }
                    }
                    "NOVEDADES" -> NovedadesView(estado = novedades, onReintentar = viewModel::cargarNovedades)
                    "HISTORIAL" -> HistorialIngresosView(historial, onReintentar = viewModel::cargarHistorial)
                    "MIS_EQUIPOS" -> MisEquiposView(equipos, onReintentar = viewModel::cargarEquipos)
                    "PERFIL" -> PerfilInstructorView(
                        perfil,
                        onBack = { currentView = "DASHBOARD" },
                        onReintentar = viewModel::cargarPerfil,
                        onEditar = { currentView = "EDITAR_PERFIL" }
                    )
                    "EDITAR_PERFIL" -> EditarPerfilView(
                        estado = perfil,
                        onBack = { currentView = "PERFIL" },
                        onGuardado = {
                            currentView = "PERFIL"
                            viewModel.cargarPerfil()
                        },
                        onReintentar = viewModel::cargarPerfil,
                        mostrarFichaPrograma = false
                    )
                    "NOTIFICACIONES" -> NotificacionesView(
                        estado = notificaciones,
                        onReintentar = viewModel::cargarNotificaciones,
                        onMarcarLeida = viewModel::marcarLeida,
                        onMarcarTodasLeidas = viewModel::marcarTodasLeidas,
                        onBack = { currentView = "DASHBOARD" }
                    )
                    "ACERCA_DE" -> AcercaDeView(onBack = { currentView = "DASHBOARD" })
                }
            }
        }

        // Dock de navegación: marca la pestaña activa (selectedKey) y, al pulsar
        // una opción, actualiza currentView para cambiar de vista.
        GlassDock(
            items = listOf(
                GlassDockItem("DASHBOARD", Icons.Default.Home, "Inicio"),
                GlassDockItem("AMBIENTES", Icons.Default.MeetingRoom, "Ambientes"),
                GlassDockItem("NOVEDADES", Icons.Default.ReportProblem, "Novedades"),
                GlassDockItem("HISTORIAL", Icons.Default.History, "Historial"),
                GlassDockItem("MIS_EQUIPOS", Icons.Default.Devices, "Equipos")
            ),
            selectedKey = currentView,
            onSelect = { currentView = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// Barra superior de vidrio: identidad SENA ACCESS con el rol, alternancia de
// tema y menú de acciones (Perfil / Cerrar sesión).
@Composable
fun InstructorTopBar(
    onLogout: () -> Unit,
    onPerfil: (() -> Unit)? = null,
    onEditarPerfil: (() -> Unit)? = null,
    onAcercaDe: (() -> Unit)? = null,
    onNotificaciones: (() -> Unit)? = null,
    noLeidas: Int = 0,
    isDark: Boolean,
    onToggleTheme: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val colors = LocalAppColors.current
    // Datos del usuario tomados de la sesión, con un perfil de ejemplo como respaldo.
    val nombre = SessionManager.userName ?: MockData.instructorDemo.nombreCompleto
    val email = SessionManager.userEmail ?: MockData.instructorDemo.user_email ?: ""

    IosGlassTopBar {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("SENA ", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("ACCESS", color = SenaGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .border(1.dp, SenaGreen.copy(0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "INSTRUCTOR",
                        color = SenaGreen,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Campana de notificaciones con badge del número de no leídas.
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
                                    .background(ErrorRed),
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
                IconButton(onClick = onToggleTheme) {
                    Icon(
                        if (isDark) Icons.Default.WbSunny else Icons.Default.DarkMode,
                        contentDescription = "Toggle theme",
                        tint = colors.textPrimary
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.Menu, null, tint = colors.textPrimary)
                    }
                    // Menú contextual con los datos del usuario y las acciones
                    // de navegación a Perfil y de cierre de sesión.
                    IosGlassDropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    AvatarPerfil(fotoPath = SessionManager.userPhoto, nombre = nombre, tamano = 48.dp)
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(SenaGreen)
                                            .border(2.dp, colors.cardBackground, CircleShape)
                                            .clickable {
                                                showMenu = false
                                                (onEditarPerfil ?: onPerfil)?.invoke()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
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
                                leadingIcon = { Icon(Icons.Default.Person, null, tint = SenaGreen) },
                                onClick = { showMenu = false; onPerfil() }
                            )
                        }
                        if (onAcercaDe != null) {
                            DropdownMenuItem(
                                text = { Text("Acerca de", color = colors.textPrimary) },
                                leadingIcon = { Icon(Icons.Default.Info, null, tint = SenaGreen) },
                                onClick = { showMenu = false; onAcercaDe() }
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

// Vista 1: resumen del Instructor sin StatCards ni Equipos: bienvenida, estado
// y actividad reciente. Las previews son resúmenes; la funcionalidad
// completa sigue en HISTORIAL, NOVEDADES y MIS_EQUIPOS (dock inferior).
@Composable
fun InstructorResumenView(
    estado: CargaUiState<ResumenInstructor>,
    historialEstado: CargaUiState<List<Ingreso>> = CargaUiState.Success(emptyList()),
    perfilEstado: CargaUiState<UsuarioApi> = CargaUiState.Loading,
    onReintentar: () -> Unit,
    onVerHistorial: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    val scrollState = rememberScrollState()
    // Perfil para el banner de bienvenida.
    val perfilUsuario = (perfilEstado as? CargaUiState.Success<UsuarioApi>)?.datos
    val nombreBienvenida = perfilUsuario?.nombreCompleto
        ?: SessionManager.userName ?: MockData.instructorDemo.nombreCompleto
    val fotoPath = perfilUsuario?.profile_photo_path
    val ficha = perfilUsuario?.user_coursenumber
    val programa = perfilUsuario?.user_program

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        IosCollapsibleHeader(
            title = "Panel de Instructor",
            subtitle = "Bienvenido, $nombreBienvenida",
            scrollOffset = scrollState.value.toFloat()
        )

        Spacer(modifier = Modifier.height(16.dp))

        BienvenidaDashboardCard(
            fotoPath = fotoPath,
            nombre = nombreBienvenida,
            rol = "INSTRUCTOR",
            ficha = ficha,
            programa = programa
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Estado actual (dentro/fuera) del instructor.
        SeccionDashboardTitulo(titulo = "TU ESTADO ACTUAL")
        Spacer(modifier = Modifier.height(8.dp))
        when (historialEstado) {
            is CargaUiState.Loading -> CargandoBox()
            is CargaUiState.Error -> ErrorBox(historialEstado.mensaje, onReintentar)
            is CargaUiState.Success -> {
                val lista = historialEstado.datos
                if (lista.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().glassSurface(cornerRadius = GlassCornerRadius).padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Info, null, tint = colors.textSecondary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Sin movimientos recientes", color = colors.textSecondary, fontSize = 13.sp)
                        }
                    }
                } else {
                    EstadoAccesoDashboardCard(ingreso = lista.first())
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Actividad reciente (preview de HISTORIAL).
        SeccionDashboardTitulo(titulo = "ACTIVIDAD RECIENTE", accionTexto = "Ver todo", onAccion = onVerHistorial)
        Spacer(modifier = Modifier.height(8.dp))
        when (historialEstado) {
            is CargaUiState.Loading -> CargandoBox()
            is CargaUiState.Error -> ErrorBox(historialEstado.mensaje, onReintentar)
            is CargaUiState.Success -> {
                val lista = historialEstado.datos
                if (lista.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().glassSurface(cornerRadius = GlassCornerRadius).padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay ingresos registrados", color = colors.textSecondary, fontSize = 13.sp)
                    }
                } else {
                    lista.take(3).forEach { item ->
                        TarjetaActividadDashboard(item)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

// Vista 2: control de ingresos en TableContainer con usuario, fecha y ubicación;
// muestra mensaje vacío o la lista según el estado de la API.
@Composable
fun HistorialIngresosView(estado: CargaUiState<List<Ingreso>>, onReintentar: () -> Unit) {
    val colors = LocalAppColors.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        IosCollapsibleHeader(
            title = "Control de Ingresos",
            subtitle = "Supervisión general de accesos",
            scrollOffset = scrollState.value.toFloat()
        )
        TableContainer(title = "Control de Ingresos", subtitle = "Supervisión general de accesos") {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Text("USUARIO", modifier = Modifier.width(140.dp), color = colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("FECHA Y HORA", modifier = Modifier.width(150.dp), color = colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("ESTADO", modifier = Modifier.width(90.dp), color = colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("UBICACIÓN", modifier = Modifier.width(90.dp), color = colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = colors.border)

            EstadoContenido(estado = estado, onReintentar = onReintentar) { items ->
                if (items.isEmpty()) {
                    EstadoVacio(
                        icono = Icons.Default.Login,
                        titulo = "No hay ingresos registrados",
                        mensaje = "Los accesos de aprendices e instructores aparecerán aquí."
                    )
                } else {
                    items.forEach { item ->
                        HorizontalDivider(color = colors.border)
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(item.user?.nombreCompleto ?: "Usuario", modifier = Modifier.width(140.dp), color = colors.textPrimary, fontSize = 13.sp)
                            Text(fechaLegible(item.ingreso_datetime), modifier = Modifier.width(150.dp), color = colors.textPrimary, fontSize = 13.sp)
                            val tipo = item.ingreso_type ?: "Entrada"
                            Text("● ${if (tipo.equals("Salida", true)) "SALIDA" else "INGRESO"}", modifier = Modifier.width(90.dp), color = if (tipo.equals("Salida", true)) Color(0xFFE67E22) else SenaGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(item.ingreso_place ?: "—", modifier = Modifier.width(90.dp), color = SenaGreen, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// Vista 3: comprobantes de los equipos del instructor en solo lectura (los
// registra el administrador), también en TableContainer con scroll horizontal.
@Composable
fun MisEquiposView(estado: CargaUiState<List<IngresoEquipo>>, onReintentar: () -> Unit) {
    val colors = LocalAppColors.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        IosCollapsibleHeader(
            title = "Mis Comprobantes",
            subtitle = "Dispositivos del instructor",
            scrollOffset = scrollState.value.toFloat()
        )
        Spacer(modifier = Modifier.height(12.dp))
        TableContainer(title = "Mis Comprobantes", subtitle = "Dispositivos del instructor") {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Text("EQUIPO", modifier = Modifier.width(100.dp), color = colors.textSecondary, fontSize = 12.sp)
                Text("MARCA/MODELO", modifier = Modifier.width(150.dp), color = colors.textSecondary, fontSize = 12.sp)
                Text("SERIAL", modifier = Modifier.width(120.dp), color = colors.textSecondary, fontSize = 12.sp)
            }
            HorizontalDivider(color = colors.border)

            EstadoContenido(estado = estado, onReintentar = onReintentar) { items ->
                if (items.isEmpty()) {
                    EstadoVacio(
                        icono = Icons.Default.Devices,
                        titulo = "No hay equipos registrados",
                        mensaje = "Los equipos registrados a tu nombre aparecerán aquí."
                    )
                } else {
                    items.forEach { eq ->
                        HorizontalDivider(color = colors.border)
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(eq.equipo_type ?: "Equipo", modifier = Modifier.width(100.dp), color = colors.textPrimary, fontSize = 13.sp)
                            Text(eq.marcaModelo, modifier = Modifier.width(150.dp), color = colors.textPrimary, fontSize = 13.sp)
                            Text(eq.equipo_serial ?: "—", modifier = Modifier.width(120.dp), color = colors.textPrimary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// Vista 4: perfil del instructor en una tarjeta de vidrio, con los datos
// obtenidos de la API (o mocks) mediante EstadoContenido.
@Composable
fun PerfilInstructorView(estado: CargaUiState<UsuarioApi>, onBack: () -> Unit, onReintentar: () -> Unit, onEditar: () -> Unit) {
    val colors = LocalAppColors.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = colors.textPrimary) }
        Spacer(modifier = Modifier.height(8.dp))

        EstadoContenido(estado = estado, onReintentar = onReintentar) { usuario ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassSurface(cornerRadius = GlassCornerRadius)
                    .padding(20.dp)
            ) {
                PerfilHeader(
                    fotoPath = usuario.profile_photo_path,
                    nombre = usuario.nombreCompleto,
                    rol = "Instructor"
                )
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = colors.border)
                Spacer(modifier = Modifier.height(4.dp))
                FilaDato(Icons.Default.Email, "Correo", usuario.user_email ?: "—")
                FilaDato(Icons.Default.Badge, "Documento", usuario.user_identification ?: "—")
                if (!usuario.user_program.isNullOrBlank()) {
                    FilaDato(Icons.Default.School, "Programa", usuario.user_program!!)
                }
                if (usuario.user_coursenumber != null && usuario.user_coursenumber > 0) {
                    FilaDato(Icons.Default.Numbers, "Ficha", usuario.user_coursenumber.toString())
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onEditar,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("EDITAR PERFIL", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Gestión de la huella dactilar local: registrar, ver estado y eliminar.
            MiHuellaSection()
        }
    }
}
