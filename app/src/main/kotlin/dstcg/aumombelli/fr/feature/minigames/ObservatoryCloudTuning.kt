package fr.aumombelli.dstcg.feature.minigames

import kotlin.random.Random

internal const val ObservatoryCloudInterCycleWaitMinMillis: Long = 5_000L
internal const val ObservatoryCloudInterCycleWaitMaxMillis: Long = 10_000L
internal const val ObservatoryCloudAccumulationTickMillis: Long = 50L
internal const val ObservatoryCloudAccumulationDurationMillis: Long = 10_000L
internal const val ObservatoryCloudTapScrubAmount: Float = 0.16f
internal const val ObservatoryCloudDragPixelsForFullScrub: Float = 900f

internal fun observatoryRandomCloudInterCycleWaitMillis(random: Random = Random.Default): Long =
    random.nextLong(
        from = ObservatoryCloudInterCycleWaitMinMillis,
        until = ObservatoryCloudInterCycleWaitMaxMillis + 1L,
    )

internal fun observatoryCloudProgressAfterTick(
    progress: Float,
    tickMillis: Long = ObservatoryCloudAccumulationTickMillis,
    durationMillis: Long = ObservatoryCloudAccumulationDurationMillis,
): Float {
    if (durationMillis <= 0L) return 1f
    return (progress + tickMillis.toFloat() / durationMillis.toFloat()).coerceIn(0f, 1f)
}

internal fun observatoryCloudScrubAmountForDrag(distancePx: Float): Float =
    (distancePx / ObservatoryCloudDragPixelsForFullScrub).coerceIn(0f, 1f)
