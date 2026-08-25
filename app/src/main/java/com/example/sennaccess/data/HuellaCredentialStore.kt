package com.example.sennaccess.data

// Almacén de credenciales ligadas a la huella dactilar: guarda el correo y la
// contraseña del usuario en SharedPreferences pero CIFRADOS con una llave
// AES256-GCM que vive en el Keystore de Android y exige verificación biométrica
// en cada uso (auth-per-use). Así el botón INGRESAR CON HUELLA puede autenticar
// contra el backend sin reescribir la contraseña: el sistema pide el dedo y solo
// entonces la llave autoriza el descifrado. La llave nunca sale del dispositivo.

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object HuellaCredentialStore {

    // Alias de la llave dentro del Keystore y nombre del SharedPreferences donde
    // se persiste el texto cifrado junto con su vector de inicialización.
    private const val ALIAS = "sennaccess_huella_key"
    private const val PREFS = "huella_store"
    private const val KEY_CIFRADO = "cred_cifrado"
    private const val KEY_IV = "cred_iv"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // Instancia el Cipher AES/GCM usado tanto para cifrar como para descifrar.
    private fun nuevoCipher(): Cipher = Cipher.getInstance(
        KeyProperties.KEY_ALGORITHM_AES + "/" +
            KeyProperties.BLOCK_MODE_GCM + "/" +
            KeyProperties.ENCRYPTION_PADDING_NONE
    )

    // Recupera la llave biométrica del Keystore o la genera la primera vez:
    // requiere autenticación del usuario en CADA operación y no se invalida
    // cuando se registran huellas nuevas en Ajustes.
    private fun obtenerLlave(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generador.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(false)
                .build()
        )
        return generador.generateKey()
    }

    // Indica si ya hay credenciales guardadas en este dispositivo.
    fun hayGuardada(context: Context): Boolean =
        prefs(context).contains(KEY_CIFRADO) && prefs(context).contains(KEY_IV)

    // Prepara un Cipher en modo cifrado para el registro. Debe envolverse en un
    // BiometricPrompt.CryptoObject para que el sistema autorice la operación
    // con la huella antes de llamar a guardar().
    fun prepararCifrado(): Cipher {
        val cipher = nuevoCipher()
        cipher.init(Cipher.ENCRYPT_MODE, obtenerLlave())
        return cipher
    }

    // Cifra y persiste las credenciales con el Cipher YA AUTORIZADO por la huella
    // (el que llega dentro del AuthenticationResult del prompt).
    fun guardar(context: Context, cipher: Cipher, email: String, password: String) {
        val datos = cipher.doFinal("$email\n$password".toByteArray(Charsets.UTF_8))
        prefs(context).edit()
            .putString(KEY_CIFRADO, Base64.encodeToString(datos, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
    }

    // Prepara un Cipher en modo descifrado con el IV guardado para lanzar el
    // BiometricPrompt. Devuelve null si la llave quedó invalidada (por ejemplo
    // al cambiar la credencial biométrica del sistema): en ese caso borra lo
    // guardado para forzar un registro nuevo en el próximo login manual.
    fun prepararDescifrado(context: Context): Cipher? {
        if (!hayGuardada(context)) return null
        return try {
            val cipher = nuevoCipher()
            val iv = Base64.decode(prefs(context).getString(KEY_IV, ""), Base64.NO_WRAP)
            cipher.init(Cipher.DECRYPT_MODE, obtenerLlave(), GCMParameterSpec(128, iv))
            cipher
        } catch (e: KeyPermanentlyInvalidatedException) {
            borrar(context)
            null
        } catch (e: Exception) {
            borrar(context)
            null
        }
    }

    // Descifra y devuelve las credenciales (correo to password) usando el Cipher
    // autorizado por la huella que llega en el resultado del prompt.
    fun leer(context: Context, cipher: Cipher): Pair<String, String> {
        val cifrado = Base64.decode(prefs(context).getString(KEY_CIFRADO, ""), Base64.NO_WRAP)
        val plano = String(cipher.doFinal(cifrado), Charsets.UTF_8)
        val partes = plano.split("\n", limit = 2)
        return Pair(partes[0], partes.getOrElse(1) { "" })
    }

    // Elimina las credenciales guardadas (la llave del Keystore se conserva para
    // futuros registros; no contiene información sensible por sí misma).
    fun borrar(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
