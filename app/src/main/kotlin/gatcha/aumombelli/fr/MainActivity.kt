package fr.aumombelli.gatcha

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import fr.aumombelli.gatcha.ui.theme.GatchaTheme

class MainActivity : ComponentActivity() {
    companion object {
        var appContainerFactory: ((Context) -> AppContainer)? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContainer = (appContainerFactory ?: AppContainer::create).invoke(applicationContext)
        setContent {
            GatchaTheme {
                GatchaApp(appContainer = appContainer)
            }
        }
    }
}
