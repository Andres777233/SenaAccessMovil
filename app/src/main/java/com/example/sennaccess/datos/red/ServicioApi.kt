package com.example.sennaccess.datos.red

// Interfaz que declara los endpoints REST del backend como funciones suspend de Retrofit.
// Cada anotación (@GET/@POST) se resuelve contra la baseUrl de ClienteApi:
// baseUrl + "login" equivale a http://127.0.0.1:8000/api/login (o la IP WiFi).

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import com.example.sennaccess.datos.modelos.Ambiente
import com.example.sennaccess.datos.modelos.ConteoNoLeidas
import com.example.sennaccess.datos.modelos.EquipoIngreso
import com.example.sennaccess.datos.modelos.EstadoConfigDobleFactor
import com.example.sennaccess.datos.modelos.EstadoVerificacion
import com.example.sennaccess.datos.modelos.Excusa
import com.example.sennaccess.datos.modelos.HorarioAmbiente
import com.example.sennaccess.datos.modelos.Ingreso
import com.example.sennaccess.datos.modelos.Notificacion
import com.example.sennaccess.datos.modelos.Novedad
import com.example.sennaccess.datos.modelos.PendientesDobleFactor
import com.example.sennaccess.datos.modelos.PeticionAcceso
import com.example.sennaccess.datos.modelos.PeticionActualizarPerfil
import com.example.sennaccess.datos.modelos.PeticionAmbiente
import com.example.sennaccess.datos.modelos.PeticionAmbienteAprendiz
import com.example.sennaccess.datos.modelos.PeticionAmbienteInstructores
import com.example.sennaccess.datos.modelos.PeticionAprobarReto
import com.example.sennaccess.datos.modelos.PeticionCrearExcusa
import com.example.sennaccess.datos.modelos.PeticionDesactivarDobleFactor
import com.example.sennaccess.datos.modelos.PeticionEquipoIngreso
import com.example.sennaccess.datos.modelos.PeticionHorario
import com.example.sennaccess.datos.modelos.PeticionInvitado
import com.example.sennaccess.datos.modelos.PeticionNovedad
import com.example.sennaccess.datos.modelos.PeticionRecuperacion
import com.example.sennaccess.datos.modelos.PeticionRegistro
import com.example.sennaccess.datos.modelos.PeticionResponderSugerencia
import com.example.sennaccess.datos.modelos.PeticionRestablecer
import com.example.sennaccess.datos.modelos.PeticionSugerencia
import com.example.sennaccess.datos.modelos.PeticionUsuario
import com.example.sennaccess.datos.modelos.PeticionValidarCodigo
import com.example.sennaccess.datos.modelos.PeticionValidarExcusa
import com.example.sennaccess.datos.modelos.PeticionValidarQr
import com.example.sennaccess.datos.modelos.PeticionVerificarClave
import com.example.sennaccess.datos.modelos.Presente
import com.example.sennaccess.datos.modelos.RespuestaAcceso
import com.example.sennaccess.datos.modelos.RespuestaEquipo
import com.example.sennaccess.datos.modelos.RespuestaIngresos
import com.example.sennaccess.datos.modelos.RespuestaMensaje
import com.example.sennaccess.datos.modelos.RespuestaPaginaSugerencias
import com.example.sennaccess.datos.modelos.RespuestaQrInvitado
import com.example.sennaccess.datos.modelos.RespuestaRegistro
import com.example.sennaccess.datos.modelos.RespuestaSalida
import com.example.sennaccess.datos.modelos.RespuestaValidarExcusa
import com.example.sennaccess.datos.modelos.Rol
import com.example.sennaccess.datos.modelos.Sugerencia
import com.example.sennaccess.datos.modelos.UsuarioApi

interface ServicioApi {

    // ---- Auth ----
    // 1. Login (POST /api/login): envía email y contraseña en el cuerpo de la petición.
    @POST("login")
    suspend fun login(@Body body: PeticionAcceso): RespuestaAcceso

    // 1a. POST /api/register: registro público de nuevas cuentas (rol Aprendiz por defecto).
    @POST("register")
    suspend fun register(@Body body: PeticionRegistro): RespuestaRegistro

    // 1b. POST /api/logout: cierra la sesión en el servidor. Requiere el token Bearer.
    @POST("logout")
    suspend fun logout(@Header("Authorization") auth: String): RespuestaSalida

    // 1c. POST /api/forgot-password: solicita el código de recuperación al correo.
    @POST("forgot-password")
    suspend fun forgotPassword(@Body body: PeticionRecuperacion): RespuestaMensaje

    // 1d. POST /api/reset-password: cambia la contraseña con el código recibido.
    @POST("reset-password")
    suspend fun resetPassword(@Body body: PeticionRestablecer): RespuestaMensaje

