package com.example.sennaccess.excusas

// Validación unificada de portería: PIN manual, QR de excusa, QR de invitado y
// código de entrada de la web en una sola sesión.
//
// Diseño v2: una sola fila de pestañas (compacta, 44dp) en lugar de cuatro
// tarjetas grandes, para que el escáner —lo importante— quede arriba y visible
// sin desplazar. La pestaña activa se marca con tinte de acento, no con un
// bloque verde lleno.

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.autenticacion.invitado.EscanearQrInvitado
import com.example.sennaccess.comun.diseno.ContenidoPestana
import com.example.sennaccess.comun.diseno.EntradaSuave
import com.example.sennaccess.comun.diseno.EspaciadoSena
import com.example.sennaccess.comun.diseno.superficiePlana
import com.example.sennaccess.comun.diseno.RadioSena
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.verdeMarca

private data class PestanaValidar(
    val icono: ImageVector,
    val titulo: String,
    val ayuda: String
)

private val PESTANAS = listOf(
    PestanaValidar(Icons.Default.Keyboard, "PIN", "PIN de 4 dígitos de la excusa: lo genera el instructor para autorizar la salida."),
    PestanaValidar(Icons.Default.QrCodeScanner, "Excusa", "Escanea el QR del permiso del aprendiz para registrar su salida."),
    PestanaValidar(Icons.Default.QrCodeScanner, "Invitado", "Escanea el QR de un invitado. Sirve una sola vez y caduca a la hora."),
    PestanaValidar(Icons.Default.QrCodeScanner, "Web", "Escanea el carnet o digita el PIN de 6 dígitos que el usuario generó en la web.")
)

@Composable
fun VistaValidarUnificada(onBack: () -> Unit) {
    val colors = ColoresAppLocal.current
    var tab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = colors.textPrimary)
            }
            Spacer(modifier = Modifier.width(2.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Validar accesos",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp
                )
                Text(
                    "Un solo lugar para permisos, invitados y códigos de la web.",
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(EspaciadoSena.sm))

        // Selector compacto: cuatro destinos en una fila.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .superficiePlana(cornerRadius = RadioSena.md)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PESTANAS.forEachIndexed { indice, pestana ->
                val seleccionada = tab == indice
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(RadioSena.sm))
                        .background(if (seleccionada) verdeMarca().copy(alpha = 0.14f) else Color.Transparent)
                        .clickable { tab = indice }
                        .padding(vertical = 10.dp, horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = pestana.icono,
                        contentDescription = null,
                        tint = if (seleccionada) verdeMarca() else colors.textSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = pestana.titulo,
                        color = if (seleccionada) colors.textPrimary else colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(EspaciadoSena.xs))
        Text(
            text = PESTANAS[tab].ayuda,
            color = colors.textSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(EspaciadoSena.sm))

        // El peso va en EntradaSuave, que sí es hijo directo del Column; el
        // contenido de la pestaña recibe el alto restante y puede hacer scroll.
        EntradaSuave(indice = tab, modifier = Modifier.weight(1f)) {
            ContenidoPestana(objetivo = tab) { pestana ->
                when (pestana) {
                    0 -> VistaValidarExcusa(onBack = onBack, mostrarCabecera = false)
                    1 -> EscanearQrExcusa(mostrarCabecera = false)
                    2 -> EscanearQrInvitado(onVolver = onBack, mostrarCabecera = false)
                    else -> EscanearQrEntrada(mostrarCabecera = false)
                }
            }
        }
    }
}
