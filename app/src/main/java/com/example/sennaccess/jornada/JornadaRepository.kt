package com.example.sennaccess.jornada

// Repositorio de jornada: encapsula los endpoints FSM y la recolección de
// pruebas de presencia (ubicación + BSSID) para el backend zero-trust.

import com.example.sennaccess.data.EmitirPermisoRequest
import com.example.sennaccess.data.EmitirPermisoResponse
import com.example.sennaccess.data.JornadaEnAulaRequest
import com.example.sennaccess.data.JornadaEstadoResponse
import com.example.sennaccess.data.JornadaQrResponse
import com.example.sennaccess.data.JornadaTransicionRequest
import com.example.sennaccess.data.RetrofitClient
import com.example.sennaccess.data.SalidaAnticipadaRequest

class JornadaRepository {

    // Estado FSM actual del aprendiz (GET /jornada/estado).
    suspend fun getEstado(token: String): JornadaEstadoResponse =
        RetrofitClient.conServicio { it.getJornadaEstado("Bearer $token") }

    // Marca EN_AULA con prueba TOTP + geo + red.
    suspend fun enAula(token: String, body: JornadaEnAulaRequest): JornadaEstadoResponse =
        RetrofitClient.conServicio { it.postEnAula("Bearer $token", body) }

    // EN_AULA -> EN_DESCANSO (solo en ventana de receso).
    suspend fun descanso(token: String, ambienteId: Int? = null): JornadaEstadoResponse =
        RetrofitClient.conServicio { it.postDescanso("Bearer $token", JornadaTransicionRequest(ambienteId)) }

    // EN_DESCANSO -> EN_AULA.
    suspend fun regresoAula(token: String, ambienteId: Int? = null): JornadaEstadoResponse =
        RetrofitClient.conServicio { it.postRegresoAula("Bearer $token", JornadaTransicionRequest(ambienteId)) }

    // Jornada -> FINALIZADO (salida normal).
    suspend fun finalizar(token: String, ambienteId: Int? = null): JornadaEstadoResponse =
        RetrofitClient.conServicio { it.postFinalizar("Bearer $token", JornadaTransicionRequest(ambienteId)) }

    // Salida anticipada con token de permiso firmado por instructor/admin.
    suspend fun salidaAnticipada(token: String, permisoToken: String, motivo: String? = null): JornadaEstadoResponse =
        RetrofitClient.conServicio { it.postSalidaAnticipada("Bearer $token", SalidaAnticipadaRequest(permisoToken, motivo)) }

    // Instructor/admin emite permiso de salida anticipada (audit trail server-side).
    suspend fun emitirPermiso(token: String, fkIdUsuario: Int, motivo: String, pin: String?): EmitirPermisoResponse =
        RetrofitClient.conServicio { it.emitirPermiso("Bearer $token", EmitirPermisoRequest(fkIdUsuario, motivo, pin)) }

    // QR dinámico del ambiente para proyección (instructor).
    suspend fun getQrAula(token: String, ambienteId: Int): JornadaQrResponse =
        RetrofitClient.conServicio { it.getQrAula("Bearer $token", ambienteId) }

    // QR actual sin ambiente fijo (cuando el backend resuelve el ambiente por horario).
    suspend fun getQrActual(token: String, ambienteId: Int? = null): JornadaQrResponse =
        RetrofitClient.conServicio { it.getQrActual("Bearer $token", ambienteId) }
}
