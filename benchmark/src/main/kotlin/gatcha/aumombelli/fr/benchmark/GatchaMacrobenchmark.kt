package fr.aumombelli.gatcha.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GatchaMacrobenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startup() {
        benchmarkRule.measureRepeated(
            packageName = TargetPackageName,
            metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            iterations = 5,
            startupMode = StartupMode.COLD,
            setupBlock = {
                pressHome()
            },
        ) {
            startAndReachMainMenu()
        }
    }

    @Test
    fun libraryScroll() {
        benchmarkRule.measureRepeated(
            packageName = TargetPackageName,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            iterations = 3,
            startupMode = StartupMode.WARM,
            setupBlock = {
                startAndReachMainMenu(resetProgressOnLaunch = true)
                openLibrary()
            },
        ) {
            scrollLibraryGrid()
        }
    }

    @Test
    fun packJourney() {
        benchmarkRule.measureRepeated(
            packageName = TargetPackageName,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            iterations = 3,
            startupMode = StartupMode.WARM,
            setupBlock = {
                startAndReachMainMenu(resetProgressOnLaunch = true)
            },
        ) {
            openPackFlowAndReturnToMenu()
        }
    }
}
