package com.example.sennaccess.aprendiz

// ViewModel del dashboard del Instructor: mantiene 5 flujos de estado CargaUiState
// (resumen, historial, equipos, perfil y novedades) alimentados desde la API vía
// los repositorios, con respaldo a datos de ejemplo cuando no hay sesión activa.

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sennaccess.data.EquipoRepository
import com.example.sennaccess.data.Ingreso
import com.example.sennaccess.data.IngresoEquipo
import com.example.sennaccess.data.IngresoRepository
import com.example.sennaccess.data.Notificacion
import com.example.sennaccess.data.NotificacionRepository
import com.example.sennaccess.data.Novedad
import com.example.sennaccess.data.NovedadRepository
import com.example.sennaccess.data.SessionManager
import com.example.sennaccess.data.UsuarioApi
import com.example.sennaccess.data.UsuarioRepository
import com.example.sennaccess.data.mock.MockData
import com.example.sennaccess.ui.CargaUiState
import com.example.sennaccess.ui.cargarConFallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Modelo del resumen: conteos de usuarios e ingresos para las StatCard.
data class ResumenInstructor(
    val usuariosCount: Int,
    val ingresosCount: Int
)

class InstructorDashboardViewModel : ViewModel() {

    // Repositorios que encapsulan las llamadas Retrofit a los endpoints de la API.
    private val usuarioRepo = UsuarioRepository()
    private val ingresoRepo = IngresoRepository()
    private val equipoRepo = EquipoRepository()
    private val novedadRepo = NovedadRepository()
    private val notificacionRepo = NotificacionRepository()

    // Patrón CargaUiState: cada flujo arranca en Loading y muta a Success o Error.
    // Se expone inmutable (asStateFlow) para que la UI solo observe.
    private val _resumen = MutableStateFlow<CargaUiState<ResumenInstructor>>(CargaUiState.Loading)
    val resumen: StateFlow<CargaUiState<ResumenInstructor>> = _resumen.asStateFlow()

    private val _historial = MutableStateFlow<CargaUiState<List<Ingreso>>>(CargaUiState.Loading)
    val historial: StateFlow<CargaUiState<List<Ingreso>>> = _historial.asStateFlow()

    private val _equipos = MutableStateFlow<CargaUiState<List<IngresoEquipo>>>(CargaUiState.Loading)
    val equipos: StateFlow<CargaUiState<List<IngresoEquipo>>> = _equipos.asStateFlow()

    private val _perfil = MutableStateFlow<CargaUiState<UsuarioApi>>(CargaUiState.Loading)
    val perfil: StateFlow<CargaUiState<UsuarioApi>> = _perfil.asStateFlow()

    private val _novedades = MutableStateFlow<CargaUiState<List<Novedad>>>(CargaUiState.Loading)
    val novedades: StateFlow<CargaUiState<List<Novedad>>> = _novedades.asStateFlow()

    private val _notificaciones = MutableStateFlow<CargaUiState<List<Notificacion>>>(CargaUiState.Loading)
    val notificaciones: StateFlow<CargaUiState<List<Notificacion>>> = _notificaciones.asStateFlow()

    // Carga todas las secciones al crear el ViewModel; el reintento las refresca.
    init {
        cargarResumen()
        cargarHistorial()
        cargarEquipos()
        cargarPerfil()
        cargarNovedades()
        cargarNotificaciones()
    }

    // Calcula el resumen combinando dos llamadas (usuarios e ingresos).
    // cargarConFallback lanza una corrutina en viewModelScope: sin sesión usa el
    // fallback y con sesión marca Loading, llama a la API y muta a Success/Error.
    fun cargarResumen() {
        // Datos de ejemplo como respaldo cuando no hay sesión activa.
        val mock = ResumenInstructor(
            usuariosCount = MockData.usuarios.size,
            ingresosCount = MockData.ingresos.size
        )
        cargarConFallback(fallback = { mock }, setState = { _resumen.value = it }) {
            val usuarios = usuarioRepo.getUsers(SessionManager.token!!)
            val ingresos = ingresoRepo.getMyIngresos(SessionManager.token!!)
            ResumenInstructor(usuarios.size, ingresos.size)
        }
    }

    // Historial de ingresos PROPIO del instructor (GET /my-ingresos); el historial
    // global del centro es exclusivo del admin.
    fun cargarHistorial() {
        cargarConFallback(fallback = { MockData.ingresos }, setState = { _historial.value = it }) {
            ingresoRepo.getMyIngresos(SessionManager.token!!)
        }
    }

    // Comprobantes de equipos del instructor vía EquipoRepository.
    fun cargarEquipos() {
        cargarConFallback(fallback = { MockData.equipos }, setState = { _equipos.value = it }) {
            equipoRepo.getMyEquipment(SessionManager.token!!)
        }
    }

    // Perfil del instructor logueado, consumido desde UsuarioRepository.
    fun cargarPerfil() {
        cargarConFallback(fallback = { MockData.instructorDemo }, setState = { _perfil.value = it }) {
            usuarioRepo.getCurrentUser(SessionManager.token!!)
        }
    }

    // Novedades del instructor: solo las que él publicó (GET /my-novedades); el
    // listado completo del centro es exclusivo del admin.
    fun cargarNovedades() {
        cargarConFallback(fallback = { MockData.novedades }, setState = { _novedades.value = it }) {
            novedadRepo.getMyNovedades(SessionManager.token!!)
        }
    }

    // Notificaciones in-app del instructor (GET /notifications).
    fun cargarNotificaciones() {
        cargarConFallback(fallback = { MockData.notificaciones }, setState = { _notificaciones.value = it }) {
            notificacionRepo.getNotificaciones(SessionManager.token!!)
        }
    }

    // Marca una notificación como leída y refresca la lista en pantalla.
    fun marcarLeida(id: Int) {
        val token = SessionManager.token ?: return
        viewModelScope.launch {
            try {
                notificacionRepo.marcarLeida(token, id)
                cargarNotificaciones()
            } catch (e: Exception) {
                // Fallo de red: se mantiene la lista sin cambios.
            }
        }
    }

    // Marca todas las notificaciones como leídas y refresca la lista en pantalla.
    fun marcarTodasLeidas() {
        val token = SessionManager.token ?: return
        viewModelScope.launch {
            try {
                notificacionRepo.marcarTodasLeidas(token)
                cargarNotificaciones()
            } catch (e: Exception) {
                // Fallo de red: se mantiene la lista sin cambios.
            }
        }
    }
}
