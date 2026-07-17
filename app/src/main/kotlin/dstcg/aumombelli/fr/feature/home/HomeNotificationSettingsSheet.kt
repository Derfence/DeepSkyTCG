package fr.aumombelli.dstcg.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.notification.NotificationSettings

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun HomeNotificationSettingsSheet(
    visible: Boolean,
    settings: NotificationSettings,
    systemPermissionGranted: Boolean,
    onFullStockEnabledChange: (Boolean) -> Unit,
    onReturnReminderEnabledChange: (Boolean) -> Unit,
    onOpenSystemSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("home-notification-settings-sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = "Notifications",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            Text(
                text = if (systemPermissionGranted) {
                    "Choisis les rappels que tu souhaites recevoir."
                } else {
                    "Les notifications sont bloquées par Android. Active un rappel pour redemander l’autorisation."
                },
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            ListItem(
                headlineContent = { Text("Stock de packs plein") },
                supportingContent = { Text("Quand le dixième pack est rechargé.") },
                trailingContent = {
                    Switch(
                        checked = settings.fullStockEnabled,
                        onCheckedChange = onFullStockEnabledChange,
                        modifier = Modifier.testTag("home-notification-full-stock-switch"),
                    )
                },
                modifier = Modifier.testTag("home-notification-full-stock-row"),
            )
            ListItem(
                headlineContent = { Text("Rappel après 7 jours") },
                supportingContent = { Text("Une seule fois jusqu’à ta prochaine ouverture de l’app.") },
                trailingContent = {
                    Switch(
                        checked = settings.returnReminderEnabled,
                        onCheckedChange = onReturnReminderEnabledChange,
                        modifier = Modifier.testTag("home-notification-return-switch"),
                    )
                },
                modifier = Modifier.testTag("home-notification-return-row"),
            )
            if (!systemPermissionGranted) {
                TextButton(
                    onClick = onOpenSystemSettings,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .testTag("home-notification-system-settings"),
                ) {
                    Text("Ouvrir les réglages Android")
                }
            }
        }
    }
}
