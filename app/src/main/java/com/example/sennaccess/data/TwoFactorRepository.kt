package com.example.sennaccess.data

// Repositorio de la verificación en dos pasos (2FA): activación desde el perfil,
// validación del código de correo, y el polling de los retos "¿Eres tú?".

class TwoFactorRepository {

    suspend fun estadoConfig(token: String): TwoFactorConfigEstado =
        RetrofitClient.conServicio { it.estadoConfig2Fa("Bearer $token") }

    suspend fun activar(token: String): TwoFactorConfigEstado =
        RetrofitClient.conServicio { it.activar2Fa("Bearer $token") }

    suspend fun desactivar(token: String, contrasena: String): TwoFactorConfigEstado =
        RetrofitClient.conServicio { it.desactivar2Fa("Bearer $token", Desactivar2FaRequest(contrasena)) }

    suspend fun validarCodigo(challengeId: String, code: String): LoginResponse =
        RetrofitClient.conServicio { it.validarCodigo2Fa(ValidarCodigo2FaRequest(challengeId, code)) }

    suspend fun estadoChallenge(challengeId: String): Verificacion2FaEstado =
        RetrofitClient.conServicio { it.estadoVerificacion2Fa(challengeId) }

    suspend fun pendientes(token: String): TwoFactorPendientes =
        RetrofitClient.conServicio { it.pendientes2Fa("Bearer $token") }

    suspend fun aprobar(token: String, challengeId: String, decision: String): MessageResponse =
        RetrofitClient.conServicio { it.aprobar2Fa("Bearer $token", Aprobar2FaRequest(challengeId, decision)) }
}