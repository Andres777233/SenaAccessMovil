package com.example.sennaccess.admin

// Pantalla contenedora del rol ADMINISTRADOR.
// Orquesta la navegación interna entre las pestañas del dock y las sub-pantallas
// (crear/actualizar usuario y perfil), manteniendo siempre visible la barra
// superior y el dock flotante de vidrio.

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.sennaccess.data.AprendizInstructor
import com.example.sennaccess.data.Notificacion
import com.example.sennaccess.data.Novedad
import com.example.sennaccess.data.UsuarioApi
import com.example.sennaccess.ui.CargaUiState
import com.example.sennaccess.ui.NotificacionesView
import com.example.sennaccess.ui.NovedadesView
import com.example.sennaccess.ui.ios.GlassDock
import com.example.sennaccess.ui.ios.GlassDockItem
import com.example.sennaccess.ui.ios.GlowSpheres
import com.example.sennaccess.ui.theme.LocalAppColors

/**
 * Contenedor principal del rol ADMINISTRADOR.
 *
 * Estructura estilo iOS (igual que aprendiz/instructor):
 *  - Fondo con patrón SENA + GlowSpheres.
 *  - Top bar de vidrio (AdminTopBar) con menú de perfil / cerrar sesión.
 *  - Contenido que cambia según la pestaña activa del dock.
 *  - Dock flotante de vidrio con las 6 funciones: Inicio, Novedades,
 *    Usuarios, Equipos, Asignaciones e Historial.
 *
 * Las sub-pantallas (crear/actualizar usuario, perfil) son estados internos
 * para mantener el dock siempre visible.
 */
