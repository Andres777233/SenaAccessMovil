package com.example.sennaccess.datos.modelos

import com.google.gson.annotations.SerializedName

// Pronóstico de accesos diarios (GET /api/admin/prediccion, solo superadmin).
data class PrediccionDia(
    @SerializedName("fecha") val fecha: String,
    @SerializedName("dia") val dia: String,
    @SerializedName("predicho") val predicho: Int,
    @SerializedName("min") val minimo: Int,
    @SerializedName("max") val maximo: Int
)

// Respuesta del pronóstico: predicciones+MAE/R² o aviso de datos insuficientes.
data class RespuestaPrediccion(
    @SerializedName("predicciones") val predicciones: List<PrediccionDia>? = null,
    @SerializedName("mae") val mae: Double? = null,
    @SerializedName("r2") val r2: Double? = null,
    @SerializedName("filas") val filas: Int? = null,
    @SerializedName("entrenado_en") val entrenadoEn: String? = null,
    @SerializedName("aviso") val aviso: String? = null
)
