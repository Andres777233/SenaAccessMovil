package com.example.sennaccess.ui.theme

// Estado global de los colores de la app. Expone la paleta activa (clara u
// oscura) a cualquier composable sin necesidad de pasar parámetros.
import androidx.compose.runtime.compositionLocalOf

// Modo de diseño: permite alternar entre el diseño original y uno renovado
enum class DesignMode {
    ORIGINAL, RENOVADO
}

// CompositionLocal que provee la paleta activa. Por defecto usa la oscura y
// puede sobrescribirse con un Provider para cambiar de tema en tiempo de ejecución.
val LocalAppColors = compositionLocalOf { darkAppColors() }

// CompositionLocal para el modo de diseño
val LocalDesignMode = compositionLocalOf { DesignMode.ORIGINAL }
