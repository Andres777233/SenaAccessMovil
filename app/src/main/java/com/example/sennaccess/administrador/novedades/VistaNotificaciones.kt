package com.example.sennaccess.administrador.novedades

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
import com.example.sennaccess.datos.modelos.Notificacion
import com.example.sennaccess.comun.fechaRelativa
import com.example.sennaccess.comun.diseno.RadioVidrio
import com.example.sennaccess.comun.diseno.RadioSena
import com.example.sennaccess.comun.diseno.GrupoSeccion
import com.example.sennaccess.comun.diseno.CabeceraPlegable
import com.example.sennaccess.comun.diseno.superficieVidrio
import com.example.sennaccess.comun.diseno.escalaPresion
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.NaranjaAmbar
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import com.example.sennaccess.comun.EstadoCarga
import com.example.sennaccess.comun.EstadoContenido
import com.example.sennaccess.comun.VistaVacia
@Composable
fun VistaNotificaciones(
    estado: EstadoCarga<List<Notificacion>>,
    onReintentar: () -> Unit,
    onMarcarLeida: (Int) -> Unit,
    onMarcarTodasLeidas: () -> Unit,
    onBack: () -> Unit
) {
    val colors = ColoresAppLocal.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = colors.textPrimary) }
        }

        CabeceraPlegable(
            title = "Notificaciones",
            subtitle = "Avisos del centro de formación",
            scrollOffset = scrollState.value.toFloat()
        )

        Spacer(modifier = Modifier.height(16.dp))

        EstadoContenido(estado = estado, onReintentar = onReintentar) { notificaciones ->
            val hayNoLeidas = notificaciones.any { it.is_read != true }

            if (hayNoLeidas) {
                Button(
                    onClick = onMarcarTodasLeidas,
                    modifier = Modifier.fillMaxWidth().height(52.dp).escalaPresion(pressedScale = 0.97f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MARCAR TODAS COMO LEÍDAS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

        if (notificaciones.isEmpty()) {
                VistaVacia(
                    icono = Icons.Default.Notifications,
                    titulo = "No tienes notificaciones",
                    mensaje = "Cuando recibas avisos del centro de formación, aparecerán aquí."
                )
            } else {
                // Lista única en plana con divisor: leídas atenuadas, nuevas con punto.
                val noLeidas = notificaciones.count { it.is_read != true }
                com.example.sennaccess.comun.diseno.GrupoSeccion(
                    titulo = "Recientes",
                    conteo = noLeidas.takeIf { it > 0 }
                ) {
                    notificaciones.forEachIndexed { i, notificacion ->
                        if (i > 0) HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = 12.dp))
                        TarjetaNotificacion(
                            notificacion = notificacion,
                            onClick = { if (notificacion.is_read != true) notificacion.id_notificacion?.let(onMarcarLeida) }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun TarjetaNotificacion(notificacion: Notificacion, onClick: () -> Unit) {
    val colors = ColoresAppLocal.current
    val leida = notificacion.is_read == true
    val tipo = notificacion.notification_type
    val icono = when (tipo) {
        "novedad" -> Icons.Default.WarningAmber
        "equipo" -> Icons.Default.Devices
        else -> Icons.Default.Info
    }
    // Fila plana con riel de estado: punto verde si es nueva. La superficie
    // la pone el GrupoSeccion padre; las nuevas llevan borde lateral verde.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadioSena.md))
            .escalaPresion(pressedScale = 0.98f)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(if (leida) colors.divider else verdeMarca())
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(VerdeSena.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = verdeMarca(), modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
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
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(verdeMarca()))
                }
            }
            Text(fechaRelativa(notificacion.created_at), color = colors.textSecondary, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(notificacion.notification_body ?: "—", color = colors.textSecondary, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}
