package com.example.sennaccess.biometria

// Almacén de credenciales ligadas a la huella dactilar: guarda el correo y la
// contraseña del usuario en SharedPreferences pero CIFRADOS con una llave
// AES256-GCM del Keystore. La huella se pide siempre con BiometricPrompt antes
// de descifrar; la llave v2 NO exige autenticación biométrica a nivel de
// hardware para que el fabricante no la invalide a los pocos días y obligue a
// registrar la huella otra vez. La llave nunca sale del dispositivo.

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

    // v1: llave auth-per-use de versiones anteriores (solo para migrar).
    // v2: llave estable sin auth de hardware; la puerta biométrica la pone la app.
    private const val ALIAS_V1 = "sennaccess_huella_key"
    private const val ALIAS_V2 = "sennaccess_huella_key_v2"
    private const val PREFS = "huella_store"
    private const val PREFS_AJUSTES = "huella_ajustes"
    private const val KEY_CIFRADO = "cred_cifrado"
    private const val KEY_IV = "cred_iv"
    private const val KEY_DUENO = "cred_dueno_email"
    private const val KEY_VERSION = "cred_version"
    private const val KEY_INVALIDA = "cred_requiere_registro"
    private const val VERSION_ACTUAL = 2

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun prefsAjustes(context: Context) =
        context.getSharedPreferences(PREFS_AJUSTES, Context.MODE_PRIVATE)

    private fun claveDecision(email: String) =
        "no_preguntar_" + email.trim().lowercase()

    // "Ahora no" solo aplica a este inicio de sesión: la huella se ofrece de
    // nuevo mientras la cuenta NO tenga una huella registrada.
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

    private fun keyStore(): KeyStore =
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun llaveExistente(alias: String): SecretKey? =
        try {
            (keyStore().getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.secretKey
        } catch (_: Exception) {
            null
        }

    // Llave v2: sin setUserAuthenticationRequired. Así el Keystore no la
    // invalida por cambios de huellas ni por tiempo; la biometría se valida en
    // la app con BiometricPrompt antes de usar el cipher.
    private fun crearLlaveV2(): SecretKey {
        val generador = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        val constructor = KeyGenParameterSpec.Builder(
            ALIAS_V2,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setInvalidatedByBiometricEnrollment(false)
        generador.init(constructor.build())
        return generador.generateKey()
    }

    private fun obtenerLlaveV2(crear: Boolean): SecretKey? =
        llaveExistente(ALIAS_V2) ?: if (crear) crearLlaveV2() else null

    // Llave vieja: solo se lee para migrar credenciales existentes.
    private fun obtenerLlaveV1(): SecretKey? = llaveExistente(ALIAS_V1)

    // Versión del formato guardado (1 = llave vieja auth-per-use, 2 = estable).
    fun versionGuardada(context: Context): Int = prefs(context).getInt(KEY_VERSION, 1)

    // Indica si hay credenciales cifradas válidas en este dispositivo.
    fun hayGuardada(context: Context): Boolean {
        val prefs = prefs(context)
        return prefs.contains(KEY_CIFRADO) &&
            prefs.contains(KEY_IV) &&
            !prefs.getBoolean(KEY_INVALIDA, false)
    }

    // Invalida las llaves biométricas del Keystore.
    fun borrarLlave() {
        try {
            val ks = keyStore()
            listOf(ALIAS_V1, ALIAS_V2).forEach { alias ->
                if (ks.containsAlias(alias)) ks.deleteEntry(alias)
            }
        } catch (_: Exception) { /* si no se puede borrar, se regenera igual */ }
    }

    // Cipher listo para cifrar con la llave estable v2.
    fun prepararCifrado(): Cipher {
        val cipher = nuevoCipher()
        val llave = obtenerLlaveV2(crear = true)
            ?: throw IllegalStateException("No se pudo obtener la llave de huella")
        cipher.init(Cipher.ENCRYPT_MODE, llave)
        return cipher
    }

    // Variante que regenera la llave v2 si quedó corrupta.
    fun prepararCifradoSeguro(): Cipher {
        try {
            return prepararCifrado()
        } catch (_: Exception) {
            try { keyStore().deleteEntry(ALIAS_V2) } catch (_: Exception) { }
            return prepararCifrado()
        }
    }

    // Cifra y guarda el correo y la clave del usuario, más el dueño en claro
    // (el correo no es secreto) para no usar la huella de otra cuenta.
    fun guardar(context: Context, cipher: Cipher, email: String, password: String): Boolean {
        val datos = cipher.doFinal("$email\n$password".toByteArray(Charsets.UTF_8))
        val ok = prefs(context).edit()
            .putString(KEY_CIFRADO, Base64.encodeToString(datos, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString(KEY_DUENO, email.trim().lowercase())
            .putInt(KEY_VERSION, VERSION_ACTUAL)
            .putBoolean(KEY_INVALIDA, false)
            .commit()
        if (ok) prefsAjustes(context).edit().remove(claveDecision(email)).apply()
        return ok && hayGuardadaPara(context, email)
    }

    // Correo dueño de la huella guardada, o null si no hay.
    fun obtenerDueno(context: Context): String? =
        prefs(context).getString(KEY_DUENO, null)?.takeIf { it.isNotBlank() }

    // Cipher para descifrar, o null si no hay credenciales o la llave ya no
    // sirve. NUNCA borra las credenciales por un fallo transitorio: marca que
    // se debe registrar de nuevo para que el usuario pueda recuperar la huella
    // desde el login o desde Perfil > Mi Huella.
    fun prepararDescifrado(context: Context): Cipher? {
        if (!prefs(context).contains(KEY_CIFRADO) || !prefs(context).contains(KEY_IV)) return null
        val version = prefs(context).getInt(KEY_VERSION, 1)
        return try {
            val llave = if (version >= VERSION_ACTUAL) {
                obtenerLlaveV2(crear = false)
            } else {
                obtenerLlaveV1()
            } ?: run {
                marcarRequiereRegistro(context)
                return null
            }
            val cipher = nuevoCipher()
            val iv = Base64.decode(prefs(context).getString(KEY_IV, ""), Base64.NO_WRAP)
            cipher.init(Cipher.DECRYPT_MODE, llave, GCMParameterSpec(128, iv))
            cipher
        } catch (_: KeyPermanentlyInvalidatedException) {
            marcarRequiereRegistro(context)
            null
        } catch (_: Exception) {
            marcarRequiereRegistro(context)
            null
        }
    }

    // Descifra y devuelve el correo y la clave. Si la credencial era v1, la
    // migra a v2 en el mismo momento (sin volver a pedir la huella).
    fun leer(context: Context, cipher: Cipher): Pair<String, String> {
        val cifrado = Base64.decode(prefs(context).getString(KEY_CIFRADO, ""), Base64.NO_WRAP)
        val plano = try {
            String(cipher.doFinal(cifrado), Charsets.UTF_8)
        } catch (e: Exception) {
            marcarRequiereRegistro(context)
            throw e
        }
        val partes = plano.split("\n", limit = 2)
        val email = partes[0]
        val password = partes.getOrElse(1) { "" }
        if (prefs(context).getInt(KEY_VERSION, 1) < VERSION_ACTUAL) {
            try {
                val nuevo = prepararCifrado()
                guardar(context, nuevo, email, password)
            } catch (_: Exception) {
                // Si no se puede migrar, las credenciales v1 siguen sirviendo.
            }
        }
        return Pair(email, password)
    }

    private fun marcarRequiereRegistro(context: Context) {
        prefs(context).edit().putBoolean(KEY_INVALIDA, true).apply()
    }

    // Borra las credenciales guardadas (la llave se conserva).
    fun borrar(context: Context) {
        prefs(context).edit().clear().apply()
    }

    // Borra credenciales y llaves (salida limpia de la huella).
    fun borrarTodo(context: Context) {
        borrar(context)
        borrarLlave()
    }
}
