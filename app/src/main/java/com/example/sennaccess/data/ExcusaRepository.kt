package com.example.sennaccess.data

// Repositorio de excusas con PIN.

class ExcusaRepository {

    suspend fun crear(token: String, aprendizId: Int, ambienteId: Int, motivo: String): Excusa =
        RetrofitClient.conServicio { it.crearExcusa("Bearer $token", CrearExcusaRequest(aprendizId, ambienteId, motivo)) }

    suspend fun misComoInstructor(token: String): List<Excusa> =
        RetrofitClient.conServicio { it.getExcusasInstructor("Bearer $token") }

    suspend fun anular(token: String, id: Int): MessageResponse =
        RetrofitClient.conServicio { it.anularExcusa("Bearer $token", id) }

    suspend fun misExcusas(token: String): List<Excusa> =
        RetrofitClient.conServicio { it.getMisExcusas("Bearer $token") }

    suspend fun validar(token: String, pin: String): ValidarExcusaResponse =
        RetrofitClient.conServicio { it.validarExcusa("Bearer $token", ValidarExcusaRequest(pin)) }

    suspend fun todasAdmin(token: String): List<Excusa> =
        RetrofitClient.conServicio { it.getExcusasAdmin("Bearer $token") }
}
