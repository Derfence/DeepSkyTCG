package fr.aumombelli.dstcg

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import fr.aumombelli.dstcg.audio.AudioSettingsRepository
import fr.aumombelli.dstcg.audio.NoOpAudioController
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class AudioSettingsRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `audio is enabled by default`() = runTest {
        val repository = newRepository()

        assertEquals(true, repository.settings.first().enabled)
    }

    @Test
    fun `audio enabled preference is written to settings flow`() = runTest {
        val repository = newRepository()

        repository.setEnabled(false)

        assertEquals(false, repository.settings.first().enabled)
    }

    @Test
    fun `no op controller updates exposed settings without playing audio`() = runTest {
        val controller = NoOpAudioController()

        controller.setEnabled(false)

        assertEquals(false, controller.settings.value.enabled)
    }

    private fun TestScope.newRepository(
        file: File = File(temporaryFolder.root, "audio-${System.nanoTime()}.preferences_pb"),
    ): AudioSettingsRepository {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { file },
        )
        return AudioSettingsRepository(dataStore)
    }
}
