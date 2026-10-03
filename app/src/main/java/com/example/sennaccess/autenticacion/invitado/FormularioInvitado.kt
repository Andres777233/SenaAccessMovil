package com.example.sennaccess.autenticacion.invitado

// Registro de INVITADO (público, sin correo ni contraseña): el visitante
// aporta documento, nombre y apellidos, y el backend devuelve un QR de un solo
// uso con 60 minutos de validez que debe presentar en recepción (POST
// /register-guest). Al generarse el QR, la pantalla pasa a CodigoQrInvitado.

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.datos.modelos.RespuestaQrInvitado
import com.example.sennaccess.datos.modelos.PeticionInvitado
import com.example.sennaccess.datos.red.ClienteApi
import com.example.sennaccess.datos.sesion.GestorSesion
import com.example.sennaccess.comun.diseno.CampoAcceso
import com.example.sennaccess.comun.diseno.CajaError
import com.example.sennaccess.comun.diseno.TarjetaVidrio
import com.example.sennaccess.comun.diseno.BotonPrimarioNeon
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.verdeMarca
import kotlinx.coroutines.launch
import com.example.sennaccess.comun.detalleHttp

@Composable
fun FormularioInvitado(
    onVolver: () -> Unit,
    onQrGenerado: (RespuestaQrInvitado) -> Unit
) {
    val colors = ColoresAppLocal.current
    val scope = rememberCoroutineScope()

    var tipoDoc by remember { mutableStateOf("CC") }
    var documento by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var docDropdownAbierto by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val tiposDoc = listOf(
        "CC" to "Cédula de Ciudadanía",
        "CE" to "Cédula de Extranjería",
        "TI" to "Tarjeta de Identidad",
        "PAS" to "Pasaporte"
    )
    val docSeleccionado = tiposDoc.firstOrNull { it.first == tipoDoc } ?: tiposDoc.first()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        TarjetaVidrio(modifier = Modifier.fillMaxWidth(0.95f)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onVolver, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver al login", tint = colors.textPrimary)
                    }
                    Column(modifier = Modifier.padding(start = 4.dp)) {
                        Text("Entrar como invitado", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Genera tu código QR de acceso", color = colors.textSecondary, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Tipo doc con barra full-touch (toda la barra abre, no solo la flecha).
                com.example.sennaccess.comun.diseno.DesplegableSena(
                    valor = "${docSeleccionado.first}: ${docSeleccionado.second}",
                    opciones = tiposDoc.map { (codigo, significado) -> "$codigo: $significado" },
                    onElegir = { elegido ->
                        tipoDoc = elegido.substringBefore(":").trim().takeIf { it.isNotEmpty() } ?: tipoDoc
                    },
                    label = "Tipo de Documento",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                CampoAcceso(
                    value = documento,
                    onValueChange = { documento = it },
                    label = "Número de Documento",
                    keyboardType = KeyboardType.Number
                )
                Spacer(modifier = Modifier.height(12.dp))
                CampoAcceso(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = "Nombre(s)"
                )
                Spacer(modifier = Modifier.height(12.dp))
                CampoAcceso(
                    value = apellidos,
                    onValueChange = { apellidos = it },
                    label = "Apellidos",
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (error != null) {
                    CajaError(texto = error!!)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                BotonPrimarioNeon(
                    text = "GENERAR MI QR",
                    icon = Icons.Default.QrCode2,
                    onClick = {
                        error = null
                        val doc = documento.trim()
                        val nom = nombre.trim()
                        val ape = apellidos.trim()
                        if (doc.isBlank() || nom.isBlank() || ape.isBlank()) {
                            error = "Completa documento, nombre y apellidos"
                            return@BotonPrimarioNeon
                        }
                        cargando = true
                        scope.launch {
                            try {
                                val res = ClienteApi.conServicio { servicio ->
                                    servicio.registerGuest(PeticionInvitado(doc, nom, ape, tipoDoc))
                                }
                                cargando = false
                                if (res.qr_token.isNullOrBlank()) {
                                    error = res.message ?: "No se pudo generar el QR. Intenta de nuevo."
                                } else {
                                    onQrGenerado(res)
                                }
                            } catch (e: retrofit2.HttpException) {
                                cargando = false
                                error = detalleHttp(e)
                            } catch (e: Exception) {
                                cargando = false
                                error = "No se pudo conectar: ${e.message ?: e.javaClass.simpleName}"
                            }
                        }
                    },
                    loading = cargando,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "El QR caduca a los 60 minutos y solo puede usarse una vez.",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
