package fr.aumombelli.dstcg.ui.screen

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun Modifier.dstcgContentInsetsPadding(
    includeBottom: Boolean = false,
): Modifier {
    val sides = if (includeBottom) {
        WindowInsetsSides.Horizontal + WindowInsetsSides.Vertical
    } else {
        WindowInsetsSides.Horizontal + WindowInsetsSides.Top
    }
    return windowInsetsPadding(WindowInsets.safeDrawing.only(sides))
}

@Composable
internal fun Modifier.dstcgBottomInsetsPadding(): Modifier =
    windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
