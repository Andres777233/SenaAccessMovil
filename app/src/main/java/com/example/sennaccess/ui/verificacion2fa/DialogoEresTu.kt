// Tarjeta "¿Eres tú?" estilo Google para los dashboards: cada pocos segundos
// consulta los retos de acceso pendientes del usuario autenticado y, si detecta
// un intento de inicio de sesión, muestra un diálogo con la IP y el dispositivo.
// "Sí, soy yo" aprueba el acceso; "No soy yo" lo rechaza.
package com.example.sennaccess.ui.verificacion2fa

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.data.SessionManager
import com.example.sennaccess.data.TwoFactorRepository
import com.example.sennaccess.data.TwoFactorReto
import com.example.sennaccess.ui.fechaRelativa
import com.example.sennaccess.ui.theme.LocalAppColors
import com.example.sennaccess.ui.theme.SenaGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun Dashboards2FaPendientes() {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    val repo = remember { TwoFactorRepository() }

    // Reto en pantalla: null = nada que mostrar.
    var retoVisible by remember { mutableStateOf<TwoFactorReto?>(null) }

    // Polling cada 8 s mientras el componente esté montado (el dashboard activo).
    LaunchedEffect(Unit) {
        while (true) {
            delay(8000)
            val token = SessionManager.token ?: continue
            if (retoVisible != null) continue
            try {
                val pendientes = repo.pendientes(token)
                if (pendientes.pending == true && pendientes.challenge != null) {
                    retoVisible = pendientes.challenge
                }
            } catch (_: Exception) {
                // Sin red: se reintenta en la siguiente vuelta, sin avisos molestos.
            }
        }
    }

    retoVisible?.let { reto ->
        AlertDialog(
            onDismissRequest = { retoVisible = null },
            containerColor = colors.cardBackground.copy(alpha = 0.98f),
            shape = RoundedCornerShape(24.dp),
            icon = {
                Icon(Icons.Default.Shield, contentDescription = null, tint = SenaGreen, modifier = Modifier.size(44.dp))
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("¿Eres tú?", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = colors.textPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Detectamos un intento de iniciar sesión en tu cuenta.",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = colors.textSecondary
                    )
                }
            },
            text = {
                Column {
                    HorizontalDivider(color = colors.border)
                    FilasInfo(reto)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = reto.challenge_id ?: run {
                            retoVisible = null
                            return@Button
                        }
                        val token = SessionManager.token
                        if (token == null) {
                            retoVisible = null
                            return@Button
                        }
                        scope.launch {
                            try {
                                repo.aprobar(token, id, "aprobar")
                            } catch (_: Exception) {
                                // Si ya se resolvió, solo se limpia la tarjeta.
                            }
                        }
                        retoVisible = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SenaGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("SÍ, SOY YO", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val id = reto.challenge_id ?: run {
                            retoVisible = null
                            return@TextButton
                        }
                        val token = SessionManager.token
                        if (token == null) {
                            retoVisible = null
                            return@TextButton
                        }
                        scope.launch {
                            try {
                                repo.aprobar(token, id, "denegar")
                            } catch (_: Exception) {
                            }
                        }
                        retoVisible = null
                    }
                ) {
                    Text("NO SOY YO", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun FilasInfo(reto: TwoFactorReto) {
    val colors = LocalAppColors.current

    Spacer(Modifier.height(8.dp))
    FilaIcono(Icons.Default.Laptop, "Dispositivo", reto.user_agent?.take(60) ?: "—")
    FilaIcono(Icons.Default.Shield, "Hora", fechaRelativa(reto.created_at))
    reto.ip?.let {
        FilaIcono(Icons.Default.Laptop, "IP", it)
    }
    Text(
        "Si no fuiste tú, este acceso será bloqueado.",
        fontSize = 12.sp,
        color = colors.textSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
    )
}

@Composable
private fun FilaIcono(icono: androidx.compose.ui.graphics.vector.ImageVector, label: String, valor: String) {
    val colors = LocalAppColors.current
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, null, tint = SenaGreen, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text("$label:", fontWeight = FontWeight.SemiBold, color = colors.textPrimary, fontSize = 13.sp)
        Spacer(Modifier.width(4.dp))
        Text(valor, color = colors.textSecondary, fontSize = 13.sp)
    }
}