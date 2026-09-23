package com.example.sennaccess.comun.diseno

// Modificador que aporta respuesta táctil estilo iOS a cualquier componente:
// al presionar reduce la escala y al soltar regresa con física elástica.
// Se usa en botones, tarjetas y en el dock de navegación flotante.
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

fun Modifier.escalaPresion(
    pressedScale: Float = 0.96f,
    interactionSource: MutableInteractionSource? = null
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = if (pressed) ResorteIos.Rebote else ResorteIos.Suave,
        label = "escalaPresion"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

fun Modifier.pressScaleWith(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f
): Modifier = this.escalaPresion(pressedScale, interactionSource)
