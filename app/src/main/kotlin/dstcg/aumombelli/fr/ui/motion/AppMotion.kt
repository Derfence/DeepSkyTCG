package fr.aumombelli.dstcg.ui.motion

import androidx.compose.runtime.Immutable
import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.model.raritySortPriority
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

enum class SkyBackdropVariant(
    val skyQuality: String,
    val twinklingStarCount: Int,
    val horizonLightCount: Int,
) {
    City(
        skyQuality = "city",
        twinklingStarCount = 12,
        horizonLightCount = 7,
    ),
    Suburban(
        skyQuality = "suburban",
        twinklingStarCount = 24,
        horizonLightCount = 4,
    ),
    Rural(
        skyQuality = "rural",
        twinklingStarCount = 38,
        horizonLightCount = 2,
    ),
    Mountain(
        skyQuality = "mountain",
        twinklingStarCount = 54,
        horizonLightCount = 0,
    ),
    ;

    val hasHorizonLights: Boolean
        get() = horizonLightCount > 0
}

enum class AppScene {
    Home,
    Library,
    Equipment,
    BadgeBook,
    PackSelection,
    PackOpening,
}

enum class ExtensionAnimationStyle {
    NeutralSky,
    BigDipper,
}

data class FractionalPoint(
    val x: Float,
    val y: Float,
)

data class ExtensionAnimationSpec(
    val style: ExtensionAnimationStyle,
    val starPattern: List<FractionalPoint> = emptyList(),
    val lineConnections: List<Pair<Int, Int>> = emptyList(),
)

data class ExtensionPatternProjection(
    val originX: Float,
    val originY: Float,
    val scale: Float,
    val minX: Float,
    val maxY: Float,
) {
    fun project(point: FractionalPoint): FractionalPoint = FractionalPoint(
        x = originX + (point.x - minX) * scale,
        y = originY + (maxY - point.y) * scale,
    )
}

data class PackOpeningSummary(
    val highestRarityLabel: String,
    val hasHolographicCard: Boolean,
)

data class PackRevealBounds(
    val leftPx: Float,
    val topPx: Float,
    val widthPx: Float,
    val heightPx: Float,
)

data class PixelPoint(
    val x: Float,
    val y: Float,
)

fun PackRevealBounds.relativeTo(containerBounds: PackRevealBounds): PackRevealBounds = PackRevealBounds(
    leftPx = leftPx - containerBounds.leftPx,
    topPx = topPx - containerBounds.topPx,
    widthPx = widthPx,
    heightPx = heightPx,
)

fun packOpeningBurstOrigin(
    originBounds: PackRevealBounds?,
    canvasWidth: Float,
    canvasHeight: Float,
    hasHolographicBurst: Boolean,
): PixelPoint {
    val baseCenter = if (originBounds != null) {
        PixelPoint(
            x = originBounds.leftPx + originBounds.widthPx / 2f,
            y = originBounds.topPx + originBounds.heightPx / 2f,
        )
    } else {
        PixelPoint(
            x = canvasWidth / 2f,
            y = canvasHeight / 2f,
        )
    }
    val holographicVerticalBias = if (hasHolographicBurst) {
        if (originBounds != null) {
            originBounds.heightPx * 0.035f
        } else {
            -canvasHeight * 0.20f
        }
    } else {
        0f
    }
    return PixelPoint(
        x = baseCenter.x,
        y = baseCenter.y - holographicVerticalBias,
    )
}

fun packOpeningBurstOrbitOrigin(
    originBounds: PackRevealBounds?,
    canvasWidth: Float,
    canvasHeight: Float,
    hasHolographicBurst: Boolean,
): PixelPoint {
    val baseOrigin = packOpeningBurstOrigin(
        originBounds = originBounds,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        hasHolographicBurst = hasHolographicBurst,
    )
    val screenCenteredOrbitOffset = if (hasHolographicBurst && originBounds == null) {
        0f
    } else {
        0f
    }
    return PixelPoint(
        x = baseOrigin.x,
        y = baseOrigin.y + screenCenteredOrbitOffset,
    )
}

@Immutable
data class HolographicCardMotion(
    val rotationYDeg: Float = 0f,
    val sweepFraction: Float = 0.5f,
    val highlightAlpha: Float = 0.18f,
    val edgeGlowAlpha: Float = 0.24f,
    val sparkleBoost: Float = 0f,
)

const val PACK_REVEAL_WIDTH_FRACTION: Float = 0.82f

enum class BurstParticleMotion {
    Radial,
    Falling,
    Orbital,
}

