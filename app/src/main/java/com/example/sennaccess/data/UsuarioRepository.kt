package com.example.sennaccess.data

// Repositorio de usuarios: expone las llamadas de la API que operan sobre perfiles y
// roles, siempre mediante el fallback USB/WiFi de RetrofitClient.

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class UsuarioRepository {

    // Lista todos los usuarios del sistema (requiere rol admin o instructor).
    suspend fun getUsers(token: String): List<UsuarioApi> =
        RetrofitClient.conServicio { it.getUsers("Bearer $token") }

    // Devuelve el perfil del usuario autenticado.
    suspend fun getCurrentUser(token: String): UsuarioApi =
        RetrofitClient.conServicio { it.getCurrentUser("Bearer $token") }

    // Obtiene el catálogo de roles, útil para filtros y desplegables.
    suspend fun getRoles(token: String): List<Role> =
        RetrofitClient.conServicio { it.getRoles("Bearer $token") }

    // Actualiza el perfil del usuario autenticado (PUT /my-profile).
    suspend fun updateMyProfile(token: String, body: UpdateProfileRequest): UsuarioApi =
        RetrofitClient.conServicio { it.updateMyProfile("Bearer $token", body) }

    // Actualiza el perfil incluyendo la foto elegida (POST my-profile multipart con
    // _method=PUT). Los campos de texto viajan como partes de texto y la imagen como
    // parte "image"; el backend la sube y guarda su URL en profile_photo_path.
    // ficha y programa son opcionales: si vienen null no se envían, lo que permite
    // usar este mismo método para el administrador (que no tiene ficha ni programa).
    suspend fun actualizarConFoto(
        token: String,
        imagen: okhttp3.MultipartBody.Part,
        identificacion: String,
        nombre: String,
        apellido: String,
        correo: String,
        ficha: Int? = null,
        programa: String? = null
    ): UsuarioApi {
        fun parte(valor: String): okhttp3.RequestBody =
            valor.toRequestBody("text/plain".toMediaType())
        return RetrofitClient.conServicio {
            it.updateMyProfileWithPhoto(
                "Bearer $token",
                parte("PUT"),
                imagen,
                parte(identificacion),
                parte(nombre),
                parte(apellido),
                parte(correo),
                ficha?.toString()?.toRequestBody("text/plain".toMediaType()),
                programa?.toRequestBody("text/plain".toMediaType())
            )
        }
    }

    // Crea un usuario nuevo (POST /admin/users, rol admin).
    suspend fun crearUsuario(token: String, body: UserRequest): UsuarioApi =
        RetrofitClient.conServicio { it.createUser("Bearer $token", body) }

    // Actualiza un usuario existente (PUT /admin/users/{id}, rol admin).
    suspend fun actualizarUsuario(token: String, id: Int, body: UserRequest): UsuarioApi =
        RetrofitClient.conServicio { it.updateUser("Bearer $token", id, body) }

    // Elimina un usuario por id (DELETE /admin/users/{id}, rol admin).
    suspend fun eliminarUsuario(token: String, id: Int): MessageResponse =
        RetrofitClient.conServicio { it.deleteUser("Bearer $token", id) }
}
