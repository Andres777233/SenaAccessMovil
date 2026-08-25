package com.example.sennaccess.data

// Repositorio de equipos tecnológicos: consulta tanto el inventario propio del usuario
// como el inventario completo del centro (restringido a admin).

class EquipoRepository {

    // Equipos registrados a nombre del usuario logueado.
    suspend fun getMyEquipment(token: String): List<IngresoEquipo> =
        RetrofitClient.conServicio { it.getMyEquipment("Bearer $token") }

    // Inventario completo de equipos del centro (solo rol admin).
    suspend fun getEquipment(token: String): List<IngresoEquipo> =
        RetrofitClient.conServicio { it.getEquipment("Bearer $token") }

    // Registra el ingreso de un equipo (admin o instructor).
    suspend fun registrar(token: String, body: IngresoEquipoRequest): EquipmentResponse =
        RetrofitClient.conServicio { it.createEquipment("Bearer $token", body) }

    // Registra el ingreso de un equipo a nombre del aprendiz (POST /my-equipment).
    suspend fun registrarPropio(token: String, body: IngresoEquipoRequest): EquipmentResponse =
        RetrofitClient.conServicio { it.createMyEquipment("Bearer $token", body) }

    // Elimina un registro de equipo del inventario (solo admin).
    suspend fun eliminar(token: String, id: Int): MessageResponse =
        RetrofitClient.conServicio { it.deleteEquipment("Bearer $token", id) }
}
