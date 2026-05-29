package fr.aumombelli.dstcg.ui.motion

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import fr.aumombelli.dstcg.performance.LocalAppPerformanceProfile
import fr.aumombelli.dstcg.ui.theme.EmberGold
import fr.aumombelli.dstcg.ui.theme.skyQualityPalette
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun AppSkyBackdrop(
    variant: SkyBackdropVariant,
    modifier: Modifier = Modifier,
    cameraTiltProgress: Float = 0f,
    horizonLightAlpha: Float = 1f,
    mountainBlendProgress: Float = 0f,
) {
    val performanceProfile = LocalAppPerformanceProfile.current
    val sourcePalette = skyQualityPalette(variant.skyQuality)
    val mountainPalette = skyQualityPalette("mountain")
    val paletteTop = lerp(sourcePalette.top, mountainPalette.top, mountainBlendProgress)
    val paletteBottom = lerp(sourcePalette.bottom, mountainPalette.bottom, mountainBlendProgress)
    val paletteGlow = lerp(sourcePalette.glow, mountainPalette.glow, mountainBlendProgress)
    val twinkleProgress = if (performanceProfile.enableAnimatedBackdrop) {
        val twinkleTransition = rememberInfiniteTransition(label = "app-backdrop-twinkle")
        val animatedProgress by twinkleTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "app-backdrop-twinkle-progress",
        )
        animatedProgress
    } else {
        0.24f
    }
    val stars = remember(variant, performanceProfile.backdropStarDensityMultiplier) {
        buildSkyStars(
            variant = variant,
            densityMultiplier = performanceProfile.backdropStarDensityMultiplier,
        )
    }
    val lights = remember(variant) { buildHorizonLights(variant) }
    val buildingWindows = remember(variant) { buildBuildingWindowLights(variant) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("app-shared-backdrop"),
    ) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(paletteTop, paletteBottom),
            ),
            size = size,
        )

        val horizonAlpha = (1f - cameraTiltProgress).coerceIn(0f, 1f)
        val horizonBase = size.height * (0.74f + cameraTiltProgress * 0.26f)

        stars.forEach { star ->
            val pulse = ((sin((twinkleProgress + star.phase) * PI * 2).toFloat() + 1f) / 2f)
            val alpha = (0.18f + pulse * 0.82f) * (0.9f + mountainBlendProgress * 0.1f)
            val x = size.width * star.x
            val y = size.height * (star.y + cameraTiltProgress * STAR_VERTICAL_TRAVEL)
            val center = Offset(x, y)
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = size.minDimension * star.radius,
                center = center,
            )
            drawCircle(
                color = EmberGold.copy(alpha = alpha * 0.28f),
                radius = size.minDimension * star.radius * 1.6f,
                center = center,
            )
        }

        if (horizonAlpha > 0f) {
            val foregroundPath = buildForegroundPath(
                variant = variant,
                size = size,
                horizonBase = horizonBase,
            )
            drawPath(
                path = foregroundPath,
                color = Color(0xFF09111E).copy(alpha = horizonAlpha),
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        paletteGlow.copy(alpha = 0.3f * horizonAlpha),
                        Color.Black.copy(alpha = 0.72f * horizonAlpha),
                    ),
                    startY = horizonBase - size.height * 0.1f,
                    endY = size.height,
                ),
                topLeft = Offset(0f, horizonBase - size.height * 0.1f),
                size = Size(size.width, size.height - horizonBase + size.height * 0.1f),
            )
            if (buildingWindows.isNotEmpty()) {
                clipPath(foregroundPath) {
                    drawBuildingWindowLights(
                        windows = buildingWindows,
                        horizonBase = horizonBase,
                        horizonAlpha = horizonAlpha,
                        lightAlpha = horizonLightAlpha,
                    )
                }
            }
        }

        if (variant.hasHorizonLights) {
            lights.forEach { light ->
                val center = Offset(size.width * light.x, horizonBase + size.height * light.yOffset)
                drawCircle(
                    color = HorizonLightYellow.copy(alpha = light.alpha * horizonAlpha * horizonLightAlpha),
                    radius = size.minDimension * light.radius,
                    center = center,
                )
                drawCircle(
                    color = HorizonLightHalo.copy(alpha = light.alpha * horizonAlpha * horizonLightAlpha * 0.55f),
                    radius = size.minDimension * light.radius * 2.1f,
                    center = center,
                )
            }
        }
    }
}

private data class SkyStar(
    val x: Float,
    val y: Float,
    val radius: Float,
    val phase: Float,
)

private data class HorizonLight(
    val x: Float,
    val yOffset: Float,
    val radius: Float,
    val alpha: Float,
)

internal data class BuildingWindowLight(
    val x: Float,
    val yOffset: Float,
    val width: Float,
    val height: Float,
    val alpha: Float,
)

