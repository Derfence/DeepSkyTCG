package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.audio.LocalAudioController
import fr.aumombelli.dstcg.audio.SoundCue
import fr.aumombelli.dstcg.performance.LocalAppPerformanceProfile
import fr.aumombelli.dstcg.ui.component.TwinklingStarsOverlay
import fr.aumombelli.dstcg.ui.motion.AppSkyBackdrop
import fr.aumombelli.dstcg.ui.motion.SkyBackdropVariant
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
internal fun MiniGameSceneBackdrop(
    modifier: Modifier = Modifier,
    variant: SkyBackdropVariant = SkyBackdropVariant.Suburban,
    mountainBlendProgress: Float = 0f,
    sparkleBoost: Float = 0.16f,
) {
    val performanceProfile = LocalAppPerformanceProfile.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050A10)),
    ) {
        AppSkyBackdrop(
            variant = variant,
            cameraTiltProgress = 0.08f,
            horizonLightAlpha = 0.62f,
            mountainBlendProgress = mountainBlendProgress,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x33000000),
                            Color(0x00000000),
                            Color(0xB8050810),
                        ),
                    ),
                ),
        )
        MiniGameConstellationOverlay(
            animated = performanceProfile.enableAnimatedBackdrop,
            modifier = Modifier.fillMaxSize(),
        )
        TwinklingStarsOverlay(
            animated = performanceProfile.enableAnimatedBackdrop,
            sparkleBoost = sparkleBoost,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
internal fun MiniGameBoardSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xB0102536),
                        Color(0xD006111E),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0x99A8F1FF),
                        Color(0x55F6C75D),
                        Color(0x6672D8FF),
                    ),
                ),
                shape = shape,
            )
            .padding(10.dp),
        content = content,
    )
}

@Composable
internal fun MiniGameHudPill(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color(0xAA07111A),
        contentColor = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
        ) {
            Text(
                text = label,
                color = Color(0xBFE1EEFF),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(0.9f),
            )
            Text(
                text = value,
                color = tint,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.1f),
            )
        }
    }
}

@Composable
internal fun MiniGamePulsingRing(
    enabled: Boolean,
    tone: MiniGameFeedbackTone,
    modifier: Modifier = Modifier,
) {
    val performanceProfile = LocalAppPerformanceProfile.current
    val progress = if (enabled && performanceProfile.enableAnimatedBackdrop) {
        val transition = rememberInfiniteTransition(label = "mini-game-node-pulse")
        val animatedProgress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "mini-game-node-pulse-progress",
        )
        animatedProgress
    } else {
        0.45f
    }
    val palette = feedbackPalette(tone)

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * (0.36f + progress * 0.2f)
        val alpha = if (enabled) (0.42f * (1f - progress)).coerceAtLeast(0.08f) else 0.12f
        drawCircle(
            color = palette.primary.copy(alpha = alpha),
            radius = radius,
            center = center,
            style = Stroke(width = size.minDimension * 0.05f),
        )
        drawCircle(
            color = palette.glow.copy(alpha = if (enabled) 0.22f else 0.08f),
            radius = size.minDimension * 0.42f,
            center = center,
        )
    }
}

