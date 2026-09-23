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
import com.example.sennaccess.autenticacion.invitado.EscanearQrInvitado
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
import com.example.sennaccess.perfil.FilaDato
import com.example.sennaccess.biometria.MiSeccionHuella
import com.example.sennaccess.administrador.novedades.VistaNotificaciones
import com.example.sennaccess.administrador.novedades.VistaNovedades
import com.example.sennaccess.perfil.FotoPerfil
import com.example.sennaccess.comun.CajaCargando
import com.example.sennaccess.comun.CajaError
import com.example.sennaccess.perfil.CabeceraPerfil
import com.example.sennaccess.comun.fechaLegible
import com.example.sennaccess.comun.fechaRelativa
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
import com.example.sennaccess.comun.diseno.CabeceraPlegable
import com.example.sennaccess.comun.diseno.MenuDesplegableVidrio
import com.example.sennaccess.comun.diseno.BarraSuperiorVidrio
import com.example.sennaccess.comun.diseno.BotonPrimarioNeon
import com.example.sennaccess.comun.diseno.BotonCambiarTema
import com.example.sennaccess.comun.diseno.superficieVidrio
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
                onToggleTheme = onToggleTheme,
                onEscanearQr = { currentView = "ESCANEAR_QR" }
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
                    "ESCANEAR_QR" -> EscanearQrInvitado(onVolver = { currentView = "DASHBOARD" })

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
    onToggleTheme: () -> Unit,
    onEscanearQr: (() -> Unit)? = null
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

                Box(
                    modifier = Modifier
                        .border(1.dp, VerdeSena.copy(0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "INSTRUCTOR",
                        color = verdeMarca(),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
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
                        if (onEscanearQr != null) {
                            DropdownMenuItem(
                                text = { Text("Escanear QR de invitado", color = colors.textPrimary) },
                                leadingIcon = { Icon(Icons.Default.QrCodeScanner, null, tint = verdeMarca()) },
                                onClick = { showMenu = false; onEscanearQr() }
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
        CabeceraPlegable(
            title = "Panel de Instructor",
            subtitle = "Bienvenido, $nombreBienvenida",
            scrollOffset = scrollState.value.toFloat()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TarjetaBienvenida(
            fotoPath = fotoPath,
            nombre = nombreBienvenida,
            rol = "INSTRUCTOR",
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
                        Text("No hay ingresos registrados", color = colors.textSecondary, fontSize = 13.sp)
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
        ContenedorTabla(title = "Mis Comprobantes", subtitle = "Dispositivos del instructor") {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = EspaciadoSena.xs)) {
                CeldaSena(texto = "EQUIPO", peso = 1f, encabezado = true)
                CeldaSena(texto = "MARCA/MODELO", peso = 1.5f, encabezado = true)
                CeldaSena(texto = "SERIAL", peso = 1.2f, encabezado = true)
            }
            HorizontalDivider(color = colors.border)

            EstadoContenido(estado = estado, onReintentar = onReintentar) { items ->
                if (items.isEmpty()) {
                    VistaVacia(
                        icono = Icons.Default.Devices,
                        titulo = "No hay equipos registrados",
                        mensaje = "Los equipos registrados a tu nombre aparecerán aquí."
                    )
                } else {
                    items.forEach { eq ->
                        HorizontalDivider(color = colors.border)
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = EspaciadoSena.sm), verticalAlignment = Alignment.CenterVertically) {
                            CeldaSena(texto = eq.equipo_type ?: "Equipo", peso = 1f)
                            CeldaSena(texto = eq.marcaModelo, peso = 1.5f)
                            CeldaSena(texto = eq.equipo_serial ?: "—", peso = 1.2f)
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
