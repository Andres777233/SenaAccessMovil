// Pantalla del QR de invitado: muestra el Bitmap generado por QRUtils con el
// token devuelto por POST /register-guest y la cuenta regresiva de 60 min.
package com.example.sennaccess.autenticacion.invitado

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.datos.modelos.RespuestaQrInvitado
import com.example.sennaccess.comun.diseno.CajaError
import com.example.sennaccess.comun.diseno.BotonBordeBrillante
import com.example.sennaccess.comun.diseno.TarjetaVidrio
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.verdeMarca
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Calendar
import java.util.TimeZone

private val patronQrIso = Regex(
    """(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2}):(\d{2})(\.\d+)?(Z|[+-]\d{2}:?\d{2})?"""
)

private fun epochMillisIso(iso: String?): Long? {
    if (iso.isNullOrBlank()) return null
    val m = patronQrIso.find(iso) ?: return null
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
    return cal.timeInMillis
}

@Composable
fun CodigoQrInvitado(
    invitado: RespuestaQrInvitado,
    onVolver: () -> Unit
) {
    val colors = ColoresAppLocal.current

    var restanteMs by remember(invitado.qr_expires_at) { mutableStateOf(0L) }
    val expiraMillis = remember(invitado.qr_expires_at) { epochMillisIso(invitado.qr_expires_at) }
    LaunchedEffect(expiraMillis) {
        val target = expiraMillis ?: return@LaunchedEffect
        while (isActive) {
            restanteMs = target - System.currentTimeMillis()
            if (restanteMs <= 0) break
            delay(1000)
        }
    }

    val qrBitmap: Bitmap? = remember(invitado.qr_token) {
        generarImagenQr(invitado.qr_token.orEmpty(), 640)
    }
    val expirado = expiraMillis != null && restanteMs <= 0
    val segundosRestantes = (restanteMs / 1000).coerceAtLeast(0)
    val minutos = segundosRestantes / 60
    val segundos = segundosRestantes % 60
    val countdown = if (expirado) "00:00" else "${minutos.toString().padStart(2, '0')}:${segundos.toString().padStart(2, '0')}"

    val context = LocalContext.current
    val nombre = listOf(invitado.user?.user_name, invitado.user?.user_lastname)
        .filter { !it.isNullOrBlank() }.joinToString(" ")
        .ifBlank { "Invitado" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .imePadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        TarjetaVidrio(modifier = Modifier.fillMaxWidth(0.95f)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onVolver, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = colors.textPrimary)
                    }
                    Column(modifier = Modifier.padding(start = 4.dp)) {
                        Text("Tu código QR", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Preséntalo en recepción", color = colors.textSecondary, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    nombre,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    "${invitado.user?.user_documento_tipo ?: ""} ${invitado.user?.user_identification ?: ""}".trim(),
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (expirado) {
                    CajaError("Este QR caducó. Genera uno nuevo.")
                } else if (qrBitmap == null) {
                    CajaError("No se pudo generar el QR. Intenta de nuevo.")
                } else {
                    Box(
                        modifier = Modifier
                            .size(230.dp)
                            .background(colors.surface.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Código QR de invitado",
                            modifier = Modifier.size(198.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    val avisoColor = if (segundosRestantes < 300) androidx.compose.ui.graphics.Color(0xFFF59E0B) else verdeMarca()
                    Text(
                        "Válido por: $countdown",
                        color = avisoColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        "Un solo uso · caduca a los ${minutos + 1} minutos",
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = colors.divider)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Al llegar, muestra este código al escáner de recepción.",
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    BotonBordeBrillante(
                        text = "COPIAR CÓDIGO",
                        icon = Icons.Default.ContentCopy,
                        onClick = {
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText("qr_invitado", invitado.qr_token.orEmpty())
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (expirado) {
                    Spacer(modifier = Modifier.height(12.dp))
                    BotonBordeBrillante(
                        text = "GENERAR OTRO QR",
                        icon = Icons.Default.ArrowBack,
                        onClick = onVolver,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
