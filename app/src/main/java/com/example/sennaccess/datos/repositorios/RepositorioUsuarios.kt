package com.example.sennaccess.datos.repositorios

// Repositorio de usuarios: expone las llamadas de la API que operan sobre perfiles y
// roles, siempre mediante el fallback USB/WiFi de ClienteApi.

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.example.sennaccess.datos.red.ClienteApi
import com.example.sennaccess.datos.modelos.PeticionActualizarPerfil
import com.example.sennaccess.datos.modelos.PeticionUsuario
import com.example.sennaccess.datos.modelos.PeticionVerificarClave
import com.example.sennaccess.datos.modelos.RespuestaMensaje
import com.example.sennaccess.datos.modelos.Rol
import com.example.sennaccess.datos.modelos.UsuarioApi

class RepositorioUsuarios {

    // Usuarios registrados (solo admin).
    suspend fun getUsers(token: String): List<UsuarioApi> =
        ClienteApi.conServicio { it.getUsers("Bearer $token") }

    // Perfil del usuario de la sesión actual.
    suspend fun getCurrentUser(token: String): UsuarioApi =
        ClienteApi.conServicio { it.getCurrentUser("Bearer $token") }

    // Roles disponibles para asignar.
    suspend fun getRoles(token: String): List<Rol> =
        ClienteApi.conServicio { it.getRoles("Bearer $token") }

    // Actualiza el perfil del usuario autenticado (PUT /my-profile).
    suspend fun updateMyProfile(token: String, body: PeticionActualizarPerfil): UsuarioApi =
        ClienteApi.conServicio { it.updateMyProfile("Bearer $token", body) }

    // Comprueba la clave actual sin iniciar sesión (puerta de la huella).
    suspend fun verificarPassword(token: String, contrasena: String): RespuestaMensaje =
        ClienteApi.conServicio { it.verificarPassword("Bearer $token", PeticionVerificarClave(contrasena)) }

    // Actualiza el perfil subiendo la foto recortada por multipart.
    suspend fun actualizarConFoto(
        token: String,
        imagen: okhttp3.MultipartBody.Part,
        identificacion: String,
        nombre: String,
        apellido: String,
        correo: String,
        ficha: Int? = null,
        programa: String? = null,
        codigoDobleFactor: String? = null
    ): UsuarioApi {
        // Convierte un texto en parte del formulario multipart.
        fun parte(valor: String): okhttp3.RequestBody =
            valor.toRequestBody("text/plain".toMediaType())
        return ClienteApi.conServicio {
            it.updateMyProfileWithPhoto(
                "Bearer $token",
                parte("PUT"),
                imagen,
                parte(identificacion),
                parte(nombre),
                parte(apellido),
                parte(correo),
                ficha?.toString()?.toRequestBody("text/plain".toMediaType()),
                programa?.toRequestBody("text/plain".toMediaType()),
                codigoDobleFactor?.toRequestBody("text/plain".toMediaType())
            )
        }
    }

    // Crea un usuario nuevo (POST /admin/users, rol admin).
    suspend fun crearUsuario(token: String, body: PeticionUsuario): UsuarioApi =
        ClienteApi.conServicio { it.createUser("Bearer $token", body) }

    // Actualiza un usuario existente (PUT /admin/users/{id}, rol admin).
    suspend fun actualizarUsuario(token: String, id: Int, body: PeticionUsuario): UsuarioApi =
        ClienteApi.conServicio { it.updateUser("Bearer $token", id, body) }

    // Elimina un usuario por id (DELETE /admin/users/{id}, rol admin).
    suspend fun eliminarUsuario(token: String, id: Int): RespuestaMensaje =
        ClienteApi.conServicio { it.deleteUser("Bearer $token", id) }
}
