package com.example.sennaccess.ui

// ViewModel para la administración de usuarios: carga el listado de usuarios
// desde la API (o desde mocks sin sesión) y lo expone como CargaUiState para
// que la vista controle Loading/Error/Success y pueda reintentar.

import androidx.lifecycle.ViewModel
import com.example.sennaccess.data.SessionManager
import com.example.sennaccess.data.UsuarioApi
import com.example.sennaccess.data.UsuarioRepository
import com.example.sennaccess.data.mock.MockData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

class UsuariosViewModel : ViewModel() {

    // Repositorio que encapsula los endpoints de usuarios de la API.
    private val repository = UsuarioRepository()

    // Flujo de estado que arranca en Loading y se expone inmutable a la UI.
    private val _uiState = MutableStateFlow<CargaUiState<List<UsuarioApi>>>(CargaUiState.Loading)
    val uiState: StateFlow<CargaUiState<List<UsuarioApi>>> = _uiState.asStateFlow()

    // Carga el listado con cargarConFallback: sin sesión usa mocks y con sesión
    // consulta la API en una corrutina, dejando el estado en Success o Error.
    fun cargarUsuarios() {
        cargarConFallback(fallback = { MockData.usuarios }, setState = { _uiState.value = it }) {
            repository.getUsers(SessionManager.token!!)
        }
    }

    // Elimina un usuario por id (DELETE /admin/users/{id}) y refresca la lista.
    suspend fun eliminarUsuario(id: Int) {
        val token = SessionManager.token ?: return
        repository.eliminarUsuario(token, id)
        cargarUsuarios()
    }
}
