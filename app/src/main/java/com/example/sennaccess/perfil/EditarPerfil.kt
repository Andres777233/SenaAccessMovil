package com.example.sennaccess.perfil

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
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.sennaccess.datos.sesion.GestorSesion
import com.example.sennaccess.datos.repositorios.RepositorioDobleFactor
import com.example.sennaccess.datos.modelos.PeticionActualizarPerfil
import com.example.sennaccess.datos.modelos.UsuarioApi
import com.example.sennaccess.datos.repositorios.RepositorioUsuarios
import com.example.sennaccess.comun.EstadoCarga
import com.example.sennaccess.comun.campoVisible
import com.example.sennaccess.comun.EstadoContenido
import com.example.sennaccess.comun.diseno.RadioVidrio
import com.example.sennaccess.comun.diseno.CabeceraPlegable
import com.example.sennaccess.comun.diseno.superficieVidrio
import com.example.sennaccess.comun.diseno.superficiePlana
import com.example.sennaccess.comun.diseno.escalaPresion
import com.example.sennaccess.comun.tema.RojoError
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.math.min
import kotlin.math.roundToInt
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

@Composable
fun VistaEditarPerfil(
    estado: EstadoCarga<UsuarioApi>,
    onBack: () -> Unit,
    onGuardado: () -> Unit,
    onReintentar: () -> Unit,
    mostrarFichaPrograma: Boolean = true
) {
    val colors = ColoresAppLocal.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var identificacion by remember { mutableStateOf("") }
    var nombres by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var ficha by remember { mutableStateOf("") }
    var programa by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    var guardando by remember { mutableStateOf(false) }
    var errorMensaje by remember { mutableStateOf<String?>(null) }
    var guardado by remember { mutableStateOf(false) }

    val repo2Fa = remember { RepositorioDobleFactor() }
    var dosFaActivo by remember { mutableStateOf(false) }
    var pedirCodigoClave by remember { mutableStateOf(false) }
    var codigoClave by remember { mutableStateOf("") }
    var errorCodigoClave by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val token = GestorSesion.token ?: return@LaunchedEffect
        try {
            dosFaActivo = repo2Fa.estadoConfig(token).two_factor_enabled == true
        } catch (_: Exception) { /* sin red: se guarda sin puerta y el backend decide */ }
    }

    val contexto = LocalContext.current
    var fotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var uriParaRecortar by remember { mutableStateOf<Uri?>(null) }
    val selectorFoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) uriParaRecortar = uri
    }

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
            .imePadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = colors.textPrimary) }
            Column(modifier = Modifier.weight(1f)) {
                Text("Editar perfil", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Actualiza tus datos", color = colors.textSecondary, fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        EstadoContenido(estado = estado, onReintentar = onReintentar) { usuario ->
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
                    .superficieVidrio(cornerRadius = RadioVidrio, elevated = true)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(VerdeSena.copy(alpha = 0.15f))
                        .escalaPresion(pressedScale = 0.95f),
                    contentAlignment = Alignment.Center
                ) {
                    val urlServidor = GestorSesion.fotoUrl(usuario.profile_photo_path)
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
                        else -> Icon(Icons.Default.Person, null, tint = verdeMarca(), modifier = Modifier.size(50.dp))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                androidx.compose.material3.TextButton(onClick = { selectorFoto.launch("image/*") }) {
                    Text("CAMBIAR FOTO", color = verdeMarca(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(usuario.nombreCompleto, color = colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Ficha de datos en plana: el vidrio queda solo para el hero de foto.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .superficiePlana(cornerRadius = RadioVidrio)
                    .padding(20.dp)
            ) {
                EtiquetaPerfil("Datos personales")
                Spacer(modifier = Modifier.height(8.dp))
                campoPerfil(identificacion, { identificacion = it }, "Número de Identificación")
                Spacer(modifier = Modifier.height(12.dp))
                campoPerfil(nombres, { nombres = it }, "Nombres")
                Spacer(modifier = Modifier.height(12.dp))
                campoPerfil(apellidos, { apellidos = it }, "Apellidos")
                Spacer(modifier = Modifier.height(12.dp))
                // Correo BLOQUEADO (fix seguridad): solo lectura + botón CAMBIAR CORREO
                // con verificación por código. Nadie cambia su correo sin el código.
                OutlinedTextField(
                    value = correo,
                    onValueChange = { },
                    label = { Text("Correo Electrónico", color = colors.textSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    readOnly = true,
                    enabled = false,
                    colors = campoPerfilColors(),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                var mostrarDialogoCorreo by remember { mutableStateOf(false) }
                var correoNuevo by remember { mutableStateOf("") }
                OutlinedButton(
                    onClick = { correoNuevo = correo; mostrarDialogoCorreo = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, verdeMarca())
                ) { Text("CAMBIAR CORREO", color = verdeMarca(), fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                if (mostrarDialogoCorreo) {
                    AlertDialog(
                        onDismissRequest = { mostrarDialogoCorreo = false },
                        containerColor = colors.cardBackground.copy(alpha = 0.98f),
                        shape = RoundedCornerShape(20.dp),
                        title = { Text("Cambiar correo", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
                        text = {
                            Column {
                                Text(
                                    "Escribe tu nuevo correo. Al guardar se enviará un código de verificación a ese correo.",
                                    color = colors.textSecondary, fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = correoNuevo,
                                    onValueChange = { correoNuevo = it },
                                    label = { Text("Nuevo correo") },
                                    modifier = Modifier.fillMaxWidth().campoVisible(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val limpio = correoNuevo.trim().lowercase()
                                val dominiosOk = listOf("@gmail.com", "@hotmail.com", "@outlook.com", "@soy.sena.edu.co")
                                if (limpio.isBlank() || dominiosOk.none { limpio.endsWith(it) }) {
                                    errorMensaje = "El nuevo correo debe ser @gmail.com, @hotmail.com, @outlook.com o @soy.sena.edu.co."
                                    return@TextButton
                                }
                                correo = limpio
                                mostrarDialogoCorreo = false
                            }) { Text("USAR ESTE CORREO", fontWeight = FontWeight.Bold, color = verdeMarca()) }
                        },
                        dismissButton = {
                            TextButton(onClick = { mostrarDialogoCorreo = false }) {
                                Text("CANCELAR", color = colors.textSecondary)
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Ficha/programa: SOLO el admin los cambia (Crear/ActualizarUsuario).
                // Aquí siempre son solo lectura, nunca editables desde el perfil propio.
                if (!usuario.user_program.isNullOrBlank() || (usuario.user_coursenumber ?: 0) > 0) {
                    if ((usuario.user_coursenumber ?: 0) > 0) {
                        campoLectura("Ficha", usuario.user_coursenumber.toString())
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    if (!usuario.user_program.isNullOrBlank()) {
                        campoLectura("Programa de Formación", usuario.user_program!!)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                if (mostrarFichaPrograma) {
                    campoPerfil(ficha, { ficha = it }, "Número de Ficha")
                    Spacer(modifier = Modifier.height(12.dp))
                    campoPerfil(programa, { programa = it }, "Programa de Formación")
                    Spacer(modifier = Modifier.height(12.dp))
                }

                EtiquetaPerfil("Seguridad")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Nueva contraseña (opcional)", color = colors.textSecondary) },                    modifier = Modifier.fillMaxWidth().campoVisible(),
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
                    colors = campoPerfilColors(),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirmar contraseña", color = colors.textSecondary) },
                    modifier = Modifier.fillMaxWidth().campoVisible(),
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = campoPerfilColors(),
                    shape = RoundedCornerShape(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

                if (errorMensaje != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(errorMensaje!!, color = RojoError, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
                }

                Spacer(modifier = Modifier.height(24.dp))

                // activo, envía PUT /my-profile y avisa al terminar.
                fun guardarPerfil(codigo: String?) {
                    if (guardando) return
                    if (password != confirmPassword) {
                        errorMensaje = "Las contraseñas no coinciden."
                        return
                    }
                    if (password.isNotBlank() && password.length < 8) {
                        errorMensaje = "La contraseña debe tener al menos 8 caracteres."
                        return
                    }
                    if (identificacion.trim().isBlank() || nombres.trim().isBlank() || apellidos.trim().isBlank()) {
                        errorMensaje = "Identificación, nombres y apellidos son obligatorios."
                        return
                    }
                    val correoLimpio = correo.trim().lowercase()
                    if (correoLimpio.isBlank()) {
                        errorMensaje = "El correo es obligatorio."
                        return
                    }
                    val dominiosOk = listOf("@gmail.com", "@hotmail.com", "@outlook.com", "@soy.sena.edu.co")
                    if (dominiosOk.none { correoLimpio.endsWith(it) }) {
                        errorMensaje = "El correo debe ser @gmail.com, @hotmail.com, @outlook.com o @soy.sena.edu.co."
                        return
                    }
                    // Con ficha oculta (instructor/admin) se conserva la del servidor:
                    // enviar null la borraría en el backend.
                    val fichaNum = if (mostrarFichaPrograma) ficha.trim().toIntOrNull() else usuario.user_coursenumber
                    if (mostrarFichaPrograma && fichaNum == null) {
                        errorMensaje = "El número de ficha debe ser numérico."
                        return
                    }
                    val fichaFinal = fichaNum
                    val programaFinal = if (mostrarFichaPrograma) programa.trim() else usuario.user_program
                    errorMensaje = null
                    guardando = true
                    scope.launch {
                        try {
                            val codigoLimpio = codigo?.ifBlank { null }
                            val perfilActualizado = if (fotoBitmap != null) {
                                if (password.isNotBlank()) {
                                    RepositorioUsuarios().updateMyProfile(
                                        GestorSesion.token!!,
                                        PeticionActualizarPerfil(
                                            user_identification = identificacion.trim(),
                                            user_name = nombres.trim(),
                                            user_lastname = apellidos.trim(),
                                            user_email = correoLimpio,
                                            user_password = password,
                                            user_coursenumber = fichaFinal,
                                            user_program = programaFinal,
                                            two_factor_code = codigoLimpio
                                        )
                                    )
                                }
                                val bytes = bitmapAJpeg(fotoBitmap!!)
                                val parteFoto = okhttp3.MultipartBody.Part.createFormData(
                                    "image", "perfil.jpg", bytes.toRequestBody("image/jpeg".toMediaType())
                                )
                                RepositorioUsuarios().actualizarConFoto(
                                    GestorSesion.token!!,
                                    parteFoto,
                                    identificacion.trim(),
                                    nombres.trim(),
                                    apellidos.trim(),
                                    correoLimpio,
                                    fichaFinal,
                                    programaFinal,
                                    codigoLimpio
                                )
                            } else {
                                RepositorioUsuarios().updateMyProfile(
                                    GestorSesion.token!!,
                                    PeticionActualizarPerfil(
                                        user_identification = identificacion.trim(),
                                        user_name = nombres.trim(),
                                        user_lastname = apellidos.trim(),
                                        user_email = correoLimpio,
                                        user_password = password.ifBlank { null },
                                        user_coursenumber = fichaFinal,
                                        user_program = programaFinal,
                                        two_factor_code = codigoLimpio
                                    )
                                )
                            }
                            GestorSesion.savePhoto(perfilActualizado.profile_photo_path)
                            guardando = false
                            guardado = true
                            codigoClave = ""
                        } catch (e: retrofit2.HttpException) {
                            guardando = false
                            val detalle = com.example.sennaccess.comun.detalleHttp(e)
                            if (e.code() == 422 && detalle.contains("código", ignoreCase = true)) {
                                pedirCodigoClave = true
                                errorCodigoClave = detalle
                            } else {
                                errorMensaje = detalle
                            }
                        } catch (e: Exception) {
                            guardando = false
                            errorMensaje = "Fallo de conexión: ${e.message ?: e.javaClass.simpleName}"
                        }
                    }
                }
                Button(
                    onClick = { guardarPerfil(codigoClave.ifBlank { null }) },
                    modifier = Modifier.fillMaxWidth().height(52.dp).escalaPresion(pressedScale = 0.97f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = Color.Black)
                ) { Text(if (guardando) "GUARDANDO..." else "GUARDAR CAMBIOS", fontWeight = FontWeight.Bold, fontSize = 15.sp) }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, colors.textSecondary)
                ) { Text("CANCELAR", color = colors.textSecondary, fontWeight = FontWeight.Bold) }

                if (pedirCodigoClave) {
                    var codigoTmp by remember { mutableStateOf(codigoClave) }
                    AlertDialog(
                        onDismissRequest = { pedirCodigoClave = false },
                        containerColor = colors.cardBackground.copy(alpha = 0.98f),
                        shape = RoundedCornerShape(20.dp),
                        icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = verdeMarca(), modifier = Modifier.size(36.dp)) },
                        title = { Text("Confirma tu cambio de clave", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
                        text = {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()) {
                                Text(
                                    "Enviamos un código de 6 dígitos a tu correo. Escríbelo para autorizar la nueva contraseña.",
                                    color = colors.textSecondary,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = codigoTmp,
                                    onValueChange = { v -> if (v.length <= 6 && v.all { it.isDigit() }) codigoTmp = v },
                                    label = { Text("Código de 6 dígitos") },
                                    modifier = Modifier.fillMaxWidth().campoVisible(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(28.dp)
                                )
                                if (errorCodigoClave != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(errorCodigoClave!!, color = RojoError, fontSize = 12.sp)
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (codigoTmp.length != 6) {
                                        errorCodigoClave = "Escribe el código de 6 dígitos."
                                        return@TextButton
                                    }
                                    codigoClave = codigoTmp
                                    pedirCodigoClave = false
                                    guardarPerfil(codigoTmp)
                                }
                            ) { Text("CONFIRMAR", fontWeight = FontWeight.Bold, color = verdeMarca()) }
                        },
                        dismissButton = {
                            TextButton(onClick = { pedirCodigoClave = false; codigoTmp = "" }) {
                                Text("CANCELAR", color = colors.textSecondary)
                            }
                        }
                    )
                }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (guardado) {
        AlertDialog(
            onDismissRequest = { guardado = false },
            containerColor = colors.cardBackground.copy(alpha = 0.98f),
            shape = RoundedCornerShape(24.dp),
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = verdeMarca(), modifier = Modifier.size(40.dp)) },
            title = { Text("Perfil actualizado", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Tus datos se guardaron correctamente.", color = colors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = { guardado = false; onGuardado() },
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = Color.Black),
                    shape = RoundedCornerShape(28.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                ) { Text("Okey", fontWeight = FontWeight.ExtraBold) }
            }
        )
    }
}

@Composable
private fun campoPerfil(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = ColoresAppLocal.current.textSecondary) },
        modifier = Modifier.fillMaxWidth().campoVisible(),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = campoPerfilColors()
    )
}

// Etiqueta de sección en editar perfil: eyebrow verde + jerarquía.
@Composable
private fun EtiquetaPerfil(texto: String) {
    Text(
        text = texto.uppercase(),
        color = verdeMarca(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.6.sp,
        modifier = Modifier.fillMaxWidth()
    )
}

// Campo bloqueado de solo lectura (ficha/programa en el perfil propio).
// Muestra el valor del servidor sin permitir edición: fix grave de seguridad.
@Composable
private fun campoLectura(label: String, valor: String) {
    OutlinedTextField(
        value = valor,
        onValueChange = { },
        label = { Text(label, color = ColoresAppLocal.current.textSecondary) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        readOnly = true,
        enabled = false,
        shape = RoundedCornerShape(16.dp),
        colors = campoPerfilColors()
    )
}

@Composable
private fun campoPerfilColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = verdeMarca(),
    unfocusedBorderColor = ColoresAppLocal.current.textSecondary.copy(alpha = 0.5f),
    focusedLabelColor = verdeMarca(),
    unfocusedLabelColor = ColoresAppLocal.current.textSecondary,
    cursorColor = verdeMarca(),
    focusedTextColor = ColoresAppLocal.current.textPrimary,
    unfocusedTextColor = ColoresAppLocal.current.textPrimary
)

private fun bitmapAJpeg(bitmap: Bitmap, calidad: Int = 85): ByteArray {
    val salida = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, calidad, salida)
    return salida.toByteArray()
}

private fun decodificarImagen(contexto: Context, uri: Uri, maxLado: Int = 2048): Bitmap? {
    return try {
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            android.graphics.ImageDecoder.decodeBitmap(
                android.graphics.ImageDecoder.createSource(contexto.contentResolver, uri)
            ) { decoder, info, _ ->
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
    var preview by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uriOriginal) {
        imagen = decodificarImagen(contexto, uriOriginal)
    }

    val ladoDp = 300.dp
    val ladoPx = with(LocalDensity.current) { ladoDp.toPx() }
    val centro = ladoPx / 2f
    val img = imagen
    val escalaTotal = if (img != null) (ladoPx / min(img.width, img.height).toFloat()) * escala else 1f
    val limiteX = if (img != null) ((img.width * escalaTotal - ladoPx) / 2f).coerceAtLeast(0f) else 0f
    val limiteY = if (img != null) ((img.height * escalaTotal - ladoPx) / 2f).coerceAtLeast(0f) else 0f
    val dx = desplazamiento.x.coerceIn(-limiteX, limiteX)
    val dy = desplazamiento.y.coerceIn(-limiteY, limiteY)
    val verdeRecorte = verdeMarca()

    fun generarRecorte(): Bitmap? {
        val actual = imagen ?: return null
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
        return salida
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
                if (preview == null) "RECORTAR FOTO" else "¿CÓMO SE VE?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                if (preview == null) "Mueve con el dedo y usa pinza para el zoom" else "Así quedará tu foto de perfil",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (preview == null) {
                Box(modifier = Modifier.size(ladoDp)) {
                    if (img == null) {
                        CircularProgressIndicator(color = verdeMarca(), modifier = Modifier.align(Alignment.Center))
                    } else {
                        Canvas(
                            modifier = Modifier
                                .size(ladoDp)
                                .clipToBounds()
                                .pointerInput(img) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        escala = (escala * zoom).coerceIn(1f, 5f)
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
                        Canvas(modifier = Modifier.size(ladoDp)) {
                            val ruta = Path().apply {
                                fillType = PathFillType.EvenOdd
                                addRect(Rect(Offset.Zero, size))
                                addOval(Rect(Offset(centro, centro), centro))
                            }
                            drawPath(ruta, Color.Black.copy(alpha = 0.65f))
                            drawCircle(verdeRecorte, style = Stroke(width = 3.dp.toPx()))
                            val tercio = size.width / 3f
                            val guia = Color.White.copy(alpha = 0.25f)
                            drawLine(guia, Offset(tercio, 0f), Offset(tercio, size.height), strokeWidth = 1f)
                            drawLine(guia, Offset(tercio * 2f, 0f), Offset(tercio * 2f, size.height), strokeWidth = 1f)
                            drawLine(guia, Offset(0f, tercio), Offset(size.width, tercio), strokeWidth = 1f)
                            drawLine(guia, Offset(0f, tercio * 2f), Offset(size.width, tercio * 2f), strokeWidth = 1f)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ZoomOut, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                    Slider(
                        value = escala,
                        onValueChange = { escala = it.coerceIn(1f, 5f) },
                        valueRange = 1f..5f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = verdeRecorte,
                            activeTrackColor = verdeRecorte,
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                        )
                    )
                    Icon(Icons.Default.ZoomIn, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { preview = generarRecorte() },
                    enabled = img != null,
                    modifier = Modifier.fillMaxWidth().height(50.dp).escalaPresion(pressedScale = 0.97f),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = Color.Black)
                ) { Text("VER VISTA PREVIA", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            } else {
                val vistaPrevia = preview
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .border(3.dp, verdeRecorte, CircleShape)
                ) {
                    if (vistaPrevia != null) {
                        Image(
                            bitmap = vistaPrevia.asImageBitmap(),
                            contentDescription = "Vista previa del recorte",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = { vistaPrevia?.let(onRecortado) },
                    modifier = Modifier.fillMaxWidth().height(50.dp).escalaPresion(pressedScale = 0.97f),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("USAR ESTA FOTO", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { preview = null },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                ) { Text("REAJUSTAR", color = Color.White, fontWeight = FontWeight.Bold) }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
            ) { Text("CANCELAR", color = Color.White, fontWeight = FontWeight.Bold) }
        }
    }
}
