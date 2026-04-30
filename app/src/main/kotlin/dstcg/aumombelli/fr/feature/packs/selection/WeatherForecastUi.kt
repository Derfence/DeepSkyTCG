package fr.aumombelli.dstcg.feature.packs.selection

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.data.WeatherPolicy
import fr.aumombelli.dstcg.data.WeatherState
import fr.aumombelli.dstcg.ui.motion.MotionCard
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.min

/**
 * Weather forecast UI item derived from the trusted UTC calendar.
 */
internal data class WeatherForecastDayUiModel(
    val dateUtc: LocalDate,
    val dayLabel: String,
    val weatherState: WeatherState,
    val multiplierLabel: String,
)

internal fun buildWeatherForecastDayUiModels(
    now: Instant,
    weatherPolicy: WeatherPolicy,
    dayCount: Int = 7,
): List<WeatherForecastDayUiModel> {
    if (dayCount <= 0) {
        return emptyList()
    }

    val startDate = now.atZone(ZoneOffset.UTC).toLocalDate()
    return (0 until dayCount).map { dayOffset ->
        val dateUtc = startDate.plusDays(dayOffset.toLong())
        val weatherState = weatherPolicy.weatherAt(dateUtc.atStartOfDay(ZoneOffset.UTC).toInstant())
        WeatherForecastDayUiModel(
            dateUtc = dateUtc,
            dayLabel = frenchShortDayLabel(dateUtc.dayOfWeek),
            weatherState = weatherState,
            multiplierLabel = weatherState.multiplierLabel,
        )
    }
}

internal fun formatWeatherForecastUtcTimeLabel(now: Instant): String {
    val utcTime = now.atOffset(ZoneOffset.UTC)
    val hour = utcTime.hour.toString().padStart(2, '0')
    val minute = utcTime.minute.toString().padStart(2, '0')
    return "$hour:$minute UTC"
}

