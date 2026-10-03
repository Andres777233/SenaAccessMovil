package com.example.sennaccess.portero

// Panel del rol PORTERO: solo las funciones operativas de recepción.
// Dock de 3 tabs: VALIDAR (PIN+QR unificados), EQUIPOS (registrar/ver),
// HISTORIAL (accesos del día). Sin usuarios, ambientes ni novedades.

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.sennaccess.administrador.accesos.ContenidoHistorial
import com.example.sennaccess.administrador.equipos.ContenidoEquiposAdmin
import com.example.sennaccess.administrador.novedades.VistaNotificaciones
import com.example.sennaccess.administrador.panel.PanelAdministradorViewModel
import com.example.sennaccess.autenticacion.verificacion.VistaConfigDobleFactor
import com.example.sennaccess.autenticacion.verificacion.VistaPendientesDobleFactor
import com.example.sennaccess.comun.EstadoCarga
import com.example.sennaccess.comun.CajaCargando
import com.example.sennaccess.comun.CajaError
import com.example.sennaccess.comun.diseno.BarraNavegacion
import com.example.sennaccess.comun.diseno.RadioSena
import com.example.sennaccess.comun.diseno.superficiePlana
import com.example.sennaccess.comun.diseno.BarraSuperiorVidrio
import com.example.sennaccess.comun.diseno.BotonCambiarTema
import com.example.sennaccess.comun.diseno.ElementoNavegacion
import com.example.sennaccess.comun.diseno.EsferasBrillo
import com.example.sennaccess.comun.diseno.MenuPerfilSena
import com.example.sennaccess.comun.diseno.EspaciadoSena
import com.example.sennaccess.comun.diseno.MenuDesplegableVidrio
import com.example.sennaccess.comun.diseno.claveNavegacion
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.RojoError
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import com.example.sennaccess.datos.modelos.Notificacion
import com.example.sennaccess.datos.modelos.estaPendiente
import com.example.sennaccess.datos.sesion.GestorSesion
import com.example.sennaccess.excusas.VistaValidarUnificada
import com.example.sennaccess.perfil.VistaEditarPerfil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelPortero(
    onCerrarSesion: () -> Unit,
    isDark: Boolean = true,
    onToggleTheme: () -> Unit = {}
) {
    var currentTab by rememberSaveable { mutableStateOf("INICIO") }
    var subScreen by rememberSaveable { mutableStateOf<String?>(null) }
    var editandoPerfil by rememberSaveable { mutableStateOf(false) }
    val colors = ColoresAppLocal.current
    val scope = rememberCoroutineScope()
    var refrescando by remember { mutableStateOf(false) }
    val viewModel: PanelAdministradorViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        key = "panel-portero-${GestorSesion.userId ?: "anon"}"
    )
    val resumen by viewModel.resumen.collectAsState()
    val historial by viewModel.historial.collectAsState()
    val perfil by viewModel.perfil.collectAsState()
    val equipos by viewModel.equipos.collectAsState()
    val notificaciones by viewModel.notificaciones.collectAsState()
    val noLeidas = (notificaciones as? EstadoCarga.Success<List<Notificacion>>)?.datos
        ?.count { it.is_read != true } ?: 0

    fun cargarActual() {
        when (subScreen) {
            "NOTIFICACIONES" -> viewModel.cargarNotificaciones()
            "PERFIL" -> viewModel.cargarPerfil()
            else -> when (currentTab) {
                "INICIO" -> { viewModel.cargarResumen(); viewModel.cargarHistorial() }
                "VALIDAR" -> { }
                "EQUIPOS" -> viewModel.cargarEquipos()
                "HISTORIAL" -> viewModel.cargarHistorial()
            }
        }
    }

    LaunchedEffect(currentTab, subScreen) { cargarActual() }
    LaunchedEffect(Unit) {
        viewModel.cargarPerfil()
        viewModel.cargarNotificaciones()
        viewModel.cargarResumen()
        viewModel.cargarHistorial()
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(20000)
            try { viewModel.cargarNotificacionesSilencioso() } catch (_: Exception) { }
        }
    }
    BackHandler(enabled = subScreen != null || currentTab != "INICIO") {
        if (subScreen != null) { subScreen = null; editandoPerfil = false }
        else currentTab = "INICIO"
    }

    Box(modifier = Modifier.fillMaxSize().systemBarsPadding().background(colors.background)) {
        EsferasBrillo(isDark = isDark)
        Column(modifier = Modifier.fillMaxSize()) {
            BarraPortero(
                onLogout = onCerrarSesion,
                onPerfil = { subScreen = "PERFIL"; editandoPerfil = false },
                onNotificaciones = { subScreen = "NOTIFICACIONES" },
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
                modifier = Modifier.fillMaxSize().padding(
                    start = EspaciadoSena.screenH,
                    end = EspaciadoSena.screenH,
                    top = EspaciadoSena.screenV,
                    bottom = EspaciadoSena.dockClearance
                )
            ) {
                when (subScreen) {
                    "PERFIL" -> if (editandoPerfil) {
                        VistaEditarPerfil(
                            estado = perfil,
                            onBack = { editandoPerfil = false },
                            onGuardado = { editandoPerfil = false; viewModel.cargarPerfil() },
                            onReintentar = viewModel::cargarPerfil,
                            mostrarFichaPrograma = false
                        )
                    } else {
                        com.example.sennaccess.administrador.panel.ContenidoPerfilAdmin(
                            perfil = perfil,
                            onBack = { subScreen = null },
                            onReintentar = viewModel::cargarPerfil,
                            onEditar = { editandoPerfil = true },
                            onConfigurar2Fa = { subScreen = "VERIFICACION_2FA" }
                        )
                    }
                    "VERIFICACION_2FA" -> VistaConfigDobleFactor(onBack = { subScreen = "PERFIL" })
                    "NOTIFICACIONES" -> VistaNotificaciones(
                        estado = notificaciones,
                        onReintentar = viewModel::cargarNotificaciones,
                        onMarcarLeida = viewModel::marcarLeida,
                        onMarcarTodasLeidas = viewModel::marcarTodasLeidas,
                        onBack = { subScreen = null }
                    )
                    else -> when (currentTab) {
                        "INICIO" -> VistaPorteroInicio(
                            resumen = resumen,
                            historial = historial,
                            onValidar = { currentTab = "VALIDAR" },
                            onVerHistorial = { currentTab = "HISTORIAL" },
                            onVerEquipos = { currentTab = "EQUIPOS" },
                            onReintentar = { viewModel.cargarResumen(); viewModel.cargarHistorial() }
                        )
                        "VALIDAR" -> VistaValidarUnificada(onBack = { currentTab = "INICIO" })
                        "EQUIPOS" -> ContenidoEquiposAdmin(
                            estado = equipos,
                            onReintentar = viewModel::cargarEquipos,
                            onBack = { currentTab = "INICIO" }
                        )
                        else -> ContenidoHistorial(
                            historial = historial,
                            onReintentar = viewModel::cargarHistorial,
                            onVerAprendices = { },
                            onVerInstructores = { },
                            onRango = { desde, hasta -> viewModel.cargarHistorialRango(desde, hasta) }
                        )
                    }
                }
            }
        }
        BarraNavegacion(
            items = listOf(
                ElementoNavegacion("INICIO", Icons.Default.Home, "Inicio"),
                ElementoNavegacion("VALIDAR", Icons.Default.VpnKey, "Validar"),
                ElementoNavegacion("EQUIPOS", Icons.Default.Devices, "Equipos"),
                ElementoNavegacion("HISTORIAL", Icons.Default.History, "Historial")
            ),
            selectedKey = claveNavegacion(currentTab, "INICIO"),
            onSelect = { currentTab = it; subScreen = null },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        VistaPendientesDobleFactor()
    }
}

