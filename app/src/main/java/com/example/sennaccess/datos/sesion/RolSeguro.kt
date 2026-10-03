package com.example.sennaccess.datos.sesion

// Normaliza el rol del backend a un valor cerrado.
// Roles válidos: aprendiz, instructor, admin y portero. Invitado es temporal
// (QR de un solo uso) y nunca navega a un dashboard.
object RolSeguro {
    // Convierte " Administrador " en "admin" y deja nulo lo desconocido.
    fun normalizar(role: String?): String? {
        return when (role?.trim()?.lowercase()) {
            "aprendiz" -> "aprendiz"
            "instructor" -> "instructor"
            "admin", "administrador" -> "admin"
            "portero", "portería", "porteria", "celador" -> "portero"
            "invitado", "visitante", "guest" -> "invitado"
            else -> null
        }
    }

    // Indica si el rol pertenece a la lista cerrada de la app.
    // Invitado temporal no es válido para dashboard.
    fun esValido(role: String?): Boolean {
        val n = normalizar(role)
        return n == "aprendiz" || n == "instructor" || n == "admin" || n == "portero"
    }

    // True si el usuario es temporal y no debe listarse en ambientes/equipos.
    fun esTemporal(role: String?): Boolean = normalizar(role) == "invitado"
}
