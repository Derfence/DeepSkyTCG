package fr.aumombelli.dstcg.audio

import kotlinx.coroutines.flow.StateFlow

data class AudioSettings(
    val enabled: Boolean = true,
)

enum class SoundCue {
    UiNavigate,
    PackBurst,
    PackReveal,
    HolographicReveal,
    MiniGameSuccess,
    MiniGameError,
    MiniGameSpecial,
    MiniGameCompletion,
    BadgeUnlock,
}

enum class AmbientTrack {
    Starfield,
    MiniGames,
}

interface AudioController {
    val settings: StateFlow<AudioSettings>

    fun play(cue: SoundCue)
    fun setAmbient(track: AmbientTrack?)
    suspend fun setEnabled(enabled: Boolean)
    fun onAppForegrounded()
    fun onAppBackgrounded()
    fun release()
}
