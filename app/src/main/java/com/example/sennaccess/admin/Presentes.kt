// Contenido de la pestaña PRESENTES del ADMINISTRADOR.
// Muestra quiénes están DENTRO ahora (GET /admin/presentes): cada persona con
// su rol, hora de entrada y tiempo de estancia. Los datos llegan del backend vía
// el AdminDashboardViewModel bajo el patrón CargaUiState.
package com.example.sennaccess.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.data.Presente
import com.example.sennaccess.ui.CargaUiState
import com.example.sennaccess.ui.EstadoContenido
import com.example.sennaccess.ui.EstadoVacio
import com.example.sennaccess.ui.fechaRelativa
import com.example.sennaccess.ui.horaCorta
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import com.example.sennaccess.ui.ios.GlassCornerRadius
import com.example.sennaccess.ui.ios.IosCollapsibleHeader
import com.example.sennaccess.ui.ios.glassSurface

// Pestaña PRESENTES: encabezado con el conteo actual y la lista de personas dentro.
@Composable
fun PresentesView(
    estado: CargaUiState<List<Presente>>,
    onReintentar: () -> Unit
) {
    val colors = LocalAppColors.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        IosCollapsibleHeader(
            title = "Personas en el Centro",
            subtitle = "Quiénes están dentro ahora",
            scrollOffset = scrollState.value.toFloat()
        )

        Spacer(modifier = Modifier.height(16.dp))

        EstadoContenido(estado = estado, onReintentar = onReintentar) { presentes ->
            // Conteo actual de personas dentro del centro.
            StatCard(
                label = "DENTRO AHORA",
                value = presentes.size.toString(),
                icon = Icons.Default.Groups,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (presentes.isEmpty()) {
                // Sin nadie dentro: estado vacío amable.
                EstadoVacio(
                    icono = Icons.Default.Groups,
                    titulo = "Nadie dentro",
                    mensaje = "Las personas que estén en el centro aparecerán aquí."
                )
            } else {
                // Lista de personas presentes como tarjetas individuales.
                presentes.forEach { persona ->
                    TarjetaPresente(persona)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

// Tarjeta de una persona presente: avatar, nombre, rol, hora de entrada y estancia.
@Composable
private fun TarjetaPresente(persona: Presente) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = GlassCornerRadius)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circular con la inicial de la persona.
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SenaGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = persona.nombreCompleto.firstOrNull()?.uppercase() ?: "?",
                color = SenaGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        // Nombre y rol a la izquierda; entrada y estancia a la derecha.
        Column(modifier = Modifier.weight(1f)) {
            Text(persona.nombreCompleto, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(persona.rol ?: "—", color = colors.textSecondary, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "Entrada ${horaCorta(persona.entrada_hora)}",
                color = SenaGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = fechaRelativa(persona.entrada_hora),
                color = colors.textSecondary,
                fontSize = 12.sp
            )
        }
    }
}

// Tarjeta de estadística: valor grande en verde con su etiqueta.
@Composable
private fun StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .glassSurface(cornerRadius = GlassCornerRadius)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(SenaGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = SenaGreen, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(value, color = SenaGreen, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                Text(label, color = colors.textSecondary, fontSize = 12.sp)
            }
        }
    }
}
