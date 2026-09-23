package com.example.sennaccess.datos.repositorios

import com.example.sennaccess.datos.red.ClienteApi
import com.example.sennaccess.datos.modelos.Novedad
import com.example.sennaccess.datos.modelos.PeticionNovedad
import com.example.sennaccess.datos.modelos.RespuestaMensaje

// Repositorio de novedades: lista las publicadas por el usuario actual y el
// catálogo general (visible solo para el rol admin).

class RepositorioNovedades {

    // Novedades publicadas (solo admin).
    suspend fun getNovedades(token: String): List<Novedad> =
        ClienteApi.conServicio { it.getNovedades("Bearer $token") }

    // Novedades visibles para el usuario actual.
    suspend fun getMyNovedades(token: String): List<Novedad> =
        ClienteApi.conServicio { it.getMyNovedades("Bearer $token") }

    // Publica una novedad (solo admin).
    suspend fun crear(token: String, body: PeticionNovedad): Novedad =
        ClienteApi.conServicio { it.createNovedad("Bearer $token", body) }

    // Borra una novedad (solo admin).
    suspend fun eliminar(token: String, id: Int): RespuestaMensaje =
        ClienteApi.conServicio { it.deleteNovedad("Bearer $token", id) }
}
