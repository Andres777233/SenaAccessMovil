package com.example.sennaccess.ui.ios

// Biblioteca central de animaciones con física elástica estilo iOS.
// Provee las curvas y springs usados en toda la app: entradas de elementos,
// transiciones entre pantallas y microinteracciones táctiles (press/release).
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Sistema de animaciones elásticas inspirado en Apple Human Interface Guidelines.
 *
 * Todas las curvas son reutilizables y seguras a 60 FPS (usan physics / GPU).
 */
object IosSpring {

    /**
     * Aproximación de la curva elástica de rebote de iOS:
     * cubic-bezier(0.175, 0.885, 0.32, 1.275)  ("elasticOut" con ligero overshoot).
     *
     * Se usa para entradas de logo / elementos que aparecen.
     */
    val ElasticOutEasing = CubicBezierEasing(0.175f, 0.885f, 0.32f, 1.275f)

    /** Tween elástico de entrada (logo, tarjetas que aparecen). */
    val ElasticOut: TweenSpec<Float> = tween(durationMillis = 700, easing = ElasticOutEasing)

    /** Tween elástico rápido (pequeñas apariciones). */
    val ElasticOutFast: TweenSpec<Float> = tween(durationMillis = 450, easing = ElasticOutEasing)

    /**
     * Spring físico con rebote medio (elástico) — equivalente a Spring.DampingRatioMediumBouncy.
     * Útil para press-scale y transiciones de elementos interactivos.
     */
    val Bouncy: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /** Spring muy elástico (entradas destacadas, logo). */
    val HighBouncy: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioHighBouncy,
        stiffness = Spring.StiffnessLow
    )

    /** Spring suave y preciso (sin overshoot) para microinteracciones de release. */
    val Gentle: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /**
     * Microinteracción táctil: vuelta suave tras presionar.
     * Duración ~150ms con ease-out, rango pedido 120–200ms.
     */
    val PressRelease: TweenSpec<Float> = tween(durationMillis = 150, easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f))

    /** Fade suave para transiciones entre pantallas. */
    val ScreenFade: TweenSpec<Float> = tween(durationMillis = 280, easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f))
}
