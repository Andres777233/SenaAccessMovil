package com.example.sennaccess.aprendiz

// Pantalla principal del Aprendiz: orquesta las 4 vistas del dashboard
// (Resumen, Historial, Comprobantes, Perfil) sobre un layout
// glassmorphism iOS con barra superior, contenido dinámico y dock flotante.

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.sennaccess.excusas.MisExcusasView
import com.example.sennaccess.ui.AcercaDeView
import com.example.sennaccess.ui.AvatarPerfil
import com.example.sennaccess.ui.CargaUiState
import com.example.sennaccess.ui.CargandoBox
import com.example.sennaccess.ui.ErrorBox
import com.example.sennaccess.ui.EstadoContenido
import com.example.sennaccess.ui.EstadoVacio
import com.example.sennaccess.ui.FilaDato
import com.example.sennaccess.ui.MiHuellaSection
import com.example.sennaccess.ui.NotificacionesView
import com.example.sennaccess.ui.PerfilHeader
import com.example.sennaccess.ui.fechaLegible
import com.example.sennaccess.ui.fechaRelativa
import com.example.sennaccess.ui.horaCorta
import com.example.sennaccess.ui.theme.ErrorRed
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.OrangeAmber
import com.example.sennaccess.ui.theme.SenaGreen
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
fun AprendizDashboard(onCerrarSesion: () -> Unit, isDark: Boolean = true, onToggleTheme: () -> Unit = {}) {
    // Pestaña activa; rememberSaveable conserva su valor al girar la pantalla.
    var currentView by rememberSaveable  { mutableStateOf("DASHBOARD") }
    val colors = LocalAppColors.current
    val viewModel: AprendizDashboardViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val scope = rememberCoroutineScope()
    // Estado del indicador de pull-to-refresh.
    var refrescando by remember { mutableStateOf(false) }

    // Recarga los datos de la pestaña activa; lo usan el cambio de pestaña y el refresco.
    fun cargarActual() {
        when (currentView) {
            "DASHBOARD" -> viewModel.cargarResumen()
            "HISTORIAL" -> viewModel.cargarHistorial()
            "COMPROBANTES" -> viewModel.cargarComprobantes()
        }
    }

    LaunchedEffect(currentView) { cargarActual() }

    // Suscripción a los StateFlow del ViewModel: recomponen la vista al cambiar su estado.
    val resumen by viewModel.resumen.collectAsState()
    val historial by viewModel.historial.collectAsState()
    val comprobantes by viewModel.comprobantes.collectAsState()
    val perfil by viewModel.perfil.collectAsState()
    val notificaciones by viewModel.notificaciones.collectAsState()

    // No leídas para el badge de la campana (0 si el estado no trae datos).
    val noLeidas = (notificaciones as? CargaUiState.Success<List<Notificacion>>)?.datos
        ?.count { it.is_read != true } ?: 0

    // Estructura con dock flotante estilo iOS (no altera currentView ni callbacks)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(colors.background)
    ) {
        // 1. Patrón de fondo
        Image(
            painter = rememberAsyncImagePainter("https://www.sena.edu.co/Style%20Library/alayout/images/pattern.png"),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().graphicsLayer(alpha = 0.15f),
            contentScale = ContentScale.Crop
        )

        // 2. Luces ambientales detrás del vidrio
        GlowSpheres(isDark = isDark)

        Column(modifier = Modifier.fillMaxSize()) {
            // 3. Barra Superior Simplificada
            AprendizTopBar(
                onLogout = onCerrarSesion,
                onPerfil = { currentView = "PERFIL" },
                onEditarPerfil = { currentView = "EDITAR_PERFIL" },
                onAcercaDe = { currentView = "ACERCA_DE" },
                onNotificaciones = { currentView = "NOTIFICACIONES" },
                noLeidas = noLeidas,
                isDark = isDark,
                onToggleTheme = onToggleTheme
            )

            // 4. Contenido Dinámico (padding inferior para no quedar bajo el dock)
            // Envuelto en PullToRefreshBox: deslizar hacia abajo recarga la pestaña activa.
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
                    "DASHBOARD" -> ResumenView(
                        estado = resumen,
                        historialEstado = historial,
                        perfilEstado = perfil,
                        onReintentar = viewModel::cargarResumen,
                        onVerHistorial = { currentView = "HISTORIAL" }
                    )
                    "HISTORIAL" -> HistorialView(historial, onReintentar = viewModel::cargarHistorial)
                    "COMPROBANTES" -> ComprobantesView(comprobantes, onReintentar = viewModel::cargarComprobantes)
                    "MIS_EXCUSAS" -> MisExcusasView(onBack = { currentView = "DASHBOARD" })
                    "PERFIL" -> PerfilAprendizView(
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
                        onReintentar = viewModel::cargarPerfil
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

        // 5. Dock flotante de vidrio estilo iOS
        // Marca la pestaña activa (selectedKey) y actualiza currentView al pulsar una opción.
        GlassDock(
            items = listOf(
                GlassDockItem("DASHBOARD", Icons.Default.Home, "Inicio"),
                GlassDockItem("MIS_EXCUSAS", Icons.Default.Assignment, "Excusas"),
                GlassDockItem("HISTORIAL", Icons.Default.History, "Historial"),
                GlassDockItem("COMPROBANTES", Icons.Default.Devices, "Equipos")
            ),
            selectedKey = currentView,
            onSelect = { currentView = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// Barra superior: identidad SENA ACCESS con el rol del usuario,
// alternancia de tema oscuro/claro y menú de acciones (Perfil / Cerrar sesión).
@Composable
fun AprendizTopBar(
    onLogout: () -> Unit,
    onPerfil: (() -> Unit)? = null,
    onEditarPerfil: (() -> Unit)? = null,
    onAcercaDe: (() -> Unit)? = null,
    onNotificaciones: (() -> Unit)? = null,
    noLeidas: Int = 0,
    isDark: Boolean = true,
    onToggleTheme: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    val colors = LocalAppColors.current
    // Datos del usuario tomados de la sesión, con un perfil de ejemplo como respaldo.
    val nombre = SessionManager.userName ?: MockData.aprendizDemo.nombreCompleto
    val email = SessionManager.userEmail ?: MockData.aprendizDemo.user_email ?: ""

    IosGlassTopBar {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("SENA ", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("ACCESS", color = SenaGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.border(1.dp, SenaGreen.copy(0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("APRENDIZ", color = SenaGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
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
                // Alternador de tema claro/oscuro (el ícono cambia según el estado actual).
                IconButton(onClick = onToggleTheme) {
                    Icon(
                        if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = null,
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

// --- VISTA 1: DASHBOARD (RESUMEN) ---
// Dashboard del Aprendiz: banner bienvenida + estado dentro/fuera + actividad
// reciente. Sin StatCards ni sección Equipos (a petición). El resto sigue
// en HISTORIAL y COMPROBANTES (dock inferior).
@Composable
fun ResumenView(
    estado: CargaUiState<ResumenAprendiz>,
    historialEstado: CargaUiState<List<Ingreso>> = CargaUiState.Success(emptyList()),
    perfilEstado: CargaUiState<UsuarioApi> = CargaUiState.Loading,
    onReintentar: () -> Unit,
    onVerHistorial: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    val scrollState = rememberScrollState()
    // Datos de perfil para el banner de bienvenida (fallback a sesión/mocks).
    val perfilUsuario = (perfilEstado as? CargaUiState.Success<UsuarioApi>)?.datos
    val nombreBienvenida = perfilUsuario?.nombreCompleto
        ?: SessionManager.userName ?: MockData.aprendizDemo.nombreCompleto
    val fotoPath = perfilUsuario?.profile_photo_path
    val ficha = perfilUsuario?.user_coursenumber
    val programa = perfilUsuario?.user_program

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        IosCollapsibleHeader(
            title = "Dashboard Aprendiz",
            subtitle = "Bienvenido, $nombreBienvenida",
            scrollOffset = scrollState.value.toFloat()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Banner de bienvenida con avatar y ficha/programa.
        BienvenidaDashboardCard(
            fotoPath = fotoPath,
            nombre = nombreBienvenida,
            rol = "APRENDIZ",
            ficha = ficha,
            programa = programa
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Estado actual (dentro/fuera) derivado del último ingreso.
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
                        Text("Aún no tienes ingresos registrados", color = colors.textSecondary, fontSize = 13.sp)
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

// --- VISTA 2: HISTORIAL DE ACCESOS ---
// Tabla de ingresos del aprendiz dentro de TableContainer; muestra un mensaje
// cuando está vacía o la lista con fecha, ubicación y tipo de acceso.
@Composable
fun HistorialView(estado: CargaUiState<List<Ingreso>>, onReintentar: () -> Unit) {
    val colors = LocalAppColors.current
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        IosCollapsibleHeader(
            title = "Historial",
            subtitle = "Registros de tus ingresos al centro de formación",
            scrollOffset = listState.firstVisibleItemScrollOffset.toFloat()
        )
        TableContainer(title = "Mi Historial de Accesos", subtitle = "Registros de tus ingresos al centro de formación") {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Text("FECHA Y HORA", modifier = Modifier.width(180.dp), color = colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("UBICACIÓN / PUNTO", modifier = Modifier.width(150.dp), color = colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("ESTADO", modifier = Modifier.width(120.dp), color = colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            EstadoContenido(estado = estado, onReintentar = onReintentar) { items ->
                if (items.isEmpty()) {
                    EstadoVacio(
                        icono = Icons.Default.History,
                        titulo = "Aún no tienes ingresos registrados",
                        mensaje = "Tus entradas y salidas del centro aparecerán aquí."
                    )
                } else {
                    LazyColumn(state = listState) {
                        items(items) { item ->
                            HorizontalDivider(color = colors.border)
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(fechaLegible(item.ingreso_datetime), modifier = Modifier.width(180.dp), color = colors.textPrimary, fontSize = 13.sp)
                                Box(modifier = Modifier.width(150.dp)) {
                                    Text(item.ingreso_place ?: "CCyS", color = SenaGreen, modifier = Modifier.border(1.dp, SenaGreen.copy(0.3f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 11.sp)
                                }
                                val tipo = item.ingreso_type ?: "Entrada"
                                Text("● ${if (tipo.equals("Salida", true)) "SALIDA" else "INGRESADO"}", modifier = Modifier.width(120.dp), color = if (tipo.equals("Salida", true)) OrangeAmber else SenaGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- VISTA 3: COMPROBANTES DE EQUIPO (SOLO LECTURA) ---
// Tabla con los equipos del aprendiz (los registra el administrador); distingue
// la lista vacía del caso con datos reales o de error.
@Composable
fun ComprobantesView(
    estado: CargaUiState<List<IngresoEquipo>>,
    onReintentar: () -> Unit
) {
    val colors = LocalAppColors.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        IosCollapsibleHeader(
            title = "Comprobantes",
            subtitle = "Registros de tus dispositivos ingresados al centro",
            scrollOffset = scrollState.value.toFloat()
        )
        Spacer(modifier = Modifier.height(16.dp))
        TableContainer(title = "Mis Comprobantes de Equipo", subtitle = "Registros de tus dispositivos ingresados al centro") {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Text("EQUIPO", modifier = Modifier.width(100.dp), color = colors.textSecondary, fontSize = 12.sp)
                Text("MARCA/MODELO", modifier = Modifier.width(150.dp), color = colors.textSecondary, fontSize = 12.sp)
                Text("SERIAL", modifier = Modifier.width(120.dp), color = colors.textSecondary, fontSize = 12.sp)
                Text("FECHA DE INGRESO", modifier = Modifier.width(160.dp), color = colors.textSecondary, fontSize = 12.sp)
                Text("ESTADO", modifier = Modifier.width(100.dp), color = colors.textSecondary, fontSize = 12.sp)
            }
            HorizontalDivider(color = colors.border)

            EstadoContenido(estado = estado, onReintentar = onReintentar) { items ->
                if (items.isEmpty()) {
                    EstadoVacio(
                        icono = Icons.Default.Devices,
                        titulo = "No tienes equipos registrados",
                        mensaje = "Cuando el administrador registre un equipo a tu nombre, lo verás aquí."
                    )
                } else {
                    items.forEach { eq ->
                        HorizontalDivider(color = colors.border)
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(eq.equipo_type ?: "Equipo", modifier = Modifier.width(100.dp), color = colors.textPrimary, fontSize = 13.sp)
                            Text(eq.marcaModelo, modifier = Modifier.width(150.dp), color = colors.textPrimary, fontSize = 13.sp)
                            Text(eq.equipo_serial ?: "—", modifier = Modifier.width(120.dp), color = colors.textPrimary, fontSize = 13.sp)
                            Text(fechaLegible(eq.entry_datetime), modifier = Modifier.width(160.dp), color = colors.textPrimary, fontSize = 13.sp)
                            Text("● INGRESADO", modifier = Modifier.width(100.dp), color = SenaGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// --- COMPONENTES DASHBOARD (banner, estado, previews) ---
// Banner de bienvenida con avatar y ficha/programa; compartido con Instructor.
@Composable
fun BienvenidaDashboardCard(
    fotoPath: String?,
    nombre: String,
    rol: String,
    ficha: Int?,
    programa: String?
) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = GlassCornerRadius)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarPerfil(fotoPath = fotoPath, nombre = nombre, tamano = 52.dp)
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
                    .border(1.dp, SenaGreen.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .background(SenaGreen.copy(alpha = 0.12f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(rol, color = SenaGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Título de sección del dashboard con acción opcional "Ver todo".
@Composable
fun SeccionDashboardTitulo(titulo: String, accionTexto: String? = null, onAccion: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(titulo, color = SenaGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        if (accionTexto != null && onAccion != null) {
            Text(
                accionTexto,
                color = SenaGreen,
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

// Card de estado dentro/fuera derivada del último ingreso.
@Composable
fun EstadoAccesoDashboardCard(ingreso: Ingreso) {
    val colors = LocalAppColors.current
    val esSalida = ingreso.ingreso_type.equals("Salida", ignoreCase = true)
    val estaDentro = !esSalida
    val colorEstado = if (estaDentro) SenaGreen else OrangeAmber
    val bgEstado = if (estaDentro) SenaGreen.copy(alpha = 0.15f) else OrangeAmber.copy(alpha = 0.15f)
    val icono = if (estaDentro) Icons.Default.Login else Icons.Default.Logout
    val titulo = if (estaDentro) "Estás dentro del centro" else "Estás fuera del centro"
    val subtitulo = "${fechaLegible(ingreso.ingreso_datetime)} • ${ingreso.ingreso_place ?: "CCyS"}"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = GlassCornerRadius)
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

// Tarjeta compacta de actividad (ingreso) para el preview.
@Composable
fun TarjetaActividadDashboard(item: Ingreso) {
    val colors = LocalAppColors.current
    val esSalida = item.ingreso_type.equals("Salida", ignoreCase = true)
    val colorTipo = if (esSalida) OrangeAmber else SenaGreen
    val icono = if (esSalida) Icons.Default.Logout else Icons.Default.Login
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = 16.dp)
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

// Tarjeta compacta de equipo para el preview.
@Composable
fun TarjetaEquipoDashboard(eq: IngresoEquipo) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = 16.dp)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(SenaGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Devices, null, tint = SenaGreen, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(eq.equipo_type ?: "Equipo", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(eq.marcaModelo, color = colors.textSecondary, fontSize = 11.sp, maxLines = 1)
                Text(eq.equipo_serial ?: "—", color = colors.textSecondary, fontSize = 11.sp)
            }
            Box(
                modifier = Modifier
                    .border(1.dp, SenaGreen.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text("● INGRESADO", color = SenaGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- COMPONENTES REUTILIZABLES ---

// Tarjeta reutilizable con icono, etiqueta y valor; compartida con el dashboard del Instructor.
@Composable
fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier) {
    val colors = LocalAppColors.current
    Box(
        modifier = modifier
            .pressScale(pressedScale = 0.96f)
            .glassSurface(cornerRadius = GlassCornerRadius)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = SenaGreen, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(label, color = colors.textSecondary, fontSize = 12.sp)
                Text(value, color = colors.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Contenedor de tabla con glassmorphism y scroll horizontal para listas anchas; compartido entre roles.
@Composable
fun TableContainer(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = GlassCornerRadius)
            .padding(20.dp)
    ) {
        Text(title, color = colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = colors.subtitleText, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(20.dp))

        Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Column(modifier = Modifier.widthIn(min = 600.dp)) {
                content()
            }
        }
    }
}

// Vista 4: perfil del aprendiz en una tarjeta de vidrio, con datos personales
// y de ficha obtenidos de la API (o mocks) mediante EstadoContenido.
@Composable
fun PerfilAprendizView(estado: CargaUiState<UsuarioApi>, onBack: () -> Unit, onReintentar: () -> Unit, onEditar: () -> Unit) {
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