@Composable
internal fun MiniGameFeedbackOverlay(
    cue: MiniGameFeedbackEvent?,
    modifier: Modifier = Modifier,
) {
    val performanceProfile = LocalAppPerformanceProfile.current
    val audioController = LocalAudioController.current
    val progress = remember { Animatable(1f) }
    LaunchedEffect(cue?.id) {
        if (cue != null) {
            audioController.play(cue.tone.soundCue())
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = if (cue.tone == MiniGameFeedbackTone.Completion) 1200 else 720,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    val visibleProgress = progress.value
    if (cue == null || visibleProgress >= 1f) return

    val particleCount = when {
        performanceProfile.isLowRamDevice -> 18
        cue.tone == MiniGameFeedbackTone.Completion -> 64
        cue.tone == MiniGameFeedbackTone.Special -> 48
        else -> 34
    }
    val particles = remember(cue.id, cue.tone, particleCount) {
        buildMiniGameSparkParticles(
            seed = cue.id * 31L + cue.tone.ordinal,
            count = particleCount,
            tone = cue.tone,
        )
    }
    val palette = feedbackPalette(cue.tone)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("mini-game-feedback-overlay"),
    ) {
        val center = Offset(size.width * 0.5f, size.height * 0.5f)
        val fade = (1f - visibleProgress).coerceIn(0f, 1f)
        val expansion = easeOutCubic(visibleProgress)
        val impact = easeOutCubic((visibleProgress / 0.28f).coerceIn(0f, 1f))

        drawCircle(
            color = palette.glow.copy(alpha = fade * 0.18f),
            radius = size.minDimension * (0.1f + expansion * 0.46f),
            center = center,
        )
        drawCircle(
            color = palette.primary.copy(alpha = fade * 0.42f),
            radius = size.minDimension * (0.12f + impact * 0.28f),
            center = center,
            style = Stroke(width = size.minDimension * (0.018f - visibleProgress * 0.011f).coerceAtLeast(0.004f)),
        )
        particles.forEach { particle ->
            val particleProgress = normalizedProgress(visibleProgress, particle.delay)
            if (particleProgress <= 0f) return@forEach

            val easedProgress = easeOutCubic(particleProgress)
            val angle = particle.angle + sin((particleProgress * PI * 2) + particle.phase).toFloat() * 0.16f
            val travel = size.minDimension * particle.travel * easedProgress
            val lift = size.height * particle.lift * easedProgress
            val particleCenter = Offset(
                x = center.x + cos(angle).toFloat() * travel,
                y = center.y + sin(angle).toFloat() * travel * 0.68f - lift,
            )
            val alpha = ((1f - particleProgress) * particle.alpha).coerceIn(0f, 1f)
            val radius = size.minDimension * particle.radius * (1f + particleProgress * 0.22f)
            drawPath(
                path = miniGameStarPath(
                    center = particleCenter,
                    points = particle.points,
                    outerRadius = radius,
                    innerRadius = radius * 0.42f,
                ),
                color = particle.color.copy(alpha = alpha),
                style = Fill,
            )
            drawPath(
                path = miniGameStarPath(
                    center = particleCenter,
                    points = particle.points,
                    outerRadius = radius,
                    innerRadius = radius * 0.42f,
                ),
                color = Color.White.copy(alpha = alpha * 0.56f),
                style = Stroke(width = size.minDimension * 0.003f),
            )
        }
    }
}

@Composable
private fun MiniGameConstellationOverlay(
    animated: Boolean,
    modifier: Modifier = Modifier,
) {
    val progress = if (animated) {
        val transition = rememberInfiniteTransition(label = "mini-game-constellation")
        val animatedProgress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 6200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "mini-game-constellation-progress",
        )
        animatedProgress
    } else {
        0.35f
    }

    Canvas(modifier = modifier) {
        val points = MiniGameConstellationPoints.map { point ->
            Offset(
                x = size.width * point.x,
                y = size.height * (point.y + sin((progress + point.phase) * PI * 2).toFloat() * 0.006f),
            )
        }
        points.zipWithNext().forEach { (start, end) ->
            drawLine(
                color = Color(0x338DEBFF),
                start = start,
                end = end,
                strokeWidth = size.minDimension * 0.0026f,
            )
        }
        points.forEachIndexed { index, point ->
            val pulse = (sin((progress + MiniGameConstellationPoints[index].phase) * PI * 2).toFloat() + 1f) / 2f
            drawCircle(
                color = Color.White.copy(alpha = 0.22f + pulse * 0.34f),
                radius = size.minDimension * (0.0038f + pulse * 0.0028f),
                center = point,
            )
        }
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x1DF6C75D),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.5f, size.height * 0.56f),
                radius = size.minDimension * 0.72f,
            ),
            cornerRadius = CornerRadius(size.minDimension * 0.08f),
        )
    }
}

private data class MiniGameSparkParticle(
    val angle: Double,
    val phase: Double,
    val delay: Float,
    val travel: Float,
    val lift: Float,
    val radius: Float,
    val alpha: Float,
    val points: Int,
    val color: Color,
)

private data class MiniGameConstellationPoint(
    val x: Float,
    val y: Float,
    val phase: Float,
)

private data class MiniGameFeedbackPalette(
    val primary: Color,
    val glow: Color,
    val accents: List<Color>,
)

