package com.example.sennaccess.datos.repositorios

import com.example.sennaccess.datos.modelos.PeticionValidarAcceso
import com.example.sennaccess.datos.modelos.RespuestaValidarAcceso
import com.example.sennaccess.datos.red.ClienteApi

// Repositorio del QR de acceso: marca entrada/salida con POST /acceso/validar.
class RepositorioAcceso {

    suspend fun validar(token: String, qrPayload: String): RespuestaValidarAcceso =
        ClienteApi.conServicio { it.validarAcceso("Bearer $token", PeticionValidarAcceso(qrPayload)) }
}
