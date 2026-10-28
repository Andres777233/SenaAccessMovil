package com.example.sennaccess.excusas

// Puerta de entrada tras el login: entra directo si el ÚLTIMO movimiento es
// Entrada (aunque sea de otro día); solo pide QR/PIN tras una Salida.
// Tras marcar, muestra el mensaje y entra solo al panel.

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.sennaccess.comun.campoVisible
import com.example.sennaccess.comun.esHoyBogota
import com.example.sennaccess.comun.diseno.CajaError as CajaErrorSena
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.datos.repositorios.RepositorioAcceso
import com.example.sennaccess.datos.repositorios.RepositorioIngresos
import com.example.sennaccess.datos.sesion.GestorSesion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PuertaAcceso(onEntrar: () -> Unit, onSalir: () -> Unit) {
    val colors = ColoresAppLocal.current
    val scope = rememberCoroutineScope()
    val repo = remember { RepositorioIngresos() }
    val repoAcceso = remember { RepositorioAcceso() }

    var cargando by remember { mutableStateOf(true) }
    var tieneEntrada by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var verificando by remember { mutableStateOf(false) }
    var mensajeExito by remember { mutableStateOf<String?>(null) }
    var pinManual by remember { mutableStateOf("") }
    var validandoPin by remember { mutableStateOf(false) }

    fun verificar(silenciosoAlEntrar: Boolean = false, alEntrar: (() -> Unit)? = null) {
        val token = GestorSesion.token ?: run {
            error = "Sesión expirada. Inicia sesión de nuevo."
            cargando = false
            return
        }
        scope.launch {
            if (!silenciosoAlEntrar) verificando = true
            try {
                // El servidor decide: entrada de HOY y último movimiento
                // Entrada. Una Entrada de ayer (sin Salida) ya no cuenta.
                val ok = try {
                    repoAcceso.estado(token).estaDentroHoy
                } catch (e: retrofit2.HttpException) {
                    if (e.code() != 404) throw e
                    // Compatibilidad con un backend anterior al endpoint:
                    // se conserva la regla estricta usando el historial.
                    val ultimo = repo.getMyIngresos(token).firstOrNull()
                    ultimo?.ingreso_type?.equals("Entrada", ignoreCase = true) == true &&
                        esHoyBogota(ultimo?.ingreso_datetime)
                }
                tieneEntrada = ok
                error = null
                if (ok) alEntrar?.invoke() ?: onEntrar()
            } catch (e: retrofit2.HttpException) {
                error = com.example.sennaccess.comun.detalleHttp(e)
                mensajeExito = null
            } catch (e: Exception) {
                error = "No se pudo conectar al servidor."
                mensajeExito = null
            } finally {
                cargando = false
                verificando = false
            }
        }
    }

    LaunchedEffect(Unit) { cargando = true; verificar() }
    BackHandler { onSalir() }

    // QR o PIN marcados: mensaje 1,2 s y entrada sola al panel.
    fun alMarcar(msg: String) {
        mensajeExito = msg
        error = null
        verificando = true
        scope.launch {
            delay(1_200)
            verificar(silenciosoAlEntrar = true)
        }
    }

    fun validarPin() {
        val token = GestorSesion.token
        if (token == null || validandoPin) return
        if (pinManual.length != 6) {
            error = "Ingresa los 6 dígitos del PIN."
            return
        }
        scope.launch {
            validandoPin = true
            error = null
            try {
                val resp = repoAcceso.validarPin(token, pinManual.trim())
                pinManual = ""
                alMarcar(resp.message ?: "Movimiento registrado.")
            } catch (e: retrofit2.HttpException) {
                error = com.example.sennaccess.comun.detalleHttp(e)
                mensajeExito = null
            } catch (e: Exception) {
                error = "No se pudo conectar al servidor."
                mensajeExito = null
            } finally {
                validandoPin = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Marca tu entrada",
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        )
        Text(
            "Escanea el QR de portería o usa el PIN que te dicten.",
            color = colors.textSecondary,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (cargando) {
            CircularProgressIndicator(color = com.example.sennaccess.comun.tema.verdeMarca(), modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Verificando tu acceso...", color = colors.textSecondary, fontSize = 13.sp)
        } else if (!tieneEntrada) {
            // El escáner avisa con onExito: se muestra el mensaje y se entra solo, sin botón manual.
            MarcarAcceso(
                onBack = {},
                mostrarCabecera = false,
                onExito = ::alMarcar
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Sin cámara: PIN de 6 dígitos dictado en portería.
            OutlinedTextField(
                value = pinManual,
                onValueChange = { pinManual = it.filter(Char::isDigit).take(6) },
                label = { Text("PIN de portería (6 dígitos)") },
                placeholder = { Text("• • • • • •") },
                modifier = Modifier.fillMaxWidth().campoVisible(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 6.sp,
                    textAlign = TextAlign.Center
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = ::validarPin,
                enabled = pinManual.length == 6 && !validandoPin,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (validandoPin) {
                    CircularProgressIndicator(color = com.example.sennaccess.comun.tema.verdeMarca(), modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Validar PIN")
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
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
