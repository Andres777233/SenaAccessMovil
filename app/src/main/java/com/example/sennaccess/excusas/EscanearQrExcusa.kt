package com.example.sennaccess.excusas

// Escáner QR de excusas en portería: CameraX (Preview) + MLKit detectan el QR
// SENA-EXCUSA:<pin>:<id> generado por CrearExcusa/MisExcusas y lo validan con
// POST /excusas/validar. Al validar se muestra la identidad (nombre, documento,
// ficha, ambiente) para confirmar que esa es la persona antes de dejarla salir.

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.sennaccess.comun.detalleHttp
import com.example.sennaccess.comun.diseno.BotonBordeBrillante
import com.example.sennaccess.comun.diseno.BotonPrimarioNeon
import com.example.sennaccess.comun.diseno.CajaError as CajaErrorSena
import com.example.sennaccess.comun.fechaLegible
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import com.example.sennaccess.datos.modelos.Excusa
import com.example.sennaccess.datos.repositorios.RepositorioExcusas
import com.example.sennaccess.datos.sesion.GestorSesion
import com.example.sennaccess.perfil.FotoPerfil
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EscanearQrExcusa(mostrarCabecera: Boolean = true) {
    val colors = ColoresAppLocal.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val repo = remember { RepositorioExcusas() }

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
    var exito by remember { mutableStateOf<Excusa?>(null) }
    var exitoNombre by remember { mutableStateOf<String?>(null) }
    var mensaje by remember { mutableStateOf<String?>(null) }

    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        )
    }

    fun validarPin(pin: String) {
        val token = GestorSesion.token
        if (token == null || cargando) return
        scope.launch {
            cargando = true
            errorValidar = null
            try {
                val resp = repo.validar(token, pin)
                exito = resp.excusa
                exitoNombre = resp.excusa?.aprendiz?.nombreCompleto ?: resp.aprendiz?.nombreCompleto
                mensaje = resp.message
            } catch (e: retrofit2.HttpException) {
                errorValidar = detalleHttp(e)
            } catch (e: Exception) {
                errorValidar = "Error de conexión: ${e.message ?: e.javaClass.simpleName}"
            } finally {
                cargando = false
            }
            delay(2_500)
            detenerAnalisis.value = false
        }
    }

    fun alEscanear(raw: String) {
        val pin = pinDeQrExcusa(raw)
        if (pin == null) {
            errorQr = "Ese QR no es una excusa válida. Pide al aprendiz el QR del permiso (o valida el PIN a mano)."
            detenerAnalisis.value = false
            return
        }
        errorQr = null
        validarPin(pin)
    }

    Column(modifier = Modifier.fillMaxWidth().imePadding()) {
        if (mostrarCabecera) {
            Text("QR de excusa para autorizar la salida.", color = colors.textSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Apunta al QR del permiso del aprendiz.",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
            }
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
                MarcoEscanerExcusa(modifier = Modifier.fillMaxWidth().aspectRatio(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                if (cargando) "Validando excusa..." else "Apunta al QR del permiso del aprendiz",
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

        Spacer(modifier = Modifier.height(12.dp))
        if (cargando) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator(color = verdeMarca(), modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Validando en el servidor...", color = colors.textSecondary, fontSize = 13.sp)
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

        exito?.let { ex ->
            val nombre = exitoNombre ?: ex.aprendiz?.nombreCompleto ?: "Aprendiz #${ex.fk_id_aprendiz}"
            val doc = ex.aprendiz?.user_identification ?: "—"
            val ficha = ex.aprendiz?.user_coursenumber?.takeIf { it > 0 }?.let { " • Ficha $it" } ?: ""
            val amb = ex.ambiente?.ambiente_nombre ?: "Ambiente #${ex.fk_id_ambiente}"
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
                            mensaje ?: "Salida autorizada",
                            color = verdeMarca(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FotoPerfil(
                            fotoPath = ex.aprendiz?.profile_photo_path,
                            nombre = nombre,
                            tamano = 48.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(nombre, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("CC $doc$ficha", color = colors.textSecondary, fontSize = 12.sp)
                            Text(amb, color = colors.textSecondary, fontSize = 12.sp)
                        }
                    }
                    if (!ex.motivo.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Motivo: ${ex.motivo}", color = colors.textSecondary, fontSize = 12.sp)
                    }
                    if (ex.usado_en != null) {
                        Text(
                            "Registrada: ${fechaLegible(ex.usado_en)}",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Verifica el documento del aprendiz antes de dejarlo salir.",
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
                    exitoNombre = null
                    mensaje = null
                    errorQr = null
                    errorValidar = null
                    detenerAnalisis.value = false
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// Marco de encuadre con esquinas verdes sobre el visor.
@Composable
private fun MarcoEscanerExcusa(modifier: Modifier = Modifier) {
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
