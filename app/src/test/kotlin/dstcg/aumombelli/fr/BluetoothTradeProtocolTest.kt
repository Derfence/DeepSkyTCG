package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.model.TradeCardRef
import fr.aumombelli.dstcg.trade.BluetoothTradeAdvertisementCodec
import fr.aumombelli.dstcg.trade.BluetoothTradeAdvertisementStatus
import fr.aumombelli.dstcg.trade.BluetoothTradeFrameCodec
import fr.aumombelli.dstcg.trade.BluetoothTradeTieBreaker
import fr.aumombelli.dstcg.trade.TradeCardPresence
import fr.aumombelli.dstcg.trade.TradePacket
import fr.aumombelli.dstcg.trade.TradePacketExpectation
import fr.aumombelli.dstcg.trade.TradeProtocolVersion
import fr.aumombelli.dstcg.trade.TradeTypeCommit
import fr.aumombelli.dstcg.trade.TradeTypeFailure
import fr.aumombelli.dstcg.trade.TradeTypeHello
import fr.aumombelli.dstcg.trade.failureResponse
import fr.aumombelli.dstcg.trade.validationErrorFor
import fr.aumombelli.dstcg.trade.verificationCodeFor
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothTradeProtocolTest {
    @Test
    fun `packet validation accepts exact expected packet`() {
        val packet = helloPacket()

        val result = packet.validationErrorFor(helloExpectation())

        assertNull(result)
    }

    @Test
    fun `packet validation rejects mismatched protocol context`() {
        val expectation = helloExpectation()
        val packet = helloPacket()

        assertEquals(
            "Version d'échange incompatible.",
            packet.copy(protocolVersion = TradeProtocolVersion + 1).validationErrorFor(expectation),
        )
        assertEquals(
            "Catalogues incompatibles.",
            packet.copy(catalogFingerprint = "catalog-2").validationErrorFor(expectation),
        )
        assertEquals(
            "Échange inconnu.",
            packet.copy(tradeId = "trade-2").validationErrorFor(expectation),
        )
        assertEquals(
            "Jeton d'échange inattendu.",
            packet.copy(nonce = "nonce-2").validationErrorFor(expectation),
        )
        assertEquals(
            "Message d'échange inattendu.",
            packet.copy(type = TradeTypeCommit).validationErrorFor(expectation),
        )
    }

    @Test
    fun `packet validation enforces card presence`() {
        val packet = helloPacket()

        assertEquals(
            "Carte distante absente.",
            packet.copy(card = null).validationErrorFor(helloExpectation()),
        )
        assertEquals(
            "Carte distante inattendue.",
            packet.validationErrorFor(
                helloExpectation().copy(cardPresence = TradeCardPresence.Forbidden),
            ),
        )
    }

    @Test
    fun `failure response strips card payload`() {
        val response = helloPacket().failureResponse("Refus.")

        assertEquals(TradeTypeFailure, response.type)
        assertFalse(response.ok)
        assertEquals("Refus.", response.reason)
        assertNull(response.card)
    }

    @Test
    fun `BLE frame codec rebuilds fragmented payload`() {
        val payload = "payload-plus-grand-que-le-fragment".encodeToByteArray()
        val chunks = BluetoothTradeFrameCodec.encode(payload, chunkSize = 8)
        val decoder = BluetoothTradeFrameCodec.Decoder()

        val decoded = chunks.mapNotNull(decoder::accept).single()

        assertArrayEquals(payload, decoded)
    }

    @Test
    fun `BLE frame codec default chunks fit the initial Android ATT payload`() {
        val payload = ByteArray(128) { index -> index.toByte() }

        val chunks = BluetoothTradeFrameCodec.encode(payload)

        assertTrue(chunks.all { it.size <= 20 })
    }

    @Test
    fun `advertisement codec preserves discoverable identity`() {
        val payload = BluetoothTradeAdvertisementCodec.encode(
            sessionId = "abcdef123456",
            catalogFingerprint = "catalog-123456",
            deviceName = "Obs. 4821",
        )

        assertEquals(24, payload.size)
        val decoded = BluetoothTradeAdvertisementCodec.decode(payload)

        assertEquals("abcdef12", decoded?.sessionId)
        assertEquals("catalo", decoded?.catalogFingerprintPrefix)
        assertEquals("Obs. 4821", decoded?.deviceName)
        assertEquals(BluetoothTradeAdvertisementStatus.Active, decoded?.status)
    }

    @Test
    fun `advertisement codec marks leaving peers`() {
        val payload = BluetoothTradeAdvertisementCodec.encode(
            sessionId = "abcdef123456",
            catalogFingerprint = "catalog-123456",
            deviceName = "Obs. 4821",
            status = BluetoothTradeAdvertisementStatus.Leaving,
        )

        assertEquals(24, payload.size)
        val decoded = BluetoothTradeAdvertisementCodec.decode(payload)

        assertEquals("abcdef12", decoded?.sessionId)
        assertEquals("catalo", decoded?.catalogFingerprintPrefix)
        assertEquals("Obs. 4821", decoded?.deviceName)
        assertEquals(BluetoothTradeAdvertisementStatus.Leaving, decoded?.status)
    }

    @Test
    fun `verification code and tie breaker are stable for both devices`() {
        assertEquals(
            verificationCodeFor("trade-1", "aaa", "bbb"),
            verificationCodeFor("trade-1", "bbb", "aaa"),
        )
        assertTrue(BluetoothTradeTieBreaker.shouldKeepOutgoingConnection("aaa", "bbb"))
        assertFalse(BluetoothTradeTieBreaker.shouldKeepOutgoingConnection("bbb", "aaa"))
    }

    private fun helloPacket(): TradePacket =
        TradePacket(
            type = TradeTypeHello,
            tradeId = "trade-1",
            catalogFingerprint = "catalog-1",
            nonce = "nonce-1",
            sessionId = "session-1",
            deviceName = "Observatoire 4821",
            card = TradeCardRef("ALP-001", "city", "standard"),
        )

    private fun helloExpectation(): TradePacketExpectation =
        TradePacketExpectation(
            expectedType = TradeTypeHello,
            expectedCatalogFingerprint = "catalog-1",
            expectedTradeId = "trade-1",
            expectedNonce = "nonce-1",
            cardPresence = TradeCardPresence.Required,
        )
}
