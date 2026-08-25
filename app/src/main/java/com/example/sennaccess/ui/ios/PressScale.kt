package com.example.sennaccess.ui.ios

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

/**
 * Efecto táctil tipo iOS: al presionar, el elemento reduce su escala levemente
 * y regresa suave con física elástica al soltar.
 *
 * Uso:
 *   Box(Modifier.pressScale().clickable { ... })
 *
 * @param pressedScale  escala al presionar (0.95–0.97 recomendado). Por defecto 0.96.
 * @param interactionSource  si el componente ya tiene uno (p.ej. Button), pásalo
 *                           para compartir el estado de pressed; si no, se crea uno.
 */
fun Modifier.pressScale(
    pressedScale: Float = 0.96f,
    interactionSource: MutableInteractionSource? = null
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()

    // Al presionar la escala anima con rebote; al soltar vuelve con física suave.
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = if (pressed) IosSpring.Bouncy else IosSpring.Gentle,
        label = "pressScale"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Variante que además devuelve el [MutableInteractionSource] para que lo uses
 * en tu propio `clickable`/`Button` y así compartir el estado pressed.
 *
 * Uso:
 *   val src = remember { MutableInteractionSource() }
 *   Box(Modifier.pressScale(interactionSource = src).clickable(src, null) { ... })
 */
fun Modifier.pressScaleWith(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f
): Modifier = this.pressScale(pressedScale, interactionSource)
