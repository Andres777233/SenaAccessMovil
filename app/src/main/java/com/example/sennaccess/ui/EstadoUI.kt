package com.example.sennaccess.ui

// Componentes de UI que materializan el patrón CargaUiState: EstadoContenido
// decide entre CargandoBox (Loading), ErrorBox (Error con reintento) o el
// contenido real (Success), centralizando el renderizado de estados en la app.

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen

// Puerta de entrada única al patrón: cada pantalla pasa su CargaUiState y el contenido real.
/**
 * Renderiza un [CargaUiState]: Loading (spinner), Error (mensaje + reintentar)
 * o Success (contenido real). Centraliza el patrón en todas las pantallas.
 */
@Composable
fun <T> EstadoContenido(
    estado: CargaUiState<T>,
    onReintentar: () -> Unit,
    content: @Composable (T) -> Unit
) {
    when (estado) {
        is CargaUiState.Loading -> CargandoBox()
        is CargaUiState.Error -> ErrorBox(estado.mensaje, onReintentar)
        is CargaUiState.Success -> content(estado.datos)
    }
}

// Indicador de progreso centrado que se muestra mientras dura la carga.
@Composable
fun CargandoBox() {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = SenaGreen, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("Cargando...", color = colors.textSecondary, fontSize = 13.sp)
        }
    }
}

// Mensaje de error con el detalle y un botón para reintentar la operación.
@Composable
fun ErrorBox(mensaje: String, onReintentar: () -> Unit) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
            Text(
                "No se pudo cargar la información",
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                mensaje,
                color = colors.textSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onReintentar,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SenaGreen)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reintentar", fontWeight = FontWeight.Bold)
            }
        }
    }
}
