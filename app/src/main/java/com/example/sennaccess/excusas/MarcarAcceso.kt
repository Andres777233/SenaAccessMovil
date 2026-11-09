package com.example.sennaccess.excusas

// Marcar entrada/salida con el QR de portería: CameraX + MLKit leen el QR
// rotativo de ENTRADA/SALIDA y lo validan con POST /acceso/validar. El modo
// manual usa el PIN de 6 dígitos que dicta portería (un solo campo, sin repetir
// el "pega el código" que antes aparecía junto a la cámara).

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.sennaccess.comun.campoVisible
import com.example.sennaccess.comun.detalleHttp
import com.example.sennaccess.comun.diseno.BotonPrimarioNeon
import com.example.sennaccess.comun.diseno.CajaError as CajaErrorSena
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import com.example.sennaccess.datos.repositorios.RepositorioAcceso
import com.example.sennaccess.datos.sesion.GestorSesion
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MarcarAcceso(
    onBack: () -> Unit,
    mostrarCabecera: Boolean = true,
    onExito: ((String) -> Unit)? = null
) {
    val colors = ColoresAppLocal.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val repo = remember { RepositorioAcceso() }

    var modo by remember { mutableIntStateOf(0) }
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
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var exito by remember { mutableStateOf<String?>(null) }
    val proveedorCamara = remember { mutableStateOf<ProcessCameraProvider?>(null) }

    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        )
    }

    // Libera la cámara al salir o al pasar al modo PIN.
    DisposableEffect(lifecycleOwner) {
        onDispose {
            try { proveedorCamara.value?.unbindAll() } catch (_: Exception) { }
        }
    }
    LaunchedEffect(modo) {
        if (modo != 0) {
            try { proveedorCamara.value?.unbindAll() } catch (_: Exception) { }
        }
    }

    fun alRegistrar(msg: String) {
        pin = ""
        if (onExito != null) onExito.invoke(msg) else exito = msg
    }

    fun validarQr(payload: String) {
        val token = GestorSesion.token
        if (token == null || cargando) return
        if (payload.isBlank()) {
            error = "Apunta la cámara al QR de portería."
            return
        }
        scope.launch {
            cargando = true
            error = null
            exito = null
            try {
                val resp = repo.validar(token, payload.trim())
                alRegistrar(resp.message ?: "Movimiento registrado.")
            } catch (e: retrofit2.HttpException) {
                error = detalleHttp(e)
            } catch (e: Exception) {
                error = "Error de conexión: ${e.message ?: e.javaClass.simpleName}"
            } finally {
                cargando = false
            }
            if (onExito == null) {
                delay(2_500)
                detenerAnalisis.value = false
            }
        }
    }

    fun validarPin() {
        val token = GestorSesion.token
        if (token == null || cargando) return
        if (pin.length != 6) {
            error = "Ingresa los 6 dígitos del PIN de portería."
            return
        }
        scope.launch {
            cargando = true
            error = null
            exito = null
            try {
                val resp = repo.validarPin(token, pin.trim())
                alRegistrar(resp.message ?: "Movimiento registrado.")
            } catch (e: retrofit2.HttpException) {
                error = detalleHttp(e)
            } catch (e: Exception) {
                error = "Error de conexión: ${e.message ?: e.javaClass.simpleName}"
            } finally {
                cargando = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        if (mostrarCabecera) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = colors.textPrimary)
                }
                Column {
                    Text("Salida del centro", color = colors.textPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    Text("QR o PIN de portería", color = colors.textSecondary, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Dos caminos, un solo campo cada uno: cámara o PIN. Nada duplicado.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surfaceVariant.copy(alpha = 0.4f))
                .padding(4.dp)
        ) {
            listOf("CÁMARA" to 0, "ESCRIBIR PIN" to 1).forEach { (texto, indice) ->
                val activo = modo == indice
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (activo) verdeMarca().copy(alpha = 0.16f) else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { modo = indice; error = null }
                        .padding(vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = if (indice == 0) Icons.Default.CameraAlt else Icons.Default.Keyboard,
                        contentDescription = null,
                        tint = if (activo) verdeMarca() else colors.textSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        texto,
                        color = if (activo) colors.textPrimary else colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (activo) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (modo == 0) {
            if (permisoCamara) {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    proveedorCamara.value = cameraProvider
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
                                                    validarQr(raw)
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
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    if (cargando) "Registrando en el historial..." else "Apunta al QR de ENTRADA o SALIDA",
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
                        text = "Activar cámara",
                        icon = Icons.Default.CameraAlt,
                        onClick = { launcherPermiso.launch(Manifest.permission.CAMERA) }
                    )
                }
            }
        } else {
            Text(
                "PIN de portería (6 dígitos)",
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Pídelo en portería si la cámara no funciona. Vale 5 minutos y un solo uso.",
                color = colors.textSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { texto ->
                    if (texto.length <= 6 && texto.all { it.isDigit() }) {
                        pin = texto
                        error = null
                    }
                },
                label = { Text("PIN de portería") },
                placeholder = { Text("• • • • • •") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().campoVisible(),
                shape = RoundedCornerShape(16.dp),
                textStyle = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp,
                    textAlign = TextAlign.Center
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            BotonPrimarioNeon(
                text = "VALIDAR PIN",
                icon = Icons.Default.Keyboard,
                onClick = { validarPin() },
                enabled = pin.length == 6 && !cargando,
                loading = cargando,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        if (cargando) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator(color = verdeMarca(), modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Registrando en el servidor...", color = colors.textSecondary, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (error != null) {
            CajaErrorSena(error!!)
            Spacer(modifier = Modifier.height(8.dp))
        }
        // El éxito local solo aplica fuera de la puerta: ahí el mensaje lo muestra PuertaAcceso.
        if (exito != null && onExito == null) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(VerdeSena.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = verdeMarca(), modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(exito!!, color = verdeMarca(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    exito = null
                    error = null
                    pin = ""
                    detenerAnalisis.value = false
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.QrCode2, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Marcar otro")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
