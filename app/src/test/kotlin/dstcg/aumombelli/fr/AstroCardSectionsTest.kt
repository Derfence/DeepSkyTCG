package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.model.MagnitudeMeasurement
import fr.aumombelli.dstcg.model.SkyEventDetails
import fr.aumombelli.dstcg.ui.component.measurementItems
import org.junit.Assert.assertTrue
import org.junit.Test

class AstroCardSectionsTest {
    @Test
    fun `measurement items include visual magnitude when provided`() {
        val items = measurementItems(
            SkyEventDetails(
                visualMagnitude = MagnitudeMeasurement(0.9, "mag. 0,9 max."),
            ),
        )

        assertTrue("Magnitude visuelle" to "mag. 0,9 max." in items)
    }
}
