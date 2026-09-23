package com.example.sennaccess.datos.repositorios

import com.example.sennaccess.datos.red.ClienteApi
import com.example.sennaccess.datos.modelos.Excusa
import com.example.sennaccess.datos.modelos.PeticionCrearExcusa
import com.example.sennaccess.datos.modelos.PeticionValidarExcusa
import com.example.sennaccess.datos.modelos.RespuestaMensaje
import com.example.sennaccess.datos.modelos.RespuestaValidarExcusa

// Repositorio de excusas con PIN.

class RepositorioExcusas {

    // Crea un permiso de salida y devuelve su PIN de un solo uso.
    suspend fun crear(token: String, aprendizId: Int, ambienteId: Int, motivo: String): Excusa =
        ClienteApi.conServicio { it.crearExcusa("Bearer $token", PeticionCrearExcusa(aprendizId, ambienteId, motivo)) }

    // Permisos generados por el instructor actual.
    suspend fun misComoInstructor(token: String): List<Excusa> =
        ClienteApi.conServicio { it.getExcusasInstructor("Bearer $token") }

    // Anula un permiso aún no usado.
    suspend fun anular(token: String, id: Int): RespuestaMensaje =
        ClienteApi.conServicio { it.anularExcusa("Bearer $token", id) }

    // Permisos del aprendiz en sesión.
    suspend fun misExcusas(token: String): List<Excusa> =
        ClienteApi.conServicio { it.getMisExcusas("Bearer $token") }

    // Valida el PIN en portería y registra la salida.
    suspend fun validar(token: String, pin: String): RespuestaValidarExcusa =
        ClienteApi.conServicio { it.validarExcusa("Bearer $token", PeticionValidarExcusa(pin)) }

    // Todos los permisos (solo admin).
    suspend fun todasAdmin(token: String): List<Excusa> =
        ClienteApi.conServicio { it.getExcusasAdmin("Bearer $token") }
}
