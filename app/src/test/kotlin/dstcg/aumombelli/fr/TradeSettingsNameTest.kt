package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.MaxTradeLocalNameBytes
import fr.aumombelli.dstcg.data.defaultTradeLocalName
import fr.aumombelli.dstcg.data.normalizeTradeLocalName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TradeSettingsNameTest {
    @Test
    fun `default trade name fits Bluetooth advertisement`() {
        val name = defaultTradeLocalName()

        assertTrue(name.matches(Regex("Obs\\. \\d{4}")))
        assertTrue(name.encodeToByteArray().size <= MaxTradeLocalNameBytes)
    }

    @Test
    fun `trade name normalization keeps at most twelve utf8 bytes`() {
        val normalized = "  Observatoire   4821  ".normalizeTradeLocalName()

        assertEquals("Observatoire", normalized)
        assertTrue(normalized.encodeToByteArray().size <= MaxTradeLocalNameBytes)
    }
}
