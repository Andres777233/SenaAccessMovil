package com.example.sennaccess.perfil

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Shield
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
import com.example.sennaccess.comun.diseno.BotonBordeBrillante
import com.example.sennaccess.comun.diseno.BotonPrimarioNeon
import com.example.sennaccess.comun.diseno.RadioSena
import com.example.sennaccess.comun.diseno.superficiePlana
import com.example.sennaccess.comun.diseno.superficieVidrio
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
@Composable
fun CabeceraPerfil(fotoPath: String?, nombre: String, rol: String) {
    val colors = ColoresAppLocal.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(116.dp)
                .shadow(18.dp, CircleShape, spotColor = VerdeSena, ambientColor = VerdeSena.copy(alpha = 0.35f))
                .border(2.dp, VerdeSena.copy(alpha = 0.55f), CircleShape)
                .clip(CircleShape)
        ) {
            FotoPerfil(fotoPath = fotoPath, nombre = nombre, tamano = 112.dp)
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
        Row(
            modifier = Modifier
                .border(1.dp, VerdeSena.copy(alpha = 0.55f), RoundedCornerShape(50))
                .background(VerdeSena.copy(alpha = 0.12f), RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Badge, null, tint = verdeMarca(), modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                rol.uppercase(),
                color = verdeMarca(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun FilaDato(icono: ImageVector, label: String, valor: String) {
    val colors = ColoresAppLocal.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(VerdeSena.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, null, tint = verdeMarca(), modifier = Modifier.size(21.dp))
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

// Perfil senior compartido por los 4 roles: hero profesional con placa de
// rol, avatar con anillo y estado, datos en plana con divisor y acciones 16dp.
@Composable
fun PerfilSenior(
    fotoPath: String?,
    nombre: String,
    rol: String,
    correo: String?,
    filas: List<Triple<ImageVector, String, String>>,
    onEditar: () -> Unit,
    onConfigurar2Fa: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ColoresAppLocal.current
    Column(modifier = modifier.fillMaxWidth()) {
        // Hero: placa superior con eyebrow + avatar 88dp + nombre + correo + chip.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .superficieVidrio(cornerRadius = com.example.sennaccess.comun.diseno.RadioVidrio, elevated = true)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CUENTA ${rol.uppercase()}",
                color = verdeMarca(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.0.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(14.dp, CircleShape, spotColor = VerdeSena, ambientColor = VerdeSena.copy(alpha = 0.3f))
                    .border(2.dp, VerdeSena.copy(alpha = 0.6f), CircleShape)
                    .clip(CircleShape)
            ) {
                FotoPerfil(fotoPath = fotoPath, nombre = nombre, tamano = 92.dp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = nombre,
                color = colors.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2
            )
            if (!correo.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = correo,
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(VerdeSena.copy(alpha = 0.14f))
                    .border(1.dp, VerdeSena.copy(alpha = 0.4f), RoundedCornerShape(100.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(VerdeSena)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${rol.uppercase()} • ACTIVO",
                    color = verdeMarca(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.0.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "INFORMACIÓN",
            color = colors.textSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.8.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .superficiePlana(cornerRadius = RadioSena.lg)
                .padding(vertical = 6.dp)
        ) {
            filas.forEachIndexed { i, (icono, etiqueta, valor) ->
                if (i > 0) androidx.compose.material3.HorizontalDivider(
                    color = colors.divider,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)) {
                    FilaDato(icono, etiqueta, valor)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "SEGURIDAD",
            color = colors.textSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.8.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        BotonPrimarioNeon(
            text = "EDITAR PERFIL",
            icon = Icons.Default.Edit,
            onClick = onEditar,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        BotonBordeBrillante(
            text = "VERIFICACIÓN EN DOS PASOS",
            icon = Icons.Default.Shield,
            onClick = onConfigurar2Fa,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
