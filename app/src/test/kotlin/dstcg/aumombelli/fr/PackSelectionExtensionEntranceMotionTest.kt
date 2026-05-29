package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.feature.packs.selection.EXTENSION_CARD_ENTRANCE_DURATION_MS
import fr.aumombelli.dstcg.feature.packs.selection.EXTENSION_CARD_ENTRANCE_OFFSET_PX
import fr.aumombelli.dstcg.feature.packs.selection.EXTENSION_CARD_ENTRANCE_STAGGER_MS
import fr.aumombelli.dstcg.feature.packs.selection.extensionBadgeStartDelayMillis
import fr.aumombelli.dstcg.feature.packs.selection.extensionEntranceAlpha
import fr.aumombelli.dstcg.feature.packs.selection.extensionEntranceDelayMillis
import fr.aumombelli.dstcg.feature.packs.selection.extensionEntranceTranslationYPx
import org.junit.Assert.assertEquals
import org.junit.Test

class PackSelectionExtensionEntranceMotionTest {
    @Test
    fun `extension entrance delay is staggered by list order`() {
        assertEquals(0, extensionEntranceDelayMillis(0))
        assertEquals(EXTENSION_CARD_ENTRANCE_STAGGER_MS, extensionEntranceDelayMillis(1))
        assertEquals(EXTENSION_CARD_ENTRANCE_STAGGER_MS * 2, extensionEntranceDelayMillis(2))
    }

    @Test
    fun `extension badge starts when its card entrance finishes`() {
        assertEquals(EXTENSION_CARD_ENTRANCE_DURATION_MS, extensionBadgeStartDelayMillis(0))
        assertEquals(
            EXTENSION_CARD_ENTRANCE_DURATION_MS + EXTENSION_CARD_ENTRANCE_STAGGER_MS,
            extensionBadgeStartDelayMillis(1),
        )
    }

    @Test
    fun `extension entrance starts below and ends at rest`() {
        assertEquals(EXTENSION_CARD_ENTRANCE_OFFSET_PX, extensionEntranceTranslationYPx(0f), 0.001f)
        assertEquals(0f, extensionEntranceTranslationYPx(1f), 0.001f)
    }

    @Test
    fun `extension entrance progress is clamped`() {
        assertEquals(0f, extensionEntranceAlpha(-1f), 0.001f)
        assertEquals(1f, extensionEntranceAlpha(2f), 0.001f)
        assertEquals(EXTENSION_CARD_ENTRANCE_OFFSET_PX, extensionEntranceTranslationYPx(-1f), 0.001f)
        assertEquals(0f, extensionEntranceTranslationYPx(2f), 0.001f)
    }
}
