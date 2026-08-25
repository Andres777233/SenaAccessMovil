package com.example.sennaccess.data

// Repositorio de passkeys (WebAuthn): combina las llamadas al backend con el
// Credential Manager de Android. El sistema operativo crea la llave, pide la
// huella al usuario y devuelve un JSON firmado que este repositorio envía al
// servidor para que lo verifique. La huella nunca sale del dispositivo.

import android.content.Context
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.gson.Gson
import com.google.gson.JsonParser
import retrofit2.HttpException

class PasskeyRepository {

    private val gson = Gson()

    // Paso 1 del login: pide las opciones de autenticación al backend.
    suspend fun loginOptions(): WebauthnOptionsResponse =
        RetrofitClient.conServicio { it.getWebauthnLoginOptions() }

    // Paso 2 del login: abre el diálogo del sistema, verifica la huella y devuelve
    // el JSON firmado por la llave privada.
    suspend fun authenticateWithBiometric(context: Context, options: WebauthnOptionsResponse): String {
        val requestJson = gson.toJson(options.options)
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetPublicKeyCredentialOption(requestJson = requestJson))
            .setPreferImmediatelyAvailableCredentials(true)
            .build()
        val response = CredentialManager.create(context).getCredential(context, request)
        val credential = response.credential
        if (credential is PublicKeyCredential) {
            return credential.authenticationResponseJson
        }
        throw IllegalStateException("El dispositivo no devolvió una passkey.")
    }

    // Paso 3 del login: envía la respuesta firmada y recibe el token de acceso.
    suspend fun login(responseJson: String): LoginResponse =
        RetrofitClient.conServicio { it.loginWithPasskey(mapOf("response" to responseJson)) }

    // Paso 1 del registro: pide las opciones de creación de la llave (con sesión).
    suspend fun registerOptions(): WebauthnOptionsResponse =
        RetrofitClient.conServicio {
            it.getWebauthnRegisterOptions(SessionManager.authHeader() ?: "")
        }

    // Paso 2 del registro: Credential Manager crea la llave tras pedir la huella y
    // devuelve el JSON de registro para guardar la llave pública en el backend.
    suspend fun createWithBiometric(context: Context, options: WebauthnOptionsResponse): String {
        val o = options.options ?: throw IllegalStateException("Sin opciones de registro.")
        val request = CreatePublicKeyCredentialRequest(
            requestJson = gson.toJson(o),
            clientDataHash = null,
            preferImmediatelyAvailableCredentials = true,
            origin = null,
            preferDefaultProvider = null,
            isAutoSelectAllowed = false
        )
        val response = CredentialManager.create(context).createCredential(context, request)
        if (response is CreatePublicKeyCredentialResponse) {
            return response.registrationResponseJson
        }
        throw IllegalStateException("El dispositivo no creó la passkey.")
    }

    // Paso 3 del registro: envía la respuesta firmada para guardar la llave pública.
    suspend fun register(responseJson: String): MessageResponse =
        RetrofitClient.conServicio {
            it.registerPasskey(SessionManager.authHeader() ?: "", mapOf("response" to responseJson))
        }

    // Lista las passkeys registradas del usuario (para gestionarlas en el perfil).
    suspend fun myPasskeys(): List<PasskeyInfo> =
        RetrofitClient.conServicio { it.getMyPasskeys(SessionManager.authHeader() ?: "") }

    // Elimina una passkey del usuario autenticado.
    suspend fun delete(id: Int): MessageResponse =
        RetrofitClient.conServicio {
            it.deletePasskey(SessionManager.authHeader() ?: "", id)
        }

    // Traduce las excepciones del Credential Manager y del backend a mensajes legibles.
    companion object {
        fun mensajeError(e: Throwable): String = when (e) {
            is HttpException -> {
                try {
                    val body = e.response()?.errorBody()?.string()
                    val json = JsonParser.parseString(body ?: "{}").asJsonObject
                    json.get("message")?.asString ?: "Error ${e.code()}"
                } catch (_: Exception) {
                    "Error ${e.code()}"
                }
            }
            is CreateCredentialException -> "No se pudo crear la huella: ${e.type}"
            is NoCredentialException ->
                "No tienes una huella registrada. Inicia sesión y créala desde el perfil."
            is GetCredentialCancellationException -> "Verificación cancelada."
            is GetCredentialException -> "No se pudo verificar: ${e.type}"
            else -> e.message ?: "Error inesperado."
        }
    }
}