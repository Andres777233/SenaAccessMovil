package com.example.sennaccess.comun.diseno

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

class EstadoDesplazamientoElastico internal constructor(
    private val scope: CoroutineScope,
    private val canScrollForward: () -> Boolean,
    private val canScrollBackward: () -> Boolean
) {
    val overscroll = Animatable(0f)

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val current = overscroll.value
            if (current == 0f) return Offset.Zero

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

            val resistance = 0.35f
            val newOverscroll = overscroll.value + leftOver * resistance
            scope.launch { overscroll.snapTo(newOverscroll) }
            return Offset(0f, leftOver)
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
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

@Composable
fun recordarEstadoRebote(scrollState: ScrollState): EstadoDesplazamientoElastico {
    val scope = rememberCoroutineScope()
    return remember(scrollState) {
        EstadoDesplazamientoElastico(
            scope = scope,
            canScrollForward = { scrollState.canScrollForward },
            canScrollBackward = { scrollState.canScrollBackward }
        )
    }
}

@Composable
fun recordarEstadoRebote(scrollableState: ScrollableState): EstadoDesplazamientoElastico {
    val scope = rememberCoroutineScope()
    return remember(scrollableState) {
        EstadoDesplazamientoElastico(
            scope = scope,
            canScrollForward = { scrollableState.canScrollForward },
            canScrollBackward = { scrollableState.canScrollBackward }
        )
    }
}

fun Modifier.desplazamientoRebote(state: EstadoDesplazamientoElastico): Modifier = this
    .nestedScroll(state.connection)
    .graphicsLayer {
        translationY = state.overscroll.value
    }