@Composable
internal fun WeatherForecastCard(
    forecast: List<WeatherForecastDayUiModel>,
    utcTimeLabel: String,
    modifier: Modifier = Modifier,
) {
    MotionCard(
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Prévision météo",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pack-weather-title"),
                )
                Text(
                    text = utcTimeLabel,
                    color = Color(0xFFBFD6FF),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.testTag("pack-weather-time"),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pack-weather-row"),
            ) {
                forecast.forEachIndexed { index, day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                contentDescription = "${day.dayLabel} ${day.weatherState.label}"
                            }
                            .testTag("pack-weather-day-$index"),
                    ) {
                        Text(
                            text = day.dayLabel,
                            color = Color(0xFFD6E4F5),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.testTag("pack-weather-day-label-$index"),
                        )
                        WeatherForecastIcon(
                            weatherState = day.weatherState,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("pack-weather-day-icon-$index"),
                        )
                        Text(
                            text = day.multiplierLabel,
                            color = Color(0xFFBFD6FF),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.testTag("pack-weather-day-multiplier-$index"),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherForecastIcon(
    weatherState: WeatherState,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        when (weatherState) {
            WeatherState.Rain -> drawRainIcon()
            WeatherState.Cloudy -> drawCloudyIcon()
            WeatherState.Clear -> drawClearIcon()
            WeatherState.Pure -> drawPureIcon()
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRainIcon() {
    drawCloud(
        color = Color(0xFFE6F0FF),
        alpha = 0.95f,
    )
    val stroke = min(size.width, size.height) * 0.08f
    val startY = size.height * 0.62f
    val endY = size.height * 0.84f
    val spacing = size.width * 0.17f
    repeat(3) { index ->
        val startX = size.width * 0.34f + (index * spacing)
        drawLine(
            color = Color(0xFF99D1FF),
            start = Offset(startX, startY),
            end = Offset(startX - stroke * 0.6f, endY),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCloudyIcon() {
    drawCloud(
        color = Color(0xFFB5C8E3),
        alpha = 0.45f,
        center = Offset(size.width * 0.38f, size.height * 0.47f),
        scale = 0.75f,
    )
    drawCloud(
        color = Color(0xFFE6F0FF),
        alpha = 0.96f,
        center = Offset(size.width * 0.56f, size.height * 0.56f),
        scale = 0.92f,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawClearIcon() {
    val minSize = min(size.width, size.height)
    val center = Offset(size.width / 2f, size.height / 2f)
    val glowRadius = minSize * 0.30f
    val coreRadius = minSize * 0.19f
    val innerRayRadius = minSize * 0.29f
    val outerRayRadius = minSize * 0.44f
    drawCircle(
        color = Color(0x44FFD86E),
        radius = glowRadius,
        center = center,
    )
    repeat(8) { index ->
        val angle = Math.toRadians(index * 45.0)
        val direction = Offset(
            x = kotlin.math.cos(angle).toFloat(),
            y = kotlin.math.sin(angle).toFloat(),
        )
        drawLine(
            color = Color(0xFFFFD86E),
            start = center + (direction * innerRayRadius),
            end = center + (direction * outerRayRadius),
            strokeWidth = minSize * 0.065f,
            cap = StrokeCap.Round,
        )
    }
    drawCircle(
        color = Color(0xFFFFD86E),
        radius = coreRadius,
        center = center,
    )
}

private fun circlePath(
    center: Offset,
    radius: Float,
): Path = Path().apply {
    addOval(
        Rect(
            left = center.x - radius,
            top = center.y - radius,
            right = center.x + radius,
            bottom = center.y + radius,
        ),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPureIcon() {
    val minSize = min(size.width, size.height)
    val moonCenter = Offset(size.width * 0.50f, size.height * 0.48f)
    val moonRadius = minSize * 0.34f

    val moon = Path.combine(PathOperation.Difference,
    circlePath(
        center = moonCenter,
        radius = moonRadius,
    ),
        circlePath(
        center = Offset(size.width * 0.9f, size.height * 0.4f),
        radius = minSize * 0.55f,
    ))
    drawPath(path = moon, color = Color(0xFFF7FBFF))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCloud(
    color: Color,
    alpha: Float,
    center: Offset = Offset(size.width / 2f, size.height * 0.52f),
    scale: Float = 1f,
) {
    val minSize = min(size.width, size.height)
    val leftRadius = minSize * 0.16f * scale
    val middleRadius = minSize * 0.20f * scale
    val rightRadius = minSize * 0.15f * scale
    val cloudColor = color.copy(alpha = alpha)
    val baseTop = center.y - (minSize * 0.02f * scale)

    drawCircle(
        color = cloudColor,
        radius = leftRadius,
        center = Offset(center.x - minSize * 0.17f * scale, baseTop),
    )
    drawCircle(
        color = cloudColor,
        radius = middleRadius,
        center = Offset(center.x, baseTop - minSize * 0.08f * scale),
    )
    drawCircle(
        color = cloudColor,
        radius = rightRadius,
        center = Offset(center.x + minSize * 0.18f * scale, baseTop),
    )
    drawRoundRect(
        color = cloudColor,
        topLeft = Offset(center.x - minSize * 0.30f * scale, center.y - minSize * 0.04f * scale),
        size = androidx.compose.ui.geometry.Size(
            width = minSize * 0.60f * scale,
            height = minSize * 0.18f * scale,
        ),
        cornerRadius = CornerRadius(minSize * 0.12f * scale),
    )
}

private fun frenchShortDayLabel(dayOfWeek: DayOfWeek): String = when (dayOfWeek) {
    DayOfWeek.MONDAY -> "Lun"
    DayOfWeek.TUESDAY -> "Mar"
    DayOfWeek.WEDNESDAY -> "Mer"
    DayOfWeek.THURSDAY -> "Jeu"
    DayOfWeek.FRIDAY -> "Ven"
    DayOfWeek.SATURDAY -> "Sam"
    DayOfWeek.SUNDAY -> "Dim"
}

private operator fun Offset.times(scale: Float): Offset = Offset(x * scale, y * scale)

private operator fun Offset.plus(other: Offset): Offset = Offset(x + other.x, y + other.y)
