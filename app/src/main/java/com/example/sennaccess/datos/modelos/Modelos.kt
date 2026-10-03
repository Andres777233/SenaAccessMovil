package com.example.sennaccess.datos.modelos

// Este archivo concentra las data classes que modelan el JSON de la API y las entidades
// de la app. @SerializedName mapea cada campo JSON (snake_case del backend Laravel)
// con la propiedad Kotlin correspondiente.

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

// Credenciales enviadas al endpoint de login. Los nombres de campo respetan el formato
// que espera el backend (user_email / user_password). device_id identifica ESTE teléfono
// para que el 2FA solo se exija desde dispositivos distintos al original (ver AlmacenDispositivo).
data class PeticionAcceso(
    @SerializedName("user_email") val user_email: String,
    @SerializedName("user_password") val user_password: String,
    @SerializedName("device_id") val device_id: String? = null
)

// Perfil básico del usuario devuelto por el acceso.
data class UsuarioSesion(
    @SerializedName("id_usuario") val id_usuario: Int? = null,
    @SerializedName("user_identification") val user_identification: String? = null,
    @SerializedName("user_name") val user_name: String? = null,
    @SerializedName("user_lastname") val user_lastname: String? = null,
    @SerializedName("user_email") val user_email: String? = null,
    @SerializedName("email_verified_at") val email_verified_at: String? = null,
    @SerializedName("user_coursenumber") val user_coursenumber: Int? = null,
    @SerializedName("user_program") val user_program: String? = null,
    @SerializedName("user_documento_tipo") val user_documento_tipo: String? = null,
    @SerializedName("user_telefono") val user_telefono: String? = null,
    @SerializedName("fk_id_rol") val fk_id_rol: Int? = null,
    @SerializedName("profile_photo_path") val profile_photo_path: String? = null
)

// Respuesta del acceso: usuario, rol y token (o reto 2FA).
data class RespuestaAcceso(
    val message: String? = null,
    val user: UsuarioSesion? = null,
    val role: String? = null,
    @SerializedName("access_token") val access_token: String? = null,
    @SerializedName("token_type") val token_type: String? = null,
    @SerializedName("two_factor_required") val two_factor_required: Boolean? = null,
    @SerializedName("two_factor_id") val two_factor_id: String? = null,
    @SerializedName("two_factor_method") val two_factor_method: String? = null,
    @SerializedName("code_sent") val two_factor_code_sent: Boolean? = null
)

// ---- Verificación en dos pasos (2FA) ----

// Configuración del 2FA del usuario autenticado (GET/POST /2fa/estado-config, activar, desactivar).
data class EstadoConfigDobleFactor(
    @SerializedName("two_factor_enabled") val two_factor_enabled: Boolean? = null,
    val message: String? = null
)

// Cuerpo para validar el código de 6 dígitos recibido por correo (POST /2fa/validar-codigo).
data class PeticionValidarCodigo(
    @SerializedName("challenge_id") val challenge_id: String,
    val code: String
)

// Clave actual para desactivar el segundo factor.
data class PeticionDesactivarDobleFactor(
    @SerializedName("user_password") val user_password: String
)

// Clave actual para operaciones sensibles del perfil.
data class PeticionVerificarClave(
    @SerializedName("user_password") val user_password: String
)

// Cuerpo para aprobar/denegar un reto desde el dispositivo confiable (POST /2fa/aprobar).
data class PeticionAprobarReto(
    @SerializedName("challenge_id") val challenge_id: String,
    val decision: String
)

// (GET /2fa/pendientes).
data class RetoDobleFactor(
    @SerializedName("challenge_id") val challenge_id: String? = null,
    val ip: String? = null,
    @SerializedName("user_agent") val user_agent: String? = null,
    @SerializedName("created_at") val created_at: String? = null,
    @SerializedName("expires_at") val expires_at: String? = null
)

// Lista de retos por aprobar en este dispositivo.
data class PendientesDobleFactor(
    val pending: Boolean? = null,
    val challenge: RetoDobleFactor? = null
)

// (GET /2fa/estado/{id}). Solo devuelve access_token/user/role cuando quedó aprobado.
data class EstadoVerificacion(
    val message: String? = null,
    @SerializedName("two_factor_id") val two_factor_id: String? = null,
    val estado: String? = null,
    @SerializedName("access_token") val access_token: String? = null,
    @SerializedName("token_type") val token_type: String? = null,
    val user: UsuarioSesion? = null,
    val role: String? = null
)

// ---- Salida y roles ----
// Respuesta del cierre de sesión.
data class RespuestaSalida(
    val message: String? = null
)

