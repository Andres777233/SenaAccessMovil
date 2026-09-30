package com.example.sennaccess.administrador.panel

// Pantalla contenedora del rol ADMINISTRADOR.
// Orquesta la navegación interna entre las pestañas del dock y las sub-pantallas
// (crear/actualizar usuario y perfil), manteniendo siempre visible la barra
// superior y el dock flotante de vidrio.

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sennaccess.perfil.VistaEditarPerfil
import com.example.sennaccess.autenticacion.invitado.EscanearQrInvitado
import com.example.sennaccess.datos.modelos.Notificacion
import com.example.sennaccess.datos.modelos.Novedad
import com.example.sennaccess.datos.modelos.UsuarioApi
import com.example.sennaccess.administrador.ambientes.VistaAdministrarAmbientes
import com.example.sennaccess.excusas.VistaValidarExcusa
import com.example.sennaccess.comun.EstadoCarga
import com.example.sennaccess.administrador.novedades.VistaNotificaciones
import com.example.sennaccess.administrador.novedades.VistaNovedades
import com.example.sennaccess.administrador.novedades.VistaSugerencias
import com.example.sennaccess.comun.diseno.BarraNavegacion
import com.example.sennaccess.comun.diseno.ElementoNavegacion
import com.example.sennaccess.comun.diseno.EsferasBrillo
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.diseno.EspaciadoSena
import com.example.sennaccess.comun.diseno.claveNavegacion
import com.example.sennaccess.autenticacion.verificacion.VistaConfigDobleFactor
import com.example.sennaccess.autenticacion.verificacion.VistaPendientesDobleFactor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.sennaccess.administrador.accesos.ContenidoAccesoAprendices
import com.example.sennaccess.administrador.accesos.ContenidoAccesoInstructores
import com.example.sennaccess.administrador.usuarios.ContenidoActualizarUsuario
import com.example.sennaccess.administrador.usuarios.ContenidoCrearUsuario
import com.example.sennaccess.administrador.equipos.ContenidoEquiposAdmin
import com.example.sennaccess.administrador.accesos.ContenidoHistorial
import com.example.sennaccess.administrador.usuarios.ContenidoUsuarios
import com.example.sennaccess.administrador.accesos.VistaPersonasDentro

