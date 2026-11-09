package com.example.sennaccess.portero

// Pantalla del admin/portería para proyectar el QR rotativo de ENTRADA o
// SALIDA. El QR se renueva cada 30 s y también permite generar un PIN de 6
// dígitos (5 minutos, un solo uso) para quienes no pueden escanear.

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.autenticacion.invitado.generarImagenQr
import com.example.sennaccess.comun.detalleHttp
import com.example.sennaccess.comun.diseno.BotonBordeBrillante
import com.example.sennaccess.comun.diseno.BotonPrimarioNeon
import com.example.sennaccess.comun.diseno.EspaciadoSena
import com.example.sennaccess.comun.diseno.RadioSena
import com.example.sennaccess.comun.diseno.superficiePlana
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.RojoError
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import com.example.sennaccess.datos.repositorios.RepositorioAcceso
import com.example.sennaccess.datos.sesion.GestorSesion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VistaQrPorteria() {
    val colors = ColoresAppLocal.current
    val scope = rememberCoroutineScope()
    val repo = remember { RepositorioAcceso() }

    var tipo by remember { mutableStateOf("entrada") }
    var payload by remember { mutableStateOf<String?>(null) }
    var segundos by remember { mutableIntStateOf(30) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var generandoPin by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf<String?>(null) }
    var errorPin by remember { mutableStateOf<String?>(null) }

    suspend fun cargar() {
        val token = GestorSesion.token ?: run {
            error = "Sesión vencida. Vuelve a iniciar sesión."
            cargando = false
            return
        }
        try {
            val resp = repo.qrPorteria(token, tipo)
            val nuevo = resp.qr_payload
            if (nuevo.isNullOrBlank()) {
                error = "El servidor no devolvió un QR válido."
            } else {
                payload = nuevo
                error = null
                val restante = resp.expira_en?.let {
                    (it - System.currentTimeMillis() / 1000L).toInt()
                }
                segundos = when {
                    restante == null -> resp.ventana_seg ?: 30
                    restante in 1..120 -> restante
                    else -> resp.ventana_seg ?: 30
                }
            }
        } catch (e: retrofit2.HttpException) {
            error = detalleHttp(e)
        } catch (e: Exception) {
            error = "No se pudo generar el QR: ${e.message ?: e.javaClass.simpleName}"
        } finally {
            cargando = false
        }
    }

    // Refresca el QR cada 25 s (antes de que venza la ventana de 30 s).
    LaunchedEffect(tipo) {
        payload = null
        error = null
        cargando = true
        pin = null
        errorPin = null
        while (true) {
            cargar()
            delay(25_000)
        }
    }
    // Cuenta atrás local.
    LaunchedEffect(tipo) {
        while (true) {
            delay(1_000)
            if (segundos > 0) segundos -= 1
        }
    }

    fun generarPin() {
        if (generandoPin) return
        val token = GestorSesion.token ?: return
        generandoPin = true
        errorPin = null
        scope.launch {
            try {
                val resp = repo.generarPinPorteria(token, tipo)
                pin = resp.pin
                if (pin.isNullOrBlank()) errorPin = "El servidor no devolvió un PIN."
            } catch (e: retrofit2.HttpException) {
                errorPin = detalleHttp(e)
            } catch (e: Exception) {
                errorPin = "No se pudo generar el PIN: ${e.message ?: e.javaClass.simpleName}"
            } finally {
                generandoPin = false
            }
        }
    }

    val qr = remember(payload) { payload?.let { generarImagenQr(it, 720) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        Text(
            "QR DE PORTERÍA",
            color = verdeMarca(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.8.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Proyecta el QR",
            color = colors.textPrimary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp
        )
        Text(
            "ENTRADA o SALIDA: el aprendiz o instructor lo escanea desde su app y queda en el historial.",
            color = colors.textSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(EspaciadoSena.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("entrada" to "ENTRADA", "salida" to "SALIDA").forEach { (clave, etiqueta) ->
                val activo = tipo == clave
                Button(
                    onClick = { tipo = clave },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(RadioSena.md),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activo) VerdeSena else colors.surfaceVariant,
                        contentColor = if (activo) Color.Black else colors.textPrimary
                    )
                ) {
                    Text(etiqueta, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(EspaciadoSena.md))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .superficiePlana(cornerRadius = RadioSena.lg)
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                if (qr != null) {
                    Image(
                        bitmap = qr.asImageBitmap(),
                        contentDescription = "QR de $tipo de portería",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    CircularProgressIndicator(color = verdeMarca(), modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = (if (tipo == "entrada") "ENTRADA" else "SALIDA") +
                    " · se renueva en ${segundos.coerceAtLeast(0)} s",
                color = if (tipo == "entrada") verdeMarca() else colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Mantenlo visible para todos. No necesitas apagar la pantalla entre escaneos.",
                color = colors.textSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )

            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(error!!, color = RojoError, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }

        Spacer(modifier = Modifier.height(EspaciadoSena.md))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .superficiePlana(cornerRadius = RadioSena.lg)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Keyboard, null, tint = verdeMarca(), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "¿Sin cámara? Dicta un PIN",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "PIN de 6 dígitos, válido 5 minutos y de un solo uso para ${if (tipo == "entrada") "entrada" else "salida"}.",
                color = colors.textSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            BotonBordeBrillante(
                text = if (generandoPin) "GENERANDO..." else "GENERAR PIN",
                icon = Icons.Default.QrCode2,
                onClick = { generarPin() },
                modifier = Modifier.fillMaxWidth()
            )
            if (errorPin != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(errorPin!!, color = RojoError, fontSize = 12.sp)
            }
            pin?.let { codigo ->
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VerdeSena.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        codigo,
                        color = verdeMarca(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 34.sp,
                        letterSpacing = 8.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(EspaciadoSena.lg))
    }
}
