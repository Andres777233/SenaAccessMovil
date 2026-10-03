package com.example.sennaccess.administrador.panel

// ViewModel del dashboard del ADMINISTRADOR.
// Obtiene del backend el resumen, el historial y el perfil, siempre bajo el
// patrón EstadoCarga para que la UI distinga carga, error y datos listos,
// con respaldo a DatosSimulados cuando no hay sesión o la API falla.

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
import com.example.sennaccess.datos.modelos.Ambiente
import com.example.sennaccess.datos.repositorios.RepositorioAmbientes
import com.example.sennaccess.datos.modelos.Presente
import com.example.sennaccess.datos.modelos.Rol
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

data class RegistroAcceso(
    val nombre: String,
    val rol: String,
    val hora: String,
    val tipo: String
)

data class DatosHistorial(
    val instructores: List<RegistroAcceso>,
    val aprendices: List<RegistroAcceso>
)

// Datos del panel admin: resumen, historial, presentes y catálogos.
class PanelAdministradorViewModel : ViewModel() {

    private val ingresoRepo = RepositorioIngresos()
    private val usuarioRepo = RepositorioUsuarios()
    private val equipoRepo = RepositorioEquipos()
    private val notificacionRepo = RepositorioNotificaciones()
    private val novedadRepo = RepositorioNovedades()

    private val _resumen = MutableStateFlow<EstadoCarga<List<Ingreso>>>(EstadoCarga.Loading)
    val resumen: StateFlow<EstadoCarga<List<Ingreso>>> = _resumen.asStateFlow()

    private val _historial = MutableStateFlow<EstadoCarga<DatosHistorial>>(EstadoCarga.Loading)
    val historial: StateFlow<EstadoCarga<DatosHistorial>> = _historial.asStateFlow()

    private val _perfil = MutableStateFlow<EstadoCarga<UsuarioApi>>(EstadoCarga.Loading)
    val perfil: StateFlow<EstadoCarga<UsuarioApi>> = _perfil.asStateFlow()

    private val _roles = MutableStateFlow<EstadoCarga<List<Rol>>>(EstadoCarga.Loading)
    val roles: StateFlow<EstadoCarga<List<Rol>>> = _roles.asStateFlow()

    private val _usuarios = MutableStateFlow<EstadoCarga<List<UsuarioApi>>>(EstadoCarga.Loading)
    val usuarios: StateFlow<EstadoCarga<List<UsuarioApi>>> = _usuarios.asStateFlow()

    private val _equipos = MutableStateFlow<EstadoCarga<List<EquipoIngreso>>>(EstadoCarga.Loading)
    val equipos: StateFlow<EstadoCarga<List<EquipoIngreso>>> = _equipos.asStateFlow()

    private val _notificaciones = MutableStateFlow<EstadoCarga<List<Notificacion>>>(EstadoCarga.Loading)
    val notificaciones: StateFlow<EstadoCarga<List<Notificacion>>> = _notificaciones.asStateFlow()

    private val _novedades = MutableStateFlow<EstadoCarga<List<Novedad>>>(EstadoCarga.Loading)
    val novedades: StateFlow<EstadoCarga<List<Novedad>>> = _novedades.asStateFlow()

    // Quiénes están DENTRO ahora (GET /admin/presentes).
    private val _presentes = MutableStateFlow<EstadoCarga<List<Presente>>>(EstadoCarga.Loading)
    val presentes: StateFlow<EstadoCarga<List<Presente>>> = _presentes.asStateFlow()

    // Ambientes del centro (GET /admin/ambientes).
    private val _ambientes = MutableStateFlow<EstadoCarga<List<Ambiente>>>(EstadoCarga.Loading)
    val ambientes: StateFlow<EstadoCarga<List<Ambiente>>> = _ambientes.asStateFlow()

    // Arranque mínimo: solo lo que pinta el INICIO (perfil, resumen del día,
    // historial del día, roles para los badges y notificaciones para el contador).
    // El resto (usuarios, equipos, novedades, presentes, ambientes) carga perezoso
    // al entrar a su pestaña para no disparar ~10 llamadas a la vez al abrir.
    init {
        cargarResumen()
        cargarHistorial()
        cargarPerfil()
        cargarRoles()
        cargarNotificaciones()
    }

    // Recarga los contadores del día.
    fun cargarResumen() {
        val hoy = hoyBogota()
        cargarConRespaldo(fallback = { DatosSimulados.ingresos }, setState = { _resumen.value = it }) {
            ingresoRepo.getIngresos(GestorSesion.token!!, desde = hoy, hasta = hoy)
        }
    }

    // Recarga los movimientos del día.
    fun cargarHistorial() {
        val hoy = hoyBogota()
        cargarHistorialRango(hoy, hoy)
    }

    // Mapa rol-por-usuario con TTL de 5 min: el historial lo necesita para
    // clasificar, pero pedir la lista completa en cada recarga duplicaba el
    // GET /admin/users más pesado de la app.
    private var mapaRoles: Map<Int?, String?>? = null
    private var mapaRolesTs = 0L

