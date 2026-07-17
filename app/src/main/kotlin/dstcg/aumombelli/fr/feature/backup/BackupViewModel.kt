package fr.aumombelli.dstcg.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.aumombelli.dstcg.data.BackupDocument
import fr.aumombelli.dstcg.data.BackupGateway
import fr.aumombelli.dstcg.data.BackupInput
import fr.aumombelli.dstcg.data.BackupPreview
import fr.aumombelli.dstcg.data.normalizeBackupPassword
import java.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class BackupDialog {
    None,
    ExportPassword,
    ImportPassword,
    Preview,
}

enum class BackupOperation {
    Idle,
    Encrypting,
    AwaitingExportDestination,
    WritingExport,
    AwaitingImportDocument,
    ReadingImport,
    InspectingImport,
    Importing,
}

data class BackupUiState(
    val lastSuccessfulImportAt: Instant? = null,
    val dialog: BackupDialog = BackupDialog.None,
    val operation: BackupOperation = BackupOperation.Idle,
    val preview: BackupPreview? = null,
    val exportDocument: BackupDocument? = null,
    val openDocumentRequestId: Int = 0,
    val importCompletedId: Int = 0,
    val message: String? = null,
    val errorMessage: String? = null,
) {
    val isBusy: Boolean
        get() = operation != BackupOperation.Idle
}

class BackupViewModel(
    private val backupGateway: BackupGateway,
    private val passwordDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()
    private var selectedImportBytes: ByteArray? = null

    init {
        viewModelScope.launch {
            backupGateway.lastSuccessfulImportAt
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = exception.message ?: "Impossible de lire l'état des sauvegardes.",
                    )
                }
                .collect { instant ->
                    _uiState.value = _uiState.value.copy(lastSuccessfulImportAt = instant)
                }
        }
    }

    fun requestExport() {
        if (_uiState.value.isBusy) return
        _uiState.value = _uiState.value.copy(
            dialog = BackupDialog.ExportPassword,
            message = null,
            errorMessage = null,
        )
    }

    fun requestImportDocument() {
        if (_uiState.value.isBusy) return
        discardSelectedImportBytes()
        discardCurrentPreview()
        _uiState.value = _uiState.value.copy(
            dialog = BackupDialog.None,
            preview = null,
            operation = BackupOperation.AwaitingImportDocument,
            openDocumentRequestId = _uiState.value.openDocumentRequestId + 1,
            message = null,
            errorMessage = null,
        )
    }

    fun acceptImportDocument(bytes: ByteArray) {
        if (_uiState.value.operation !in setOf(BackupOperation.AwaitingImportDocument, BackupOperation.ReadingImport)) {
            bytes.fill(0)
            return
        }
        discardSelectedImportBytes()
        selectedImportBytes = bytes
        _uiState.value = _uiState.value.copy(
            operation = BackupOperation.Idle,
            dialog = BackupDialog.ImportPassword,
        )
    }

    fun beginImportDocumentRead() {
        if (_uiState.value.operation == BackupOperation.AwaitingImportDocument) {
            _uiState.value = _uiState.value.copy(operation = BackupOperation.ReadingImport)
        }
    }

    fun cancelImportDocumentSelection() {
        if (_uiState.value.operation in setOf(BackupOperation.AwaitingImportDocument, BackupOperation.ReadingImport)) {
            _uiState.value = _uiState.value.copy(operation = BackupOperation.Idle)
        }
    }

    fun reportDocumentReadFailure(message: String) {
        _uiState.value = _uiState.value.copy(operation = BackupOperation.Idle, errorMessage = message)
    }

    fun submitExportPassword(password: String, confirmation: String) {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                operation = BackupOperation.Encrypting,
                errorMessage = null,
            )
            val normalizedPasswords = runCatching {
                withContext(passwordDispatcher) {
                    normalizeBackupPassword(password) to normalizeBackupPassword(confirmation)
                }
            }.getOrElse {
                showFailure(it)
                return@launch
            }
            if (normalizedPasswords.first != normalizedPasswords.second) {
                _uiState.value = _uiState.value.copy(
                    operation = BackupOperation.Idle,
                    errorMessage = "Les mots de passe ne correspondent pas.",
                )
                return@launch
            }
            runCatching { backupGateway.exportBackup(normalizedPasswords.first) }
                .onSuccess { document ->
                    _uiState.value = _uiState.value.copy(
                        operation = BackupOperation.AwaitingExportDestination,
                        dialog = BackupDialog.None,
                        exportDocument = document,
                    )
                }
                .onFailure(::showFailure)
        }
    }

    fun submitImportPassword(password: String) {
        if (_uiState.value.isBusy) return
        val bytes = selectedImportBytes ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                operation = BackupOperation.InspectingImport,
                errorMessage = null,
            )
            runCatching { backupGateway.inspectBackup(BackupInput(bytes), password) }
                .onSuccess { preview ->
                    discardSelectedImportBytes()
                    _uiState.value = _uiState.value.copy(
                        operation = BackupOperation.Idle,
                        dialog = BackupDialog.Preview,
                        preview = preview,
                    )
                }
                .onFailure(::showFailure)
        }
    }

    fun confirmImport() {
        if (_uiState.value.isBusy) return
        val preview = _uiState.value.preview ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                operation = BackupOperation.Importing,
                errorMessage = null,
            )
            runCatching { backupGateway.importBackup(preview.token) }
                .onSuccess { result ->
                    discardSelectedImportBytes()
                    _uiState.value = _uiState.value.copy(
                        operation = BackupOperation.Idle,
                        dialog = BackupDialog.None,
                        preview = null,
                        lastSuccessfulImportAt = result.importedAt,
                        importCompletedId = _uiState.value.importCompletedId + 1,
                        message = "Sauvegarde importée avec succès.",
                    )
                }
                .onFailure(::showFailure)
        }
    }

    fun dismissDialog() {
        if (_uiState.value.isBusy) return
        discardSelectedImportBytes()
        discardCurrentPreview()
        _uiState.value = _uiState.value.copy(
            dialog = BackupDialog.None,
            preview = null,
            errorMessage = null,
        )
    }

    fun consumeExportDocument(saved: Boolean) {
        _uiState.value = _uiState.value.copy(
            operation = BackupOperation.Idle,
            exportDocument = null,
            message = if (saved) "Sauvegarde exportée avec succès." else null,
            errorMessage = if (saved) null else "L'export de la sauvegarde a été annulé.",
        )
    }

    fun beginExportDocumentWrite() {
        if (_uiState.value.operation == BackupOperation.AwaitingExportDestination) {
            _uiState.value = _uiState.value.copy(operation = BackupOperation.WritingExport)
        }
    }

    fun reportDocumentWriteFailure(message: String) {
        _uiState.value = _uiState.value.copy(
            operation = BackupOperation.Idle,
            exportDocument = null,
            errorMessage = message,
        )
    }

    private fun showFailure(exception: Throwable) {
        _uiState.value = _uiState.value.copy(
            operation = BackupOperation.Idle,
            errorMessage = exception.message ?: "L'opération de sauvegarde a échoué.",
        )
    }

    private fun discardCurrentPreview() {
        val token = _uiState.value.preview?.token ?: return
        viewModelScope.launch {
            runCatching { backupGateway.discardBackupPreview(token) }
        }
    }

    private fun discardSelectedImportBytes() {
        selectedImportBytes?.fill(0)
        selectedImportBytes = null
    }
}