data class BurstParticleSpec(
    val rarityLabel: String,
    val motion: BurstParticleMotion,
    val delayFraction: Float,
    val travelFactor: Float,
    val radius: Float,
    val alpha: Float,
    val angle: Double = 0.0,
    val xFraction: Float = 0.5f,
    val horizontalDrift: Float = 0f,
    val startYFraction: Float = -0.18f,
    val spinTurns: Float = 0f,
)

internal data class BookPose(
    val lift: Float,
    val pitchX: Float,
    val yawY: Float,
    val openAngle: Float,
    val pageFan: Float,
    val spreadWidth: Float,
    val shadowAlpha: Float,
    val frontCoverDominance: Float,
)

fun pickSkyBackdropVariant(index: Int): SkyBackdropVariant {
    val variants = SkyBackdropVariant.entries
    return variants[index.mod(variants.size)]
}

fun randomSkyBackdropVariant(random: Random = Random.Default): SkyBackdropVariant =
    pickSkyBackdropVariant(random.nextInt(SkyBackdropVariant.entries.size))

internal fun calculateBookPose(progress: Float): BookPose {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val liftPhase = normalizedPhase(
        progress = clampedProgress,
        start = 0f,
        end = 0.30f,
    )
    val openPhase = normalizedPhase(
        progress = clampedProgress,
        start = 0.30f,
        end = 0.78f,
    )
    val settlePhase = normalizedPhase(
        progress = clampedProgress,
        start = 0.78f,
        end = 1f,
    )

    val lifted = easeOutCubic(liftPhase)
    val opened = easeOutCubic(openPhase)
    val settled = easeOutCubic(settlePhase)
    val settleBounce = sin(settled * PI.toFloat()).coerceAtLeast(0f) * (1f - settled) * 0.035f

    val baseOpenAngle = 142f * opened
    val openAngle = (baseOpenAngle + settleBounce * 20f).coerceIn(0f, 142f)
    val spreadWidth = scalarLerp(0.78f, 1.18f, opened) + settled * 0.08f + settleBounce

    return BookPose(
        lift = lifted,
        pitchX = scalarLerp(12f, 10f, settled),
        yawY = scalarLerp(
            start = -14f,
            stop = scalarLerp(-10f, -6f, settled),
            fraction = opened,
        ),
        openAngle = openAngle,
        pageFan = scalarLerp(0f, 12f, opened * 0.88f + settled * 0.12f),
        spreadWidth = spreadWidth,
        shadowAlpha = scalarLerp(0.18f, 0.34f, lifted * 0.7f + opened * 0.3f),
        frontCoverDominance = scalarLerp(1f, 0.42f, opened).coerceIn(0.3f, 1f),
    )
}

fun packOpeningHolographicMotion(
    relativePageOffset: Float,
    settleCueProgress: Float,
    interactiveEffectsEnabled: Boolean,
): HolographicCardMotion {
    val clampedOffset = relativePageOffset.coerceIn(-1f, 1f)
    val proximity = (1f - kotlin.math.abs(clampedOffset)).coerceIn(0f, 1f)
    val interactionAmount = (1f - proximity).coerceIn(0f, 1f)
    val cueBoost = settleCueProgress.coerceIn(0f, 1f)

    return HolographicCardMotion(
        rotationYDeg = if (interactiveEffectsEnabled) {
            (-clampedOffset * (2.8f + interactionAmount * 4.2f + cueBoost * 1.1f)).coerceIn(-7.5f, 7.5f)
        } else {
            0f
        },
        sweepFraction = (0.5f - clampedOffset * 0.38f + cueBoost * 0.22f).coerceIn(0.06f, 0.94f),
        highlightAlpha = (0.22f + interactionAmount * 0.28f + cueBoost * 0.42f).coerceIn(0.16f, 0.86f),
        edgeGlowAlpha = (0.24f + proximity * 0.16f + cueBoost * 0.28f).coerceIn(0.2f, 0.78f),
        sparkleBoost = if (interactiveEffectsEnabled) {
            (0.14f + interactionAmount * 0.58f + cueBoost * 0.48f).coerceIn(0f, 1f)
        } else {
            0f
        },
    )
}

fun autoplayHolographicMotion(
    loopProgress: Float,
    interactiveEffectsEnabled: Boolean,
): HolographicCardMotion {
    val primaryWave = sin(loopProgress * PI * 2).toFloat()
    val secondaryWave = sin(loopProgress * PI * 4).toFloat()
    val primaryNormalized = (primaryWave + 1f) / 2f
    val secondaryNormalized = (secondaryWave + 1f) / 2f

    return HolographicCardMotion(
        rotationYDeg = if (interactiveEffectsEnabled) {
            scalarLerp(-1.8f, 1.8f, primaryNormalized)
        } else {
            0f
        },
        sweepFraction = scalarLerp(0.18f, 0.82f, primaryNormalized),
        highlightAlpha = scalarLerp(0.22f, 0.4f, secondaryNormalized),
        edgeGlowAlpha = scalarLerp(0.24f, 0.42f, primaryNormalized),
        sparkleBoost = if (interactiveEffectsEnabled) {
            scalarLerp(0.18f, 0.46f, secondaryNormalized)
        } else {
            0f
        },
    )
}

