package com.example.sennaccess.instructor.panel

// Pantalla principal del Instructor: orquesta las 4 vistas del dashboard
// (Resumen, Control de Ingresos, Equipos y Perfil) con barra superior de
// vidrio, contenido dinámico y dock flotante, reutilizando los componentes
// StatCard y ContenedorTabla del dashboard del Aprendiz.

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.datos.modelos.Ingreso
import com.example.sennaccess.datos.modelos.EquipoIngreso
import com.example.sennaccess.datos.modelos.Notificacion
import com.example.sennaccess.datos.sesion.GestorSesion
import com.example.sennaccess.datos.modelos.UsuarioApi
import com.example.sennaccess.datos.simulados.DatosSimulados
import com.example.sennaccess.instructor.ambientes.VistaDetalleAmbiente
import com.example.sennaccess.instructor.ambientes.VistaMisAmbientes
import com.example.sennaccess.datos.modelos.Ambiente
import com.example.sennaccess.excusas.VistaCrearExcusa
import com.example.sennaccess.perfil.VistaEditarPerfil
import com.example.sennaccess.comun.HistorialPorDias
import com.example.sennaccess.aprendiz.panel.TarjetaBienvenida
import com.example.sennaccess.aprendiz.panel.TituloSeccion
import com.example.sennaccess.aprendiz.panel.TarjetaEstadoAcceso
import com.example.sennaccess.aprendiz.panel.TarjetaActividad
import com.example.sennaccess.aprendiz.panel.ContenedorTabla
import com.example.sennaccess.comun.diseno.TipoInsignia
import com.example.sennaccess.comun.diseno.InsigniaSena
import com.example.sennaccess.comun.diseno.CeldaSena
import com.example.sennaccess.comun.EstadoCarga
import com.example.sennaccess.comun.EstadoContenido
import com.example.sennaccess.comun.VistaVacia
import com.example.sennaccess.administrador.novedades.VistaNotificaciones
import com.example.sennaccess.administrador.novedades.VistaNovedades
import com.example.sennaccess.perfil.FotoPerfil
import com.example.sennaccess.comun.CajaCargando
import com.example.sennaccess.comun.CajaError
import com.example.sennaccess.comun.fechaLegible
import com.example.sennaccess.comun.fechaRelativa
import com.example.sennaccess.comun.esHoyBogota
import com.example.sennaccess.comun.horaCorta
import com.example.sennaccess.autenticacion.verificacion.VistaConfigDobleFactor
import com.example.sennaccess.autenticacion.verificacion.VistaPendientesDobleFactor
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.RojoError
import com.example.sennaccess.comun.tema.verdeMarca
import com.example.sennaccess.comun.diseno.BarraNavegacion
import com.example.sennaccess.comun.diseno.ElementoNavegacion
import com.example.sennaccess.comun.diseno.BotonBordeBrillante
import com.example.sennaccess.comun.diseno.EsferasBrillo
import com.example.sennaccess.comun.diseno.MenuPerfilSena
import com.example.sennaccess.comun.diseno.CabeceraPlegable
import com.example.sennaccess.comun.diseno.MenuDesplegableVidrio
import com.example.sennaccess.comun.diseno.BarraSuperiorVidrio
import com.example.sennaccess.comun.diseno.BotonPrimarioNeon
import com.example.sennaccess.comun.diseno.BotonCambiarTema
import com.example.sennaccess.comun.diseno.EntradaSuave
import com.example.sennaccess.comun.diseno.superficieVidrio
import com.example.sennaccess.comun.diseno.superficiePlana
import com.example.sennaccess.comun.diseno.RadioVidrio
import com.example.sennaccess.comun.diseno.EspaciadoSena
import com.example.sennaccess.comun.diseno.claveNavegacion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelInstructor(onCerrarSesion: () -> Unit, isDark: Boolean = true, onToggleTheme: () -> Unit = {}) {
    var currentView by rememberSaveable { mutableStateOf("DASHBOARD") }
    var ambienteSeleccionado by remember { mutableStateOf<Ambiente?>(null) }

    var mostrarAutorizar by remember { mutableStateOf(false) }
    val colors = ColoresAppLocal.current
    // Instancia ligada al usuario en sesión: otro login nunca reutiliza el perfil en memoria.
    val viewModel: PanelInstructorViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        key = "panel-instructor-${GestorSesion.userId ?: "anon"}"
    )
    val scope = rememberCoroutineScope()
    var refrescando by remember { mutableStateOf(false) }

    fun cargarActual() {
        when (currentView) {
            "DASHBOARD" -> viewModel.cargarResumen()
            "NOVEDADES" -> viewModel.cargarNovedades()
            "HISTORIAL" -> viewModel.cargarHistorial()
            "MIS_EQUIPOS" -> viewModel.cargarEquipos()
            "PERFIL", "EDITAR_PERFIL" -> viewModel.cargarPerfil()
            "NOTIFICACIONES" -> viewModel.cargarNotificaciones()
            "AMBIENTES" -> {}
        }
    }

    LaunchedEffect(currentView) { cargarActual() }
    // Recarga el perfil al entrar al panel: corrige sesión anterior en el mismo proceso.
    LaunchedEffect(Unit) { viewModel.cargarPerfil() }
    // Polling silencioso: notificaciones en tiempo real para el instructor.
    LaunchedEffect(Unit) {
        while (true) {
            delay(20000)
            try { viewModel.cargarNotificacionesSilencioso() } catch (_: Exception) { }
        }
    }

    // El botón atrás del sistema retrocede dentro del panel en vez de no hacer nada.
    BackHandler(enabled = currentView != "DASHBOARD" || ambienteSeleccionado != null || mostrarAutorizar) {
        when {
            mostrarAutorizar -> mostrarAutorizar = false
            ambienteSeleccionado != null -> ambienteSeleccionado = null
            currentView == "EDITAR_PERFIL" || currentView == "VERIFICACION_2FA" -> currentView = "PERFIL"
            currentView != "DASHBOARD" -> currentView = "DASHBOARD"
        }
    }

    val resumen by viewModel.resumen.collectAsState()
    val historial by viewModel.historial.collectAsState()
    val equipos by viewModel.equipos.collectAsState()
    val perfil by viewModel.perfil.collectAsState()
    val novedades by viewModel.novedades.collectAsState()
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
            BarraInstructor(
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
                    "DASHBOARD" -> VistaResumenInstructor(
                        estado = resumen,
                        historialEstado = historial,
                        perfilEstado = perfil,
                        onReintentar = viewModel::cargarResumen,
                        onVerHistorial = { currentView = "HISTORIAL" }
                    )
                    "AMBIENTES" -> {
                        when {
                            mostrarAutorizar -> VistaCrearExcusa(onBack = { mostrarAutorizar = false }, ambienteIdInicial = ambienteSeleccionado?.id_ambiente)
                            ambienteSeleccionado != null -> VistaDetalleAmbiente(
                                ambiente = ambienteSeleccionado!!,
                                onBack = { ambienteSeleccionado = null },
                                onProyectarQr = null,
                                onAutorizarSalida = { mostrarAutorizar = true }
                            )
                            else -> VistaMisAmbientes(onAmbienteClick = { ambienteSeleccionado = it })
                        }
                    }
                    "NOVEDADES" -> VistaNovedades(estado = novedades, onReintentar = viewModel::cargarNovedades)
                    "HISTORIAL" -> VistaHistorialIngresos(historial, onReintentar = viewModel::cargarHistorial)
                    "MIS_EQUIPOS" -> VistaMisEquipos(equipos, onReintentar = viewModel::cargarEquipos)
                    "PERFIL" -> VistaPerfilInstructor(
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
                        onReintentar = viewModel::cargarPerfil,
                        mostrarFichaPrograma = false
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
                ElementoNavegacion("AMBIENTES", Icons.Default.MeetingRoom, "Ambientes"),
                ElementoNavegacion("NOVEDADES", Icons.Default.ReportProblem, "Novedades"),
                ElementoNavegacion("HISTORIAL", Icons.Default.History, "Historial"),
                ElementoNavegacion("MIS_EQUIPOS", Icons.Default.Devices, "Equipos")
            ),
            selectedKey = claveNavegacion(currentView, "DASHBOARD"),
            onSelect = { currentView = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        VistaPendientesDobleFactor()
    }
}

