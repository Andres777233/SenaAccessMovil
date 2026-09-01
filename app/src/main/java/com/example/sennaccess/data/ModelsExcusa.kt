package com.example.sennaccess.data

// Modelos de excusas con PIN (instructor crea → admin valida en salida).

import com.google.gson.annotations.SerializedName

data class Excusa(
    @SerializedName("id_excusa") val id_excusa: Int? = null,
    @SerializedName("fk_id_aprendiz") val fk_id_aprendiz: Int? = null,
    @SerializedName("fk_id_ambiente") val fk_id_ambiente: Int? = null,
    @SerializedName("fk_id_instructor") val fk_id_instructor: Int? = null,
    @SerializedName("motivo") val motivo: String? = null,
    @SerializedName("pin") val pin: String? = null,
    @SerializedName("estado") val estado: String? = null,
    @SerializedName("expira_en") val expira_en: String? = null,
    @SerializedName("usado_en") val usado_en: String? = null,
    @SerializedName("created_at") val created_at: String? = null,
    @SerializedName("updated_at") val updated_at: String? = null,
    val aprendiz: UsuarioApi? = null,
    val ambiente: Ambiente? = null,
    val instructor: UsuarioApi? = null
)

data class CrearExcusaRequest(
    @SerializedName("fk_id_aprendiz") val fk_id_aprendiz: Int,
    @SerializedName("fk_id_ambiente") val fk_id_ambiente: Int,
    @SerializedName("motivo") val motivo: String
)

data class ValidarExcusaRequest(
    @SerializedName("pin") val pin: String
)

data class ValidarExcusaResponse(
    @SerializedName("message") val message: String? = null,
    val excusa: Excusa? = null,
    val aprendiz: UsuarioApi? = null
)
