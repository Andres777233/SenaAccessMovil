package com.example.sennaccess.data

// Repositorio de ambientes: CRUD admin + pivotes instructor/aprendiz.

class AmbienteRepository {

    // Cualquier rol: todos los ambientes.
    suspend fun getAmbientes(token: String): List<Ambiente> =
        RetrofitClient.conServicio { it.getAmbientes("Bearer $token") }

    // Ambientes del instructor/aprendiz logueado.
    suspend fun getMisAmbientes(token: String): List<Ambiente> =
        RetrofitClient.conServicio { it.getMisAmbientes("Bearer $token") }

    suspend fun getMisAprendices(token: String, ambienteId: Int): List<UsuarioApi> =
        RetrofitClient.conServicio { it.getMisAprendices("Bearer $token", ambienteId) }

    suspend fun addMisAprendiz(token: String, ambienteId: Int, aprendizId: Int): MessageResponse =
        RetrofitClient.conServicio { it.addMisAprendiz("Bearer $token", ambienteId, AmbienteAprendizRequest(aprendizId)) }

    suspend fun removeMisAprendiz(token: String, ambienteId: Int, aprendizId: Int): MessageResponse =
        RetrofitClient.conServicio { it.removeMisAprendiz("Bearer $token", ambienteId, aprendizId) }

    suspend fun createAmbiente(token: String, body: AmbienteRequest): Ambiente =
        RetrofitClient.conServicio { it.createAmbiente("Bearer $token", body) }

    suspend fun updateAmbiente(token: String, id: Int, body: AmbienteRequest): Ambiente =
        RetrofitClient.conServicio { it.updateAmbiente("Bearer $token", id, body) }

    suspend fun deleteAmbiente(token: String, id: Int): MessageResponse =
        RetrofitClient.conServicio { it.deleteAmbiente("Bearer $token", id) }

    suspend fun getAprendicesDeAmbiente(token: String, id: Int): List<UsuarioApi> =
        RetrofitClient.conServicio { it.getAprendicesDeAmbiente("Bearer $token", id) }

    suspend fun addAprendizAAmbiente(token: String, ambienteId: Int, aprendizId: Int): MessageResponse =
        RetrofitClient.conServicio { it.addAprendizAAmbiente("Bearer $token", ambienteId, AmbienteAprendizRequest(aprendizId)) }

    suspend fun removeAprendizDeAmbiente(token: String, ambienteId: Int, aprendizId: Int): MessageResponse =
        RetrofitClient.conServicio { it.removeAprendizDeAmbiente("Bearer $token", ambienteId, aprendizId) }

    suspend fun syncInstructores(token: String, ambienteId: Int, instructorIds: List<Int>): Ambiente =
        RetrofitClient.conServicio { it.syncInstructores("Bearer $token", ambienteId, AmbienteInstructoresRequest(instructorIds)) }
}
