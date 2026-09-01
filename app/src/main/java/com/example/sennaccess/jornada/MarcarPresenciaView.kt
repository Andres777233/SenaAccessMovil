package com.example.sennaccess.jornada

// Pantalla de presencia física del aprendiz: FSM Registrado -> EN_AULA <-> EN_DESCANSO -> FINALIZADO.
// Desacopla el login del registro de presencia: valida TOTP (QR rotativo) + ubicación/BSSID server-side.
// El servidor es la fuente de verdad (hora NTP, ventanas de descanso, permisos).

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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.example.sennaccess.data.EstadoJornada
import com.example.sennaccess.data.JornadaEstadoResponse
import com.example.sennaccess.ui.CargaUiState
import com.example.sennaccess.ui.CargandoBox
import com.example.sennaccess.ui.ErrorBox
import com.example.sennaccess.ui.fechaLegible
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import com.example.sennaccess.ui.theme.ErrorRed
import com.example.sennaccess.ui.theme.OrangeAmber
import com.example.sennaccess.ui.ios.GlassCornerRadius
import com.example.sennaccess.ui.ios.glassSurface
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun MarcarPresenciaView(
    onBack: () -> Unit,
    viewModel: JornadaViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val estado by viewModel.estado.collectAsState()
    val operacion by viewModel.operacion.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var mostrarEscaner by remember { mutableStateOf(false) }
    var mostrarSalidaDialog by remember { mutableStateOf(false) }
    var permisoToken by remember { mutableStateOf("") }
    var motivoSalida by remember { mutableStateOf("") }
    var ambienteIdPresencia by remember { mutableStateOf<Int?>(null) }

    // Permisos de ubicación para la capa geo/BSSID.
    var permisoUbicacionConcedido by remember {
        mutableStateOf(tienePermisoUbicacion(context))
    }
    val launcherUbicacion = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { res ->
        permisoUbicacionConcedido = res[Manifest.permission.ACCESS_FINE_LOCATION] == true || res[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Snackbar para resultado de la operación.
    LaunchedEffect(operacion) {
        val op = operacion ?: return@LaunchedEffect
        when (op) {
            is CargaUiState.Success -> {
                val msg = op.datos.message ?: "Estado actualizado: ${op.datos.estado ?: ""}"
                snackbar.showSnackbar(msg)
                viewModel.limpiarOperacion()
            }
            is CargaUiState.Error -> {
                snackbar.showSnackbar(op.mensaje)
                viewModel.limpiarOperacion()
            }
            else -> {}
        }
    }

    // Si el escáner está activo, lo muestra a pantalla completa.
    if (mostrarEscaner) {
        EscanerJornadaQrView(
            onQrDetectado = { code ->
                mostrarEscaner = false
                // Extrae ambienteId del contenido si viene en formato SENA-JORNADA:1:CODE:30
                val amb = try { code.split(":").getOrNull(1)?.toIntOrNull() } catch (_: Exception) { null }
                val qrParaBackend = code
                if (!permisoUbicacionConcedido) {
                    launcherUbicacion.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }
                viewModel.marcarEnAula(context, qrParaBackend, amb ?: ambienteIdPresencia)
            },
            onCerrar = { mostrarEscaner = false }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = SenaGreen) }
                Text("Control de Jornada", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(start = 4.dp))
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = viewModel::cargarEstado) { Icon(Icons.Default.Refresh, null, tint = colors.textSecondary) }
            }
            Text("Tu presencia se valida con QR rotativo + ubicación y red del centro.", color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp))
            Spacer(modifier = Modifier.height(16.dp))

            // Card de estado FSM.
            when (val s = estado) {
                is CargaUiState.Loading -> CargandoBox()
                is CargaUiState.Error -> ErrorBox(s.mensaje, viewModel::cargarEstado)
                is CargaUiState.Success -> {
                    val dato = s.datos
                    val ej = dato.estadoEnum
                    ambienteIdPresencia = dato.ambiente_id
                    JornadaEstadoCard(dato)
                    Spacer(modifier = Modifier.height(16.dp))
                    // Aviso de permisos de ubicación si faltan y el usuario quiere marcar EN_AULA.
                    if (!permisoUbicacionConcedido && ej == EstadoJornada.REGISTRADO) {
                        Box(modifier = Modifier.fillMaxWidth().glassSurface(cornerRadius = 12.dp).border(1.dp, OrangeAmber.copy(0.6f), RoundedCornerShape(12.dp)).padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, tint = OrangeAmber, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Concede el permiso de ubicación para validar que estás en el centro.", color = colors.textPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                TextButton(onClick = { launcherUbicacion.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }) { Text("Permitir", color = SenaGreen, fontWeight = FontWeight.Bold) }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Botones según transiciones permitidas (server-side) + heurística local.
                    val permitidas = dato.transiciones_permitidas?.map { it.uppercase() } ?: emptyList()
                    val puedeEnAula = permitidas.contains("EN_AULA") || ej == EstadoJornada.REGISTRADO
                    val puedeDescanso = permitidas.contains("EN_DESCANSO") || ej == EstadoJornada.EN_AULA
                    val puedeRegreso = permitidas.contains("EN_AULA") && ej == EstadoJornada.EN_DESCANSO
                    val puedeFinalizar = permitidas.contains("FINALIZADO") || ej == EstadoJornada.EN_AULA || ej == EstadoJornada.EN_DESCANSO

                    // Estado final: sin acciones, muestra auditoría.
                    if (ej.esFinal()) {
                        Box(modifier = Modifier.fillMaxWidth().glassSurface(cornerRadius = GlassCornerRadius).padding(16.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CheckCircle, null, tint = SenaGreen, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Jornada ${ej.label()}", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Cierre: ${fechaLegible(dato.ultimo_cambio)} • 18:00 cierre automático", color = colors.textSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        // Acciones primarias.
                        if (ej == EstadoJornada.REGISTRADO && puedeEnAula) {
                            Button(
                                onClick = { mostrarEscaner = true },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("REGISTRAR EN AULA (escanear QR)", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            // Hint capas de verificación.
                            CapasVerificacionRow()
                        }
                        if (ej == EstadoJornada.EN_AULA) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = viewModel::marcarDescanso,
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SenaGreen.copy(0.18f), contentColor = SenaGreen)
                                ) {
                                    Icon(Icons.Default.Coffee, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Descanso", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Button(
                                    onClick = viewModel::marcarFinalizar,
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black)
                                ) {
                                    Icon(Icons.Default.DoorFront, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Finalizar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            TextButton(onClick = { mostrarSalidaDialog = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                Icon(Icons.Default.Logout, null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Salida anticipada (con permiso)", color = ErrorRed, fontSize = 12.sp)
                            }
                            CapasVerificacionRow()
                        }
                        if (ej == EstadoJornada.EN_DESCANSO) {
                            Button(
                                onClick = viewModel::marcarRegresoAula,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black)
                            ) {
                                Icon(Icons.Default.DoorFront, null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("REGRESAR A AULA", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Estás en receso. Vuelve al aula antes de que cierre la ventana.", color = colors.textSecondary, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                        // Operación en curso.
                        if (operacion is CargaUiState.Loading) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = SenaGreen, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Validando en el servidor (NTP + TOTP + ubicación)...", color = colors.textSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Ventana lectiva y descansos (config por ambiente).
                    JornadaConfigCard(dato)
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
    }

    // Diálogo de salida anticipada: pide el token del instructor + motivo.
    if (mostrarSalidaDialog) {
        AlertDialog(
            onDismissRequest = { mostrarSalidaDialog = false },
            title = { Text("Salida anticipada", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Requiere autorización del instructor/admin. Pide tu permiso (token) y motivo.", color = LocalAppColors.current.textSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = permisoToken, onValueChange = { permisoToken = it }, label = { Text("Token de permiso") }, placeholder = { Text("pega aquí el token") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = motivoSalida, onValueChange = { motivoSalida = it }, label = { Text("Motivo (opcional)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    mostrarSalidaDialog = false
                    viewModel.marcarSalidaAnticipada(permisoToken, motivoSalida.ifBlank { null })
                }, colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black)) { Text("Confirmar salida") }
            },
            dismissButton = { TextButton(onClick = { mostrarSalidaDialog = false }) { Text("Cancelar") } }
        )
    }
}

// Card del estado FSM con color semántico y timestamp.
@Composable
private fun JornadaEstadoCard(dato: JornadaEstadoResponse) {
    val colors = LocalAppColors.current
    val ej = dato.estadoEnum
    val (bg, fg, icon) = when (ej) {
        EstadoJornada.EN_AULA -> Triple(SenaGreen.copy(0.15f), SenaGreen, Icons.Default.CheckCircle)
        EstadoJornada.EN_DESCANSO -> Triple(OrangeAmber.copy(0.15f), OrangeAmber, Icons.Default.Coffee)
        EstadoJornada.FINALIZADO, EstadoJornada.SALIDA_ANTICIPADA, EstadoJornada.ABANDONO_UNILATERAL -> Triple(SenaGreen.copy(0.12f), SenaGreen, Icons.Default.CheckCircle)
        else -> Triple(colors.surfaceVariant, colors.textSecondary, Icons.Default.Campaign)
    }
    Box(modifier = Modifier.fillMaxWidth().glassSurface(cornerRadius = GlassCornerRadius).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = fg, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ej.label(), color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(fg.copy(0.18f)).border(1.dp, fg.copy(0.4f), RoundedCornerShape(6.dp)).padding(horizontal = 7.dp, vertical = 3.dp)) {
                        Text(ej.raw, color = fg, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Último cambio: ${fechaLegible(dato.ultimo_cambio)}", color = colors.textSecondary, fontSize = 12.sp)
                if (!dato.ambiente_nombre.isNullOrBlank()) Text("Ambiente: ${dato.ambiente_nombre}", color = colors.textSecondary, fontSize = 12.sp)
            }
        }
    }
}

// Fila de las 3 capas de verificación (hint educativo).
@Composable
private fun CapasVerificacionRow() {
    val colors = LocalAppColors.current
    Row(modifier = Modifier.fillMaxWidth().glassSurface(cornerRadius = 12.dp).padding(10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        CapaChip(Icons.Default.QrCodeScanner, "QR TOTP")
        CapaChip(Icons.Default.LocationOn, "GPS")
        CapaChip(Icons.Default.Wifi, "Wi-Fi")
    }
}
@Composable
private fun CapaChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    val colors = LocalAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 6.dp)) {
        Icon(icon, null, tint = SenaGreen, modifier = Modifier.size(18.dp))
        Text(label, color = colors.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

// Card de configuración de jornada (ventana + descansos).
@Composable
private fun JornadaConfigCard(dato: JornadaEstadoResponse) {
    val colors = LocalAppColors.current
    Box(modifier = Modifier.fillMaxWidth().glassSurface(cornerRadius = 12.dp).padding(14.dp)) {
        Column {
            Text("Jornada", color = SenaGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text("${dato.jornada_inicio ?: "13:00"} — ${dato.jornada_fin ?: "18:00"}  •  hora validada por NTP del servidor", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            if (!dato.descansos.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = colors.border)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Ventanas de receso", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                dato.descansos.forEach { v ->
                    Text("• ${v.inicio ?: "?"}–${v.fin ?: "?"}", color = colors.textSecondary, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text("A las 18:00 el servidor cierra automáticamente las jornadas activas (FINALIZADO o ABANDONO).", color = colors.textSecondary, fontSize = 11.sp)
        }
    }
}

// Escáner dedicado para el QR de aula (TOTP). Reusa el patrón de EscanearQrView.
@Composable
private fun EscanerJornadaQrView(onQrDetectado: (String) -> Unit, onCerrar: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permisoCamara by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permisoCamara = it }
    val procesando = remember { AtomicBoolean(false) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (permisoCamara) {
            AndroidView(factory = { ctx ->
                val previewView = PreviewView(ctx).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
                val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                val scanner = BarcodeScanning.getClient()
                analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { proxy ->
                    val img = proxy.image
                    val rot = proxy.imageInfo.rotationDegrees
                    if (img == null || procesando.get()) { proxy.close(); return@setAnalyzer }
                    scanner.process(InputImage.fromMediaImage(img, rot))
                        .addOnSuccessListener { barcodes ->
                            val raw = barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE && !it.rawValue.isNullOrBlank() }?.rawValue
                            if (raw != null && procesando.compareAndSet(false, true)) {
                                scope.launch { onQrDetectado(raw) }
                            }
                        }.addOnCompleteListener { proxy.close() }
                }
                val future = ProcessCameraProvider.getInstance(ctx)
                future.addListener({
                    val provider = future.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    try { provider.unbindAll(); provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis) } catch (_: Exception) {}
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            })
        }
        Box(modifier = Modifier.align(Alignment.Center).size(250.dp).border(3.dp, SenaGreen, RoundedCornerShape(16.dp)))
        Column(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().background(Color.Black.copy(0.55f)).padding(horizontal = 8.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCerrar) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
                Text("Escanear QR del aula", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(0.55f)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (!permisoCamara) {
                Text("Se necesita permiso de cámara para escanear el QR rotativo.", color = Color.White, textAlign = TextAlign.Center, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }, colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black), shape = RoundedCornerShape(10.dp)) { Text("Conceder permiso", fontWeight = FontWeight.Bold) }
            } else {
                Text("Apunta al QR proyectado en el aula (cambia cada 30s)", color = Color.White, textAlign = TextAlign.Center, fontSize = 13.sp)
            }
        }
    }
}
