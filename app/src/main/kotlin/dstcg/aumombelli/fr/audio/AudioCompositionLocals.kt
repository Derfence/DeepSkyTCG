package fr.aumombelli.dstcg.audio

import androidx.compose.runtime.staticCompositionLocalOf

private val DefaultAudioController = NoOpAudioController()

val LocalAudioController = staticCompositionLocalOf<AudioController> {
    DefaultAudioController
}
