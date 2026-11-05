package com.example.sennaccess.comun

// Catálogos y reglas de jornada del SENA.
// Centraliza jornadas, horarios automáticos por jornada, ubicaciones de centros
// y la jornada efectiva por día (L-V base + sábado especial).

object Jornadas {
    const val MANANA = "Mañana"
    const val TARDE = "Tarde"
    const val NOCHE = "Noche"
    val TODAS = listOf(MANANA, TARDE, NOCHE)

    // Hora automática al elegir jornada. El horario NO se edita a mano: cada
    // jornada tiene su franja fija y así no se guardan horas incoherentes.
    fun horasPara(jornada: String): Pair<String, String> = when (jornada.trim().lowercase()) {
        "mañana", "manana" -> "06:00" to "13:00"
        "tarde" -> "13:00" to "18:00"
        "noche", "nocturna" -> "18:00" to "22:00"
        else -> "13:00" to "18:00"
    }

    // Texto legible de la franja: "6 a.m. – 1 p.m.".
    fun textoHorario(jornada: String?): String? {
        if (normalizar(jornada) == null) return null
        val (ini, fin) = horasPara(jornada!!)
        return "${a12h(ini)} – ${a12h(fin)}"
    }

    // Convierte "13:00" o "13:00:00" a "1:00 p.m.".
    fun a12h(hora: String?): String {
        val limpia = horaCorta(hora) ?: return "—"
        val h = limpia.substringBefore(":").toIntOrNull() ?: return limpia
        val m = limpia.substringAfter(":", "00")
        val sufijo = if (h < 12) "a.m." else "p.m."
        val h12 = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
        return if (m == "00") "$h12 $sufijo" else "$h12:$m $sufijo"
    }

    // El backend devuelve "13:00:00"; las pantallas necesitan "13:00".
    fun horaCorta(hora: String?): String? {
        val t = hora?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val partes = t.split(":")
        if (partes.size < 2) return t
        return "${partes[0].padStart(2, '0')}:${partes[1]}"
    }

    fun normalizar(jornada: String?): String? = when (jornada?.trim()?.lowercase()) {
        "mañana", "manana" -> MANANA
        "tarde" -> TARDE
        "noche", "nocturna" -> NOCHE
        else -> null
    }
}

object UbicacionesSena {
    // Desplegable al crear ambientes: sedes reales, sin texto libre.
    val SEDES = listOf(
        "CCyS",
        "Ciudad Jardín",
        "Cazuca",
        "Soacha Centro",
        "Sibaté",
        "Otra sede"
    )
}

object JornadaEfectiva {
    // Aprendiz de tarde L-V pero de mañana los sábados: jornada_base + jornada_sabado.
    // Si es sábado (Calendar.SATURDAY) y hay jornada_sabado, manda la del sábado.
    fun paraHoy(jornadaBase: String?, jornadaSabado: String?): String? {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("America/Bogota"))
        val esSabado = cal.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.SATURDAY
        return if (esSabado && !jornadaSabado.isNullOrBlank()) jornadaSabado else jornadaBase
    }

    fun esSabadoHoy(): Boolean {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("America/Bogota"))
        return cal.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.SATURDAY
    }
}
