package com.example.sennaccess.datos.sesion

// Guardia de versión: al detectar una versión nueva (o primera instalación),
// borra todo rastro local (correos, dispositivo, huella, sesión y retos) para
// que la app arranque desde cero y nunca reutilice el rol o las credenciales
// de una versión anterior.
import android.content.Context
import com.example.sennaccess.BuildConfig
import com.example.sennaccess.autenticacion.verificacion.AlmacenEnlaceDobleFactor
import com.example.sennaccess.biometria.AlmacenHuella
object GuardianVersion {
    // Prefs internas de la guardia (no se respaldan ni se limpian).
    private const val PREFS = "version_guard"
    private const val KEY_VERSION = "last_version"

    // Si cambió la versión, purga datos heredados y arranca limpio.
    fun aplicarSiActualizo(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val anterior = prefs.getInt(KEY_VERSION, -1)
        val actual = BuildConfig.VERSION_CODE
        if (anterior == actual) return
        GestorSesion.purgarDisco(context)
        GestorSesion.clear()
        AlmacenEnlaceDobleFactor.limpiar()
        AlmacenHuella.borrarTodo(context)
        context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("dispositivo_store", Context.MODE_PRIVATE).edit().clear().apply()
        prefs.edit().putInt(KEY_VERSION, actual).apply()
    }
}
