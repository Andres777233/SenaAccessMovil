package com.example.sennaccess.comun.diseno

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
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca

// Cabecera senior fija: eyebrow + título + subtítulo + acción lateral.
// Reemplaza el colapso animado (movimiento gratuito en listados): una sola
// jerarquía izquierda, sin re-animar en cada scroll. Misma firma para no
// romper las 20 pantallas que ya la usan.
@Composable
fun CabeceraPlegable(
    title: String,
    scrollOffset: Float,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    collapseRange: Float = 220f,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    val colors = ColoresAppLocal.current
    androidx.compose.foundation.layout.Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "SENA ACCESS",
                color = verdeMarca(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.sp,
                maxLines = 2
            )
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    maxLines = 2
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
fun BarraSuperiorVidrio(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val colors = ColoresAppLocal.current
    Box(
        modifier
            .fillMaxWidth()
            .background(colors.topBarBackground.copy(alpha = 0.92f))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}
