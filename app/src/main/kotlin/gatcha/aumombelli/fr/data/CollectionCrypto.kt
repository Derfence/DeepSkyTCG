package fr.aumombelli.gatcha.data

import fr.aumombelli.gatcha.model.OwnedCollection
import fr.aumombelli.gatcha.model.OwnedCardEntry
import fr.aumombelli.gatcha.model.OwnedVariantCount
import fr.aumombelli.gatcha.model.normalized
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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object CollectionCrypto {
    private const val IvSize = 12
    private const val GcmTagLengthBits = 128
    private const val LegacyDefaultSkyQuality = "city"
    private const val LegacyDefaultFinish = "standard"
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
        return decodeCollectionPayload(decompressed.decodeToString())
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

    private fun decodeCollectionPayload(payload: String): OwnedCollection {
        val root = json.parseToJsonElement(payload).jsonObject
        val version = root["version"]?.jsonPrimitive?.intOrNull ?: 1
        val cardsElement = root["cards"] as? JsonObject ?: return OwnedCollection(version = version)

        val isLegacyCountMap = cardsElement.values.all { value ->
            value is JsonPrimitive && value.intOrNull != null
        }
        if (isLegacyCountMap) {
            return OwnedCollection(
                version = version,
                cards = cardsElement.mapValues { (_, value) ->
                    legacyEntryFromCount(value)
                }.toSortedMap(),
            ).normalized()
        }

        return json.decodeFromJsonElement(OwnedCollection.serializer(), root).normalized()
    }

    private fun legacyEntryFromCount(value: JsonElement): OwnedCardEntry {
        val count = value.jsonPrimitive.intOrNull ?: 0
        return OwnedCardEntry(
            totalOwned = count,
            variants = listOf(
                OwnedVariantCount(
                    skyQuality = LegacyDefaultSkyQuality,
                    finish = LegacyDefaultFinish,
                    count = count,
                ),
            ),
        )
    }
}
