package com.example.sennaccess.data

// Este archivo concentra las data classes que modelan el JSON de la API y las entidades
// de la app. @SerializedName mapea cada campo JSON (snake_case del backend Laravel)
// con la propiedad Kotlin correspondiente.

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

// Credenciales enviadas al endpoint de login. Los nombres de campo respetan el formato
// que espera el backend (user_email / user_password).
data class LoginRequest(
    @SerializedName("user_email") val user_email: String,
    @SerializedName("user_password") val user_password: String
)

// Opciones WebAuthn (passkeys) que devuelve el backend: options es el JSON estándar
// que se le pasa al Credential Manager de Android y challenge se usa para la firma.
data class WebauthnOptionsResponse(
    val options: JsonObject? = null,
    val challenge: String? = null
)

// Passkey registrada en el backend (solo metadatos; la llave privada nunca sale
// del dispositivo).
data class PasskeyInfo(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("credential_id") val credential_id: String? = null,
    @SerializedName("created_at") val created_at: String? = null
)

// Usuario tal como lo devuelve el endpoint de login: perfil básico con su rol.
data class User(
    @SerializedName("id_usuario") val id_usuario: Int? = null,
    @SerializedName("user_identification") val user_identification: String? = null,
    @SerializedName("user_name") val user_name: String? = null,
    @SerializedName("user_lastname") val user_lastname: String? = null,
    @SerializedName("user_email") val user_email: String? = null,
    @SerializedName("user_coursenumber") val user_coursenumber: Int? = null,
    @SerializedName("user_program") val user_program: String? = null,
    @SerializedName("fk_id_rol") val fk_id_rol: Int? = null,
    @SerializedName("profile_photo_path") val profile_photo_path: String? = null
)

// Respuesta del login: mensaje, datos del usuario, nombre del rol y access_token que
// la app guarda en SessionManager para autenticar el resto de las llamadas.
data class LoginResponse(
    val message: String? = null,
    val user: User? = null,
    val role: String? = null,
    @SerializedName("access_token") val access_token: String? = null,
    @SerializedName("token_type") val token_type: String? = null
)

// Respuesta del cierre de sesión: solo confirmación del servidor.
data class LogoutResponse(
    val message: String? = null
)

// Catálogo de roles: identificador y nombre (admin, Instructor, Aprendiz). Permite
// decidir la navegación y los permisos de cada pantalla según el rol del usuario.
data class Role(
    @SerializedName("id_rol") val id_rol: Int? = null,
    @SerializedName("rol_name") val rol_name: String? = null
)

// Usuario completo como lo devuelve la API en los listados administrativos. Incluye
// el objeto Role anidado y utilidades para mostrar el nombre y comparar roles.
data class UsuarioApi(
    @SerializedName("id_usuario") val id_usuario: Int? = null,
    @SerializedName("user_identification") val user_identification: String? = null,
    @SerializedName("user_name") val user_name: String? = null,
    @SerializedName("user_lastname") val user_lastname: String? = null,
    @SerializedName("user_email") val user_email: String? = null,
    @SerializedName("user_coursenumber") val user_coursenumber: Int? = null,
    @SerializedName("user_program") val user_program: String? = null,
    @SerializedName("fk_id_rol") val fk_id_rol: Int? = null,
    @SerializedName("profile_photo_path") val profile_photo_path: String? = null,
    val role: Role? = null
) {
    val nombreCompleto: String get() = listOfNotNull(user_name, user_lastname).joinToString(" ").ifBlank { "Sin nombre" }
    fun esRol(rol: String): Boolean = role?.rol_name.equals(rol, ignoreCase = true)
}

// Cuerpo para crear o actualizar un usuario desde el panel del admin
// (POST/PUT /admin/users). El password es opcional al actualizar.
data class UserRequest(
    @SerializedName("user_identification") val user_identification: String,
    @SerializedName("user_name") val user_name: String,
    @SerializedName("user_lastname") val user_lastname: String,
    @SerializedName("user_email") val user_email: String,
    @SerializedName("user_password") val user_password: String? = null,
    @SerializedName("user_coursenumber") val user_coursenumber: Int? = null,
    @SerializedName("user_program") val user_program: String? = null,
    @SerializedName("fk_id_rol") val fk_id_rol: Int
)

