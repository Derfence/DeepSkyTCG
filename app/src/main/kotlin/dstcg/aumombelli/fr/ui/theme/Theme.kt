package fr.aumombelli.dstcg.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DstcgColors = darkColorScheme(
    primary = AuroraTeal,
    secondary = EmberGold,
    background = MidnightBlue,
    surface = Void,
    onPrimary = Void,
    onSecondary = Void,
    onBackground = Frost,
    onSurface = Frost,
)

@Composable
fun DstcgTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DstcgColors,
        typography = AppTypography,
        content = content,
    )
}
