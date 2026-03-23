package gatcha.aumombelli.fr.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GatchaColors = darkColorScheme(
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
fun GatchaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GatchaColors,
        typography = AppTypography,
        content = content,
    )
}
