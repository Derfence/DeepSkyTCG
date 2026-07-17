package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.notification.LocalNotificationType
import fr.aumombelli.dstcg.notification.content
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPublisherTest {
    @Test
    fun `full stock notification announces ten available packs`() {
        val content = LocalNotificationType.FullStock.content()

        assertEquals("Tes packs sont rechargés", content.title)
        assertEquals("Ton stock est plein : 10 packs t’attendent.", content.message)
    }

    @Test
    fun `return reminder uses a neutral invitation`() {
        val content = LocalNotificationType.ReturnReminder.content()

        assertEquals("L’observatoire t’attend", content.title)
        assertEquals(
            "Cela fait 7 jours que tu n’as pas joué. De nouveaux packs t’attendent.",
            content.message,
        )
    }
}
