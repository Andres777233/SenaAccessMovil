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

    // Juego de autenticadores permitidos: en Android 10 o superior se acepta además
    // de la huella/rostro el PIN/patrón del dispositivo (DEVICE_CREDENTIAL), de modo
    // que el ingreso sin contraseña funciona también en equipos sin sensor fuerte.
    // En Android 8-9 (API 28-29) solo existe el sensor fuerte.
    fun authenticators(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        }

    // Indica si el dispositivo admite alguno de los métodos de autenticación del
    // sistema (huella/rostro/PIN) y hay al menos uno registrado en los Ajustes.
    fun isAvailable(context: Context): Boolean =
        BiometricManager.from(context)
            .canAuthenticate(authenticators()) ==
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

        // Con DEVICE_CREDENTIAL (API 30+) la API prohíbe el botón negativo; en API 28-29
        // solo hay sensor fuerte y el botón negativo es obligatorio. Por eso se construye
        // el PromptInfo de forma distinta según la versión.
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(authenticators())
        } else {
            builder.setNegativeButtonText("Cancelar")
                .setAllowedAuthenticators(authenticators())
        }

        val info = builder.build()

        if (cryptoObject != null) {
            prompt.authenticate(info, cryptoObject)
        } else {
            prompt.authenticate(info)
        }
    }
}