// Rol disponible para asignar a usuarios.
data class Rol(
    @SerializedName("id_rol") val id_rol: Int? = null,
    @SerializedName("rol_name") val rol_name: String? = null
)

// ---- Usuarios ----
// Usuario completo tal como lo guarda el backend.
data class UsuarioApi(
    @SerializedName("id_usuario") val id_usuario: Int? = null,
    @SerializedName("user_identification") val user_identification: String? = null,
    @SerializedName("user_name") val user_name: String? = null,
    @SerializedName("user_lastname") val user_lastname: String? = null,
    @SerializedName("user_email") val user_email: String? = null,
    @SerializedName("email_verified_at") val email_verified_at: String? = null,
    @SerializedName("user_coursenumber") val user_coursenumber: Int? = null,
    @SerializedName("user_program") val user_program: String? = null,
    @SerializedName("user_documento_tipo") val user_documento_tipo: String? = null,
    @SerializedName("user_telefono") val user_telefono: String? = null,
    @SerializedName("user_jornada") val user_jornada: String? = null,
    @SerializedName("user_jornada_sabado") val user_jornada_sabado: String? = null,
    @SerializedName("fk_id_rol") val fk_id_rol: Int? = null,
    @SerializedName("profile_photo_path") val profile_photo_path: String? = null,
    val role: Rol? = null
) {
    val nombreCompleto: String get() = listOfNotNull(user_name, user_lastname).joinToString(" ").ifBlank { "Sin nombre" }
    fun esRol(rol: String): Boolean = role?.rol_name.equals(rol, ignoreCase = true)
    // Invitado temporal (QR): nunca va a ambientes ni a dueños de equipos.
    fun esInvitado(): Boolean {
        val r = role?.rol_name?.trim()?.lowercase()
        return r == "invitado" || r == "visitante" || r == "guest"
    }
    // Jornada efectiva hoy (sábado especial si aplica).
    fun jornadaHoy(): String? {
        val base = user_jornada
        val sab = user_jornada_sabado
        if (sab.isNullOrBlank()) return base
        return try {
            com.example.sennaccess.comun.JornadaEfectiva.paraHoy(base, sab)
        } catch (_: Exception) { base }
    }
}

// (POST/PUT /admin/users). El password es opcional al actualizar.
data class PeticionUsuario(
    @SerializedName("user_identification") val user_identification: String,
    @SerializedName("user_name") val user_name: String,
    @SerializedName("user_lastname") val user_lastname: String,
    @SerializedName("user_email") val user_email: String,
    @SerializedName("user_password") val user_password: String? = null,
    @SerializedName("user_coursenumber") val user_coursenumber: Int? = null,
    @SerializedName("user_program") val user_program: String? = null,
    @SerializedName("user_documento_tipo") val user_documento_tipo: String? = null,
    @SerializedName("user_telefono") val user_telefono: String? = null,
    @SerializedName("user_jornada") val user_jornada: String? = null,
    @SerializedName("user_jornada_sabado") val user_jornada_sabado: String? = null,
    @SerializedName("fk_id_rol") val fk_id_rol: Int
)

// ---- Ingresos y presencia ----
// Marca de entrada o salida del historial.
data class Ingreso(
    @SerializedName("id_ingreso") val id_ingreso: Int? = null,
    @SerializedName("ingreso_datetime") val ingreso_datetime: String? = null,
    @SerializedName("ingreso_place") val ingreso_place: String? = null,
    @SerializedName("ingreso_type") val ingreso_type: String? = null,
    @SerializedName("fk_id_user") val fk_id_user: Int? = null,
    val user: UsuarioApi? = null
)

// Respuesta paginada del historial global (GET /admin/ingresos): el backend
data class RespuestaIngresos(
    @SerializedName("data") val data: List<Ingreso>? = null
)

// Usuario que está DENTRO ahora (GET /admin/presentes): identificación, nombre
data class Presente(
    @SerializedName("id_usuario") val id_usuario: Int? = null,
    @SerializedName("user_name") val user_name: String? = null,
    @SerializedName("user_lastname") val user_lastname: String? = null,
    val rol: String? = null,
    @SerializedName("entrada_hora") val entrada_hora: String? = null
) {
    val nombreCompleto: String get() = listOfNotNull(user_name, user_lastname).joinToString(" ").ifBlank { "Usuario" }
}

