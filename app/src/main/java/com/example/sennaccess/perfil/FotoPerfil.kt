package com.example.sennaccess.perfil

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
import com.example.sennaccess.datos.sesion.GestorSesion
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
@Composable
fun FotoPerfil(fotoPath: String?, nombre: String, tamano: Dp = 80.dp) {
    val colors = ColoresAppLocal.current
    val url = GestorSesion.fotoUrl(fotoPath)
    Box(
        modifier = Modifier
            .size(tamano)
            .clip(CircleShape)
            .background(VerdeSena.copy(alpha = 0.15f)),
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
                Text(inicial, color = verdeMarca(), fontSize = (tamano.value * 0.45f).sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Person, null, tint = verdeMarca(), modifier = Modifier.size(tamano / 2))
            }
        }
    }
}
