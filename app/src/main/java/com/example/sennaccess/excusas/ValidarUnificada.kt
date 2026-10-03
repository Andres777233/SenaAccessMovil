package com.example.sennaccess.excusas

// Validación unificada de portería: PIN manual, QR de excusa y QR de invitado
// en una sola sesión con pestañas. El operario decide si escribe el PIN a mano,
// escanea el QR del permiso del aprendiz o escanea un invitado, sin salir de
// la pantalla.

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.autenticacion.invitado.EscanearQrInvitado
import com.example.sennaccess.comun.diseno.ContenidoPestana
import com.example.sennaccess.comun.diseno.EntradaSuave
import com.example.sennaccess.comun.diseno.superficiePlana
import com.example.sennaccess.comun.diseno.RadioSena
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca

@Composable
fun VistaValidarUnificada(onBack: () -> Unit) {
    val colors = ColoresAppLocal.current
    var tab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Cabecera profesional con volver + eyebrow + título.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = colors.textPrimary) }
            Column(modifier = Modifier.weight(1f)) {
                Text("CONTROL DE PORTERÍA", color = verdeMarca(), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
                Text("Validar accesos", color = colors.textPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            }
        }
        Text("Salidas por PIN o QR de excusa, o entradas de invitado por QR.", color = colors.textSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(14.dp))
        // Segmentado profesional en plana: tres opciones grandes de 52dp.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .superficiePlana(cornerRadius = RadioSena.lg)
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                SegmentoValidar(
                    seleccionado = tab == 0,
                    icono = Icons.Default.Keyboard,
                    titulo = "PIN A MANO",
                    subtitulo = "Excusas",
                    onClick = { tab = 0 },
                    modifier = Modifier.weight(1f)
                )
                SegmentoValidar(
                    seleccionado = tab == 1,
                    icono = Icons.Default.QrCodeScanner,
                    titulo = "QR EXCUSA",
                    subtitulo = "Permisos",
                    onClick = { tab = 1 },
                    modifier = Modifier.weight(1f)
                )
            }
            SegmentoValidar(
                seleccionado = tab == 2,
                icono = Icons.Default.QrCodeScanner,
                titulo = "QR INVITADO",
                subtitulo = "Visitantes",
                onClick = { tab = 2 },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        EntradaSuave(indice = tab) {
            ContenidoPestana(objetivo = tab) { pestana ->
                when (pestana) {
                    0 -> VistaValidarExcusa(onBack = onBack, mostrarCabecera = false)
                    1 -> EscanearQrExcusa(mostrarCabecera = false)
                    else -> EscanearQrInvitado(onVolver = onBack, mostrarCabecera = false)
                }
            }
        }
    }
}

@Composable
private fun SegmentoValidar(
    seleccionado: Boolean,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    subtitulo: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ColoresAppLocal.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (seleccionado) VerdeSena else Color.Transparent, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Icon(icono, null, tint = if (seleccionado) Color.Black else colors.textSecondary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(titulo, color = if (seleccionado) Color.Black else colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        Text(subtitulo, color = if (seleccionado) Color.Black.copy(alpha = 0.7f) else colors.textSecondary, fontSize = 11.sp)
    }
}
