package com.example.sennaccess.datos.repositorios

import com.example.sennaccess.datos.red.ClienteApi
import com.example.sennaccess.datos.modelos.Ingreso
import com.example.sennaccess.datos.modelos.Presente

// Repositorio de ingresos de personas: separa el listado personal del registro global.

class RepositorioIngresos {

    // Ingresos del usuario logueado (visible para cualquier rol con sesión).
    suspend fun getMyIngresos(token: String): List<Ingreso> =
        ClienteApi.conServicio { it.getMyIngresos("Bearer $token") }

    // Movimientos globales con rango opcional de fechas (solo admin).
    suspend fun getIngresos(token: String, desde: String? = null, hasta: String? = null): List<Ingreso> =
        ClienteApi.conServicio {
            it.getIngresos("Bearer $token", desde = desde, hasta = hasta)
        }.data ?: emptyList()

    // Descarga el historial en CSV, Excel o PDF con las columnas elegidas.
    suspend fun exportarHistorial(token: String, formato: String = "csv", cols: String? = null, desde: String? = null, hasta: String? = null): okhttp3.ResponseBody =
        ClienteApi.conServicio { it.exportIngresos("Bearer $token", formato = formato, cols = cols, desde = desde, hasta = hasta) }

    // Usuarios cuya última marca es entrada (quién está dentro).
    suspend fun getPresentes(token: String): List<Presente> =
        ClienteApi.conServicio { it.getPresentes("Bearer $token") }
}
