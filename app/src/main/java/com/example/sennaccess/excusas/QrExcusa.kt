package com.example.sennaccess.excusas

// QR de la excusa (opción A móvil): codifica SENA-EXCUSA:<pin>:<id> con el
// generador ZXing ya usado por invitados. Portería lo escanea y valida el PIN
// con POST /excusas/validar. Fondo blanco puro para lectura fiable en claro y oscuro.

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.autenticacion.invitado.generarImagenQr
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.verdeMarca
import com.example.sennaccess.datos.modelos.Excusa

@Composable
fun CodigoQrExcusa(excusa: Excusa, lado: Dp = 190.dp, mostrarPin: Boolean = true) {
    val colors = ColoresAppLocal.current
    val contenido = remember(excusa.id_excusa, excusa.pin) { contenidoQrExcusa(excusa) }
    val bitmap: Bitmap? = remember(contenido) {
        contenido?.let { generarImagenQr(it, 512) }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (bitmap != null) {
            // Blanco puro + borde sutil: escanea igual en claro y oscuro.
            Box(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .border(1.dp, colors.divider, RoundedCornerShape(20.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "QR de la excusa ${excusa.pin ?: ""}",
                    modifier = Modifier.size(lado)
                )
            }
        } else {
            Text(
                "No se pudo generar el QR. Usa el PIN manual.",
                color = colors.textSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
        if (mostrarPin) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "o PIN ${excusa.pin ?: "—"}",
                color = verdeMarca(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
