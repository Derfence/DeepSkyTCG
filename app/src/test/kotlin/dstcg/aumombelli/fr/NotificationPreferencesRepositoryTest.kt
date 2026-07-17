package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.notification.NotificationPreferencesRepository
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPreferencesRepositoryTest {
    @Test
    fun `permission acceptance enables both notification types`() = runTest {
        val repository = NotificationPreferencesRepository(inMemoryPreferencesDataStore())

        repository.completeAutomaticPermissionRequest(granted = true)

        val settings = repository.current()
        assertTrue(settings.automaticPermissionRequested)
        assertTrue(settings.fullStockEnabled)
        assertTrue(settings.returnReminderEnabled)
    }

    @Test
    fun `full stock can only be claimed once per armed cycle`() = runTest {
        val repository = NotificationPreferencesRepository(inMemoryPreferencesDataStore())
        repository.completeAutomaticPermissionRequest(granted = true)
        repository.setFullStockArmed(true)

        assertTrue(repository.claimFullStockNotification())
        assertFalse(repository.claimFullStockNotification())

        repository.setFullStockArmed(true)
        assertTrue(repository.claimFullStockNotification())
    }

    @Test
    fun `return reminder claim rejects stale app opening`() = runTest {
        val repository = NotificationPreferencesRepository(inMemoryPreferencesDataStore())
        val firstOpening = Instant.parse("2026-07-01T10:00:00Z")
        val latestOpening = Instant.parse("2026-07-02T10:00:00Z")
        repository.completeAutomaticPermissionRequest(granted = true)
        repository.recordAppOpened(firstOpening)
        repository.recordAppOpened(latestOpening)

        assertFalse(repository.claimReturnReminder(firstOpening))
        assertTrue(repository.claimReturnReminder(latestOpening))
        assertFalse(repository.claimReturnReminder(latestOpening))
    }

    @Test
    fun `disabling a type clears its pending arm`() = runTest {
        val repository = NotificationPreferencesRepository(inMemoryPreferencesDataStore())
        repository.completeAutomaticPermissionRequest(granted = true)
        repository.setFullStockArmed(true)
        repository.setFullStockEnabled(false)
        repository.setReturnReminderEnabled(false)

        val settings = repository.current()
        assertFalse(settings.fullStockArmed)
        assertFalse(settings.returnReminderArmed)
    }
}
