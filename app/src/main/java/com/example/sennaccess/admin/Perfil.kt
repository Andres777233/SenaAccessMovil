package com.example.sennaccess.admin

// Perfil del ADMINISTRADOR (contenido de pestaña).
// Muestra los datos personales provenientes de la API con respaldo a datos de
// ejemplo, con la cabecera compartida (avatar grande + chip de rol) y filas de
// datos con icono. La edición (nombre, correo, contraseña y foto) se abre como
// sub-pantalla con EditarPerfilView mediante el botón EDITAR PERFIL.

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
import com.example.sennaccess.data.UsuarioApi
import com.example.sennaccess.ui.CargaUiState
import com.example.sennaccess.ui.EstadoContenido
import com.example.sennaccess.ui.FilaDato
import com.example.sennaccess.ui.MiHuellaSection
import com.example.sennaccess.ui.PerfilHeader
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import com.example.sennaccess.ui.ios.GlassCornerRadius
import com.example.sennaccess.ui.ios.IosCollapsibleHeader
import com.example.sennaccess.ui.ios.glassSurface

/**
 * Perfil del ADMINISTRADOR (contenido de pestaña).
 * Los datos (nombre/correo) llegan desde la API (GET /user) con respaldo a ejemplo.
 */
@Composable
fun PerfilContent(
    perfil: CargaUiState<UsuarioApi>,
    onBack: () -> Unit,
    onReintentar: () -> Unit,
    onEditar: () -> Unit
) {
    val colors = LocalAppColors.current
    val scrollState = rememberScrollState()

    // Contenedor desplazable centrado con el contenido del perfil.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Encabezado de la pantalla de perfil.
        IosCollapsibleHeader(
            title = "Perfil",
            subtitle = "Información personal y seguridad",
            scrollOffset = scrollState.value.toFloat()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // EstadoContenido gestiona carga/error/éxito; con los datos del usuario
        // se pinta la cabecera y las filas de información personal.
        EstadoContenido(estado = perfil, onReintentar = onReintentar) { usuario ->
            // Tarjeta de vidrio con cabecera, datos y acción de edición.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassSurface(cornerRadius = GlassCornerRadius)
                    .padding(20.dp)
            ) {
                PerfilHeader(
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
                // Botón que abre la sub-pantalla de edición (datos + foto).
                Button(
                    onClick = onEditar,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("EDITAR PERFIL", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Gestión de la huella dactilar local: registrar, ver estado y eliminar.
            MiHuellaSection()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
