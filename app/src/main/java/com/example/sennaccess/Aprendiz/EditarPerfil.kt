package com.example.sennaccess.aprendiz

// Vista de edición del perfil propio, compartida por Aprendiz e Instructor.
// Formulario de vidrio prellenado con los datos de la API (PUT /my-profile):
// identificación, nombres, apellidos, correo, ficha, programa y contraseña
// opcional (solo se cambia si viene llena). Al guardar refresca el perfil.

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.sennaccess.data.SessionManager
import com.example.sennaccess.data.UpdateProfileRequest
import com.example.sennaccess.data.UsuarioApi
import com.example.sennaccess.data.UsuarioRepository
import com.example.sennaccess.ui.CargaUiState
import com.example.sennaccess.ui.EstadoContenido
import com.example.sennaccess.ui.ios.GlassCornerRadius
import com.example.sennaccess.ui.ios.IosCollapsibleHeader
import com.example.sennaccess.ui.ios.glassSurface
import com.example.sennaccess.ui.ios.pressScale
import com.example.sennaccess.ui.theme.ErrorRed
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.math.min
import kotlin.math.roundToInt
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

// Vista de edición de perfil: recibe el estado del perfil (para prellenar los
// campos) y los callbacks de retroceso y de perfil actualizado. Si mostrarFichaPrograma
// es false (rol administrador) se ocultan los campos de ficha y programa y no se
// validan ni se envían; la foto se sube igual por multipart.
@Composable
fun EditarPerfilView(
    estado: CargaUiState<UsuarioApi>,
    onBack: () -> Unit,
    onGuardado: () -> Unit,
    onReintentar: () -> Unit,
    mostrarFichaPrograma: Boolean = true
) {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Estados de los campos, iniciados con los datos del usuario cargado.
    var identificacion by remember { mutableStateOf("") }
    var nombres by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var ficha by remember { mutableStateOf("") }
    var programa by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // Estado de la operación de guardado.
    var guardando by remember { mutableStateOf(false) }
    var errorMensaje by remember { mutableStateOf<String?>(null) }
    var guardado by remember { mutableStateOf(false) }

    // Foto de perfil: al elegir de la galería se abre primero el recortador
    // circular; solo el resultado recortado se guarda en fotoBitmap.
    val contexto = LocalContext.current
    var fotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var uriParaRecortar by remember { mutableStateOf<Uri?>(null) }
    val selectorFoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) uriParaRecortar = uri
    }

    // Recortador circular sobre la imagen recién elegida.
    uriParaRecortar?.let { uri ->
        RecortarFotoDialog(
            uriOriginal = uri,
            onRecortado = { recortada ->
                fotoBitmap = recortada
                uriParaRecortar = null
            },
            onCancel = { uriParaRecortar = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = colors.textPrimary) }
        }
        Spacer(modifier = Modifier.height(8.dp))

        EstadoContenido(estado = estado, onReintentar = onReintentar) { usuario ->
            // Los campos se rellenan la primera vez que llegan los datos del perfil.
            LaunchedEffect(usuario) {
                identificacion = usuario.user_identification ?: ""
                nombres = usuario.user_name ?: ""
                apellidos = usuario.user_lastname ?: ""
                correo = usuario.user_email ?: ""
                ficha = usuario.user_coursenumber?.toString() ?: ""
                programa = usuario.user_program ?: ""
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassSurface(cornerRadius = GlassCornerRadius)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar: muestra la foto elegida, si no la del servidor y si no el
                // icono por defecto. Toca la imagen para abrir la galería.
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(SenaGreen.copy(alpha = 0.15f))
                        .pressScale(pressedScale = 0.95f),
                    contentAlignment = Alignment.Center
                ) {
                    val urlServidor = SessionManager.fotoUrl(usuario.profile_photo_path)
                    when {
                        fotoBitmap != null -> Image(
                            bitmap = fotoBitmap!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        urlServidor != null -> AsyncImage(
                            model = urlServidor, contentDescription = null,
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                        )
                        else -> Icon(Icons.Default.Person, null, tint = SenaGreen, modifier = Modifier.size(50.dp))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                androidx.compose.material3.TextButton(onClick = { selectorFoto.launch("image/*") }) {
                    Text("CAMBIAR FOTO", color = SenaGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(usuario.nombreCompleto, color = colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))

                campoPerfil(identificacion, { identificacion = it }, "Número de Identificación")
                Spacer(modifier = Modifier.height(12.dp))
                campoPerfil(nombres, { nombres = it }, "Nombres")
                Spacer(modifier = Modifier.height(12.dp))
                campoPerfil(apellidos, { apellidos = it }, "Apellidos")
                Spacer(modifier = Modifier.height(12.dp))
                campoPerfil(correo, { correo = it }, "Correo Electrónico")
                Spacer(modifier = Modifier.height(12.dp))
                if (mostrarFichaPrograma) {
                    campoPerfil(ficha, { ficha = it }, "Número de Ficha")
                    Spacer(modifier = Modifier.height(12.dp))
                    campoPerfil(programa, { programa = it }, "Programa de Formación")
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Contraseña nueva (opcional): si se deja vacía no se modifica.
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Nueva contraseña (opcional)", color = colors.textSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = colors.textSecondary
                            )
                        }
                    },
                    colors = campoPerfilColors()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirmar contraseña", color = colors.textSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = campoPerfilColors()
                )

                // Error de validación o de la API.
                if (errorMensaje != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(errorMensaje!!, color = ErrorRed, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botón guardar: valida, envía PUT /my-profile y avisa al terminar.
                Button(
                    onClick = {
                        if (guardando) return@Button
                        if (password != confirmPassword) {
                            errorMensaje = "Las contraseñas no coinciden."
                            return@Button
                        }
                        if (password.isNotBlank() && password.length < 6) {
                            errorMensaje = "La contraseña debe tener al menos 6 caracteres."
                            return@Button
                        }
                        val fichaNum = if (mostrarFichaPrograma) ficha.trim().toIntOrNull() else null
                        if (mostrarFichaPrograma && fichaNum == null) {
                            errorMensaje = "El número de ficha debe ser numérico."
                            return@Button
                        }
                        // Sin ficha/programa visibles (admin e instructor) se envían null
                        // para que el backend los deje vacíos; el aprendiz conserva los suyos.
                        val fichaFinal = if (mostrarFichaPrograma) fichaNum else null
                        val programaFinal = if (mostrarFichaPrograma) programa.trim() else null
                        errorMensaje = null
                        guardando = true
                        scope.launch {
                            try {
                                // Con foto recortada se envía multipart (campo "image");
                                // la imagen sale cuadrada 512px en JPEG para que la
                                // subida nunca falle por tamaño o formato.
                                 val perfilActualizado = if (fotoBitmap != null) {
                                     val bytes = bitmapAJpeg(fotoBitmap!!)
                                     val parteFoto = okhttp3.MultipartBody.Part.createFormData(
                                         "image", "perfil.jpg", bytes.toRequestBody("image/jpeg".toMediaType())
                                     )
                                    UsuarioRepository().actualizarConFoto(
                                        SessionManager.token!!,
                                        parteFoto,
                                        identificacion.trim(),
                                        nombres.trim(),
                                        apellidos.trim(),
                                        correo.trim(),
                                        fichaFinal,
                                        programaFinal
                                    )
                                } else {
                                    UsuarioRepository().updateMyProfile(
                                        SessionManager.token!!,
                                        UpdateProfileRequest(
                                            user_identification = identificacion.trim(),
                                            user_name = nombres.trim(),
                                            user_lastname = apellidos.trim(),
                                            user_email = correo.trim(),
                                            user_password = password.ifBlank { null },
                                            user_coursenumber = fichaFinal,
                                            user_program = programaFinal
                                        )
                                    )
                                }
                                SessionManager.savePhoto(perfilActualizado.profile_photo_path)
                                guardando = false
                                guardado = true
                            } catch (e: retrofit2.HttpException) {
                                guardando = false
                                errorMensaje = com.example.sennaccess.ui.detalleHttp(e)
                            } catch (e: Exception) {
                                guardando = false
                                errorMensaje = "Fallo de conexión: ${e.message ?: e.javaClass.simpleName}"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp).pressScale(pressedScale = 0.97f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black)
                ) { Text(if (guardando) "GUARDANDO..." else "GUARDAR CAMBIOS", fontWeight = FontWeight.Bold, fontSize = 15.sp) }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, colors.textSecondary)
                ) { Text("CANCELAR", color = colors.textSecondary, fontWeight = FontWeight.Bold) }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Confirmación de guardado: refresca el perfil y vuelve a la vista anterior.
    if (guardado) {
        AlertDialog(
            onDismissRequest = { guardado = false },
            containerColor = colors.cardBackground.copy(alpha = 0.98f),
            shape = RoundedCornerShape(24.dp),
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SenaGreen, modifier = Modifier.size(40.dp)) },
            title = { Text("Perfil actualizado", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Tus datos se guardaron correctamente.", color = colors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = { guardado = false; onGuardado() },
                    colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                ) { Text("Okey", fontWeight = FontWeight.ExtraBold) }
            }
        )
    }
}

// Campo de texto del formulario de perfil con el estilo de la app.
@Composable
private fun campoPerfil(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = LocalAppColors.current.textSecondary) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = campoPerfilColors()
    )
}

