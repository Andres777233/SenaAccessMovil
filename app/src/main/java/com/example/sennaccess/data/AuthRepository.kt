package com.example.sennaccess.data

// Repositorio de autenticación: encapsula las llamadas de sesión del backend.
// Incluye login, registro de cuentas, solicitud/cambio de contraseña y el cierre
// de sesión best-effort (la app limpia su estado local aunque falle).

class AuthRepository {

    // Autentica al usuario con correo y contraseña y devuelve la respuesta del login.
    // device_id identifica ESTE teléfono para el 2FA por dispositivo (ver DispositivoStore).
    suspend fun login(email: String, password: String, deviceId: String? = null): LoginResponse =
        RetrofitClient.conServicio { it.login(LoginRequest(email, password, deviceId)) }

    // Registra una cuenta nueva (rol Aprendiz por defecto en el backend).
    suspend fun register(body: RegisterRequest): RegisterResponse =
        RetrofitClient.conServicio { it.register(body) }

    // Solicita el envío del código de recuperación al correo indicado.
    suspend fun forgotPassword(email: String): MessageResponse =
        RetrofitClient.conServicio { it.forgotPassword(ForgotRequest(email)) }

    // Cambia la contraseña usando el código de recuperación recibido.
    suspend fun resetPassword(body: ResetRequest): MessageResponse =
        RetrofitClient.conServicio { it.resetPassword(body) }

    // Reenvía el enlace de verificación de correo al usuario autenticado.
    suspend fun resendVerification(token: String): MessageResponse =
        RetrofitClient.conServicio { it.resendVerification("Bearer $token") }

    // Cierra la sesión en el servidor (registra la "Salida" y revoca el token).
    suspend fun logout(token: String): LogoutResponse =
        RetrofitClient.conServicio { it.logout("Bearer $token") }
}
