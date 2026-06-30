package fr.aumombelli.dstcg.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NoOpAudioController(
    initialSettings: AudioSettings = AudioSettings(),
) : AudioController {
    private val mutableSettings = MutableStateFlow(initialSettings)

    override val settings: StateFlow<AudioSettings> = mutableSettings.asStateFlow()

    override fun play(cue: SoundCue, options: AudioPlaybackOptions) = Unit

    override fun setAmbient(track: AmbientTrack?) = Unit

    override suspend fun setEnabled(enabled: Boolean) {
        mutableSettings.update { it.copy(enabled = enabled) }
    }

    override fun onAppForegrounded() = Unit

    override fun onAppBackgrounded() = Unit

    override fun release() = Unit
}
