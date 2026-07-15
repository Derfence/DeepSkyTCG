package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.BackupCodec
import fr.aumombelli.dstcg.data.BackupFormatException
import fr.aumombelli.dstcg.data.BackupPasswordException
import fr.aumombelli.dstcg.data.PortableBackupPayload
import fr.aumombelli.dstcg.data.ProgressSnapshot
import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.StandaloneProgress
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
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
    fun `short password is rejected`() {
        codec.encrypt(payload(), "trop court")
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
}