@Composable
fun PanelAdministrador(
    onCerrarSesion: () -> Unit,
    isDark: Boolean = true,
    onToggleTheme: () -> Unit = {}
) {
    var currentTab by rememberSaveable { mutableStateOf("INICIO") }
    var subScreen by rememberSaveable { mutableStateOf<PantallaAdmin?>(null) }
    var editandoPerfil by rememberSaveable { mutableStateOf(false) }
    // Usuario seleccionado para edicion; proviene del GET /admin/users de ModeloUsuarios.
    var usuarioAEditar by remember { mutableStateOf<com.example.sennaccess.datos.modelos.UsuarioApi?>(null) }
    val colors = ColoresAppLocal.current
    val scope = rememberCoroutineScope()
    var refrescando by remember { mutableStateOf(false) }
    // Instancia ligada al usuario en sesión: otro login nunca reutiliza el perfil en memoria.
    val viewModel: PanelAdministradorViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        key = "panel-admin-${com.example.sennaccess.datos.sesion.GestorSesion.userId ?: "anon"}"
    )

    val resumen by viewModel.resumen.collectAsState()
    val historial by viewModel.historial.collectAsState()
    val perfil by viewModel.perfil.collectAsState()
    val roles by viewModel.roles.collectAsState()
    val usuarios by viewModel.usuarios.collectAsState()
    val equipos by viewModel.equipos.collectAsState()
    val notificaciones by viewModel.notificaciones.collectAsState()
    val novedades by viewModel.novedades.collectAsState()
    val presentes by viewModel.presentes.collectAsState()
    val ambientes by viewModel.ambientes.collectAsState()

    val noLeidas = (notificaciones as? EstadoCarga.Success<List<Notificacion>>)?.datos
        ?.count { it.is_read != true } ?: 0

    fun cargarActual() {
        when (subScreen) {
            PantallaAdmin.EQUIPOS -> viewModel.cargarEquipos()
            PantallaAdmin.NOTIFICACIONES -> viewModel.cargarNotificaciones()
            PantallaAdmin.PERFIL -> viewModel.cargarPerfil()
            else -> when (currentTab) {
                "INICIO" -> viewModel.cargarResumen()
                "NOVEDADES" -> viewModel.cargarNovedades()
                "HISTORIAL" -> viewModel.cargarHistorial()
                "PRESENTES" -> viewModel.cargarPresentes()
            }
        }
    }

    LaunchedEffect(currentTab, subScreen) { cargarActual() }
    // Recarga el perfil al entrar al panel: corrige sesión anterior en el mismo proceso.
    LaunchedEffect(Unit) { viewModel.cargarPerfil() }

    // El botón atrás del sistema retrocede dentro del panel en vez de no hacer nada.
    BackHandler(enabled = editandoPerfil || subScreen != null || currentTab != "INICIO") {
        when {
            editandoPerfil -> editandoPerfil = false
            subScreen != null -> subScreen = null
            currentTab != "INICIO" -> { currentTab = "INICIO"; subScreen = null }
        }
    }

    fun irATab(tab: String) {
        currentTab = tab
        editandoPerfil = false
        subScreen = if (tab == "EQUIPOS") PantallaAdmin.EQUIPOS else null
    }

    val onNavigate: (PantallaAdmin) -> Unit = { screen ->
        when (screen) {
            PantallaAdmin.PANEL -> { currentTab = "INICIO"; subScreen = null }
            PantallaAdmin.CREAR_USUARIO -> subScreen = PantallaAdmin.CREAR_USUARIO
            PantallaAdmin.ACTUALIZAR_USUARIO -> subScreen = PantallaAdmin.ACTUALIZAR_USUARIO
            PantallaAdmin.PERFIL -> { subScreen = PantallaAdmin.PERFIL; editandoPerfil = false }
            PantallaAdmin.USUARIOS -> { currentTab = "USUARIOS"; subScreen = null }
            PantallaAdmin.REPORTE_NOVEDADES -> { currentTab = "NOVEDADES"; subScreen = null }
            PantallaAdmin.ACCESO_APRENDICES -> subScreen = PantallaAdmin.ACCESO_APRENDICES
            PantallaAdmin.ACCESO_INSTRUCTORES -> subScreen = PantallaAdmin.ACCESO_INSTRUCTORES
            PantallaAdmin.EQUIPOS -> subScreen = PantallaAdmin.EQUIPOS
            PantallaAdmin.NOTIFICACIONES -> subScreen = PantallaAdmin.NOTIFICACIONES

            PantallaAdmin.AMBIENTES -> subScreen = PantallaAdmin.AMBIENTES
            PantallaAdmin.VALIDAR_EXCUSA -> subScreen = PantallaAdmin.VALIDAR_EXCUSA
            PantallaAdmin.ESCANEAR_QR -> subScreen = PantallaAdmin.ESCANEAR_QR
            PantallaAdmin.VERIFICACION_2FA -> subScreen = PantallaAdmin.VERIFICACION_2FA
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(colors.background)
    ) {
        EsferasBrillo(isDark = isDark)

        Column(modifier = Modifier.fillMaxSize()) {
            BarraSuperiorAdmin(
                onLogout = onCerrarSesion,
                onNavigate = onNavigate,
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
                when (subScreen) {
                    PantallaAdmin.CREAR_USUARIO -> ContenidoCrearUsuario(
                        roles = roles,
                        onReintentarRoles = viewModel::cargarRoles,
                        onNavigate = onNavigate
                    )
                    PantallaAdmin.ACTUALIZAR_USUARIO -> {
                        val usuario = usuarioAEditar
                        if (usuario == null) {
                            // Pantalla restaurada sin usuario: vuelve a la lista en vez de quedar en blanco.
                            LaunchedEffect(Unit) { onNavigate(PantallaAdmin.USUARIOS) }
                        } else {
                            ContenidoActualizarUsuario(
                                usuario = usuario,
                                roles = roles,
                                onReintentarRoles = viewModel::cargarRoles,
                                onNavigate = onNavigate
                            )
                        }
                    }
                    PantallaAdmin.PERFIL -> if (editandoPerfil) {
                        VistaEditarPerfil(
                            estado = perfil,
                            onBack = { editandoPerfil = false },
                            onGuardado = {
                                editandoPerfil = false
                                viewModel.cargarPerfil()
                            },
                            onReintentar = viewModel::cargarPerfil,
                            mostrarFichaPrograma = false
                        )
                    } else {
                        ContenidoPerfilAdmin(
                            perfil = perfil,
                            onBack = { subScreen = null },
                            onReintentar = viewModel::cargarPerfil,
                            onEditar = { editandoPerfil = true },
                            onConfigurar2Fa = { subScreen = PantallaAdmin.VERIFICACION_2FA }
                        )
                    }
                    PantallaAdmin.VERIFICACION_2FA -> VistaConfigDobleFactor(onBack = { subScreen = PantallaAdmin.PERFIL })
                    PantallaAdmin.ACCESO_APRENDICES -> ContenidoAccesoAprendices(
                        estado = historial,
                        onReintentar = viewModel::cargarHistorial,
                        onBack = { subScreen = null },
                        onNavigate = onNavigate
                    )
                    PantallaAdmin.ACCESO_INSTRUCTORES -> ContenidoAccesoInstructores(
                        estado = historial,
                        onReintentar = viewModel::cargarHistorial,
                        onBack = { subScreen = null },
                        onNavigate = onNavigate
                    )
                    PantallaAdmin.EQUIPOS -> ContenidoEquiposAdmin(
                        estado = equipos,
                        onReintentar = viewModel::cargarEquipos,
                        onBack = { subScreen = null }
                    )
                    PantallaAdmin.NOTIFICACIONES -> VistaNotificaciones(
                        estado = notificaciones,
                        onReintentar = viewModel::cargarNotificaciones,
                        onMarcarLeida = viewModel::marcarLeida,
                        onMarcarTodasLeidas = viewModel::marcarTodasLeidas,
                        onBack = { subScreen = null }
                    )

                    PantallaAdmin.AMBIENTES -> VistaAdministrarAmbientes(onBack = { subScreen = null })
                    PantallaAdmin.VALIDAR_EXCUSA -> VistaValidarExcusa(onBack = { subScreen = null })
                    PantallaAdmin.ESCANEAR_QR -> EscanearQrInvitado(onVolver = { subScreen = null })
                    else -> when (currentTab) {
                        "INICIO" -> ResumenPanelAdmin(resumen = resumen, onReintentar = viewModel::cargarResumen)
                        "NOVEDADES" -> VistaNovedades(
                            estado = novedades,
                            onReintentar = viewModel::cargarNovedades
                        )
                        "SUGERENCIAS" -> VistaSugerencias()
                        "USUARIOS" -> ContenidoUsuarios(
                            onNavigate = onNavigate,
                            onEditarUsuario = { usuario ->
                                // (del GET /admin/users) y se abre el formulario de actualización.
                                usuarioAEditar = usuario
                                subScreen = PantallaAdmin.ACTUALIZAR_USUARIO
                            }
                        )
                        "HISTORIAL" -> ContenidoHistorial(
                            historial = historial,
                            onReintentar = viewModel::cargarHistorial,
                            onVerAprendices = { onNavigate(PantallaAdmin.ACCESO_APRENDICES) },
                            onVerInstructores = { onNavigate(PantallaAdmin.ACCESO_INSTRUCTORES) },
                            onRango = { desde, hasta -> viewModel.cargarHistorialRango(desde, hasta) }
                        )
                        "PRESENTES" -> VistaPersonasDentro(
                            ambientesEstado = ambientes,
                            presentesEstado = presentes,
                            ingresosEstado = resumen,
                            onReintentar = {
                                viewModel.cargarPresentes()
                                viewModel.cargarAmbientes()
                                viewModel.cargarResumen()
                            }
                        )
                    }
                }
            }
        }

        BarraNavegacion(
            items = listOf(
                ElementoNavegacion("INICIO", Icons.Default.Home, "Inicio"),
                ElementoNavegacion("NOVEDADES", Icons.Default.WarningAmber, "Novedades"),
                ElementoNavegacion("SUGERENCIAS", Icons.Default.Lightbulb, "Sugerencias"),
                ElementoNavegacion("PRESENTES", Icons.Default.Groups, "Presentes"),
                ElementoNavegacion("USUARIOS", Icons.Default.People, "Usuarios"),
                ElementoNavegacion("EQUIPOS", Icons.Default.Devices, "Equipos"),
                ElementoNavegacion("HISTORIAL", Icons.Default.History, "Historial")
            ),
            selectedKey = claveNavegacion(currentTab, "INICIO"),
            onSelect = { irATab(it) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        VistaPendientesDobleFactor()
    }
}
