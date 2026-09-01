package com.example.sennaccess.ui

// Pantalla "ACERCA DE / VERSIÓN" compartida por los tres roles.
// Muestra la versión de la app, un resumen de su propósito, las novedades
// de la versión actual y los créditos. Se abre desde el menú de la top bar.

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.BuildConfig
import com.example.sennaccess.ui.ios.GlassCornerRadius
import com.example.sennaccess.ui.ios.IosCollapsibleHeader
import com.example.sennaccess.ui.ios.glassSurface
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen

// Pantalla de información de la app: versión, novedades y créditos.
@Composable
fun AcercaDeView(onBack: () -> Unit) {
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
            title = "Acerca de",
            subtitle = "Información de la aplicación",
            scrollOffset = scrollState.value.toFloat()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tarjeta principal: logo, nombre, versión y descripción.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassSurface(cornerRadius = GlassCornerRadius)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(SenaGreen.copy(alpha = 0.15f))
                    .border(2.dp, SenaGreen.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.School, contentDescription = null, tint = SenaGreen, modifier = Modifier.size(44.dp))
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text("SENA ", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 26.sp)
            Text("ACCESS", color = SenaGreen, fontWeight = FontWeight.Bold, fontSize = 26.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Versión ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", color = colors.textSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Aplicación móvil del SENA para el control de ingresos de aprendices, " +
                    "instructores y equipos al centro de formación, con avisos y notificaciones en tiempo real.",
                color = colors.textSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Detalles técnicos de la aplicación.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassSurface(cornerRadius = GlassCornerRadius)
                .padding(18.dp)
        ) {
            Text("DETALLES", color = SenaGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(6.dp))
            FilaDato(Icons.Default.Info, "Versión", BuildConfig.VERSION_NAME)
            FilaDato(Icons.Default.PhoneAndroid, "Plataforma", "Android (Kotlin + Compose)")
            FilaDato(Icons.Default.Cloud, "Servidor", "Railway (producción)")
            FilaDato(Icons.Default.Badge, "Institución", "SENA - Centro de Servicios y Comercio")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Novedades de la versión actual.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassSurface(cornerRadius = GlassCornerRadius)
                .padding(18.dp)
        ) {
            Text("QUÉ HAY DE NUEVO", color = SenaGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(10.dp))
            NovedadItem("1.1", "Fechas legibles, actualización con deslizar (pull-to-refresh), estados vacíos amables y pantalla Acerca de.")
            NovedadItem("1.0", "Lanzamiento en la nube: login con huella, historial de ingresos, equipos, novedades y notificaciones conectados a Railway.")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Hecho con el apoyo del SENA - Centro de Servicios y Comercio",
            color = colors.textSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

// Fila de novedad del changelog: versión en chip verde y descripción.
@Composable
private fun NovedadItem(version: String, detalle: String) {
    val colors = LocalAppColors.current
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Box(
            modifier = Modifier
                .border(1.dp, SenaGreen.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .background(SenaGreen.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text("v$version", color = SenaGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(detalle, color = colors.textSecondary, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
    }
}
