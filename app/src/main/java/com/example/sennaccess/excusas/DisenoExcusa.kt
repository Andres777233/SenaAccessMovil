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
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.TimeZone

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

fun textoVigencia(expiraEn: String?, ahora: Long): String {
    if (expiraEn.isNullOrBlank()) return "Vigencia 15 min"
    val fin = parsearIsoSimple(expiraEn) ?: return "Expira ${fechaHoraCorta(expiraEn)}"
    val diffMin = ((fin - ahora) / 60_000).toInt()
    return if (diffMin < 0) "Vencida ${fechaHoraCorta(expiraEn)}"
    else "Vence en $diffMin min • ${fechaHoraCorta(expiraEn)}"
}

fun textoCreacionExcusa(ex: Excusa): String {
    val base = ex.ambiente?.ambiente_nombre ?: "Ambiente #${ex.fk_id_ambiente}"
    val fecha = ex.created_at?.let { " • ${fechaLegible(it)}" } ?: ""
    val motivo = ex.motivo?.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""
    return base + motivo + fecha
}

private val patronIsoExcusa = Regex("""(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2}):(\d{2})""")
private fun parsearIsoSimple(iso: String): Long? {
    val m = patronIsoExcusa.find(iso) ?: return null
    val (a, me, d, h, mi, s) = m.destructured
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(a.toInt(), me.toInt() - 1, d.toInt(), h.toInt(), mi.toInt(), s.toInt())
    }.timeInMillis
}
