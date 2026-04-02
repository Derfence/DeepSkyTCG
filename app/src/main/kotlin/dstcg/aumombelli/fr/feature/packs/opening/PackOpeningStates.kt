package fr.aumombelli.dstcg.feature.packs.opening

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding

@Composable
internal fun EmptyPackState(
    onDone: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .dstcgContentInsetsPadding(includeBottom = true)
            .padding(24.dp),
    ) {
        Text("Aucun pack ouvert n'est disponible.", color = Color.White)
        Button(
            onClick = onDone,
            modifier = Modifier.testTag("pack-opening-done"),
        ) {
            Text("Retour au menu")
        }
    }
}

@Composable
internal fun PackOpeningErrorState(
    message: String,
    onDone: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .dstcgContentInsetsPadding(includeBottom = true)
            .padding(24.dp),
    ) {
        Text(message, color = Color(0xFFFFA3A3))
        Button(
            onClick = onDone,
            modifier = Modifier.testTag("pack-opening-done"),
        ) {
            Text("Retour au menu")
        }
    }
}
