package com.example.sennaccess.datos.repositorios

import com.example.sennaccess.datos.red.ClienteApi
import com.example.sennaccess.datos.modelos.EstadoConfigDobleFactor
import com.example.sennaccess.datos.modelos.EstadoVerificacion
import com.example.sennaccess.datos.modelos.PendientesDobleFactor
import com.example.sennaccess.datos.modelos.PeticionAprobarReto
import com.example.sennaccess.datos.modelos.PeticionDesactivarDobleFactor
import com.example.sennaccess.datos.modelos.PeticionValidarCodigo
import com.example.sennaccess.datos.modelos.RespuestaAcceso
import com.example.sennaccess.datos.modelos.RespuestaMensaje

// Repositorio de la verificación en dos pasos: activación, código del correo
// y el flujo de aprobación "¿Eres tú?" entre dispositivos.

class RepositorioDobleFactor {

    // Consulta si el usuario tiene activo el segundo factor.
    suspend fun estadoConfig(token: String): EstadoConfigDobleFactor =
        ClienteApi.conServicio { it.estadoConfig2Fa("Bearer $token") }

    // Activa la verificación en dos pasos desde el perfil.
    suspend fun activar(token: String): EstadoConfigDobleFactor =
        ClienteApi.conServicio { it.activar2Fa("Bearer $token") }

    // Desactiva el segundo factor previa contraseña.
    suspend fun desactivar(token: String, contrasena: String): EstadoConfigDobleFactor =
        ClienteApi.conServicio { it.desactivar2Fa("Bearer $token", PeticionDesactivarDobleFactor(contrasena)) }

    // Valida el código del correo y completa el inicio de sesión.
    suspend fun validarCodigo(challengeId: String, code: String): RespuestaAcceso =
        ClienteApi.conServicio { it.validarCodigo2Fa(PeticionValidarCodigo(challengeId, code)) }

    // Estado del reto, para el polling del teléfono que intenta entrar.
    suspend fun estadoChallenge(challengeId: String): EstadoVerificacion =
        ClienteApi.conServicio { it.estadoVerificacion2Fa(challengeId) }

    // Retos pendientes de aprobar en este dispositivo.
    suspend fun pendientes(token: String): PendientesDobleFactor =
        ClienteApi.conServicio { it.pendientes2Fa("Bearer $token") }

    // Aprueba o rechaza un acceso intentado desde otro dispositivo.
    suspend fun aprobar(token: String, challengeId: String, decision: String): RespuestaMensaje =
        ClienteApi.conServicio { it.aprobar2Fa("Bearer $token", PeticionAprobarReto(challengeId, decision)) }
}
