package fr.aumombelli.dstcg.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DstcgBaselineProfile {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() {
        baselineProfileRule.collect(
            packageName = TargetPackageName,
            includeInStartupProfile = true,
        ) {
            startAndReachMainMenu(resetProgressOnLaunch = true)
            openLibrary()
            scrollLibraryGrid()
            device.pressBack()
            waitForText("Open Pack")
            openPackFlowAndReturnToMenu()
        }
    }
}
