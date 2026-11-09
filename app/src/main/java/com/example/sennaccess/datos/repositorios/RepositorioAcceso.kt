package com.example.sennaccess.datos.repositorios

import com.example.sennaccess.datos.modelos.PeticionPinPorteria
import com.example.sennaccess.datos.modelos.PeticionValidarAcceso
import com.example.sennaccess.datos.modelos.PeticionValidarCarnet
import com.example.sennaccess.datos.modelos.PeticionValidarCodigoAcceso
import com.example.sennaccess.datos.modelos.PeticionValidarPin
import com.example.sennaccess.datos.modelos.EstadoPuerta
import com.example.sennaccess.datos.modelos.RespuestaPinPorteria
import com.example.sennaccess.datos.modelos.RespuestaQrPorteria
import com.example.sennaccess.datos.modelos.RespuestaValidarAcceso
import com.example.sennaccess.datos.modelos.RespuestaValidarCarnet
import com.example.sennaccess.datos.modelos.RespuestaValidarCodigoAcceso
import com.example.sennaccess.datos.red.ClienteApi

// Repositorio del QR de acceso: marca entrada/salida con POST /acceso/validar.
class RepositorioAcceso {

    // Estado barato de la puerta: evita descargar todo el historial solo para
    // saber si la persona ya está dentro hoy.
    suspend fun estado(token: String): EstadoPuerta =
        ClienteApi.conServicio { it.getEstadoAcceso("Bearer $token") }

    // QR rotativo de portería: entrada o salida.
    suspend fun qrPorteria(token: String, tipo: String): RespuestaQrPorteria =
        ClienteApi.conServicio { it.getQrPorteria("Bearer $token", tipo) }

    // PIN de 6 dígitos para dictar en portería.
    suspend fun generarPinPorteria(token: String, tipo: String): RespuestaPinPorteria =
        ClienteApi.conServicio {
            it.generarPinPorteria("Bearer $token", PeticionPinPorteria(tipo))
        }

    suspend fun validar(token: String, qrPayload: String): RespuestaValidarAcceso =
        ClienteApi.conServicio { it.validarAcceso("Bearer $token", PeticionValidarAcceso(qrPayload)) }

    // PIN corto dictado en portería (fallback sin cámara).
    suspend fun validarPin(token: String, pin: String): RespuestaValidarAcceso =
        ClienteApi.conServicio { it.validarPinAcceso("Bearer $token", PeticionValidarPin(pin)) }

    // Código de entrada generado en la web: QR escaneado o PIN dictado.
    suspend fun validarCodigo(token: String, codigo: String? = null, pin: String? = null): RespuestaValidarCodigoAcceso =
        ClienteApi.conServicio {
            it.validarCodigoAcceso("Bearer $token", PeticionValidarCodigoAcceso(codigo = codigo, pin = pin))
        }

    // Carnet digital: registra la Entrada del dueño del QR (web o móvil).
    suspend fun validarCarnet(token: String, carnet: String): RespuestaValidarCarnet =
        ClienteApi.conServicio {
            it.validarCarnetAcceso("Bearer $token", PeticionValidarCarnet(carnet))
        }
}
