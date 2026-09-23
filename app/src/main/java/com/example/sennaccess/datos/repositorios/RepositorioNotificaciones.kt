package com.example.sennaccess.datos.repositorios

import com.example.sennaccess.datos.red.ClienteApi
import com.example.sennaccess.datos.modelos.ConteoNoLeidas
import com.example.sennaccess.datos.modelos.Notificacion
import com.example.sennaccess.datos.modelos.RespuestaMensaje

// Repositorio de notificaciones in-app: listado con estado de lectura, conteo de
// no leídas (badge de la campana) y las operaciones de marcado individual/total.

class RepositorioNotificaciones {

    // Notificaciones del usuario en sesión.
    suspend fun getNotificaciones(token: String): List<Notificacion> =
        ClienteApi.conServicio { it.getNotifications("Bearer $token") }

    // Cantidad de notificaciones sin leer.
    suspend fun unreadCount(token: String): ConteoNoLeidas =
        ClienteApi.conServicio { it.getUnreadCount("Bearer $token") }

    // Marca una notificación como leída.
    suspend fun marcarLeida(token: String, id: Int): RespuestaMensaje =
        ClienteApi.conServicio { it.markNotificationRead("Bearer $token", id) }

    // Marca todas como leídas.
    suspend fun marcarTodasLeidas(token: String): RespuestaMensaje =
        ClienteApi.conServicio { it.markAllNotificationsRead("Bearer $token") }
}
