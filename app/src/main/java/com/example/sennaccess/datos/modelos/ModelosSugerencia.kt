package com.example.sennaccess.datos.modelos

import com.google.gson.annotations.SerializedName

// Sugerencia del buzón: refleja la tabla `sugerencias` del backend
// (SugerenciaController + modelo Sugerencia del WEB).

data class Sugerencia(
    @SerializedName("id_sugerencia") val id_sugerencia: Int? = null,
    @SerializedName("sugerencia_asunto") val sugerencia_asunto: String? = null,
    @SerializedName("sugerencia_body") val sugerencia_body: String? = null,
    @SerializedName("sugerencia_categoria") val sugerencia_categoria: String? = null,
    @SerializedName("sugerencia_status") val sugerencia_status: String? = null,
    @SerializedName("respuesta_admin") val respuesta_admin: String? = null,
    @SerializedName("responded_at") val responded_at: String? = null,
    @SerializedName("fk_id_usuario") val fk_id_usuario: Int? = null,
    @SerializedName("created_at") val created_at: String? = null,
    val user: UsuarioApi? = null
)

// Datos para POST /api/sugerencias (tope de 3 por día, lo valida el servidor).
data class PeticionSugerencia(
    @SerializedName("sugerencia_asunto") val sugerencia_asunto: String,
    @SerializedName("sugerencia_body") val sugerencia_body: String,
    @SerializedName("sugerencia_categoria") val sugerencia_categoria: String
)

// Datos para PUT /api/admin/sugerencias/{id}/responder.
data class PeticionResponderSugerencia(
    @SerializedName("respuesta_admin") val respuesta_admin: String,
    @SerializedName("sugerencia_status") val sugerencia_status: String
)

// Envoltorio paginado de GET /api/sugerencias (paginate de Laravel).
data class RespuestaPaginaSugerencias(
    @SerializedName("data") val data: List<Sugerencia>? = null
)
