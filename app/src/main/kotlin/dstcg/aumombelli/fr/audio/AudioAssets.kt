package fr.aumombelli.dstcg.audio

internal const val SoundsAssetDirectory = "sounds"

internal val SoundCue.assetPath: String
    get() = "$SoundsAssetDirectory/${mix.assetFileName}"

internal val AmbientTrack.assetPath: String
    get() = "$SoundsAssetDirectory/${mix.assetFileName}"

internal fun requiredAudioAssetPaths(): Set<String> =
    SoundCue.entries.mapTo(mutableSetOf()) { it.assetPath }.also { paths ->
        AmbientTrack.entries.mapTo(paths) { it.assetPath }
    }