fun extensionAnimationSpec(extensionId: String): ExtensionAnimationSpec = when (extensionId) {
    "astronomes-en-herbe" -> ExtensionAnimationSpec(
        style = ExtensionAnimationStyle.BigDipper,
        starPattern = listOf(
            FractionalPoint(0.000f, 0.375f), // Alkaid
            FractionalPoint(0.125f, 0.630f), // Mizar
            FractionalPoint(0.306f, 0.690f), // Alioth
            FractionalPoint(0.542f, 0.758f), // Megrez
            FractionalPoint(0.968f, 1.000f), // Dubhe
            FractionalPoint(1.000f, 0.769f), // Merak
            FractionalPoint(0.688f, 0.625f), // Phecda
        ),
        lineConnections = listOf(
            0 to 1,
            1 to 2,
            2 to 3,
            3 to 4,
            4 to 5,
            5 to 6,
            6 to 3,
        ),
    )
    else -> ExtensionAnimationSpec(style = ExtensionAnimationStyle.NeutralSky)
}

fun projectExtensionPattern(
    spec: ExtensionAnimationSpec,
    canvasWidth: Float,
    canvasHeight: Float,
): ExtensionPatternProjection {
    if (spec.starPattern.isEmpty()) {
        return ExtensionPatternProjection(
            originX = canvasWidth / 2f,
            originY = canvasHeight / 2f,
            scale = 0f,
            minX = 0f,
            maxY = 0f,
        )
    }

    val minX = spec.starPattern.minOf { it.x.toDouble() }.toFloat()
    val maxX = spec.starPattern.maxOf { it.x.toDouble() }.toFloat()
    val minY = spec.starPattern.minOf { it.y.toDouble() }.toFloat()
    val maxY = spec.starPattern.maxOf { it.y.toDouble() }.toFloat()
    val patternWidth = (maxX - minX).coerceAtLeast(0.0001f)
    val patternHeight = (maxY - minY).coerceAtLeast(0.0001f)
    val scale = min(canvasWidth / patternWidth, canvasHeight / patternHeight)
    val projectedWidth = patternWidth * scale
    val projectedHeight = patternHeight * scale
    val originX = (canvasWidth - projectedWidth) / 2f
    val originY = (canvasHeight - projectedHeight) / 2f

    return ExtensionPatternProjection(
        originX = originX,
        originY = originY,
        scale = scale,
        minX = minX,
        maxY = maxY,
    )
}

fun extensionLineReveal(
    lineProgress: Float,
    lineIndex: Int,
    lineCount: Int,
    revealWindow: Float,
): Float {
    if (lineCount <= 0) return 0f
    val segmentStart = lineIndex.toFloat() / lineCount.toFloat()
    val segmentEnd = (lineIndex + 1).toFloat() / lineCount.toFloat()
    val segmentSpan = (segmentEnd - segmentStart).coerceAtLeast(0.0001f)
    return ((lineProgress - segmentStart) / segmentSpan).coerceIn(0f, 1f)
}

fun extensionPointReveal(
    spec: ExtensionAnimationSpec,
    pointIndex: Int,
    lineProgress: Float,
    isReversing: Boolean,
    revealWindow: Float,
): Float {
    val adjacentReveals = spec.lineConnections.mapIndexedNotNull { index, connection ->
        if (connection.first == pointIndex || connection.second == pointIndex) {
            extensionLineReveal(
                lineProgress = lineProgress,
                lineIndex = index,
                lineCount = spec.lineConnections.size,
                revealWindow = revealWindow,
            )
        } else {
            null
        }
    }
    if (adjacentReveals.isEmpty()) return 0f

    return if (isReversing) {
        if (adjacentReveals.any { it >= 0.999f }) 1f else 0f
    } else {
        adjacentReveals.maxOrNull() ?: 0f
    }
}

fun summarizePackOpening(displayCards: List<DisplayCard>): PackOpeningSummary? {
    if (displayCards.isEmpty()) return null

    val highestRarityLabel = displayCards
        .maxBy { raritySortPriority(it.definition.rarityLabel) }
        .definition
        .rarityLabel

    return PackOpeningSummary(
        highestRarityLabel = highestRarityLabel,
        hasHolographicCard = displayCards.any { it.activeVariant.isHolographic },
    )
}

