// Pantalla de restablecimiento de contraseña: permite cambiar la contraseña con
// el código de recuperación recibido por correo (POST /api/reset-password).
// Al confirmar el cambio con éxito, vuelve al login con onBackToLogin.
// Paquete donde está este archivo
package com.example.sennaccess.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.res.painterResource
import com.example.sennaccess.R
import com.example.sennaccess.data.AuthRepository
import com.example.sennaccess.data.ResetRequest
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import com.example.sennaccess.ui.theme.WarningYellow
import com.example.sennaccess.ui.ios.GlowSpheres
import com.example.sennaccess.ui.ios.glassSurface
import com.example.sennaccess.ui.ios.GlassCornerRadiusLg
import com.example.sennaccess.ui.ios.pressScale
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.CheckCircle
import kotlinx.coroutines.launch

// ESTA ES LA PANTALLA DE RESTABLECER CONTRASEÑA
// Composable principal: formulario de código + nueva contraseña en tarjeta de cristal.
@Composable
fun PasswordResetScreen(
    // Función para volver al login al terminar
    onBackToLogin: () -> Unit,
    isDark: Boolean = true,
    onToggleTheme: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()

    // Campos del formulario: código de recuperación y nueva contraseña.
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // Estado del envío: en curso, error o cambio completado.
    var enviando by remember { mutableStateOf(false) }
    var errorMensaje by remember { mutableStateOf<String?>(null) }
    var completado by remember { mutableStateOf(false) }

    // Caja principal que ocupa toda la pantalla
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        IconButton(
            onClick = onToggleTheme,
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
        ) {
            Icon(
                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = if (isDark) "Modo claro" else "Modo oscuro",
                tint = colors.textPrimary
            )
        }

        // Imagen de fondo (patrón)
        Image(
            painter = rememberAsyncImagePainter(
                "https://www.sena.edu.co/Style%20Library/alayout/images/pattern.png"
            ),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.12f),
            contentScale = ContentScale.Crop
        )

        // Luces ambientales detrás del vidrio
        GlowSpheres(isDark = isDark)

        // Caja principal centrada
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {

            // TARJETA CENTRAL (vidrio esmerilado iOS)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .glassSurface(cornerRadius = GlassCornerRadiusLg, elevated = true)
                    .padding(horizontal = 24.dp, vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {

                // Column organiza elementos verticalmente
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Texto grande SENA
                    Text(
                        "SENA",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textPrimary
                    )

                    // Texto pequeño debajo
                    Text(
                        "Bienvenido al CCyS",
                        fontSize = 16.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Logo del sena
                    Image(
                        painter = rememberAsyncImagePainter(
                            "https://www.sena.edu.co/Style%20Library/alayout/images/logoSena.png?rev=40"
                        ),
                        contentDescription = "Logo SENA",
                        modifier = Modifier
                            .size(90.dp)
                            .padding(bottom = 16.dp)
                    )

                    // Título de la pantalla
                    Text(
                        text = "Restablecer Contraseña",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = WarningYellow,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    // INPUT DEL CÓDIGO DE RECUPERACIÓN
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase().take(10) },
                        label = { Text("Código de recuperación", color = colors.textSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = resetCamposColors(colors)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // INPUT NUEVA CONTRASEÑA
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Nueva contraseña", color = colors.textSecondary) },
                        modifier = Modifier.fillMaxWidth(),
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
                        colors = resetCamposColors(colors)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // INPUT CONFIRMAR CONTRASEÑA
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirmar contraseña", color = colors.textSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = resetCamposColors(colors)
                    )

                    // Error de validación (coincidencia o respuesta del backend).
                    if (errorMensaje != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMensaje!!,
                            color = ErrorRed,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // BOTÓN CAMBIAR CONTRASEÑA
                    Button(
                        onClick = {
                            if (enviando) return@Button
                            if (code.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                                errorMensaje = "Completa todos los campos."
                                return@Button
                            }
                            if (password != confirmPassword) {
                                errorMensaje = "Las contraseñas no coinciden."
                                return@Button
                            }
                            if (password.length < 8) {
                                errorMensaje = "La contraseña debe tener al menos 8 caracteres."
                                return@Button
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScale(pressedScale = 0.97f)
                            .shadow(15.dp, RoundedCornerShape(12.dp), spotColor = SenaGreen),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SenaGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {

                        // Row organiza horizontalmente
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            // icono enviar
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // texto botón
                            Text(
                                text = "CAMBIAR CONTRASEÑA",
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // BOTÓN TEXTO VOLVER AL LOGIN
                    TextButton(
                        onClick = onBackToLogin
                    ) {

                        // texto con color personalizado
                        Text(
                            text = buildAnnotatedString {
                                append("¿Ya tienes tu contraseña? ")
                                withStyle(style = SpanStyle(color = SenaGreen, fontWeight = FontWeight.Bold)) {
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

    // Confirmación del cambio de contraseña: al aceptar regresa al login.
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
                    colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text("Okey", fontWeight = FontWeight.ExtraBold)
                }
            }
        )
    }
}

// Paleta de colores común para los campos del formulario de restablecimiento.
@Composable
private fun resetCamposColors(colors: AppColors) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SenaGreen,
    unfocusedBorderColor = colors.divider,
    focusedTextColor = colors.textPrimary,
    unfocusedTextColor = colors.textPrimary,
    focusedContainerColor = colors.surfaceVariant.copy(alpha = 0.5f),
    unfocusedContainerColor = colors.surfaceVariant.copy(alpha = 0.5f)
)
