package fr.aumombelli.dstcg

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import fr.aumombelli.dstcg.app.AppLaunchSceneExtraKey
import fr.aumombelli.dstcg.app.AppResetProgressExtraKey
import fr.aumombelli.dstcg.app.parseAppLaunchConfig
import fr.aumombelli.dstcg.notification.AndroidNotificationPublisher
import fr.aumombelli.dstcg.notification.NotificationPreferencesRepository
import fr.aumombelli.dstcg.ui.theme.DstcgTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        var appContainerFactory: ((Context) -> AppContainer)? = null
    }

    private lateinit var notificationPreferences: NotificationPreferencesRepository
    private var appContentStarted = false
    private var launchGateOpen = false
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        lifecycleScope.launch {
            notificationPreferences.completeAutomaticPermissionRequest(granted)
            startAppContent()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { !launchGateOpen }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        notificationPreferences = NotificationPreferencesRepository.fromContext(applicationContext)
        if (appContainerFactory != null) {
            startAppContent()
            return
        }
        lifecycleScope.launch {
            val settings = notificationPreferences.current()
            val permissionGranted = AndroidNotificationPublisher.hasRuntimePermission(this@MainActivity)
            when {
                settings.automaticPermissionRequested -> startAppContent()
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || permissionGranted -> {
                    notificationPreferences.completeAutomaticPermissionRequest(granted = true)
                    startAppContent()
                }
                else -> {
                    // Persist before launching so a killed activity does not ask automatically again.
                    notificationPreferences.completeAutomaticPermissionRequest(granted = false)
                    launchGateOpen = true
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun startAppContent() {
        if (appContentStarted) return
        appContentStarted = true
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
        launchGateOpen = true
    }
}
