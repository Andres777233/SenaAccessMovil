package com.example.sennaccess.ui

// ViewModel del login: expone el estado de autenticación (LoginUiState) a la UI,
// realiza la llamada a la API mediante AuthRepository y persiste la sesión en
// SessionManager cuando las credenciales son válidas.

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sennaccess.data.AuthRepository
import com.example.sennaccess.data.LoginResponse
import com.example.sennaccess.data.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Estados de la pantalla de login: reposo, autenticando, éxito o error.
sealed class LoginUiState {
    data object Idle : LoginUiState()
    data object Loading : LoginUiState()
    data class Success(val response: LoginResponse) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel : ViewModel() {

    // Repositorio que encapsula la llamada Retrofit del endpoint de login.
    private val repository = AuthRepository()

    // Estado observable por la UI; el MutableStateFlow privado solo lo muta este ViewModel.
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // Respuesta y token del último login, útiles para la navegación posterior.
    var lastResponse: LoginResponse? = null
        private set

    var token: String? = null
        private set

    // Lanza la autenticación en una corrutina (viewModelScope): marca Loading,
    // consulta la API, guarda token y datos de usuario y notifica el resultado.
    fun login(email: String, password: String) {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            try {
                val response = repository.login(email.trim(), password)
                lastResponse = response
                token = response.access_token
                SessionManager.saveSession(
                    response.access_token,
                    response.user?.id_usuario,
                    response.user?.user_name,
                    response.user?.user_email,
                    response.role
                )
                SessionManager.savePhoto(response.user?.profile_photo_path)
                _uiState.value = LoginUiState.Success(response)
            } catch (e: retrofit2.HttpException) {
                val msg = if (e.code() == 401) "Credenciales incorrectas" else "Error ${e.code()}"
                _uiState.value = LoginUiState.Error(msg)
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error("No se pudo conectar al servidor")
            }
        }
    }

    // Vuelve el estado al reposo, usado al limpiar o regresar a la pantalla de login.
    fun reset() {
        _uiState.value = LoginUiState.Idle
    }

    // Registra el éxito de un login con passkey (la verificación la hizo el
    // servidor): guarda la sesión y notifica a la UI para navegar al dashboard.
    fun onPasskeySuccess(response: LoginResponse) {
        lastResponse = response
        token = response.access_token
        SessionManager.saveSession(
            response.access_token,
            response.user?.id_usuario,
            response.user?.user_name,
            response.user?.user_email,
            response.role
        )
        SessionManager.savePhoto(response.user?.profile_photo_path)
        _uiState.value = LoginUiState.Success(response)
    }
}