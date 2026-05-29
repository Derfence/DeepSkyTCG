package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.model.TradeCardRef
import fr.aumombelli.dstcg.trade.NfcTradeApdu
import fr.aumombelli.dstcg.trade.NfcTradeCardPresence
import fr.aumombelli.dstcg.trade.NfcTradePacket
import fr.aumombelli.dstcg.trade.NfcTradePacketExpectation
import fr.aumombelli.dstcg.trade.NfcTradeProtocolVersion
import fr.aumombelli.dstcg.trade.NfcTradeTypeCommit
import fr.aumombelli.dstcg.trade.NfcTradeTypeFailure
import fr.aumombelli.dstcg.trade.NfcTradeTypeHello
import fr.aumombelli.dstcg.trade.failureResponse
import fr.aumombelli.dstcg.trade.validationErrorFor
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NfcTradeProtocolTest {
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
            "Version d'echange incompatible.",
            packet.copy(protocolVersion = NfcTradeProtocolVersion + 1).validationErrorFor(expectation),
        )
        assertEquals(
            "Catalogues incompatibles.",
            packet.copy(catalogFingerprint = "catalog-2").validationErrorFor(expectation),
        )
        assertEquals(
            "Echange NFC inconnu.",
            packet.copy(tradeId = "trade-2").validationErrorFor(expectation),
        )
        assertEquals(
            "Jeton NFC inattendu.",
            packet.copy(nonce = "nonce-2").validationErrorFor(expectation),
        )
        assertEquals(
            "Message NFC inattendu.",
            packet.copy(type = NfcTradeTypeCommit).validationErrorFor(expectation),
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
                helloExpectation().copy(cardPresence = NfcTradeCardPresence.Forbidden),
            ),
        )
    }

    @Test
    fun `failure response strips card payload`() {
        val response = helloPacket().failureResponse("Refuse.")

        assertEquals(NfcTradeTypeFailure, response.type)
        assertFalse(response.ok)
        assertEquals("Refuse.", response.reason)
        assertNull(response.card)
    }

    @Test
    fun `trade APDU parser accepts only exact command format`() {
        val payload = byteArrayOf(0x01, 0x02, 0x03)
        val command = NfcTradeApdu.command(payload)

        assertArrayEquals(payload, NfcTradeApdu.payloadFromCommand(command))
        assertNull(NfcTradeApdu.payloadFromCommand(command + byteArrayOf(0x00)))

        val wrongParameterCommand = command.copyOf().also { bytes -> bytes[2] = 0x01 }
        assertNull(NfcTradeApdu.payloadFromCommand(wrongParameterCommand))
    }

    @Test
    fun `select AID parser accepts only exact command format`() {
        val command = NfcTradeApdu.selectAidCommand()

        assertTrue(NfcTradeApdu.isSelectAid(command))
        assertFalse(NfcTradeApdu.isSelectAid(command + byteArrayOf(0x00)))

        val wrongParameterCommand = command.copyOf().also { bytes -> bytes[2] = 0x00 }
        assertFalse(NfcTradeApdu.isSelectAid(wrongParameterCommand))
    }

    private fun helloPacket(): NfcTradePacket =
        NfcTradePacket(
            type = NfcTradeTypeHello,
            tradeId = "trade-1",
            catalogFingerprint = "catalog-1",
            nonce = "nonce-1",
            card = TradeCardRef("ALP-001", "city", "standard"),
        )

    private fun helloExpectation(): NfcTradePacketExpectation =
        NfcTradePacketExpectation(
            expectedType = NfcTradeTypeHello,
            expectedCatalogFingerprint = "catalog-1",
            expectedTradeId = "trade-1",
            expectedNonce = "nonce-1",
            cardPresence = NfcTradeCardPresence.Required,
        )
}
