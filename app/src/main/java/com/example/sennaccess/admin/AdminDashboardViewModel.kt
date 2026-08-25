package com.example.sennaccess.admin

// ViewModel del dashboard del ADMINISTRADOR.
// Obtiene del backend el resumen, el historial y el perfil, siempre bajo el
// patrón CargaUiState para que la UI distinga carga, error y datos listos,
// con respaldo a MockData cuando no hay sesión o la API falla.

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
import com.example.sennaccess.data.Role
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

// Modelo de un registro de acceso en el historial del admin (nombre, rol, hora y tipo).
data class RegistroAccesoAdmin(
    val nombre: String,
    val rol: String,
    val hora: String,
    val tipo: String
)

// Modelo del historial del admin: separa los accesos por rol (instructores y aprendices).
data class HistorialAdminData(
    val instructores: List<RegistroAccesoAdmin>,
    val aprendices: List<RegistroAccesoAdmin>
)

class AdminDashboardViewModel : ViewModel() {

    // Repositorios que encapsulan el acceso a la API (ingresos y usuarios).
    private val ingresoRepo = IngresoRepository()
    private val usuarioRepo = UsuarioRepository()
    private val equipoRepo = EquipoRepository()
    private val notificacionRepo = NotificacionRepository()
    private val novedadRepo = NovedadRepository()

    // Estados de la UI como StateFlow: la pantalla observa estas propiedades
    // y reacciona ante Loading/Success/Error sin acoplar la lógica de red a Compose.
    private val _resumen = MutableStateFlow<CargaUiState<List<Ingreso>>>(CargaUiState.Loading)
    val resumen: StateFlow<CargaUiState<List<Ingreso>>> = _resumen.asStateFlow()

    private val _historial = MutableStateFlow<CargaUiState<HistorialAdminData>>(CargaUiState.Loading)
    val historial: StateFlow<CargaUiState<HistorialAdminData>> = _historial.asStateFlow()

    private val _perfil = MutableStateFlow<CargaUiState<UsuarioApi>>(CargaUiState.Loading)
    val perfil: StateFlow<CargaUiState<UsuarioApi>> = _perfil.asStateFlow()

    private val _roles = MutableStateFlow<CargaUiState<List<Role>>>(CargaUiState.Loading)
    val roles: StateFlow<CargaUiState<List<Role>>> = _roles.asStateFlow()

    private val _usuarios = MutableStateFlow<CargaUiState<List<UsuarioApi>>>(CargaUiState.Loading)
    val usuarios: StateFlow<CargaUiState<List<UsuarioApi>>> = _usuarios.asStateFlow()

    private val _equipos = MutableStateFlow<CargaUiState<List<IngresoEquipo>>>(CargaUiState.Loading)
    val equipos: StateFlow<CargaUiState<List<IngresoEquipo>>> = _equipos.asStateFlow()

    private val _notificaciones = MutableStateFlow<CargaUiState<List<Notificacion>>>(CargaUiState.Loading)
    val notificaciones: StateFlow<CargaUiState<List<Notificacion>>> = _notificaciones.asStateFlow()

    private val _novedades = MutableStateFlow<CargaUiState<List<Novedad>>>(CargaUiState.Loading)
    val novedades: StateFlow<CargaUiState<List<Novedad>>> = _novedades.asStateFlow()

    init {
        // Al crear el ViewModel se disparan las cargas iniciales en paralelo.
        cargarResumen()
        cargarHistorial()
        cargarPerfil()
        cargarRoles()
        cargarUsuarios()
        cargarEquipos()
        cargarNotificaciones()
        cargarNovedades()
    }

    // Carga el resumen de ingresos del día; si no hay sesión o la API falla,
    // se entregan los datos de ejemplo de MockData.
    fun cargarResumen() {
        cargarConFallback(fallback = { MockData.ingresos }, setState = { _resumen.value = it }) {
            ingresoRepo.getIngresos(SessionManager.token!!)
        }
    }

