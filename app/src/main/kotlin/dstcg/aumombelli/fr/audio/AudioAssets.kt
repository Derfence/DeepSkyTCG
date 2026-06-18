package fr.aumombelli.dstcg.audio

internal const val SoundsAssetDirectory = "sounds"

internal val SoundCue.assetPath: String
    get() = "$SoundsAssetDirectory/$assetFileName"

internal val AmbientTrack.assetPath: String
    get() = "$SoundsAssetDirectory/$assetFileName"

internal fun requiredAudioAssetPaths(): Set<String> =
    SoundCue.entries.mapTo(mutableSetOf()) { it.assetPath }.also { paths ->
        AmbientTrack.entries.mapTo(paths) { it.assetPath }
    }

private val SoundCue.assetFileName: String
    get() = when (this) {
        SoundCue.UiNavigate -> "sound_ui_navigate.ogg"
        SoundCue.LibraryOpen -> "sound_ui_library_open.ogg"
        SoundCue.LibraryClose -> "sound_ui_library_close.ogg"
        SoundCue.EquipmentOpen -> "sound_ui_equipment_open.ogg"
        SoundCue.EquipmentClose -> "sound_ui_equipment_close.ogg"
        SoundCue.BadgeBookOpen -> "sound_ui_badge_open.ogg"
        SoundCue.BadgeBookClose -> "sound_ui_badge_close.ogg"
        SoundCue.PackBurst -> "sound_pack_burst.ogg"
        SoundCue.PackReveal -> "sound_pack_reveal.ogg"
        SoundCue.HolographicReveal -> "sound_holographic_reveal.ogg"
        SoundCue.MiniGameSuccess -> "sound_minigame_success.ogg"
        SoundCue.MiniGameError -> "sound_minigame_error.ogg"
        SoundCue.MiniGameCompletion -> "sound_minigame_completion.ogg"
        SoundCue.BadgeUnlock -> "sound_badge_unlock.ogg"
    }

private val AmbientTrack.assetFileName: String
    get() = when (this) {
        AmbientTrack.Starfield -> "ambient_starfield.mp3"
        AmbientTrack.MiniGames -> "ambient_minigames.mp3"
    }
