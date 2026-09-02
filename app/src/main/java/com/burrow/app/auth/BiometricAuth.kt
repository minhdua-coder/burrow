package com.burrow.app.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

private const val ALLOWED_AUTHENTICATORS = BIOMETRIC_STRONG or BIOMETRIC_WEAK

/** Thin wrapper around androidx.biometric so the PIN lock can offer fingerprint/face unlock as a shortcut, with PIN always kept as the fallback. */
object BiometricAuth {

    fun isAvailable(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(ALLOWED_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    /** Null when biometric unlock can be enabled; otherwise a user-facing reason it can't. */
    fun unavailabilityReason(context: Context): String? =
        when (BiometricManager.from(context).canAuthenticate(ALLOWED_AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> null
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                "No fingerprint enrolled — add one in Settings first"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                "This device has no fingerprint sensor"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                "Fingerprint sensor is temporarily unavailable"
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                "A security update is needed for biometric unlock"
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED ->
                "Biometric unlock isn't supported on this device."
            else -> "Biometric unlock isn't available on this device."
        }

    fun authenticate(activity: FragmentActivity, title: String, onSuccess: () -> Unit, onError: (String) -> Unit = {}) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onError(errString.toString())
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setNegativeButtonText("Use PIN")
            .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
            .build()
        prompt.authenticate(info)
    }
}