    // Carga ingresos y usuarios, cruza el rol de cada uno y construye el historial
    // separado por instructores/aprendices. Fallback a mock en modo demo o si falla.
    fun cargarHistorial() {
        cargarConFallback(fallback = { buildHistorial(MockData.ingresos, emptyMap()) }, setState = { _historial.value = it }) {
            val ingresos = ingresoRepo.getIngresos(SessionManager.token!!)
            val usuarios = usuarioRepo.getUsers(SessionManager.token!!)
            val rolesPorUsuario = usuarios.associate { it.id_usuario to it.role?.rol_name }
            buildHistorial(ingresos, rolesPorUsuario)
        }
    }

    // Carga el perfil del admin actual desde la API (o el demo de MockData).
    fun cargarPerfil() {
        cargarConFallback(fallback = { MockData.adminDemo }, setState = { _perfil.value = it }) {
            usuarioRepo.getCurrentUser(SessionManager.token!!)
        }
    }

    // Carga el listado completo de usuarios (GET /admin/users); el AdminDashboard
    // lo usa para filtrar instructores en el reporte al instructor.
    fun cargarUsuarios() {
        cargarConFallback(fallback = { MockData.usuarios }, setState = { _usuarios.value = it }) {
            usuarioRepo.getUsers(SessionManager.token!!)
        }
    }

    // Carga el catalogo de roles desde la API (GET /admin/roles); fallback a los
    // roles de ejemplo cuando no hay sesion activa.
    fun cargarRoles() {
        val mockRoles = listOf(MockData.rolAdmin, MockData.rolInstructor, MockData.rolAprendiz)
        cargarConFallback(fallback = { mockRoles }, setState = { _roles.value = it }) {
            usuarioRepo.getRoles(SessionManager.token!!)
        }
    }

    // Carga el inventario completo de equipos del centro (GET /admin/equipment);
    // fallback a los equipos de ejemplo cuando no hay sesión o la API falla.
    fun cargarEquipos() {
        cargarConFallback(fallback = { MockData.equipos }, setState = { _equipos.value = it }) {
            equipoRepo.getEquipment(SessionManager.token!!)
        }
    }

    // Carga las notificaciones del usuario actual (GET /notifications).
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

    // Novedades del centro para el admin (GET /novedades); la vista compartida
    // permite reportar y borrar cualquier novedad (el admin borra las de todos).
    fun cargarNovedades() {
        cargarConFallback(fallback = { MockData.novedades }, setState = { _novedades.value = it }) {
            novedadRepo.getNovedades(SessionManager.token!!)
        }
    }

    // Clasifica cada ingreso según el rol del usuario: prioriza el rol anidado
    // del ingreso y resuelve contra rolesPorUsuario cuando este viene nulo.
    private fun buildHistorial(
        ingresos: List<Ingreso>,
        rolesPorUsuario: Map<Int?, String?>
    ): HistorialAdminData {
        val instructores = mutableListOf<RegistroAccesoAdmin>()
        val aprendices = mutableListOf<RegistroAccesoAdmin>()
        ingresos.forEach { ingreso ->
            val rol = ingreso.user?.role?.rol_name ?: rolesPorUsuario[ingreso.fk_id_user]
            // Enruta el ingreso a la lista correspondiente según su rol.
            when {
                rol.equals("Instructor", ignoreCase = true) ->
                    instructores += RegistroAccesoAdmin(
                        nombre = ingreso.user?.nombreCompleto ?: "Usuario",
                        rol = "Instructor",
                        hora = ingreso.ingreso_datetime ?: "",
                        tipo = ingreso.ingreso_type ?: "Entrada"
                    )
                rol.equals("Aprendiz", ignoreCase = true) ->
                    aprendices += RegistroAccesoAdmin(
                        nombre = ingreso.user?.nombreCompleto ?: "Usuario",
                        rol = "Aprendiz",
                        hora = ingreso.ingreso_datetime ?: "",
                        tipo = ingreso.ingreso_type ?: "Entrada"
                    )
            }
        }
        return HistorialAdminData(instructores, aprendices)
    }
}
