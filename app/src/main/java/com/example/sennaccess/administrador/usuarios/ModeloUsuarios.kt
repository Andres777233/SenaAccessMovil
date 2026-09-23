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

    // Recarga la lista de usuarios.
    fun cargarUsuarios() {
        cargarConRespaldo(fallback = { DatosSimulados.usuarios }, setState = { _uiState.value = it }) {
            repository.getUsers(GestorSesion.token!!)
        }
    }

    // Elimina un usuario por id (DELETE /admin/users/{id}) y refresca la lista.
    suspend fun eliminarUsuario(id: Int) {
        val token = GestorSesion.token ?: return
        repository.eliminarUsuario(token, id)
        cargarUsuarios()
    }
}