@Composable
fun VistaPorteroInicio(
    resumen: EstadoCarga<List<com.example.sennaccess.datos.modelos.Ingreso>>,
    historial: EstadoCarga<com.example.sennaccess.administrador.panel.DatosHistorial>,
    onValidar: () -> Unit,
    onVerHistorial: () -> Unit,
    onVerEquipos: () -> Unit,
    onReintentar: () -> Unit
) {
    val colors = ColoresAppLocal.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Hero de turno: eyebrow + saludo + acción principal de validación.
        Text("TURNO DE PORTERÍA", color = verdeMarca(), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Hola, ${GestorSesion.userName ?: "Portero"}",
            color = colors.textPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, maxLines = 1
        )
        Text("Controla entradas y salidas del centro desde aquí.", color = colors.textSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(14.dp))
        // Cinta contextual: excusas pendientes por validar (toca para ir al dock).
        CintaPendientesPortero(onValidar = onValidar)
        Spacer(modifier = Modifier.height(16.dp))
        // Contadores del día desde el resumen (entradas/salidas de hoy).
        when (val r = resumen) {
            is EstadoCarga.Loading -> CajaCargando()
            is EstadoCarga.Error -> CajaError(r.mensaje, onReintentar = onReintentar)
            is EstadoCarga.Success -> {
                val entradas = r.datos.count { !(it.ingreso_type ?: "").equals("Salida", ignoreCase = true) }
                val salidas = r.datos.count { (it.ingreso_type ?: "").equals("Salida", ignoreCase = true) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ContadorPortero("Entradas", entradas.toString(), Modifier.weight(1f))
                    ContadorPortero("Salidas", salidas.toString(), Modifier.weight(1f))
                    ContadorPortero("Movimientos", r.datos.size.toString(), Modifier.weight(1f))
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Accesos directos secundarios a Equipos e Historial.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.weight(1f).superficiePlana(cornerRadius = RadioSena.lg).clickable { onVerEquipos() }.padding(16.dp)
            ) {
                Column {
                    Icon(Icons.Default.Devices, null, tint = verdeMarca(), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Equipos", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Registrar y buscar", color = colors.textSecondary, fontSize = 12.sp)
                }
            }
            Box(
                modifier = Modifier.weight(1f).superficiePlana(cornerRadius = RadioSena.lg).clickable { onVerHistorial() }.padding(16.dp)
            ) {
                Column {
                    Icon(Icons.Default.History, null, tint = verdeMarca(), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Historial", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Movimientos del día", color = colors.textSecondary, fontSize = 12.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CintaPendientesPortero(onValidar: () -> Unit) {
    val colors = ColoresAppLocal.current
    val scope = rememberCoroutineScope()
    var pendientes by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val t = GestorSesion.token ?: return@launch
                val lista = com.example.sennaccess.datos.repositorios.RepositorioExcusas().todasAdmin(t)
                pendientes = lista.count { it.estaPendiente }
            } catch (_: Exception) { pendientes = null }
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(VerdeSena.copy(alpha = 0.10f))
            .clickable(onClick = onValidar)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Icon(Icons.Default.VpnKey, null, tint = verdeMarca(), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                when (val n = pendientes) {
                    null -> "Turno activo"
                    0 -> "Sin pendientes por validar"
                    1 -> "1 excusa pendiente por validar"
                    else -> "$n excusas pendientes por validar"
                },
                color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp
            )
            Text("Toca para abrir Validar en el dock", color = colors.textSecondary, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = "Ir a validar", tint = verdeMarca(), modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun ContadorPortero(titulo: String, valor: String, modifier: Modifier = Modifier) {
    val colors = ColoresAppLocal.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.superficiePlana(cornerRadius = RadioSena.lg).padding(vertical = 14.dp, horizontal = 8.dp)
    ) {
        Text(valor, color = colors.textPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(titulo.uppercase(), color = colors.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.0.sp)
    }
}

@Composable
private fun BarraPortero(
    onLogout: () -> Unit,
    onPerfil: () -> Unit,
    onNotificaciones: () -> Unit,
    noLeidas: Int,
    isDark: Boolean,
    onToggleTheme: () -> Unit
) {
    // Barra unificada de los 4 roles: marca + chip de rol + notis + menú perfil.
    com.example.sennaccess.comun.diseno.BarraSuperiorSena(
        rol = "Portero",
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
                onPerfil = onPerfil
            )
        },
        onLogout = onLogout
    )
}
