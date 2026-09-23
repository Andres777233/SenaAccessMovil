package com.example.sennaccess.administrador.panel

// Perfil del ADMINISTRADOR (contenido de pestaña).
// Muestra los datos personales provenientes de la API con respaldo a datos de
// ejemplo, con la cabecera compartida (avatar grande + chip de rol) y filas de
// datos con icono. La edición (nombre, correo, contraseña y foto) se abre como
// sub-pantalla con VistaEditarPerfil mediante el botón EDITAR PERFIL.

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.datos.modelos.UsuarioApi
import com.example.sennaccess.comun.EstadoCarga
import com.example.sennaccess.comun.EstadoContenido
import com.example.sennaccess.perfil.FilaDato
import com.example.sennaccess.biometria.MiSeccionHuella
import com.example.sennaccess.perfil.CabeceraPerfil
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.diseno.RadioVidrio
import com.example.sennaccess.comun.diseno.CabeceraPlegable
import com.example.sennaccess.comun.diseno.superficieVidrio
// Los datos (nombre/correo) llegan desde la API (GET /user) con respaldo a ejemplo.
@Composable
fun ContenidoPerfilAdmin(
    perfil: EstadoCarga<UsuarioApi>,
    onBack: () -> Unit,
    onReintentar: () -> Unit,
    onEditar: () -> Unit,
    onConfigurar2Fa: () -> Unit
) {
    val colors = ColoresAppLocal.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CabeceraPlegable(
            title = "Perfil",
            subtitle = "Información personal y seguridad",
            scrollOffset = scrollState.value.toFloat()
        )

        Spacer(modifier = Modifier.height(16.dp))

        EstadoContenido(estado = perfil, onReintentar = onReintentar) { usuario ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .superficieVidrio(cornerRadius = RadioVidrio)
                    .padding(20.dp)
            ) {
                CabeceraPerfil(
                    fotoPath = usuario.profile_photo_path,
                    nombre = usuario.nombreCompleto,
                    rol = "Administrador"
                )
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = colors.border)
                Spacer(modifier = Modifier.height(4.dp))
                FilaDato(Icons.Default.Email, "Correo", usuario.user_email ?: "—")
                FilaDato(Icons.Default.Badge, "Documento", usuario.user_identification ?: "—")
                if (!usuario.user_program.isNullOrBlank()) {
                    FilaDato(Icons.Default.School, "Programa", usuario.user_program!!)
                }
                if (usuario.user_coursenumber != null && usuario.user_coursenumber > 0) {
                    FilaDato(Icons.Default.Numbers, "Ficha", usuario.user_coursenumber.toString())
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onEditar,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeSena, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("EDITAR PERFIL", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onConfigurar2Fa,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Default.Shield, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("VERIFICACIÓN EN DOS PASOS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            MiSeccionHuella()
            Spacer(modifier = Modifier.height(24.dp))
            Spacer(modifier = Modifier.imePadding().height(96.dp))
        }
    }
}
