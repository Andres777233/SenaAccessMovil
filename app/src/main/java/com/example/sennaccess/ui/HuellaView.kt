package com.example.sennaccess.ui

// Sección MI HUELLA compartida por los perfiles de Aprendiz, Instructor y Admin:
// gestiona la huella registrada EN ESTE DISPOSITIVO para el botón INGRESAR CON
// HUELLA del login. Las credenciales se guardan cifradas con una llave del
// Keystore que solo se desbloquea con la huella (HuellaCredentialStore); si aún
// no existe, se registra confirmando la contraseña y verificando el dedo.

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.data.HuellaCredentialStore
import com.example.sennaccess.data.SessionManager
import com.example.sennaccess.data.UsuarioRepository
import com.example.sennaccess.ui.campoVisible
import com.example.sennaccess.ui.theme.ErrorRed
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import com.example.sennaccess.ui.theme.verdeMarca
import com.example.sennaccess.ui.ios.GlassCornerRadius
import com.example.sennaccess.ui.ios.glassSurface
import kotlinx.coroutines.launch

@Composable
fun MiHuellaSection() {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var registrada by remember { mutableStateOf(HuellaCredentialStore.hayGuardada(context)) }
    var ocupado by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var errorMensaje by remember { mutableStateOf<String?>(null) }
    var confirmarBorrar by remember { mutableStateOf(false) }
    // Doble contraseña actual: solo si ambas coinciden se habilita el registro.
    var password by remember { mutableStateOf("") }
    var confirmarPassword by remember { mutableStateOf("") }
    var verPassword by remember { mutableStateOf(false) }
    var verConfirmar by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = GlassCornerRadius)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Fingerprint, null, tint = verdeMarca(), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Mi Huella", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = if (registrada) SenaGreen.copy(alpha = 0.15f) else colors.borderLight,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    if (registrada) "Activa" else "Inactiva",
                    color = if (registrada) verdeMarca() else colors.textSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            if (registrada) "Ingresa a la app con tu huella" else "Registra tu huella para ingresar sin contraseña",
            color = colors.textSecondary,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(12.dp))

        if (registrada) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Huella registrada en este dispositivo",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = { if (!ocupado) confirmarBorrar = true },
                    enabled = !ocupado,
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Eliminar", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        } else {
            // Paso 1: contraseña actual + confirmación (deben coincidir).
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña actual") },
                modifier = Modifier.fillMaxWidth().campoVisible(),
                singleLine = true,
                visualTransformation = if (verPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { verPassword = !verPassword }) {
                        Icon(if (verPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = confirmarPassword,
                onValueChange = { confirmarPassword = it },
                label = { Text("Confirmar contraseña actual") },
                modifier = Modifier.fillMaxWidth().campoVisible(),
                singleLine = true,
                visualTransformation = if (verConfirmar) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { verConfirmar = !verConfirmar }) {
                        Icon(if (verConfirmar) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (ocupado) return@Button
                    errorMensaje = null
                    mensaje = null
                    val correo = SessionManager.userEmail
                    when {
                        correo == null -> errorMensaje = "Inicia sesión para registrar tu huella."
                        password.isBlank() -> errorMensaje = "Escribe tu contraseña actual."
                        // Sin confirmación coincidente no se abre el registro biométrico.
                        password != confirmarPassword -> errorMensaje = "Las contraseñas no coinciden."
                        // Para cifrar se exige biometría fuerte: el PIN no abre el
                        // CryptoObject y antes eso terminaba en "Error al probar la llave".
                        !BiometricAuth.isAvailable(context, forCrypto = true) ->
                            errorMensaje = "Tu dispositivo no tiene huella configurada."
                        else -> {
                            ocupado = true
                            scope.launch {
                                try {
                                    // La contraseña se confirma con la sesión (token), no con un
                                    // login nuevo: así no se dispara el 2FA ni llegan correos y
                                    // funciona aunque la verificación en dos pasos esté activa.
                                    val token = SessionManager.token
                                    if (token == null) {
                                        ocupado = false
                                        errorMensaje = "Sesión vencida. Vuelve a ingresar y registra tu huella."
                                        return@launch
                                    }
                                    UsuarioRepository().verificarPassword(token, password)
                                } catch (e: retrofit2.HttpException) {
                                    ocupado = false
                                    errorMensaje = when (e.code()) {
                                        422 -> "Contraseña incorrecta"
                                        401 -> "Sesión vencida. Vuelve a ingresar y registra tu huella."
                                        else -> "Error ${e.code()}"
                                    }
                                    return@launch
                                } catch (e: Exception) {
                                    ocupado = false
                                    errorMensaje = "Error de conexión"
                                    return@launch
                                }
                                try {
                                    // Sin cast directo: resuelve la actividad aunque el contexto
                                    // venga envuelto por Compose/temas.
                                    val activity = BiometricAuth.activityDe(context)
                                        ?: throw IllegalStateException("Sin actividad")
                                    // Llave segura: si la anterior quedó invalidada al borrar
                                    // la huella, se regenera en vez de fallar siempre.
                                    val cipher = HuellaCredentialStore.prepararCifradoSeguro()
                                    BiometricAuth.authenticate(
                                        activity = activity,
                                        title = "Registra tu huella",
                                        subtitle = "Toca el sensor",
                                        cryptoObject = BiometricPrompt.CryptoObject(cipher),
                                        onSuccess = { result ->
                                            ocupado = false
                                            try {
                                                val seguro = result.cryptoObject?.cipher
                                                if (seguro == null) {
                                                    errorMensaje = "No se pudo activar la huella. Inténtalo de nuevo."
                                                    return@authenticate
                                                }
                                                HuellaCredentialStore.guardar(
                                                    context,
                                                    seguro,
                                                    correo.trim(),
                                                    password
                                                )
                                                mensaje = "Huella registrada."
                                                password = ""
                                                confirmarPassword = ""
                                                registrada = true
                                            } catch (e: Exception) {
                                                errorMensaje = "No se pudo guardar la huella."
                                            }
                                        },
                                        onError = { motivo ->
                                            ocupado = false
                                            errorMensaje = motivo
                                        },
                                        onFailed = { aviso ->
                                            errorMensaje = aviso
                                        }
                                    )
                                } catch (e: Throwable) {
                                    ocupado = false
                                    // Último recurso: purga la llave corrupta para que el
                                    // siguiente intento parta de cero en vez de repetir el error.
                                    HuellaCredentialStore.borrarLlave()
                                    errorMensaje = "Se renovó tu llave de seguridad. Toca REGISTRAR de nuevo y pon tu huella."
                                }
                            }
                        }
                    }
                },
                enabled = !ocupado,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black)
            ) { Text(if (ocupado) "VERIFICANDO..." else "REGISTRAR", fontWeight = FontWeight.Bold) }
        }

        if (mensaje != null) {
            Spacer(Modifier.height(6.dp))
            Surface(color = verdeMarca().copy(alpha = 0.10f), shape = RoundedCornerShape(8.dp)) {
                Text(mensaje!!, color = verdeMarca(), fontSize = 12.sp, modifier = Modifier.padding(8.dp))
            }
        }
        if (errorMensaje != null) {
            Spacer(Modifier.height(6.dp))
            Surface(color = ErrorRed.copy(alpha = 0.10f), shape = RoundedCornerShape(8.dp)) {
                Text(errorMensaje!!, color = ErrorRed, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
            }
        }
    }

    if (confirmarBorrar) {
        AlertDialog(
            onDismissRequest = { confirmarBorrar = false },
            containerColor = colors.cardBackground.copy(alpha = 0.98f),
            shape = RoundedCornerShape(28.dp),
            icon = { Icon(Icons.Default.Delete, null, tint = ErrorRed, modifier = Modifier.size(36.dp)) },
            title = { Text("¿Eliminar huella?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Ya no podrás usar el acceso con huella en este teléfono.", color = colors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmarBorrar = false
                        // Borrado total (credenciales + llave): el próximo registro genera
                        // una llave nueva y no reutiliza la invalidada.
                        HuellaCredentialStore.borrarTodo(context)
                        registrada = false
                        mensaje = "Huella eliminada."
                        errorMensaje = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.Black),
                    shape = RoundedCornerShape(28.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                ) { Text("ELIMINAR", fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarBorrar = false }) {
                    Text("CANCELAR", color = colors.textSecondary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
