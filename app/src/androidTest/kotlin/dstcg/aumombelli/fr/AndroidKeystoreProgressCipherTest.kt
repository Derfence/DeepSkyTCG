package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.AndroidKeystoreProgressCipher
import java.security.KeyStore
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AndroidKeystoreProgressCipherTest {
    @Test
    fun encrypt_and_decrypt_round_trip_uses_keystore_generated_iv() {
        val alias = "dstcg_android_test_key_${System.nanoTime()}"

        try {
            val cipher = AndroidKeystoreProgressCipher(alias = alias)
            val plaintext = "pixel-2-keystore-round-trip".encodeToByteArray()

            val encryptedPayload = cipher.encrypt(plaintext)

            assertFalse(encryptedPayload.iv.isEmpty())
            assertArrayEquals(plaintext, cipher.decrypt(encryptedPayload))
        } finally {
            deleteSecretKey(alias)
        }
    }

    private fun deleteSecretKey(alias: String) {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        keyStore.deleteEntry(alias)
    }
}
