package fr.aumombelli.gatcha

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import fr.aumombelli.gatcha.ui.theme.GatchaTheme

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
