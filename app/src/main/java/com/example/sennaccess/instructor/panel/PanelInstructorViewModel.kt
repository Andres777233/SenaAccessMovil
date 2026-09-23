package com.example.sennaccess.instructor.panel

// ViewModel del dashboard del Instructor: mantiene 5 flujos de estado EstadoCarga
// (resumen, historial, equipos, perfil y novedades) alimentados desde la API vía
// los repositorios, con respaldo a datos de ejemplo cuando no hay sesión activa.

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sennaccess.datos.repositorios.RepositorioEquipos
import com.example.sennaccess.datos.modelos.Ingreso
import com.example.sennaccess.datos.modelos.EquipoIngreso
import com.example.sennaccess.datos.repositorios.RepositorioIngresos
import com.example.sennaccess.datos.modelos.Notificacion
import com.example.sennaccess.datos.repositorios.RepositorioNotificaciones
import com.example.sennaccess.datos.modelos.Novedad
import com.example.sennaccess.datos.repositorios.RepositorioNovedades
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

data class ResumenInstructor(
    val usuariosCount: Int,
    val ingresosCount: Int
)

// Datos del panel del instructor: resumen, equipos y novedades.
class PanelInstructorViewModel : ViewModel() {

    private val usuarioRepo = RepositorioUsuarios()
    private val ingresoRepo = RepositorioIngresos()
    private val equipoRepo = RepositorioEquipos()
    private val novedadRepo = RepositorioNovedades()
    private val notificacionRepo = RepositorioNotificaciones()

    private val _resumen = MutableStateFlow<EstadoCarga<ResumenInstructor>>(EstadoCarga.Loading)
    val resumen: StateFlow<EstadoCarga<ResumenInstructor>> = _resumen.asStateFlow()

    private val _historial = MutableStateFlow<EstadoCarga<List<Ingreso>>>(EstadoCarga.Loading)
    val historial: StateFlow<EstadoCarga<List<Ingreso>>> = _historial.asStateFlow()

    private val _equipos = MutableStateFlow<EstadoCarga<List<EquipoIngreso>>>(EstadoCarga.Loading)
    val equipos: StateFlow<EstadoCarga<List<EquipoIngreso>>> = _equipos.asStateFlow()

    private val _perfil = MutableStateFlow<EstadoCarga<UsuarioApi>>(EstadoCarga.Loading)
    val perfil: StateFlow<EstadoCarga<UsuarioApi>> = _perfil.asStateFlow()

    private val _novedades = MutableStateFlow<EstadoCarga<List<Novedad>>>(EstadoCarga.Loading)
    val novedades: StateFlow<EstadoCarga<List<Novedad>>> = _novedades.asStateFlow()

    private val _notificaciones = MutableStateFlow<EstadoCarga<List<Notificacion>>>(EstadoCarga.Loading)
    val notificaciones: StateFlow<EstadoCarga<List<Notificacion>>> = _notificaciones.asStateFlow()

    init {
        cargarResumen()
        cargarHistorial()
        cargarEquipos()
        cargarPerfil()
        cargarNovedades()
        cargarNotificaciones()
    }

    // Recarga el resumen del instructor.
    fun cargarResumen() {
        val mock = ResumenInstructor(
            usuariosCount = DatosSimulados.usuarios.size,
            ingresosCount = DatosSimulados.ingresos.size
        )
        cargarConRespaldo(fallback = { mock }, setState = { _resumen.value = it }) {
            val usuarios = usuarioRepo.getUsers(GestorSesion.token!!)
            val ingresos = ingresoRepo.getMyIngresos(GestorSesion.token!!)
            ResumenInstructor(usuarios.size, ingresos.size)
        }
    }

    // Historial de ingresos PROPIO del instructor (GET /my-ingresos); el historial
    fun cargarHistorial() {
        cargarConRespaldo(fallback = { DatosSimulados.ingresos }, setState = { _historial.value = it }) {
            ingresoRepo.getMyIngresos(GestorSesion.token!!)
        }
    }

    // Recarga sus equipos.
    fun cargarEquipos() {
        cargarConRespaldo(fallback = { DatosSimulados.equipos }, setState = { _equipos.value = it }) {
            equipoRepo.getMyEquipment(GestorSesion.token!!)
        }
    }

    // Recarga su perfil.
    fun cargarPerfil() {
        cargarConRespaldo(fallback = { DatosSimulados.instructorDemo }, setState = { _perfil.value = it }) {
            usuarioRepo.getCurrentUser(GestorSesion.token!!)
        }
    }

    // Novedades del instructor: solo las que él publicó (GET /my-novedades); el
    fun cargarNovedades() {
        cargarConRespaldo(fallback = { DatosSimulados.novedades }, setState = { _novedades.value = it }) {
            novedadRepo.getMyNovedades(GestorSesion.token!!)
        }
    }

    // Notificaciones in-app del instructor (GET /notifications).
    fun cargarNotificaciones() {
        cargarConRespaldo(fallback = { DatosSimulados.notificaciones }, setState = { _notificaciones.value = it }) {
            notificacionRepo.getNotificaciones(GestorSesion.token!!)
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
