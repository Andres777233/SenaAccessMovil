package com.example.sennaccess.administrador.prediccion

// Pronóstico de accesos diarios del superadmin (GET /api/admin/prediccion).

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.comun.CajaCargando
import com.example.sennaccess.comun.CajaError
import com.example.sennaccess.comun.VistaVacia
import com.example.sennaccess.comun.detalleHttp
import com.example.sennaccess.comun.diseno.CabeceraPantalla
import com.example.sennaccess.comun.diseno.GrupoSeccion
import com.example.sennaccess.comun.diseno.superficiePlana
import com.example.sennaccess.comun.diseno.superficieVidrio
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.verdeMarca
import com.example.sennaccess.datos.modelos.RespuestaPrediccion
import com.example.sennaccess.datos.repositorios.RepositorioPrediccion
import com.example.sennaccess.datos.sesion.GestorSesion
import kotlinx.coroutines.launch

// Fecha YYYY-MM-DD a "14 oct".
private fun fechaCorta(fecha: String): String {
    val partes = fecha.split("-")
    if (partes.size != 3) return fecha
    val meses = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
    val mes = partes[1].toIntOrNull()?.let { meses.getOrNull(it - 1) } ?: return fecha
    return "${partes[2].trimStart('0')} $mes"
}

@Composable
fun VistaPrediccion(onBack: () -> Unit) {
    val colors = ColoresAppLocal.current
    val scope = rememberCoroutineScope()
    val repo = remember { RepositorioPrediccion() }

    var cargando by remember { mutableStateOf(true) }
    var respuesta by remember { mutableStateOf<RespuestaPrediccion?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun cargar() {
        val token = GestorSesion.token
        if (token == null) {
            error = "Sesión expirada. Inicia sesión de nuevo."
            cargando = false
            return
        }
        scope.launch {
            cargando = true
            error = null
            try {
                respuesta = repo.obtener(token)
            } catch (e: retrofit2.HttpException) {
                error = detalleHttp(e)
            } catch (e: Exception) {
                error = "Error de conexión: ${e.message ?: e.javaClass.simpleName}"
            } finally {
                cargando = false
            }
        }
    }

    LaunchedEffect(Unit) { cargar() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = colors.textPrimary)
            }
            CabeceraPantalla(
                eyebrow = "Pronóstico",
                titulo = "Accesos previstos",
                subtitulo = "Cuánta gente entrará al centro",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        when {
            cargando -> CajaCargando()
            error != null -> CajaError(error!!, ::cargar)
            respuesta?.aviso != null -> VistaVacia(
                icono = Icons.Default.TrendingUp,
                titulo = "Aún sin pronóstico",
                mensaje = respuesta?.aviso ?: "Se necesitan más días con movimientos."
            )
            else -> {
                val datos = respuesta
                val lista = datos?.predicciones.orEmpty()
                val manana = lista.firstOrNull()
                if (manana != null) {
                    // Cifra protagonista: mañana.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .superficieVidrio(elevated = true)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("MAÑANA ≈ ${manana.predicho}", color = verdeMarca(), fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
                            Text("Entre ${manana.minimo} y ${manana.maximo} personas", color = colors.textSecondary, fontSize = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
                GrupoSeccion(titulo = "Próximos días", conteo = lista.size, modifier = Modifier.fillMaxWidth()) {
                    lista.forEachIndexed { i, d ->
                        if (i > 0) HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = 14.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${d.dia} · ${fechaCorta(d.fecha)}", color = colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Entre ${d.minimo} y ${d.maximo}", color = colors.textSecondary, fontSize = 12.sp)
                            }
                            Text("${d.predicho}", color = verdeMarca(), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Modelo entrenado con ${datos?.filas ?: 0} días · error medio ±${datos?.mae ?: 0} · R² ${datos?.r2 ?: 0}",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
