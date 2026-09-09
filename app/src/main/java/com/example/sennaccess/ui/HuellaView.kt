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
import androidx.fragment.app.FragmentActivity
import com.example.sennaccess.data.AuthRepository
import com.example.sennaccess.data.HuellaCredentialStore
import com.example.sennaccess.data.SessionManager
import com.example.sennaccess.ui.theme.ErrorRed
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
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
    var password by remember { mutableStateOf("") }
    var verPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = GlassCornerRadius)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Fingerprint, null, tint = SenaGreen, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Mi Huella", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (registrada) SenaGreen.copy(alpha = 0.15f) else colors.borderLight,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    if (registrada) "Activa" else "Inactiva",
                    color = if (registrada) SenaGreen else colors.textSecondary,
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
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (verPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { verPassword = !verPassword }) {
                        Icon(if (verPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (ocupado) return@Button
                    errorMensaje = null
                    mensaje = null
                    val correo = SessionManager.userEmail
                    when {
                        correo == null -> errorMensaje = "Inicia sesión para registrar tu huella."
                        password.isBlank() -> errorMensaje = "Escribe tu contraseña."
                        !BiometricAuth.isAvailable(context) ->
                            errorMensaje = "Tu dispositivo no tiene huella configurada."
                        else -> {
                            ocupado = true
                            scope.launch {
                                try {
                                    AuthRepository().login(correo, password.trim())
                                } catch (e: retrofit2.HttpException) {
                                    ocupado = false
                                    errorMensaje = if (e.code() == 401) "Contraseña incorrecta" else "Error ${e.code()}"
                                    return@launch
                                } catch (e: Exception) {
                                    ocupado = false
                                    errorMensaje = "Error de conexión"
                                    return@launch
                                }
                                try {
                                    val activity = context as? FragmentActivity
                                        ?: throw IllegalStateException("Sin actividad")
                                    val cipher = HuellaCredentialStore.prepararCifrado()
                                    BiometricAuth.authenticate(
                                        activity = activity,
                                        title = "Registra tu huella",
                                        subtitle = "Toca el sensor",
                                        cryptoObject = BiometricPrompt.CryptoObject(cipher),
                                        onSuccess = { result ->
                                            ocupado = false
                                            try {
                                                HuellaCredentialStore.guardar(
                                                    context,
                                                    result.cryptoObject!!.cipher!!,
                                                    correo,
                                                    password.trim()
                                                )
                                                mensaje = "Huella registrada."
                                                password = ""
                                                registrada = true
                                            } catch (e: Exception) {
                                                errorMensaje = "No se pudo guardar la huella."
                                            }
                                        },
                                        onError = { motivo ->
                                            ocupado = false
                                            if (!motivo.contains("cancel", ignoreCase = true)) {
                                                errorMensaje = motivo
                                            }
                                        }
                                    )
                                } catch (e: Throwable) {
                                    ocupado = false
                                    errorMensaje = "Error al preparar la llave de seguridad."
                                }
                            }
                        }
                    }
                },
                enabled = !ocupado,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black)
            ) { Text(if (ocupado) "VERIFICANDO..." else "REGISTRAR", fontWeight = FontWeight.Bold) }
        }

        if (mensaje != null) {
            Spacer(Modifier.height(6.dp))
            Surface(color = SenaGreen.copy(alpha = 0.10f), shape = RoundedCornerShape(8.dp)) {
                Text(mensaje!!, color = SenaGreen, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
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
            shape = RoundedCornerShape(24.dp),
            icon = { Icon(Icons.Default.Delete, null, tint = ErrorRed, modifier = Modifier.size(36.dp)) },
            title = { Text("¿Eliminar huella?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Ya no podrás usar el acceso con huella en este teléfono.", color = colors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmarBorrar = false
                        HuellaCredentialStore.borrar(context)
                        registrada = false
                        mensaje = "Huella eliminada."
                        errorMensaje = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
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
