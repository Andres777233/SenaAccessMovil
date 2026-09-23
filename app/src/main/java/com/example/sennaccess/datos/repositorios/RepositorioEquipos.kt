package com.example.sennaccess.datos.repositorios

import com.example.sennaccess.datos.red.ClienteApi
import com.example.sennaccess.datos.modelos.EquipoIngreso
import com.example.sennaccess.datos.modelos.PeticionEquipoIngreso
import com.example.sennaccess.datos.modelos.RespuestaEquipo
import com.example.sennaccess.datos.modelos.RespuestaMensaje

// Repositorio de equipos tecnológicos: consulta tanto el inventario propio del usuario
// como el inventario completo del centro (restringido a admin).

class RepositorioEquipos {

    // Equipos registrados a nombre del usuario actual.
    suspend fun getMyEquipment(token: String): List<EquipoIngreso> =
        ClienteApi.conServicio { it.getMyEquipment("Bearer $token") }

    // Todos los equipos (solo admin).
    suspend fun getEquipment(token: String): List<EquipoIngreso> =
        ClienteApi.conServicio { it.getEquipment("Bearer $token") }

    // Registra un equipo con dueño (solo admin).
    suspend fun registrar(token: String, body: PeticionEquipoIngreso): RespuestaEquipo =
        ClienteApi.conServicio { it.createEquipment("Bearer $token", body) }

    // Registra el ingreso de un equipo a nombre del aprendiz (POST /my-equipment).
    suspend fun registrarPropio(token: String, body: PeticionEquipoIngreso): RespuestaEquipo =
        ClienteApi.conServicio { it.createMyEquipment("Bearer $token", body) }

    // Elimina un equipo por su id (solo admin).
    suspend fun eliminar(token: String, id: Int): RespuestaMensaje =
        ClienteApi.conServicio { it.deleteEquipment("Bearer $token", id) }
}
