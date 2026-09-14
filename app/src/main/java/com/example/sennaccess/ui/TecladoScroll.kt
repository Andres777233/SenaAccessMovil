package com.example.sennaccess.ui

// Helper de teclado amable: cuando un campo de texto recibe el foco, pide al
// contenedor con scroll que lo deje visible en vez de quedarse mirando arriba.
// Úsalo en cada OutlinedTextField de formularios (login, registro, contraseñas,
// perfil, códigos) junto con imePadding() en la columna con scroll: así el
// contenido se encoge sobre el teclado y el scroll sigue al dedo mientras escribes.

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Lleva el campo enfocado a la zona visible (con una pausa breve para que el
// teclado termine de abrirse); si no hay scroll ancestro, no hace nada malo.
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.campoVisible(): Modifier = composed {
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    bringIntoViewRequester(requester).onFocusEvent { estado ->
        if (estado.isFocused) {
            scope.launch {
                delay(350)
                try {
                    requester.bringIntoView()
                } catch (_: Exception) { /* sin scroll ancestro: se ignora */ }
            }
        }
    }
}
