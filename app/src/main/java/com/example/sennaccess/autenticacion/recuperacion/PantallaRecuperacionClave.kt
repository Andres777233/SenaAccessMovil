// Pantalla de recuperación de contraseña: permite solicitar el envío de un código
// de recuperación al correo (POST /api/forgot-password). Al enviarlo con éxito,
// navega a la pantalla de restablecimiento con onNavigateToReset; desde aquí
// también se regresa al login con onBackToLogin.
package com.example.sennaccess.autenticacion.recuperacion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.datos.repositorios.RepositorioAutenticacion
import com.example.sennaccess.comun.diseno.FondoAcceso
import com.example.sennaccess.comun.diseno.CampoAcceso
import com.example.sennaccess.comun.diseno.CajaError
import com.example.sennaccess.comun.diseno.superficiePlana
import com.example.sennaccess.comun.diseno.BotonPrimarioNeon
import com.example.sennaccess.comun.diseno.BotonCambiarTema
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import kotlinx.coroutines.launch

@Composable
fun PantallaRecuperacionClave(
    onBackToLogin: () -> Unit,
    onNavigateToReset: () -> Unit = {},
    isDark: Boolean = true,
    onToggleTheme: () -> Unit = {}
) {
    val colors = ColoresAppLocal.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var errorMensaje by remember { mutableStateOf<String?>(null) }
    var enviado by remember { mutableStateOf(false) }

    FondoAcceso(isDark = isDark) {
        BotonCambiarTema(
            isDark = isDark,
            onToggleTheme = onToggleTheme,
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(top = 64.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "RECUPERAR CLAVE",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.8.sp),
                color = verdeMarca(),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Recupera tu acceso",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Te enviamos un código de 6 dígitos a tu correo.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .superficiePlana(cornerRadius = 24.dp)
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    CampoAcceso(
                        value = email,
                        onValueChange = { email = it },
                        label = "Correo Electrónico",
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    if (errorMensaje != null) {
                        CajaError(
                            texto = errorMensaje!!,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    BotonPrimarioNeon(
                        text = "ENVIAR CÓDIGO",
                        icon = Icons.Default.Send,
                        loading = enviando,
                        onClick = {
                            if (enviando) return@BotonPrimarioNeon
                            if (email.isBlank()) {
                                errorMensaje = "Ingresa tu correo electrónico."
                                return@BotonPrimarioNeon
                            }
                            errorMensaje = null
                            enviando = true
                            scope.launch {
                                try {
                                    RepositorioAutenticacion().forgotPassword(email.trim())
                                    enviando = false
                                    enviado = true
                                } catch (e: retrofit2.HttpException) {
                                    enviando = false
                                    errorMensaje = "Error ${e.code()}. Intenta de nuevo."
                                } catch (e: Exception) {
                                    enviando = false
                                    errorMensaje = "No se pudo conectar al servidor."
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    TextButton(onClick = onBackToLogin) {
                        Text(
                            text = buildAnnotatedString {
                                append("¿Recordaste tu contraseña? ")
                                withStyle(style = SpanStyle(color = verdeMarca(), fontWeight = FontWeight.Bold)) {
                                    append("Inicia sesión")
                                }
                            },
                            color = colors.textSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    if (enviado) {
        AlertDialog(
            onDismissRequest = { enviado = false },
            containerColor = colors.cardBackground.copy(alpha = 0.98f),
            shape = RoundedCornerShape(24.dp),
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VerdeSena, modifier = Modifier.size(40.dp)) },
            title = {
                Text("Código enviado", color = colors.textPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Revisa tu correo electrónico y usa el código de 8 caracteres para restablecer tu contraseña.",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { enviado = false; onNavigateToReset() },
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = androidx.compose.ui.graphics.Color.Black),
                    shape = RoundedCornerShape(28.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text("Continuar", fontWeight = FontWeight.ExtraBold)
                }
            }
        )
    }
}
