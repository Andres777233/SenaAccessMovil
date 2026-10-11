package com.example.sennaccess.datos.repositorios

import com.example.sennaccess.datos.modelos.RespuestaPrediccion
import com.example.sennaccess.datos.red.ClienteApi

// Pronóstico de accesos diarios: GET /api/admin/prediccion (solo superadmin).
class RepositorioPrediccion {

    suspend fun obtener(token: String, dias: Int = 7): RespuestaPrediccion =
        ClienteApi.conServicio { it.getPrediccion("Bearer $token", dias) }
}
