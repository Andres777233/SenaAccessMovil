package com.example.sennaccess.aplicacion
import com.example.sennaccess.R

// Pantalla de presentación inicial (Splash): se muestra al abrir la aplicación con
// una animación de logo tipo iOS. Al terminar la secuencia de entrada/salida invoca
// onFinished, que en ActividadPrincipal lleva a la pantalla de aterrizaje (landing).

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.BuildConfig
import com.example.sennaccess.comun.diseno.ResorteIos
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import kotlinx.coroutines.delay

@Composable
fun PantallaCarga(isDark: Boolean = true, onFinished: () -> Unit) {

    // --- Animación de ENTRADA (logo) con física elástica ---
    val logoScale = remember { Animatable(0f) }
    val logoAlpha = remember { Animatable(0f) }
    val glowAlpha = remember { Animatable(0f) }

    // --- Animación de SALIDA (fade + scale) ---
    var exiting by remember { mutableStateOf(false) }
    val exitAlpha by animateFloatAsState(
        targetValue = if (exiting) 0f else 1f,
        animationSpec = ResorteIos.ScreenFade,
        label = "splashExitAlpha"
    )
    val exitScale by animateFloatAsState(
        targetValue = if (exiting) 1.08f else 1f,
        animationSpec = ResorteIos.ScreenFade,
        label = "splashExitScale"
    )

    var showSubtitle by remember { mutableStateOf(false) }
    val subtitleAlpha by animateFloatAsState(
        targetValue = if (showSubtitle) 1f else 0f,
        animationSpec = tween(400),
        label = "subtitleAlpha"
    )

    LaunchedEffect(Unit) {
        glowAlpha.animateTo(1f, animationSpec = tween(420))
        logoAlpha.animateTo(1f, animationSpec = tween(250))
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        showSubtitle = true
        delay(450)
        exiting = true
        delay(280)
        onFinished()
    }

    val bg = ColoresAppLocal.current.background

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .alpha(exitAlpha)
            .graphicsLayer {
                scaleX = exitScale
                scaleY = exitScale
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp)) {

                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .alpha(glowAlpha.value)
                        .background(
                            Brush.radialGradient(
                                colors = if (isDark) listOf(
                                    VerdeSena.copy(alpha = 0.55f),
                                    VerdeSena.copy(alpha = 0.18f),
                                    Color.Transparent
                                ) else listOf(
                                    VerdeSena.copy(alpha = 0.72f),
                                    VerdeSena.copy(alpha = 0.28f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Image(
                    painter = painterResource(id = R.drawable.logo_sena),
                    contentDescription = "SENA",
                    modifier = Modifier
                        .size(180.dp)
                        .alpha(logoAlpha.value)
                        .scale(logoScale.value)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "SENA ACCESS",
                color = verdeMarca(),
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                modifier = Modifier.alpha(subtitleAlpha)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Versión ${BuildConfig.VERSION_NAME}",
                color = ColoresAppLocal.current.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.alpha(subtitleAlpha)
            )
        }
    }
}
