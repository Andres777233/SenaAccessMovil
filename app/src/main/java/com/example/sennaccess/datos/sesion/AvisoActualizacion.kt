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
    var obligatoria by remember { mutableStateOf(false) }

    // Se comprueba al arrancar y cada 6 horas mientras la app siga abierta.
    LaunchedEffect(Unit) {
        while (isActive) {
            try {
                val datos = ClienteApi.conServicio { it.versionApp() }
                val instalada = BuildConfig.VERSION_CODE
                val remota = datos.version_code ?: 0
                val minimo = datos.min_version_code ?: 0
                // Obligatoria solo si la instalada quedó por debajo del mínimo;
                // si solo hay una versión nueva, se avisa y se puede posponer.
                if (minimo > instalada || remota > instalada) {
                    version = datos
                    obligatoria = minimo > instalada
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
            onDismissRequest = { if (!obligatoria) abierto = false },
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
                        if (obligatoria) {
                            datos.mensaje ?: "Hay una nueva versión de la app. Actualiza para continuar."
                        } else {
                            "Hay una nueva versión disponible. Puedes actualizar ahora (se instala encima, sin borrar datos) o hacerlo después."
                        },
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
                        // Solo se abre el APK si viene por HTTPS.
                        if (!url.isNullOrBlank() && url.startsWith("https://", ignoreCase = true)) {
                            try {
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            } catch (_: Exception) { }
                        }
                    }
                ) {
                    Text(
                        if (obligatoria) "DESCARGAR ACTUALIZACIÓN" else "ACTUALIZAR",
                        color = verdeMarca(),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = if (obligatoria) null else {
                {
                    TextButton(onClick = { abierto = false }) {
                        Text("AHORA NO", color = colors.textSecondary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }
}
