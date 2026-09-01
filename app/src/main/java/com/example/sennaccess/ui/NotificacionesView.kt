package com.example.sennaccess.ui

// Vista de NOTIFICACIONES in-app, compartida por los tres roles.
// Lista las notificaciones del usuario (GET /api/notifications), marca una como
// leída al tocarla (PUT /notifications/{id}/read) y permite marcarlas todas
// (PUT /notifications/read-all). El borde resalta las no leídas.

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.data.Notificacion
import com.example.sennaccess.ui.fechaRelativa
import com.example.sennaccess.ui.ios.GlassCornerRadius
import com.example.sennaccess.ui.ios.IosCollapsibleHeader
import com.example.sennaccess.ui.ios.glassSurface
import com.example.sennaccess.ui.ios.pressScale
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.OrangeAmber
import com.example.sennaccess.ui.theme.SenaGreen// Vista de notificaciones: encabezado, botón de leídas y lista de tarjetas.
@Composable
fun NotificacionesView(
    estado: CargaUiState<List<Notificacion>>,
    onReintentar: () -> Unit,
    onMarcarLeida: (Int) -> Unit,
    onMarcarTodasLeidas: () -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalAppColors.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = colors.textPrimary) }
        }

        IosCollapsibleHeader(
            title = "Notificaciones",
            subtitle = "Avisos del centro de formación",
            scrollOffset = scrollState.value.toFloat()
        )

        Spacer(modifier = Modifier.height(16.dp))

        EstadoContenido(estado = estado, onReintentar = onReintentar) { notificaciones ->
            val hayNoLeidas = notificaciones.any { it.is_read != true }

            // Botón para marcar todas como leídas (solo si hay pendientes).
            if (hayNoLeidas) {
                Button(
                    onClick = onMarcarTodasLeidas,
                    modifier = Modifier.fillMaxWidth().height(48.dp).pressScale(pressedScale = 0.97f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MARCAR TODAS COMO LEÍDAS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (notificaciones.isEmpty()) {
                EstadoVacio(
                    icono = Icons.Default.Notifications,
                    titulo = "No tienes notificaciones",
                    mensaje = "Cuando recibas avisos del centro de formación, aparecerán aquí."
                )
            } else {
                notificaciones.forEach { notificacion ->
                    TarjetaNotificacion(
                        notificacion = notificacion,
                        onClick = { if (notificacion.is_read != true) notificacion.id_notificacion?.let(onMarcarLeida) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

// Tarjeta de vidrio de una notificación; resalta las no leídas con borde verde.
@Composable
private fun TarjetaNotificacion(notificacion: Notificacion, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val leida = notificacion.is_read == true
    val tipo = notificacion.notification_type
    val icono = when (tipo) {
        "novedad" -> Icons.Default.WarningAmber
        "equipo" -> Icons.Default.Devices
        else -> Icons.Default.Info
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(pressedScale = 0.98f)
            .then(
                if (leida) Modifier.glassSurface(cornerRadius = GlassCornerRadius)
                else Modifier
                    .glassSurface(cornerRadius = GlassCornerRadius)
                    .border(1.dp, SenaGreen.copy(alpha = 0.5f), RoundedCornerShape(GlassCornerRadius))
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(SenaGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = SenaGreen, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    notificacion.notification_title ?: "Notificación",
                    color = colors.textPrimary,
                    fontWeight = if (leida) FontWeight.Medium else FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                if (!leida) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SenaGreen))
                }
            }
            Text(fechaRelativa(notificacion.created_at), color = colors.textSecondary, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(notificacion.notification_body ?: "—", color = colors.textSecondary, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}