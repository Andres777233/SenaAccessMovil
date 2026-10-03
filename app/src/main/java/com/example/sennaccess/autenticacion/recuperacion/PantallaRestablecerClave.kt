// Pantalla de restablecimiento de contraseña: permite cambiar la contraseña con
// el código de recuperación recibido por correo (POST /api/reset-password).
// Al confirmar el cambio con éxito, vuelve al login con onBackToLogin.
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.datos.repositorios.RepositorioAutenticacion
import com.example.sennaccess.datos.modelos.PeticionRestablecer
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
fun PantallaRestablecerClave(
    onBackToLogin: () -> Unit,
    isDark: Boolean = true,
    onToggleTheme: () -> Unit = {}
) {
    val colors = ColoresAppLocal.current
    val scope = rememberCoroutineScope()

    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var enviando by remember { mutableStateOf(false) }
    var errorMensaje by remember { mutableStateOf<String?>(null) }
    var completado by remember { mutableStateOf(false) }

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
                text = "NUEVA CLAVE",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.8.sp),
                color = verdeMarca(),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Crea tu nueva clave",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Usa el código de 6 dígitos que llegó a tu correo.",
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
                        value = code,
                        onValueChange = { code = it.uppercase().take(10) },
                        label = "Código de recuperación",
                        imeAction = ImeAction.Next
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CampoAcceso(
                        value = password,
                        onValueChange = { password = it },
                        label = "Nueva contraseña",
                        isPassword = true,
                        imeAction = ImeAction.Next
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CampoAcceso(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = "Confirmar contraseña",
                        isPassword = true,
                        imeAction = ImeAction.Done
                    )

                    if (errorMensaje != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        CajaError(texto = errorMensaje!!)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    BotonPrimarioNeon(
                        text = "CAMBIAR CONTRASEÑA",
                        icon = Icons.Default.Send,
                        loading = enviando,
                        onClick = {
                            if (enviando) return@BotonPrimarioNeon
                            if (code.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                                errorMensaje = "Completa todos los campos."
                                return@BotonPrimarioNeon
                            }
                            if (password != confirmPassword) {
                                errorMensaje = "Las contraseñas no coinciden."
                                return@BotonPrimarioNeon
                            }
                            if (password.length < 8) {
                                errorMensaje = "La contraseña debe tener al menos 8 caracteres."
                                return@BotonPrimarioNeon
                            }
                            errorMensaje = null
                            enviando = true
                            scope.launch {
                                try {
                                    RepositorioAutenticacion().resetPassword(
                                        PeticionRestablecer(
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
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VerdeSena, modifier = Modifier.size(40.dp)) },
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
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = androidx.compose.ui.graphics.Color.Black),
                    shape = RoundedCornerShape(28.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text("Okey", fontWeight = FontWeight.ExtraBold)
                }
            }
        )
    }
}
