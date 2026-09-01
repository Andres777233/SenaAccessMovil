package com.example.sennaccess.jornada

// Proveedor de BSSID/SSID institucional (capa de red del módulo de presencia).
// Usa WifiManager estándar, sin dependencias extra; en Android 13+ requiere
// permiso de ubicación para leer el BSSID.

import android.content.Context
import android.net.wifi.WifiManager

// Datos de la red Wi-Fi actual (pueden ser null si no hay Wi-Fi o sin permisos).
data class WifiDato(
    val bssid: String?,
    val ssid: String?
)

// Lee el BSSID y SSID actuales de forma segura (null-safe).
// En Android 8+ el BSSID puede venir en mayúsculas con ":"; se normaliza a minúsculas.
fun obtenerWifiDato(context: Context): WifiDato {
    return try {
        val appCtx = context.applicationContext
        val wifi = appCtx.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val info = wifi?.connectionInfo
        val bssidRaw = info?.bssid
        val ssidRaw = info?.ssid
        // Filtra valores placeholder del sistema cuando no hay permisos o no hay conexión.
        val bssid = bssidRaw?.takeIf { it.isNotBlank() && it != "02:00:00:00:00:00" }?.lowercase()
        val ssid = ssidRaw?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
            ?.removeSurrounding("\"")
        WifiDato(bssid, ssid)
    } catch (_: Exception) {
        WifiDato(null, null)
    }
}
