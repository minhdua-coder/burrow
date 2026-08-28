package com.burrow.app.data

import android.util.Base64
import java.security.SecureRandom

private val SECURE_RANDOM = SecureRandom()
private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

val ID_SUFFIX_LENGTHS = listOf(6, 8, 12, 16, 24, 32)
val KEY_BYTE_LENGTHS = listOf(16, 24, 32, 48, 64)

/** Cryptographically random slug/suffix to append to an object id so it can't be guessed (anti-IDOR). */
fun generateRandomSlug(length: Int): String {
    val sb = StringBuilder(length)
    repeat(length) { sb.append(ALPHABET[SECURE_RANDOM.nextInt(ALPHABET.length)]) }
    return sb.toString()
}

/** Cryptographically random key, standard base64 (with +, /, = padding) - e.g. for API keys/secrets. */
fun generateRandomBase64Key(byteCount: Int): String {
    val bytes = ByteArray(byteCount)
    SECURE_RANDOM.nextBytes(bytes)
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
}
