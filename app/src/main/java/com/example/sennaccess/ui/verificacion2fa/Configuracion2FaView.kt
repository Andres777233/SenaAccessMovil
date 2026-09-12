// Sub-pantalla "Verificación en dos pasos" del perfil: muestra el estado (activa/
// inactiva), explica el método (aprobar desde otro dispositivo + código al correo)
// y permite activar o desactivar. Desactivar exige la contraseña actual.
package com.example.sennaccess.ui.verificacion2fa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.data.SessionManager
import com.example.sennaccess.data.TwoFactorRepository
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import com.example.sennaccess.ui.ios.GlassCornerRadius
import com.example.sennaccess.ui.ios.IosCollapsibleHeader
import com.example.sennaccess.ui.ios.glassSurface
import com.google.gson.JsonParser
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

// Extrae el mensaje del servidor (misma utilidad que en Verificacion2FaScreen).
private fun mensajeHttp(e: Throwable): String {
    val http = e as? retrofit2.HttpException
    if (http != null) {
        return try {
            val body = http.response()?.errorBody()?.string()
            val msj = JsonParser.parseString(body).asJsonObject["message"]?.asString
            msj ?: "Error ${http.code()}"
        } catch (_: Exception) {
            "Error ${http.code()}"
        }
    }
    return "No se pudo conectar al servidor"
}

@Composable
fun Configuracion2FaView(
    onBack: () -> Unit
) {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    val repo = remember { TwoFactorRepository() }
    val token = remember { SessionManager.token }

    var activo by remember { mutableStateOf<Boolean?>(null) }
    var trabajando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }

    // Diálogo de desactivación: pide la contraseña actual.
    var pedirPass by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var errorPass by remember { mutableStateOf<String?>(null) }
    var desactivando by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (token != null) {
            try {
                activo = repo.estadoConfig(token).two_factor_enabled == true
            } catch (e: Exception) {
                mensaje = mensajeHttp(e)
                activo = false
            }
        }
    }

    fun listo(nuevoActivo: Boolean, ok: String) {
        activo = nuevoActivo
        mensaje = ok
        trabajando = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.textPrimary)
        }
        Spacer(Modifier.height(8.dp))

        IosCollapsibleHeader(
            title = "Verificación en dos pasos",
            subtitle = "Seguridad de tu cuenta",
            scrollOffset = 0f
        )

        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassSurface(cornerRadius = GlassCornerRadius)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = if (activo == true) SenaGreen else colors.textSecondary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            when (activo) {
                null -> {
                    CircularProgressIndicator(Modifier.size(22.dp), color = SenaGreen, strokeWidth = 2.dp)
                    Spacer(Modifier.height(8.dp))
                    Text("Consultando el estado...", color = colors.textSecondary, fontSize = 13.sp)
                }
                true -> {
                    Text("ACTIVADA", color = SenaGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Cada inicio de sesión pedirá confirmación: apruebas el acceso desde este dispositivo (¿Soy yo?) o escribes el código de 6 dígitos que llega a tu correo.",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(18.dp))
                    OutlinedButton(
                        onClick = { pedirPass = true },
                        enabled = !trabajando,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Lock, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("DESACTIVAR", fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    Text("INACTIVA", color = colors.textSecondary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Si la activas, un inicio de sesión tuyo desde otro equipo solo se completará si lo apruebas desde este dispositivo o con el código enviado a tu correo.",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = {
                            if (token == null) {
                                mensaje = "No hay sesión activa."
                                return@Button
                            }
                            trabajando = true
                            mensaje = null
                            scope.launch {
                                try {
                                    val resp = repo.activar(token)
                                    listo(resp.two_factor_enabled == true, resp.message ?: "Verificación activada.")
                                } catch (e: Exception) {
                                    mensaje = mensajeHttp(e)
                                    trabajando = false
                                }
                            }
                        },
                        enabled = !trabajando,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (trabajando) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("ACTIVAR VERIFICACIÓN EN DOS PASOS", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            mensaje?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = SenaGreen, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    // Diálogo para desactivar: exige la contraseña actual de la cuenta.
    if (pedirPass) {
        AlertDialog(
            onDismissRequest = { pedirPass = false; password = "" },
            containerColor = colors.cardBackground.copy(alpha = 0.98f),
            shape = RoundedCornerShape(20.dp),
            icon = { Icon(Icons.Default.Shield, contentDescription = null, tint = SenaGreen) },
            title = { Text("Desactivar verificación en dos pasos", color = colors.textPrimary) },
            text = {
                Column {
                    Text(
                        "Por seguridad, confirma tu contraseña actual.",
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña actual") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp)
                    )
                    errorPass?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, color = Color(0xFFD32F2F), fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "La contraseña nunca se almacena; solo se envía cifrada contra el servidor para validar el cambio.",
                        color = colors.textSecondary.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !desactivando,
                    onClick = {
                        if (token == null) {
                            errorPass = "No hay sesión activa."
                            return@TextButton
                        }
                        if (password.isBlank()) {
                            errorPass = "Escribe tu contraseña."
                            return@TextButton
                        }
                        desactivando = true
                        errorPass = null
                        scope.launch {
                            try {
                                val resp = repo.desactivar(token, password)
                                pedirPass = false
                                password = ""
                                listo(resp.two_factor_enabled == true, resp.message ?: "Verificación desactivada.")
                            } catch (e: Exception) {
                                errorPass = mensajeHttp(e)
                            }
                            desactivando = false
                        }
                    }
                ) {
                    if (desactivando) {
                        CircularProgressIndicator(Modifier.size(14.dp), color = SenaGreen, strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("Confirmar", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                }
            },
            dismissButton = {
                TextButton(onClick = { pedirPass = false; password = "" }) {
                    Text("Cancelar", color = colors.textSecondary)
                }
            }
        )
    }
}