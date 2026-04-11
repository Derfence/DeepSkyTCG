package fr.aumombelli.dstcg

import androidx.compose.ui.geometry.Rect
import fr.aumombelli.dstcg.feature.equipment.resolveEquipmentActivationCoachmarkVisibility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EquipmentActivationCoachmarkVisibilityTest {
    @Test
    fun hidden_target_section_keeps_scroll_hint_visible() {
        val visibility = resolveEquipmentActivationCoachmarkVisibility(
            targetEnabled = true,
            viewportHeightPx = 640f,
            buttonBounds = null,
            targetSectionOffscreenBelow = true,
        )

        assertNull(visibility.visibleBounds)
        assertEquals(true, visibility.showScrollDownHint)
    }

    @Test
    fun partially_visible_section_does_not_hide_hint_while_button_is_still_below_viewport() {
        val visibility = resolveEquipmentActivationCoachmarkVisibility(
            targetEnabled = true,
            viewportHeightPx = 640f,
            buttonBounds = Rect(left = 32f, top = 700f, right = 328f, bottom = 756f),
            targetSectionOffscreenBelow = false,
        )

        assertNull(visibility.visibleBounds)
        assertEquals(true, visibility.showScrollDownHint)
    }

    @Test
    fun visible_button_switches_back_to_text_bubble() {
        val buttonBounds = Rect(left = 32f, top = 560f, right = 328f, bottom = 616f)

        val visibility = resolveEquipmentActivationCoachmarkVisibility(
            targetEnabled = true,
            viewportHeightPx = 640f,
            buttonBounds = buttonBounds,
            targetSectionOffscreenBelow = false,
        )

        assertEquals(buttonBounds, visibility.visibleBounds)
        assertEquals(false, visibility.showScrollDownHint)
    }
}
