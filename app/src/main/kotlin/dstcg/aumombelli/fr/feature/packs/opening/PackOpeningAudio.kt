package fr.aumombelli.dstcg.feature.packs.opening

import fr.aumombelli.dstcg.audio.AudioController
import fr.aumombelli.dstcg.audio.AudioPlaybackOptions
import fr.aumombelli.dstcg.audio.SoundCue
import kotlin.random.Random

internal const val PACK_OPENING_SOFT_REVEAL_MIN_VOLUME_MULTIPLIER = 0.58f
internal const val PACK_OPENING_SOFT_REVEAL_MAX_VOLUME_MULTIPLIER = 0.64f
internal const val PACK_OPENING_SOFT_REVEAL_MIN_PLAYBACK_RATE_MULTIPLIER = 0.96f
internal const val PACK_OPENING_SOFT_REVEAL_MAX_PLAYBACK_RATE_MULTIPLIER = 1.04f

internal fun playPackOpeningSoftRevealCue(
    audioController: AudioController,
    random: Random = Random,
) {
    audioController.play(
        cue = SoundCue.PackReveal,
        options = randomPackOpeningSoftRevealAudioOptions(random),
    )
}

internal fun randomPackOpeningSoftRevealAudioOptions(
    random: Random,
): AudioPlaybackOptions = AudioPlaybackOptions(
    volumeMultiplier = random.nextFloatIn(
        min = PACK_OPENING_SOFT_REVEAL_MIN_VOLUME_MULTIPLIER,
        max = PACK_OPENING_SOFT_REVEAL_MAX_VOLUME_MULTIPLIER,
    ),
    playbackRateMultiplier = random.nextFloatIn(
        min = PACK_OPENING_SOFT_REVEAL_MIN_PLAYBACK_RATE_MULTIPLIER,
        max = PACK_OPENING_SOFT_REVEAL_MAX_PLAYBACK_RATE_MULTIPLIER,
    ),
)

private fun Random.nextFloatIn(min: Float, max: Float): Float =
    min + nextFloat() * (max - min)
