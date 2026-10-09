package com.example.sennaccess.datos.repositorios

import com.example.sennaccess.datos.modelos.PeticionChat
import com.example.sennaccess.datos.modelos.RespuestaChat
import com.example.sennaccess.datos.red.ClienteApi

// Repositorio del asistente virtual: envía mensajes a POST /api/chatbot.
class RepositorioChatbot {

    suspend fun preguntar(token: String, mensaje: String): RespuestaChat =
        ClienteApi.conServicio { it.chatear("Bearer $token", PeticionChat(mensaje)) }
}
