package com.example.sennaccess.ui.ios

// Barra de navegación flotante estilo dock de iOS con fondo de vidrio:
// incluye indicador "pill" animado y escala elástica en la pestaña activa.
// Reemplaza a la barra inferior en las pantallas principales de la app.
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import androidx.compose.ui.graphics.luminance

/**
 * Item del dock flotante.
 */
data class GlassDockItem(
    val key: String,
    val icon: ImageVector,
    val label: String,
    val contentDescription: String = label
)

/**
 * Barra de navegación flotante suspendida estilo dock de iOS.
 *
 *  - Fondo con glassmorphism (translúcido + highlight + borde).
 *  - Esquinas muy redondas (35dp).
 *  - Sombra suave de baja opacidad.
 *  - Indicador animado (pill) tras la pestaña activa + escala/color animados.
 *
 * NO altera la lógica: solo notifica [onSelect] con la key del item.
 *
 * @param items   lista de pestañas (máx ~5 recomendado).
 * @param selectedKey  key actualmente activa.
 * @param onSelect  callback con la key al pulsar.
 */
@Composable
fun GlassDock(
    items: List<GlassDockItem>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(35.dp)
    val isLight = colors.background.luminance() > 0.5f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        // Fondo de vidrio del dock: sombra y borde adaptados al tema.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isLight) 14.dp else 24.dp,
                    shape = shape,
                    clip = false,
                    ambientColor = if (isLight) Color.Black.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.35f),
                    spotColor = if (isLight) Color.Black.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.45f)
                )
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colors.surface.copy(alpha = if (isLight) 0.92f else 0.75f),
                            colors.surface.copy(alpha = if (isLight) 0.85f else 0.55f)
                        )
                    )
                )
                .background(Color.White.copy(alpha = if (isLight) 0.0f else 0.05f))
                .border(1.dp, if (isLight) colors.border.copy(alpha = 0.9f) else colors.borderLight.copy(alpha = 0.18f), shape)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = item.key == selectedKey
                DockItem(
                    item = item,
                    selected = selected,
                    onClick = { onSelect(item.key) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// Item interno del dock: combina el efecto press con las animaciones del estado activo.
@Composable
private fun DockItem(
    item: GlassDockItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val interactionSource = remember { MutableInteractionSource() }

    // Animaciones del item: escala, opacidad y visibilidad del indicador según el estado.
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = IosSpring.Bouncy,
        label = "dockIconScale"
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.55f,
        animationSpec = IosSpring.Gentle,
        label = "dockIconAlpha"
    )
    val pillAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = IosSpring.Gentle,
        label = "dockPill"
    )

    Column(
        modifier = modifier
            .pressScale(pressedScale = 0.92f, interactionSource = interactionSource)
            .clip(RoundedCornerShape(24.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 52.dp, height = 34.dp)
        ) {
            // Indicador "pill" animado tras el icono activo.
            Box(
                Modifier
                    .fillMaxSize()
                    .scale(iconScale)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        SenaGreen.copy(alpha = 0.18f * pillAlpha)
                    )
            )
            Icon(
                imageVector = item.icon,
                contentDescription = item.contentDescription,
                tint = if (selected) SenaGreen else colors.textSecondary,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        this.alpha = iconAlpha
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )
        }
        Text(
            text = item.label,
            color = if (selected) SenaGreen else colors.textSecondary,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.graphicsLayer {
                alpha = iconAlpha
            }
        )
    }
}
