package com.example.sennaccess.administrador.novedades

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
import com.example.sennaccess.comun.tema.RojoError
import com.example.sennaccess.comun.campoVisible
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import com.example.sennaccess.comun.diseno.RadioVidrio
import com.example.sennaccess.comun.diseno.CabeceraPlegable
import com.example.sennaccess.comun.diseno.superficieVidrio
import com.example.sennaccess.comun.diseno.superficiePlana
import com.example.sennaccess.comun.diseno.escalaPresion
import com.example.sennaccess.administrador.panel.PantallaAdmin

@Composable
fun ContenidoReporteNovedades(onNavigate: (PantallaAdmin) -> Unit) {
    val colors = ColoresAppLocal.current
    var elemento by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf("") }
    var hora by remember { mutableStateOf("") }
    var accesorio by remember { mutableStateOf("") }
    var propietario by remember { mutableStateOf("") }
    var admin by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .imePadding()
    ) {
        CabeceraPlegable(
            title = "Reporte de Novedades",
            subtitle = "Registro de elementos entregados al centro",
            scrollOffset = scrollState.value.toFloat()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Ficha única en plana: 5 campos con divisor, un aviso y un Enviar.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .superficiePlana(cornerRadius = RadioVidrio)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            EtiquetaReporte("Elemento")
            CampoReporte("Elemento", elemento, { elemento = it })
            DividerReporte()
            EtiquetaReporte("Fecha y hora")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = fecha, onValueChange = { fecha = it }, label = { Text("Fecha") }, modifier = Modifier.weight(1f).campoVisible(), colors = campoRepColors())
                OutlinedTextField(value = hora, onValueChange = { hora = it }, label = { Text("Hora") }, modifier = Modifier.weight(1f).campoVisible(), colors = campoRepColors())
            }
            DividerReporte()
            EtiquetaReporte("Accesorio y propietario")
            CampoReporte("Accesorio Adicional", accesorio, { accesorio = it })
            Spacer(modifier = Modifier.height(12.dp))
            CampoReporte("Propietario", propietario, { propietario = it })
            DividerReporte()
            EtiquetaReporte("Registra")
            CampoReporte("Administrador que Registra", admin, { admin = it })
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.errorBackground, RoundedCornerShape(16.dp))
                .border(1.dp, RojoError.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Text(
                "AVISO - El Centro De Servicio Y Comercio no se hace responsable por objetos de valor no reportados en este comprobante.",
                color = RojoError, fontSize = 12.sp, textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { onNavigate(PantallaAdmin.PANEL) },
            modifier = Modifier.fillMaxWidth().height(52.dp).escalaPresion(pressedScale = 0.97f),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = Color.Black)
        ) { Text("Enviar", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun EtiquetaReporte(texto: String) {
    Text(
        text = texto.uppercase(),
        color = verdeMarca(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.6.sp,
        modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
    )
}

@Composable
private fun DividerReporte() {
    HorizontalDivider(
        color = ColoresAppLocal.current.divider,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun CampoReporte(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().campoVisible(),
        colors = campoRepColors()
    )
}

@Composable
private fun campoRepColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = verdeMarca(), unfocusedBorderColor = ColoresAppLocal.current.textSecondary,
    focusedLabelColor = verdeMarca(), unfocusedLabelColor = ColoresAppLocal.current.textSecondary,
    cursorColor = verdeMarca(), focusedTextColor = ColoresAppLocal.current.textPrimary, unfocusedTextColor = ColoresAppLocal.current.textPrimary
)
