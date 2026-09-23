package com.example.sennaccess.comun

// Define el estado genérico de carga (EstadoCarga) y el helper cargarConRespaldo,
// que centraliza el patrón Loading/Success/Error para todas las pantallas que
// consumen la API. Sin sesión NO hay datos: se devuelve Error para no pintar
// nunca el perfil ni los datos de otra persona o de ejemplo como si fueran reales.

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sennaccess.datos.sesion.GestorSesion
import kotlinx.coroutines.launch

// Tres estados: carga en curso, éxito con datos o error con mensaje para la UI.

sealed class EstadoCarga<out T> {
    data object Loading : EstadoCarga<Nothing>()
    data class Success<T>(val datos: T) : EstadoCarga<T>()
    data class Error(val mensaje: String) : EstadoCarga<Nothing>()
}

fun <T> ViewModel.cargarConRespaldo(
    fallback: () -> T,
    setState: (EstadoCarga<T>) -> Unit,
    llamadaRed: suspend () -> T
) {
    val token = GestorSesion.token
    if (token == null) {
        setState(EstadoCarga.Error("Sesión expirada. Inicia sesión de nuevo."))
        return
    }
    setState(EstadoCarga.Loading)
    viewModelScope.launch {
        try {
            setState(EstadoCarga.Success(llamadaRed()))
        } catch (e: retrofit2.HttpException) {
            val msg = if (e.code() == 401) "Sesión expirada" else detalleHttp(e)
            setState(EstadoCarga.Error(msg))
        } catch (e: Exception) {
            setState(EstadoCarga.Error("Fallo de conexión: ${e.message ?: e.javaClass.simpleName}"))
        }
    }
}

fun detalleHttp(e: retrofit2.HttpException): String {
    return try {
        val json = org.json.JSONObject(e.response()?.errorBody()?.string() ?: "{}")
        val errores = json.optJSONObject("errors")
        val primero = if (errores != null && errores.length() > 0) {
            val campo = errores.keys().next()
            errores.optJSONArray(campo)?.getString(0) ?: errores.optString(campo)
        } else null
        val texto = primero ?: json.optString("message").takeIf { it.isNotBlank() } ?: e.message().orEmpty()
        "Error ${e.code()}: $texto".trimEnd()
    } catch (_: Exception) {
        "Error ${e.code()}: ${e.message().orEmpty()}".trimEnd()
    }
}
