package com.example.sennaccess.aprendiz.panel

// ViewModel del dashboard del Aprendiz: mantiene 4 flujos de estado EstadoCarga
// (resumen, historial, comprobantes y perfil) alimentados desde la API
// vía los repositorios, con respaldo a datos de ejemplo cuando no hay sesión activa.

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sennaccess.datos.repositorios.RepositorioEquipos
import com.example.sennaccess.datos.modelos.Ingreso
import com.example.sennaccess.datos.modelos.EquipoIngreso
import com.example.sennaccess.datos.repositorios.RepositorioIngresos
import com.example.sennaccess.datos.modelos.Notificacion
import com.example.sennaccess.datos.repositorios.RepositorioNotificaciones
import com.example.sennaccess.datos.sesion.GestorSesion
import com.example.sennaccess.datos.modelos.UsuarioApi
import com.example.sennaccess.datos.repositorios.RepositorioUsuarios
import com.example.sennaccess.datos.simulados.DatosSimulados
import com.example.sennaccess.comun.EstadoCarga
import com.example.sennaccess.comun.cargarConRespaldo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ResumenAprendiz(
    val nombre: String,
    val ingresosCount: Int,
    val equiposCount: Int
)

// Datos del panel del aprendiz: resumen, historial y comprobantes.
class PanelAprendizViewModel : ViewModel() {

    private val ingresoRepo = RepositorioIngresos()
    private val equipoRepo = RepositorioEquipos()
    private val usuarioRepo = RepositorioUsuarios()
    private val notificacionRepo = RepositorioNotificaciones()

    private val _resumen = MutableStateFlow<EstadoCarga<ResumenAprendiz>>(EstadoCarga.Loading)
    val resumen: StateFlow<EstadoCarga<ResumenAprendiz>> = _resumen.asStateFlow()

    private val _historial = MutableStateFlow<EstadoCarga<List<Ingreso>>>(EstadoCarga.Loading)
    val historial: StateFlow<EstadoCarga<List<Ingreso>>> = _historial.asStateFlow()

    private val _comprobantes = MutableStateFlow<EstadoCarga<List<EquipoIngreso>>>(EstadoCarga.Loading)
    val comprobantes: StateFlow<EstadoCarga<List<EquipoIngreso>>> = _comprobantes.asStateFlow()

    private val _perfil = MutableStateFlow<EstadoCarga<UsuarioApi>>(EstadoCarga.Loading)
    val perfil: StateFlow<EstadoCarga<UsuarioApi>> = _perfil.asStateFlow()

    private val _notificaciones = MutableStateFlow<EstadoCarga<List<Notificacion>>>(EstadoCarga.Loading)
    val notificaciones: StateFlow<EstadoCarga<List<Notificacion>>> = _notificaciones.asStateFlow()

    init {
        cargarResumen()
        cargarHistorial()
        cargarComprobantes()
        cargarPerfil()
        cargarNotificaciones()
    }

    // Recarga el resumen del aprendiz.
    fun cargarResumen() {
        val mock = ResumenAprendiz(
            nombre = GestorSesion.userName ?: DatosSimulados.aprendizDemo.nombreCompleto,
            ingresosCount = DatosSimulados.historialAprendiz.size,
            equiposCount = DatosSimulados.equipos.size
        )
        cargarConRespaldo(fallback = { mock }, setState = { _resumen.value = it }) {
            val ingresos = ingresoRepo.getMyIngresos(GestorSesion.token!!)
            val equipos = equipoRepo.getMyEquipment(GestorSesion.token!!)
            ResumenAprendiz(
                nombre = GestorSesion.userName ?: DatosSimulados.aprendizDemo.nombreCompleto,
                ingresosCount = ingresos.size,
                equiposCount = equipos.size
            )
        }
    }

    // Recarga su historial de movimientos.
    fun cargarHistorial() {
        cargarConRespaldo(fallback = { DatosSimulados.historialAprendiz }, setState = { _historial.value = it }) {
            ingresoRepo.getMyIngresos(GestorSesion.token!!)
        }
    }

    // Recarga sus equipos (comprobantes).
    fun cargarComprobantes() {
        cargarConRespaldo(fallback = { DatosSimulados.equipos }, setState = { _comprobantes.value = it }) {
            equipoRepo.getMyEquipment(GestorSesion.token!!)
        }
    }

    // Recarga su perfil.
    fun cargarPerfil() {
        cargarConRespaldo(fallback = { DatosSimulados.aprendizDemo }, setState = { _perfil.value = it }) {
            usuarioRepo.getCurrentUser(GestorSesion.token!!)
        }
    }

    // Notificaciones in-app del aprendiz (GET /notifications).
    fun cargarNotificaciones() {
        cargarConRespaldo(fallback = { DatosSimulados.notificaciones }, setState = { _notificaciones.value = it }) {
            notificacionRepo.getNotificaciones(GestorSesion.token!!)
        }
    }

    // Polling silencioso cada 20 s: notificaciones en tiempo real sin parpadeo.
    fun cargarNotificacionesSilencioso() {
        val token = GestorSesion.token ?: return
        viewModelScope.launch {
            try {
                _notificaciones.value = EstadoCarga.Success(notificacionRepo.getNotificaciones(token))
            } catch (_: Exception) { }
        }
    }

    // Marca una notificación como leída.
    fun marcarLeida(id: Int) {
        val token = GestorSesion.token ?: return
        viewModelScope.launch {
            try {
                notificacionRepo.marcarLeida(token, id)
                cargarNotificaciones()
            } catch (e: Exception) {
            }
        }
    }

    // Marca todas las notificaciones como leídas.
    fun marcarTodasLeidas() {
        val token = GestorSesion.token ?: return
        viewModelScope.launch {
            try {
                notificacionRepo.marcarTodasLeidas(token)
                cargarNotificaciones()
            } catch (e: Exception) {
            }
        }
    }
}
