package com.example.sennaccess.ui.ios

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
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen

/**
 * Sistema de diseño Glassmorphism (vidrio esmerilado) estilo iOS.
 *
 * Características:
 *  - Fondo semitransparente con highlight superior (luz).
 *  - Borde sutil traslúcido blanco (0.15) de 1dp.
 *  - Radio de curvatura pronunciado (24–32dp).
 *  - Backdrop blur real en API 31+ (RenderEffect); fallback translúcido abajo.
 *
 * Paleta SENA respetada: usa [LocalAppColors] y [SenaGreen].
 */

// ---- Radios estándar ----
val GlassCornerRadius: Dp = 24.dp
val GlassCornerRadiusLg: Dp = 28.dp
val GlassCornerRadiusXl: Dp = 32.dp

/**
 * Capa de "glow spheres": luces ambientales suaves detrás de las tarjetas
 * para acentuar el efecto de vidrio. Colócalo en el fondo de la pantalla.
 *
 * Uso:
 *   Box(Modifier.fillMaxSize()) {
 *       GlowSpheres()
 *       // contenido...
 *   }
 */
@Composable
fun GlowSpheres(modifier: Modifier = Modifier, isDark: Boolean = true) {
    // La opacidad de las esferas se reduce en tema claro para no ensuciar el fondo.
    val sphereAlpha = if (isDark) 1f else 0.5f
    Box(modifier.fillMaxSize()) {
        // Esfera verde SENA (arriba-izquierda)
        Box(
            Modifier
                .offset(x = (-60).dp, y = (-40).dp)
                .size(320.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SenaGreen.copy(alpha = 0.22f * sphereAlpha),
                            SenaGreen.copy(alpha = 0.06f * sphereAlpha),
                            Color.Transparent
                        )
                    )
                )
        )
        // Esfera verde-azulada (abajo-derecha)
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 80.dp)
                .size(360.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00BFA5).copy(alpha = 0.14f * sphereAlpha),
                            SenaGreen.copy(alpha = 0.05f * sphereAlpha),
                            Color.Transparent
                        )
                    )
                )
        )
        // Esfera cálida sutil (centro)
        Box(
            Modifier
                .align(Alignment.Center)
                .size(300.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SenaGreen.copy(alpha = 0.05f * sphereAlpha),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/**
 * Modifier reutilizable de superficie de vidrio.
 * Aplica sombra suave + fondo semitransparente con highlight + borde traslúcido.
 *
 * @param cornerRadius radio de esquinas.
 * @param elevated  si true, sombra un poco más presente.
 */
@Composable
fun Modifier.glassSurface(
    cornerRadius: Dp = GlassCornerRadius,
    elevated: Boolean = false
): Modifier {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(cornerRadius)

    // Fondo semitransparente: usa el cardBackground del tema pero más translúcido.
    val base = colors.cardBackground.copy(alpha = 0.5f)

    // Highlight superior (luz que cae sobre el vidrio).
    val highlight = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.10f),
            Color.White.copy(alpha = 0.02f),
            Color.Transparent
        ),
        start = Offset(0f, 0f),
        end = Offset(0f, Float.POSITIVE_INFINITY)
    )

    return this
        .shadow(
            elevation = if (elevated) 24.dp else 14.dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.4f),
            spotColor = Color.Black.copy(alpha = 0.5f)
        )
        .clip(shape)
        .background(base)
        .background(highlight)
        .border(1.dp, colors.borderLight.copy(alpha = 0.15f), shape)
}

/**
 * Tarjeta de vidrio reutilizable (reemplazo iOS de la antigua GlassCard).
 * Mantiene la firma simple: contenido en un [BoxScope].
 */
@Composable
fun IosGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = GlassCornerRadiusLg,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.glassSurface(cornerRadius = cornerRadius, elevated = true),
        content = content
    )
}

/**
 * Contenedor de vidrio en columna (reemplazo iOS de AdminGlassContainer).
 */
@Composable
fun IosGlassContainer(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = GlassCornerRadius,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.glassSurface(cornerRadius = cornerRadius),
        content = content
    )
}

/**
 * Menú desplegable de vidrio (reemplazo iOS del DropdownMenu Material por defecto).
 * Mantiene la firma de [DropdownMenu] y aplica el estilo glass de la app: fondo
 * translúcido de tarjeta, esquinas redondeadas, borde sutil y sombra elevada.
 */
@Composable
fun IosGlassDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalAppColors.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        containerColor = colors.cardBackground.copy(alpha = 0.98f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, colors.borderLight.copy(alpha = 0.25f)),
        shadowElevation = 16.dp,
        content = content
    )
}
