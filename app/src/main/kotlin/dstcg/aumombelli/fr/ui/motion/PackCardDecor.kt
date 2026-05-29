package fr.aumombelli.dstcg.ui.motion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.random.Random

internal data class PackRarityStarSpec(
    val rarityLabel: String,
    val xFraction: Float,
    val yFraction: Float,
    val radiusFraction: Float,
)

internal data class PackTearBandSpec(
    val toothCount: Int,
    val toothDepthFraction: Float,
)

internal data class PackCardDecorSpec(
    val rarityStars: List<PackRarityStarSpec>,
    val tearBand: PackTearBandSpec,
)

internal val DefaultPackCardDecorSpec = packCardDecorSpec(seed = 0)

internal fun packCardDecorSpec(seed: Int, isEpicBoosted: Boolean = false): PackCardDecorSpec = PackCardDecorSpec(
    rarityStars = buildPackRarityStarSpecs(seed = seed, isEpicBoosted = isEpicBoosted),
    tearBand = PackTearBandSpec(
        toothCount = 10,
        toothDepthFraction = 0.03f,
    ),
)

private fun buildPackRarityStarSpecs(seed: Int, isEpicBoosted: Boolean): List<PackRarityStarSpec> {
    val random = Random(seed)
    val rarityLabels = buildList {
        repeat(11) { add("Common") }
        repeat(4) { add("Uncommon") }
        repeat(3) { add("Rare") }
        add("Epic")
        if (isEpicBoosted) add("Epic")
    }

    return rarityLabels.map { rarityLabel ->
        val topWindow = random.nextBoolean()
        val yStart = if (topWindow) 0.10f else 0.60f
        val radius = when (rarityLabel) {
            "Common" -> random.nextFloatIn(0.014f, 0.019f)
            "Uncommon" -> random.nextFloatIn(0.021f, 0.025f)
            "Rare" -> random.nextFloatIn(0.027f, 0.030f)
            "Epic" -> random.nextFloatIn(0.032f, 0.034f)
            else -> random.nextFloatIn(0.014f, 0.019f)
        }

        PackRarityStarSpec(
            rarityLabel = rarityLabel,
            xFraction = random.nextFloatIn(0.10f + radius, 0.90f - radius),
            yFraction = random.nextFloatIn(yStart + radius, yStart + 0.30f - radius),
            radiusFraction = radius,
        )
    }
}

private fun Random.nextFloatIn(
    start: Float,
    end: Float,
): Float = start + nextFloat() * (end - start)

internal fun buildPackSawtoothEdgePoints(
    width: Float,
    baselineY: Float,
    tipY: Float,
    toothCount: Int,
): List<Offset> {
    if (toothCount <= 0) return listOf(Offset(0f, baselineY), Offset(width, baselineY))

    val toothWidth = width / toothCount.toFloat()
    val halfToothWidth = toothWidth / 2f
    return buildList(capacity = toothCount * 2 + 1) {
        add(Offset(0f, baselineY))
        repeat(toothCount) { index ->
            val startX = toothWidth * index
            add(Offset(startX + halfToothWidth, tipY))
            add(Offset(startX + toothWidth, baselineY))
        }
    }
}

internal fun buildPackSawtoothOutlinePath(
    topEdge: List<Offset>,
    bottomEdge: List<Offset>,
): Path = Path().apply {
    if (topEdge.isEmpty() || bottomEdge.isEmpty()) return@apply

    moveTo(topEdge.first().x, topEdge.first().y)
    topEdge.drop(1).forEach { point ->
        lineTo(point.x, point.y)
    }
    bottomEdge.asReversed().forEach { point ->
        lineTo(point.x, point.y)
    }
    close()
}
