package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.model.NewPlayerOnboardingContent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewPlayerOnboardingContentTest {
    @Test
    fun `welcome intro introduces Aster as the guide`() {
        val message = NewPlayerOnboardingContent.welcomeIntro.message

        assertTrue(message.contains("Mon nom est Aster"))
        assertTrue(message.contains("je te guiderai pour terminer ta collection"))
        assertFalse(message.contains("Bonne chance pour réunir"))
    }

    @Test
    fun `conclusion closes the guided onboarding`() {
        val conclusion = NewPlayerOnboardingContent.conclusion

        assertTrue(conclusion.title.contains("prêt"))
        assertTrue(conclusion.message.contains("améliorer une carte"))
        assertTrue(conclusion.message.contains("Bonne collection"))
    }
}
