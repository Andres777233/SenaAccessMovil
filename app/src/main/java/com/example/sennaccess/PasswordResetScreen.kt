// Pantalla de restablecimiento de contraseña: permite cambiar la contraseña con
// el código de recuperación recibido por correo (POST /api/reset-password).
// Al confirmar el cambio con éxito, vuelve al login con onBackToLogin.
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.data.AuthRepository
import com.example.sennaccess.data.ResetRequest
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
fun PasswordResetScreen(
    onBackToLogin: () -> Unit,
    isDark: Boolean = true,
    onToggleTheme: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()

    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var enviando by remember { mutableStateOf(false) }
    var errorMensaje by remember { mutableStateOf<String?>(null) }
    var completado by remember { mutableStateOf(false) }

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
                        text = "Restablecer Contraseña",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    AuthField(
                        value = code,
                        onValueChange = { code = it.uppercase().take(10) },
                        label = "Código de recuperación",
                        imeAction = ImeAction.Next
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AuthField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Nueva contraseña",
                        isPassword = true,
                        imeAction = ImeAction.Next
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AuthField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = "Confirmar contraseña",
                        isPassword = true,
                        imeAction = ImeAction.Done
                    )

                    if (errorMensaje != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        ErrorBox(texto = errorMensaje!!)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    PrimaryNeonButton(
                        text = "CAMBIAR CONTRASEÑA",
                        icon = Icons.Default.Send,
                        loading = enviando,
                        onClick = {
                            if (enviando) return@PrimaryNeonButton
                            if (code.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                                errorMensaje = "Completa todos los campos."
                                return@PrimaryNeonButton
                            }
                            if (password != confirmPassword) {
                                errorMensaje = "Las contraseñas no coinciden."
                                return@PrimaryNeonButton
                            }
                            if (password.length < 8) {
                                errorMensaje = "La contraseña debe tener al menos 8 caracteres."
                                return@PrimaryNeonButton
                            }
                            errorMensaje = null
                            enviando = true
                            scope.launch {
                                try {
                                    AuthRepository().resetPassword(
                                        ResetRequest(
                                            code = code,
                                            password = password,
                                            password_confirmation = confirmPassword
                                        )
                                    )
                                    enviando = false
                                    completado = true
                                } catch (e: retrofit2.HttpException) {
                                    enviando = false
                                    errorMensaje = try {
                                        val json = org.json.JSONObject(e.response()?.errorBody()?.string() ?: "{}")
                                        json.optString("message").ifBlank { "Error ${e.code()}. Verifica el código." }
                                    } catch (_: Exception) {
                                        "Error ${e.code()}. Verifica el código."
                                    }
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
                                append("¿Ya tienes tu contraseña? ")
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

    if (completado) {
        AlertDialog(
            onDismissRequest = { completado = false },
            containerColor = colors.cardBackground.copy(alpha = 0.98f),
            shape = RoundedCornerShape(24.dp),
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SenaGreen, modifier = Modifier.size(40.dp)) },
            title = {
                Text("Contraseña actualizada", color = colors.textPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Tu contraseña se cambió correctamente. Ya puedes iniciar sesión con tu nueva contraseña.",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { completado = false; onBackToLogin() },
                    colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = androidx.compose.ui.graphics.Color.Black),
                    shape = RoundedCornerShape(28.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text("Okey", fontWeight = FontWeight.ExtraBold)
                }
            }
        )
    }
}