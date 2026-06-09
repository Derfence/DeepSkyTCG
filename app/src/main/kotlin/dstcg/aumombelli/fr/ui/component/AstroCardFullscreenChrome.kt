package fr.aumombelli.dstcg.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
internal fun AstroCardFullscreenCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "astro-card-fullscreen-close",
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.34f))
            .testTag(testTag),
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Fermer",
            tint = Color.White,
        )
    }
}
