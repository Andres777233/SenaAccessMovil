package com.example.sennaccess.administrador.usuarios

// ViewModel para la administración de usuarios: carga el listado de usuarios
// desde la API (o desde mocks sin sesión) y lo expone como EstadoCarga para
// que la vista controle Loading/Error/Success y pueda reintentar.

import androidx.lifecycle.ViewModel
import com.example.sennaccess.datos.sesion.GestorSesion
import com.example.sennaccess.datos.modelos.UsuarioApi
import com.example.sennaccess.datos.repositorios.RepositorioUsuarios
import com.example.sennaccess.datos.simulados.DatosSimulados
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import com.example.sennaccess.comun.EstadoCarga
import com.example.sennaccess.comun.cargarConRespaldo

// Lista de usuarios del admin, con borrado y recarga.
class ModeloUsuarios : ViewModel() {

    private val repository = RepositorioUsuarios()

    private val _uiState = MutableStateFlow<EstadoCarga<List<UsuarioApi>>>(EstadoCarga.Loading)
    val uiState: StateFlow<EstadoCarga<List<UsuarioApi>>> = _uiState.asStateFlow()

    // Cuentas que esperan aprobacion: el listado general NO las trae (el
    // backend las filtra), asi que se piden aparte con estado=pendiente.
    private val _pendientes = MutableStateFlow<List<UsuarioApi>>(emptyList())
    val pendientes: StateFlow<List<UsuarioApi>> = _pendientes.asStateFlow()

    // Recarga la lista de usuarios y las solicitudes pendientes.
    fun cargarUsuarios() {
        cargarConRespaldo(fallback = { DatosSimulados.usuarios }, setState = { _uiState.value = it }) {
            repository.getUsers(GestorSesion.token!!)
        }
        cargarPendientes()
    }

    // Carga las cuentas en espera de aprobacion (bandeja de solicitudes).
    fun cargarPendientes() {
        val token = GestorSesion.token ?: return
        viewModelScope.launch {
            try {
                _pendientes.value = repository.getCuentasPendientes(token)
            } catch (e: Exception) {
                // Sin pendientes no se rompe la pantalla: la bandeja simplemente
                // queda vacia y el aviso real se muestra al aprobar o rechazar.
                _pendientes.value = emptyList()
            }
        }
    }

    // Elimina un usuario por id (DELETE /admin/users/{id}) y refresca la lista.
    suspend fun eliminarUsuario(id: Int) {
        val token = GestorSesion.token ?: return
        repository.eliminarUsuario(token, id)
        cargarUsuarios()
    }

    // Aprueba una cuenta pendiente y refresca la lista.
    suspend fun aprobarCuenta(id: Int): String {
        val token = GestorSesion.token ?: return "No hay sesión activa."
        val respuesta = repository.aprobarCuenta(token, id)
        cargarUsuarios()
        return respuesta.message ?: "Cuenta aprobada."
    }

    // Rechaza la solicitud de una cuenta y refresca la lista.
    suspend fun rechazarCuenta(id: Int): String {
        val token = GestorSesion.token ?: return "No hay sesión activa."
        val respuesta = repository.rechazarCuenta(token, id)
        cargarUsuarios()
        return respuesta.message ?: "Solicitud rechazada."
    }

    // Cierra la sesion del ViewModel: las solicitudes pendientes se limpian.
    fun limpiarPendientes() {
        _pendientes.value = emptyList()
    }
}
