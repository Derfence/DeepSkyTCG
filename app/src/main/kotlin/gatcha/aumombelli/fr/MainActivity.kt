package gatcha.aumombelli.fr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import gatcha.aumombelli.fr.ui.theme.GatchaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContainer = AppContainer(applicationContext)
        setContent {
            GatchaTheme {
                GatchaApp(appContainer = appContainer)
            }
        }
    }
}
