package com.example.sennaccess.comun.diseno

// Rutas tipadas que envuelven el router manual de ActividadPrincipal. Eliminan los
// strings mágicos en dashboards: cada rol expone su enum y el dock deriva la
// pestaña activa aunque haya sub-pantallas (PERFIL, NOTIS, 2FA -> Inicio).
object RutasSena {
    const val SPLASH = "splash"
    const val LANDING = "landing"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val RECOVERY = "recovery"
    const val RESET = "reset"
    const val APRENDIZ = "aprendiz_dashboard"
    const val INSTRUCTOR = "instructor_dashboard"
    const val ADMIN = "admin"
}

object DestinoAprendiz {
    const val DASHBOARD = "DASHBOARD"
    const val MIS_EXCUSAS = "MIS_EXCUSAS"
    const val HISTORIAL = "HISTORIAL"
    const val COMPROBANTES = "COMPROBANTES"
    const val PERFIL = "PERFIL"
    const val EDITAR_PERFIL = "EDITAR_PERFIL"
    const val NOTIFICACIONES = "NOTIFICACIONES"
    const val VERIFICACION_2FA = "VERIFICACION_2FA"
    const val INICIO = "DASHBOARD"
}

object DestinoInstructor {
    const val DASHBOARD = "DASHBOARD"
    const val AMBIENTES = "AMBIENTES"
    const val NOVEDADES = "NOVEDADES"
    const val HISTORIAL = "HISTORIAL"
    const val MIS_EQUIPOS = "MIS_EQUIPOS"
    const val PERFIL = "PERFIL"
    const val EDITAR_PERFIL = "EDITAR_PERFIL"
    const val NOTIFICACIONES = "NOTIFICACIONES"
    const val VERIFICACION_2FA = "VERIFICACION_2FA"
    const val INICIO = "DASHBOARD"
}

object PestanaAdmin {
    const val INICIO = "INICIO"
    const val NOVEDADES = "NOVEDADES"
    const val USUARIOS = "USUARIOS"
    const val AMBIENTES = "AMBIENTES"
}

object PestanaPortero {
    const val VALIDAR = "VALIDAR"
    const val EQUIPOS = "EQUIPOS"
    const val HISTORIAL = "HISTORIAL"
}

fun claveNavegacion(view: String, fallback: String): String {
    val v = view.uppercase()
    return when {
        v in setOf("PERFIL", "EDITAR_PERFIL", "NOTIFICACIONES", "VERIFICACION_2FA", "NOTIS", "2FA") -> fallback
        else -> view
    }
}

fun esSubPantalla(view: String): Boolean {
    return view.uppercase() in setOf(
        "PERFIL", "EDITAR_PERFIL", "NOTIFICACIONES", "NOTIS",
        "VERIFICACION_2FA", "2FA", "AMBIENTES", "VALIDAR_EXCUSA"
    )
}
