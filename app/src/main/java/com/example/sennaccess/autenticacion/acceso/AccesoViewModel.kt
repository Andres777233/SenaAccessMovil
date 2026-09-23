package com.example.sennaccess.autenticacion.acceso

// ViewModel del login: expone el estado de autenticación (EstadoAcceso) a la UI,
// realiza la llamada a la API mediante RepositorioAutenticacion y persiste la sesión en
// GestorSesion cuando las credenciales son válidas.

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sennaccess.datos.repositorios.RepositorioAutenticacion
import com.example.sennaccess.datos.modelos.RespuestaAcceso
import com.example.sennaccess.datos.sesion.RolSeguro
import com.example.sennaccess.datos.sesion.GestorSesion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.sennaccess.comun.detalleHttp

sealed class EstadoAcceso {
    data object Idle : EstadoAcceso()
    data object Loading : EstadoAcceso()
    data class Success(val response: RespuestaAcceso) : EstadoAcceso()
    data class Error(val message: String) : EstadoAcceso()
}

// Estado del acceso: login, reto 2FA y limpieza.
class AccesoViewModel : ViewModel() {

    private val repository = RepositorioAutenticacion()

    private val _uiState = MutableStateFlow<EstadoAcceso>(EstadoAcceso.Idle)
    val uiState: StateFlow<EstadoAcceso> = _uiState.asStateFlow()

    var lastResponse: RespuestaAcceso? = null
        private set

    var token: String? = null
        private set

    // Accede con correo y clave; deriva al 2FA si el backend lo exige.
    fun login(email: String, password: String, deviceId: String? = null) {
        _uiState.value = EstadoAcceso.Loading
        GestorSesion.clear()
        token = null
        lastResponse = null
        viewModelScope.launch {
            try {
                val response = repository.login(email.trim(), password, deviceId)
                lastResponse = response
                if (response.two_factor_required == true) {
                    _uiState.value = EstadoAcceso.Success(response)
                    return@launch
                }
                if (!RolSeguro.esValido(response.role)) {
                    GestorSesion.clear()
                    _uiState.value = EstadoAcceso.Error("Tu cuenta no tiene un rol válido. Contacta al administrador.")
                    return@launch
                }
                token = response.access_token
                GestorSesion.saveSession(
                    response.access_token,
                    response.user?.id_usuario,
                    response.user?.user_name,
                    response.user?.user_email,
                    response.role,
                    response.user?.email_verified_at != null
                )
                GestorSesion.savePhoto(response.user?.profile_photo_path)
                _uiState.value = EstadoAcceso.Success(response)
            } catch (e: retrofit2.HttpException) {
                _uiState.value = EstadoAcceso.Error(detalleHttp(e))
            } catch (e: Exception) {
                _uiState.value = EstadoAcceso.Error("No se pudo conectar al servidor")
            }
        }
    }

    // Limpia el estado para un nuevo intento.
    fun reset() {
        _uiState.value = EstadoAcceso.Idle
    }
}
