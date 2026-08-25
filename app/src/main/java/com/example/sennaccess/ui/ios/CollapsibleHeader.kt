package com.example.sennaccess.ui.ios

// Encabezado grande colapsable estilo iOS ("Large Header"): el título aparece
// grande al inicio y al hacer scroll se encoge hasta fundirse en una barra
// superior de vidrio. Se usa como cabecera de pantallas con scroll.
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen

/**
 * Encabezado grande colapsable tipo iOS ("Large Header").
 *
 * Comportamiento:
 *  - Con [scrollOffset] pequeño muestra el título GRANDE (34sp) al estilo iOS.
 *  - Al hacer scroll, se encoge y se funde en una barra superior de vidrio.
 *
 * La barra superior de vidrio aparece progresivamente con el scroll.
 *
 * @param title   texto grande.
 * @param scrollOffset  valor actual del scroll (p.ej. scrollState.value).
 * @param collapseRange  px de scroll en los que colapsa por completo.
 * @param trailing  acciones a la derecha en la barra colapsada (iconos).
 */
@Composable
fun IosCollapsibleHeader(
    title: String,
    scrollOffset: Float,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    collapseRange: Float = 220f,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    val colors = LocalAppColors.current

    // Progreso de colapso 0 (grande) -> 1 (barra superior).
    val collapse = (scrollOffset / collapseRange).coerceIn(0f, 1f)
    val animatedCollapse by animateFloatAsState(
        targetValue = collapse,
        animationSpec = IosSpring.Gentle,
        label = "headerCollapse"
    )

    // Título grande: escala + desvanecimiento + se desliza hacia arriba.
    val largeScale = lerp(1f, 0.72f, animatedCollapse)
    val largeAlpha = lerp(1f, 0f, (animatedCollapse * 1.4f).coerceIn(0f, 1f))
    val largeOffsetY = lerp(0f, -28f, animatedCollapse)

    // Barra superior de vidrio: aparece con el colapso.
    val topBarAlpha = ((animatedCollapse - 0.5f) * 2f).coerceIn(0f, 1f)

    // Capa raíz: el título grande (arriba) se desliza y la barra de vidrio aparece.
    Box(modifier.fillMaxWidth()) {
        // --- TÍTULO GRANDE ---
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .graphicsLayer {
                    scaleX = largeScale
                    scaleY = largeScale
                    translationY = largeOffsetY
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 1f)
                }
                .alpha(largeAlpha)
        ) {
            androidx.compose.foundation.layout.Column {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // --- BARRA SUPERIOR DE VIDRIO (colapsada) ---
        Box(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .alpha(topBarAlpha)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SenaGreen.copy(alpha = 0.10f),
                            colors.topBarBackground.copy(alpha = 0.85f),
                            colors.topBarBackground.copy(alpha = 0.6f)
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            androidx.compose.foundation.layout.Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                trailing?.invoke(this)
            }
        }
    }
}

/**
 * Variante simple de barra superior de vidrio (sin título grande),
 * para reemplazar los TopBars actuales con estética iOS.
 */
@Composable
fun IosGlassTopBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val colors = LocalAppColors.current
    Box(
        modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SenaGreen.copy(alpha = 0.10f),
                        colors.topBarBackground.copy(alpha = 0.75f),
                        colors.topBarBackground.copy(alpha = 0.45f)
                    )
                )
            )
            .background(colors.surface.copy(alpha = 0.15f))
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}