    // 1e. POST /api/register-guest: el visitante genera su QR de invitado de un solo
    @POST("register-guest")
    suspend fun registerGuest(@Body body: PeticionInvitado): RespuestaQrInvitado

    // 1f. POST /api/validate-guest-qr: recepción valida el QR (token) del invitado y
    @POST("validate-guest-qr")
    suspend fun validateGuestQr(@Body body: PeticionValidarQr): RespuestaMensaje

    // 1g. POST /api/email/verification-notification: reenvía el enlace de verificación
    @POST("email/verification-notification")
    suspend fun resendVerification(@Header("Authorization") auth: String): RespuestaMensaje

    // ---- Verificación en dos pasos (2FA) ----
    // 1f. POST /api/2fa/validar-codigo: el dispositivo en intento de login envía el
    @POST("2fa/validar-codigo")
    suspend fun validarCodigo2Fa(@Body body: PeticionValidarCodigo): RespuestaAcceso

    // 1g. GET /api/2fa/estado/{id}: polling del dispositivo que intenta iniciar sesión.
    @GET("2fa/estado/{challengeId}")
    suspend fun estadoVerificacion2Fa(@Path("challengeId") challengeId: String): EstadoVerificacion

    // 1h. GET /api/2fa/estado-config: si el usuario autenticado tiene 2FA activo.
    @GET("2fa/estado-config")
    suspend fun estadoConfig2Fa(@Header("Authorization") auth: String): EstadoConfigDobleFactor

    // 1i. POST /api/2fa/activar y /desactivar: alternan el 2FA desde el perfil.
    @POST("2fa/activar")
    suspend fun activar2Fa(@Header("Authorization") auth: String): EstadoConfigDobleFactor

    @POST("2fa/desactivar")
    suspend fun desactivar2Fa(@Header("Authorization") auth: String, @Body body: PeticionDesactivarDobleFactor): EstadoConfigDobleFactor

    // 1j. GET /api/2fa/pendientes: retos de acceso pendientes de aprobar. Lo consulta
    @GET("2fa/pendientes")
    suspend fun pendientes2Fa(@Header("Authorization") auth: String): PendientesDobleFactor

    // 1k. POST /api/2fa/aprobar: aprueba o deniega un reto desde el dispositivo confiable.
    @POST("2fa/aprobar")
    suspend fun aprobar2Fa(@Header("Authorization") auth: String, @Body body: PeticionAprobarReto): RespuestaMensaje

    // ---- Cualquier rol (sesión) ----
    // 2. GET /api/user: perfil del usuario autenticado. Cualquier rol con sesión puede
    @GET("user")
    suspend fun getCurrentUser(@Header("Authorization") auth: String): UsuarioApi

    // 3. GET /api/my-ingresos: registros de ingreso del usuario logueado. Listado
    @GET("my-ingresos")
    suspend fun getMyIngresos(@Header("Authorization") auth: String): List<Ingreso>

    // 4. GET /api/my-equipment: equipos registrados a nombre del usuario actual.
    @GET("my-equipment")
    suspend fun getMyEquipment(@Header("Authorization") auth: String): List<EquipoIngreso>

    // 4b. PUT /api/my-profile: actualiza el perfil del usuario logueado.
    @PUT("my-profile")
    suspend fun updateMyProfile(@Header("Authorization") auth: String, @Body body: PeticionActualizarPerfil): UsuarioApi

    // 4b2. POST /api/my-profile/verificar-password: confirma la contraseña actual
    @POST("my-profile/verificar-password")
    suspend fun verificarPassword(@Header("Authorization") auth: String, @Body body: PeticionVerificarClave): RespuestaMensaje

    // 4c. POST /api/my-profile con _method=PUT y multipart/form-data: misma actualización
    @Multipart
    @POST("my-profile")
    suspend fun updateMyProfileWithPhoto(
        @Header("Authorization") auth: String,
        @Part("_method") method: okhttp3.RequestBody,
        @Part image: MultipartBody.Part,
        @Part("user_identification") identificacion: okhttp3.RequestBody,
        @Part("user_name") nombre: okhttp3.RequestBody,
        @Part("user_lastname") apellido: okhttp3.RequestBody,
        @Part("user_email") correo: okhttp3.RequestBody,
        @Part("user_coursenumber") ficha: okhttp3.RequestBody? = null,
        @Part("user_program") programa: okhttp3.RequestBody? = null,
        @Part("two_factor_code") codigoDobleFactor: okhttp3.RequestBody? = null
    ): UsuarioApi

    // ---- Admin / Instructor ----
    @GET("admin/users")
    suspend fun getUsers(@Header("Authorization") auth: String): List<UsuarioApi>

