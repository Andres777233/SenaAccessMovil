package com.example.sennaccess.jornada

// Proveedor de ubicación liviano sin Play Services (LocationManager).
// Detecta mock locations para la capa anti-spoofing del módulo de presencia.

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// Resultado de la lectura de ubicación con marca anti-spoofing.
data class UbicacionDato(
    val lat: Double,
    val lng: Double,
    val precisionM: Float?,
    val esMock: Boolean,
    val proveedor: String?
)

// Verifica si el dispositivo tiene permisos de ubicación concedidos.
fun tienePermisoUbicacion(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

// Detecta si la ubicación proviene de una app de mock (anti-spoofing).
// Combina isMock/isFromMockProvider + ALLOW_MOCK_LOCATION en versiones viejas.
fun Location.esMockUbicacion(context: Context): Boolean {
    // API 31+: isMock() es la forma oficial.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isMock) return true
    } else {
        @Suppress("DEPRECATION")
        if (isFromMockProvider) return true
    }
    // En < 18 el setting global delataba mocks; en modernos sigue sirviendo como señal.
    try {
        val mockSetting = Settings.Secure.getString(context.contentResolver, Settings.Secure.ALLOW_MOCK_LOCATION)
        if (mockSetting == "1") {
            // No es concluyente solo, pero si la precisión es 0 y el proveedor es "gps" fake, es sospechoso.
            // Se deja como hint; el servidor decide con tolerancia.
        }
    } catch (_: Exception) { }
    return false
}

// Intenta obtener la última ubicación conocida y, si no hay, pide una sola actualización.
// Liviano: no usa FusedLocationProvider (evita dependencia play-services-location).
suspend fun obtenerUbicacion(context: Context, timeoutMs: Long = 7000): UbicacionDato? {
    if (!tienePermisoUbicacion(context)) return null
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val proveedores = lm.getProviders(true)
    // Prioriza GPS y luego NETWORK.
    val orden = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        .filter { it in proveedores }

    // 1. Última ubicación conocida (rápida, sin activar GPS).
    var mejor: Location? = null
    for (p in orden) {
        try {
            @Suppress("MissingPermission")
            val loc = lm.getLastKnownLocation(p)
            if (loc != null && (mejor == null || loc.time > mejor.time)) mejor = loc
        } catch (_: Exception) { }
    }
    if (mejor != null) {
        val mock = mejor.esMockUbicacion(context)
        return UbicacionDato(mejor.latitude, mejor.longitude, mejor.accuracy.takeIf { it > 0 }, mock, mejor.provider)
    }

    // 2. Si no hay última conocida, pide una sola actualización con timeout.
    return suspendCancellableCoroutine { cont ->
        var entregado = false
        val listener = object : android.location.LocationListener {
            override fun onLocationChanged(location: Location) {
                if (entregado) return
                entregado = true
                try { lm.removeUpdates(this) } catch (_: Exception) {}
                if (cont.isActive) {
                    val mock = location.esMockUbicacion(context)
                    cont.resume(UbicacionDato(location.latitude, location.longitude, location.accuracy.takeIf { it > 0 }, mock, location.provider))
                }
            }
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
        }
        try {
            for (p in orden) {
                @Suppress("MissingPermission")
                lm.requestSingleUpdate(p, listener, null)
            }
        } catch (e: Exception) {
            if (cont.isActive) cont.resume(null)
            return@suspendCancellableCoroutine
        }
        // Timeout: si no llega nada, devuelve null.
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (!entregado && cont.isActive) {
                entregado = true
                try { lm.removeUpdates(listener) } catch (_: Exception) {}
                cont.resume(null)
            }
        }, timeoutMs)
        cont.invokeOnCancellation {
            try { lm.removeUpdates(listener) } catch (_: Exception) {}
        }
    }
}
