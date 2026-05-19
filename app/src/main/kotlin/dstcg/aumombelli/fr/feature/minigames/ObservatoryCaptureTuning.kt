package fr.aumombelli.dstcg.feature.minigames

import fr.aumombelli.dstcg.model.MiniGameDifficulty

internal const val ObservatoryCapturePressBoost: Float = 0.20f
internal const val ObservatoryCaptureDecayTickMillis: Long = 16L

internal fun MiniGameDifficulty.observatoryCaptureDecayPerSecond(): Float = when (this) {
    MiniGameDifficulty.Apprentice -> 0.40f
    MiniGameDifficulty.Observer -> 0.60f
    MiniGameDifficulty.Scientist -> 0.75f
    MiniGameDifficulty.Explorer -> 0.80f
}
