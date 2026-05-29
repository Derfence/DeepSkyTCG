package fr.aumombelli.dstcg.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

internal enum class SceneNavigationIcon {
    Back,
    Close,
}

@Composable
internal fun SceneNavigationButton(
    icon: SceneNavigationIcon,
    onClick: () -> Unit,
    contentDescription: String,
    testTag: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.38f))
            .testTag(testTag),
    ) {
        Icon(
            imageVector = when (icon) {
                SceneNavigationIcon.Back -> Icons.AutoMirrored.Filled.ArrowBack
                SceneNavigationIcon.Close -> Icons.Filled.Close
            },
            contentDescription = contentDescription,
            tint = Color.White,
        )
    }
}
