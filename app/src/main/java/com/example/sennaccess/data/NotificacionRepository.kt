package com.example.sennaccess.data

// Repositorio de notificaciones in-app: listado con estado de lectura, conteo de
// no leídas (badge de la campana) y las operaciones de marcado individual/total.

class NotificacionRepository {

    // Notificaciones del usuario logueado (máx. 50, de más reciente a más antigua).
    suspend fun getNotificaciones(token: String): List<Notificacion> =
        RetrofitClient.conServicio { it.getNotifications("Bearer $token") }

    // Conteo de notificaciones sin leer para el badge de la campana.
    suspend fun unreadCount(token: String): UnreadCount =
        RetrofitClient.conServicio { it.getUnreadCount("Bearer $token") }

    // Marca una notificación como leída.
    suspend fun marcarLeida(token: String, id: Int): MessageResponse =
        RetrofitClient.conServicio { it.markNotificationRead("Bearer $token", id) }

    // Marca todas las notificaciones como leídas.
    suspend fun marcarTodasLeidas(token: String): MessageResponse =
        RetrofitClient.conServicio { it.markAllNotificationsRead("Bearer $token") }
}