private data class WindowFacadeSpec(
    val startX: Float,
    val endX: Float,
    val topYOffset: Float,
    val bottomYOffset: Float,
    val columns: Int,
    val rows: Int,
    val activationProbability: Float,
    val insetX: Float = 0.008f,
    val insetTop: Float = 0.014f,
    val insetBottom: Float = 0.016f,
)

private const val VISIBLE_STAR_BAND = 0.82f
private const val STAR_TOP_OVERFLOW = 0.16f
private const val STAR_VERTICAL_TRAVEL = 0.12f
private val HorizonLightYellow = Color(0xFFFFD95A)
private val HorizonLightHalo = Color(0xFFFFF0A6)

private fun buildForegroundPath(
    variant: SkyBackdropVariant,
    size: Size,
    horizonBase: Float,
): Path = when (variant) {
    SkyBackdropVariant.City -> buildCityForegroundPath(size, horizonBase)
    SkyBackdropVariant.Suburban -> buildSuburbanForegroundPath(size, horizonBase)
    SkyBackdropVariant.Rural -> buildRuralForegroundPath(size, horizonBase)
    SkyBackdropVariant.Mountain -> buildMountainForegroundPath(size, horizonBase)
}

private fun buildCityForegroundPath(
    size: Size,
    horizonBase: Float,
): Path = Path().apply {
    moveTo(0f, size.height)
    lineTo(0f, horizonBase - size.height * 0.05f)
    lineTo(size.width * 0.08f, horizonBase - size.height * 0.05f)
    lineTo(size.width * 0.08f, horizonBase - size.height * 0.17f)
    lineTo(size.width * 0.16f, horizonBase - size.height * 0.17f)
    lineTo(size.width * 0.16f, horizonBase - size.height * 0.09f)
    lineTo(size.width * 0.21f, horizonBase - size.height * 0.09f)
    lineTo(size.width * 0.21f, horizonBase - size.height * 0.21f)
    lineTo(size.width * 0.33f, horizonBase - size.height * 0.21f)
    lineTo(size.width * 0.33f, horizonBase - size.height * 0.13f)
    lineTo(size.width * 0.41f, horizonBase - size.height * 0.13f)
    lineTo(size.width * 0.41f, horizonBase - size.height * 0.27f)
    lineTo(size.width * 0.52f, horizonBase - size.height * 0.27f)
    lineTo(size.width * 0.52f, horizonBase - size.height * 0.08f)
    lineTo(size.width * 0.6f, horizonBase - size.height * 0.08f)
    lineTo(size.width * 0.6f, horizonBase - size.height * 0.19f)
    lineTo(size.width * 0.71f, horizonBase - size.height * 0.19f)
    lineTo(size.width * 0.71f, horizonBase - size.height * 0.11f)
    lineTo(size.width * 0.77f, horizonBase - size.height * 0.11f)
    lineTo(size.width * 0.77f, horizonBase - size.height * 0.23f)
    lineTo(size.width * 0.9f, horizonBase - size.height * 0.23f)
    lineTo(size.width * 0.9f, horizonBase - size.height * 0.06f)
    lineTo(size.width, horizonBase - size.height * 0.06f)
    lineTo(size.width, size.height)
    close()
}

private fun buildSuburbanForegroundPath(
    size: Size,
    horizonBase: Float,
): Path = Path().apply {
    moveTo(0f, size.height)
    lineTo(0f, horizonBase - size.height * 0.03f)
    lineTo(size.width * 0.08f, horizonBase - size.height * 0.05f)
    lineTo(size.width * 0.08f, horizonBase - size.height * 0.11f)
    lineTo(size.width * 0.16f, horizonBase - size.height * 0.11f)
    lineTo(size.width * 0.16f, horizonBase - size.height * 0.06f)
    lineTo(size.width * 0.21f, horizonBase - size.height * 0.06f)
    lineTo(size.width * 0.21f, horizonBase - size.height * 0.13f)
    lineTo(size.width * 0.33f, horizonBase - size.height * 0.13f)
    lineTo(size.width * 0.33f, horizonBase - size.height * 0.08f)
    lineTo(size.width * 0.41f, horizonBase - size.height * 0.08f)
    lineTo(size.width * 0.41f, horizonBase - size.height * 0.05f)
    lineTo(size.width * 0.52f, horizonBase - size.height * 0.05f)
    lineTo(size.width * 0.62f, horizonBase - size.height * 0.03f)
    lineTo(size.width * 0.66f, horizonBase - size.height * 0.09f)
    lineTo(size.width * 0.72f, horizonBase - size.height * 0.03f)
    lineTo(size.width * 0.76f, horizonBase - size.height * 0.03f)
    lineTo(size.width * 0.81f, horizonBase - size.height * 0.12f)
    lineTo(size.width * 0.88f, horizonBase - size.height * 0.03f)
    lineTo(size.width * 0.93f, horizonBase - size.height * 0.03f)
    lineTo(size.width * 0.97f, horizonBase - size.height * 0.08f)
    lineTo(size.width, horizonBase - size.height * 0.05f)
    lineTo(size.width, size.height)
    close()
}

