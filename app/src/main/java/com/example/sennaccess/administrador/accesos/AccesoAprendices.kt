package com.example.sennaccess.administrador.accesos

// Contenido de la pantalla ACCESO APRENDICES del ADMINISTRADOR.
// Lista los registros de ingreso de aprendices consumidos desde la API
// (GET /admin/ingresos + /admin/users) a traves del PanelAdministradorViewModel.

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.comun.EstadoCarga
import com.example.sennaccess.comun.EstadoContenido
import com.example.sennaccess.comun.VistaVacia
import com.example.sennaccess.comun.diseno.FiltroSena
import com.example.sennaccess.comun.diseno.BuscadorSena
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import com.example.sennaccess.comun.diseno.RadioVidrio
import com.example.sennaccess.comun.diseno.CabeceraPlegable
import com.example.sennaccess.comun.diseno.superficieVidrio
import com.example.sennaccess.administrador.panel.DatosHistorial
import com.example.sennaccess.administrador.panel.PantallaAdmin

@Composable
fun ContenidoAccesoAprendices(
    estado: EstadoCarga<DatosHistorial>,
    onReintentar: () -> Unit,
    onBack: () -> Unit,
    onNavigate: (PantallaAdmin) -> Unit
) {
    val colors = ColoresAppLocal.current
    val scrollState = rememberScrollState()
    var busqueda by remember { mutableStateOf("") }
    var filtroTipo by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = verdeMarca()
                )
            }
        }

        CabeceraPlegable(
            title = "Accesos de Aprendices",
            subtitle = "Registro de ingresos de aprendices al centro",
            scrollOffset = scrollState.value.toFloat()
        )

        Spacer(modifier = Modifier.height(16.dp))

        BuscadorSena(valor = busqueda, onValor = { busqueda = it }, placeholder = "Buscar aprendiz por nombre...")
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FiltroSena(texto = "Todos", seleccionado = filtroTipo == 0, onClick = { filtroTipo = 0 })
            FiltroSena(texto = "Entradas", seleccionado = filtroTipo == 1, onClick = { filtroTipo = 1 })
            FiltroSena(texto = "Salidas", seleccionado = filtroTipo == 2, onClick = { filtroTipo = 2 })
        }
        Spacer(modifier = Modifier.height(12.dp))

        EstadoContenido(estado = estado, onReintentar = onReintentar) { data ->
            val q = busqueda.trim()
            val registros = data.aprendices.filter { r ->
                (filtroTipo == 0 || (filtroTipo == 1 && r.tipo.equals("Entrada", ignoreCase = true)) || (filtroTipo == 2 && r.tipo.equals("Salida", ignoreCase = true))) &&
                    (q.isBlank() || r.nombre.contains(q, ignoreCase = true))
            }
            Text("${registros.size} registros", color = colors.textSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            if (registros.isEmpty()) {
                VistaVacia(
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

            TextButton(onClick = { onNavigate(PantallaAdmin.ACCESO_INSTRUCTORES) }) {
                Text("Ver registro de instructores >", color = verdeMarca())
            }
        }
    }
}
