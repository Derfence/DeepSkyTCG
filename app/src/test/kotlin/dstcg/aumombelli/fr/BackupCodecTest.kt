package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.BackupCodec
import fr.aumombelli.dstcg.data.BackupFormatException
import fr.aumombelli.dstcg.data.BackupPasswordException
import fr.aumombelli.dstcg.data.PortableBackupPayload
import fr.aumombelli.dstcg.data.ProgressSnapshot
import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.StandaloneProgress
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BackupCodecTest {
    private val codec = BackupCodec()
    private val password = "phrase secrète robuste"

    @Test
    fun `round trip preserves portable progression`() {
        val payload = payload(openedPackCount = 7)

        val restored = codec.decrypt(codec.encrypt(payload, password), password)

        assertEquals(payload, restored)
    }

    @Test
    fun `same payload produces distinct encrypted documents`() {
        val first = codec.encrypt(payload(), password)
        val second = codec.encrypt(payload(), password)

        assertFalse(first.contentEquals(second))
    }

    @Test(expected = BackupFormatException::class)
    fun `wrong password is rejected`() {
        codec.decrypt(codec.encrypt(payload(), password), "autre phrase secrète")
    }

    @Test(expected = BackupFormatException::class)
    fun `modified ciphertext is rejected`() {
        val json = Json.parseToJsonElement(codec.encrypt(payload(), password).decodeToString()).jsonObject
        val ciphertext = Base64.getDecoder().decode(json.getValue("ciphertextBase64").toString().trim('"'))
        ciphertext[ciphertext.lastIndex] = (ciphertext.last().toInt() xor 1).toByte()
        val modified = json.toMutableMap().apply {
            this["ciphertextBase64"] = kotlinx.serialization.json.JsonPrimitive(
                Base64.getEncoder().encodeToString(ciphertext),
            )
        }

        codec.decrypt(kotlinx.serialization.json.JsonObject(modified).toString().encodeToByteArray(), password)
    }

    @Test(expected = BackupPasswordException::class)
    fun `empty password is rejected`() {
        codec.encrypt(payload(), "")
    }

    @Test
    fun `single character password is accepted`() {
        val encrypted = codec.encrypt(payload(), "x")

        assertEquals(payload(), codec.decrypt(encrypted, "x"))
    }

    @Test
    fun `unicode password is normalized to NFC`() {
        val decomposed = "e\u0301"
        val composed = "é"

        val encrypted = codec.encrypt(payload(), decomposed)

        assertEquals(payload(), codec.decrypt(encrypted, composed))
    }

    @Test
    fun `very long password is accepted`() {
        val longPassword = "é".repeat(10_000)

        val encrypted = codec.encrypt(payload(), longPassword)

        assertEquals(payload(), codec.decrypt(encrypted, longPassword))
    }

    @Test(expected = BackupFormatException::class)
    fun `truncated envelope is rejected`() {
        codec.decrypt("{\"format\":\"DSTCG_BACKUP\"".encodeToByteArray(), password)
    }

    @Test(expected = BackupFormatException::class)
    fun `modified KDF parameters are rejected before decryption`() {
        val json = Json.parseToJsonElement(codec.encrypt(payload(), password).decodeToString()).jsonObject
        val modified = json.toMutableMap().apply {
            this["kdfIterations"] = kotlinx.serialization.json.JsonPrimitive(1)
        }

        codec.decrypt(kotlinx.serialization.json.JsonObject(modified).toString().encodeToByteArray(), password)
    }

    @Test
    fun `V1 golden document remains byte compatible`() {
        val deterministicCodec = BackupCodec(SequentialSecureRandom())
        val document = deterministicCodec.encrypt(payload(), "x")
        val goldenDocument = requireNotNull(
            javaClass.getResourceAsStream("/backups/v1-empty.dstcgsave"),
        ).bufferedReader().use { it.readText().trim().encodeToByteArray() }

        assertArrayEquals(goldenDocument, document)
        assertEquals("08f3c407b2b8521f10dd0971e4f7ccf14deb8f8de432a5a5a5c1841592214c26", goldenDocument.sha256())
        assertEquals(payload(), deterministicCodec.decrypt(goldenDocument, "x"))
    }

    private fun payload(openedPackCount: Int = 0): PortableBackupPayload = PortableBackupPayload(
        backupId = "backup-id",
        createdAtUtc = "2026-07-15T10:00:00Z",
        appVersionCode = 1,
        progressSchemaVersion = ProgressSnapshot.CURRENT_SCHEMA_VERSION,
        progress = StandaloneProgress(
            collection = OwnedCollection(),
            openedPackCount = openedPackCount,
        ),
    )

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString("") { byte -> "%02x".format(byte) }

    private class SequentialSecureRandom : SecureRandom() {
        private var nextValue = 0

        override fun nextBytes(bytes: ByteArray) {
            bytes.indices.forEach { index ->
                bytes[index] = nextValue++.toByte()
            }
        }
    }
}
