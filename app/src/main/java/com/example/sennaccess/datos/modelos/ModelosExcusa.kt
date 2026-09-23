package com.example.sennaccess.datos.modelos

// Modelos de excusas con PIN (instructor crea → admin valida en salida).

import com.google.gson.annotations.SerializedName

// Permiso de salida con PIN de un solo uso y 15 minutos de vigencia.
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
    @SerializedName("aprendiz") val aprendiz: UsuarioApi? = null,
    @SerializedName("ambiente") val ambiente: Ambiente? = null,
    @SerializedName("instructor") val instructor: UsuarioApi? = null
)

// Estado en minúsculas, listo para comparar.
val Excusa.estadoNormalizado: String get() = (estado ?: "").lowercase()
// Indica si el permiso sigue pendiente.
val Excusa.estaPendiente: Boolean get() = estadoNormalizado == "pendiente"

// Datos para generar un permiso de salida.
data class PeticionCrearExcusa(
    @SerializedName("fk_id_aprendiz") val fk_id_aprendiz: Int,
    @SerializedName("fk_id_ambiente") val fk_id_ambiente: Int,
    @SerializedName("motivo") val motivo: String
)

// PIN presentado en portería.
data class PeticionValidarExcusa(
    @SerializedName("pin") val pin: String
)

// Resultado de validar el PIN.
data class RespuestaValidarExcusa(
    @SerializedName("message") val message: String? = null,
    @SerializedName("excusa") val excusa: Excusa? = null,
    @SerializedName("aprendiz") val aprendiz: UsuarioApi? = null
)
