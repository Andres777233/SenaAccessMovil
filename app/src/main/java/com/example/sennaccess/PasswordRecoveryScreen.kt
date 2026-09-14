// Pantalla de recuperación de contraseña: permite solicitar el envío de un código
// de recuperación al correo (POST /api/forgot-password). Al enviarlo con éxito,
// navega a la pantalla de restablecimiento con onNavigateToReset; desde aquí
// también se regresa al login con onBackToLogin.
package com.example.sennaccess


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
import com.example.sennaccess.data.AuthRepository
import com.example.sennaccess.ui.ios.AuthBackdrop
import com.example.sennaccess.ui.ios.AuthField
import com.example.sennaccess.ui.ios.AuthLogo
import com.example.sennaccess.ui.ios.ErrorBox
import com.example.sennaccess.ui.ios.IosGlassCard
import com.example.sennaccess.ui.ios.PrimaryNeonButton
import com.example.sennaccess.ui.ios.ThemeToggleButton
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import com.example.sennaccess.ui.theme.verdeMarca
import kotlinx.coroutines.launch

@Composable
fun PasswordRecoveryScreen(
    onBackToLogin: () -> Unit,
    onNavigateToReset: () -> Unit = {},
    isDark: Boolean = true,
    onToggleTheme: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var errorMensaje by remember { mutableStateOf<String?>(null) }
    var enviado by remember { mutableStateOf(false) }

    AuthBackdrop(isDark = isDark) {
        ThemeToggleButton(
            isDark = isDark,
            onToggleTheme = onToggleTheme,
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
        )

        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            IosGlassCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AuthLogo(modifier = Modifier.size(90.dp).padding(bottom = 16.dp))
                    Text(
                        text = "Recuperar Contraseña",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    AuthField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Correo Electrónico",
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    if (errorMensaje != null) {
                        ErrorBox(
                            texto = errorMensaje!!,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    PrimaryNeonButton(
                        text = "ENVIAR CÓDIGO",
                        icon = Icons.Default.Send,
                        loading = enviando,
                        onClick = {
                            if (enviando) return@PrimaryNeonButton
                            if (email.isBlank()) {
                                errorMensaje = "Ingresa tu correo electrónico."
                                return@PrimaryNeonButton
                            }
                            errorMensaje = null
                            enviando = true
                            scope.launch {
                                try {
                                    AuthRepository().forgotPassword(email.trim())
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
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SenaGreen, modifier = Modifier.size(40.dp)) },
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
                    colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = androidx.compose.ui.graphics.Color.Black),
                    shape = RoundedCornerShape(28.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text("Continuar", fontWeight = FontWeight.ExtraBold)
                }
            }
        )
    }
}