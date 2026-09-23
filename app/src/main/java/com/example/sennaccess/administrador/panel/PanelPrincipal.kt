package com.example.sennaccess.administrador.panel

// Contenido de la pestaña INICIO del ADMINISTRADOR (panel de resumen).
// Muestra el historial de ingresos del día con su horario; la navegación al
// resto de pestañas se hace a través del dock de PanelAdministrador.

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
import com.example.sennaccess.datos.modelos.Ingreso
import com.example.sennaccess.comun.EstadoCarga
import com.example.sennaccess.comun.EstadoContenido
import com.example.sennaccess.comun.VistaVacia
import com.example.sennaccess.comun.horaCorta
import com.example.sennaccess.comun.diseno.RadioVidrio
import com.example.sennaccess.comun.diseno.CabeceraPlegable
import com.example.sennaccess.comun.diseno.superficieVidrio
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
enum class PantallaAdmin { PANEL, USUARIOS, CREAR_USUARIO, ACTUALIZAR_USUARIO,
    ACCESO_APRENDICES, ACCESO_INSTRUCTORES, REPORTE_NOVEDADES, PERFIL,
    EQUIPOS, NOTIFICACIONES, AMBIENTES, VALIDAR_EXCUSA, VERIFICACION_2FA,
    ESCANEAR_QR }

@Composable
fun ResumenPanelAdmin(resumen: EstadoCarga<List<Ingreso>>, onReintentar: () -> Unit) {
    val colors = ColoresAppLocal.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        CabeceraPlegable(
            title = "Panel Administrador",
            subtitle = "Resumen general del centro de formación",
            scrollOffset = scrollState.value.toFloat()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "HISTORIAL DEL DÍA",
            color = verdeMarca(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        EstadoContenido(estado = resumen, onReintentar = onReintentar) { ingresos ->
            if (ingresos.isEmpty()) {
                VistaVacia(
                    icono = Icons.Default.Schedule,
                    titulo = "Aún no hay ingresos registrados hoy",
                    mensaje = "Las entradas y salidas del centro aparecerán aquí."
                )
            } else {
                ingresos.forEach { ingreso ->
                    AccesoResumenCard(
                        nombre = ingreso.user?.nombreCompleto ?: "Usuario",
                        tipo = ingreso.ingreso_type ?: "Acceso",
                        hora = horaCorta(ingreso.ingreso_datetime)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun AccesoResumenCard(nombre: String, tipo: String, hora: String) {
    val colors = ColoresAppLocal.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .superficieVidrio(cornerRadius = RadioVidrio)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(VerdeSena.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Schedule, null, tint = verdeMarca(), modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(nombre, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(tipo, color = colors.textSecondary, fontSize = 12.sp)
        }
        Text(hora, color = verdeMarca(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
