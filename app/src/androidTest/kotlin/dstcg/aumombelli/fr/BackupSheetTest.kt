package fr.aumombelli.dstcg

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import fr.aumombelli.dstcg.feature.backup.BackupDialog
import fr.aumombelli.dstcg.feature.backup.BackupSheet
import fr.aumombelli.dstcg.feature.backup.BackupUiState
import org.junit.Rule
import org.junit.Test

class BackupSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sheet_displays_actions_and_last_import_status() {
        composeRule.setContent {
            MaterialTheme {
                BackupSheet(
                    visible = true,
                    state = BackupUiState(),
                    onDismiss = {},
                    onRequestExport = {},
                    onRequestImport = {},
                    onSubmitExportPassword = { _, _ -> },
                    onSubmitImportPassword = {},
                    onConfirmImport = {},
                    onDismissDialog = {},
                )
            }
        }

        composeRule.onNodeWithTag("backup-last-import").assertExists()
        composeRule.onNodeWithTag("backup-export").assertExists()
        composeRule.onNodeWithTag("backup-import").assertExists()
    }

    @Test
    fun export_password_dialog_exposes_two_secret_fields() {
        composeRule.setContent {
            MaterialTheme {
                BackupSheet(
                    visible = true,
                    state = BackupUiState(dialog = BackupDialog.ExportPassword),
                    onDismiss = {},
                    onRequestExport = {},
                    onRequestImport = {},
                    onSubmitExportPassword = { _, _ -> },
                    onSubmitImportPassword = {},
                    onConfirmImport = {},
                    onDismissDialog = {},
                )
            }
        }

        composeRule.onNodeWithTag("backup-export-password").assertExists()
        composeRule.onNodeWithTag("backup-export-confirmation").assertExists()
        composeRule.onNodeWithTag("backup-export-password-visibility").assertExists()
        composeRule.onNodeWithText("12 caractères minimum", substring = true).assertDoesNotExist()
        composeRule.onNodeWithTag("backup-export-confirm").assertExists()
    }
}