// ---- Equipos ----
// Equipo registrado con su dueño.
data class EquipoIngreso(
    @SerializedName("id_ingreso_equipo") val id_ingreso_equipo: Int? = null,
    @SerializedName("fk_id_usuario") val fk_id_usuario: Int? = null,
    @SerializedName("equipo_type") val equipo_type: String? = null,
    @SerializedName("equipo_brand") val equipo_brand: String? = null,
    @SerializedName("equipo_model") val equipo_model: String? = null,
    @SerializedName("equipo_color") val equipo_color: String? = null,
    @SerializedName("equipo_serial") val equipo_serial: String? = null,
    @SerializedName("equipo_observations") val equipo_observations: String? = null,
    @SerializedName("equipo_accesorios") val equipo_accesorios: List<Accesorio>? = null,
    @SerializedName("entry_datetime") val entry_datetime: String? = null,
    val user: UsuarioApi? = null
) {
    val marcaModelo: String get() = listOfNotNull(equipo_brand, equipo_model).joinToString(" ").ifBlank { "—" }
}

// Accesorio declarado de un equipo.
data class Accesorio(
    @SerializedName("tipo") val tipo: String,
    @SerializedName("marca") val marca: String? = null,
    @SerializedName("color") val color: String? = null,
    @SerializedName("inalambrico") val inalambrico: Boolean? = null
)

// Datos para registrar un equipo.
data class PeticionEquipoIngreso(
    @SerializedName("equipo_type") val equipo_type: String,
    @SerializedName("equipo_brand") val equipo_brand: String,
    @SerializedName("equipo_color") val equipo_color: String,
    @SerializedName("equipo_serial") val equipo_serial: String,
    @SerializedName("equipo_observations") val equipo_observations: String? = null,
    @SerializedName("fk_id_usuario") val fk_id_usuario: Int? = null,
    @SerializedName("equipo_accesorios") val equipo_accesorios: List<Accesorio>? = null
)

// Respuesta del registro de equipo.
data class RespuestaEquipo(
    val message: String? = null,
    val data: EquipoIngreso? = null
)

// ---- Novedades ----
// Novedad publicada por el admin.
data class Novedad(
    @SerializedName("id_novedad") val id_novedad: Int? = null,
    @SerializedName("novedad_ambiente") val novedad_ambiente: String? = null,
    @SerializedName("novedad_title") val novedad_title: String? = null,
    @SerializedName("novedad_body") val novedad_body: String? = null,
    @SerializedName("novedad_datetime") val novedad_datetime: String? = null,
    @SerializedName("fk_id_usuario") val fk_id_usuario: Int? = null,
    val user: UsuarioApi? = null
)

// Datos para publicar una novedad.
data class PeticionNovedad(
    @SerializedName("novedad_ambiente") val novedad_ambiente: String,
    @SerializedName("fk_id_ambiente") val fk_id_ambiente: Int? = null,
    @SerializedName("novedad_title") val novedad_title: String,
    @SerializedName("novedad_body") val novedad_body: String
)

// ---- Registro e invitados ----
// Datos del registro público de aprendices.
data class PeticionRegistro(
    @SerializedName("user_identification") val user_identification: String,
    @SerializedName("user_name") val user_name: String,
    @SerializedName("user_lastname") val user_lastname: String,
    @SerializedName("user_email") val user_email: String,
    @SerializedName("user_password") val user_password: String,
    @SerializedName("user_password_confirmation") val user_password_confirmation: String,
    @SerializedName("user_coursenumber") val user_coursenumber: Int,
    @SerializedName("user_program") val user_program: String,
    @SerializedName("user_documento_tipo") val user_documento_tipo: String,
    @SerializedName("user_telefono") val user_telefono: String? = null,
    @SerializedName("user_jornada") val user_jornada: String? = null,
    @SerializedName("user_jornada_sabado") val user_jornada_sabado: String? = null
)

// Respuesta del registro de cuenta.
data class RespuestaRegistro(
    val message: String? = null,
    val user: UsuarioSesion? = null
)

// Datos enviados a POST /register-guest: el visitante solo aporta documento,
data class PeticionInvitado(
    @SerializedName("user_identification") val user_identification: String,
    @SerializedName("user_name") val user_name: String,
    @SerializedName("user_lastname") val user_lastname: String,
    @SerializedName("user_documento_tipo") val user_documento_tipo: String
)

// QR de un solo uso entregado al invitado.
data class RespuestaQrInvitado(
    val message: String? = null,
    val user: UsuarioSesion? = null,
    @SerializedName("qr_token") val qr_token: String? = null,
    @SerializedName("qr_expires_at") val qr_expires_at: String? = null
)

// Datos enviados a POST /validate-guest-qr: el token contenido en el QR escaneado.
data class PeticionValidarQr(
    @SerializedName("qr_token") val qr_token: String
)

// ---- Recuperación de clave ----
// Correo al que se envía el código de recuperación.
data class PeticionRecuperacion(
    val email: String
)

