package com.example.sennaccess.data

// Guardia de versión: al detectar una versión nueva (o primera instalación),
// borra todo rastro local (correos, dispositivo, huella, sesión y retos) para
// que la app arranque desde cero y nunca reutilice el rol o las credenciales
// de una versión anterior.
import android.content.Context
import com.example.sennaccess.BuildConfig
import com.example.sennaccess.ui.verificacion2fa.DeepLink2FaStore

object VersionGuard {
    // Prefs internas de la guardia (no se respaldan ni se limpian).
    private const val PREFS = "version_guard"
    private const val KEY_VERSION = "last_version"

    // Se llama una vez en MainActivity.onCreate, antes de pintar nada.
    fun aplicarSiActualizo(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val anterior = prefs.getInt(KEY_VERSION, -1)
        val actual = BuildConfig.VERSION_CODE
        if (anterior == actual) return
        // Versión distinta: purga total del estado local persistido.
        SessionManager.clear()
        DeepLink2FaStore.limpiar()
        HuellaCredentialStore.borrarTodo(context)
        context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("dispositivo_store", Context.MODE_PRIVATE).edit().clear().apply()
        prefs.edit().putInt(KEY_VERSION, actual).apply()
    }
}
