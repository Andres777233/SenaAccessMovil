package com.example.sennaccess.comun.diseno

// Lenguaje de movimiento de la app (Operate: feedback y continuidad, sin coreografía).
// Tesis: press instantáneo 120ms; entradas fade+scale(0.96)+slide 10dp ≤240ms;
// stagger 40ms máx 5 elementos; tabs con eje-X compartido 220ms; salidas 150ms.
// Paleta y colores intactos: aquí solo vive el movimiento.

// Fixed by 965e2c9b69d9ffb6a489d0294a5aefb on 2026-10-03:
// Combined overlapping LaunchedEffects into one; single visible flag drives the
// entrance exactly once (avoids double-run on recomposition).
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

// Retardo escalonado por índice: 40ms por paso, tope 200ms (5 elementos).
fun retardoEscalon(indice: Int): Int = (indice.coerceAtLeast(0) * 40).coerceAtMost(200)

// Entrada suave única: aparece una vez al componerse, sin re-animar en recomposiciones.
@Composable
fun EntradaSuave(
    indice: Int = 0,
    modifier: Modifier = Modifier,
    contenido: @Composable AnimatedVisibilityScope.() -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    // Un solo efecto: enciende la entrada una única vez al entrar a composición.
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(220, retardoEscalon(indice), ResorteIos.EntradaRapida.easing)) +
            scaleIn(
                initialScale = 0.96f,
                animationSpec = tween(240, retardoEscalon(indice), ResorteIos.EntradaNormal.easing)
            ) +
            slideInVertically(
                initialOffsetY = { it / 12 },
                animationSpec = tween(240, retardoEscalon(indice), ResorteIos.EntradaNormal.easing)
            ),
        exit = fadeOut(tween(150)),
        content = contenido
    )
}

// Cambio de pestaña con eje-X compartido: la entrante empuja, la saliente se va rápido.
@Composable
fun <T> ContenidoPestana(
    objetivo: T,
    modifier: Modifier = Modifier,
    contenido: @Composable (T) -> Unit
) {
    AnimatedContent(
        targetState = objetivo,
        modifier = modifier,
        transitionSpec = {
            (fadeIn(tween(220, easing = ResorteIos.EntradaRapida.easing)) +
                slideInVertically(
                    initialOffsetY = { it / 14 },
                    animationSpec = tween(220, easing = ResorteIos.EntradaNormal.easing)
                )).togetherWith(fadeOut(tween(150)))
        },
        label = "pestana"
    ) { estado ->
        Box { contenido(estado) }
    }
}
