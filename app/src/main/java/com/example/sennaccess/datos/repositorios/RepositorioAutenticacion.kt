package com.example.sennaccess.datos.repositorios

import com.example.sennaccess.datos.red.ClienteApi
import com.example.sennaccess.datos.modelos.PeticionAcceso
import com.example.sennaccess.datos.modelos.PeticionRecuperacion
import com.example.sennaccess.datos.modelos.PeticionRegistro
import com.example.sennaccess.datos.modelos.PeticionRestablecer
import com.example.sennaccess.datos.modelos.RespuestaAcceso
import com.example.sennaccess.datos.modelos.RespuestaMensaje
import com.example.sennaccess.datos.modelos.RespuestaRegistro
import com.example.sennaccess.datos.modelos.RespuestaSalida

// Repositorio de autenticación: login, registro, recuperación de clave y salida.
// El cierre de sesión es best-effort: la app limpia su estado aunque falle la red.

class RepositorioAutenticacion {

    // Inicia sesión con correo y clave; incluye el id del dispositivo para el 2FA.
    suspend fun login(email: String, password: String, deviceId: String? = null): RespuestaAcceso =
        ClienteApi.conServicio { it.login(PeticionAcceso(email, password, deviceId)) }

    // Registra una cuenta nueva de aprendiz.
    suspend fun register(body: PeticionRegistro): RespuestaRegistro =
        ClienteApi.conServicio { it.register(body) }

    // Pide el código de recuperación al correo indicado.
    suspend fun forgotPassword(email: String): RespuestaMensaje =
        ClienteApi.conServicio { it.forgotPassword(PeticionRecuperacion(email)) }

    // Cambia la clave usando el código recibido por correo.
    suspend fun resetPassword(body: PeticionRestablecer): RespuestaMensaje =
        ClienteApi.conServicio { it.resetPassword(body) }

    // Reenvía el enlace de verificación al correo registrado.
    suspend fun resendVerification(token: String): RespuestaMensaje =
        ClienteApi.conServicio { it.resendVerification("Bearer $token") }

    // Cierra la sesión en el servidor y registra la salida.
    suspend fun logout(token: String): RespuestaSalida =
        ClienteApi.conServicio { it.logout("Bearer $token") }
}
