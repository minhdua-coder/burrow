package com.burrow.app.data

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec

data class GithubInstallationToken(val token: String, val expiresAt: String)

/**
 * Signs a GitHub App JWT with the app's RS256 private key and exchanges it
 * for a short-lived installation access token - the same flow as the
 * openssl/curl recipe in GitHub's docs, done with only the JDK's built-in
 * crypto (no external library) so it runs entirely on-device.
 */
object GithubAppAuth {

    suspend fun fetchInstallationToken(
        appId: String,
        installationId: String,
        privateKeyPem: String,
    ): Result<GithubInstallationToken> = withContext(Dispatchers.IO) {
        runCatching {
            val privateKey = parsePemPrivateKey(privateKeyPem)
            val jwt = buildJwt(appId, privateKey)
            exchangeForInstallationToken(jwt, installationId)
        }
    }

    private fun buildJwt(appId: String, privateKey: PrivateKey): String {
        val now = System.currentTimeMillis() / 1000
        val iat = now - 60
        val exp = now + 540
        val header = base64Url("""{"alg":"RS256","typ":"JWT"}""".toByteArray())
        val payload = base64Url("""{"iat":$iat,"exp":$exp,"iss":"$appId"}""".toByteArray())
        val signingInput = "$header.$payload"
        val signature = Signature.getInstance("SHA256withRSA").apply {
            initSign(privateKey)
            update(signingInput.toByteArray())
        }.sign()
        return "$signingInput.${base64Url(signature)}"
    }

    private fun exchangeForInstallationToken(jwt: String, installationId: String): GithubInstallationToken {
        val url = URL("https://api.github.com/app/installations/$installationId/access_tokens")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer $jwt")
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.connectTimeout = 8000
        connection.readTimeout = 8000

        val code = connection.responseCode
        val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) error("GitHub API error $code: $body")

        val json = JSONObject(body)
        return GithubInstallationToken(json.getString("token"), json.optString("expires_at"))
    }

    private fun base64Url(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)

    private fun parsePemPrivateKey(pem: String): PrivateKey {
        val isPkcs1 = pem.contains("BEGIN RSA PRIVATE KEY")
        val cleaned = pem
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        val der = Base64.decode(cleaned, Base64.DEFAULT)
        val pkcs8Der = if (isPkcs1) wrapPkcs1AsPkcs8(der) else der
        return KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(pkcs8Der))
    }

    // GitHub App keys download as PKCS#1 ("BEGIN RSA PRIVATE KEY"), but
    // java.security only parses PKCS#8. Wrap the PKCS#1 bytes in the fixed
    // PKCS#8 envelope (version + rsaEncryption AlgorithmIdentifier) by hand -
    // that's the whole difference between the two formats for a plain RSA key.
    private fun wrapPkcs1AsPkcs8(pkcs1: ByteArray): ByteArray {
        val rsaOidWithNull = byteArrayOf(
            0x06, 0x09, 0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x01, 0x01,
            0x05, 0x00,
        )
        val algorithmId = derSequence(rsaOidWithNull)
        val version = byteArrayOf(0x02, 0x01, 0x00)
        val privateKeyOctetString = derOctetString(pkcs1)
        return derSequence(version + algorithmId + privateKeyOctetString)
    }

    private fun derLength(length: Int): ByteArray {
        if (length < 0x80) return byteArrayOf(length.toByte())
        val bytes = mutableListOf<Byte>()
        var remaining = length
        while (remaining > 0) {
            bytes.add(0, (remaining and 0xFF).toByte())
            remaining = remaining shr 8
        }
        return byteArrayOf((0x80 or bytes.size).toByte()) + bytes.toByteArray()
    }

    private fun derSequence(content: ByteArray): ByteArray = byteArrayOf(0x30) + derLength(content.size) + content
    private fun derOctetString(content: ByteArray): ByteArray = byteArrayOf(0x04) + derLength(content.size) + content
}
