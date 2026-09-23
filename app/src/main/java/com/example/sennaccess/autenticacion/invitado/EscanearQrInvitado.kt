// Escáner de QR de invitado en recepción: CameraX (Preview) + MLKit (detección
// de QR) validan POST /validate-guest-qr. Incluye pestaña de entrada manual del
// token para equipos sin cámara o con poca luz.
package com.example.sennaccess.autenticacion.invitado

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.sennaccess.datos.red.ClienteApi
import com.example.sennaccess.datos.modelos.PeticionValidarQr
import com.example.sennaccess.comun.detalleHttp
import com.example.sennaccess.comun.diseno.CampoAcceso
import com.example.sennaccess.comun.diseno.CajaError as CajaErrorIos
import com.example.sennaccess.comun.diseno.BotonBordeBrillante
import com.example.sennaccess.comun.diseno.BotonPrimarioNeon
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.verdeMarca
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch

@Composable
fun EscanearQrInvitado(
    onVolver: () -> Unit
) {
    val colors = ColoresAppLocal.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var pestana by remember { mutableStateOf(0) }
    var permisoCamara by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcherPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido -> permisoCamara = concedido }

    val detenerAnalisis = remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(false) }
    var tokenManual by remember { mutableStateOf("") }
    var resultadoMensaje by remember { mutableStateOf<String?>(null) }
    var resultadoExito by remember { mutableStateOf(false) }
    var infoVisible by remember { mutableStateOf(false) }

    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        )
    }

    fun validarToken(token: String) {
        val tk = token.trim()
        if (tk.isBlank() || cargando) return
        scope.launch {
            cargando = true
            try {
                val resp = ClienteApi.conServicio { servicio ->
                    servicio.validateGuestQr(PeticionValidarQr(tk))
                }
                cargando = false
                resultadoExito = true
                resultadoMensaje = resp.message ?: "Entrada registrada correctamente"
            } catch (e: retrofit2.HttpException) {
                cargando = false
                resultadoExito = false
                resultadoMensaje = detalleHttp(e)
            } catch (e: Exception) {
                cargando = false
                resultadoExito = false
                resultadoMensaje = "Error de conexión: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize().imePadding().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onVolver, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = colors.textPrimary)
                }
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text("Escanear QR de invitado", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Registra la Entrada del visitante", color = colors.textSecondary, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surfaceVariant.copy(alpha = 0.4f))
                    .padding(4.dp)
            ) {
                ModoEscaneoItem(
                    texto = "CAMARA",
                    icono = Icons.Default.CameraAlt,
                    activo = pestana == 0,
                    onClick = { pestana = 0 }
                )
                ModoEscaneoItem(
                    texto = "ESCRIBIR CODIGO",
                    icono = Icons.Default.Keyboard,
                    activo = pestana == 1,
                    onClick = { pestana = 1 }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (pestana) {
                0 -> {
                    if (permisoCamara) {
                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).apply {
                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                    cameraProviderFuture.addListener({
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().also { p ->
                                            p.setSurfaceProvider(this.surfaceProvider)
                                        }
                                        val analysis = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()
                                        analysis.setAnalyzer(
                                            ContextCompat.getMainExecutor(ctx)
                                        ) { imageProxy ->
                                            if (detenerAnalisis.value) {
                                                imageProxy.close()
                                                return@setAnalyzer
                                            }
                                            val media = imageProxy.image
                                            if (media == null) {
                                                imageProxy.close()
                                                return@setAnalyzer
                                            }
                                            val input = InputImage.fromMediaImage(
                                                media, imageProxy.imageInfo.rotationDegrees
                                            )
                                            scanner.process(input)
                                                .addOnSuccessListener { barcodes ->
                                                    val raw = barcodes.firstOrNull {
                                                        !it.rawValue.isNullOrBlank()
                                                    }?.rawValue
                                                    if (raw != null) {
                                                        detenerAnalisis.value = true
                                                        validarToken(raw)
                                                    }
                                                }
                                                .addOnCompleteListener { imageProxy.close() }
                                        }
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview,
                                            analysis
                                        )
                                    }, ContextCompat.getMainExecutor(ctx))
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(colors.surface)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Apunta al código QR del visitante",
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "La cámara está desactivada. Permite su uso para escanear el QR.",
                                color = colors.textSecondary,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            BotonPrimarioNeon(
                                text = "ACTIVAR CAMARA",
                                icon = Icons.Default.CameraAlt,
                                onClick = {
                                    launcherPermiso.launch(Manifest.permission.CAMERA)
                                }
                            )
                        }
                    }
                }

                else -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        CampoAcceso(
                            value = tokenManual,
                            onValueChange = { tokenManual = it },
                            label = "Token del QR",
                            keyboardType = KeyboardType.Password,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { validarToken(tokenManual) },
                            enabled = !cargando && tokenManual.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(28.dp)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = verdeMarca(),
                                contentColor = androidx.compose.ui.graphics.Color.Black,
                                disabledContainerColor = verdeMarca().copy(alpha = 0.35f),
                                disabledContentColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f)
                            ),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            if (cargando) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = androidx.compose.ui.graphics.Color.Black,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("VALIDAR CODIGO", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Pega aquí el token del QR que copió el visitante.",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (resultadoMensaje != null) {
                if (resultadoExito) {
                    Surface(
                        color = com.example.sennaccess.comun.tema.VerdeSena.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "$resultadoMensaje\nPuedes escanear el siguiente QR.",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    CajaErrorIos(resultadoMensaje!!)
                }
                Spacer(modifier = Modifier.height(10.dp))
                BotonBordeBrillante(
                    text = "ESCANEAR OTRO",
                    icon = Icons.Default.QrCode2,
                    onClick = {
                        resultadoMensaje = null
                        tokenManual = ""
                        detenerAnalisis.value = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun RowScope.ModoEscaneoItem(
    texto: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    activo: Boolean,
    onClick: () -> Unit
) {
    val colors = ColoresAppLocal.current
    Row(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(11.dp))
            .background(if (activo) colors.surface else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = if (activo) verdeMarca() else colors.textSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            texto,
            color = if (activo) colors.textPrimary else colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = if (activo) FontWeight.Bold else FontWeight.Normal
        )
    }
}
