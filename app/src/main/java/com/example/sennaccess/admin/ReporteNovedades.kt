package com.example.sennaccess.admin

// Formulario de REPORTE DE NOVEDADES del ADMINISTRADOR (pestaña NOVEDADES).
// Registra elementos/accesorios entregados al centro, con aviso de
// responsabilidad; al enviar navega de vuelta al panel de inicio.

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.ui.theme.ErrorRed
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import com.example.sennaccess.ui.ios.GlassCornerRadius
import com.example.sennaccess.ui.ios.IosCollapsibleHeader
import com.example.sennaccess.ui.ios.glassSurface
import com.example.sennaccess.ui.ios.pressScale

/**
 * Formulario de novedades del ADMINISTRADOR (contenido de pestaña).
 */
@Composable
fun ReporteNovedadesContent(onNavigate: (AdminScreen) -> Unit) {
    val colors = LocalAppColors.current
    // Estado local de los campos: elemento, fecha/hora, accesorio, propietario y admin.
    var elemento by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf("") }
    var hora by remember { mutableStateOf("") }
    var accesorio by remember { mutableStateOf("") }
    var propietario by remember { mutableStateOf("") }
    var admin by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    // Contenedor del formulario desplazable.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Encabezado de la pantalla de novedades.
        IosCollapsibleHeader(
            title = "Reporte de Novedades",
            subtitle = "Registro de elementos entregados al centro",
            scrollOffset = scrollState.value.toFloat()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Cada campo se agrupa en su propia tarjeta de vidrio para separarlos visualmente.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .glassSurface(cornerRadius = GlassCornerRadius)
                .padding(16.dp)
        ) { CampoReporte("Elemento", elemento, { elemento = it }) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .glassSurface(cornerRadius = GlassCornerRadius)
                .padding(16.dp)
        ) {
            // Fecha y hora del reporte en fila de dos columnas.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = fecha, onValueChange = { fecha = it }, label = { Text("Fecha") }, modifier = Modifier.weight(1f), colors = campoRepColors())
                OutlinedTextField(value = hora, onValueChange = { hora = it }, label = { Text("Hora") }, modifier = Modifier.weight(1f), colors = campoRepColors())
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .glassSurface(cornerRadius = GlassCornerRadius)
                .padding(16.dp)
        ) { CampoReporte("Accesorio Adicional", accesorio, { accesorio = it }) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .glassSurface(cornerRadius = GlassCornerRadius)
                .padding(16.dp)
        ) { CampoReporte("Propietario", propietario, { propietario = it }) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .glassSurface(cornerRadius = GlassCornerRadius)
                .padding(16.dp)
        ) { CampoReporte("Administrador que Registra", admin, { admin = it }) }
        Spacer(modifier = Modifier.height(6.dp))
        // Aviso legal: el centro no se responsabiliza por objetos de valor no reportados.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(10.dp, RoundedCornerShape(12.dp))
                .background(colors.errorBackground, RoundedCornerShape(12.dp))
                .border(1.dp, ErrorRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Text(
                "AVISO - El Centro De Servicio Y Comercio no se hace responsable por objetos de valor no reportados en este comprobante.",
                color = ErrorRed, fontSize = 12.sp, textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Botón Enviar: por ahora vuelve al panel (registro del reporte).
        Button(
            onClick = { onNavigate(AdminScreen.PANEL) },
            modifier = Modifier.fillMaxWidth().height(50.dp).pressScale(pressedScale = 0.97f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = colors.textPrimary)
        ) { Text("Enviar", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

// Campo de texto reutilizable de este formulario.
@Composable
private fun CampoReporte(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        colors = campoRepColors()
    )
}

// Paleta de colores común para los campos del formulario de novedades.
@Composable
private fun campoRepColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SenaGreen, unfocusedBorderColor = LocalAppColors.current.textSecondary,
    focusedLabelColor = SenaGreen, unfocusedLabelColor = LocalAppColors.current.textSecondary,
    cursorColor = SenaGreen, focusedTextColor = LocalAppColors.current.textPrimary, unfocusedTextColor = LocalAppColors.current.textPrimary
)
