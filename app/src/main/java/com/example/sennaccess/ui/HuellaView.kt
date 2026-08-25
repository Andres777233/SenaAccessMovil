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

    // Estado de la huella en este dispositivo (se consulta al entrar a la vista).
    var registrada by remember { mutableStateOf(HuellaCredentialStore.hayGuardada(context)) }
    var ocupado by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var errorMensaje by remember { mutableStateOf<String?>(null) }
    // Abre el diálogo de confirmación de borrado.
    var confirmarBorrar by remember { mutableStateOf(false) }
    // Contraseña que confirma el usuario para registrar la huella desde el perfil.
    var password by remember { mutableStateOf("") }
    var verPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = GlassCornerRadius)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = SenaGreen, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Mi Huella", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Ingresa a la app solo con tu huella. Tus credenciales se guardan cifradas en este teléfono y se desbloquean únicamente con tu huella.",
            color = colors.textSecondary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (registrada) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = SenaGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Huella registrada en este dispositivo",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { if (!ocupado) confirmarBorrar = true },
                enabled = !ocupado,
                modifier = Modifier.fillMaxWidth().height(45.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.Black)
            ) { Text("ELIMINAR HUELLA", fontWeight = FontWeight.Bold) }
        } else {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Confirma tu contraseña") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation =
                    if (verPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { verPassword = !verPassword }) {
                        Icon(
                            imageVector = if (verPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    if (ocupado) return@Button
                    errorMensaje = null
                    mensaje = null
                    val correo = SessionManager.userEmail
                    when {
                        correo == null ->
                            errorMensaje = "Inicia sesión para registrar tu huella."
                        password.isBlank() ->
                            errorMensaje = "Escribe tu contraseña para registrar la huella."
                        !BiometricAuth.isAvailable(context) ->
                            errorMensaje = "Tu dispositivo no tiene huella configurada. Regístrala en Ajustes del sistema."
                        else -> {
                            ocupado = true
                            scope.launch {
                                // Valida la contraseña contra el backend antes de guardarla:
                                // así el botón INGRESAR CON HUELLA funcionará con seguridad.
                                try {
                                    AuthRepository().login(correo, password.trim())
                                } catch (e: retrofit2.HttpException) {
                                    ocupado = false
                                    errorMensaje = if (e.code() == 401) "La contraseña no es correcta." else "Error ${e.code()} al validar tu contraseña."
                                    return@launch
                                } catch (e: Exception) {
                                    ocupado = false
                                    errorMensaje = "No se pudo conectar al servidor."
                                    return@launch
                                }
                                // Con la contraseña validada, pide la huella al sistema y
                                // guarda las credenciales cifradas con la llave del Keystore.
                                try {
                                    val activity = context as? FragmentActivity
                                        ?: throw IllegalStateException("Sin actividad")
                                    val cipher = HuellaCredentialStore.prepararCifrado()
                                    BiometricAuth.authenticate(
                                        activity = activity,
                                        title = "Registra tu huella",
                                        subtitle = "Toca el sensor para proteger tus credenciales",
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
                                                mensaje = "Huella registrada correctamente."
                                                password = ""
                                                registrada = true
                                            } catch (e: Exception) {
                                                errorMensaje = "No se pudo guardar la huella. Intenta de nuevo."
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
                                    errorMensaje = "No se pudo preparar la llave de seguridad. Inicia sesión en la app y vuelve a intentar."
                                }
                            }
                        }
                    }
                },
                enabled = !ocupado,
                modifier = Modifier.fillMaxWidth().height(45.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black)
            ) { Text(if (ocupado) "VERIFICANDO..." else "REGISTRAR HUELLA", fontWeight = FontWeight.Bold) }
        }

        if (mensaje != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(mensaje!!, color = SenaGreen, fontSize = 12.sp)
        }
        if (errorMensaje != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(errorMensaje!!, color = ErrorRed, fontSize = 12.sp)
        }
    }

    // Confirmación antes de eliminar la huella registrada en el dispositivo.
    if (confirmarBorrar) {
        AlertDialog(
            onDismissRequest = { confirmarBorrar = false },
            containerColor = colors.cardBackground.copy(alpha = 0.98f),
            shape = RoundedCornerShape(24.dp),
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(40.dp)) },
            title = { Text("¿Eliminar huella?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Ya no podrás usar INGRESAR CON HUELLA en este teléfono hasta volver a registrarla.",
                    color = colors.textSecondary
                )
            },
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
