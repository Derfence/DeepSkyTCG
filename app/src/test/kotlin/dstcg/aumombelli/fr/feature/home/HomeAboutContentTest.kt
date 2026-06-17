package fr.aumombelli.dstcg.feature.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HomeAboutContentTest {
    @Test
    fun `about sections stay focused on app credits`() {
        val sections = homeAboutSections()

        assertEquals(listOf("Crédits"), sections.map { it.title })
        assertFalse(sections.flatMap { it.lines }.any { it.contains("crédit audio", ignoreCase = true) })
    }
}