// Paleta de colores común para los campos del formulario (verde al enfocar).
@Composable
private fun campoPerfilColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SenaGreen,
    unfocusedBorderColor = LocalAppColors.current.textSecondary.copy(alpha = 0.5f),
    focusedLabelColor = SenaGreen,
    unfocusedLabelColor = LocalAppColors.current.textSecondary,
    cursorColor = SenaGreen,
    focusedTextColor = LocalAppColors.current.textPrimary,
    unfocusedTextColor = LocalAppColors.current.textPrimary
)

// Comprime un bitmap ya recortado a JPEG para la subida multipart al servidor.
private fun bitmapAJpeg(bitmap: Bitmap, calidad: Int = 85): ByteArray {
    val salida = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, calidad, salida)
    return salida.toByteArray()
}

// Decodifica la imagen de una URI a Bitmap limitando el lado mayor (para no
// gastar memoria con fotos de cámara); soporta HEIC vía ImageDecoder.
private fun decodificarImagen(contexto: Context, uri: Uri, maxLado: Int = 2048): Bitmap? {
    return try {
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            android.graphics.ImageDecoder.decodeBitmap(
                android.graphics.ImageDecoder.createSource(contexto.contentResolver, uri)
            ) { decoder, info, _ ->
                // Bitmap de software para poder dibujarlo y comprimirlo a JPEG.
                decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                val mayor = maxOf(info.size.width, info.size.height)
                if (mayor > maxLado) {
                    decoder.setTargetSize(
                        (info.size.width * maxLado.toFloat() / mayor).toInt().coerceAtLeast(1),
                        (info.size.height * maxLado.toFloat() / mayor).toInt().coerceAtLeast(1)
                    )
                }
            }
        } else {
            contexto.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            }
        }
    } catch (_: Exception) {
        null
    }
}