private fun buildRuralForegroundPath(
    size: Size,
    horizonBase: Float,
): Path = Path().apply {
    moveTo(0f, size.height)
    lineTo(0f, horizonBase - size.height * 0.01f)
    lineTo(size.width * 0.12f, horizonBase - size.height * 0.015f)
    lineTo(size.width * 0.14f, horizonBase - size.height * 0.060f)
    lineTo(size.width * 0.16f, horizonBase - size.height * 0.080f)
    lineTo(size.width * 0.17f, horizonBase - size.height * 0.070f)
    lineTo(size.width * 0.22f, horizonBase - size.height * 0.015f)
    lineTo(size.width * 0.30f, horizonBase - size.height * 0.015f)
    lineTo(size.width * 0.36f, horizonBase - size.height * 0.005f)
    lineTo(size.width * 0.55f, horizonBase - size.height * 0.012f)
    lineTo(size.width * 0.67f, horizonBase - size.height * 0.012f)
    lineTo(size.width * 0.70f, horizonBase - size.height * 0.070f)
    lineTo(size.width * 0.72f, horizonBase - size.height * 0.075f)
    lineTo(size.width * 0.76f, horizonBase - size.height * 0.069f)
    lineTo(size.width * 0.79f, horizonBase - size.height * 0.012f)
    lineTo(size.width * 0.88f, horizonBase - size.height * 0.012f)
    lineTo(size.width * 0.90f, horizonBase - size.height * 0.059f)
    lineTo(size.width * 0.92f, horizonBase - size.height * 0.060f)
    lineTo(size.width * 0.94f, horizonBase - size.height * 0.056f)
    lineTo(size.width * 0.97f, horizonBase - size.height * 0.012f)
    lineTo(size.width, horizonBase - size.height * 0.012f)
    lineTo(size.width, size.height)
    close()
}

private fun buildMountainForegroundPath(
    size: Size,
    horizonBase: Float,
): Path = Path().apply {
    moveTo(0f, size.height)
    lineTo(0f, horizonBase)
    lineTo(size.width * 0.16f, horizonBase - size.height * 0.08f)
    lineTo(size.width * 0.28f, horizonBase - size.height * 0.03f)
    lineTo(size.width * 0.41f, horizonBase - size.height * 0.11f)
    lineTo(size.width * 0.55f, horizonBase - size.height * 0.02f)
    lineTo(size.width * 0.72f, horizonBase - size.height * 0.14f)
    lineTo(size.width * 0.84f, horizonBase - size.height * 0.05f)
    lineTo(size.width, horizonBase - size.height * 0.09f)
    lineTo(size.width, size.height)
    close()
}

private fun buildSkyStars(
    variant: SkyBackdropVariant,
    densityMultiplier: Float,
): List<SkyStar> {
    val random = Random(variant.ordinal * 11 + 19)
    val generatedStarCount = ceil(
        variant.twinklingStarCount * densityMultiplier * (VISIBLE_STAR_BAND + STAR_TOP_OVERFLOW) / VISIBLE_STAR_BAND,
    ).toInt()
    return List(generatedStarCount) {
        SkyStar(
            x = random.nextFloat(),
            y = -STAR_TOP_OVERFLOW + random.nextFloat() * (VISIBLE_STAR_BAND + STAR_TOP_OVERFLOW),
            radius = 0.0018f + random.nextFloat() * 0.0035f,
            phase = random.nextFloat(),
        )
    }
}

private fun buildHorizonLights(variant: SkyBackdropVariant): List<HorizonLight> {
    val random = Random(variant.ordinal * 17 + 7)
    return List(variant.horizonLightCount) {
        HorizonLight(
            x = 0.01f + random.nextFloat() * 0.98f,
            yOffset = -0.03f + random.nextFloat() * 0.085f,
            radius = 0.0032f + random.nextFloat() * 0.0044f,
            alpha = 0.35f + random.nextFloat() * 0.45f,
        )
    }
}

