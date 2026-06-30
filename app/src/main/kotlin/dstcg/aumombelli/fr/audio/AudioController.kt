package fr.aumombelli.dstcg.audio

import kotlinx.coroutines.flow.StateFlow

data class AudioSettings(
    val enabled: Boolean = true,
)

data class AudioPlaybackOptions(
    val volumeMultiplier: Float = 1f,
    val playbackRateMultiplier: Float = 1f,
)

enum class SoundCue {
    UiNavigate,
    PackSelectionOpen,
    PackSelectionClose,
    LibraryOpen,
    LibraryClose,
    CraftingOpen,
    CraftingClose,
    EquipmentOpen,
    EquipmentClose,
    BadgeBookOpen,
    BadgeBookClose,
    MiniGamesOpen,
    MiniGamesClose,
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

    fun play(cue: SoundCue, options: AudioPlaybackOptions = AudioPlaybackOptions())
    fun setAmbient(track: AmbientTrack?)
    suspend fun setEnabled(enabled: Boolean)
    fun onAppForegrounded()
    fun onAppBackgrounded()
    fun release()
}
