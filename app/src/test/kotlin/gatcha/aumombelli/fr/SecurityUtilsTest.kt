package gatcha.aumombelli.fr

import gatcha.aumombelli.fr.data.CollectionCrypto
import gatcha.aumombelli.fr.data.SecurityUtils
import gatcha.aumombelli.fr.model.OwnedCollection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SecurityUtilsTest {
    @Test
    fun `password hash normalizes username`() {
        val first = SecurityUtils.computeClientPasswordHash(" Alice ", "secret")
        val second = SecurityUtils.computeClientPasswordHash("alice", "secret")

        assertEquals(first, second)
    }

    @Test
    fun `collection crypto round trip succeeds`() {
        val collection = OwnedCollection(cards = mapOf("ALP-001" to 2, "MON-004" to 1))
        val passwordHash = SecurityUtils.computeClientPasswordHash("alice", "secret")

        val encrypted = CollectionCrypto.serializeAndEncrypt(collection, passwordHash)
        val decrypted = CollectionCrypto.decryptAndDeserialize(encrypted, passwordHash)

        assertEquals(collection, decrypted)
        assertNotEquals(collection.cards.toString(), encrypted)
    }
}
