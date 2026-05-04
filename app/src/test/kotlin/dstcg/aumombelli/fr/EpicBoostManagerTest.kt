package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.EntropySource
import fr.aumombelli.dstcg.data.EpicBoostManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpicBoostManagerTest {
    @Test
    fun `rollEpicBoostBoosterIndex returns null when appearance roll fails`() {
        val manager = EpicBoostManager(QueuedEntropySource(100_000))

        assertNull(manager.rollEpicBoostBoosterIndex())
    }

    @Test
    fun `rollEpicBoostBoosterIndex returns booster index when appearance roll succeeds`() {
        val manager = EpicBoostManager(QueuedEntropySource(99_999, 2))

        assertEquals(2, manager.rollEpicBoostBoosterIndex())
    }

    private class QueuedEntropySource(
        private vararg val values: Int,
    ) : EntropySource {
        private var index = 0

        override fun nextInt(bound: Int): Int {
            val value = values.getOrNull(index) ?: 0
            index += 1
            return value.mod(bound.coerceAtLeast(1))
        }
    }
}
