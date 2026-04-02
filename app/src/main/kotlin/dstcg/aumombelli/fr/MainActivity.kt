package fr.aumombelli.dstcg

import android.content.Context
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import fr.aumombelli.dstcg.app.AppLaunchSceneExtraKey
import fr.aumombelli.dstcg.app.AppResetProgressExtraKey
import fr.aumombelli.dstcg.app.parseAppLaunchConfig
import fr.aumombelli.dstcg.ui.theme.DstcgTheme

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
            DstcgTheme {
                DstcgApp(
                    appContainer = appContainer,
                    launchConfig = launchConfig,
                )
            }
        }
    }
}
