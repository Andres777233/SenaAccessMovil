package com.example.sennaccess.datos.sesion

// Normaliza el rol del backend a un valor cerrado.
// Solo existen aprendiz, instructor y admin. Cualquier otro valor,
// vacio o nulo se considera invalido y nunca debe navegar a un dashboard.
object RolSeguro {
    // Convierte " Administrador " en "admin" y deja nulo lo desconocido.
    fun normalizar(role: String?): String? {
        return when (role?.trim()?.lowercase()) {
            "aprendiz" -> "aprendiz"
            "instructor" -> "instructor"
            "admin", "administrador" -> "admin"
            else -> null
        }
    }

    // Indica si el rol pertenece a la lista cerrada de la app.
    fun esValido(role: String?): Boolean = normalizar(role) != null
}
