package com.example.sennaccess.data

// Modelos del módulo de presencia física y gestión de jornada (FSM).
// Contrato API para el backend WEB (NTP, validación geo/red, TOTP, audit trail).
// El servidor es la fuente de verdad; el móvil solo orquesta transiciones.

import com.google.gson.annotations.SerializedName

// FSM de la jornada del aprendiz: flujo formal controlado por el backend.
enum class EstadoJornada(val raw: String) {
    REGISTRADO("REGISTRADO"),
    EN_AULA("EN_AULA"),
    EN_DESCANSO("EN_DESCANSO"),
    FINALIZADO("FINALIZADO"),
    ABANDONO_UNILATERAL("ABANDONO_UNILATERAL"),
    SALIDA_ANTICIPADA("SALIDA_ANTICIPADA");

    companion object {
        fun fromRaw(v: String?): EstadoJornada = entries.find { it.raw.equals(v, true) } ?: REGISTRADO
    }

    // Etiqueta legible para la UI.
    fun label(): String = when (this) {
        REGISTRADO -> "Registrado"
        EN_AULA -> "En aula"
        EN_DESCANSO -> "En descanso"
        FINALIZADO -> "Finalizado"
        ABANDONO_UNILATERAL -> "Abandono"
        SALIDA_ANTICIPADA -> "Salida anticipada"
    }

    // Si la jornada ya terminó (estado absorbente).
    fun esFinal(): Boolean = this == FINALIZADO || this == ABANDONO_UNILATERAL || this == SALIDA_ANTICIPADA
}

// Ventana de receso predefinida por el backend (regla de negocio por ambiente/jornada).
data class VentanaDescanso(
    @SerializedName("inicio") val inicio: String? = null,
    @SerializedName("fin") val fin: String? = null
)

// Respuesta de GET /jornada/estado: estado FSM actual + configuración de la jornada.
data class JornadaEstadoResponse(
    @SerializedName("estado") val estado: String? = null,
    @SerializedName("ambiente_id") val ambiente_id: Int? = null,
    @SerializedName("ambiente_nombre") val ambiente_nombre: String? = null,
    @SerializedName("jornada_inicio") val jornada_inicio: String? = null,
    @SerializedName("jornada_fin") val jornada_fin: String? = null,
    @SerializedName("descansos") val descansos: List<VentanaDescanso>? = null,
    @SerializedName("ultimo_cambio") val ultimo_cambio: String? = null,
    @SerializedName("transiciones_permitidas") val transiciones_permitidas: List<String>? = null,
    @SerializedName("permiso_token") val permiso_token: String? = null,
    @SerializedName("message") val message: String? = null
) {
    val estadoEnum: EstadoJornada get() = EstadoJornada.fromRaw(estado)
}

// Payload para POST /jornada/en-aula (prueba de presencia con 3 factores).
data class JornadaEnAulaRequest(
    @SerializedName("qr_code") val qr_code: String,
    @SerializedName("ambiente_id") val ambiente_id: Int? = null,
    @SerializedName("lat") val lat: Double? = null,
    @SerializedName("lng") val lng: Double? = null,
    @SerializedName("precision_m") val precision_m: Float? = null,
    @SerializedName("bssid") val bssid: String? = null,
    @SerializedName("ssid") val ssid: String? = null,
    @SerializedName("ts_cliente") val ts_cliente: String? = null,
    @SerializedName("es_mock") val es_mock: Boolean? = null
)

// Transición simple sin QR (descanso, regreso, finalizar).
data class JornadaTransicionRequest(
    @SerializedName("ambiente_id") val ambiente_id: Int? = null,
    @SerializedName("motivo") val motivo: String? = null
)

// POST /jornada/salida-anticipada: exige token del instructor/admin + motivo.
data class SalidaAnticipadaRequest(
    @SerializedName("permiso_token") val permiso_token: String,
    @SerializedName("motivo") val motivo: String? = null
)

// POST /jornada/emitir-permiso: el instructor/admin autoriza la salida.
data class EmitirPermisoRequest(
    @SerializedName("fk_id_usuario") val fk_id_usuario: Int,
    @SerializedName("motivo") val motivo: String,
    @SerializedName("pin") val pin: String? = null
)

// Respuesta de emitir-permiso: token de corta vida + auditoría.
data class EmitirPermisoResponse(
    @SerializedName("permiso_token") val permiso_token: String? = null,
    @SerializedName("expira_en") val expira_en: String? = null,
    @SerializedName("message") val message: String? = null
)

// GET /jornada/qr/{ambienteId} o /jornada/qr-actual: código TOTP actual para proyectar.
data class JornadaQrResponse(
    @SerializedName("code") val code: String? = null,
    @SerializedName("qr_content") val qr_content: String? = null,
    @SerializedName("ambiente_id") val ambiente_id: Int? = null,
    @SerializedName("ambiente_nombre") val ambiente_nombre: String? = null,
    @SerializedName("expira_en") val expira_en: String? = null,
    @SerializedName("periodo_s") val periodo_s: Int? = null
)

// Entrada de auditoría de salida anticipada (GET /jornada/auditoria, solo admin).
data class AuditoriaSalida(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("fk_id_usuario") val fk_id_usuario: Int? = null,
    @SerializedName("fk_id_instructor") val fk_id_instructor: Int? = null,
    @SerializedName("motivo") val motivo: String? = null,
    @SerializedName("permiso_token") val permiso_token: String? = null,
    @SerializedName("estado") val estado: String? = null,
    @SerializedName("created_at") val created_at: String? = null,
    val usuario: UsuarioApi? = null,
    val instructor: UsuarioApi? = null
)
