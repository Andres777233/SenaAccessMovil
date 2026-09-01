// Utilidad para generar Bitmap de QR con ZXing (usada por el QR del aula).
package com.example.sennaccess.ui

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

// Codifica el contenido del QR en un Bitmap cuadrado (negro sobre blanco).
// Devuelve null si el texto no se puede codificar.
fun generarQrBitmap(contenido: String, lado: Int = 512): Bitmap? {
    return try {
        val matriz = QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE, lado, lado)
        val pixeles = IntArray(lado * lado)
        for (y in 0 until lado) {
            for (x in 0 until lado) {
                pixeles[y * lado + x] = if (matriz[x, y]) AndroidColor.BLACK else AndroidColor.WHITE
            }
        }
        Bitmap.createBitmap(lado, lado, Bitmap.Config.RGB_565).apply {
            setPixels(pixeles, 0, lado, 0, 0, lado, lado)
        }
    } catch (_: Exception) {
        null
    }
}
