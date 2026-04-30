package fr.aumombelli.dstcg.feature.equipment

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.model.EquipmentType
import fr.aumombelli.dstcg.ui.component.equipmentCategoryColorTokens
import fr.aumombelli.dstcg.ui.component.drawEquipmentMountGlyph
import fr.aumombelli.dstcg.ui.component.drawEquipmentObservatoryGlyph
import fr.aumombelli.dstcg.ui.component.drawEquipmentTelescopeGlyph

internal fun EquipmentType.usageCountLabel(): String = when (this) {
    EquipmentType.Observatory,
    EquipmentType.Telescope,
    -> "Utilisés"

    EquipmentType.Mount -> "Utilisées"
}

@Composable
internal fun EquipmentMetricTile(
    label: String,
    value: String,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.08f),
        modifier = Modifier.widthIn(min = 88.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                text = label,
                color = Color(0xFF9AB4D3),
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = value,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
internal fun EquipmentPill(
    text: String,
    palette: EquipmentCategoryPalette,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = palette.chipColor,
        modifier = modifier,
    ) {
        Text(
            text = text,
            color = palette.accentText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
internal fun EquipmentLevelPill(
    text: String,
    palette: EquipmentCategoryPalette,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color.Black.copy(alpha = 0.20f),
    ) {
        Text(
            text = text,
            color = palette.accentText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
        )
    }
}

@Composable
internal fun EquipmentCategoryBadge(
    type: EquipmentType,
    icon: EquipmentCategoryIconUi,
    badgeSize: Dp,
    modifier: Modifier = Modifier,
) {
    val palette = equipmentPalette(type)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(badgeSize)
            .clip(CircleShape)
            .background(palette.iconBrush),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(badgeSize * 0.2f),
        ) {
            val strokeWidth = minOf(size.width, size.height) * 0.075f
            val strokeColor = palette.iconStroke
            when (icon) {
                EquipmentCategoryIconUi.Observatory -> drawEquipmentObservatoryGlyph(
                    strokeColor = strokeColor,
                    strokeWidth = strokeWidth,
                )

                EquipmentCategoryIconUi.Telescope -> drawEquipmentTelescopeGlyph(
                    strokeColor = strokeColor,
                    strokeWidth = strokeWidth,
                )

                EquipmentCategoryIconUi.Mount -> drawEquipmentMountGlyph(
                    strokeColor = strokeColor,
                    strokeWidth = strokeWidth,
                )
            }
        }
    }
}

internal data class EquipmentCategoryPalette(
    val panelBrush: Brush,
    val cardBrush: Brush,
    val iconBrush: Brush,
    val accent: Color,
    val accentText: Color,
    val iconStroke: Color,
    val chipColor: Color,
)

internal fun equipmentPalette(type: EquipmentType): EquipmentCategoryPalette {
    val tokens = equipmentCategoryColorTokens(type)
    return EquipmentCategoryPalette(
        panelBrush = Brush.linearGradient(
            colors = listOf(
                tokens.panelStart,
                tokens.panelEnd,
            ),
        ),
        cardBrush = Brush.verticalGradient(
            colors = listOf(
                tokens.cardStart,
                tokens.cardEnd,
            ),
        ),
        iconBrush = Brush.radialGradient(
            colors = listOf(
                tokens.iconStart,
                tokens.iconEnd,
            ),
        ),
        accent = tokens.accent,
        accentText = tokens.accentText,
        iconStroke = tokens.iconStroke,
        chipColor = tokens.chipColor,
    )
}
