package com.example.sennaccess.comun

// Utilidades de formato de fechas para las vistas.
// El backend devuelve fechas ISO ("2026-08-25T14:30:00.000000Z"); estas funciones
// las convierten a texto legible en español sin depender de java.time (minSdk 24).

import java.util.Calendar
import java.util.TimeZone

// Patrón que cubre el ISO del backend con o sin microsegundos y con zona (Z o ±HH:MM).
private val patronIso = Regex(
    """(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2}):(\d{2})(\.\d+)?(Z|[+-]\d{2}:?\d{2})?"""
)

private val meses = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

fun fechaLegible(iso: String?): String {
    val d = parsearIso(iso) ?: return "—"
    return "${d.get(Calendar.DAY_OF_MONTH)} ${meses[d.get(Calendar.MONTH)]} ${d.get(Calendar.YEAR)}, " +
        "${dosDigitos(d.get(Calendar.HOUR_OF_DAY))}:${dosDigitos(d.get(Calendar.MINUTE))}"
}

fun fechaHoraCorta(iso: String?): String {
    val d = parsearIso(iso) ?: return "—"
    return "${d.get(Calendar.DAY_OF_MONTH)} ${meses[d.get(Calendar.MONTH)]}, " +
        "${dosDigitos(d.get(Calendar.HOUR_OF_DAY))}:${dosDigitos(d.get(Calendar.MINUTE))}"
}

fun horaCorta(iso: String?): String {
    val d = parsearIso(iso) ?: return "—"
    val hoy = Calendar.getInstance()
    val mismoDia = d.get(Calendar.YEAR) == hoy.get(Calendar.YEAR) &&
        d.get(Calendar.DAY_OF_YEAR) == hoy.get(Calendar.DAY_OF_YEAR)
    return if (mismoDia) "${dosDigitos(d.get(Calendar.HOUR_OF_DAY))}:${dosDigitos(d.get(Calendar.MINUTE))}"
    else fechaHoraCorta(iso)
}

fun fechaRelativa(iso: String?): String {
    val d = parsearIso(iso) ?: return "—"
    val diff = System.currentTimeMillis() - d.timeInMillis
    if (diff < 0 || diff > 7 * 24 * 3600 * 1000L) return fechaLegible(iso)
    val minutos = diff / 60000
    return when {
        minutos < 1 -> "hace un momento"
        minutos < 60 -> "hace $minutos min"
        minutos < 1440 -> "hace ${minutos / 60} h"
        else -> "hace ${minutos / 1440} d"
    }
}

private fun parsearIso(iso: String?): Calendar? {
    if (iso.isNullOrBlank()) return null
    val m = patronIso.find(iso) ?: return null
    val (anio, mes, dia, hora, min, seg, _, zona) = m.destructured
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(Calendar.YEAR, anio.toInt())
        set(Calendar.MONTH, mes.toInt() - 1)
        set(Calendar.DAY_OF_MONTH, dia.toInt())
        set(Calendar.HOUR_OF_DAY, hora.toInt())
        set(Calendar.MINUTE, min.toInt())
        set(Calendar.SECOND, seg.toInt())
    }
    if (zona.isNotEmpty() && zona != "Z" && zona != "z") {
        val signo = if (zona.startsWith("-")) -1 else 1
        val numeros = zona.replace(":", "").drop(1)
        val hh = numeros.take(2).toIntOrNull() ?: 0
        val mm = numeros.drop(2).take(2).toIntOrNull() ?: 0
        cal.add(Calendar.MINUTE, signo * (hh * 60 + mm))
    }
    cal.timeZone = TimeZone.getDefault()
    return cal
}

private fun dosDigitos(n: Int): String = if (n < 10) "0$n" else n.toString()
