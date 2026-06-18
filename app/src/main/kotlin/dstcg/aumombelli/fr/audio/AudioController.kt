package fr.aumombelli.dstcg.audio

import kotlinx.coroutines.flow.StateFlow

data class AudioSettings(
    val enabled: Boolean = true,
)

enum class SoundCue {
    UiNavigate,
    LibraryOpen,
    LibraryClose,
    EquipmentOpen,
    EquipmentClose,
    BadgeBookOpen,
    BadgeBookClose,
    PackBurst,
    PackReveal,
    HolographicReveal,
    MiniGameSuccess,
    MiniGameError,
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
