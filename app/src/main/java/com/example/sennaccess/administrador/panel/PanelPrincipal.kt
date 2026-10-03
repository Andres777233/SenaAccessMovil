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
import com.example.sennaccess.comun.diseno.EntradaSuave
import com.example.sennaccess.comun.diseno.superficieVidrio
import com.example.sennaccess.comun.diseno.superficiePlana
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
        com.example.sennaccess.comun.diseno.CabeceraPantalla(
            eyebrow = "Inicio",
            titulo = "Panel Administrador",
            subtitulo = "Movimiento de hoy en el centro de formación"
        )

        EstadoContenido(estado = resumen, onReintentar = onReintentar) { ingresos ->
            if (ingresos.isEmpty()) {
                VistaVacia(
                    icono = Icons.Default.Schedule,
                    titulo = "Aún no hay ingresos registrados hoy",
                    mensaje = "Las entradas y salidas del centro aparecerán aquí."
                )
            } else {
                val entradas = ingresos.count { !(it.ingreso_type ?: "").equals("Salida", ignoreCase = true) }
                val salidas = ingresos.size - entradas
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    EntradaSuave(indice = 0, modifier = Modifier.weight(1.4f)) {
                        HeroTotalAdmin(total = "${ingresos.size}", modifier = Modifier.fillMaxWidth())
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        EntradaSuave(indice = 1) {
                            ContadorResumen("Entradas", "$entradas", Icons.Default.Login, Modifier.fillMaxWidth())
                        }
                        EntradaSuave(indice = 2) {
                            ContadorResumen("Salidas", "$salidas", Icons.Default.Logout, Modifier.fillMaxWidth())
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                com.example.sennaccess.comun.diseno.GrupoSeccion(
                    titulo = "Historial del día",
                    conteo = ingresos.size,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ingresos.forEachIndexed { idx, ingreso ->
                        if (idx > 0) HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = 14.dp))
                        AccesoResumenFila(
                            nombre = ingreso.user?.nombreCompleto ?: "Usuario",
                            tipo = ingreso.ingreso_type ?: "Acceso",
                            lugar = ingreso.ingreso_place,
                            hora = horaCorta(ingreso.ingreso_datetime)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun HeroTotalAdmin(total: String, modifier: Modifier = Modifier) {
    val colors = ColoresAppLocal.current
    Column(
        modifier = modifier
            .superficieVidrio(cornerRadius = RadioVidrio, elevated = true)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(VerdeSena.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SwapHoriz, null, tint = verdeMarca(), modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                "TOTAL HOY",
                color = verdeMarca(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(total, color = colors.textPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp)
        Text("movimientos", color = colors.textSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun ContadorResumen(titulo: String, valor: String, icono: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    val colors = ColoresAppLocal.current
    Row(
        modifier = modifier
            .superficiePlana(cornerRadius = RadioVidrio)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, null, tint = verdeMarca(), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(valor, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(titulo, color = colors.textSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun AccesoResumenFila(nombre: String, tipo: String, lugar: String?, hora: String) {
    val colors = ColoresAppLocal.current
    val esSalida = tipo.equals("Salida", ignoreCase = true)
    val tinta = if (esSalida) com.example.sennaccess.comun.tema.NaranjaAmbar else verdeMarca()
    val icono = if (esSalida) Icons.Default.Logout else Icons.Default.Login
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(CircleShape).background(tinta.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, null, tint = tinta, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(nombre, color = colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
            Text(
                listOf(tipo, lugar?.takeIf { it.isNotBlank() }).filterNotNull().joinToString(" • "),
                color = colors.textSecondary, fontSize = 12.sp, maxLines = 1
            )
        }
        Text(hora, color = tinta, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