// Código recibido y clave nueva para restablecer el acceso.
data class PeticionRestablecer(
    val code: String,
    val password: String,
    @SerializedName("password_confirmation") val password_confirmation: String
)

// Cuerpo para actualizar el perfil propio (PUT /my-profile). La contraseña es
data class PeticionActualizarPerfil(
    @SerializedName("user_identification") val user_identification: String,
    @SerializedName("user_name") val user_name: String,
    @SerializedName("user_lastname") val user_lastname: String,
    @SerializedName("user_email") val user_email: String,
    @SerializedName("user_password") val user_password: String? = null,
    @SerializedName("user_coursenumber") val user_coursenumber: Int? = null,
    @SerializedName("user_program") val user_program: String? = null,
    @SerializedName("two_factor_code") val two_factor_code: String? = null
)

// ---- Ambientes ----
// Salón con horario, sede e instructores asignados.
data class Ambiente(
    @SerializedName("id_ambiente") val id_ambiente: Int? = null,
    @SerializedName("ambiente_nombre") val ambiente_nombre: String? = null,
    @SerializedName("ambiente_capacidad") val ambiente_capacidad: Int? = null,
    @SerializedName("ambiente_ubicacion") val ambiente_ubicacion: String? = null,
    @SerializedName("ambiente_estado") val ambiente_estado: String? = null,
    @SerializedName("ambiente_jornada") val ambiente_jornada: String? = null,
    @SerializedName("fk_id_instructor") val fk_id_instructor: Int? = null,
    @SerializedName("hora_inicio") val hora_inicio: String? = null,
    @SerializedName("hora_fin") val hora_fin: String? = null,
    @SerializedName("aprendices_count") val aprendices_count: Int? = null,
    val instructor: UsuarioApi? = null,
    val instructores: List<UsuarioApi>? = null,
    val aprendices: List<UsuarioApi>? = null
)

// Datos para crear o actualizar un ambiente.
data class PeticionAmbiente(
    @SerializedName("ambiente_nombre") val ambiente_nombre: String,
    @SerializedName("ambiente_capacidad") val ambiente_capacidad: Int? = null,
    @SerializedName("ambiente_ubicacion") val ambiente_ubicacion: String? = null,
    @SerializedName("ambiente_estado") val ambiente_estado: String? = null,
    @SerializedName("ambiente_jornada") val ambiente_jornada: String? = null,
    @SerializedName("hora_inicio") val hora_inicio: String? = null,
    @SerializedName("hora_fin") val hora_fin: String? = null,
    @SerializedName("fk_id_instructor") val fk_id_instructor: Int? = null,
    val instructores: List<Int>? = null
)

// Petición para asignar/añadir un aprendiz a un ambiente (POST /mis-ambientes/{id}/aprendices).
data class PeticionAmbienteAprendiz(
    @SerializedName("fk_id_usuario") val fk_id_usuario: Int
)

// Sincronización de instructores de un ambiente (POST /admin/ambientes/{id}/instructores).
data class PeticionAmbienteInstructores(
    val instructores: List<Int>
)

// Ventana de clase de un ambiente.
data class HorarioAmbiente(
    @SerializedName("id_horario") val id_horario: Int? = null,
    @SerializedName("fk_id_ambiente") val fk_id_ambiente: Int? = null,
    @SerializedName("dia") val dia: String? = null,
    @SerializedName("jornada") val jornada: String? = null,
    @SerializedName("fk_id_instructor") val fk_id_instructor: Int? = null,
    val instructor: UsuarioApi? = null
)

// Horario para crear o actualizar.
data class PeticionHorario(
    @SerializedName("dia") val dia: String,
    @SerializedName("jornada") val jornada: String,
    @SerializedName("fk_id_instructor") val fk_id_instructor: Int
)

// ---- Notificaciones ----
// Notificación del usuario en sesión.
data class Notificacion(
    @SerializedName("id_notificacion") val id_notificacion: Int? = null,
    @SerializedName("fk_id_usuario") val fk_id_usuario: Int? = null,
    @SerializedName("notification_title") val notification_title: String? = null,
    @SerializedName("notification_body") val notification_body: String? = null,
    @SerializedName("notification_type") val notification_type: String? = null,
    @SerializedName("is_read") val is_read: Boolean? = null,
    @SerializedName("created_at") val created_at: String? = null
)

// Cantidad de notificaciones sin leer.
data class ConteoNoLeidas(
    val unread: Int? = null
)

// Respuesta genérica con mensaje del servidor.
data class RespuestaMensaje(
    val message: String? = null
)
