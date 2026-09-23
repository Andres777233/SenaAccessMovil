package com.example.sennaccess.comun.diseno

// Sistema de superficies con glassmorphism (vidrio esmerilado) estilo iOS:
// tarjetas, contenedores y luces ambientales de fondo. Se usa como base visual
// de las pantallas principales de la app.
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import androidx.compose.ui.graphics.luminance

// ---- Radios estándar ----
val RadioVidrio: Dp = 24.dp
val RadioVidrioGrande: Dp = 28.dp
val RadioVidrioExtra: Dp = 32.dp

@Composable
fun EsferasBrillo(modifier: Modifier = Modifier, isDark: Boolean = true) {
    val sphereAlpha = if (isDark) 1f else 0.28f
    Box(modifier.fillMaxSize()) {
        Box(
            Modifier
                .offset(x = (-60).dp, y = (-40).dp)
                .size(320.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            VerdeSena.copy(alpha = 0.22f * sphereAlpha),
                            VerdeSena.copy(alpha = 0.06f * sphereAlpha),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 80.dp)
                .size(360.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00BFA5).copy(alpha = 0.14f * sphereAlpha),
                            VerdeSena.copy(alpha = 0.05f * sphereAlpha),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            Modifier
                .align(Alignment.Center)
                .size(300.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            VerdeSena.copy(alpha = 0.05f * sphereAlpha),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
fun Modifier.superficieVidrio(
    cornerRadius: Dp = RadioVidrio,
    elevated: Boolean = false
): Modifier {
    val colors = ColoresAppLocal.current
    val shape = RoundedCornerShape(cornerRadius)
    val isLight = colors.background.luminance() > 0.5f

    val base = if (isLight) {
        colors.cardBackground.copy(alpha = 0.96f)
    } else {
        colors.cardBackground.copy(alpha = 0.6f)
    }

    val highlight = if (isLight) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.7f),
                Color.White.copy(alpha = 0.3f),
                Color.Transparent
            ),
            start = Offset(0f, 0f),
            end = Offset(0f, Float.POSITIVE_INFINITY)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.18f),
                Color.White.copy(alpha = 0.05f),
                Color.Transparent
            ),
            start = Offset(0f, 0f),
            end = Offset(0f, Float.POSITIVE_INFINITY)
        )
    }

    return this
        .shadow(
            elevation = if (isLight) {
                if (elevated) 20.dp else 12.dp
            } else {
                if (elevated) 28.dp else 18.dp
            },
            shape = shape,
            clip = false,
            ambientColor = if (isLight) {
                Color.Black.copy(alpha = 0.15f)
            } else {
                Color.Black.copy(alpha = 0.5f)
            },
            spotColor = if (isLight) {
                Color.Black.copy(alpha = 0.18f)
            } else {
                Color.Black.copy(alpha = 0.6f)
            }
        )
        .clip(shape)
        .background(base)
        .background(highlight)
        .border(
            1.2.dp,
            if (isLight) {
                colors.border.copy(alpha = 0.95f)
            } else {
                colors.borderLight.copy(alpha = 0.25f)
            },
            shape
        )
}

@Composable
fun TarjetaVidrio(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = RadioVidrioGrande,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.superficieVidrio(cornerRadius = cornerRadius, elevated = true),
        content = content
    )
}

@Composable
fun ContenedorVidrio(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = RadioVidrio,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.superficieVidrio(cornerRadius = cornerRadius),
        content = content
    )
}

@Composable
fun MenuDesplegableVidrio(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = ColoresAppLocal.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        containerColor = colors.cardBackground.copy(alpha = 0.98f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, colors.borderLight.copy(alpha = 0.25f)),
        shadowElevation = 16.dp,
        content = content
    )
}
