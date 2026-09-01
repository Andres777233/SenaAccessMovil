package com.example.sennaccess.jornada

// ViewModel FSM del módulo de presencia física.
// Fuente de verdad: GET /jornada/estado (hora NTP + reglas del backend).
// El móvil solo recolecta pruebas (QR + ubicación + BSSID) y orquesta la UI.

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sennaccess.data.EstadoJornada
import com.example.sennaccess.data.JornadaEnAulaRequest
import com.example.sennaccess.data.JornadaEstadoResponse
import com.example.sennaccess.data.JornadaQrResponse
import com.example.sennaccess.data.SessionManager
import com.example.sennaccess.ui.CargaUiState
import com.example.sennaccess.ui.cargarConFallback
import com.example.sennaccess.ui.detalleHttp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class JornadaViewModel : ViewModel() {

    private val repo = JornadaRepository()

    // Estado FSM actual del aprendiz.
    private val _estado = MutableStateFlow<CargaUiState<JornadaEstadoResponse>>(CargaUiState.Loading)
    val estado: StateFlow<CargaUiState<JornadaEstadoResponse>> = _estado.asStateFlow()

    // Resultado de la última transición (éxito/error) para mostrar snackbar/dialog.
    private val _operacion = MutableStateFlow<CargaUiState<JornadaEstadoResponse>?>(null)
    val operacion: StateFlow<CargaUiState<JornadaEstadoResponse>?> = _operacion.asStateFlow()

    // QR dinámico para el instructor (polling cada 30s).
    private val _qr = MutableStateFlow<CargaUiState<JornadaQrResponse>>(CargaUiState.Loading)
    val qr: StateFlow<CargaUiState<JornadaQrResponse>> = _qr.asStateFlow()

    // Resultado de emitir permiso de salida anticipada.
    private val _permiso = MutableStateFlow<CargaUiState<com.example.sennaccess.data.EmitirPermisoResponse>?>(null)
    val permiso: StateFlow<CargaUiState<com.example.sennaccess.data.EmitirPermisoResponse>?> = _permiso.asStateFlow()

    init { cargarEstado() }

    // Carga el estado FSM del servidor; sin sesión devuelve un mock REGISTRADO.
    fun cargarEstado() {
        val mock = JornadaEstadoResponse(
            estado = EstadoJornada.REGISTRADO.raw,
            jornada_inicio = "13:00",
            jornada_fin = "18:00",
            descansos = emptyList(),
            ultimo_cambio = null,
            transiciones_permitidas = listOf("EN_AULA"),
            message = "Modo demo: sin sesión"
        )
        cargarConFallback(fallback = { mock }, setState = { _estado.value = it }) {
            repo.getEstado(SessionManager.token!!)
        }
    }

    // Limpia el resultado de la última operación (tras mostrar el snackbar).
    fun limpiarOperacion() { _operacion.value = null }
    fun limpiarPermiso() { _permiso.value = null }
    fun limpiarQr() { _qr.value = CargaUiState.Loading }

    // Intenta EN_AULA: requiere QR escaneado + pruebas de ubicación/red.
    // El contexto se usa para LocationManager y WifiManager (sin dependencias GMS).
    fun marcarEnAula(context: Context, qrCode: String, ambienteId: Int? = null) {
        val token = SessionManager.token
        if (token == null) {
            _operacion.value = CargaUiState.Error("Inicia sesión para registrar presencia.")
            return
        }
        viewModelScope.launch {
            _operacion.value = CargaUiState.Loading
            try {
                // Recolecta pruebas en paralelo (ubicación puede tardar hasta 7s).
                val ubic = obtenerUbicacion(context)
                val wifi = obtenerWifiDato(context)
                val ts = isoNow()
                val req = JornadaEnAulaRequest(
                    qr_code = qrCode,
                    ambiente_id = ambienteId,
                    lat = ubic?.lat,
                    lng = ubic?.lng,
                    precision_m = ubic?.precisionM,
                    bssid = wifi.bssid,
                    ssid = wifi.ssid,
                    ts_cliente = ts,
                    es_mock = ubic?.esMock
                )
                val resp = repo.enAula(token, req)
                _operacion.value = CargaUiState.Success(resp)
                _estado.value = CargaUiState.Success(resp)
            } catch (e: retrofit2.HttpException) {
                _operacion.value = CargaUiState.Error(detalleHttp(e))
            } catch (e: Exception) {
                _operacion.value = CargaUiState.Error("Fallo de conexión: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    // Transiciones simples del FSM (validación de ventana en el servidor).
    fun marcarDescanso(ambienteId: Int? = null) = transicionSimple { repo.descanso(it, ambienteId) }
    fun marcarRegresoAula(ambienteId: Int? = null) = transicionSimple { repo.regresoAula(it, ambienteId) }
    fun marcarFinalizar(ambienteId: Int? = null) = transicionSimple { repo.finalizar(it, ambienteId) }

    // Salida anticipada: requiere token emitido por instructor/admin + motivo.
    fun marcarSalidaAnticipada(permisoToken: String, motivo: String? = null) {
        val token = SessionManager.token ?: run {
            _operacion.value = CargaUiState.Error("Sin sesión")
            return
        }
        if (permisoToken.isBlank()) {
            _operacion.value = CargaUiState.Error("Falta el permiso de salida del instructor.")
            return
        }
        viewModelScope.launch {
            _operacion.value = CargaUiState.Loading
            try {
                val resp = repo.salidaAnticipada(token, permisoToken.trim(), motivo)
                _operacion.value = CargaUiState.Success(resp)
                _estado.value = CargaUiState.Success(resp)
            } catch (e: retrofit2.HttpException) {
                _operacion.value = CargaUiState.Error(detalleHttp(e))
            } catch (e: Exception) {
                _operacion.value = CargaUiState.Error("Fallo de conexión: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    // Instructor/admin: obtiene el QR actual del ambiente para proyectar.
    fun cargarQr(ambienteId: Int? = null) {
        val token = SessionManager.token
        if (token == null) {
            // Mock: genera un code local para demo sin backend.
            val code = generarTotp("JBSWY3DPEHPK3PXP", periodSec = 30) ?: "123456"
            val mock = JornadaQrResponse(code = code, qr_content = construirContenidoQr(code, ambienteId ?: 1), ambiente_id = ambienteId ?: 1, ambiente_nombre = "Ambiente Demo", expira_en = null, periodo_s = 30)
            _qr.value = CargaUiState.Success(mock)
            return
        }
        viewModelScope.launch {
            _qr.value = CargaUiState.Loading
            try {
                val resp = if (ambienteId != null) repo.getQrAula(token, ambienteId) else repo.getQrActual(token, null)
                _qr.value = CargaUiState.Success(resp)
            } catch (e: retrofit2.HttpException) {
                _qr.value = CargaUiState.Error(detalleHttp(e))
            } catch (e: Exception) {
                _qr.value = CargaUiState.Error("Fallo de conexión: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    // Instructor/admin: emite permiso de salida anticipada para un aprendiz.
    fun emitirPermiso(fkIdUsuario: Int, motivo: String, pin: String?) {
        val token = SessionManager.token ?: run {
            _permiso.value = CargaUiState.Error("Sin sesión")
            return
        }
        if (motivo.isBlank()) {
            _permiso.value = CargaUiState.Error("El motivo es obligatorio.")
            return
        }
        viewModelScope.launch {
            _permiso.value = CargaUiState.Loading
            try {
                val resp = repo.emitirPermiso(token, fkIdUsuario, motivo.trim(), pin?.trim()?.takeIf { it.isNotBlank() })
                _permiso.value = CargaUiState.Success(resp)
            } catch (e: retrofit2.HttpException) {
                _permiso.value = CargaUiState.Error(detalleHttp(e))
            } catch (e: Exception) {
                _permiso.value = CargaUiState.Error("Fallo de conexión: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    // Helper genérico para transiciones simples.
    private fun transicionSimple(bloque: suspend (String) -> JornadaEstadoResponse) {
        val token = SessionManager.token ?: run {
            _operacion.value = CargaUiState.Error("Sin sesión")
            return
        }
        viewModelScope.launch {
            _operacion.value = CargaUiState.Loading
            try {
                val resp = bloque(token)
                _operacion.value = CargaUiState.Success(resp)
                _estado.value = CargaUiState.Success(resp)
            } catch (e: retrofit2.HttpException) {
                _operacion.value = CargaUiState.Error(detalleHttp(e))
            } catch (e: Exception) {
                _operacion.value = CargaUiState.Error("Fallo de conexión: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    private fun isoNow(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date())
    }
}
