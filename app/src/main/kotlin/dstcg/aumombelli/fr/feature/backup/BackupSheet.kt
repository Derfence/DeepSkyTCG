package fr.aumombelli.dstcg.feature.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSheet(
    visible: Boolean,
    state: BackupUiState,
    onDismiss: () -> Unit,
    onRequestExport: () -> Unit,
    onRequestImport: () -> Unit,
    onSubmitExportPassword: (String, String) -> Unit,
    onSubmitImportPassword: (String) -> Unit,
    onConfirmImport: () -> Unit,
    onDismissDialog: () -> Unit,
) {
    if (!visible) return

    ModalBottomSheet(
        onDismissRequest = { if (!state.isBusy) onDismiss() },
        modifier = Modifier.testTag("backup-sheet"),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text("Sauvegarde")
            Text(
                state.lastSuccessfulImportAt?.let {
                    "Dernière importation réalisée le ${it.toDisplayDate()}."
                } ?: "Aucune importation réalisée sur cette installation.",
                modifier = Modifier.testTag("backup-last-import"),
            )
            Text(
                "La sauvegarde contient ta progression, mais pas les préférences de son ni le nom Bluetooth.",
            )
            state.message?.let { Text(it, modifier = Modifier.testTag("backup-message")) }
            state.errorMessage?.let { Text(it, modifier = Modifier.testTag("backup-error")) }
            if (state.isBusy) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator()
                    Text("Opération en cours…")
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = onRequestExport,
                    enabled = !state.isBusy,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("backup-export"),
                ) {
                    Text("Exporter")
                }
                OutlinedButton(
                    onClick = onRequestImport,
                    enabled = !state.isBusy,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("backup-import"),
                ) {
                    Text("Importer")
                }
            }
        }
    }

    when (state.dialog) {
        BackupDialog.ExportPassword -> ExportPasswordDialog(
            busy = state.isBusy,
            errorMessage = state.errorMessage,
            onConfirm = onSubmitExportPassword,
            onDismiss = onDismissDialog,
        )

        BackupDialog.ImportPassword -> ImportPasswordDialog(
            busy = state.isBusy,
            errorMessage = state.errorMessage,
            onConfirm = onSubmitImportPassword,
            onDismiss = onDismissDialog,
        )

        BackupDialog.Preview -> AlertDialog(
            onDismissRequest = { if (!state.isBusy) onDismissDialog() },
            title = { Text("Importer cette sauvegarde ?") },
            text = {
                val preview = state.preview
                Text(
                    if (preview == null) {
                        "L'aperçu n'est plus disponible."
                    } else {
                        "Créée le ${preview.createdAt.toDisplayDate()}\n" +
                            "${preview.distinctCardCount} cartes distinctes, " +
                            "${preview.totalOwnedCardCount} exemplaires, " +
                            "${preview.openedPackCount} packs ouverts.\n\n" +
                            "La progression actuelle sera remplacée."
                    },
                    modifier = Modifier.testTag("backup-preview"),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmImport,
                    enabled = !state.isBusy,
                    modifier = Modifier.testTag("backup-preview-confirm"),
                ) { Text("Importer") }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissDialog,
                    enabled = !state.isBusy,
                ) { Text("Annuler") }
            },
        )

        BackupDialog.None -> Unit
    }
}

@Composable
private fun ExportPasswordDialog(
    busy: Boolean,
    errorMessage: String?,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Protéger la sauvegarde") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Choisis un mot de passe et conserve-le : il sera nécessaire pour importer la sauvegarde.")
                PasswordField("Mot de passe", password, { password = it }, "backup-export-password")
                PasswordField("Confirmation", confirmation, { confirmation = it }, "backup-export-confirmation")
                errorMessage?.let { Text(it, modifier = Modifier.testTag("backup-dialog-error")) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(password, confirmation) },
                enabled = !busy,
                modifier = Modifier.testTag("backup-export-confirm"),
            ) { Text("Continuer") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Annuler") } },
    )
}

@Composable
private fun ImportPasswordDialog(
    busy: Boolean,
    errorMessage: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Déverrouiller la sauvegarde") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PasswordField("Mot de passe d'export", password, { password = it }, "backup-import-password")
                errorMessage?.let { Text(it, modifier = Modifier.testTag("backup-dialog-error")) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(password) },
                enabled = !busy,
                modifier = Modifier.testTag("backup-import-password-confirm"),
            ) { Text("Vérifier") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Annuler") } },
    )
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    testTag: String,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(
                onClick = { passwordVisible = !passwordVisible },
                modifier = Modifier.testTag("$testTag-visibility"),
            ) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (passwordVisible) {
                        "Masquer le mot de passe"
                    } else {
                        "Afficher le mot de passe"
                    },
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
    )
}

private fun Instant.toDisplayDate(): String = DISPLAY_DATE_FORMATTER.format(this)

private val DISPLAY_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter
    .ofPattern("dd/MM/yyyy 'à' HH:mm")
    .withZone(ZoneId.systemDefault())
