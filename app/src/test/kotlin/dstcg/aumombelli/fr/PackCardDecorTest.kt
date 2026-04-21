package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.ui.motion.DefaultPackCardDecorSpec
import fr.aumombelli.dstcg.ui.motion.buildPackSawtoothEdgePoints
import fr.aumombelli.dstcg.ui.motion.packCardDecorSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PackCardDecorTest {
    @Test
    fun rarity_star_distribution_matches_requested_pack_mix() {
        val counts = DefaultPackCardDecorSpec.rarityStars
            .groupingBy { it.rarityLabel }
            .eachCount()

        assertEquals(3, counts["Rare"])
        assertEquals(1, counts["Epic"])
        assertEquals(4, counts["Uncommon"])
        assertTrue((counts["Common"] ?: 0) > DefaultPackCardDecorSpec.rarityStars.size / 2)
    }

    @Test
    fun rarity_stars_stay_inside_visible_pack_area() {
        listOf(0, 1, 42, 84, 2026).forEach { seed ->
            packCardDecorSpec(seed = seed).rarityStars.forEach { star ->
                assertTrue(star.radiusFraction in 0.014f..0.034f)
                assertTrue(star.xFraction - star.radiusFraction >= 0.10f)
                assertTrue(star.xFraction + star.radiusFraction <= 0.90f)
                assertTrue(
                    (star.yFraction - star.radiusFraction >= 0.10f &&
                        star.yFraction + star.radiusFraction <= 0.40f) ||
                        (star.yFraction - star.radiusFraction >= 0.60f &&
                            star.yFraction + star.radiusFraction <= 0.90f),
                )
            }
        }
    }

    @Test
    fun random_star_positions_are_stable_for_one_seed_and_different_between_seeds() {
        val first = packCardDecorSpec(seed = 42).rarityStars
        val second = packCardDecorSpec(seed = 42).rarityStars
        val other = packCardDecorSpec(seed = 84).rarityStars

        assertEquals(first, second)
        assertNotEquals(first, other)
    }

    @Test
    fun sawtooth_edge_alternates_between_baseline_and_tip_for_each_tooth() {
        val points = buildPackSawtoothEdgePoints(
            width = 220f,
            baselineY = 26f,
            tipY = 38f,
            toothCount = 10,
        )

        assertEquals(21, points.size)
        assertEquals(0f, points.first().x, 0.001f)
        assertEquals(220f, points.last().x, 0.001f)

        points.forEachIndexed { index, point ->
            val expectedY = if (index == 0 || index % 2 == 0) 26f else 38f
            assertEquals(expectedY, point.y, 0.001f)
        }
    }
}
