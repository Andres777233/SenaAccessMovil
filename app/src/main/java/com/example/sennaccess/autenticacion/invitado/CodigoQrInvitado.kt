// Pantalla del QR de invitado: SOLO el código (sin panel ni historial).
// Primero muestra el QR de entrada; cuando portería lo escanea, consulta el
// estado de la visita y cambia sola al QR de salida. La visita dura 6 horas.
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.datos.modelos.EstadoVisitaInvitado
import com.example.sennaccess.datos.modelos.RespuestaQrInvitado
import com.example.sennaccess.datos.red.ClienteApi
import com.example.sennaccess.comun.diseno.CajaError
import com.example.sennaccess.comun.diseno.BotonBordeBrillante
import com.example.sennaccess.comun.diseno.TarjetaVidrio
import com.example.sennaccess.comun.milisDeIso
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.verdeMarca
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun CodigoQrInvitado(
    invitado: RespuestaQrInvitado,
    onVolver: () -> Unit
) {
    val colors = ColoresAppLocal.current
    val tokenEntrada = invitado.qr_token.orEmpty()

    // Estado real de la visita, consultado cada 5 s al servidor.
    var visita by remember(tokenEntrada) { mutableStateOf<EstadoVisitaInvitado?>(null) }
    LaunchedEffect(tokenEntrada) {
        while (isActive) {
            try {
                val estado = ClienteApi.conServicio { it.estadoVisitaInvitado(tokenEntrada) }
                visita = estado
            } catch (_: Exception) {
                // Sin conexión se mantiene el último estado conocido.
            }
            delay(5000)
        }
    }

    val estadoVisita = visita?.estado ?: "pendiente"
    val enSalida = estadoVisita == "dentro"
    val terminada = estadoVisita == "salida" || estadoVisita == "expirado"
    val tokenVisible = if (enSalida) (visita?.qr_salida ?: "") else tokenEntrada
    val venceEn = if (enSalida) {
        visita?.qr_salida_expires_at ?: invitado.qr_expires_at
    } else {
        invitado.qr_expires_at
    }

    var restanteMs by remember(venceEn) { mutableStateOf(0L) }
    val expiraMillis = remember(venceEn) { milisDeIso(venceEn) }
    LaunchedEffect(expiraMillis) {
        val target = expiraMillis ?: return@LaunchedEffect
        while (isActive) {
            restanteMs = target - System.currentTimeMillis()
            if (restanteMs <= 0) break
            delay(1000)
        }
    }

    val qrBitmap: Bitmap? = remember(tokenVisible) {
        if (tokenVisible.isBlank()) null else generarImagenQr(tokenVisible, 640)
    }
    val expirado = expiraMillis != null && restanteMs <= 0
    val segundosRestantes = (restanteMs / 1000).coerceAtLeast(0)
    val horas = segundosRestantes / 3600
    val minutos = (segundosRestantes % 3600) / 60
    val segundos = segundosRestantes % 60
    val countdown = when {
        expirado -> "00:00"
        horas > 0 -> "${horas}h ${minutos.toString().padStart(2, '0')}m"
        else -> "${minutos.toString().padStart(2, '0')}:${segundos.toString().padStart(2, '0')}"
    }

    val context = LocalContext.current
    val nombre = (visita?.nombre ?: listOf(invitado.user?.user_name, invitado.user?.user_lastname)
        .filter { !it.isNullOrBlank() }.joinToString(" "))
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
                        Text(
                            when {
                                terminada && estadoVisita == "salida" -> "Visita finalizada"
                                terminada -> "Visita vencida"
                                enSalida -> "Tu QR de salida"
                                else -> "Tu QR de entrada"
                            },
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            when {
                                terminada -> "Gracias por visitarnos"
                                enSalida -> "Muéstralo en portería al salir"
                                else -> "Preséntalo en recepción al entrar"
                            },
                            color = colors.textSecondary,
                            fontSize = 13.sp
                        )
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

                if (terminada) {
                    Text(
                        if (estadoVisita == "salida") "✅" else "⌛",
                        fontSize = 44.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (estadoVisita == "salida") {
                            "Tu salida quedó registrada en portería."
                        } else {
                            "La visita superó el plazo de ${invitado.horas_vigencia ?: 6} horas y se cerró automáticamente."
                        },
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
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
                            contentDescription = "Código QR de la visita",
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
                        if (enSalida) {
                            "Código de salida · un solo uso"
                        } else {
                            "Código de entrada · un solo uso · visita de ${invitado.horas_vigencia ?: 6} horas"
                        },
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
                            if (enSalida) {
                                "Al salir del centro, muestra este código en portería."
                            } else {
                                "Al llegar, muestra este código al escáner de recepción."
                            },
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
                                ClipData.newPlainText("qr_invitado", tokenVisible)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (terminada) {
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