    @POST("admin/users")
    suspend fun createUser(@Header("Authorization") auth: String, @Body body: PeticionUsuario): UsuarioApi

    @PUT("admin/users/{id}")
    suspend fun updateUser(@Header("Authorization") auth: String, @Path("id") id: Int, @Body body: PeticionUsuario): UsuarioApi

    @DELETE("admin/users/{id}")
    suspend fun deleteUser(@Header("Authorization") auth: String, @Path("id") id: Int): RespuestaMensaje

    // 6. GET /api/admin/ingresos: registro global de ingresos de todos los usuarios.
    @GET("admin/ingresos")
    suspend fun getIngresos(
        @Header("Authorization") auth: String,
        @Query("per_page") perPage: Int = 500,
        @Query("desde") desde: String? = null,
        @Query("hasta") hasta: String? = null
    ): RespuestaIngresos

    // 7. GET /api/admin/roles: catálogo de roles, útil para filtros y desplegables.
    @GET("admin/roles")
    suspend fun getRoles(@Header("Authorization") auth: String): List<Rol>

    // 7a. GET /api/admin/presentes: usuarios que están DENTRO ahora (su último
    @GET("admin/presentes")
    suspend fun getPresentes(@Header("Authorization") auth: String): List<Presente>

    // 8. GET /api/my-novedades: novedades publicadas por el usuario logueado.
    @GET("my-novedades")
    suspend fun getMyNovedades(@Header("Authorization") auth: String): List<Novedad>

    // 9. GET /api/novedades: listado general de novedades con búsqueda opcional por
    @GET("novedades")
    suspend fun getNovedades(
        @Header("Authorization") auth: String,
        @Query("search") search: String? = null
    ): List<Novedad>

    // ---- Solo Admin ----
    // 10. GET /api/admin/equipment: inventario completo de equipos registrados en
    @GET("admin/equipment")
    suspend fun getEquipment(@Header("Authorization") auth: String): List<EquipoIngreso>

    // 11. POST /api/admin/equipment: registra el ingreso de un equipo. Disponible para
    @POST("admin/equipment")
    suspend fun createEquipment(
        @Header("Authorization") auth: String,
        @Body body: PeticionEquipoIngreso
    ): RespuestaEquipo

    // 11a. POST /api/my-equipment: registra el ingreso de un equipo a nombre del
    @POST("my-equipment")
    suspend fun createMyEquipment(
        @Header("Authorization") auth: String,
        @Body body: PeticionEquipoIngreso
    ): RespuestaEquipo

    // 11b. DELETE /api/admin/equipment/{id}: elimina un registro de equipo (solo admin).
    @DELETE("admin/equipment/{id}")
    suspend fun deleteEquipment(
        @Header("Authorization") auth: String,
        @Path("id") id: Int
    ): RespuestaMensaje

    // 12. GET /api/ambientes: catálogo de ambientes para cualquier rol autenticado.
    @GET("ambientes")
    suspend fun getAmbientes(@Header("Authorization") auth: String): List<Ambiente>

    // 12a. GET /api/mis-ambientes: ambientes donde el instructor/aprendiz está asignado.
    @GET("mis-ambientes")
    suspend fun getMisAmbientes(@Header("Authorization") auth: String): List<Ambiente>

    @GET("mis-ambientes/{id}/aprendices")
    suspend fun getMisAprendices(@Header("Authorization") auth: String, @Path("id") id: Int): List<UsuarioApi>

    @POST("mis-ambientes/{id}/aprendices")
    suspend fun addMisAprendiz(@Header("Authorization") auth: String, @Path("id") id: Int, @Body body: PeticionAmbienteAprendiz): RespuestaMensaje

    @DELETE("mis-ambientes/{id}/aprendices/{userId}")
    suspend fun removeMisAprendiz(@Header("Authorization") auth: String, @Path("id") id: Int, @Path("userId") userId: Int): RespuestaMensaje

    @GET("admin/ambientes/{id}/aprendices")
    suspend fun getAprendicesDeAmbiente(@Header("Authorization") auth: String, @Path("id") id: Int): List<UsuarioApi>

    @POST("admin/ambientes/{id}/aprendices")
    suspend fun addAprendizAAmbiente(@Header("Authorization") auth: String, @Path("id") id: Int, @Body body: PeticionAmbienteAprendiz): RespuestaMensaje

    @DELETE("admin/ambientes/{id}/aprendices/{userId}")
    suspend fun removeAprendizDeAmbiente(@Header("Authorization") auth: String, @Path("id") id: Int, @Path("userId") userId: Int): RespuestaMensaje

    @POST("admin/ambientes/{id}/instructores")
    suspend fun syncInstructores(@Header("Authorization") auth: String, @Path("id") id: Int, @Body body: PeticionAmbienteInstructores): Ambiente

