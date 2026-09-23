package com.example.sennaccess.administrador.novedades

// Registro local de novedades ya leídas (el backend no expone "leída").
// Guarda los IDs en SharedPreferences `novedades_leidas` por dispositivo.

import android.content.Context

object AlmacenNovedadesLeidas {
    private const val PREFS = "novedades_leidas"
    private const val KEY = "leidas"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun obtenerLeidas(context: Context): Set<String> =
        prefs(context).getStringSet(KEY, emptySet())?.toSet() ?: emptySet()

    fun marcarLeida(context: Context, id: Int) {
        val actual = obtenerLeidas(context).toMutableSet()
        actual.add(id.toString())
        prefs(context).edit().putStringSet(KEY, actual).apply()
    }

    fun marcarMuchas(context: Context, ids: Collection<Int>) {
        val actual = obtenerLeidas(context).toMutableSet()
        actual.addAll(ids.map { it.toString() })
        prefs(context).edit().putStringSet(KEY, actual).apply()
    }
}
