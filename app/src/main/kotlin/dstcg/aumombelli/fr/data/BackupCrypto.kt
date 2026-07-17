package fr.aumombelli.dstcg.data

import java.security.SecureRandom
import java.text.Normalizer
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal interface BackupCryptography {
    fun encrypt(payload: PortableBackupPayload, password: String): ByteArray
    fun decrypt(bytes: ByteArray, password: String): PortableBackupPayload
}

internal class BackupCodec(
    private val secureRandom: SecureRandom = SecureRandom(),
) : BackupCryptography {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
    }

    override fun encrypt(payload: PortableBackupPayload, password: String): ByteArray {
        val normalizedPassword = normalizeBackupPassword(password)
        val salt = ByteArray(SALT_SIZE_BYTES).also(secureRandom::nextBytes)
        val nonce = ByteArray(NONCE_SIZE_BYTES).also(secureRandom::nextBytes)
        val key = deriveKey(normalizedPassword, salt)
        val cipher = Cipher.getInstance(BackupEnvelope.CIPHER)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_SIZE_BITS, nonce))
        cipher.updateAAD(authenticatedHeader(salt))
        val plaintext = json.encodeToString(PortableBackupPayload.serializer(), payload).encodeToByteArray()
        val ciphertext = try {
            cipher.doFinal(plaintext)
        } finally {
            plaintext.fill(0)
        }
        val envelope = BackupEnvelope(
            saltBase64 = Base64.getEncoder().encodeToString(salt),
            nonceBase64 = Base64.getEncoder().encodeToString(nonce),
            ciphertextBase64 = Base64.getEncoder().encodeToString(ciphertext),
        )
        return json.encodeToString(BackupEnvelope.serializer(), envelope).encodeToByteArray()
    }

    override fun decrypt(bytes: ByteArray, password: String): PortableBackupPayload {
        val normalizedPassword = normalizeBackupPassword(password)
        val envelope = try {
            json.decodeFromString(BackupEnvelope.serializer(), bytes.decodeToString())
        } catch (exception: Exception) {
            throw BackupFormatException("Le fichier de sauvegarde est illisible.", exception)
        }
        validateEnvelope(envelope)
        val salt = decodeBase64(envelope.saltBase64, "sel")
        val nonce = decodeBase64(envelope.nonceBase64, "nonce")
        val ciphertext = decodeBase64(envelope.ciphertextBase64, "contenu")
        if (salt.size != SALT_SIZE_BYTES || nonce.size != NONCE_SIZE_BYTES || ciphertext.size <= TAG_SIZE_BYTES) {
            throw BackupFormatException("Les paramètres cryptographiques de la sauvegarde sont invalides.")
        }
        val cipher = Cipher.getInstance(BackupEnvelope.CIPHER)
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(normalizedPassword, salt), GCMParameterSpec(TAG_SIZE_BITS, nonce))
        cipher.updateAAD(authenticatedHeader(salt))
        val plaintext = try {
            cipher.doFinal(ciphertext)
        } catch (_: AEADBadTagException) {
            throw BackupFormatException(INVALID_PASSWORD_OR_BACKUP_MESSAGE)
        } catch (_: Exception) {
            throw BackupFormatException(INVALID_PASSWORD_OR_BACKUP_MESSAGE)
        }
        return try {
            json.decodeFromString(PortableBackupPayload.serializer(), plaintext.decodeToString())
        } catch (exception: SerializationException) {
            throw BackupFormatException("Le contenu de la sauvegarde est incompatible.", exception)
        } finally {
            plaintext.fill(0)
        }
    }

    private fun validateEnvelope(envelope: BackupEnvelope) {
        if (
            envelope.format != BackupEnvelope.FORMAT ||
            envelope.formatVersion != BackupEnvelope.CURRENT_FORMAT_VERSION ||
            envelope.kdf != BackupEnvelope.KDF ||
            envelope.kdfIterations != BackupEnvelope.KDF_ITERATIONS ||
            envelope.cipher != BackupEnvelope.CIPHER
        ) {
            throw BackupFormatException("Cette version de sauvegarde n'est pas prise en charge.")
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val chars = password.toCharArray()
        val spec = PBEKeySpec(chars, salt, BackupEnvelope.KDF_ITERATIONS, KEY_SIZE_BITS)
        return try {
            val encoded = SecretKeyFactory.getInstance(BackupEnvelope.KDF).generateSecret(spec).encoded
            try {
                SecretKeySpec(encoded, "AES")
            } finally {
                encoded.fill(0)
            }
        } finally {
            chars.fill('\u0000')
            spec.clearPassword()
        }
    }

    private fun authenticatedHeader(salt: ByteArray): ByteArray = buildString {
        append(BackupEnvelope.FORMAT)
        append('|')
        append(BackupEnvelope.CURRENT_FORMAT_VERSION)
        append('|')
        append(BackupEnvelope.KDF)
        append('|')
        append(BackupEnvelope.KDF_ITERATIONS)
        append('|')
        append(BackupEnvelope.CIPHER)
        append('|')
        append(Base64.getEncoder().encodeToString(salt))
    }.encodeToByteArray()

    private fun decodeBase64(value: String, label: String): ByteArray = try {
        Base64.getDecoder().decode(value)
    } catch (exception: IllegalArgumentException) {
        throw BackupFormatException("Le $label de la sauvegarde est invalide.", exception)
    }

    companion object {
        const val INVALID_PASSWORD_OR_BACKUP_MESSAGE =
            "Mot de passe incorrect ou sauvegarde endommagée."
        private const val SALT_SIZE_BYTES = 16
        private const val NONCE_SIZE_BYTES = 12
        private const val KEY_SIZE_BITS = 256
        private const val TAG_SIZE_BITS = 128
        private const val TAG_SIZE_BYTES = TAG_SIZE_BITS / 8
    }
}

fun normalizeBackupPassword(password: String): String {
    val normalized = Normalizer.normalize(password, Normalizer.Form.NFC)
    if (normalized.isEmpty()) {
        throw BackupPasswordException("Le mot de passe ne peut pas être vide.")
    }
    return normalized
}
