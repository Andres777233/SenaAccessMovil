package com.example.sennaccess.data

// Repositorio de ingresos de personas: separa el listado personal del registro global.

class IngresoRepository {

    // Ingresos del usuario logueado (visible para cualquier rol con sesión).
    suspend fun getMyIngresos(token: String): List<Ingreso> =
        RetrofitClient.conServicio { it.getMyIngresos("Bearer $token") }

    // Registro global de ingresos de todos los usuarios (solo admin). El backend
    // responde paginado; aquí se extrae la lista "data" para la vista.
    suspend fun getIngresos(token: String): List<Ingreso> =
        RetrofitClient.conServicio { it.getIngresos("Bearer $token") }.data ?: emptyList()

    // Descarga el historial completo en formato CSV (solo admin).
    suspend fun exportarCsv(token: String): okhttp3.ResponseBody =
        RetrofitClient.conServicio { it.exportIngresos("Bearer $token") }
}
