// Guarda el reto 2FA llegado por deep link (botones "Sí/No soy yo" del correo).
// ActividadPrincipal lo escribe y el dashboard ("¿Eres tú?") lo consume sin cambiar
// firmas de navegación: por eso vive en un objeto global y no en parámetros.
package com.example.sennaccess.autenticacion.verificacion

import androidx.compose.runtime.mutableStateOf

object AlmacenEnlaceDobleFactor {
    // Id del reto a preseleccionar en el dashboard con sesión.
    var challengeId = mutableStateOf<String?>(null)
        private set
    // Decisión del correo ("aprobar"/"denegar"): el dashboard la ejecuta
    // automáticamente para que los botones del correo sean funcionales.
    var decision = mutableStateOf<String?>(null)
        private set

    fun guardar(id: String?, dec: String?) {
        if (id.isNullOrBlank()) return
        challengeId.value = id
        val d = dec?.trim()?.lowercase()
        decision.value = if (d == "aprobar" || d == "denegar" || d == "si" || d == "no") {
            if (d == "si") "aprobar" else if (d == "no") "denegar" else d
        } else null
    }

    fun limpiar() {
        challengeId.value = null
        decision.value = null
    }
}
