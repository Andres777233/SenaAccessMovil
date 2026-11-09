package com.example.sennaccess.excusas

// Validación de excusas en portería: un solo lugar con cámara o PIN de 4
// dígitos. La bandeja de excusas pendientes sigue visible y se refresca cada
// 10 s, así ningún permiso recién creado se pierde mientras se valida otro.

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import com.example.sennaccess.comun.diseno.RadioSena
import com.example.sennaccess.comun.diseno.superficiePlana
import com.example.sennaccess.comun.diseno.superficieVidrio
import com.example.sennaccess.comun.fechaLegible
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.RojoError
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import com.example.sennaccess.datos.modelos.Excusa
import com.example.sennaccess.datos.modelos.estaPendiente
import com.example.sennaccess.datos.repositorios.RepositorioExcusas
import com.example.sennaccess.datos.sesion.GestorSesion
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VistaValidarExcusaCompleta(onBack: () -> Unit, mostrarCabecera: Boolean = true) {
    val colors = ColoresAppLocal.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val repo = remember { RepositorioExcusas() }
    val token = GestorSesion.token

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
    val proveedorCamara = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        )
    }

    var pin by remember { mutableStateOf("") }
    var validando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var errorCarga by remember { mutableStateOf<String?>(null) }
    var exito by remember { mutableStateOf<Excusa?>(null) }
    var exitoNombre by remember { mutableStateOf<String?>(null) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var pendientes by remember { mutableStateOf<List<Excusa>>(emptyList()) }
    var pideConfirmar by remember { mutableStateOf(false) }
    val ahora = recordarAhoraCada30s()

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

    suspend fun cargarPendientes(silencioso: Boolean = false) {
        if (token == null) return
        try {
            pendientes = repo.todasAdmin(token).filter { it.estaPendiente }
            errorCarga = null
        } catch (e: retrofit2.HttpException) { if (!silencioso) errorCarga = detalleHttp(e) }
        catch (e: Exception) { if (!silencioso) errorCarga = "No se pudieron cargar pendientes: ${e.message}" }
    }

    LaunchedEffect(Unit) { cargarPendientes() }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000)
            cargarPendientes(silencioso = true)
        }
    }

    fun ejecutarValidacion(pinTexto: String) {
        if (validando) return
        if (pinTexto.length != 4) { error = "Ingresa los 4 dígitos del PIN"; return }
        if (token == null) { error = "Sin sesión"; return }
        validando = true
        error = null
        exito = null
        scope.launch {
            try {
                val resp = repo.validar(token, pinTexto)
                validando = false
                exito = resp.excusa
                exitoNombre = resp.excusa?.aprendiz?.nombreCompleto ?: resp.aprendiz?.nombreCompleto
                mensaje = resp.message
                pin = ""
                pideConfirmar = false
                detenerAnalisis.value = true
                cargarPendientes(silencioso = true)
            } catch (e: retrofit2.HttpException) {
                validando = false
                pideConfirmar = false
                error = detalleHttp(e)
                delay(1_500)
                detenerAnalisis.value = false
            } catch (e: Exception) {
                validando = false
                pideConfirmar = false
                error = "Fallo de conexión: ${e.message}"
                delay(1_500)
                detenerAnalisis.value = false
            }
        }
    }

    fun alEscanear(raw: String) {
        val leido = pinDeQrExcusa(raw)
        if (leido == null) {
            error = "Ese QR no es una excusa válida. Pide el QR del permiso o escribe el PIN."
            detenerAnalisis.value = false
            return
        }
        pin = leido
        ejecutarValidacion(leido)
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()) {
        if (mostrarCabecera) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = colors.textPrimary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "VALIDAR SALIDA",
                        color = verdeMarca(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp
                    )
                    Text("Excusa del aprendiz", color = colors.textPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                }
                IconButton(onClick = { scope.launch { cargarPendientes() } }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Recargar pendientes", tint = colors.textSecondary)
                }
            }
            Text(
                "Escanea el QR del permiso o escribe el PIN de 4 dígitos. Al validar se registra la Salida.",
                color = colors.textSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Pendientes en tiempo real",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { scope.launch { cargarPendientes() } },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Recargar pendientes", tint = colors.textSecondary)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

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
                        .background(if (activo) verdeMarca().copy(alpha = 0.16f) else Color.Transparent)
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

        if (modo == 0 && exito == null) {
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
                                    analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                                        if (detenerAnalisis.value || validando) {
                                            imageProxy.close()
                                            return@setAnalyzer
                                        }
                                        val media = imageProxy.image
                                        if (media == null) {
                                            imageProxy.close()
                                            return@setAnalyzer
                                        }
                                        val input = InputImage.fromMediaImage(media, imageProxy.imageInfo.rotationDegrees)
                                        scanner.process(input)
                                            .addOnSuccessListener { barcodes ->
                                                val raw = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
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
                    MarcoEscanerExcusaCompleta(modifier = Modifier.fillMaxWidth().aspectRatio(1f))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    if (validando) "Validando excusa..." else "Apunta al QR del permiso del aprendiz",
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
                        "La cámara está desactivada. Permite su uso o cambia a escribir el PIN.",
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    BotonPrimarioNeon(
                        text = "ACTIVAR CÁMARA",
                        icon = Icons.Default.CameraAlt,
                        onClick = { launcherPermiso.launch(Manifest.permission.CAMERA) }
                    )
                }
            }
        } else if (modo == 1) {
            Text(
                "PIN de excusa (4 dígitos)",
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "El instructor se lo entrega al aprendiz. Toca una excusa pendiente para autollenarlo.",
                color = colors.textSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { v -> pin = v.filter { it.isDigit() }.take(4) },
                label = { Text("PIN de excusa (4 dígitos)") },
                placeholder = { Text("• • • •") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().campoVisible(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp,
                    textAlign = TextAlign.Center
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { pideConfirmar = true },
                enabled = !validando && pin.length == 4,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = Color.Black)
            ) {
                if (validando) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("VALIDAR Y REGISTRAR SALIDA", fontWeight = FontWeight.Bold)
            }
        } else {
            // Tras validar en cámara se oculta el preview y manda el detalle.
            Spacer(modifier = Modifier.height(2.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))
        if (validando) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator(color = verdeMarca(), modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Validando en el servidor...", color = colors.textSecondary, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (error != null) {
            CajaErrorSena(error!!)
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (errorCarga != null) {
            Text(errorCarga!!, color = RojoError, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (pideConfirmar) {
            AlertDialog(
                onDismissRequest = { if (!validando) pideConfirmar = false },
                title = { Text("¿Validar PIN $pin?") },
                text = { Text("Se registra la Salida del aprendiz en su historial. Verifica su documento antes de confirmar.") },
                confirmButton = {
                    TextButton(onClick = { ejecutarValidacion(pin) }, enabled = !validando) {
                        Text(if (validando) "Validando..." else "Sí, validar")
                    }
                },
                dismissButton = { TextButton(onClick = { pideConfirmar = false }, enabled = !validando) { Text("Cancelar") } }
            )
        }

        exito?.let { ex ->
            Box(modifier = Modifier.fillMaxWidth().superficieVidrio(cornerRadius = 24.dp, elevated = true).padding(16.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = verdeMarca(), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(mensaje ?: "Salida autorizada", color = verdeMarca(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        exitoNombre ?: ex.aprendiz?.nombreCompleto ?: "Aprendiz #${ex.fk_id_aprendiz}",
                        color = colors.textPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                    Text(
                        "Documento: ${ex.aprendiz?.user_identification ?: "—"} • ${ex.aprendiz?.user_email ?: ""}",
                        color = colors.textPrimary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(textoCreacionExcusa(ex), color = colors.textPrimary, fontSize = 14.sp)
                    if (ex.usado_en != null) {
                        Text(
                            "Registrada: ${fechaLegible(ex.usado_en)}",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Autoriza la salida del aprendiz y actualiza el historial (Salida).",
                        color = colors.textPrimary,
                        fontSize = 13.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    exito = null
                    pin = ""
                    error = null
                    mensaje = null
                    exitoNombre = null
                    detenerAnalisis.value = false
                    scope.launch { cargarPendientes() }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VerdeSena.copy(alpha = 0.18f),
                    contentColor = verdeMarca()
                )
            ) { Text("VALIDAR OTRA", fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Text(
            "EXCUSAS PENDIENTES",
            color = verdeMarca(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.8.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Toca una para usar su PIN (siguen llegando en tiempo real).",
            color = colors.textPrimary,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (pendientes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().superficiePlana(cornerRadius = RadioSena.md).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay excusas pendientes.", color = colors.textSecondary, fontSize = 13.sp)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                pendientes.forEach { ex ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(colors.surface, RoundedCornerShape(16.dp))
                            .clickable(enabled = ex.pin?.length == 4) {
                                modo = 1
                                ex.pin?.let { pin = it }
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                ex.aprendiz?.nombreCompleto ?: "Aprendiz #${ex.fk_id_aprendiz}",
                                color = colors.textPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            val documento = ex.aprendiz?.user_identification?.takeIf { it.isNotBlank() }
                            val ficha = ex.aprendiz?.user_coursenumber?.takeIf { it > 0 }
                            if (documento != null || ficha != null) {
                                Text(
                                    listOfNotNull(
                                        documento?.let { "Documento: $it" },
                                        ficha?.let { "Ficha: $it" }
                                    ).joinToString("  •  "),
                                    color = colors.textPrimary,
                                    fontSize = 14.sp,
                                    maxLines = 2
                                )
                            }
                            Text(
                                "Ambiente: ${ex.ambiente?.ambiente_nombre ?: "—"}",
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                maxLines = 2
                            )
                            Text(
                                "Instructor: ${ex.instructor?.nombreCompleto ?: "#${ex.fk_id_instructor}"}",
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                maxLines = 2
                            )
                            ex.motivo?.takeIf { it.isNotBlank() }?.let { motivo ->
                                Text(
                                    "Motivo: $motivo",
                                    color = colors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 3
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "PIN ${ex.pin ?: "—"}",
                                color = verdeMarca(),
                                fontSize = 27.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 3.sp,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                textoVigencia(ex.expira_en, ahora),
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        InsigniaExcusa(ex.estado)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun MarcoEscanerExcusaCompleta(modifier: Modifier = Modifier) {
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
