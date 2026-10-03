package com.example.sennaccess.datos.sesion

// Guarda en memoria los datos de la sesión activa (token y perfil del usuario).
// No persiste en disco: la sesión se pierde al reiniciar la app, por diseño.
// Además persiste la lista de correos usados en el login (SharedPreferences
// "login_prefs") para sugerir autocompletado en el campo de correo.

import android.content.Context
import com.example.sennaccess.datos.red.ClienteApi

object GestorSesion {
    // Preferencias donde persiste la ruta de la foto para no perderla al reiniciar.
    // La clave incluye el id del usuario: nunca se restaura la foto de otra cuenta.
    private const val PREFS = "sena_session"
    private const val KEY_FOTO_BASE = "foto_perfil"

    @Volatile
    private var appContext: Context? = null

    var token: String? = null
        private set
    var userId: Int? = null
        private set
    var userName: String? = null
        private set
    var userEmail: String? = null
        private set
    var userRole: String? = null
        private set
    var userPhoto: String? = null
        private set
    // Versión de foto: se incrementa en cada guardado para que Coil tome la
    // imagen nueva al instante (cache-busting ?v=N) sin esperar recarga.
    var photoVersion: Int = 0
        private set
    var emailVerified: Boolean? = null
        private set

    private fun claveFoto(id: Int?): String =
        if (id != null) "${KEY_FOTO_BASE}_$id" else KEY_FOTO_BASE

    // Enlaza el contexto. No restaura ninguna foto sin sesión: la foto fantasma
    // de una cuenta anterior nunca debe aparecer al arrancar.
    fun enlazar(context: Context) {
        appContext = context.applicationContext
    }

    // Guarda la sesión tras un acceso exitoso.
    fun saveSession(token: String?, id: Int?, name: String?, email: String?, role: String?, emailVerified: Boolean? = null) {
        this.token = token
        this.userId = id
        this.userName = name
        this.userEmail = email
        this.userRole = role
        this.emailVerified = emailVerified
    }

    // Registra si el correo ya fue verificado.
    fun saveEmailVerified(verified: Boolean?) {
        this.emailVerified = verified
    }

    // Guarda la foto de perfil y la persiste ligada al usuario actual.
    // Incrementa photoVersion para refresco instantáneo en FotoPerfil.
    fun savePhoto(photo: String?) {
        this.userPhoto = photo
        photoVersion++
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()?.putString(claveFoto(userId), photo)?.apply()
    }

    // Actualiza solo el token (p. ej. al renovarse).
    fun saveToken(token: String?) {
        this.token = token
    }

    // Cierra la sesión local y limpia los datos, incluida la foto persistida.
    fun clear() {
        val idPrevio = userId
        token = null
        userId = null
        userName = null
        userEmail = null
        userRole = null
        userPhoto = null
        emailVerified = null
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.let { editor ->
            editor.remove(claveFoto(idPrevio))
            editor.remove(KEY_FOTO_BASE)
            editor.apply()
        }
    }

    // Limpia todo rastro de sesión en disco aunque aún no haya contexto enlazado
    // (purga de versión): borra las fotos de cualquier usuario anterior.
    fun purgarDisco(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    // Cabecera Bearer para las llamadas autenticadas.
    fun authHeader(): String? = token?.let { "Bearer $it" }

    // Resuelve la foto (absoluta o relativa) a URL cargable.
    fun fotoUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http")) return path
        val base = ClienteApi.raizServidor()
        return base.trimEnd('/') + (if (path.startsWith("/")) "" else "/") + path
    }

    // Guarda el correo para el autocompletado del acceso.
    fun guardarCorreoUsado(context: Context, email: String) {
        val limpio = email.trim()
        if (limpio.isBlank()) return
        val prefs = context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        val actual = prefs.getString("correos_guardados", "")?.split("|")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
        actual.removeAll { it.equals(limpio, ignoreCase = true) }
        actual.add(0, limpio)
        while (actual.size > 5) actual.removeAt(actual.lastIndex)
        prefs.edit().putString("correos_guardados", actual.joinToString("|")).apply()
    }

    // Últimos correos usados en este teléfono.
    fun obtenerCorreosUsados(context: Context): List<String> {
        val prefs = context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        val raw = prefs.getString("correos_guardados", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("|").filter { it.isNotBlank() }
    }

    // Autorrelleno de contraseña: guarda la clave del último ingreso exitoso
    // por correo (prefs privadas del dispositivo) y la devuelve al elegir
    // ese correo en el login. No viaja a ningún servidor.
    private const val PREFS_CLAVES = "login_claves"
    fun guardarClavePara(context: Context, email: String, password: String) {
        val correo = email.trim().lowercase()
        if (correo.isBlank() || password.isEmpty()) return
        context.getSharedPreferences(PREFS_CLAVES, Context.MODE_PRIVATE)
            .edit().putString(correo, password).apply()
    }
    fun obtenerClavePara(context: Context, email: String): String? {
        val correo = email.trim().lowercase()
        if (correo.isBlank()) return null
        return context.getSharedPreferences(PREFS_CLAVES, Context.MODE_PRIVATE)
            .getString(correo, null)?.takeIf { it.isNotEmpty() }
    }
}
