package fr.aumombelli.dstcg.feature.equipment

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates

private fun Rect.isVerticallyVisibleWithin(viewportHeightPx: Float): Boolean =
    bottom > 0f && top < viewportHeightPx

internal data class EquipmentActivationCoachmarkVisibility(
    val visibleBounds: Rect?,
    val showScrollDownHint: Boolean,
)

internal fun resolveEquipmentActivationCoachmarkVisibility(
    targetEnabled: Boolean,
    buttonBoundsInRoot: Rect?,
    buttonBoundsInViewport: Rect?,
    viewportHeightPx: Float,
    targetSectionOffscreenBelow: Boolean,
): EquipmentActivationCoachmarkVisibility {
    if (!targetEnabled) {
        return EquipmentActivationCoachmarkVisibility(
            visibleBounds = null,
            showScrollDownHint = false,
        )
    }

    if (buttonBoundsInViewport != null && viewportHeightPx > 0f) {
        val buttonVisible = buttonBoundsInViewport.isVerticallyVisibleWithin(viewportHeightPx)
        return EquipmentActivationCoachmarkVisibility(
            visibleBounds = if (buttonVisible) buttonBoundsInRoot else null,
            showScrollDownHint = !buttonVisible && buttonBoundsInViewport.top >= viewportHeightPx,
        )
    }

    if (buttonBoundsInRoot != null && !targetSectionOffscreenBelow) {
        return EquipmentActivationCoachmarkVisibility(
            visibleBounds = buttonBoundsInRoot,
            showScrollDownHint = false,
        )
    }

    return EquipmentActivationCoachmarkVisibility(
        visibleBounds = null,
        showScrollDownHint = targetSectionOffscreenBelow,
    )
}

internal class LayoutCoordinatesHolder {
    var value: LayoutCoordinates? = null
}