fun burstRarityLabelsUpTo(highestRarityLabel: String?): List<String> {
    val rarities = listOf("Common", "Uncommon", "Rare", "Epic")
    val highestPriority = highestRarityLabel?.let(::raritySortPriority) ?: return emptyList()
    return rarities.filter { raritySortPriority(it) <= highestPriority }
}

fun buildBurstParticleSpecs(
    highestRarityLabel: String?,
    hasHolographicBurst: Boolean,
): List<BurstParticleSpec> {
    val allowedRarities = burstRarityLabelsUpTo(highestRarityLabel)
    if (allowedRarities.isEmpty()) return emptyList()
    val random = Random(
        seed = ((highestRarityLabel?.hashCode() ?: 0) * 31) + if (hasHolographicBurst) 1 else 0,
    )

    var radialCount = 92 + allowedRarities.size * 28
    if (hasHolographicBurst) radialCount += 72
    val radialParticles = List(radialCount) {
        val rarityLabel = allowedRarities[random.nextInt(allowedRarities.size)]
        val driftMagnitude = if (hasHolographicBurst) {
            0.032f + random.nextFloat() * 0.062f
        } else {
            0.008f + random.nextFloat() * 0.016f
        }
        BurstParticleSpec(
            rarityLabel = rarityLabel,
            motion = BurstParticleMotion.Radial,
            delayFraction = 0.04f + random.nextFloat() * 0.8f,
            travelFactor = if (hasHolographicBurst) {
                5.2f + random.nextFloat() * 5.4f
            } else {
                4.4f + random.nextFloat() * 4.8f
            },
            radius = if (hasHolographicBurst) {
                0.0185f + random.nextFloat() * 0.0225f
            } else {
                0.0175f + random.nextFloat() * 0.0200f
            },
            alpha = if (hasHolographicBurst) {
                0.5f + random.nextFloat() * 0.34f
            } else {
                0.42f + random.nextFloat() * 0.34f
            },
            angle = (-PI / 2f) + (random.nextFloat() - 0.5f) * if (hasHolographicBurst) 0.62f else 0.42f,
            horizontalDrift = if (random.nextBoolean()) {
                driftMagnitude
            } else {
                -driftMagnitude
            },
            spinTurns = if (hasHolographicBurst) {
                1.35f + random.nextFloat() * 1.45f
            } else {
                0.45f + random.nextFloat() * 0.45f
            },
        )
    }

    if (!hasHolographicBurst) return radialParticles

    val fallingCount = 56
    val fallingParticles = List(fallingCount) {
        BurstParticleSpec(
            rarityLabel = allowedRarities[random.nextInt(allowedRarities.size)],
            motion = BurstParticleMotion.Falling,
            delayFraction = 0.16f + random.nextFloat() * 0.66f,
            travelFactor = 1.3f + random.nextFloat() * 0.7f,
            radius = 0.0185f + random.nextFloat() * 0.0215f,
            alpha = 0.46f + random.nextFloat() * 0.28f,
            xFraction = 0.06f + random.nextFloat() * 0.88f,
            horizontalDrift = -0.14f + random.nextFloat() * 0.28f,
            startYFraction = -0.18f - random.nextFloat() * 0.2f,
            angle = random.nextFloat().toDouble() * PI * 2,
            spinTurns = 1.1f + random.nextFloat() * 1.9f,
        )
    }

    val orbitalCount = 24
    val orbitalParticles = List(orbitalCount) {
        BurstParticleSpec(
            rarityLabel = allowedRarities[random.nextInt(allowedRarities.size)],
            motion = BurstParticleMotion.Orbital,
            delayFraction = 0.08f + random.nextFloat() * 0.44f,
            travelFactor = 0.56f + random.nextFloat() * 0.88f,
            radius = 0.0200f + random.nextFloat() * 0.0240f,
            alpha = 0.54f + random.nextFloat() * 0.24f,
            angle = random.nextFloat().toDouble() * PI * 2,
            horizontalDrift = 0.22f + random.nextFloat() * 0.16f,
            startYFraction = -0.08f + random.nextFloat() * 0.16f,
            spinTurns = 0.95f + random.nextFloat() * 1.8f,
        )
    }
    return radialParticles + fallingParticles + orbitalParticles
}

private fun normalizedPhase(
    progress: Float,
    start: Float,
    end: Float,
): Float = ((progress - start) / (end - start).coerceAtLeast(0.0001f)).coerceIn(0f, 1f)

private fun easeOutCubic(value: Float): Float {
    val clamped = value.coerceIn(0f, 1f)
    return 1f - (1f - clamped) * (1f - clamped) * (1f - clamped)
}
