package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.feature.packs.opening.PACK_OPENING_SOFT_REVEAL_MAX_PLAYBACK_RATE_MULTIPLIER
import fr.aumombelli.dstcg.feature.packs.opening.PACK_OPENING_SOFT_REVEAL_MAX_VOLUME_MULTIPLIER
import fr.aumombelli.dstcg.feature.packs.opening.PACK_OPENING_SOFT_REVEAL_MIN_PLAYBACK_RATE_MULTIPLIER
import fr.aumombelli.dstcg.feature.packs.opening.PACK_OPENING_SOFT_REVEAL_MIN_VOLUME_MULTIPLIER
import fr.aumombelli.dstcg.feature.packs.opening.randomPackOpeningSoftRevealAudioOptions
import kotlin.random.Random
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PackOpeningAudioTest {
    @Test
    fun `soft reveal audio options are quieter and varied`() {
        val random = Random(seed = 42)
        val volumeRange = PACK_OPENING_SOFT_REVEAL_MIN_VOLUME_MULTIPLIER..
            PACK_OPENING_SOFT_REVEAL_MAX_VOLUME_MULTIPLIER
        val playbackRateRange = PACK_OPENING_SOFT_REVEAL_MIN_PLAYBACK_RATE_MULTIPLIER..
            PACK_OPENING_SOFT_REVEAL_MAX_PLAYBACK_RATE_MULTIPLIER

        val firstOptions = randomPackOpeningSoftRevealAudioOptions(random)
        val secondOptions = randomPackOpeningSoftRevealAudioOptions(random)

        assertTrue(firstOptions.volumeMultiplier in volumeRange)
        assertTrue(secondOptions.volumeMultiplier in volumeRange)
        assertTrue(firstOptions.playbackRateMultiplier in playbackRateRange)
        assertTrue(secondOptions.playbackRateMultiplier in playbackRateRange)
        assertNotEquals(firstOptions, secondOptions)
    }
}
