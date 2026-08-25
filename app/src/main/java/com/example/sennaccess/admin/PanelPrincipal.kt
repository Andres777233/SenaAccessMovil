package com.example.sennaccess.admin

// Contenido de la pestaña INICIO del ADMINISTRADOR (panel de resumen).
// Muestra el historial de ingresos del día con su horario; la navegación al
// resto de pestañas se hace a través del dock de AdminDashboard.

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.data.Ingreso
import com.example.sennaccess.ui.CargaUiState
import com.example.sennaccess.ui.EstadoContenido
import com.example.sennaccess.ui.ios.GlassCornerRadius
import com.example.sennaccess.ui.ios.IosCollapsibleHeader
import com.example.sennaccess.ui.ios.glassSurface
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen

// Catálogo de pantallas del módulo admin; usado por la navegación interna.
enum class AdminScreen { PANEL, USUARIOS, CREAR_USUARIO, ACTUALIZAR_USUARIO,
    ACCESO_APRENDICES, ACCESO_INSTRUCTORES, REPORTE_NOVEDADES, PERFIL,
    EQUIPOS, NOTIFICACIONES, ASIGNACIONES }

/**
 * Panel de inicio del ADMINISTRADOR (contenido de la pestaña INICIO).
 * Resumen del día: historial de ingresos del centro con sus horarios.
 */
@Composable
fun AdminPanelResumen(resumen: CargaUiState<List<Ingreso>>, onReintentar: () -> Unit) {
    val colors = LocalAppColors.current
    // Estado de desplazamiento compartido con el encabezado colapsable.
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Encabezado que se colapsa al hacer scroll para aprovechar el espacio vertical.
        IosCollapsibleHeader(
            title = "Panel Administrador",
            subtitle = "Resumen general del centro de formación",
            scrollOffset = scrollState.value.toFloat()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Título de sección que introduce el listado de ingresos del día.
        Text(
            "HISTORIAL DEL DÍA",
            color = SenaGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        // EstadoContenido resuelve carga/error/éxito; el bloque recibe los datos listos.
        EstadoContenido(estado = resumen, onReintentar = onReintentar) { ingresos ->
            if (ingresos.isEmpty()) {
                // Estado vacío: aún no hay ingresos registrados hoy.
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Text("Aún no hay ingresos registrados hoy.", color = colors.textSecondary, fontSize = 14.sp)
                }
            } else {
                // Lista los ingresos del día como tarjetas individuales.
                ingresos.forEach { ingreso ->
                    AccesoResumenCard(
                        nombre = ingreso.user?.nombreCompleto ?: "Usuario",
                        tipo = ingreso.ingreso_type ?: "Acceso",
                        hora = ingreso.ingreso_datetime ?: "—"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// Tarjeta individual de un ingreso: nombre, tipo de acceso y hora.
@Composable
private fun AccesoResumenCard(nombre: String, tipo: String, hora: String) {
    val colors = LocalAppColors.current
    // Fila con fondo de vidrio que presenta los tres datos del ingreso.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = GlassCornerRadius)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circular con ícono de reloj que identifica visualmente un acceso.
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SenaGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Schedule, null, tint = SenaGreen, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        // Columna principal: nombre y tipo de acceso del usuario.
        Column(modifier = Modifier.weight(1f)) {
            Text(nombre, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(tipo, color = colors.textSecondary, fontSize = 12.sp)
        }
        // Hora del acceso resaltada a la derecha.
        Text(hora, color = SenaGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
