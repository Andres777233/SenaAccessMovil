package com.example.sennaccess.ui

// Define el estado genérico de carga (CargaUiState) y el helper cargarConFallback,
// que centraliza el patrón Loading/Success/Error para todas las pantallas que
// consumen la API, con respaldo a datos de ejemplo cuando no hay sesión.

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sennaccess.data.SessionManager
import kotlinx.coroutines.launch

// Tres estados: carga en curso, éxito con datos o error con mensaje para la UI.
/**
 * Estado genérico de carga para todas las pantallas que consumen la API.
 * Sigue el mismo patrón de LoginUiState/UsuariosUiState pero reutilizable.
 */
sealed class CargaUiState<out T> {
    data object Loading : CargaUiState<Nothing>()
    data class Success<T>(val datos: T) : CargaUiState<T>()
    data class Error(val mensaje: String) : CargaUiState<Nothing>()
}

/**
 * Ejecuta una llamada de red con respaldo a mocks:
 * - Sin sesión activa (botones demo) → devuelve los datos de ejemplo.
 * - Con sesión y la API responde → devuelve los datos reales.
 * - Con sesión y la API falla → estado Error (la vista ofrece reintentar).
 */
fun <T> ViewModel.cargarConFallback(
    fallback: () -> T,
    setState: (CargaUiState<T>) -> Unit,
    llamadaRed: suspend () -> T
) {
    // Sin sesión activa no hay token: se entrega directamente el fallback (mocks)
    // sin tocar la red, garantizando que las pantallas demo sigan funcionando.
    val token = SessionManager.token
    if (token == null) {
        setState(CargaUiState.Success(fallback()))
        return
    }
    // Con sesión se marca Loading y se lanza una corrutina en viewModelScope:
    // la llamada a la API se ejecuta en segundo plano sin bloquear la UI.
    setState(CargaUiState.Loading)
    viewModelScope.launch {
        try {
            setState(CargaUiState.Success(llamadaRed()))
        } catch (e: retrofit2.HttpException) {
            // Errores HTTP: el 401 se traduce a "Sesión expirada" y el resto a su código.
            val msg = if (e.code() == 401) "Sesión expirada" else "Error ${e.code()}"
            setState(CargaUiState.Error(msg))
        } catch (e: Exception) {
            // Cualquier otro fallo (red, formato, etc.) se muestra como error genérico;
            // la vista ofrece reintentar para volver a intentar la llamada.
            setState(CargaUiState.Error("No se pudo conectar al servidor"))
        }
    }
}
