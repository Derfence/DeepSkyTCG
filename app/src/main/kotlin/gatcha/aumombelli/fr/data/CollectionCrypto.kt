package gatcha.aumombelli.fr.data

import gatcha.aumombelli.fr.model.OwnedCollection
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.json.Json

object CollectionCrypto {
    private const val IvSize = 12
    private const val GcmTagLengthBits = 128
    private val random = SecureRandom()
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun serializeAndEncrypt(collection: OwnedCollection, passwordHash: String): String {
        val payload = json.encodeToString(OwnedCollection.serializer(), collection).encodeToByteArray()
        val compressed = compress(payload)
        val iv = ByteArray(IvSize).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(passwordHash), GCMParameterSpec(GcmTagLengthBits, iv))
        val encrypted = cipher.doFinal(compressed)
        return Base64.getEncoder().encodeToString(iv + encrypted)
    }

    fun decryptAndDeserialize(blob: String, passwordHash: String): OwnedCollection {
        val bytes = Base64.getDecoder().decode(blob)
        require(bytes.size > IvSize) { "Encrypted payload is too short." }
        val iv = bytes.copyOfRange(0, IvSize)
        val encrypted = bytes.copyOfRange(IvSize, bytes.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(passwordHash), GCMParameterSpec(GcmTagLengthBits, iv))
        val decompressed = decompress(cipher.doFinal(encrypted))
        return json.decodeFromString(OwnedCollection.serializer(), decompressed.decodeToString())
    }

    private fun deriveKey(passwordHash: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest("$passwordHash:gatcha-collection-key".encodeToByteArray())
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun compress(bytes: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).use { it.write(bytes) }
        return output.toByteArray()
    }

    private fun decompress(bytes: ByteArray): ByteArray =
        GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readAllBytes() }
}
