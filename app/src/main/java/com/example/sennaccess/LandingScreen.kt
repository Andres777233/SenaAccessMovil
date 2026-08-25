package com.example.sennaccess.ui.theme

// Pantalla de inicio (landing): puerta de entrada de la aplicación.
// Se muestra tras el splash y ofrece dos caminos: ingresar (login) o registrarse
// (register), además de alternar el tema claro/oscuro desde la esquina superior.

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.res.painterResource
import com.example.sennaccess.R
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.ios.GlowSpheres
import com.example.sennaccess.ui.ios.glassSurface
import com.example.sennaccess.ui.ios.GlassCornerRadiusXl
import com.example.sennaccess.ui.ios.pressScale
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode


@Composable
fun LandingScreen(onNavigateToLogin: () -> Unit, onNavigateToRegister: () -> Unit, isDark: Boolean = true, onToggleTheme: () -> Unit = {}) {
    // Composable principal: pantalla de bienvenida con branding SENA y botones de acceso.
    val colors = LocalAppColors.current

    // --- CONTENEDOR RAÍZ ---
    // Ocupa toda la pantalla con un fondo negro muy oscuro.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Alternador de tema claro/oscuro en la esquina superior derecha.
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

        // --- CAPA 1: PATRÓN DE FONDO ---
        // Imagen que se repite sutilmente al fondo.
        Image(
            painter = rememberAsyncImagePainter("https://www.sena.edu.co/Style%20Library/alayout/images/pattern.png"),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().graphicsLayer(alpha = 0.12f),
            contentScale = ContentScale.Crop
        )

        // --- CAPA 1.5: LUCES AMBIENTALES (glow spheres) ---
        GlowSpheres(isDark = isDark)

        // --- CAPA 2: CONTENEDOR CENTRADOR ---
        // Centra nuestra tarjeta "de cristal" en medio de la pantalla.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {

            // --- TARJETA DE CRISTAL (Glassmorphism) ---
            // Aplicamos las sombras, el fondo semitransparente y el borde neón.
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .glassSurface(cornerRadius = GlassCornerRadiusXl, elevated = true)
                    .padding(horizontal = 24.dp, vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {

                // --- CONTENIDO INTERNO (Logo, Textos y Botones) ---
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Logo SENA
                    Image(
                        painter = painterResource(R.drawable.logo_sena),
                        contentDescription = "Logo SENA",
                        modifier = Modifier
                            .size(120.dp)
                            .padding(bottom = 24.dp)
                    )

                    // Título SENA ACCESS
                    Text(
                        text = buildAnnotatedString {
                            append("SENA ")
                            withStyle(style = SpanStyle(color = SenaGreen, fontWeight = FontWeight.Bold)) {
                                append("ACCESS")
                            }
                        },
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textPrimary,
                        letterSpacing = 2.sp
                    )

                    // Subtítulo
                    Text(
                        text = "CONTROL DE ACCESO BIOMÉTRICO",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
                    )

                    // Botón Ingresar
                    Button(
                        onClick = onNavigateToLogin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScale(pressedScale = 0.97f)
                            .shadow(15.dp, RoundedCornerShape(50), spotColor = SenaGreen)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(text = "INGRESAR", fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón Registrarse
                    OutlinedButton(
                        onClick = onNavigateToRegister,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScale(pressedScale = 0.97f)
                            .height(56.dp),
                        border = BorderStroke(2.dp, SenaGreen.copy(alpha = 0.8f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(text = "REGISTRARSE", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    // Separador sutil
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        color = SenaGreen.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Versión del sistema
                    Text(
                        text = "Sistema de Gestión de Ambientes y Equipos",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "v0.1.99",
                        fontSize = 12.sp,
                        color = SenaGreen.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}