    // Recarga movimientos entre dos fechas.
    fun cargarHistorialRango(desde: String, hasta: String) {
        cargarConRespaldo(fallback = { buildHistorial(DatosSimulados.ingresos, emptyMap()) }, setState = { _historial.value = it }) {
            val token = GestorSesion.token!!
            val ingresos = ingresoRepo.getIngresos(token, desde = desde, hasta = hasta)
            val ahora = System.currentTimeMillis()
            val roles = mapaRoles?.takeIf { ahora - mapaRolesTs < 5 * 60 * 1000 }
                ?: usuarioRepo.getUsers(token)
                    .associate { it.id_usuario to it.role?.rol_name }
                    .also { mapaRoles = it; mapaRolesTs = ahora }
            buildHistorial(ingresos, roles)
        }
    }

    // Invalida el mapa de roles (tras crear/editar/eliminar usuarios).
    // El TTL de 5 min ya acota la desactualización en uso normal.
    fun invalidarMapaRoles() {
        mapaRoles = null
    }

    private fun hoyBogota(): String {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("America/Bogota"))
        return String.format(
            java.util.Locale.US,
            "%04d-%02d-%02d",
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    // Recarga el perfil del admin.
    fun cargarPerfil() {
        cargarConRespaldo(fallback = { DatosSimulados.adminDemo }, setState = { _perfil.value = it }) {
            usuarioRepo.getCurrentUser(GestorSesion.token!!)
        }
    }

    // Carga el listado completo de usuarios (GET /admin/users); el PanelAdministrador
    fun cargarUsuarios() {
        cargarConRespaldo(fallback = { DatosSimulados.usuarios }, setState = { _usuarios.value = it }) {
            usuarioRepo.getUsers(GestorSesion.token!!)
        }
    }

    // Carga el catalogo de roles desde la API (GET /admin/roles).
    // NO se inventa ningún rol local: el id debe existir en el servidor o la
    // cuenta queda con rol inválido (el id 4 del servidor es Invitado, no Portero).
    fun cargarRoles() {
        val mockRoles = listOf(DatosSimulados.rolAdmin, DatosSimulados.rolInstructor, DatosSimulados.rolAprendiz)
        cargarConRespaldo(fallback = { mockRoles }, setState = { _roles.value = it }) {
            usuarioRepo.getRoles(GestorSesion.token!!)
        }
    }

    // Carga el inventario completo de equipos del centro (GET /admin/equipment);
    fun cargarEquipos() {
        cargarConRespaldo(fallback = { DatosSimulados.equipos }, setState = { _equipos.value = it }) {
            equipoRepo.getEquipment(GestorSesion.token!!)
        }
    }

    // Carga las notificaciones del usuario actual (GET /notifications).
    fun cargarNotificaciones() {
        cargarConRespaldo(fallback = { DatosSimulados.notificaciones }, setState = { _notificaciones.value = it }) {
            notificacionRepo.getNotificaciones(GestorSesion.token!!)
        }
    }

    // Polling silencioso: actualiza sin pasar por Loading para no parpadear.
    // Hace que las notificaciones lleguen en tiempo real a todos los roles.
    fun cargarNotificacionesSilencioso() {
        val token = GestorSesion.token ?: return
        viewModelScope.launch {
            try {
                val lista = notificacionRepo.getNotificaciones(token)
                _notificaciones.value = EstadoCarga.Success(lista)
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

    // Novedades del centro para el admin (GET /novedades); la vista compartida
    fun cargarNovedades() {
        cargarConRespaldo(fallback = { DatosSimulados.novedades }, setState = { _novedades.value = it }) {
            novedadRepo.getNovedades(GestorSesion.token!!)
        }
    }

    // Quiénes están DENTRO ahora (GET /admin/presentes). Sin sesión (demo) se
    fun cargarPresentes() {
        cargarConRespaldo(fallback = { emptyList() }, setState = { _presentes.value = it }) {
            ingresoRepo.getPresentes(GestorSesion.token!!)
        }
    }

    // Recarga los ambientes.
    fun cargarAmbientes() {
        cargarConRespaldo(fallback = { emptyList() }, setState = { _ambientes.value = it }) {
            RepositorioAmbientes().getAmbientes(GestorSesion.token!!)
        }
    }

    private fun buildHistorial(
        ingresos: List<Ingreso>,
        rolesPorUsuario: Map<Int?, String?>
    ): DatosHistorial {
        val instructores = mutableListOf<RegistroAcceso>()
        val aprendices = mutableListOf<RegistroAcceso>()
        ingresos.forEach { ingreso ->
            val rol = ingreso.user?.role?.rol_name ?: rolesPorUsuario[ingreso.fk_id_user]
            when {
                rol.equals("Instructor", ignoreCase = true) ->
                    instructores += RegistroAcceso(
                        nombre = ingreso.user?.nombreCompleto ?: "Usuario",
                        rol = "Instructor",
                        hora = ingreso.ingreso_datetime ?: "",
                        tipo = ingreso.ingreso_type ?: "Entrada"
                    )
                rol.equals("Aprendiz", ignoreCase = true) ->
                    aprendices += RegistroAcceso(
                        nombre = ingreso.user?.nombreCompleto ?: "Usuario",
                        rol = "Aprendiz",
                        hora = ingreso.ingreso_datetime ?: "",
                        tipo = ingreso.ingreso_type ?: "Entrada"
                    )
            }
        }
        return DatosHistorial(instructores, aprendices)
    }
}
