package com.example.sennaccess.data

// Guarda en memoria los datos de la sesión activa (token y perfil del usuario).
// No persiste en disco: la sesión se pierde al reiniciar la app, por diseño.

object SessionManager {
    // Datos de la sesión actual. Los setters son privados: solo pueden modificarse a
    // través de los métodos de este objeto para evitar estados inconsistentes.
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

    // Almacena la sesión completa tras un login exitoso.
    fun saveSession(token: String?, id: Int?, name: String?, email: String?, role: String?) {
        this.token = token
        this.userId = id
        this.userName = name
        this.userEmail = email
        this.userRole = role
    }

    // Guarda o actualiza la URL de la foto de perfil (relativa "/avatars/x" o absoluta).
    fun savePhoto(photo: String?) {
        this.userPhoto = photo
    }

    // Actualiza solo el token, por ejemplo cuando se renueva.
    fun saveToken(token: String?) {
        this.token = token
    }

    // Cierra la sesión y limpia todos los datos; se usa al hacer logout.
    fun clear() {
        token = null
        userId = null
        userName = null
        userEmail = null
        userRole = null
        userPhoto = null
    }

    // Construye el header HTTP de autorización con el esquema Bearer a partir del token
    // guardado. Devuelve null si no hay sesión (se usa para decidir el fallback a mocks).
    fun authHeader(): String? = token?.let { "Bearer $it" }

    // Convierte la ruta de la foto en una URL que Coil pueda cargar. Si ya es absoluta
    // (Cloudinary) se devuelve tal cual; si es relativa ("/avatars/x.jpg") se antepone
    // la raíz del servidor activo (USB 127.0.0.1 o WiFi 192.168.x.x), sin "/api".
    fun fotoUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http")) return path
        val base = RetrofitClient.raizServidor()
        return base.trimEnd('/') + (if (path.startsWith("/")) "" else "/") + path
    }
}