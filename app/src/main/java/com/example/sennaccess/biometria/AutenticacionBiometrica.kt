package com.example.sennaccess.biometria

// Helper de autenticación biométrica real (androidx.biometric): comprueba que el
// dispositivo tenga huella registrada y lanza el diálogo del sistema para verificar
// la identidad del usuario. Soporta un CryptoObject opcional: cuando se pasa, el
// sistema autoriza la operación criptográfica (cifrar/descifrar credenciales de
// AlmacenHuella) solo si la huella es válida.

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object AutenticacionBiometrica {

    fun authenticators(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        }

    fun biometricStrongOnly(): Int =
        BiometricManager.Authenticators.BIOMETRIC_STRONG

    fun isAvailable(context: Context, forCrypto: Boolean = false): Boolean {
        val allowed = if (forCrypto) biometricStrongOnly() else authenticators()
        return BiometricManager.from(context).canAuthenticate(allowed) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

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

    fun activityDe(context: Context): FragmentActivity? {
        var actual: Context? = context
        while (actual != null) {
            if (actual is FragmentActivity) return actual
            actual = (actual as? android.content.ContextWrapper)?.baseContext
        }
        return null
    }
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

            override fun onAuthenticationFailed() {
                onFailed?.invoke("Huella no reconocida. Inténtalo de nuevo.")
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                mensajeError(errorCode, errString)?.let { onError(it) }
            }
        })

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
