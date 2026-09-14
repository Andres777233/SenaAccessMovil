package com.example.sennaccess

// Pantalla de inicio (landing): puerta de entrada de la aplicación.
// Se muestra tras el splash y ofrece dos caminos: ingresar (login) o registrarse
// (register), además de alternar el tema claro/oscuro desde la esquina superior.

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.ui.ios.AuthBackdrop
import com.example.sennaccess.ui.ios.AuthLogo
import com.example.sennaccess.ui.ios.GlowOutlinedButton
import com.example.sennaccess.ui.ios.IosGlassCard
import com.example.sennaccess.ui.ios.PrimaryNeonButton
import com.example.sennaccess.ui.ios.ThemeToggleButton
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.verdeMarca

@Composable
fun LandingScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    isDark: Boolean = true,
    onToggleTheme: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    val marca = verdeMarca()

    AuthBackdrop(isDark = isDark) {
        ThemeToggleButton(
            isDark = isDark,
            onToggleTheme = onToggleTheme,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 12.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            IosGlassCard(modifier = Modifier.fillMaxWidth(0.96f)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AuthLogo(modifier = Modifier.size(120.dp))

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = buildAnnotatedString {
                            append("Sena ")
                            withStyle(
                                style = SpanStyle(color = marca, fontWeight = FontWeight.Bold)
                            ) {
                                append("ACCESS")
                            }
                        },
                        color = colors.textPrimary,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "CONTROL DE ACCESO BIOMÉTRICO",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    PrimaryNeonButton(
                        text = "INGRESAR",
                        icon = Icons.AutoMirrored.Filled.Login,
                        onClick = onNavigateToLogin,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    GlowOutlinedButton(
                        text = "REGISTRARSE",
                        icon = Icons.Default.AppRegistration,
                        onClick = onNavigateToRegister,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        color = marca.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Sistema de Gestión de Ambientes y Equipos",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}