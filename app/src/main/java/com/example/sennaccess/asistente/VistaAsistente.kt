package com.example.sennaccess.asistente

// Asistente virtual del sistema (POST /api/chatbot): chat simple con el
// backend, que responde con Gemini o devuelve error honesto (503 sin clave).

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.comun.detalleHttp
import com.example.sennaccess.comun.diseno.CabeceraPantalla
import com.example.sennaccess.comun.diseno.superficiePlana
import com.example.sennaccess.comun.diseno.RadioSena
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.RojoError
import com.example.sennaccess.comun.tema.VerdeSena
import com.example.sennaccess.comun.tema.verdeMarca
import com.example.sennaccess.datos.modelos.MensajeChat
import com.example.sennaccess.datos.repositorios.RepositorioChatbot
import com.example.sennaccess.datos.sesion.GestorSesion
import kotlinx.coroutines.launch

@Composable
fun VistaAsistente(onBack: () -> Unit) {
    val colors = ColoresAppLocal.current
    val scope = rememberCoroutineScope()
    val listaState = rememberLazyListState()

    var mensajes by remember { mutableStateOf(listOf(MensajeChat("Hola, soy el asistente de SenaAccess. Pregúntame sobre el sistema.", false))) }
    var texto by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun enviar() {
        val pregunta = texto.trim()
        if (pregunta.isBlank() || enviando) return
        val token = GestorSesion.token
        if (token == null) {
            error = "Sesión expirada. Inicia sesión de nuevo."
            return
        }
        error = null
        enviando = true
        mensajes = mensajes + MensajeChat(pregunta, true)
        texto = ""
        scope.launch {
            try {
                val res = RepositorioChatbot().preguntar(token, pregunta)
                enviando = false
                val respuesta = res.reply?.ifBlank { null } ?: res.error ?: "No pude generar una respuesta. Intenta de nuevo."
                mensajes = mensajes + MensajeChat(respuesta, false)
            } catch (e: retrofit2.HttpException) {
                enviando = false
                val msg = detalleHttp(e)
                mensajes = mensajes + MensajeChat(msg, false)
            } catch (e: Exception) {
                enviando = false
                mensajes = mensajes + MensajeChat("No se pudo conectar: ${e.message ?: e.javaClass.simpleName}", false)
            }
        }
    }

    LaunchedEffect(mensajes.size) {
        if (mensajes.isNotEmpty()) listaState.animateScrollToItem(mensajes.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = colors.textPrimary)
            }
            Icon(Icons.Default.SmartToy, contentDescription = null, tint = verdeMarca(), modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Asistente", color = colors.textPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text("Ayuda sobre el sistema", color = colors.textSecondary, fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            state = listaState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(mensajes) { m ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (m.esUsuario) Arrangement.End else Arrangement.Start
                ) {
                    Text(
                        text = m.texto,
                        color = if (m.esUsuario) androidx.compose.ui.graphics.Color.Black else colors.textPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .superficiePlana(cornerRadius = RadioSena.md)
                            .padding(12.dp)
                    )
                }
            }
            if (enviando) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        CircularProgressIndicator(color = verdeMarca(), modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                    }
                }
            }
        }

        if (error != null) {
            Text(error!!, color = RojoError, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it },
                label = { Text("Escribe tu pregunta") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                singleLine = false,
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = ::enviar,
                enabled = !enviando,
                modifier = Modifier.size(52.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = verdeMarca(), modifier = Modifier.size(26.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}
