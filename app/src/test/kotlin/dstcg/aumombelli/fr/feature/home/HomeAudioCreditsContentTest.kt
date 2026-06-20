package fr.aumombelli.dstcg.feature.home

import fr.aumombelli.dstcg.audio.AudioCreditEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeAudioCreditsContentTest {
    @Test
    fun `audio credit display entries format credited sounds`() {
        val entries = homeAudioCreditDisplayEntries(
            audioCredits = listOf(
                AudioCreditEntry(
                    fileName = "sound_pack_reveal.ogg",
                    usage = "Révélation d'une carte",
                    title = "Crystal Reveal",
                    artist = "Example Artist",
                    license = "CC BY",
                    sourcePage = "https://example.com/audio",
                    changes = "Converti en OGG",
                ),
            ),
        )

        assertEquals(
            HomeAudioCreditDisplayEntry(
                fileName = "sound_pack_reveal.ogg",
                title = "Crystal Reveal",
                artist = "Example Artist",
                license = "CC BY",
                sourcePage = "https://example.com/audio",
                changes = "Converti en OGG",
            ),
            entries.single(),
        )
    }

    @Test
    fun `audio credit display entries hide empty placeholders`() {
        val entries = homeAudioCreditDisplayEntries(
            audioCredits = listOf(
                AudioCreditEntry(
                    fileName = "sound_pack_reveal.wav",
                    usage = "Révélation d'une carte",
                    title = "",
                    artist = "",
                    license = "",
                    sourcePage = "",
                ),
            ),
        )

        assertTrue(entries.isEmpty())
    }
}
