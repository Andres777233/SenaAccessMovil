package com.example.sennaccess.aprendiz

// ViewModel del dashboard del Aprendiz: mantiene 4 flujos de estado CargaUiState
// (resumen, historial, comprobantes y perfil) alimentados desde la API
// vía los repositorios, con respaldo a datos de ejemplo cuando no hay sesión activa.

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sennaccess.data.EquipoRepository
import com.example.sennaccess.data.Ingreso
import com.example.sennaccess.data.IngresoEquipo
import com.example.sennaccess.data.IngresoRepository
import com.example.sennaccess.data.Notificacion
import com.example.sennaccess.data.NotificacionRepository
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

// Modelo del resumen: nombre del aprendiz y conteos derivados de sus registros.
data class ResumenAprendiz(
    val nombre: String,
    val ingresosCount: Int,
    val equiposCount: Int
)

class AprendizDashboardViewModel : ViewModel() {

    // Repositorios que encapsulan las llamadas Retrofit a los endpoints de la API.
    private val ingresoRepo = IngresoRepository()
    private val equipoRepo = EquipoRepository()
    private val usuarioRepo = UsuarioRepository()
    private val notificacionRepo = NotificacionRepository()

    // Patrón CargaUiState: cada flujo arranca en Loading y muta a Success o Error.
    // Se expone inmutable (asStateFlow) para que la UI solo observe.
    private val _resumen = MutableStateFlow<CargaUiState<ResumenAprendiz>>(CargaUiState.Loading)
    val resumen: StateFlow<CargaUiState<ResumenAprendiz>> = _resumen.asStateFlow()

    private val _historial = MutableStateFlow<CargaUiState<List<Ingreso>>>(CargaUiState.Loading)
    val historial: StateFlow<CargaUiState<List<Ingreso>>> = _historial.asStateFlow()

    private val _comprobantes = MutableStateFlow<CargaUiState<List<IngresoEquipo>>>(CargaUiState.Loading)
    val comprobantes: StateFlow<CargaUiState<List<IngresoEquipo>>> = _comprobantes.asStateFlow()

    private val _perfil = MutableStateFlow<CargaUiState<UsuarioApi>>(CargaUiState.Loading)
    val perfil: StateFlow<CargaUiState<UsuarioApi>> = _perfil.asStateFlow()

    private val _notificaciones = MutableStateFlow<CargaUiState<List<Notificacion>>>(CargaUiState.Loading)
    val notificaciones: StateFlow<CargaUiState<List<Notificacion>>> = _notificaciones.asStateFlow()

    // Carga todas las secciones al crear el ViewModel; el reintento las refresca.
    init {
        cargarResumen()
        cargarHistorial()
        cargarComprobantes()
        cargarPerfil()
        cargarNotificaciones()
    }

    // Calcula el resumen combinando dos llamadas (ingresos y equipos) del aprendiz.
    // cargarConFallback lanza una corrutina en viewModelScope: sin sesión usa el
    // fallback y con sesión marca Loading, llama a la API y muta a Success/Error.
    fun cargarResumen() {
        // Datos de ejemplo como respaldo cuando no hay sesión activa.
        val mock = ResumenAprendiz(
            nombre = SessionManager.userName ?: MockData.aprendizDemo.nombreCompleto,
            ingresosCount = MockData.historialAprendiz.size,
            equiposCount = MockData.equipos.size
        )
        cargarConFallback(fallback = { mock }, setState = { _resumen.value = it }) {
            val ingresos = ingresoRepo.getMyIngresos(SessionManager.token!!)
            val equipos = equipoRepo.getMyEquipment(SessionManager.token!!)
            ResumenAprendiz(
                nombre = SessionManager.userName ?: MockData.aprendizDemo.nombreCompleto,
                ingresosCount = ingresos.size,
                equiposCount = equipos.size
            )
        }
    }

    // Historial de ingresos del aprendiz, con la misma lógica de fallback a mocks.
    fun cargarHistorial() {
        cargarConFallback(fallback = { MockData.historialAprendiz }, setState = { _historial.value = it }) {
            ingresoRepo.getMyIngresos(SessionManager.token!!)
        }
    }

    // Comprobantes de equipos registrados por el aprendiz vía EquipoRepository.
    fun cargarComprobantes() {
        cargarConFallback(fallback = { MockData.equipos }, setState = { _comprobantes.value = it }) {
            equipoRepo.getMyEquipment(SessionManager.token!!)
        }
    }

    // Perfil del aprendiz logueado, consumido desde UsuarioRepository.
    fun cargarPerfil() {
        cargarConFallback(fallback = { MockData.aprendizDemo }, setState = { _perfil.value = it }) {
            usuarioRepo.getCurrentUser(SessionManager.token!!)
        }
    }

    // Notificaciones in-app del aprendiz (GET /notifications).
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
