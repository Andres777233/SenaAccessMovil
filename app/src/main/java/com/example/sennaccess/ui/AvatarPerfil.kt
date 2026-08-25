package com.example.sennaccess.ui

// Avatar circular de perfil compartido por las pantallas de Aprendiz, Instructor y
// Admin. Carga la foto del servidor con Coil resolviendo rutas relativas contra el
// servidor activo; si no hay foto muestra la inicial del nombre sobre fondo verde.

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sennaccess.data.SessionManager
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen

@Composable
fun AvatarPerfil(fotoPath: String?, nombre: String, tamano: Dp = 80.dp) {
    val colors = LocalAppColors.current
    val url = SessionManager.fotoUrl(fotoPath)
    Box(
        modifier = Modifier
            .size(tamano)
            .clip(CircleShape)
            .background(SenaGreen.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = "Foto de perfil",
                modifier = Modifier.size(tamano),
                contentScale = ContentScale.Crop
            )
        } else {
            val inicial = nombre.trim().firstOrNull()?.uppercase() ?: ""
            if (inicial.isNotBlank()) {
                Text(inicial, color = SenaGreen, fontSize = (tamano.value * 0.45f).sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Person, null, tint = SenaGreen, modifier = Modifier.size(tamano / 2))
            }
        }
    }
}
