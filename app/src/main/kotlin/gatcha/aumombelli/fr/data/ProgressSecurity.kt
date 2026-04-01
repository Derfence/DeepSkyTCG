package fr.aumombelli.gatcha.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.random.Random
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

interface EntropySource {
    fun nextInt(bound: Int): Int
}

class SecureEntropySource(
    private val secureRandom: SecureRandom = SecureRandom(),
) : EntropySource {
    override fun nextInt(bound: Int): Int = secureRandom.nextInt(bound)
}

class RandomEntropySource(
    private val random: Random,
) : EntropySource {
    override fun nextInt(bound: Int): Int = random.nextInt(bound)
}

data class EncryptedPayload(
    val iv: ByteArray,
    val ciphertext: ByteArray,
)

interface ProgressCipher {
    fun encrypt(plaintext: ByteArray): EncryptedPayload
    fun decrypt(payload: EncryptedPayload): ByteArray
}

class AesGcmProgressCipher(
    private val keyProvider: () -> SecretKey,
    private val secureRandom: SecureRandom = SecureRandom(),
) : ProgressCipher {
    override fun encrypt(plaintext: ByteArray): EncryptedPayload {
        val iv = ByteArray(AesGcmCipherSupport.IV_SIZE_BYTES).also(secureRandom::nextBytes)
        val cipher = AesGcmCipherSupport.newCipher()
        cipher.init(Cipher.ENCRYPT_MODE, keyProvider(), AesGcmCipherSupport.specFor(iv))
        return EncryptedPayload(
            iv = iv,
            ciphertext = cipher.doFinal(plaintext),
        )
    }

    override fun decrypt(payload: EncryptedPayload): ByteArray {
        val cipher = AesGcmCipherSupport.newCipher()
        cipher.init(Cipher.DECRYPT_MODE, keyProvider(), AesGcmCipherSupport.specFor(payload.iv))
        return cipher.doFinal(payload.ciphertext)
    }
}

class AndroidKeystoreProgressCipher(
    private val alias: String = DEFAULT_ALIAS,
) : ProgressCipher {
    override fun encrypt(plaintext: ByteArray): EncryptedPayload {
        val cipher = AesGcmCipherSupport.newCipher()
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = checkNotNull(cipher.iv) {
            "Android Keystore AES/GCM doit generer un IV pendant le chiffrement."
        }
        return EncryptedPayload(
            iv = iv,
            ciphertext = cipher.doFinal(plaintext),
        )
    }

    override fun decrypt(payload: EncryptedPayload): ByteArray {
        val cipher = AesGcmCipherSupport.newCipher()
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), AesGcmCipherSupport.specFor(payload.iv))
        return cipher.doFinal(payload.ciphertext)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existingKey = keyStore.getKey(alias, null) as? SecretKey
        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .setKeySize(256)
                .build(),
        )
        return keyGenerator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val DEFAULT_ALIAS = "gatcha_standalone_progress_key"
    }
}

private object AesGcmCipherSupport {
    const val IV_SIZE_BYTES = 12
    private const val TAG_SIZE_BITS = 128
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    fun newCipher(): Cipher = Cipher.getInstance(TRANSFORMATION)

    fun specFor(iv: ByteArray): GCMParameterSpec = GCMParameterSpec(TAG_SIZE_BITS, iv)
}

@Serializable
data class EncryptedProgressEnvelope(
    val schemaVersion: Int = 1,
    val ivBase64: String = "",
    val ciphertextBase64: String = "",
) {
    fun isEmpty(): Boolean = ivBase64.isBlank() || ciphertextBase64.isBlank()

    fun toPayload(): EncryptedPayload = EncryptedPayload(
        iv = Base64.getDecoder().decode(ivBase64),
        ciphertext = Base64.getDecoder().decode(ciphertextBase64),
    )

    companion object {
        fun fromPayload(payload: EncryptedPayload): EncryptedProgressEnvelope = EncryptedProgressEnvelope(
            ivBase64 = Base64.getEncoder().encodeToString(payload.iv),
            ciphertextBase64 = Base64.getEncoder().encodeToString(payload.ciphertext),
        )
    }
}

object EncryptedProgressEnvelopeSerializer : Serializer<EncryptedProgressEnvelope> {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override val defaultValue: EncryptedProgressEnvelope = EncryptedProgressEnvelope()

    override suspend fun readFrom(input: InputStream): EncryptedProgressEnvelope {
        val text = input.readBytes().decodeToString()
        if (text.isBlank()) {
            return defaultValue
        }
        return try {
            json.decodeFromString(EncryptedProgressEnvelope.serializer(), text)
        } catch (exception: SerializationException) {
            throw CorruptionException("L'enveloppe de progression chiffree n'a pas pu etre lue.", exception)
        }
    }

    override suspend fun writeTo(
        t: EncryptedProgressEnvelope,
        output: OutputStream,
    ) {
        output.write(
            json.encodeToString(EncryptedProgressEnvelope.serializer(), t).encodeToByteArray(),
        )
    }
}
