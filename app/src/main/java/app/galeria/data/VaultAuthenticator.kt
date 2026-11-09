package app.galeria.data

import android.app.Activity
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK
import android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import app.galeria.R

class VaultAuthenticator(private val activity: Activity) {
    fun authenticate(onSuccess: () -> Unit, onError: (String) -> Unit): CancellationSignal {
        val cancellation = CancellationSignal()
        val prompt = BiometricPrompt.Builder(activity)
            .setTitle(activity.getString(R.string.vault_unlock_title))
            .setSubtitle(activity.getString(R.string.vault_unlock_subtitle))
            .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .build()
        prompt.authenticate(cancellation, activity.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.BIOMETRIC_ERROR_CANCELED &&
                    errorCode != BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED
                ) onError(errString.toString())
            }
        })
        return cancellation
    }
}
