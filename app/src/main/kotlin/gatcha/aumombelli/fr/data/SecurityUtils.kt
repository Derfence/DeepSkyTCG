package gatcha.aumombelli.fr.data

import java.security.MessageDigest

object SecurityUtils {
    fun normalizeUsername(username: String): String = username.trim().lowercase()

    fun computeClientPasswordHash(username: String, password: String): String {
        val normalized = normalizeUsername(username)
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest("$normalized:$password".encodeToByteArray())
            .joinToString(separator = "") { "%02x".format(it) }
    }
}
