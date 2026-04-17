package fr.aumombelli.dstcg

import android.graphics.Bitmap
import fr.aumombelli.dstcg.performance.AppPerformanceProfile
import fr.aumombelli.dstcg.testsupport.fixtures.testEquipmentCardDefinition
import fr.aumombelli.dstcg.ui.component.EquipmentArtMode
import fr.aumombelli.dstcg.ui.component.equipmentArtAssetPath
import fr.aumombelli.dstcg.ui.component.equipmentArtBitmapRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class EquipmentArtTest {
    @Test
    fun equipment_art_asset_path_uses_equipements_folder() {
        val definition = testEquipmentCardDefinition(
            id = "mount-2",
            imageRef = "equipment_mount_2",
        )

        assertEquals(
            "card_art/equipements/equipment_mount_2.webp",
            equipmentArtAssetPath(definition),
        )
    }

    @Test
    fun inventory_mode_uses_thumbnail_bucket() {
        val request = equipmentArtBitmapRequest(
            mode = EquipmentArtMode.Inventory,
            performanceProfile = testPerformanceProfile(),
        )

        assertEquals("equipment-thumbnail", request.bucketLabel)
        assertEquals(384, request.targetWidthPx)
        assertEquals(672, request.targetHeightPx)
        assertEquals(Bitmap.Config.RGB_565, request.bitmapConfig)
    }

    @Test
    fun detail_mode_uses_detail_bucket() {
        val request = equipmentArtBitmapRequest(
            mode = EquipmentArtMode.Detail,
            performanceProfile = testPerformanceProfile(),
        )

        assertEquals("equipment-detail", request.bucketLabel)
        assertEquals(1024, request.targetWidthPx)
        assertEquals(1796, request.targetHeightPx)
        assertEquals(Bitmap.Config.ARGB_8888, request.bitmapConfig)
    }

    private fun testPerformanceProfile(): AppPerformanceProfile = AppPerformanceProfile(
        isLowRamDevice = false,
        cardArtCacheMaxBytes = 32 * 1024 * 1024,
        thumbnailTargetWidthPx = 384,
        thumbnailTargetHeightPx = 672,
        detailTargetWidthPx = 1024,
        detailTargetHeightPx = 1796,
        enableAnimatedBackdrop = true,
        backdropStarDensityMultiplier = 1f,
        enableAnimatedThumbnailTwinkles = true,
        enableAnimatedBadgeCoins = true,
    )
}
