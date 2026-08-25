package com.example.sennaccess.ui.ios

// Física de scroll con rebote estilo iOS para columnas y listas verticales:
// al llegar a un extremo el contenido se "estira" y regresa con un spring.
// Se aplica a cualquier pantalla scrolleable de la app.
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Física de scroll elástico tipo iOS (rebote en los extremos / "bouncing physics").
 *
 * Implementa un [NestedScrollConnection] que:
 *  - Consume el "overscroll" cuando ya no se puede seguir desplazando.
 *  - Desplaza visualmente el contenido (translationY) con resistencia.
 *  - Al soltar, regresa con un spring elástico suave (60 FPS).
 *
 * Uso:
 *   val scroll = rememberScrollState()
 *   val bouncy = rememberBouncyScrollState(scroll)
 *   Column(Modifier.bouncyScroll(bouncy).verticalScroll(scroll)) { ... }
 */
class BouncyScrollState internal constructor(
    private val scope: CoroutineScope,
    private val canScrollForward: () -> Boolean,
    private val canScrollBackward: () -> Boolean
) {
    /** Desplazamiento visual actual del overscroll (px). */
    val overscroll = Animatable(0f)

    // El connection traduce los eventos de scroll del contenedor en overscroll visual.
    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val current = overscroll.value
            if (current == 0f) return Offset.Zero

            // Si hay overscroll acumulado y el usuario hace scroll en dirección
            // contraria, lo reducimos primero (sensación natural).
            val delta = available.y
            return if ((current > 0f && delta < 0f) || (current < 0f && delta > 0f)) {
                val consumed = (current + delta).coerceIn(
                    if (current > 0f) 0f..current else current..0f
                )
                val used = consumed - current
                scope.launch { overscroll.snapTo(consumed) }
                Offset(0f, used)
            } else Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            val leftOver = available.y
            if (leftOver == 0f) return Offset.Zero

            // Aplica resistencia al overscroll (se "estira" menos cuanto más tiras).
            val resistance = 0.35f
            val newOverscroll = overscroll.value + leftOver * resistance
            scope.launch { overscroll.snapTo(newOverscroll) }
            return Offset(0f, leftOver)
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            // Al soltar / hacer fling, regresamos a 0 con spring elástico.
            if (overscroll.value != 0f) {
                overscroll.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            return super.onPreFling(available)
        }
    }

    /** Devuelve el contenido a su sitio al cancelar gesto. */
    suspend fun release() {
        if (overscroll.value != 0f) {
            overscroll.animateTo(
                0f,
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }
}

/** Crea el estado de scroll con rebote a partir de un [ScrollState]. */
@Composable
fun rememberBouncyScrollState(scrollState: ScrollState): BouncyScrollState {
    val scope = rememberCoroutineScope()
    return remember(scrollState) {
        BouncyScrollState(
            scope = scope,
            canScrollForward = { scrollState.canScrollForward },
            canScrollBackward = { scrollState.canScrollBackward }
        )
    }
}

/** Variante para estados genéricos (LazyColumn, etc.). */
@Composable
fun rememberBouncyScrollState(scrollableState: ScrollableState): BouncyScrollState {
    val scope = rememberCoroutineScope()
    return remember(scrollableState) {
        BouncyScrollState(
            scope = scope,
            canScrollForward = { scrollableState.canScrollForward },
            canScrollBackward = { scrollableState.canScrollBackward }
        )
    }
}

/**
 * Aplica el efecto de rebote iOS al contenedor scrolleable.
 * Colócalo ANTES del `verticalScroll`/`lazy` para que traduzca el contenido.
 */
fun Modifier.bouncyScroll(state: BouncyScrollState): Modifier = this
    .nestedScroll(state.connection)
    .graphicsLayer {
        translationY = state.overscroll.value
    }
