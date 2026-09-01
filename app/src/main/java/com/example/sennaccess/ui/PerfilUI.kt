package com.example.sennaccess.ui

// Componentes visuales del perfil compartidos por los tres roles (Aprendiz,
// Instructor y Admin): cabecera con avatar grande y chip de rol, y filas de
// datos con icono que reemplazan las listas planas de texto del perfil.

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen

// Cabecera del perfil: avatar grande con anillo y brillo verde, nombre del
// usuario y su rol como chip. Se usa en las tres pantallas de perfil.
@Composable
fun PerfilHeader(fotoPath: String?, nombre: String, rol: String) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar de 116dp con sombra verde y anillo de marca.
        Box(
            modifier = Modifier
                .size(116.dp)
                .shadow(18.dp, CircleShape, spotColor = SenaGreen, ambientColor = SenaGreen.copy(alpha = 0.35f))
                .border(2.dp, SenaGreen.copy(alpha = 0.55f), CircleShape)
                .clip(CircleShape)
        ) {
            AvatarPerfil(fotoPath = fotoPath, nombre = nombre, tamano = 112.dp)
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            nombre,
            color = colors.textPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Chip del rol con icono de identificación SENA.
        Row(
            modifier = Modifier
                .border(1.dp, SenaGreen.copy(alpha = 0.55f), RoundedCornerShape(50))
                .background(SenaGreen.copy(alpha = 0.12f), RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Badge, null, tint = SenaGreen, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                rol.uppercase(),
                color = SenaGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// Fila etiqueta/valor del perfil con icono dentro de una caja circular verde.
// Reemplaza las filas planas de texto y da más jerarquía a cada dato.
@Composable
fun FilaDato(icono: ImageVector, label: String, valor: String) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Caja circular con el icono del dato (correo, documento, ficha, programa).
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(SenaGreen.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, null, tint = SenaGreen, modifier = Modifier.size(21.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label.uppercase(),
                color = colors.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp
            )
            Text(
                valor.ifBlank { "—" },
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
