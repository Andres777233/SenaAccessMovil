// Aviso de actualización: al abrir la app se consulta GET /api/app/version y,
// si el versionCode instalado es menor al mínimo publicado, se muestra un
// diálogo que NO se puede cerrar y lleva a descargar el APK nuevo.
//
// Así nadie se queda usando una versión vieja cuando el backend cambia.
package com.example.sennaccess.datos.sesion

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sennaccess.BuildConfig
import com.example.sennaccess.datos.modelos.RespuestaVersionApp
import com.example.sennaccess.datos.red.ClienteApi
import com.example.sennaccess.comun.tema.ColoresAppLocal
import com.example.sennaccess.comun.tema.verdeMarca
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun AvisoActualizacion() {
    val colors = ColoresAppLocal.current
    var version by remember { mutableStateOf<RespuestaVersionApp?>(null) }
    var abierto by remember { mutableStateOf(false) }

    // Se comprueba al arrancar y cada 6 horas mientras la app siga abierta.
    LaunchedEffect(Unit) {
        while (isActive) {
            try {
                val datos = ClienteApi.conServicio { it.versionApp() }
                val minimo = datos.min_version_code ?: 0
                if (minimo > BuildConfig.VERSION_CODE) {
                    version = datos
                    abierto = true
                }
            } catch (_: Exception) {
                // Sin conexión: la app sigue funcionando con la versión instalada.
            }
            delay(6 * 60 * 60 * 1000L)
        }
    }

    val datos = version
    if (abierto && datos != null) {
        val ctx = androidx.compose.ui.platform.LocalContext.current
        AlertDialog(
            onDismissRequest = { /* obligatorio: no se puede cerrar */ },
            containerColor = colors.surface,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    tint = verdeMarca(),
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    "Actualiza Sena Access",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        datos.mensaje ?: "Hay una nueva versión de la app. Actualiza para continuar.",
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .background(colors.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "Instalada: ${BuildConfig.VERSION_NAME} · Nueva: ${datos.version_name ?: ""}",
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val url = datos.apk_url
                        if (!url.isNullOrBlank()) {
                            try {
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            } catch (_: Exception) { }
                        }
                    }
                ) {
                    Text("DESCARGAR ACTUALIZACIÓN", color = verdeMarca(), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