// Diálogo de recorte circular de la foto de perfil: la imagen se mueve y hace
// zoom con gestos bajo un marco circular fijo; al confirmar devuelve un Bitmap
// cuadrado (512px) con exactamente lo visible dentro del círculo.
@Composable
private fun RecortarFotoDialog(
    uriOriginal: Uri,
    onRecortado: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    val contexto = LocalContext.current

    var imagen by remember { mutableStateOf<Bitmap?>(null) }
    var escala by remember { mutableStateOf(1f) }
    var desplazamiento by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(uriOriginal) {
        imagen = decodificarImagen(contexto, uriOriginal)
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "RECORTAR FOTO",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Mueve con el dedo y usa pinza para encuadrar",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(24.dp))

            val ladoDp = 300.dp
            val ladoPx = with(LocalDensity.current) { ladoDp.toPx() }
            val centro = ladoPx / 2f
            val img = imagen
            // Escala total: base (lado menor cubre el círculo) por la del usuario.
            val escalaTotal = if (img != null) {
                (ladoPx / min(img.width, img.height).toFloat()) * escala
            } else 1f
            // Límites de arrastre para que el círculo siempre quede cubierto.
            val limiteX = if (img != null) ((img.width * escalaTotal - ladoPx) / 2f).coerceAtLeast(0f) else 0f
            val limiteY = if (img != null) ((img.height * escalaTotal - ladoPx) / 2f).coerceAtLeast(0f) else 0f
            val dx = desplazamiento.x.coerceIn(-limiteX, limiteX)
            val dy = desplazamiento.y.coerceIn(-limiteY, limiteY)

            Box(modifier = Modifier.size(ladoDp)) {
                if (img == null) {
                    CircularProgressIndicator(color = SenaGreen, modifier = Modifier.align(Alignment.Center))
                } else {
                    // Imagen transformada (zoom + arrastre), recortada al marco.
                    Canvas(
                        modifier = Modifier
                            .size(ladoDp)
                            .clipToBounds()
                            .pointerInput(img) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    escala = (escala * zoom).coerceAtLeast(1f)
                                    desplazamiento += pan
                                }
                            }
                    ) {
                        val dibujo = img.asImageBitmap()
                        val izquierda = centro + dx - img.width * escalaTotal / 2f
                        val arriba = centro + dy - img.height * escalaTotal / 2f
                        drawImage(
                            image = dibujo,
                            srcOffset = IntOffset.Zero,
                            srcSize = IntSize(dibujo.width, dibujo.height),
                            dstOffset = IntOffset(izquierda.roundToInt(), arriba.roundToInt()),
                            dstSize = IntSize(
                                (dibujo.width * escalaTotal).roundToInt(),
                                (dibujo.height * escalaTotal).roundToInt()
                            )
                        )
                    }
                    // Máscara oscura fuera del círculo + anillo verde.
                    Canvas(modifier = Modifier.size(ladoDp)) {
                        val ruta = Path().apply {
                            fillType = PathFillType.EvenOdd
                            addRect(Rect(Offset.Zero, size))
                            addOval(Rect(Offset(centro, centro), centro))
                        }
                        drawPath(ruta, Color.Black.copy(alpha = 0.65f))
                        drawCircle(SenaGreen, style = Stroke(width = 3.dp.toPx()))
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    val actual = imagen ?: return@Button
                    val salida = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
                    val lienzo = android.graphics.Canvas(salida)
                    lienzo.drawColor(android.graphics.Color.WHITE)
                    val matriz = android.graphics.Matrix().apply {
                        setTranslate(-actual.width / 2f, -actual.height / 2f)
                        postScale(escalaTotal, escalaTotal)
                        postTranslate(centro + dx, centro + dy)
                        postScale(512f / ladoPx, 512f / ladoPx)
                    }
                    val pintura = android.graphics.Paint().apply { isFilterBitmap = true }
                    lienzo.drawBitmap(actual, matriz, pintura)
                    onRecortado(salida)
                },
                enabled = imagen != null,
                modifier = Modifier.fillMaxWidth().height(50.dp).pressScale(pressedScale = 0.97f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black)
            ) { Text("RECORTAR", fontWeight = FontWeight.Bold, fontSize = 15.sp) }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
            ) { Text("CANCELAR", color = Color.White, fontWeight = FontWeight.Bold) }
        }
    }
}