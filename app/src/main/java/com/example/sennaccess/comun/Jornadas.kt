package com.example.sennaccess.comun

// Catálogos y reglas de jornada del SENA.
// Centraliza jornadas, horarios automáticos por jornada, ubicaciones de centros
// y la jornada efectiva por día (L-V base + sábado especial).

object Jornadas {
    const val MANANA = "Mañana"
    const val TARDE = "Tarde"
    const val NOCHE = "Noche"
    val TODAS = listOf(MANANA, TARDE, NOCHE)

    // Hora automática al elegir jornada al crear un ambiente.
    fun horasPara(jornada: String): Pair<String, String> = when (jornada.trim().lowercase()) {
        "mañana", "manana" -> "06:00" to "12:00"
        "tarde" -> "13:00" to "18:00"
        "noche", "nocturna" -> "18:00" to "22:00"
        else -> "13:00" to "18:00"
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
