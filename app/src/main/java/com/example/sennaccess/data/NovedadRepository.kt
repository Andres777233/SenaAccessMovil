package com.example.sennaccess.data

// Repositorio de novedades: lista las publicadas por el usuario actual y el
// catálogo general (visible solo para el rol admin).

class NovedadRepository {

    // Listado general de novedades del centro (solo admin), con búsqueda opcional.
    suspend fun getNovedades(token: String): List<Novedad> =
        RetrofitClient.conServicio { it.getNovedades("Bearer $token") }

    // Novedades publicadas por el usuario logueado.
    suspend fun getMyNovedades(token: String): List<Novedad> =
        RetrofitClient.conServicio { it.getMyNovedades("Bearer $token") }

    // Publica una nueva novedad (disponible para cualquier rol autenticado).
    suspend fun crear(token: String, body: NovedadRequest): Novedad =
        RetrofitClient.conServicio { it.createNovedad("Bearer $token", body) }

    // Elimina una novedad; el backend solo lo permite al autor o a un admin.
    suspend fun eliminar(token: String, id: Int): MessageResponse =
        RetrofitClient.conServicio { it.deleteNovedad("Bearer $token", id) }
}
