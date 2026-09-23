package com.example.sennaccess.datos.repositorios

import com.example.sennaccess.datos.modelos.Ambiente
import com.example.sennaccess.datos.red.ClienteApi
import com.example.sennaccess.datos.modelos.PeticionAmbiente
import com.example.sennaccess.datos.modelos.PeticionAmbienteAprendiz
import com.example.sennaccess.datos.modelos.PeticionAmbienteInstructores
import com.example.sennaccess.datos.modelos.RespuestaMensaje
import com.example.sennaccess.datos.modelos.UsuarioApi

// Repositorio de ambientes: CRUD admin + pivotes instructor/aprendiz.

class RepositorioAmbientes {

    // Ambientes registrados (solo admin).
    suspend fun getAmbientes(token: String): List<Ambiente> =
        ClienteApi.conServicio { it.getAmbientes("Bearer $token") }

    // Salones asignados al instructor en sesión.
    suspend fun getMisAmbientes(token: String): List<Ambiente> =
        ClienteApi.conServicio { it.getMisAmbientes("Bearer $token") }

    // Estudiantes del salón indicado.
    suspend fun getMisAprendices(token: String, ambienteId: Int): List<UsuarioApi> =
        ClienteApi.conServicio { it.getMisAprendices("Bearer $token", ambienteId) }

    // Agrega un aprendiz a uno de mis salones.
    suspend fun addMisAprendiz(token: String, ambienteId: Int, aprendizId: Int): RespuestaMensaje =
        ClienteApi.conServicio { it.addMisAprendiz("Bearer $token", ambienteId, PeticionAmbienteAprendiz(aprendizId)) }

    // Quita un aprendiz de uno de mis salones.
    suspend fun removeMisAprendiz(token: String, ambienteId: Int, aprendizId: Int): RespuestaMensaje =
        ClienteApi.conServicio { it.removeMisAprendiz("Bearer $token", ambienteId, aprendizId) }

    // Crea un ambiente con horario y sede (solo admin).
    suspend fun createAmbiente(token: String, body: PeticionAmbiente): Ambiente =
        ClienteApi.conServicio { it.createAmbiente("Bearer $token", body) }

    // Actualiza los datos de un ambiente (solo admin).
    suspend fun updateAmbiente(token: String, id: Int, body: PeticionAmbiente): Ambiente =
        ClienteApi.conServicio { it.updateAmbiente("Bearer $token", id, body) }

    // Elimina un ambiente (solo admin).
    suspend fun deleteAmbiente(token: String, id: Int): RespuestaMensaje =
        ClienteApi.conServicio { it.deleteAmbiente("Bearer $token", id) }

    // Estudiantes de un ambiente (solo admin).
    suspend fun getAprendicesDeAmbiente(token: String, id: Int): List<UsuarioApi> =
        ClienteApi.conServicio { it.getAprendicesDeAmbiente("Bearer $token", id) }

    // Asigna un aprendiz al ambiente (solo admin).
    suspend fun addAprendizAAmbiente(token: String, ambienteId: Int, aprendizId: Int): RespuestaMensaje =
        ClienteApi.conServicio { it.addAprendizAAmbiente("Bearer $token", ambienteId, PeticionAmbienteAprendiz(aprendizId)) }

    // Retira un aprendiz del ambiente (solo admin).
    suspend fun removeAprendizDeAmbiente(token: String, ambienteId: Int, aprendizId: Int): RespuestaMensaje =
        ClienteApi.conServicio { it.removeAprendizDeAmbiente("Bearer $token", ambienteId, aprendizId) }

    // Sincroniza los instructores asignados al ambiente.
    suspend fun syncInstructores(token: String, ambienteId: Int, instructorIds: List<Int>): Ambiente =
        ClienteApi.conServicio { it.syncInstructores("Bearer $token", ambienteId, PeticionAmbienteInstructores(instructorIds)) }
}
