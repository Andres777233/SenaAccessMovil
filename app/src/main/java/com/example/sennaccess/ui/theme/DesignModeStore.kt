package com.example.sennaccess.ui.theme

// Store global del modo de diseño (original vs renovado).
// Vive fuera de la navegación para que cualquier menú hamburguesa pueda
// alternarlo sin cambiar firmas ni lógica de pantallas. Solo afecta lo visual.
import androidx.compose.runtime.mutableStateOf

object DesignModeStore {
    // Estado observable: al cambiar se recomponen MainActivity y los menús.
    val mode = mutableStateOf(DesignMode.ORIGINAL)

    // Alterna entre el diseño original y el renovado.
    fun toggle() {
        mode.value = if (mode.value == DesignMode.ORIGINAL) DesignMode.RENOVADO else DesignMode.ORIGINAL
    }
}
