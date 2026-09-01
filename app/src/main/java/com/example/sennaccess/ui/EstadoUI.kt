package com.example.sennaccess.ui

// Componentes de UI que materializan el patrón CargaUiState: EstadoContenido
// decide entre CargandoBox (Loading), ErrorBox (Error con reintento) o el
// contenido real (Success), centralizando el renderizado de estados en la app.

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

// Estado vacío amable: ícono en círculo verde, título y mensaje de apoyo.
@Composable
fun EstadoVacio(
    icono: ImageVector,
    titulo: String,
    mensaje: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp)) {
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(SenaGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icono, contentDescription = null, tint = SenaGreen, modifier = Modifier.size(30.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(titulo, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text(mensaje, color = colors.textSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
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
