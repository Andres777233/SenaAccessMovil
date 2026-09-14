package com.example.sennaccess.ui.ds

// Rutas tipadas que envuelven el router manual de MainActivity. Eliminan los
// strings mágicos en dashboards: cada rol expone su enum y el dock deriva la
// pestaña activa aunque haya sub-pantallas (PERFIL, NOTIS, 2FA -> Inicio).
object SenaRoutes {
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

// Destinos tipados por rol: eliminan strings mágicos en dashboards.
// Los valores String se conservan para rememberSaveable (mismo flujo, sin
// librería nueva ni cambio de comportamiento).
object AprendizDest {
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

// Destinos del instructor (dock de 5 + sub-pantallas internas).
object InstructorDest {
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

// Tabs del admin (dock de 6). Las sub-pantallas usan AdminScreen existente.
object AdminTab {
    const val INICIO = "INICIO"
    const val NOVEDADES = "NOVEDADES"
    const val PRESENTES = "PRESENTES"
    const val USUARIOS = "USUARIOS"
    const val EQUIPOS = "EQUIPOS"
    const val HISTORIAL = "HISTORIAL"
}

// Dada la vista actual (incluye sub-pantallas ocultas), devuelve la key que el
// dock debe resaltar. Evita el dock sin selección en PERFIL/NOTIS/2FA.
fun dockKeyFor(view: String, fallback: String): String {
    val v = view.uppercase()
    return when {
        v in setOf("PERFIL", "EDITAR_PERFIL", "NOTIFICACIONES", "VERIFICACION_2FA", "NOTIS", "2FA") -> fallback
        else -> view
    }
}

// Sub-pantallas que no tienen tab propio y deben tratarse como contenido
// interno (no rompen el dock ni la pila).
fun esSubPantalla(view: String): Boolean {
    return view.uppercase() in setOf(
        "PERFIL", "EDITAR_PERFIL", "NOTIFICACIONES", "NOTIS",
        "VERIFICACION_2FA", "2FA", "AMBIENTES", "VALIDAR_EXCUSA"
    )
}
