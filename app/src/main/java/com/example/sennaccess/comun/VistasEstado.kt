package com.example.sennaccess.comun

// Componentes de UI que materializan el patrón EstadoCarga: EstadoContenido
// decide entre CajaCargando (Loading), CajaError (Error con reintento) o el
// contenido real (Success), centralizando el renderizado de estados en la app.

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca

@Composable
fun <T> EstadoContenido(
    estado: EstadoCarga<T>,
    onReintentar: () -> Unit,
    content: @Composable (T) -> Unit
) {
    when (estado) {
        is EstadoCarga.Loading -> CajaCargando()
        is EstadoCarga.Error -> CajaError(estado.mensaje, onReintentar)
        is EstadoCarga.Success -> content(estado.datos)
    }
}

@Composable
fun CajaCargando() {
    val colors = ColoresAppLocal.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = verdeMarca(), modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("Cargando...", color = colors.textSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
fun VistaVacia(
    icono: ImageVector,
    titulo: String,
    mensaje: String,
    modifier: Modifier = Modifier
) {
    val colors = ColoresAppLocal.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp)) {
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(VerdeSena.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icono, contentDescription = null, tint = verdeMarca(), modifier = Modifier.size(30.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(titulo, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text(mensaje, color = colors.textSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun CajaError(mensaje: String, onReintentar: () -> Unit) {
    val colors = ColoresAppLocal.current
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
                modifier = Modifier.heightIn(min = 48.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = verdeMarca())
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reintentar carga", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reintentar", fontWeight = FontWeight.Bold)
            }
        }
    }
}
