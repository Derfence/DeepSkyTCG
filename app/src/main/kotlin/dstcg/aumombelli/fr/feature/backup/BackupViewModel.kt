package fr.aumombelli.dstcg.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.aumombelli.dstcg.data.BackupDocument
import fr.aumombelli.dstcg.data.BackupGateway
import fr.aumombelli.dstcg.data.BackupInput
import fr.aumombelli.dstcg.data.BackupPreview
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

enum class BackupDialog {
    None,
    ExportPassword,
    ImportPassword,
    Preview,
}

data class BackupUiState(
    val lastSuccessfulImportAt: Instant? = null,
    val dialog: BackupDialog = BackupDialog.None,
    val isBusy: Boolean = false,
    val preview: BackupPreview? = null,
    val exportDocument: BackupDocument? = null,
    val openDocumentRequestId: Int = 0,
    val importCompletedId: Int = 0,
    val message: String? = null,
    val errorMessage: String? = null,
)

class BackupViewModel(
    private val backupGateway: BackupGateway,
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
        selectedImportBytes = null
        _uiState.value = _uiState.value.copy(
            dialog = BackupDialog.None,
            preview = null,
            openDocumentRequestId = _uiState.value.openDocumentRequestId + 1,
            message = null,
            errorMessage = null,
        )
    }

    fun acceptImportDocument(bytes: ByteArray) {
        if (_uiState.value.isBusy) return
        selectedImportBytes = bytes
        _uiState.value = _uiState.value.copy(dialog = BackupDialog.ImportPassword)
    }

    fun reportDocumentReadFailure(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }

    fun submitExportPassword(password: String, confirmation: String) {
        if (_uiState.value.isBusy) return
        if (password != confirmation) {
            _uiState.value = _uiState.value.copy(errorMessage = "Les mots de passe ne correspondent pas.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, errorMessage = null)
            runCatching { backupGateway.exportBackup(password) }
                .onSuccess { document ->
                    _uiState.value = _uiState.value.copy(
                        isBusy = false,
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
            _uiState.value = _uiState.value.copy(isBusy = true, errorMessage = null)
            runCatching { backupGateway.inspectBackup(BackupInput(bytes), password) }
                .onSuccess { preview ->
                    _uiState.value = _uiState.value.copy(
                        isBusy = false,
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
            _uiState.value = _uiState.value.copy(isBusy = true, errorMessage = null)
            runCatching { backupGateway.importBackup(preview.token) }
                .onSuccess { result ->
                    selectedImportBytes = null
                    _uiState.value = _uiState.value.copy(
                        isBusy = false,
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
        selectedImportBytes = null
        _uiState.value = _uiState.value.copy(
            dialog = BackupDialog.None,
            preview = null,
            errorMessage = null,
        )
    }

    fun consumeExportDocument(saved: Boolean) {
        _uiState.value = _uiState.value.copy(
            exportDocument = null,
            message = if (saved) "Sauvegarde exportée avec succès." else null,
            errorMessage = if (saved) null else "L'export de la sauvegarde a été annulé.",
        )
    }

    fun reportDocumentWriteFailure(message: String) {
        _uiState.value = _uiState.value.copy(exportDocument = null, errorMessage = message)
    }

    private fun showFailure(exception: Throwable) {
        _uiState.value = _uiState.value.copy(
            isBusy = false,
            errorMessage = exception.message ?: "L'opération de sauvegarde a échoué.",
        )
    }
}
