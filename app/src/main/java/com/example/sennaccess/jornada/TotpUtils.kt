package com.example.sennaccess.jornada

// Utilidades TOTP RFC 6238 livianas (sin dependencias): HMAC-SHA1 + base32.
// Se usan solo para el modo demo/mock del QR de aula cuando el backend
// aún no entrega el code. En producción el instructor muestra el code
// que devuelve GET /jornada/qr/{ambiente} calculado server-side.

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

// Alfabeto base32 (RFC 4648).
private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

// Decodifica base32 a bytes. Tolera "=" de padding y minúsculas.
fun decodificarBase32(base32: String): ByteArray? {
    return try {
        val limpio = base32.trim().replace("=", "").replace(" ", "").uppercase()
        if (limpio.isEmpty()) return null
        var buffer = 0
        var bitsLeft = 0
        val out = mutableListOf<Byte>()
        for (c in limpio) {
            val v = BASE32_ALPHABET.indexOf(c)
            if (v < 0) return null
            buffer = (buffer shl 5) or v
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                out.add(((buffer shr bitsLeft) and 0xFF).toByte())
            }
        }
        out.toByteArray()
    } catch (_: Exception) { null }
}

// Genera un código TOTP de `digits` dígitos con periodo `periodSec` (default 30s).
// El secreto debe venir en base32. Si falla, devuelve null.
fun generarTotp(secretBase32: String, tiempoMs: Long = System.currentTimeMillis(), periodSec: Int = 30, digits: Int = 6): String? {
    return try {
        val key = decodificarBase32(secretBase32) ?: return null
        val counter = (tiempoMs / 1000) / periodSec
        val data = ByteArray(8)
        var c = counter
        for (i in 7 downTo 0) {
            data[i] = (c and 0xFF).toByte()
            c = c shr 8
        }
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        val hash = mac.doFinal(data)
        val offset = (hash.last().toInt() and 0x0F)
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)
        val otp = binary % 10.0.pow(digits).toInt()
        otp.toString().padStart(digits, '0')
    } catch (_: Exception) { null }
}

// Segundos restantes de la ventana actual (para el countdown del QR).
fun segundosRestantesVentana(periodSec: Int = 30, tiempoMs: Long = System.currentTimeMillis()): Int {
    val seg = (tiempoMs / 1000) % periodSec
    return (periodSec - seg).toInt().coerceIn(0, periodSec)
}

// Construye el contenido del QR proyectado en el aula (formato convenido).
// El backend puede validar `code` + `ambienteId` + ventana sin exponer el secreto.
fun construirContenidoQr(code: String, ambienteId: Int, periodoS: Int = 30): String {
    // Formato: SENA-JORNADA:<ambienteId>:<code>:<periodo>
    return "SENA-JORNADA:$ambienteId:$code:$periodoS"
}