@Composable
fun AdminDashboard(
    onCerrarSesion: () -> Unit,
    isDark: Boolean = true,
    onToggleTheme: () -> Unit = {}
) {
    // Estado de navegación interno: la pestaña activa del dock (currentTab)
    // y la sub-pantalla superpuesta (subScreen), que tiene prioridad sobre la pestaña.
    var currentTab by rememberSaveable { mutableStateOf("INICIO") }
    var subScreen by rememberSaveable { mutableStateOf<AdminScreen?>(null) }
    // Usuario seleccionado para edicion; proviene del GET /admin/users de UsuariosViewModel.
    var usuarioAEditar by remember { mutableStateOf<com.example.sennaccess.data.UsuarioApi?>(null) }
    val colors = LocalAppColors.current
    // ViewModel compartido que expone resumen, historial y perfil como
    // CargaUiState; la UI solo observa y reacciona ante carga/error/datos.
    val viewModel: AdminDashboardViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    // Se observan los estados de datos; la UI se recompone
    // automaticamente cuando cualquiera de ellos cambia.
    val resumen by viewModel.resumen.collectAsState()
    val historial by viewModel.historial.collectAsState()
    val perfil by viewModel.perfil.collectAsState()
    val roles by viewModel.roles.collectAsState()
    val usuarios by viewModel.usuarios.collectAsState()
    val equipos by viewModel.equipos.collectAsState()
    val notificaciones by viewModel.notificaciones.collectAsState()
    val novedades by viewModel.novedades.collectAsState()
    val asignaciones by viewModel.asignaciones.collectAsState()

    // No leídas para el badge de la campana (0 si el estado no trae datos).
    val noLeidas = (notificaciones as? CargaUiState.Success<List<Notificacion>>)?.datos
        ?.count { it.is_read != true } ?: 0

    // Al cambiar de pestaña o sub-pantalla se refrescan los datos de esa sección
    // desde la API, para que el admin vea los registros más recientes (equipos,
    // novedades, historial) aunque se hayan creado después de abrir la app.
    LaunchedEffect(currentTab, subScreen) {
        when (subScreen) {
            AdminScreen.EQUIPOS -> viewModel.cargarEquipos()
            AdminScreen.NOTIFICACIONES -> viewModel.cargarNotificaciones()
            AdminScreen.ASIGNACIONES -> { viewModel.cargarAsignaciones(); viewModel.cargarUsuarios() }
            else -> when (currentTab) {
                "INICIO" -> viewModel.cargarResumen()
                "NOVEDADES" -> viewModel.cargarNovedades()
                "HISTORIAL" -> viewModel.cargarHistorial()
            }
        }
    }

    // Instructores disponibles para asignar.
    val instructores = (usuarios as? CargaUiState.Success<List<UsuarioApi>>)?.datos
        ?.filter { it.esRol("Instructor") }
        ?.sortedBy { it.nombreCompleto }
        ?: emptyList()

    // Aprendices disponibles.
    val aprendices = (usuarios as? CargaUiState.Success<List<UsuarioApi>>)?.datos
        ?.filter { it.esRol("Aprendiz") }
        ?.sortedBy { it.nombreCompleto }
        ?: emptyList()

    // Selecciona una pestaña del dock y limpia la sub-pantalla abierta,
    // garantizando que al cambiar de pestaña se vuelva al contenido principal.
    fun irATab(tab: String) {
        currentTab = tab
        subScreen = if (tab == "ASIGNACIONES") AdminScreen.ASIGNACIONES else null
    }

    // Traduce cada destino del admin a una pestaña del dock o a una sub-pantalla.
    val onNavigate: (AdminScreen) -> Unit = { screen ->
        when (screen) {
            // El panel de inicio y los usuarios resuelven a pestañas del dock.
            AdminScreen.PANEL -> { currentTab = "INICIO"; subScreen = null }
            // Los formularios de usuario y el perfil se abren como sub-pantallas superpuestas.
            AdminScreen.CREAR_USUARIO -> subScreen = AdminScreen.CREAR_USUARIO
            AdminScreen.ACTUALIZAR_USUARIO -> subScreen = AdminScreen.ACTUALIZAR_USUARIO
            AdminScreen.PERFIL -> subScreen = AdminScreen.PERFIL
            AdminScreen.USUARIOS -> { currentTab = "USUARIOS"; subScreen = null }
            AdminScreen.REPORTE_NOVEDADES -> { currentTab = "NOVEDADES"; subScreen = null }
            // Los accesos de aprendices/instructores se muestran como sub-pantallas
            // del historial, consumiendo el mismo estado de la API.
            AdminScreen.ACCESO_APRENDICES -> subScreen = AdminScreen.ACCESO_APRENDICES
            AdminScreen.ACCESO_INSTRUCTORES -> subScreen = AdminScreen.ACCESO_INSTRUCTORES
            AdminScreen.EQUIPOS -> subScreen = AdminScreen.EQUIPOS
            AdminScreen.NOTIFICACIONES -> subScreen = AdminScreen.NOTIFICACIONES
            AdminScreen.ASIGNACIONES -> subScreen = AdminScreen.ASIGNACIONES
        }
    }

    // Contenedor raíz: fija el fondo del tema y respeta las barras del sistema.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(colors.background)
    ) {
        // Fondo de marca SENA a baja opacidad, decorativo y detrás de todo el contenido.
        Image(
            painter = rememberAsyncImagePainter("https://www.sena.edu.co/Style%20Library/alayout/images/pattern.png"),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().graphicsLayer(alpha = 0.15f),
            contentScale = ContentScale.Crop
        )

        // Esferas luminosas decorativas que dan el efecto de vidrio al fondo.
        GlowSpheres(isDark = isDark)

        // Columna principal: barra superior fija y área de contenido debajo.
        Column(modifier = Modifier.fillMaxSize()) {
            // Barra superior con acceso a perfil y cierre de sesión; el perfil
            // se abre como sub-pantalla sin cambiar de pestaña.
            AdminTopBar(
                onLogout = onCerrarSesion,
                onNavigate = onNavigate,
                noLeidas = noLeidas,
                isDark = isDark,
                onToggleTheme = onToggleTheme
            )

            // Área central de contenido; deja espacio inferior para que el dock no tape nada.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp)
            ) {
                // Si hay una sub-pantalla activa se muestra con prioridad; si no,
                // se pinta el contenido de la pestaña seleccionada en el dock.
                when (subScreen) {
                    // Sub-pantallas: formulario de nuevo usuario, edicion, perfil y accesos.
                    AdminScreen.CREAR_USUARIO -> CrearUsuarioContent(
                        roles = roles,
                        onReintentarRoles = viewModel::cargarRoles,
                        onNavigate = onNavigate
                    )
                    AdminScreen.ACTUALIZAR_USUARIO -> {
                        usuarioAEditar?.let { usuario ->
                            ActualizarUsuarioContent(
                                usuario = usuario,
                                roles = roles,
                                onReintentarRoles = viewModel::cargarRoles,
                                onNavigate = onNavigate
                            )
                        }
                    }
                    AdminScreen.PERFIL -> PerfilContent(
                        perfil = perfil,
                        onBack = { subScreen = null },
                        onReintentar = viewModel::cargarPerfil
                    )
                    AdminScreen.ACCESO_APRENDICES -> AccesoAprendicesContent(
                        estado = historial,
                        onReintentar = viewModel::cargarHistorial,
                        onBack = { subScreen = null },
                        onNavigate = onNavigate
                    )
                    AdminScreen.ACCESO_INSTRUCTORES -> AccesoInstructoresContent(
                        estado = historial,
                        onReintentar = viewModel::cargarHistorial,
                        onBack = { subScreen = null },
                        onNavigate = onNavigate
                    )
                    AdminScreen.EQUIPOS -> AdminEquiposContent(
                        estado = equipos,
                        onReintentar = viewModel::cargarEquipos,
                        onBack = { subScreen = null }
                    )
                    AdminScreen.NOTIFICACIONES -> NotificacionesView(
                        estado = notificaciones,
                        onReintentar = viewModel::cargarNotificaciones,
                        onMarcarLeida = viewModel::marcarLeida,
                        onMarcarTodasLeidas = viewModel::marcarTodasLeidas,
                        onBack = { subScreen = null }
                    )
                    AdminScreen.ASIGNACIONES -> AsignacionesView(
                        estado = asignaciones,
                        instructores = instructores,
                        aprendices = aprendices,
                        onReintentar = viewModel::cargarAsignaciones,
                        onCrear = { viewModel.crearAsignacion(it) { subScreen = null } },
                        onEliminar = { viewModel.eliminarAsignacion(it) },
                        onBack = { subScreen = null }
                    )
                    else -> when (currentTab) {
                        // INICIO: resumen del día con el historial de ingresos del centro.
                        "INICIO" -> AdminPanelResumen(resumen = resumen, onReintentar = viewModel::cargarResumen)
                        // NOVEDADES: formulario de reporte + listado del centro (compartido con los demás roles).
                        "NOVEDADES" -> NovedadesView(
                            estado = novedades,
                            onReintentar = viewModel::cargarNovedades
                        )
                        // USUARIOS: gestión de instructores/aprendices con edición en línea.
                        "USUARIOS" -> UsuariosContent(
                            onNavigate = onNavigate,
                            onEditarUsuario = { usuario ->
                                // Al pulsar editar en una tarjeta se memoriza el usuario
                                // (del GET /admin/users) y se abre el formulario de actualización.
                                usuarioAEditar = usuario
                                subScreen = AdminScreen.ACTUALIZAR_USUARIO
                            }
                        )
                        // HISTORIAL: registro de accesos del centro separado por rol.
                        "HISTORIAL" -> HistorialAdminContent(
                            historial = historial,
                            onReintentar = viewModel::cargarHistorial,
                            onVerAprendices = { onNavigate(AdminScreen.ACCESO_APRENDICES) },
                            onVerInstructores = { onNavigate(AdminScreen.ACCESO_INSTRUCTORES) }
                        )
                    }
                }
            }
        }

        // Dock flotante de vidrio: define las 6 pestañas del admin y su ícono.
        // La pestaña activa se resalta y al tocar otra se dispara irATab.
        GlassDock(
            items = listOf(
                GlassDockItem("INICIO", Icons.Default.Home, "Inicio"),
                GlassDockItem("NOVEDADES", Icons.Default.WarningAmber, "Novedades"),
                GlassDockItem("USUARIOS", Icons.Default.People, "Usuarios"),
                GlassDockItem("EQUIPOS", Icons.Default.Devices, "Equipos"),
                GlassDockItem("ASIGNACIONES", Icons.Default.SupervisorAccount, "Asignar"),
                GlassDockItem("HISTORIAL", Icons.Default.History, "Historial")
            ),
            selectedKey = currentTab,
            onSelect = { irATab(it) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
