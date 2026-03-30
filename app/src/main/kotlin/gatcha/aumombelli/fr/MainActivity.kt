package fr.aumombelli.gatcha

import android.content.Context
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import fr.aumombelli.gatcha.app.AppLaunchSceneExtraKey
import fr.aumombelli.gatcha.app.AppResetProgressExtraKey
import fr.aumombelli.gatcha.app.parseAppLaunchConfig
import fr.aumombelli.gatcha.ui.theme.GatchaTheme

class MainActivity : ComponentActivity() {
    companion object {
        var appContainerFactory: ((Context) -> AppContainer)? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        val appContainer = (appContainerFactory ?: AppContainer::create).invoke(applicationContext)
        val launchConfig = parseAppLaunchConfig(
            rawSceneValue = intent.getStringExtra(AppLaunchSceneExtraKey),
            resetProgressOnLaunch = intent.getBooleanExtra(AppResetProgressExtraKey, false),
        )
        setContent {
            GatchaTheme {
                GatchaApp(
                    appContainer = appContainer,
                    launchConfig = launchConfig,
                )
            }
        }
    }
}
