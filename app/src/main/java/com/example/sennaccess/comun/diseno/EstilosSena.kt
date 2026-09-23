package com.example.sennaccess.comun.diseno

// Tokens del Design System SenaAccess: única fuente de verdad para espaciado,
// radios, tamaños táctiles y elevaciones. Los colores NO se tocan (decisión
// del dueño: verde 0xFF02D914 idéntico en ambos temas).
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Espaciado vertical/horizontal permitido en toda la app.
object EspaciadoSena {
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 20.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val screenH: Dp = 16.dp
    val screenV: Dp = 16.dp
    val dockClearance: Dp = 120.dp
}

object RadioSena {
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val pill: Dp = 28.dp
}

object ToqueSena {
    val min: Dp = 48.dp
    val buttonH: Dp = 52.dp
}

object ElevacionSena {
    val card: Dp = 12.dp
    val cardElevated: Dp = 20.dp
    val dock: Dp = 18.dp
    val dialog: Dp = 16.dp
}
