package com.example.sennaccess.ui

// Helper de autenticación biométrica real (androidx.biometric): comprueba que el
// dispositivo tenga huella registrada y lanza el diálogo del sistema para verificar
// la identidad del usuario. Soporta un CryptoObject opcional: cuando se pasa, el
// sistema autoriza la operación criptográfica (cifrar/descifrar credenciales de
// HuellaCredentialStore) solo si la huella es válida.

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuth {

    // Juego de autenticadores para desbloqueo simple (sin cifrado): en Android 10
    // o superior se acepta además de la huella/rostro el PIN/patrón del dispositivo
    // (DEVICE_CREDENTIAL), de modo que el ingreso sin contraseña funciona también
    // en equipos sin sensor fuerte. En Android 8-9 (API 28-29) solo sensor fuerte.
    fun authenticators(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        }

    // Juego estricto SOLO para operaciones con CryptoObject (cifrar/descifrar la
    // huella guardada): Android prohíbe combinar CryptoObject con DEVICE_CREDENTIAL
    // y lanza IllegalArgumentException que cerraba la app. Por eso el registro y
    // el login con huella exigen biometría fuerte y nunca PIN/patrón.
    fun biometricStrongOnly(): Int =
        BiometricManager.Authenticators.BIOMETRIC_STRONG

    // Indica si el dispositivo admite autenticación. Con forCrypto=true exige
    // biometría fuerte registrada (para cifrado); con false acepta PIN/patrón.
    fun isAvailable(context: Context, forCrypto: Boolean = false): Boolean {
        val allowed = if (forCrypto) biometricStrongOnly() else authenticators()
        return BiometricManager.from(context).canAuthenticate(allowed) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    // Lanza el diálogo biométrico del sistema sin operación criptográfica:
    // onSuccess se invoca cuando la huella es verificada; onError recibe el motivo.
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: ((String) -> Unit)? = null
    ) {
        authenticate(activity, title, subtitle, null, { onSuccess() }, onError, onFailed)
    }

    // Resuelve el FragmentActivity aunque el contexto venga envuelto (Compose,
// Dialog o temas): recorre los ContextWrapper hasta hallarlo. Sin esto el cast
// directo "context as? FragmentActivity" devolvía null y el login con huella
// fallaba o cerraba la app en algunos dispositivos.
    fun activityDe(context: Context): FragmentActivity? {
        var actual: Context? = context
        while (actual != null) {
            if (actual is FragmentActivity) return actual
            actual = (actual as? android.content.ContextWrapper)?.baseContext
        }
        return null
    }
    // Traduce los códigos de error del sistema a mensajes claros para el usuario
    // (evita mostrar textos crudos como LOCKOUT o cancelaciones silenciosas).
    fun mensajeError(errorCode: Int, errString: CharSequence): String? {
        return when (errorCode) {
            BiometricPrompt.ERROR_USER_CANCELED,
            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
            BiometricPrompt.ERROR_CANCELED -> null
            BiometricPrompt.ERROR_LOCKOUT ->
                "Demasiados intentos. Espera 30 segundos e inténtalo de nuevo."
            BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
                "Huella bloqueada. Desbloquea el teléfono con tu PIN y vuelve a intentar."
            BiometricPrompt.ERROR_NO_BIOMETRICS ->
                "No hay huellas registradas. Regístrala en Ajustes del sistema."
            BiometricPrompt.ERROR_HW_UNAVAILABLE, BiometricPrompt.ERROR_UNABLE_TO_PROCESS ->
                "Sensor no disponible ahora. Inténtalo de nuevo."
            else -> errString.toString().ifBlank { "No se pudo verificar tu huella." }
        }
    }

    // Variante con CryptoObject: el prompt del sistema desbloquea la llave del
    // Keystore solo tras verificar la huella; el Cipher llega en el resultado.
    // Con crypto SIEMPRE se exige BIOMETRIC_STRONG (nunca PIN) para no crashear.
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        cryptoObject: BiometricPrompt.CryptoObject?,
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (String) -> Unit,
        onFailed: ((String) -> Unit)? = null
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess(result)
            }

            // Huella no reconocida pero reintentable: no cierra el diálogo, solo avisa.
            override fun onAuthenticationFailed() {
                onFailed?.invoke("Huella no reconocida. Inténtalo de nuevo.")
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                mensajeError(errorCode, errString)?.let { onError(it) }
            }
        })

        // Con CryptoObject se exige biometría fuerte y botón negativo visible;
        // sin CryptoObject se permite PIN/patrón en API 30+ (sin botón negativo).
        val conCrypto = cryptoObject != null
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        if (conCrypto) {
            builder.setNegativeButtonText("Cancelar")
                .setAllowedAuthenticators(biometricStrongOnly())
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(authenticators())
        } else {
            builder.setNegativeButtonText("Cancelar")
                .setAllowedAuthenticators(authenticators())
        }

        val info = builder.build()

        // Si el sistema rechaza la combinación (p. ej. sin biometría fuerte), se
        // reporta como error en pantalla en vez de cerrar la app de golpe.
        try {
            if (cryptoObject != null) {
                prompt.authenticate(info, cryptoObject)
            } else {
                prompt.authenticate(info)
            }
        } catch (e: Exception) {
            onError("Tu dispositivo no admite huella para esta acción. Usa tu contraseña.")
        }
    }
}