internal fun buildBuildingWindowLights(variant: SkyBackdropVariant): List<BuildingWindowLight> = when (variant) {
    SkyBackdropVariant.City -> buildFacadeWindowLights(
        seed = 113,
        facades = listOf(
            WindowFacadeSpec(0.016f, 0.078f, -0.05f, -0.014f, columns = 2, rows = 2, activationProbability = 0.55f),
            WindowFacadeSpec(0.084f, 0.158f, -0.17f, -0.016f, columns = 3, rows = 4, activationProbability = 0.68f),
            WindowFacadeSpec(0.164f, 0.208f, -0.09f, -0.016f, columns = 2, rows = 2, activationProbability = 0.62f),
            WindowFacadeSpec(0.214f, 0.328f, -0.21f, -0.018f, columns = 4, rows = 5, activationProbability = 0.66f),
            WindowFacadeSpec(0.334f, 0.408f, -0.13f, -0.016f, columns = 3, rows = 3, activationProbability = 0.6f),
            WindowFacadeSpec(0.414f, 0.518f, -0.27f, -0.018f, columns = 4, rows = 6, activationProbability = 0.69f),
            WindowFacadeSpec(0.524f, 0.598f, -0.08f, -0.015f, columns = 3, rows = 2, activationProbability = 0.58f),
            WindowFacadeSpec(0.604f, 0.708f, -0.19f, -0.018f, columns = 4, rows = 4, activationProbability = 0.65f),
            WindowFacadeSpec(0.714f, 0.768f, -0.11f, -0.015f, columns = 2, rows = 3, activationProbability = 0.57f),
            WindowFacadeSpec(0.774f, 0.898f, -0.23f, -0.018f, columns = 5, rows = 5, activationProbability = 0.67f),
            WindowFacadeSpec(0.904f, 0.992f, -0.06f, -0.015f, columns = 3, rows = 2, activationProbability = 0.5f),
        ),
    )
    SkyBackdropVariant.Suburban -> buildFacadeWindowLights(
        seed = 151,
        facades = listOf(
            WindowFacadeSpec(0.088f, 0.156f, -0.11f, -0.018f, columns = 2, rows = 3, activationProbability = 0.78f),
            WindowFacadeSpec(0.214f, 0.326f, -0.13f, -0.018f, columns = 3, rows = 3, activationProbability = 0.76f),
            WindowFacadeSpec(0.336f, 0.406f, -0.08f, -0.018f, columns = 2, rows = 2, activationProbability = 0.72f),
            WindowFacadeSpec(0.414f, 0.488f, -0.05f, -0.018f, columns = 2, rows = 1, activationProbability = 0.65f),
        ),
    )
    SkyBackdropVariant.Rural,
    SkyBackdropVariant.Mountain,
    -> emptyList()
}

private fun buildFacadeWindowLights(
    seed: Int,
    facades: List<WindowFacadeSpec>,
): List<BuildingWindowLight> {
    val random = Random(seed)
    return buildList {
        facades.forEach { facade ->
            val usableWidth = facade.endX - facade.startX - facade.insetX * 2f
            val usableHeight = facade.bottomYOffset - facade.topYOffset - facade.insetTop - facade.insetBottom
            if (usableWidth <= 0f || usableHeight <= 0f) return@forEach

            val stepX = usableWidth / facade.columns
            val stepY = usableHeight / facade.rows

            repeat(facade.columns) { column ->
                repeat(facade.rows) { row ->
                    if (random.nextFloat() <= facade.activationProbability) {
                        add(
                            BuildingWindowLight(
                                x = facade.startX + facade.insetX + stepX * (column + 0.5f) +
                                    (random.nextFloat() - 0.5f) * stepX * 0.12f,
                                yOffset = facade.topYOffset + facade.insetTop + stepY * (row + 0.5f) +
                                    (random.nextFloat() - 0.5f) * stepY * 0.12f,
                                width = stepX * (0.26f + random.nextFloat() * 0.08f),
                                height = stepY * (0.32f + random.nextFloat() * 0.12f),
                                alpha = 0.46f + random.nextFloat() * 0.34f,
                            ),
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawBuildingWindowLights(
    windows: List<BuildingWindowLight>,
    horizonBase: Float,
    horizonAlpha: Float,
    lightAlpha: Float,
) {
    windows.forEach { window ->
        val width = size.width * window.width
        val height = size.height * window.height
        val center = Offset(
            x = size.width * window.x,
            y = horizonBase + size.height * window.yOffset,
        )
        val alpha = window.alpha * horizonAlpha * lightAlpha

        drawCircle(
            color = EmberGold.copy(alpha = alpha * 0.12f),
            radius = max(width, height) * 0.62f,
            center = center,
        )
        drawRect(
            color = EmberGold.copy(alpha = alpha),
            topLeft = Offset(center.x - width / 2f, center.y - height / 2f),
            size = Size(width, height),
        )
        drawRect(
            color = Color.White.copy(alpha = alpha * 0.34f),
            topLeft = Offset(center.x - width * 0.28f, center.y - height * 0.24f),
            size = Size(width * 0.56f, height * 0.48f),
        )
    }
}
