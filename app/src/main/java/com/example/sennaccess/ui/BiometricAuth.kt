package com.example.sennaccess.ui

// Helper de autenticación biométrica real (androidx.biometric): comprueba que el
// dispositivo tenga huella registrada y lanza el diálogo del sistema para verificar
// la identidad del usuario. Soporta un CryptoObject opcional: cuando se pasa, el
// sistema autoriza la operación criptográfica (cifrar/descifrar credenciales de
// HuellaCredentialStore) solo si la huella es válida.

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuth {

    // Indica si el dispositivo tiene un sensor biométrico fuerte (huella) y hay
    // al menos una huella registrada en los Ajustes del sistema.
    fun isAvailable(context: Context): Boolean =
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

    // Lanza el diálogo biométrico del sistema sin operación criptográfica:
    // onSuccess se invoca cuando la huella es verificada; onError recibe el motivo.
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        authenticate(activity, title, subtitle, null, { onSuccess() }, onError)
    }

    // Variante con CryptoObject: el prompt del sistema desbloquea la llave del
    // Keystore solo tras verificar la huella; el Cipher llega en el resultado.
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        cryptoObject: BiometricPrompt.CryptoObject?,
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess(result)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errString.toString())
            }
        })

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Cancelar")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        if (cryptoObject != null) {
            prompt.authenticate(info, cryptoObject)
        } else {
            prompt.authenticate(info)
        }
    }
}
