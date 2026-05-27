package fr.aumombelli.dstcg.feature.packs.selection

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.data.buildPackChargeUiStatus
import fr.aumombelli.dstcg.data.nextUtcDayStartCompat
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import fr.aumombelli.dstcg.ui.motion.GenericPackCardShell
import fr.aumombelli.dstcg.ui.motion.MotionCard
import java.time.Duration
import java.time.Instant

internal fun PackSelectionUiState.buildLiveChargeStatus(now: Instant) = buildPackChargeUiStatus(
    rechargeState = rechargeState,
    now = now,
    drawCooldown = drawCooldown,
    maxStoredDraws = maxStoredDraws,
    weatherPolicy = weatherPolicy,
    rechargeMultiplier = rechargeMultiplier,
)

internal fun computeTrustedNow(
    trustedNow: Instant,
    trustedElapsedRealtimeMs: Long,
): Instant {
    val elapsedSinceReference = (SystemClock.elapsedRealtime() - trustedElapsedRealtimeMs)
        .coerceAtLeast(0L)
    return trustedNow.plusMillis(elapsedSinceReference)
}

@Composable
internal fun PackChargeStatus(
    availableDrawCount: Int,
    maxStoredDraws: Int,
    rechargeProgress: Float,
    remainingDurationText: String?,
    modifier: Modifier = Modifier,
) {
    val safeMaxStoredDraws = maxStoredDraws.coerceAtLeast(0)
    val visibleAvailableDrawCount = availableDrawCount.coerceIn(0, safeMaxStoredDraws)
    val readyPackLabel = if (visibleAvailableDrawCount == 1) "pack prêt" else "packs prêts"
    val capacityLabel = if (safeMaxStoredDraws == 1) {
        "sur 1 ouverture stockable"
    } else {
        "sur $safeMaxStoredDraws ouvertures stockables"
    }
    val statusText = if (safeMaxStoredDraws > 0 && visibleAvailableDrawCount >= safeMaxStoredDraws) {
        "Stock plein ${safeMaxStoredDraws}/${safeMaxStoredDraws}"
    } else {
        "Prochaine charge dans ${remainingDurationText ?: "..."}"
    }

    MotionCard(modifier = modifier) {
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
                    text = visibleAvailableDrawCount.toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.testTag("pack-status-ready-count"),
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                ) {
                    Text(
                        text = readyPackLabel,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = capacityLabel,
                        color = Color(0xFFBFD6FF),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Text(
                    text = "$visibleAvailableDrawCount/$safeMaxStoredDraws",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("pack-status-count"),
                )
            }
            PackStockSlots(
                availableDrawCount = visibleAvailableDrawCount,
                maxStoredDraws = safeMaxStoredDraws,
                modifier = Modifier.testTag("pack-status-stock-slots"),
            )
            LinearProgressIndicator(
                progress = { rechargeProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .testTag("pack-status-progress"),
            )
            Text(
                text = statusText,
                color = Color(0xFFD6E4F5),
                modifier = Modifier.testTag("pack-status-remaining"),
            )
        }
    }
}

@Composable
internal fun PackStockSlots(
    availableDrawCount: Int,
    maxStoredDraws: Int,
    modifier: Modifier = Modifier,
) {
    if (maxStoredDraws <= 0) return

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val gap = 4.dp
        val totalGap = gap * (maxStoredDraws - 1).coerceAtLeast(0)
        val rawPackWidth = (maxWidth - totalGap).coerceAtLeast(0.dp) / maxStoredDraws.toFloat()
        val packWidth = minOf(rawPackWidth, 24.dp).coerceAtLeast(8.dp)
        val packHeight = packWidth / TRADING_CARD_WIDTH_OVER_HEIGHT

        Row(
            horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            repeat(maxStoredDraws) { index ->
                val isFilled = index < availableDrawCount
                PackStockSlot(
                    index = index,
                    isFilled = isFilled,
                    width = packWidth,
                    height = packHeight,
                )
            }
        }
    }
}

@Composable
private fun PackStockSlot(
    index: Int,
    isFilled: Boolean,
    width: Dp,
    height: Dp,
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .semantics {
                contentDescription = if (isFilled) {
                    "Pack disponible ${index + 1}"
                } else {
                    "Emplacement de pack vide ${index + 1}"
                }
            }
            .testTag("pack-status-pack-slot-$index"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = if (isFilled) 1f else 0.34f
                    scaleX = if (isFilled) 1f else 0.92f
                    scaleY = if (isFilled) 1f else 0.92f
                }
                .testTag(
                    if (isFilled) {
                        "pack-status-pack-slot-filled"
                    } else {
                        "pack-status-pack-slot-empty"
                    },
                ),
        ) {
            GenericPackCardShell(
                decorSeed = "pack-stock-$index",
                revealProgress = if (isFilled) 1f else 0f,
                contentPadding = 1.dp,
            )
        }
    }
}

internal fun millisUntilNextUtcDayStart(now: Instant): Long {
    val nextUtcDayStart = now.nextUtcDayStartCompat()
    return Duration.between(now, nextUtcDayStart).toMillis().coerceAtLeast(0L)
}
