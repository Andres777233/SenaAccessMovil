package com.example.sennaccess.biometria

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

object AlmacenHuella {

    private const val ALIAS = "sennaccess_huella_key"
    private const val PREFS = "huella_store"
    private const val PREFS_AJUSTES = "huella_ajustes"
    private const val KEY_CIFRADO = "cred_cifrado"
    private const val KEY_IV = "cred_iv"
    private const val KEY_DUENO = "cred_dueno_email"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun prefsAjustes(context: Context) =
        context.getSharedPreferences(PREFS_AJUSTES, Context.MODE_PRIVATE)

    private fun claveDecision(email: String) =
        "no_preguntar_" + email.trim().lowercase()

    // "Ahora no" solo aplica a este inicio de sesión: la huella se ofrece de
    // nuevo mientras la cuenta NO tenga una huella registrada. Así nadie queda
    // sin poder registrarla nunca (antes el descarte era permanente).
    fun noPreguntarMas(context: Context, email: String) {
        prefsAjustes(context).edit().remove(claveDecision(email)).apply()
    }

    // True si se puede ofrecer registrar la huella a esta cuenta.
    fun preguntarHuella(context: Context, email: String): Boolean = !hayGuardadaPara(context, email)

    // Indica si hay credenciales cifradas para ese correo exacto.
    fun hayGuardadaPara(context: Context, email: String): Boolean =
        hayGuardada(context) && obtenerDueno(context).equals(email.trim(), ignoreCase = true)

    private fun nuevoCipher(): Cipher = Cipher.getInstance(
        KeyProperties.KEY_ALGORITHM_AES + "/" +
            KeyProperties.BLOCK_MODE_GCM + "/" +
            KeyProperties.ENCRYPTION_PADDING_NONE
    )

    private fun obtenerLlave(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val constructor = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
        // Exige biometría fuerte explícitamente (auth por uso) en Android 11+.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            constructor.setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG
            )
        }
        generador.init(constructor.build())
        return generador.generateKey()
    }

    // Indica si hay credenciales cifradas en este dispositivo.
    fun hayGuardada(context: Context): Boolean =
        prefs(context).contains(KEY_CIFRADO) && prefs(context).contains(KEY_IV)

    // Invalida la llave biométrica del Keystore.
    fun borrarLlave() {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (keyStore.containsAlias(ALIAS)) keyStore.deleteEntry(ALIAS)
        } catch (_: Exception) { /* si no se puede borrar, se regenera igual */ }
    }

    // Cipher listo para cifrar tras pedir la huella.
    fun prepararCifrado(): Cipher {
        val cipher = nuevoCipher()
        cipher.init(Cipher.ENCRYPT_MODE, obtenerLlave())
        return cipher
    }

    // Variante que regenera la llave si quedó invalidada.
    fun prepararCifradoSeguro(): Cipher {
        try {
            return prepararCifrado()
        } catch (e: KeyPermanentlyInvalidatedException) {
            borrarLlave()
            return prepararCifrado()
        } catch (e: java.security.InvalidKeyException) {
            borrarLlave()
            return prepararCifrado()
        } catch (e: javax.crypto.IllegalBlockSizeException) {
            borrarLlave()
            return prepararCifrado()
        }
    }

    // Cifra y guarda el correo y la clave del usuario, más el dueño en claro
    // (el correo no es secreto) para no usar la huella de otra cuenta.
    // Devuelve true solo si quedó realmente escrito en el dispositivo.
    fun guardar(context: Context, cipher: Cipher, email: String, password: String): Boolean {
        val datos = cipher.doFinal("$email\n$password".toByteArray(Charsets.UTF_8))
        val ok = prefs(context).edit()
            .putString(KEY_CIFRADO, Base64.encodeToString(datos, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString(KEY_DUENO, email.trim().lowercase())
            .commit()
        if (ok) prefsAjustes(context).edit().remove(claveDecision(email)).apply()
        return ok && hayGuardadaPara(context, email)
    }

    // Correo dueño de la huella guardada, o null si no hay.
    fun obtenerDueno(context: Context): String? =
        prefs(context).getString(KEY_DUENO, null)?.takeIf { it.isNotBlank() }

    // Cipher para descifrar, o null si no hay nada guardado.
    // Solo borra ante llave invalidada permanentemente; un error transitorio
    // (p.ej. sin huella del sistema) NO borra las credenciales guardadas.
    fun prepararDescifrado(context: Context): Cipher? {
        if (!hayGuardada(context)) return null
        return try {
            val cipher = nuevoCipher()
            val iv = Base64.decode(prefs(context).getString(KEY_IV, ""), Base64.NO_WRAP)
            cipher.init(Cipher.DECRYPT_MODE, obtenerLlave(), GCMParameterSpec(128, iv))
            cipher
        } catch (e: KeyPermanentlyInvalidatedException) {
            borrar(context)
            borrarLlave()
            null
        } catch (e: Exception) {
            null
        }
    }

    // Descifra y devuelve el correo y la clave.
    fun leer(context: Context, cipher: Cipher): Pair<String, String> {
        val cifrado = Base64.decode(prefs(context).getString(KEY_CIFRADO, ""), Base64.NO_WRAP)
        val plano = String(cipher.doFinal(cifrado), Charsets.UTF_8)
        val partes = plano.split("\n", limit = 2)
        return Pair(partes[0], partes.getOrElse(1) { "" })
    }

    // Borra las credenciales guardadas.
    fun borrar(context: Context) {
        prefs(context).edit().clear().apply()
    }

    // Borra credenciales y llave (salida limpia de la huella).
    fun borrarTodo(context: Context) {
        borrar(context)
        borrarLlave()
    }
}
