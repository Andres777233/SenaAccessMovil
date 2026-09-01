package com.example.sennaccess.admin

// Contenido de la pantalla ACCESO APRENDICES del ADMINISTRADOR.
// Lista los registros de ingreso de aprendices consumidos desde la API
// (GET /admin/ingresos + /admin/users) a traves del AdminDashboardViewModel.

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.ui.CargaUiState
import com.example.sennaccess.ui.EstadoContenido
import com.example.sennaccess.ui.EstadoVacio
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import com.example.sennaccess.ui.ios.GlassCornerRadius
import com.example.sennaccess.ui.ios.IosCollapsibleHeader
import com.example.sennaccess.ui.ios.glassSurface

/**
 * Pantalla de acceso de APRENDICES del ADMINISTRADOR (sub-pantalla).
 * Consume el historial real del ViewModel y filtra solo los aprendices.
 */
@Composable
fun AccesoAprendicesContent(
    estado: CargaUiState<HistorialAdminData>,
    onReintentar: () -> Unit,
    onBack: () -> Unit,
    onNavigate: (AdminScreen) -> Unit
) {
    val colors = LocalAppColors.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Boton de retroceso para volver al historial general.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = SenaGreen
                )
            }
        }

        // Encabezado de la pantalla de accesos de aprendices.
        IosCollapsibleHeader(
            title = "Accesos de Aprendices",
            subtitle = "Registro de ingresos de aprendices al centro",
            scrollOffset = scrollState.value.toFloat()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // EstadoContenido gestiona carga/error/éxito; filtra solo aprendices.
        EstadoContenido(estado = estado, onReintentar = onReintentar) { data ->
            val registros = data.aprendices

            if (registros.isEmpty()) {
                EstadoVacio(
                    icono = Icons.Default.School,
                    titulo = "No hay registros de aprendices",
                    mensaje = "Los accesos de los aprendices aparecerán aquí."
                )
            } else {
                registros.forEach { registro ->
                    TarjetaAccesoAdmin(
                        nombre = registro.nombre,
                        rol = registro.rol,
                        hora = registro.hora,
                        tipo = registro.tipo
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Acceso directo a la pantalla de registros de instructores.
            TextButton(onClick = { onNavigate(AdminScreen.ACCESO_INSTRUCTORES) }) {
                Text("Ver registro de instructores >", color = SenaGreen)
            }
        }
    }
}
