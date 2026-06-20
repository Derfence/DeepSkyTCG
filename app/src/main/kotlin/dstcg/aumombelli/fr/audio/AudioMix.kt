package fr.aumombelli.dstcg.audio

internal const val AudioMaxStreams = 6
internal const val DefaultAmbientFadeInMillis = 700
internal const val DefaultAmbientFadeOutMillis = 250
internal const val DefaultAmbientCrossFadeMillis = 900
internal const val SoundPoolMinPlaybackRate = 0.5f
internal const val SoundPoolMaxPlaybackRate = 2f

internal data class SoundCueMix(
    val assetFileName: String,
    val volume: Float,
    val playbackRate: Float = 1f,
    val cooldownMillis: Long = 0L,
    val ambientDucking: AmbientDucking? = null,
)

internal data class AmbientTrackMix(
    val assetFileName: String,
    val volume: Float,
    val fadeInMillis: Int = DefaultAmbientFadeInMillis,
    val fadeOutMillis: Int = DefaultAmbientFadeOutMillis,
    val crossFadeMillis: Int = DefaultAmbientCrossFadeMillis,
)

internal data class AmbientDucking(
    val targetGain: Float,
    val attackMillis: Int,
    val holdMillis: Int,
    val releaseMillis: Int,
)

internal val SoundCue.mix: SoundCueMix
    get() = when (this) {
        SoundCue.UiNavigate -> uiCue("sound_ui_navigate.ogg", volume = 0.30f, cooldownMillis = 45L)
        SoundCue.PackSelectionOpen -> uiCue("sound_ui_navigate.ogg", volume = 0.32f, cooldownMillis = 90L)
        SoundCue.PackSelectionClose -> uiCue(
            assetFileName = "sound_ui_navigate.ogg",
            volume = 0.28f,
            playbackRate = 0.94f,
            cooldownMillis = 90L,
        )
        SoundCue.LibraryOpen -> uiCue("sound_ui_library_open.ogg", volume = 0.36f, cooldownMillis = 140L)
        SoundCue.LibraryClose -> uiCue("sound_ui_library_close.ogg", volume = 0.32f, cooldownMillis = 140L)
        SoundCue.CraftingOpen -> uiCue(
            assetFileName = "sound_ui_navigate.ogg",
            volume = 0.34f,
            playbackRate = 1.04f,
            cooldownMillis = 120L,
        )
        SoundCue.CraftingClose -> uiCue(
            assetFileName = "sound_ui_navigate.ogg",
            volume = 0.30f,
            playbackRate = 0.96f,
            cooldownMillis = 120L,
        )
        SoundCue.EquipmentOpen -> uiCue("sound_ui_equipment_open.ogg", volume = 0.36f, cooldownMillis = 160L)
        SoundCue.EquipmentClose -> uiCue("sound_ui_equipment_close.ogg", volume = 0.32f, cooldownMillis = 160L)
        SoundCue.BadgeBookOpen -> uiCue("sound_ui_badge_open.ogg", volume = 0.36f, cooldownMillis = 160L)
        SoundCue.BadgeBookClose -> uiCue("sound_ui_badge_close.ogg", volume = 0.32f, cooldownMillis = 160L)
        SoundCue.MiniGamesOpen -> uiCue(
            assetFileName = "sound_ui_navigate.ogg",
            volume = 0.34f,
            playbackRate = 1.06f,
            cooldownMillis = 120L,
        )
        SoundCue.MiniGamesClose -> uiCue(
            assetFileName = "sound_ui_navigate.ogg",
            volume = 0.30f,
            playbackRate = 0.94f,
            cooldownMillis = 120L,
        )
        SoundCue.PackBurst -> effectCue(
            assetFileName = "sound_pack_burst.ogg",
            volume = 0.48f,
            cooldownMillis = 600L,
            ambientDucking = AmbientDucking(
                targetGain = 0.52f,
                attackMillis = 80,
                holdMillis = 260,
                releaseMillis = 650,
            ),
        )
        SoundCue.PackReveal -> effectCue(
            assetFileName = "sound_pack_reveal.ogg",
            volume = 0.42f,
            cooldownMillis = 180L,
            ambientDucking = AmbientDucking(
                targetGain = 0.58f,
                attackMillis = 60,
                holdMillis = 160,
                releaseMillis = 540,
            ),
        )
        SoundCue.HolographicReveal -> effectCue(
            assetFileName = "sound_holographic_reveal.ogg",
            volume = 0.46f,
            cooldownMillis = 220L,
            ambientDucking = AmbientDucking(
                targetGain = 0.56f,
                attackMillis = 70,
                holdMillis = 220,
                releaseMillis = 620,
            ),
        )
        SoundCue.MiniGameSuccess -> uiCue("sound_minigame_success.ogg", volume = 0.34f, cooldownMillis = 110L)
        SoundCue.MiniGameError -> uiCue(
            assetFileName = "sound_minigame_error.ogg",
            volume = 0.32f,
            playbackRate = 0.92f,
            cooldownMillis = 140L,
        )
        SoundCue.MiniGameSpecial -> effectCue(
            assetFileName = "sound_holographic_reveal.ogg",
            volume = 0.28f,
            playbackRate = 1.10f,
            cooldownMillis = 180L,
            ambientDucking = AmbientDucking(
                targetGain = 0.72f,
                attackMillis = 60,
                holdMillis = 120,
                releaseMillis = 420,
            ),
        )
        SoundCue.MiniGameCompletion -> effectCue(
            assetFileName = "sound_minigame_completion.ogg",
            volume = 0.50f,
            playbackRate = 1.04f,
            cooldownMillis = 500L,
            ambientDucking = AmbientDucking(
                targetGain = 0.60f,
                attackMillis = 80,
                holdMillis = 300,
                releaseMillis = 700,
            ),
        )
        SoundCue.BadgeUnlock -> effectCue(
            assetFileName = "sound_badge_unlock.ogg",
            volume = 0.46f,
            cooldownMillis = 500L,
            ambientDucking = AmbientDucking(
                targetGain = 0.55f,
                attackMillis = 80,
                holdMillis = 260,
                releaseMillis = 680,
            ),
        )
    }

internal val AmbientTrack.mix: AmbientTrackMix
    get() = when (this) {
        AmbientTrack.Starfield -> AmbientTrackMix(
            assetFileName = "ambient_starfield.mp3",
            volume = 0.20f,
        )
        AmbientTrack.MiniGames -> AmbientTrackMix(
            assetFileName = "ambient_minigames.mp3",
            volume = 0.22f,
        )
    }

private fun uiCue(
    assetFileName: String,
    volume: Float,
    playbackRate: Float = 1f,
    cooldownMillis: Long,
): SoundCueMix = SoundCueMix(
    assetFileName = assetFileName,
    volume = volume,
    playbackRate = playbackRate,
    cooldownMillis = cooldownMillis,
)

private fun effectCue(
    assetFileName: String,
    volume: Float,
    playbackRate: Float = 1f,
    cooldownMillis: Long,
    ambientDucking: AmbientDucking,
): SoundCueMix = SoundCueMix(
    assetFileName = assetFileName,
    volume = volume,
    playbackRate = playbackRate,
    cooldownMillis = cooldownMillis,
    ambientDucking = ambientDucking,
)