// Registro de entrada de una persona al centro: fecha/hora, lugar y tipo de ingreso,
// junto con el usuario al que pertenece.
data class Ingreso(
    @SerializedName("id_ingreso") val id_ingreso: Int? = null,
    @SerializedName("ingreso_datetime") val ingreso_datetime: String? = null,
    @SerializedName("ingreso_place") val ingreso_place: String? = null,
    @SerializedName("ingreso_type") val ingreso_type: String? = null,
    @SerializedName("fk_id_user") val fk_id_user: Int? = null,
    val user: UsuarioApi? = null
)

// Respuesta paginada del historial global (GET /admin/ingresos): el backend
// devuelve {data:[...]} con metadatos de paginación, no una lista plana.
data class IngresosResponse(
    @SerializedName("data") val data: List<Ingreso>? = null
)

// Registro de entrada de un equipo tecnológico: datos del dispositivo (tipo, marca,
// modelo, color, serial), observaciones, accesorios que lleva y el usuario que lo registró.
data class IngresoEquipo(
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

// Accesorio de un equipo registrado: tipo (Mouse, Teclado, Audífonos), marca, color y
// si es inalámbrico (aplica para mouse y audífonos).
data class Accesorio(
    @SerializedName("tipo") val tipo: String,
    @SerializedName("marca") val marca: String? = null,
    @SerializedName("color") val color: String? = null,
    @SerializedName("inalambrico") val inalambrico: Boolean? = null
)

// Cuerpo de la petición para registrar un equipo: datos del portátil más los
// accesorios que lleva. En modo admin se envía fk_id_usuario para asignar el
// dueño; sin él el backend lo asigna a partir del token.
data class IngresoEquipoRequest(
    @SerializedName("equipo_type") val equipo_type: String,
    @SerializedName("equipo_brand") val equipo_brand: String,
    @SerializedName("equipo_color") val equipo_color: String,
    @SerializedName("equipo_serial") val equipo_serial: String,
    @SerializedName("equipo_observations") val equipo_observations: String? = null,
    @SerializedName("fk_id_usuario") val fk_id_usuario: Int? = null,
    @SerializedName("equipo_accesorios") val equipo_accesorios: List<Accesorio>? = null
)

// Respuesta de la creación de un equipo: mensaje de confirmación y el registro creado.
data class EquipmentResponse(
    val message: String? = null,
    val data: IngresoEquipo? = null
)

// Novedad o aviso publicado por un instructor/admin: ambiente, título, cuerpo y fecha
// de publicación, con el autor asociado.
data class Novedad(
    @SerializedName("id_novedad") val id_novedad: Int? = null,
    @SerializedName("novedad_ambiente") val novedad_ambiente: String? = null,
    @SerializedName("novedad_title") val novedad_title: String? = null,
    @SerializedName("novedad_body") val novedad_body: String? = null,
    @SerializedName("novedad_datetime") val novedad_datetime: String? = null,
    @SerializedName("fk_id_usuario") val fk_id_usuario: Int? = null,
    val user: UsuarioApi? = null
)

// Cuerpo de la petición para crear una novedad: ambiente (nombre o FK), título y detalle.
data class NovedadRequest(
    @SerializedName("novedad_ambiente") val novedad_ambiente: String,
    @SerializedName("fk_id_ambiente") val fk_id_ambiente: Int? = null,
    @SerializedName("novedad_title") val novedad_title: String,
    @SerializedName("novedad_body") val novedad_body: String
)

// Datos enviados al endpoint público de registro de cuentas (rol por defecto Aprendiz).
data class RegisterRequest(
    @SerializedName("user_identification") val user_identification: String,
    @SerializedName("user_name") val user_name: String,
    @SerializedName("user_lastname") val user_lastname: String,
    @SerializedName("user_email") val user_email: String,
    @SerializedName("user_password") val user_password: String,
    @SerializedName("user_password_confirmation") val user_password_confirmation: String,
    @SerializedName("user_coursenumber") val user_coursenumber: Int,
    @SerializedName("user_program") val user_program: String
)

// Respuesta del registro de cuenta: mensaje de confirmación y el usuario creado.
data class RegisterResponse(
    val message: String? = null,
    val user: User? = null
)

// Solicitud de código de recuperación: solo el correo institucional.
data class ForgotRequest(
    val email: String
)

// Cambio de contraseña con el código recibido por correo.
data class ResetRequest(
    val code: String,
    val password: String,
    @SerializedName("password_confirmation") val password_confirmation: String
)

// Cuerpo para actualizar el perfil propio (PUT /my-profile). La contraseña es
// opcional: solo se cambia cuando viene llena.
data class UpdateProfileRequest(
    @SerializedName("user_identification") val user_identification: String,
    @SerializedName("user_name") val user_name: String,
    @SerializedName("user_lastname") val user_lastname: String,
    @SerializedName("user_email") val user_email: String,
    @SerializedName("user_password") val user_password: String? = null,
    @SerializedName("user_coursenumber") val user_coursenumber: Int,
    @SerializedName("user_program") val user_program: String
)

// Ambiente de formación: datos generales, responsable y los instructores asignados.
data class Ambiente(
    @SerializedName("id_ambiente") val id_ambiente: Int? = null,
    @SerializedName("ambiente_nombre") val ambiente_nombre: String? = null,
    @SerializedName("ambiente_capacidad") val ambiente_capacidad: Int? = null,
    @SerializedName("ambiente_ubicacion") val ambiente_ubicacion: String? = null,
    @SerializedName("ambiente_estado") val ambiente_estado: String? = null,
    @SerializedName("ambiente_jornada") val ambiente_jornada: String? = null,
    @SerializedName("fk_id_instructor") val fk_id_instructor: Int? = null,
    val instructor: UsuarioApi? = null,
    val instructores: List<UsuarioApi>? = null
)

// Cuerpo para crear/editar un ambiente: datos generales más los instructores asignados.
data class AmbienteRequest(
    @SerializedName("ambiente_nombre") val ambiente_nombre: String,
    @SerializedName("ambiente_capacidad") val ambiente_capacidad: Int? = null,
    @SerializedName("ambiente_ubicacion") val ambiente_ubicacion: String? = null,
    @SerializedName("ambiente_estado") val ambiente_estado: String? = null,
    @SerializedName("ambiente_jornada") val ambiente_jornada: String? = null,
    @SerializedName("fk_id_instructor") val fk_id_instructor: Int? = null,
    val instructores: List<Int>? = null
)

// Horario de un ambiente: día, jornada e instructor asignado a esa celda.
data class HorarioAmbiente(
    @SerializedName("id_horario") val id_horario: Int? = null,
    @SerializedName("fk_id_ambiente") val fk_id_ambiente: Int? = null,
    @SerializedName("dia") val dia: String? = null,
    @SerializedName("jornada") val jornada: String? = null,
    @SerializedName("fk_id_instructor") val fk_id_instructor: Int? = null,
    val instructor: UsuarioApi? = null
)

// Cuerpo para crear/actualizar una celda de horario de un ambiente.
data class HorarioRequest(
    @SerializedName("dia") val dia: String,
    @SerializedName("jornada") val jornada: String,
    @SerializedName("fk_id_instructor") val fk_id_instructor: Int
)

// Asignación aprendiz -> instructor, con ambiente y jornada opcionales.
data class AprendizInstructor(
    @SerializedName("id_asignacion") val id_asignacion: Int? = null,
    @SerializedName("fk_id_aprendiz") val fk_id_aprendiz: Int? = null,
    @SerializedName("fk_id_instructor") val fk_id_instructor: Int? = null,
    @SerializedName("fk_id_ambiente") val fk_id_ambiente: Int? = null,
    @SerializedName("jornada") val jornada: String? = null,
    val aprendiz: UsuarioApi? = null,
    val instructor: UsuarioApi? = null,
    val ambiente: Ambiente? = null
)

// Cuerpo para asignar (o actualizar) un instructor a un aprendiz.
data class AsignacionRequest(
    @SerializedName("fk_id_aprendiz") val fk_id_aprendiz: Int,
    @SerializedName("fk_id_instructor") val fk_id_instructor: Int,
    @SerializedName("fk_id_ambiente") val fk_id_ambiente: Int? = null,
    @SerializedName("jornada") val jornada: String? = null
)

// Notificación in-app dirigida a un usuario: título, cuerpo, tipo y estado de lectura.
data class Notificacion(
    @SerializedName("id_notificacion") val id_notificacion: Int? = null,
    @SerializedName("fk_id_usuario") val fk_id_usuario: Int? = null,
    @SerializedName("notification_title") val notification_title: String? = null,
    @SerializedName("notification_body") val notification_body: String? = null,
    @SerializedName("notification_type") val notification_type: String? = null,
    @SerializedName("is_read") val is_read: Boolean? = null,
    @SerializedName("created_at") val created_at: String? = null
)

// Conteo de notificaciones sin leer (badge de la campana).
data class UnreadCount(
    val unread: Int? = null
)

// Respuesta genérica de mensaje único (registro, eliminación, marcado, etc.).
data class MessageResponse(
    val message: String? = null
)