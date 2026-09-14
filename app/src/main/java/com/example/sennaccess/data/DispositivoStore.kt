package com.example.sennaccess.data

// Identificador estable de ESTE teléfono para la verificación en dos pasos: se
// genera una vez (UUID) y se envía en cada login como device_id. El backend solo
// debe pedir el segundo factor cuando el device_id difiere del dispositivo
// original del usuario (o al cambiar la clave desde el perfil); así el dueño no
// ve el código al entrar normal o con huella en su propio equipo. Reinstalar la
// app genera un id nuevo y el backend lo trata como dispositivo nuevo (seguro).

import android.content.Context
import java.util.UUID

object DispositivoStore {

    // Nombre de las preferencias y clave donde vive el id del dispositivo.
    private const val PREFS = "dispositivo_store"
    private const val KEY_ID = "device_id"

    // Devuelve el id existente o crea y persiste uno nuevo la primera vez.
    fun obtenerId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_ID, null)
        if (id.isNullOrBlank()) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_ID, id).apply()
        }
        return id
    }
}
