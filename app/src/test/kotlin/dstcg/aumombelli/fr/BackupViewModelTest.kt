package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.BackupDocument
import fr.aumombelli.dstcg.data.BackupGateway
import fr.aumombelli.dstcg.data.BackupImportResult
import fr.aumombelli.dstcg.data.BackupInput
import fr.aumombelli.dstcg.data.BackupPreview
import fr.aumombelli.dstcg.feature.backup.BackupDialog
import fr.aumombelli.dstcg.feature.backup.BackupOperation
import fr.aumombelli.dstcg.feature.backup.BackupViewModel
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `export accepts one normalized character and stays busy through SAF write`() =
        runTest(mainDispatcherRule.dispatcher) {
        val gateway = FakeBackupGateway()
        val viewModel = BackupViewModel(gateway, mainDispatcherRule.dispatcher)
        viewModel.requestExport()

        viewModel.submitExportPassword("e\u0301", "é")
        advanceUntilIdle()

        assertEquals("é", gateway.exportedPassword)
        assertEquals(BackupOperation.AwaitingExportDestination, viewModel.uiState.value.operation)
        assertTrue(viewModel.uiState.value.isBusy)

        viewModel.beginExportDocumentWrite()
        assertEquals(BackupOperation.WritingExport, viewModel.uiState.value.operation)
        viewModel.consumeExportDocument(saved = true)
        assertFalse(viewModel.uiState.value.isBusy)
    }

    @Test
    fun `export rejects mismatched confirmation without calling gateway`() =
        runTest(mainDispatcherRule.dispatcher) {
        val gateway = FakeBackupGateway()
        val viewModel = BackupViewModel(gateway, mainDispatcherRule.dispatcher)

        viewModel.submitExportPassword("a", "b")
        advanceUntilIdle()

        assertNull(gateway.exportedPassword)
        assertEquals("Les mots de passe ne correspondent pas.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `cancelled import selection releases busy state`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = BackupViewModel(FakeBackupGateway(), mainDispatcherRule.dispatcher)

        viewModel.requestImportDocument()
        assertEquals(BackupOperation.AwaitingImportDocument, viewModel.uiState.value.operation)

        viewModel.cancelImportDocumentSelection()
        assertEquals(BackupOperation.Idle, viewModel.uiState.value.operation)
    }

    @Test
    fun `dismissing preview discards decrypted payload`() = runTest(mainDispatcherRule.dispatcher) {
        val gateway = FakeBackupGateway()
        val viewModel = BackupViewModel(gateway, mainDispatcherRule.dispatcher)
        viewModel.requestImportDocument()
        viewModel.beginImportDocumentRead()
        viewModel.acceptImportDocument(byteArrayOf(1))
        viewModel.submitImportPassword("x")
        advanceUntilIdle()
        assertEquals(BackupDialog.Preview, viewModel.uiState.value.dialog)

        viewModel.dismissDialog()
        advanceUntilIdle()

        assertEquals(listOf("preview-token"), gateway.discardedTokens)
    }

    private class FakeBackupGateway : BackupGateway {
        override val lastSuccessfulImportAt = MutableStateFlow<Instant?>(null)
        var exportedPassword: String? = null
        val discardedTokens = mutableListOf<String?>()

        override suspend fun exportBackup(password: String): BackupDocument {
            exportedPassword = password
            return BackupDocument("backup.dstcgsave", byteArrayOf(1), NOW)
        }

        override suspend fun inspectBackup(input: BackupInput, password: String): BackupPreview = BackupPreview(
            token = "preview-token",
            createdAt = NOW,
            appVersionCode = 1,
            progressSchemaVersion = 1,
            distinctCardCount = 0,
            totalOwnedCardCount = 0,
            openedPackCount = 0,
        )

        override suspend fun importBackup(previewToken: String): BackupImportResult =
            BackupImportResult(NOW, NOW)

        override suspend fun discardBackupPreview(previewToken: String?) {
            discardedTokens += previewToken
        }
    }

    companion object {
        private val NOW = Instant.parse("2026-07-15T10:00:00Z")
    }
}
