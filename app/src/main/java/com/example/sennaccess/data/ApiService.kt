package com.example.sennaccess.data

// Interfaz que declara los endpoints REST del backend como funciones suspend de Retrofit.
// Cada anotación (@GET/@POST) se resuelve contra la baseUrl de RetrofitClient:
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

interface ApiService {

    // ---- Auth ----
    // 1. Login (POST /api/login): envía email y contraseña en el cuerpo de la petición.
    //    Es el único endpoint público, no requiere token. Devuelve el usuario, su rol
    //    y el access_token que la app guardará para autenticar el resto de llamadas.
    @POST("login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    // 1a. POST /api/register: registro público de nuevas cuentas (rol Aprendiz por defecto).
    @POST("register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse

    // 1b. POST /api/logout: cierra la sesión en el servidor. Requiere el token Bearer.
    //    Registra la "Salida" del usuario en el historial antes de invalidar el token.
    @POST("logout")
    suspend fun logout(@Header("Authorization") auth: String): LogoutResponse

    // 1c. POST /api/forgot-password: solicita el código de recuperación al correo.
    @POST("forgot-password")
    suspend fun forgotPassword(@Body body: ForgotRequest): MessageResponse

    // 1d. POST /api/reset-password: cambia la contraseña con el código recibido.
    @POST("reset-password")
    suspend fun resetPassword(@Body body: ResetRequest): MessageResponse

    // ---- WebAuthn (passkeys): login con huella ----
    // El servidor genera un reto, el dispositivo lo firma con su llave privada
    // (desbloqueada por la biometría) y aquí se verifica la firma. El registro
    // requiere sesión; el login es público y devuelve el mismo LoginResponse.
    @POST("webauthn/login/options")
    suspend fun getWebauthnLoginOptions(): WebauthnOptionsResponse

    @POST("webauthn/login")
    suspend fun loginWithPasskey(@Body body: Map<String, String>): LoginResponse

    @POST("webauthn/register/options")
    suspend fun getWebauthnRegisterOptions(@Header("Authorization") auth: String): WebauthnOptionsResponse

    @POST("webauthn/register")
    suspend fun registerPasskey(
        @Header("Authorization") auth: String,
        @Body body: Map<String, String>
    ): MessageResponse

    @GET("webauthn/passkeys")
    suspend fun getMyPasskeys(@Header("Authorization") auth: String): List<PasskeyInfo>

    @DELETE("webauthn/passkeys/{id}")
    suspend fun deletePasskey(
        @Header("Authorization") auth: String,
        @Path("id") id: Int
    ): MessageResponse

    // ---- Cualquier rol (sesión) ----
    // 2. GET /api/user: perfil del usuario autenticado. Cualquier rol con sesión puede
    //    pedir sus propios datos; el token viaja en el header "Authorization: Bearer".
    @GET("user")
    suspend fun getCurrentUser(@Header("Authorization") auth: String): UsuarioApi

    // 3. GET /api/my-ingresos: registros de ingreso del usuario logueado. Listado
    //    personal, disponible para cualquier rol autenticado.
    @GET("my-ingresos")
    suspend fun getMyIngresos(@Header("Authorization") auth: String): List<Ingreso>

    // 4. GET /api/my-equipment: equipos registrados a nombre del usuario actual.
    @GET("my-equipment")
    suspend fun getMyEquipment(@Header("Authorization") auth: String): List<IngresoEquipo>

    // 4b. PUT /api/my-profile: actualiza el perfil del usuario logueado.
    @PUT("my-profile")
    suspend fun updateMyProfile(@Header("Authorization") auth: String, @Body body: UpdateProfileRequest): UsuarioApi

    // 4c. POST /api/my-profile con _method=PUT y multipart/form-data: misma actualización
    //     del perfil pero incluyendo la foto (campo "image") para subirla al servidor.
    //     Laravel interpreta el campo "_method" como PUT real sobre la ruta my-profile.
    //     ficha y programa son opcionales (null): Retrofit omite esas partes, lo que
    //     permite subir foto a roles que no tienen ficha, como el administrador.
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
        @Part("user_program") programa: okhttp3.RequestBody? = null
    ): UsuarioApi

    // ---- Admin / Instructor ----
    // 5. Secciones administrativas: requieren rol admin o instructor y el token Bearer.
    @GET("admin/users")
    suspend fun getUsers(@Header("Authorization") auth: String): List<UsuarioApi>

    // 5b. CRUD de usuarios: crear, actualizar y eliminar desde el panel del admin.
    @POST("admin/users")
    suspend fun createUser(@Header("Authorization") auth: String, @Body body: UserRequest): UsuarioApi

    @PUT("admin/users/{id}")
    suspend fun updateUser(@Header("Authorization") auth: String, @Path("id") id: Int, @Body body: UserRequest): UsuarioApi

    @DELETE("admin/users/{id}")
    suspend fun deleteUser(@Header("Authorization") auth: String, @Path("id") id: Int): MessageResponse

    // 6. GET /api/admin/ingresos: registro global de ingresos de todos los usuarios.
    //    La respuesta es paginada ({data:[...]}); per_page=500 trae el historial
    //    completo de una sola vez para la vista móvil.
    @GET("admin/ingresos")
    suspend fun getIngresos(
        @Header("Authorization") auth: String,
        @Query("per_page") perPage: Int = 500
    ): IngresosResponse

    // 7. GET /api/admin/roles: catálogo de roles, útil para filtros y desplegables.
    @GET("admin/roles")
    suspend fun getRoles(@Header("Authorization") auth: String): List<Role>

    // 7a. GET /api/admin/presentes: usuarios que están DENTRO ahora (su último
    //     registro es una Entrada sin Salida). Solo admin.
    @GET("admin/presentes")
    suspend fun getPresentes(@Header("Authorization") auth: String): List<Presente>

    // 8. GET /api/my-novedades: novedades publicadas por el usuario logueado.
    @GET("my-novedades")
    suspend fun getMyNovedades(@Header("Authorization") auth: String): List<Novedad>

    // 9. GET /api/novedades: listado general de novedades con búsqueda opcional por
    //    texto, enviada como parámetro de consulta "search".
    @GET("novedades")
    suspend fun getNovedades(
        @Header("Authorization") auth: String,
        @Query("search") search: String? = null
    ): List<Novedad>

    // ---- Solo Admin ----
    // 10. GET /api/admin/equipment: inventario completo de equipos registrados en
    //     ingresos. Acceso restringido al rol admin.
    @GET("admin/equipment")
    suspend fun getEquipment(@Header("Authorization") auth: String): List<IngresoEquipo>

    // 11. POST /api/admin/equipment: registra el ingreso de un equipo. Disponible para
    //     admin e instructor; el backend asigna el usuario desde el token.
    @POST("admin/equipment")
    suspend fun createEquipment(
        @Header("Authorization") auth: String,
        @Body body: IngresoEquipoRequest
    ): EquipmentResponse

    // 11a. POST /api/my-equipment: registra el ingreso de un equipo a nombre del
    //     aprendiz autenticado (equivalente a /admin/equipment pero para rol Aprendiz).
    @POST("my-equipment")
    suspend fun createMyEquipment(
        @Header("Authorization") auth: String,
        @Body body: IngresoEquipoRequest
    ): EquipmentResponse

    // 11b. DELETE /api/admin/equipment/{id}: elimina un registro de equipo (solo admin).
    @DELETE("admin/equipment/{id}")
    suspend fun deleteEquipment(
        @Header("Authorization") auth: String,
        @Path("id") id: Int
    ): MessageResponse

    // 12. GET /api/ambientes: catálogo de ambientes para cualquier rol autenticado.
    @GET("ambientes")
    suspend fun getAmbientes(@Header("Authorization") auth: String): List<Ambiente>

    // 12a. GET /api/mis-ambientes: ambientes donde el instructor/aprendiz está asignado.
    @GET("mis-ambientes")
    suspend fun getMisAmbientes(@Header("Authorization") auth: String): List<Ambiente>

    @GET("mis-ambientes/{id}/aprendices")
    suspend fun getMisAprendices(@Header("Authorization") auth: String, @Path("id") id: Int): List<UsuarioApi>

    @POST("mis-ambientes/{id}/aprendices")
    suspend fun addMisAprendiz(@Header("Authorization") auth: String, @Path("id") id: Int, @Body body: AmbienteAprendizRequest): MessageResponse

    @DELETE("mis-ambientes/{id}/aprendices/{userId}")
    suspend fun removeMisAprendiz(@Header("Authorization") auth: String, @Path("id") id: Int, @Path("userId") userId: Int): MessageResponse

    @GET("admin/ambientes/{id}/aprendices")
    suspend fun getAprendicesDeAmbiente(@Header("Authorization") auth: String, @Path("id") id: Int): List<UsuarioApi>

    @POST("admin/ambientes/{id}/aprendices")
    suspend fun addAprendizAAmbiente(@Header("Authorization") auth: String, @Path("id") id: Int, @Body body: AmbienteAprendizRequest): MessageResponse

    @DELETE("admin/ambientes/{id}/aprendices/{userId}")
    suspend fun removeAprendizDeAmbiente(@Header("Authorization") auth: String, @Path("id") id: Int, @Path("userId") userId: Int): MessageResponse

    @POST("admin/ambientes/{id}/instructores")
    suspend fun syncInstructores(@Header("Authorization") auth: String, @Path("id") id: Int, @Body body: AmbienteInstructoresRequest): Ambiente

    // ---- Ambientes (solo admin) ----
    // 13. CRUD de ambientes: crear, editar, eliminar y consultar horarios.
    @POST("admin/ambientes")
    suspend fun createAmbiente(@Header("Authorization") auth: String, @Body body: AmbienteRequest): Ambiente

    @PUT("admin/ambientes/{id}")
    suspend fun updateAmbiente(
        @Header("Authorization") auth: String,
        @Path("id") id: Int,
        @Body body: AmbienteRequest
    ): Ambiente

    @DELETE("admin/ambientes/{id}")
    suspend fun deleteAmbiente(@Header("Authorization") auth: String, @Path("id") id: Int): MessageResponse

    @GET("admin/ambientes/{id}/horarios")
    suspend fun getHorarios(@Header("Authorization") auth: String, @Path("id") id: Int): List<HorarioAmbiente>

    @POST("admin/ambientes/{id}/horario")
    suspend fun createHorario(
        @Header("Authorization") auth: String,
        @Path("id") id: Int,
        @Body body: HorarioRequest
    ): HorarioAmbiente

    @DELETE("admin/ambiente-horarios/{id}")
    suspend fun deleteHorario(@Header("Authorization") auth: String, @Path("id") id: Int): MessageResponse

    // ---- Novedades: crear y eliminar (el propietario o un admin) ----
    @POST("novedades")
    suspend fun createNovedad(@Header("Authorization") auth: String, @Body body: NovedadRequest): Novedad

    @DELETE("novedades/{id}")
    suspend fun deleteNovedad(@Header("Authorization") auth: String, @Path("id") id: Int): MessageResponse

    // ---- Notificaciones in-app ----
    @GET("notifications")
    suspend fun getNotifications(@Header("Authorization") auth: String): List<Notificacion>

    @GET("notifications/unread-count")
    suspend fun getUnreadCount(@Header("Authorization") auth: String): UnreadCount

    @PUT("notifications/{id}/read")
    suspend fun markNotificationRead(@Header("Authorization") auth: String, @Path("id") id: Int): MessageResponse

    @PUT("notifications/read-all")
    suspend fun markAllNotificationsRead(@Header("Authorization") auth: String): MessageResponse

    // ---- Exportación de historial a CSV (solo admin) ----
    @GET("admin/ingresos/export")
    suspend fun exportIngresos(@Header("Authorization") auth: String): ResponseBody

    // ---- Jornada / Presencia física (FSM) ----
    // El servidor valida ventana NTP (configurable por ambiente), TOTP
    // y geolocalización/BSSID; la app solo recolecta la prueba y orquesta la UI.
    @GET("jornada/estado")
    suspend fun getJornadaEstado(@Header("Authorization") auth: String): JornadaEstadoResponse

    @POST("jornada/en-aula")
    suspend fun postEnAula(@Header("Authorization") auth: String, @Body body: JornadaEnAulaRequest): JornadaEstadoResponse

    @POST("jornada/descanso")
    suspend fun postDescanso(@Header("Authorization") auth: String, @Body body: JornadaTransicionRequest): JornadaEstadoResponse

    @POST("jornada/regreso-aula")
    suspend fun postRegresoAula(@Header("Authorization") auth: String, @Body body: JornadaTransicionRequest): JornadaEstadoResponse

    @POST("jornada/finalizar")
    suspend fun postFinalizar(@Header("Authorization") auth: String, @Body body: JornadaTransicionRequest): JornadaEstadoResponse

    @POST("jornada/salida-anticipada")
    suspend fun postSalidaAnticipada(@Header("Authorization") auth: String, @Body body: SalidaAnticipadaRequest): JornadaEstadoResponse

    @POST("jornada/emitir-permiso")
    suspend fun emitirPermiso(@Header("Authorization") auth: String, @Body body: EmitirPermisoRequest): EmitirPermisoResponse

    @GET("jornada/qr/{ambienteId}")
    suspend fun getQrAula(@Header("Authorization") auth: String, @Path("ambienteId") ambienteId: Int): JornadaQrResponse

    @GET("jornada/qr-actual")
    suspend fun getQrActual(@Header("Authorization") auth: String, @Query("ambiente_id") ambienteId: Int? = null): JornadaQrResponse

    @GET("jornada/auditoria")
    suspend fun getAuditoria(@Header("Authorization") auth: String): List<AuditoriaSalida>

    @GET("jornada/presentes")
    suspend fun getJornadaPresentes(@Header("Authorization") auth: String): List<Presente>

    // ---- Excusas con PIN (instructor crea, admin valida) ----
    @POST("instructor/excusas")
    suspend fun crearExcusa(@Header("Authorization") auth: String, @Body body: CrearExcusaRequest): Excusa

    @GET("instructor/excusas")
    suspend fun getExcusasInstructor(@Header("Authorization") auth: String): List<Excusa>

    @DELETE("instructor/excusas/{id}")
    suspend fun anularExcusa(@Header("Authorization") auth: String, @Path("id") id: Int): MessageResponse

    @GET("mis-excusas")
    suspend fun getMisExcusas(@Header("Authorization") auth: String): List<Excusa>

    @POST("excusas/validar")
    suspend fun validarExcusa(@Header("Authorization") auth: String, @Body body: ValidarExcusaRequest): ValidarExcusaResponse

    @GET("admin/excusas")
    suspend fun getExcusasAdmin(@Header("Authorization") auth: String): List<Excusa>
}