    // ---- Ambientes (solo admin) ----
    @POST("admin/ambientes")
    suspend fun createAmbiente(@Header("Authorization") auth: String, @Body body: PeticionAmbiente): Ambiente

    @PUT("admin/ambientes/{id}")
    suspend fun updateAmbiente(
        @Header("Authorization") auth: String,
        @Path("id") id: Int,
        @Body body: PeticionAmbiente
    ): Ambiente

    @DELETE("admin/ambientes/{id}")
    suspend fun deleteAmbiente(@Header("Authorization") auth: String, @Path("id") id: Int): RespuestaMensaje

    @GET("admin/ambientes/{id}/horarios")
    suspend fun getHorarios(@Header("Authorization") auth: String, @Path("id") id: Int): List<HorarioAmbiente>

    @POST("admin/ambientes/{id}/horario")
    suspend fun createHorario(
        @Header("Authorization") auth: String,
        @Path("id") id: Int,
        @Body body: PeticionHorario
    ): HorarioAmbiente

    @DELETE("admin/ambiente-horarios/{id}")
    suspend fun deleteHorario(@Header("Authorization") auth: String, @Path("id") id: Int): RespuestaMensaje

    // ---- Novedades: crear y eliminar (el propietario o un admin) ----
    @POST("novedades")
    suspend fun createNovedad(@Header("Authorization") auth: String, @Body body: PeticionNovedad): Novedad

    @DELETE("novedades/{id}")
    suspend fun deleteNovedad(@Header("Authorization") auth: String, @Path("id") id: Int): RespuestaMensaje

    // ---- Sugerencias: crear y ver las propias (cualquier rol), bandeja y
    // respuesta (solo admin). Rutas del SugerenciaController del backend.
    @GET("my-sugerencias")
    suspend fun getMisSugerencias(@Header("Authorization") auth: String): List<Sugerencia>

    @POST("sugerencias")
    suspend fun crearSugerencia(@Header("Authorization") auth: String, @Body body: PeticionSugerencia): Sugerencia

    @GET("sugerencias")
    suspend fun getSugerencias(
        @Header("Authorization") auth: String,
        @Query("q") q: String? = null,
        @Query("status") status: String? = null,
        @Query("categoria") categoria: String? = null,
        @Query("per_page") perPage: Int = 50
    ): RespuestaPaginaSugerencias

    @PUT("admin/sugerencias/{id}/responder")
    suspend fun responderSugerencia(
        @Header("Authorization") auth: String,
        @Path("id") id: Int,
        @Body body: PeticionResponderSugerencia
    ): Sugerencia

    @DELETE("admin/sugerencias/{id}")
    suspend fun eliminarSugerencia(@Header("Authorization") auth: String, @Path("id") id: Int): RespuestaMensaje

    // ---- Notificaciones in-app ----
    @GET("notifications")
    suspend fun getNotifications(@Header("Authorization") auth: String): List<Notificacion>

    @GET("notifications/unread-count")
    suspend fun getUnreadCount(@Header("Authorization") auth: String): ConteoNoLeidas

    @PUT("notifications/{id}/read")
    suspend fun markNotificationRead(@Header("Authorization") auth: String, @Path("id") id: Int): RespuestaMensaje

    @PUT("notifications/read-all")
    suspend fun markAllNotificationsRead(@Header("Authorization") auth: String): RespuestaMensaje

    // ---- Exportación de historial (solo admin): CSV (default), Excel o PDF,
    @GET("admin/ingresos/export")
    suspend fun exportIngresos(
        @Header("Authorization") auth: String,
        @Query("formato") formato: String = "csv",
        @Query("cols") cols: String? = null,
        @Query("desde") desde: String? = null,
        @Query("hasta") hasta: String? = null
    ): ResponseBody

    // ---- Excusas con PIN (instructor crea, admin valida) ----
    @POST("instructor/excusas")
    suspend fun crearExcusa(@Header("Authorization") auth: String, @Body body: PeticionCrearExcusa): Excusa

    @GET("instructor/excusas")
    suspend fun getExcusasInstructor(@Header("Authorization") auth: String): List<Excusa>

    @DELETE("instructor/excusas/{id}")
    suspend fun anularExcusa(@Header("Authorization") auth: String, @Path("id") id: Int): RespuestaMensaje

    @GET("mis-excusas")
    suspend fun getMisExcusas(@Header("Authorization") auth: String): List<Excusa>

    @POST("excusas/validar")
    suspend fun validarExcusa(@Header("Authorization") auth: String, @Body body: PeticionValidarExcusa): RespuestaValidarExcusa

    @GET("admin/excusas")
    suspend fun getExcusasAdmin(@Header("Authorization") auth: String): List<Excusa>
}
