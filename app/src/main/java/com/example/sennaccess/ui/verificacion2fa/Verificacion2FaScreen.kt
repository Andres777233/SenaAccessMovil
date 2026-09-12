// Pantalla de segundo factor que aparece tras un login cuando la cuenta tiene
// "Verificación en dos pasos" activada. Ofrece dos caminos:
//  1) Código de 6 dígitos enviado al correo (fallback).
//  2) Aprobación "¿Eres tú?" desde otro dispositivo con sesión: aqui se hace
//     polling del estado del reto hasta que el otro dispositivo aprueba/deniega.
package com.example.sennaccess.ui.verificacion2fa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.data.LoginResponse
import com.example.sennaccess.data.SessionManager
import com.example.sennaccess.data.TwoFactorRepository
import com.example.sennaccess.data.Verificacion2FaEstado
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import com.example.sennaccess.ui.ios.GlowSpheres
import com.example.sennaccess.ui.ios.IosGlassCard
import com.google.gson.JsonParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Guarda la sesión completa cuando el 2FA se resuelve con un LoginResponse (código).
fun guardarSesionDesde2Fa(response: LoginResponse) {
    SessionManager.saveSession(
        response.access_token,
        response.user?.id_usuario,
        response.user?.user_name,
        response.user?.user_email,
        response.role,
        response.user?.email_verified_at != null
    )
    SessionManager.savePhoto(response.user?.profile_photo_path)
}

// Guarda la sesión cuando el reto ya quedó aprobado y el backend entrega el token.
fun guardarSesionDesde2Fa(estado: Verificacion2FaEstado) {
    SessionManager.saveSession(
        estado.access_token,
        estado.user?.id_usuario,
        estado.user?.user_name,
        estado.user?.user_email,
        estado.role,
        estado.user?.email_verified_at != null
    )
    SessionManager.savePhoto(estado.user?.profile_photo_path)
}

// Extrae el mensaje real del servidor (HTTP 4xx/5xx) o un genérico si no hay red.
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
fun Verificacion2FaScreen(
    challengeId: String,
    isDark: Boolean,
    onCancel: () -> Unit,
    onLoginSuccess: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val repo = remember { TwoFactorRepository() }
    val scope = rememberCoroutineScope()

    // Código de 6 dígitos del correo (fallback).
    var codigo by remember { mutableStateOf("") }
    var enviandoCodigo by remember { mutableStateOf(false) }
    var errorCodigo by remember { mutableStateOf<String?>(null) }

    // Mensajes del modo "aprobar desde otro dispositivo".
    var estadoMensaje by remember { mutableStateOf<String?>(null) }
    var resuelto by remember { mutableStateOf(false) }
    var aprobado by remember { mutableStateOf(false) }

    // Polling cada 4 s: mientras el otro dispositivo no apruebe/deniegue, se espera.
    LaunchedEffect(challengeId) {
        while (!resuelto) {
            delay(4000)
            if (resuelto) break
            try {
                val estado = repo.estadoChallenge(challengeId)
                when (estado.estado) {
                    "aprobado" -> {
                        guardarSesionDesde2Fa(estado)
                        resuelto = true
                        aprobado = true
                    }
                    "rechazado" -> {
                        estadoMensaje = "El intento fue rechazado en tu otro dispositivo."
                        resuelto = true
                    }
                    "expirado" -> {
                        estadoMensaje = "El intento de acceso expiró. Vuelve a iniciar sesión."
                        resuelto = true
                    }
                }
            } catch (_: Exception) {
                // Sin red: sigue reintentando silenciosamente en la siguiente vuelta.
            }
        }
    }

    // Al conseguir el token (vía código o aprobación), navega al dashboard.
    LaunchedEffect(aprobado) {
        if (aprobado) {
            onLoginSuccess(SessionManager.userRole ?: "")
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(vertical = 24.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        GlowSpheres(isDark = isDark)

        IosGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 22.dp, vertical = 28.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cabecera con candado verde.
                Box(
                    Modifier
                        .size(64.dp)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = SenaGreen,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    "Verificación en dos pasos",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Revisa tu correo con el código de 6 dígitos, o aprueba este intento desde tu otro dispositivo.",
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                // ---- Opción 1: código del correo (fallback) ----
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Código del correo", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = codigo,
                    onValueChange = { new ->
                        if (new.length <= 6 && new.all { it.isDigit() }) codigo = new
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Código de 6 dígitos") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp)
                )

                errorCodigo?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = Color(0xFFD32F2F), fontSize = 13.sp)
                }

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = {
                        val code = codigo.trim()
                        if (code.length != 6) {
                            errorCodigo = "Escribe el código de 6 dígitos."
                            return@Button
                        }
                        enviandoCodigo = true
                        errorCodigo = null
                        scope.launch {
                            try {
                                val resp = repo.validarCodigo(challengeId, code)
                                guardarSesionDesde2Fa(resp)
                                onLoginSuccess(resp.role ?: SessionManager.userRole ?: "")
                            } catch (e: Exception) {
                                errorCodigo = mensajeHttp(e)
                                enviandoCodigo = false
                            }
                        }
                    },
                    enabled = !enviandoCodigo,
                    colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (enviandoCodigo) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                        Spacer(Modifier.size(8.dp))
                    }
                    Text("VERIFICAR CÓDIGO", fontWeight = FontWeight.ExtraBold)
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = colors.borderLight)
                Spacer(Modifier.height(16.dp))

                // ---- Opción 2: aprobar desde otro dispositivo ----
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Smartphone, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Aprobar desde otro dispositivo", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                }

                Spacer(Modifier.height(10.dp))

                if (resuelto && estadoMensaje == null) {
                    Text("Acceso aprobado. Entrando...", color = SenaGreen, fontWeight = FontWeight.Bold)
                } else {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = SenaGreen, strokeWidth = 2.dp)
                        Spacer(Modifier.size(10.dp))
                        Text(
                            "Esperando tu aprobación en el otro dispositivo...",
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                estadoMensaje?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = Color(0xFFD32F2F), fontSize = 13.sp, textAlign = TextAlign.Center)
                }

                Spacer(Modifier.height(16.dp))

                TextButton(onClick = onCancel) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Otra vez", color = colors.textSecondary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}