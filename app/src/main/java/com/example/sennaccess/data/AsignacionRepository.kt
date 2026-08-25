package com.example.sennaccess.data

// Repositorio de asignaciones aprendiz→instructor: lista, crea y elimina
// la supervisión de aprendices a través de instructores (solo admin).

class AsignacionRepository {

    suspend fun listar(token: String): List<AprendizInstructor> =
        RetrofitClient.conServicio { it.getAprendizInstructores("Bearer $token") }

    suspend fun crear(token: String, body: AsignacionRequest): AprendizInstructor =
        RetrofitClient.conServicio { it.createAprendizInstructor("Bearer $token", body) }

    suspend fun eliminar(token: String, id: Int): MessageResponse =
        RetrofitClient.conServicio { it.deleteAprendizInstructor("Bearer $token", id) }
}
