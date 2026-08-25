package com.example.sennaccess.ui.theme

// Estado global de los colores de la app. Expone la paleta activa (clara u
// oscura) a cualquier composable sin necesidad de pasar parámetros.
import androidx.compose.runtime.compositionLocalOf

// CompositionLocal que provee la paleta activa. Por defecto usa la oscura y
// puede sobrescribirse con un Provider para cambiar de tema en tiempo de ejecución.
val LocalAppColors = compositionLocalOf { darkAppColors() }
