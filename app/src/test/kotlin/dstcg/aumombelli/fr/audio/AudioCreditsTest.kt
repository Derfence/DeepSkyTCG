package fr.aumombelli.dstcg.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioCreditsTest {
    @Test
    fun `decodes audio credits catalog`() {
        val catalog = decodeAudioCreditsCatalog(
            """
            {
              "schemaVersion": 1,
              "entries": [
                {
                  "fileName": "sound_pack_reveal.ogg",
                  "usage": "Révélation d'une carte",
                  "title": "Crystal Reveal",
                  "artist": "Example Artist",
                  "license": "CC BY",
                  "sourcePage": "https://example.com/audio"
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(1, catalog.schemaVersion)
        assertEquals("sound_pack_reveal.ogg", catalog.entries.single().fileName)
        assertEquals("Crystal Reveal", catalog.entries.single().title)
        assertEquals("Example Artist", catalog.entries.single().artist)
        assertEquals("CC BY", catalog.entries.single().license)
    }
}