@Composable
fun BarraInstructor(
    onLogout: () -> Unit,
    onPerfil: (() -> Unit)? = null,
    onEditarPerfil: (() -> Unit)? = null,
    onNotificaciones: (() -> Unit)? = null,
    noLeidas: Int = 0,
    isDark: Boolean,
    onToggleTheme: () -> Unit
) {
    // Barra unificada de los 4 roles: marca + chip de rol + notis + menú perfil.
    com.example.sennaccess.comun.diseno.BarraSuperiorSena(
        rol = "Instructor",
        noLeidas = noLeidas,
        isDark = isDark,
        onToggleTheme = onToggleTheme,
        onNotificaciones = onNotificaciones,
        menu = { cerrar ->
            MenuPerfilSena(
                cerrar = cerrar,
                nombre = com.example.sennaccess.datos.sesion.GestorSesion.userName ?: "Usuario",
                email = com.example.sennaccess.datos.sesion.GestorSesion.userEmail ?: "",
                fotoPath = com.example.sennaccess.datos.sesion.GestorSesion.userPhoto,
                onPerfil = onPerfil,
                onEditarPerfil = onEditarPerfil
            )
        },
        onLogout = onLogout
    )
}

@Composable
fun VistaResumenInstructor(
    estado: EstadoCarga<ResumenInstructor>,
    historialEstado: EstadoCarga<List<Ingreso>> = EstadoCarga.Success(emptyList()),
    perfilEstado: EstadoCarga<UsuarioApi> = EstadoCarga.Loading,
    onReintentar: () -> Unit,
    onVerHistorial: () -> Unit = {}
) {
    val colors = ColoresAppLocal.current
    val scrollState = rememberScrollState()
    val perfilUsuario = (perfilEstado as? EstadoCarga.Success<UsuarioApi>)?.datos
    val nombreBienvenida = perfilUsuario?.nombreCompleto
        ?: GestorSesion.userName ?: DatosSimulados.instructorDemo.nombreCompleto
    val fotoPath = perfilUsuario?.profile_photo_path
    val ficha = perfilUsuario?.user_coursenumber
    val programa = perfilUsuario?.user_program

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        com.example.sennaccess.comun.diseno.CabeceraPantalla(
            eyebrow = "Instructor",
            titulo = nombreBienvenida,
            subtitulo = "Tus ambientes y movimiento de hoy"
        )

        EntradaSuave(indice = 0) {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                TarjetaBienvenida(
                    fotoPath = fotoPath,
                    nombre = nombreBienvenida,
                    rol = "INSTRUCTOR",
                    ficha = ficha,
                    programa = programa
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        com.example.sennaccess.aprendiz.panel.TituloSeccion(titulo = "TU ESTADO ACTUAL")
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            // Solo el día actual en el inicio: el historial completo vive en su pestaña.
            val historialHoy = when (historialEstado) {
                is EstadoCarga.Success -> EstadoCarga.Success(historialEstado.datos.filter { esHoyBogota(it.ingreso_datetime) })
                else -> historialEstado
            }
            when (historialHoy) {
                is EstadoCarga.Loading -> CajaCargando()
                is EstadoCarga.Error -> CajaError(historialHoy.mensaje, onReintentar)
                is EstadoCarga.Success -> {
                    val lista = historialHoy.datos
                    if (lista.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().superficiePlana(cornerRadius = RadioVidrio).padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Info, null, tint = colors.textSecondary, modifier = Modifier.size(26.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Sin movimientos hoy", color = colors.textSecondary, fontSize = 13.sp)
                            }
                        }
                    } else {
                        EntradaSuave(indice = 1) {
                            TarjetaEstadoAcceso(ingreso = lista.first())
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        com.example.sennaccess.aprendiz.panel.TituloSeccion(titulo = "ACTIVIDAD DE HOY", accionTexto = "Ver todo", onAccion = onVerHistorial)
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            val historialHoy = when (historialEstado) {
                is EstadoCarga.Success -> EstadoCarga.Success(historialEstado.datos.filter { esHoyBogota(it.ingreso_datetime) })
                else -> historialEstado
            }
            when (historialHoy) {
                is EstadoCarga.Loading -> CajaCargando()
                is EstadoCarga.Error -> CajaError(historialHoy.mensaje, onReintentar)
                is EstadoCarga.Success -> {
                    val lista = historialHoy.datos
                    if (lista.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().superficiePlana(cornerRadius = RadioVidrio).padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Sin movimientos hoy: tu historial completo está en Control de ingresos", color = colors.textSecondary, fontSize = 13.sp)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .superficiePlana(cornerRadius = RadioVidrio)
                                .padding(vertical = 4.dp)
                        ) {
                            lista.take(3).forEachIndexed { i, item ->
                                if (i > 0) HorizontalDivider(
                                    color = colors.divider,
                                    modifier = Modifier.padding(horizontal = 14.dp)
                                )
                                EntradaSuave(indice = 2 + i) {
                                    TarjetaActividad(item)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun VistaHistorialIngresos(estado: EstadoCarga<List<Ingreso>>, onReintentar: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        CabeceraPlegable(
            title = "Control de Ingresos",
            subtitle = "Tus accesos por día",
            scrollOffset = 0f
        )
        ContenedorTabla(title = "Control de Ingresos", subtitle = "Últimos 3 días • calendario para otro día") {
            EstadoContenido(estado = estado, onReintentar = onReintentar) { items ->
                HistorialPorDias(items = items, mostrarUsuario = false, textoVacio = "No hay ingresos registrados")
            }
        }
    }
}

@Composable
fun VistaMisEquipos(estado: EstadoCarga<List<EquipoIngreso>>, onReintentar: () -> Unit) {
    val colors = ColoresAppLocal.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        CabeceraPlegable(
            title = "Mis Comprobantes",
            subtitle = "Dispositivos del instructor",
            scrollOffset = scrollState.value.toFloat()
        )
        Spacer(modifier = Modifier.height(12.dp))
        ContenedorTabla(title = "Mis Comprobantes", subtitle = "Detalle completo de tus dispositivos") {
            EstadoContenido(estado = estado, onReintentar = onReintentar) { items ->
                if (items.isEmpty()) {
                    VistaVacia(
                        icono = Icons.Default.Devices,
                        titulo = "No hay equipos registrados",
                        mensaje = "Los equipos registrados a tu nombre aparecerán aquí."
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items.forEach { eq ->
                            com.example.sennaccess.administrador.equipos.TarjetaEquipoDetallada(eq)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VistaPerfilInstructor(estado: EstadoCarga<UsuarioApi>, onBack: () -> Unit, onReintentar: () -> Unit, onEditar: () -> Unit, onConfigurar2Fa: () -> Unit) {
    val colors = ColoresAppLocal.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .imePadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = colors.textPrimary) }
            Column(modifier = Modifier.weight(1f)) {
                Text("Perfil", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Información personal y seguridad", color = colors.textSecondary, fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        EstadoContenido(estado = estado, onReintentar = onReintentar) { usuario ->
            val filas = buildList {
                add(Triple(Icons.Default.Badge, "Documento", usuario.user_identification ?: "—"))
                if (!usuario.user_program.isNullOrBlank()) add(Triple(Icons.Default.School, "Programa", usuario.user_program!!))
                if (usuario.user_coursenumber != null && usuario.user_coursenumber > 0) add(Triple(Icons.Default.Numbers, "Ficha", usuario.user_coursenumber.toString()))
            }
            com.example.sennaccess.perfil.PerfilSenior(
                fotoPath = usuario.profile_photo_path,
                nombre = usuario.nombreCompleto,
                rol = "Instructor",
                correo = usuario.user_email,
                filas = filas,
                onEditar = onEditar,
                onConfigurar2Fa = onConfigurar2Fa
            )
            Spacer(modifier = Modifier.imePadding().height(96.dp))
        }
    }
}
