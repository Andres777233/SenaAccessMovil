package com.example.sennaccess.excusas

// Componentes compartidos del flujo de excusas (instructor → aprendiz → admin).
// Centraliza badge de estado, cuenta atrás de vigencia y textos de expiración
// para que las 3 vistas se vean igual y no dupliquen lógica.

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.sennaccess.datos.modelos.estaPendiente
import com.example.sennaccess.datos.modelos.Excusa
import com.example.sennaccess.comun.diseno.TipoInsignia
import com.example.sennaccess.comun.diseno.InsigniaSena
import com.example.sennaccess.comun.fechaHoraCorta
import com.example.sennaccess.comun.fechaLegible
import com.example.sennaccess.comun.horaBogota
import com.example.sennaccess.comun.milisDeIso
import kotlinx.coroutines.delay

@Composable
fun InsigniaExcusa(estado: String?) {
    val normal = (estado ?: "").lowercase()
    val tipo = when (normal) {
        "pendiente" -> TipoInsignia.AVISO
        "usada" -> TipoInsignia.NEUTRO
        "expirada", "anulada" -> TipoInsignia.ERROR
        else -> TipoInsignia.NEUTRO
    }
    InsigniaSena(texto = estado?.uppercase() ?: "—", tipo = tipo)
}

@Composable
fun recordarAhoraCada30s(): Long {
    var ahora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            ahora = System.currentTimeMillis()
        }
    }
    return ahora
}

// Ticker fino (1s) para la cuenta atrás del PIN protagonista cuando quedan
// pocos minutos; las listas siguen con el de 30s para no drenar batería.
@Composable
fun recordarAhoraCadaSegundo(): Long {
    var ahora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            ahora = System.currentTimeMillis()
        }
    }
    return ahora
}

// Prefijo del QR de excusa (opción A móvil: deriva del PIN, sin backend).
// Formato: SENA-EXCUSA:<pin4>:<id>. Portería lo detecta y valida el PIN
// con el mismo POST /excusas/validar de siempre.
const val PREFIJO_QR_EXCUSA = "SENA-EXCUSA:"

fun contenidoQrExcusa(ex: Excusa): String? {
    val pin = ex.pin?.filter { it.isDigit() }?.takeIf { it.length == 4 } ?: return null
    val id = ex.id_excusa ?: return null
    return "$PREFIJO_QR_EXCUSA$pin:$id"
}

// Extrae el PIN de un QR escaneado (nil si no es QR de excusa válido).
fun pinDeQrExcusa(contenido: String?): String? {
    if (contenido.isNullOrBlank()) return null
    val limpio = contenido.trim()
    if (!limpio.startsWith(PREFIJO_QR_EXCUSA)) return null
    val pin = limpio.removePrefix(PREFIJO_QR_EXCUSA).substringBefore(":").filter { it.isDigit() }
    return pin.takeIf { it.length == 4 }
}

fun esQrExcusa(contenido: String?): Boolean = pinDeQrExcusa(contenido) != null

// Vigencia con hora absoluta de Popayán + restante: "Vence 14:35 (Popayán) ·
// quedan 12:40". Con segundos solo si faltan menos de 5 min.
fun textoVigencia(expiraEn: String?, ahora: Long): String {
    val fin = milisDeIso(expiraEn) ?: return "Vigencia 15 min"
    val hora = horaBogota(expiraEn)
    val diffMs = fin - ahora
    if (diffMs < 0) return "Vencida $hora (Popayán)"
    val totalSeg = (diffMs / 1_000).toInt()
    val restante = if (totalSeg < 5 * 60) {
        val mm = totalSeg / 60
        val ss = (totalSeg % 60).toString().padStart(2, '0')
        "quedan $mm:$ss"
    } else {
        "quedan ${totalSeg / 60} min"
    }
    return "Vence $hora (Popayán) · $restante"
}

fun textoCreacionExcusa(ex: Excusa): String {
    val base = ex.ambiente?.ambiente_nombre ?: "Ambiente #${ex.fk_id_ambiente}"
    val fecha = ex.created_at?.let { " • ${fechaLegible(it)}" } ?: ""
    val motivo = ex.motivo?.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""
    return base + motivo + fecha
}
