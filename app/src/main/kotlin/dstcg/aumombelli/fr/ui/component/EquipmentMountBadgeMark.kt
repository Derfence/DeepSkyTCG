package fr.aumombelli.dstcg.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.model.EquipmentType

@Composable
fun EquipmentMountBadgeMark(
    emblemSize: Dp = 56.dp,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val tokens = equipmentCategoryColorTokens(EquipmentType.Mount)
    val taggedModifier = testTag?.let { modifier.testTag(it) } ?: modifier

    Canvas(
        modifier = taggedModifier.size(emblemSize),
    ) {
        drawEquipmentMountGlyph(
            strokeColor = tokens.iconStroke,
            strokeWidth = minOf(size.width, size.height) * 0.085f,
        )
    }
}
