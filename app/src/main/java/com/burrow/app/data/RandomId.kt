package com.burrow.app.data

import java.security.SecureRandom

private val SECURE_RANDOM = SecureRandom()
private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

val ID_SUFFIX_LENGTHS = listOf(6, 8, 12, 16, 24, 32)

/** Cryptographically random slug/suffix to append to an object id so it can't be guessed (anti-IDOR). */
fun generateRandomSlug(length: Int): String {
    val sb = StringBuilder(length)
    repeat(length) { sb.append(ALPHABET[SECURE_RANDOM.nextInt(ALPHABET.length)]) }
    return sb.toString()
}
