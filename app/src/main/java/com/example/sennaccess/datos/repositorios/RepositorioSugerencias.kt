package com.example.sennaccess.datos.repositorios

import com.example.sennaccess.datos.red.ClienteApi
import com.example.sennaccess.datos.modelos.PeticionResponderSugerencia
import com.example.sennaccess.datos.modelos.PeticionSugerencia
import com.example.sennaccess.datos.modelos.RespuestaMensaje
import com.example.sennaccess.datos.modelos.Sugerencia

// Repositorio del buzón de sugerencias: el usuario ve las propias vía
// /my-sugerencias y el admin la bandeja completa (filtrable) vía /sugerencias.

class RepositorioSugerencias {

    // Sugerencias del usuario actual.
    suspend fun getMisSugerencias(token: String): List<Sugerencia> =
        ClienteApi.conServicio { it.getMisSugerencias("Bearer $token") }

    // Bandeja del admin (respuesta paginada: se devuelve solo la lista).
    suspend fun getSugerenciasAdmin(
        token: String,
        q: String? = null,
        status: String? = null,
        categoria: String? = null
    ): List<Sugerencia> =
        ClienteApi.conServicio {
            it.getSugerencias("Bearer $token", q, status, categoria).data.orEmpty()
        }

    // Envía una sugerencia (cualquier rol autenticado).
    suspend fun crear(token: String, body: PeticionSugerencia): Sugerencia =
        ClienteApi.conServicio { it.crearSugerencia("Bearer $token", body) }

    // Responde una sugerencia y/o actualiza su estado (solo admin).
    suspend fun responder(token: String, id: Int, body: PeticionResponderSugerencia): Sugerencia =
        ClienteApi.conServicio { it.responderSugerencia("Bearer $token", id, body) }

    // Elimina una sugerencia (solo admin).
    suspend fun eliminar(token: String, id: Int): RespuestaMensaje =
        ClienteApi.conServicio { it.eliminarSugerencia("Bearer $token", id) }
}
