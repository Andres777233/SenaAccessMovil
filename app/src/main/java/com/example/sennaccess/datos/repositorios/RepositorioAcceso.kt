package com.example.sennaccess.datos.repositorios

import com.example.sennaccess.datos.modelos.PeticionValidarAcceso
import com.example.sennaccess.datos.modelos.PeticionValidarCodigoAcceso
import com.example.sennaccess.datos.modelos.PeticionValidarPin
import com.example.sennaccess.datos.modelos.RespuestaValidarAcceso
import com.example.sennaccess.datos.modelos.RespuestaValidarCodigoAcceso
import com.example.sennaccess.datos.red.ClienteApi

// Repositorio del QR de acceso: marca entrada/salida con POST /acceso/validar.
class RepositorioAcceso {

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
}
