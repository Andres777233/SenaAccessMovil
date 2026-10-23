package com.example.sennaccess.excusas

// Entrada asistida desde la web: porteria escanea el QR SENA-ACCESO:<token>
// (o dicta el PIN de 6 digitos) y el backend registra la Entrada del usuario
// que genero el codigo. Reutiliza CameraX + MLKit del escaner de excusas.

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Person
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
import com.example.sennaccess.comun.detalleHttp
import com.example.sennaccess.comun.diseno.BotonBordeBrillante
import com.example.sennaccess.comun.diseno.BotonPrimarioNeon
import com.example.sennaccess.comun.diseno.CajaError as CajaErrorSena
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import com.example.sennaccess.datos.modelos.UsuarioCodigoAcceso
import com.example.sennaccess.datos.repositorios.RepositorioAcceso
import com.example.sennaccess.datos.sesion.GestorSesion
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EscanearQrEntrada(mostrarCabecera: Boolean = true) {
    val colors = ColoresAppLocal.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val repo = remember { RepositorioAcceso() }

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
    var errorQr by remember { mutableStateOf<String?>(null) }
    var errorValidar by remember { mutableStateOf<String?>(null) }
    var exito by remember { mutableStateOf<UsuarioCodigoAcceso?>(null) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var pin by remember { mutableStateOf("") }

    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        )
    }

    fun validar(codigo: String? = null, pinTexto: String? = null) {
        val token = GestorSesion.token
        if (token == null || cargando) return

        scope.launch {
            cargando = true
            errorValidar = null
            try {
                val resp = repo.validarCodigo(token, codigo = codigo, pin = pinTexto)
                exito = resp.usuario
                mensaje = resp.message
                pin = ""
                detenerAnalisis.value = true
            } catch (e: retrofit2.HttpException) {
                errorValidar = detalleHttp(e)
                detenerAnalisis.value = false
            } catch (e: Exception) {
                errorValidar = "Error de conexión: ${e.message ?: e.javaClass.simpleName}"
                detenerAnalisis.value = false
            } finally {
                cargando = false
            }

            if (exito == null) {
                delay(1_200)
                detenerAnalisis.value = false
            }
        }
    }

    fun alEscanear(raw: String) {
        if (!raw.startsWith("SENA-ACCESO:")) {
            errorQr = "Ese QR no es un código de entrada. Pide al usuario que lo genere desde la web."
            detenerAnalisis.value = false
            return
        }
        errorQr = null
        detenerAnalisis.value = true
        validar(codigo = raw)
    }

    Column(modifier = Modifier.fillMaxWidth().imePadding()) {
        if (mostrarCabecera) {
            Text(
                "Escanea el QR de entrada generado en la web o dicta el PIN.",
                color = colors.textSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (permisoCamara) {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
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
                                    if (detenerAnalisis.value || cargando) {
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
                                                alEscanear(raw)
                                            }
                                        }
                                        .addOnCompleteListener { imageProxy.close() }
                                }
                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        analysis
                                    )
                                } catch (_: Exception) { }
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.surface)
                )
                MarcoEscanerEntrada(modifier = Modifier.fillMaxWidth().aspectRatio(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                if (cargando) "Validando código..." else "Apunta al QR que muestra la web del usuario",
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
                    onClick = { launcherPermiso.launch(Manifest.permission.CAMERA) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            "O digita el PIN de 6 dígitos",
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { texto ->
                if (texto.length <= 6 && texto.all { it.isDigit() }) {
                    pin = texto
                    errorValidar = null
                }
            },
            label = { Text("PIN de entrada") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        BotonPrimarioNeon(
            text = "VALIDAR PIN",
            icon = Icons.Default.Keyboard,
            onClick = { validar(pinTexto = pin) },
            enabled = pin.length == 6 && !cargando,
            loading = cargando,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))
        if (cargando) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator(color = verdeMarca(), modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Registrando entrada...", color = colors.textSecondary, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (errorQr != null) {
            CajaErrorSena(errorQr!!)
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (errorValidar != null) {
            CajaErrorSena(errorValidar!!)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (exito != null || mensaje != null) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(VerdeSena.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = verdeMarca(), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            mensaje ?: "Entrada registrada",
                            color = verdeMarca(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, null, tint = verdeMarca(), modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                exito?.nombre ?: "Usuario",
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                "Rol: ${exito?.rol ?: "—"}",
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                "Documento: ${exito?.identificacion ?: "—"}",
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Verifica la identidad antes de dar la entrada.",
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            BotonBordeBrillante(
                text = "ESCANEAR OTRO",
                icon = Icons.Default.QrCode2,
                onClick = {
                    exito = null
                    mensaje = null
                    errorQr = null
                    errorValidar = null
                    pin = ""
                    detenerAnalisis.value = false
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MarcoEscanerEntrada(modifier: Modifier = Modifier) {
    val verde = verdeMarca()
    Canvas(modifier = modifier.padding(28.dp)) {
        val largo = 64.dp.toPx()
        val grosor = 6.dp.toPx()
        val w = size.width
        val h = size.height
        drawLine(verde, androidx.compose.ui.geometry.Offset(0f, grosor / 2), androidx.compose.ui.geometry.Offset(largo, grosor / 2), grosor)
        drawLine(verde, androidx.compose.ui.geometry.Offset(grosor / 2, 0f), androidx.compose.ui.geometry.Offset(grosor / 2, largo), grosor)
        drawLine(verde, androidx.compose.ui.geometry.Offset(w - largo, grosor / 2), androidx.compose.ui.geometry.Offset(w, grosor / 2), grosor)
        drawLine(verde, androidx.compose.ui.geometry.Offset(w - grosor / 2, 0f), androidx.compose.ui.geometry.Offset(w - grosor / 2, largo), grosor)
        drawLine(verde, androidx.compose.ui.geometry.Offset(0f, h - grosor / 2), androidx.compose.ui.geometry.Offset(largo, h - grosor / 2), grosor)
        drawLine(verde, androidx.compose.ui.geometry.Offset(grosor / 2, h - largo), androidx.compose.ui.geometry.Offset(grosor / 2, h), grosor)
        drawLine(verde, androidx.compose.ui.geometry.Offset(w - largo, h - grosor / 2), androidx.compose.ui.geometry.Offset(w, h - grosor / 2), grosor)
        drawLine(verde, androidx.compose.ui.geometry.Offset(w - grosor / 2, h - largo), androidx.compose.ui.geometry.Offset(w - grosor / 2, h), grosor)
    }
}
