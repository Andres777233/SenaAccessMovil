package com.example.sennaccess.excusas

// Puerta de entrada tras el login: si no hay Entrada de hoy (Bogotá), pide
// escanear el QR de portería antes de entrar al panel. Tras marcar, acceso
// libre sin re-escanear. Solo aprendiz e instructor pasan por aquí.

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.comun.diseno.CajaError as CajaErrorSena
import com.example.sennaccess.comun.esHoyBogota
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.datos.repositorios.RepositorioIngresos
import com.example.sennaccess.datos.sesion.GestorSesion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PuertaAcceso(onEntrar: () -> Unit, onSalir: () -> Unit) {
    val colors = ColoresAppLocal.current
    val scope = rememberCoroutineScope()
    val repo = remember { RepositorioIngresos() }

    var cargando by remember { mutableStateOf(true) }
    var tieneEntrada by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var verificando by remember { mutableStateOf(false) }
    var mensajeExito by remember { mutableStateOf<String?>(null) }

    fun verificar(silenciosoAlEntrar: Boolean = false, alEntrar: (() -> Unit)? = null) {
        val token = GestorSesion.token ?: run {
            error = "Sesión expirada. Inicia sesión de nuevo."
            cargando = false
            return
        }
        scope.launch {
            if (!silenciosoAlEntrar) verificando = true
            try {
                val lista = repo.getMyIngresos(token)
                val ok = lista.any { it.ingreso_type.equals("Entrada", ignoreCase = true) && esHoyBogota(it.ingreso_datetime) }
                tieneEntrada = ok
                error = null
                if (ok) alEntrar?.invoke() ?: onEntrar()
            } catch (e: retrofit2.HttpException) {
                error = com.example.sennaccess.comun.detalleHttp(e)
            } catch (e: Exception) {
                error = "No se pudo conectar al servidor."
            } finally {
                cargando = false
                verificando = false
            }
        }
    }

    LaunchedEffect(Unit) { cargando = true; verificar() }
    BackHandler { onSalir() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "CONTROL DE ACCESO",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp,
            color = com.example.sennaccess.comun.tema.verdeMarca()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Marca tu entrada",
            color = colors.textPrimary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp
        )
        Text(
            "Escanea el QR de portería para entrar hoy.",
            color = colors.textSecondary,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (cargando) {
            CircularProgressIndicator(color = com.example.sennaccess.comun.tema.verdeMarca(), modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Verificando tu entrada de hoy...", color = colors.textSecondary, fontSize = 13.sp)
        } else if (!tieneEntrada) {
            // El escáner avisa con onExito: se muestra el mensaje y se entra solo, sin botón manual.
            MarcarAcceso(
                onBack = {},
                mostrarCabecera = false,
                onExito = { msg ->
                    mensajeExito = msg
                    error = null
                    verificando = true
                    scope.launch {
                        delay(1_200)
                        verificar(silenciosoAlEntrar = true)
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (mensajeExito != null) {
                // Confirmación breve antes de entrar al panel.
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(
                            com.example.sennaccess.comun.tema.VerdeSena.copy(alpha = 0.10f),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            null,
                            tint = com.example.sennaccess.comun.tema.verdeMarca(),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                mensajeExito!!,
                                color = com.example.sennaccess.comun.tema.verdeMarca(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (verificando) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Entrando al panel...",
                                    color = colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (error != null) {
                CajaErrorSena(error!!)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
