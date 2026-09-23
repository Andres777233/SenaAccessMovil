package com.example.sennaccess.comun.diseno

// Biblioteca central de animaciones con física elástica estilo iOS.
// Provee las curvas y springs usados en toda la app: entradas de elementos,
// transiciones entre pantallas y microinteracciones táctiles (press/release).
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object ResorteIos {

    val ElasticOutEasing = CubicBezierEasing(0.175f, 0.885f, 0.32f, 1.275f)

    val ElasticOut: TweenSpec<Float> = tween(durationMillis = 700, easing = ElasticOutEasing)

    val ElasticOutFast: TweenSpec<Float> = tween(durationMillis = 450, easing = ElasticOutEasing)

    val Rebote: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val HighBouncy: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioHighBouncy,
        stiffness = Spring.StiffnessLow
    )

    val Suave: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val PressRelease: TweenSpec<Float> = tween(durationMillis = 150, easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f))

    val ScreenFade: TweenSpec<Float> = tween(durationMillis = 280, easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f))
}
