package fr.aumombelli.dstcg.audio

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioMixTest {
    @Test
    fun `all sound cues resolve to valid mix settings`() {
        SoundCue.entries.forEach { cue ->
            val mix = cue.mix

            assertTrue("${cue.name} asset must be an audio file", mix.assetFileName.hasAudioExtension())
            assertTrue("${cue.name} volume must be safe", mix.volume in 0f..1f)
            assertTrue(
                "${cue.name} playback rate must be supported by SoundPool",
                mix.playbackRate in SoundPoolMinPlaybackRate..SoundPoolMaxPlaybackRate,
            )
            assertTrue("${cue.name} cooldown must be positive or zero", mix.cooldownMillis >= 0L)
        }
    }

    @Test
    fun `all ambient tracks resolve to valid mix settings`() {
        AmbientTrack.entries.forEach { track ->
            val mix = track.mix

            assertTrue("${track.name} asset must be an audio file", mix.assetFileName.hasAudioExtension())
            assertTrue("${track.name} volume must be safe", mix.volume in 0f..1f)
            assertTrue("${track.name} fade in must be positive", mix.fadeInMillis > 0)
            assertTrue("${track.name} fade out must be positive", mix.fadeOutMillis > 0)
            assertTrue("${track.name} crossfade must be positive", mix.crossFadeMillis > 0)
        }
    }

    @Test
    fun `ducking settings reduce ambience temporarily without muting it`() {
        SoundCue.entries
            .mapNotNull { it.mix.ambientDucking }
            .forEach { ducking ->
                assertTrue(ducking.targetGain in 0f..1f)
                assertTrue(ducking.targetGain > 0f)
                assertTrue(ducking.attackMillis >= 0)
                assertTrue(ducking.holdMillis >= 0)
                assertTrue(ducking.releaseMillis >= 0)
            }
    }

    @Test
    fun `conceptual scene cues can reuse generic assets until dedicated sounds exist`() {
        val runtimeFileNames = File(assetsRoot(), SoundsAssetDirectory)
            .listFiles { file -> file.extension in setOf("mp3", "ogg") }
            .orEmpty()
            .mapTo(mutableSetOf()) { it.name }
        val conceptualCues = listOf(
            SoundCue.PackSelectionOpen,
            SoundCue.PackSelectionClose,
            SoundCue.CraftingOpen,
            SoundCue.CraftingClose,
            SoundCue.MiniGamesOpen,
            SoundCue.MiniGamesClose,
            SoundCue.MiniGameSpecial,
        )

        conceptualCues.forEach { cue ->
            assertTrue(
                "${cue.name} must resolve through the runtime asset inventory",
                cue.mix.assetFileName in runtimeFileNames,
            )
        }
    }

    private fun assetsRoot(): File = listOf(
        File("src/main/assets"),
        File("app/src/main/assets"),
    ).first { it.isDirectory }

    private fun String.hasAudioExtension(): Boolean =
        substringAfterLast('.', missingDelimiterValue = "") in setOf("mp3", "ogg")
}
