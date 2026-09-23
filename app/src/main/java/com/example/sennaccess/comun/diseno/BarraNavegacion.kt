package com.example.sennaccess.comun.diseno

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import androidx.compose.ui.graphics.luminance

data class ElementoNavegacion(
    val key: String,
    val icon: ImageVector,
    val label: String,
    val contentDescription: String = label
)

@Composable
fun BarraNavegacion(
    items: List<ElementoNavegacion>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ColoresAppLocal.current
    val shape = RoundedCornerShape(35.dp)
    val isLight = colors.background.luminance() > 0.5f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isLight) 18.dp else 28.dp,
                    shape = shape,
                    clip = false,
                    ambientColor = if (isLight) {
                        Color.Black.copy(alpha = 0.15f)
                    } else {
                        Color.Black.copy(alpha = 0.4f)
                    },
                    spotColor = if (isLight) {
                        Color.Black.copy(alpha = 0.19f)
                    } else {
                        Color.Black.copy(alpha = 0.5f)
                    }
                )
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colors.surface.copy(alpha = if (isLight) 0.96f else 0.8f),
                            colors.surface.copy(alpha = if (isLight) 0.9f else 0.65f)
                        )
                    )
                )
                .background(Color.White.copy(alpha = if (isLight) 0.05f else 0.08f))
                .border(
                    1.2.dp,
                    if (isLight) {
                        colors.border.copy(alpha = 0.95f)
                    } else {
                        colors.borderLight.copy(alpha = 0.25f)
                    },
                    shape
                )
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

@Composable
private fun DockItem(
    item: ElementoNavegacion,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ColoresAppLocal.current
    val interactionSource = remember { MutableInteractionSource() }
    val acento = if (colors.background.luminance() > 0.5f) verdeMarca() else VerdeSena

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = ResorteIos.Rebote,
        label = "dockIconScale"
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.55f,
        animationSpec = ResorteIos.Suave,
        label = "dockIconAlpha"
    )
    val pillAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = ResorteIos.Suave,
        label = "dockPill"
    )

    Column(
        modifier = modifier
            .escalaPresion(pressedScale = 0.92f, interactionSource = interactionSource)
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
            Box(
                Modifier
                    .fillMaxSize()
                    .scale(iconScale)
                    .clip(RoundedCornerShape(18.dp))
                    .background(acento.copy(alpha = 0.22f * pillAlpha))
            )
            Icon(
                imageVector = item.icon,
                contentDescription = item.contentDescription,
                tint = if (selected) acento else colors.textSecondary,
                modifier = Modifier
                    .size(26.dp)
                    .graphicsLayer {
                        this.alpha = iconAlpha
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )
        }
        Text(
            text = item.label,
            color = if (selected) acento else colors.textSecondary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.graphicsLayer {
                alpha = iconAlpha
            }
        )
    }
}