private fun buildMiniGameSparkParticles(
    seed: Long,
    count: Int,
    tone: MiniGameFeedbackTone,
): List<MiniGameSparkParticle> {
    val random = Random(seed)
    val palette = feedbackPalette(tone)
    return List(count) {
        MiniGameSparkParticle(
            angle = random.nextDouble(0.0, PI * 2),
            phase = random.nextDouble(0.0, PI * 2),
            delay = random.nextFloat() * 0.28f,
            travel = 0.18f + random.nextFloat() * if (tone == MiniGameFeedbackTone.Completion) 0.72f else 0.46f,
            lift = if (tone == MiniGameFeedbackTone.Error) 0.0f else random.nextFloat() * 0.16f,
            radius = 0.007f + random.nextFloat() * 0.011f,
            alpha = 0.42f + random.nextFloat() * 0.44f,
            points = if (random.nextFloat() > 0.72f) 5 else 4,
            color = palette.accents[random.nextInt(palette.accents.size)],
        )
    }
}

private fun feedbackPalette(tone: MiniGameFeedbackTone): MiniGameFeedbackPalette = when (tone) {
    MiniGameFeedbackTone.Success -> MiniGameFeedbackPalette(
        primary = Color(0xFF75E0C2),
        glow = Color(0xFF3EE5D0),
        accents = listOf(Color(0xFF75E0C2), Color(0xFFEAF7FF), Color(0xFFF6C75D)),
    )

    MiniGameFeedbackTone.Error -> MiniGameFeedbackPalette(
        primary = Color(0xFFFF8A7A),
        glow = Color(0xFFFF5F6D),
        accents = listOf(Color(0xFFFF8A7A), Color(0xFFFFC4BD), Color(0xFFF6C75D)),
    )

    MiniGameFeedbackTone.Special -> MiniGameFeedbackPalette(
        primary = Color(0xFF9AEAFF),
        glow = Color(0xFFFFB7EA),
        accents = listOf(Color(0xFF9AEAFF), Color(0xFFFFB7EA), Color(0xFFFFF1A8), Color.White),
    )

    MiniGameFeedbackTone.Completion -> MiniGameFeedbackPalette(
        primary = Color(0xFFF6C75D),
        glow = Color(0xFF8DEBFF),
        accents = listOf(Color(0xFFF6C75D), Color(0xFF8DEBFF), Color(0xFFFFC7EF), Color.White),
    )
}

private fun MiniGameFeedbackTone.soundCue(): SoundCue = when (this) {
    MiniGameFeedbackTone.Success -> SoundCue.MiniGameSuccess
    MiniGameFeedbackTone.Error -> SoundCue.MiniGameError
    MiniGameFeedbackTone.Special -> SoundCue.MiniGameSpecial
    MiniGameFeedbackTone.Completion -> SoundCue.MiniGameCompletion
}

private fun normalizedProgress(progress: Float, delay: Float): Float {
    val denominator = (1f - delay).coerceAtLeast(0.001f)
    return ((progress - delay) / denominator).coerceIn(0f, 1f)
}

private fun easeOutCubic(value: Float): Float {
    val clamped = value.coerceIn(0f, 1f)
    return 1f - (1f - clamped) * (1f - clamped) * (1f - clamped)
}

private fun miniGameStarPath(
    center: Offset,
    points: Int,
    outerRadius: Float,
    innerRadius: Float,
): Path {
    val path = Path()
    val angleStep = PI / points
    repeat(points * 2) { index ->
        val radius = if (index % 2 == 0) outerRadius else innerRadius
        val angle = -PI / 2 + index * angleStep
        val point = Offset(
            x = center.x + cos(angle).toFloat() * radius,
            y = center.y + sin(angle).toFloat() * radius,
        )
        if (index == 0) {
            path.moveTo(point.x, point.y)
        } else {
            path.lineTo(point.x, point.y)
        }
    }
    path.close()
    return path
}

private val MiniGameConstellationPoints = listOf(
    MiniGameConstellationPoint(0.18f, 0.2f, 0.0f),
    MiniGameConstellationPoint(0.34f, 0.16f, 0.18f),
    MiniGameConstellationPoint(0.52f, 0.24f, 0.36f),
    MiniGameConstellationPoint(0.67f, 0.19f, 0.52f),
    MiniGameConstellationPoint(0.82f, 0.31f, 0.72f),
)
