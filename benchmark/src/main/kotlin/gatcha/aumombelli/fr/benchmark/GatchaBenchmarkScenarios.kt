package fr.aumombelli.gatcha.benchmark

import android.content.Intent
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

internal const val TargetPackageName = "fr.aumombelli.gatcha"
private const val LaunchSceneExtraKey = "fr.aumombelli.gatcha.extra.LAUNCH_SCENE"
private const val ResetProgressExtraKey = "fr.aumombelli.gatcha.extra.RESET_PROGRESS"
private const val LaunchSceneMainMenu = "main-menu"
private const val DefaultUiTimeoutMs = 12_000L
private const val TagProbeTimeoutMs = 750L

internal fun MacrobenchmarkScope.startAndReachMainMenu(
    resetProgressOnLaunch: Boolean = false,
) {
    pressHome()
    startActivityAndWait(launchMainMenuIntent(resetProgressOnLaunch))
    waitForTagOrText(
        tag = "menu-open-pack",
        text = "Open Pack",
        timeoutMs = 20_000L,
    )
}

internal fun MacrobenchmarkScope.openLibrary() {
    tapTagOrText(
        tag = "menu-library",
        text = "Library",
    )
    waitForTextContains("Les cartes obtenues")
}

internal fun MacrobenchmarkScope.scrollLibraryGrid() {
    val displayWidth = device.displayWidth
    val displayHeight = device.displayHeight
    repeat(3) {
        device.swipe(
            displayWidth / 2,
            (displayHeight * 0.8f).toInt(),
            displayWidth / 2,
            (displayHeight * 0.25f).toInt(),
            24,
        )
        device.waitForIdle()
    }
}

internal fun MacrobenchmarkScope.openPackFlowAndReturnToMenu() {
    tapTagOrText(
        tag = "menu-open-pack",
        text = "Open Pack",
    )
    waitForTextContains("Choisis l'extension")
    tapFirstExtensionObserver()
    tapFirstBoosterAndWaitForOpening()
    waitForTagOrText(
        tag = "pack-opening-title",
        text = "Pack Opening",
        timeoutMs = 20_000L,
    )
    device.pressBack()
    waitForTagOrText(
        tag = "menu-open-pack",
        text = "Open Pack",
        timeoutMs = 20_000L,
    )
}

internal fun MacrobenchmarkScope.waitForText(
    text: String,
    timeoutMs: Long = DefaultUiTimeoutMs,
) = checkNotNull(findText(text, timeoutMs)) {
    "Timed out waiting for $text"
}

internal fun MacrobenchmarkScope.waitForTextContains(
    text: String,
    timeoutMs: Long = DefaultUiTimeoutMs,
) = checkNotNull(findTextContains(text, timeoutMs)) {
    "Timed out waiting for text containing $text"
}

internal fun MacrobenchmarkScope.waitForTag(
    tag: String,
    timeoutMs: Long = DefaultUiTimeoutMs,
) = device.wait(
    Until.findObject(By.res(TargetPackageName, tag)),
    timeoutMs,
)

internal fun MacrobenchmarkScope.waitForTagOrText(
    tag: String,
    text: String,
    timeoutMs: Long = DefaultUiTimeoutMs,
): UiObject2 = waitForTag(tag, TagProbeTimeoutMs) ?: waitForText(text, timeoutMs)

internal fun MacrobenchmarkScope.tapTagOrText(
    tag: String,
    text: String,
    timeoutMs: Long = DefaultUiTimeoutMs,
) {
    tapObject(waitForTagOrText(tag = tag, text = text, timeoutMs = timeoutMs))
}

private fun MacrobenchmarkScope.tapFirstExtensionObserver() {
    waitForTag(
        tag = "pack-extension-enter-astronomes-en-herbe",
        timeoutMs = TagProbeTimeoutMs,
    )?.let(::tapObject)
        ?: findText("Observer", timeoutMs = 4_000L)?.let(::tapObject)
        ?: device.click(
            (device.displayWidth * 0.5f).toInt(),
            (device.displayHeight * 0.36f).toInt(),
        )
    device.waitForIdle()
}

private fun MacrobenchmarkScope.tapFirstBoosterAndWaitForOpening() {
    waitForTag("pack-booster-0", timeoutMs = TagProbeTimeoutMs)?.let(::tapObject)?.also {
        return
    }

    device.waitForIdle()
    Thread.sleep(1_600)

    val candidateTapPoints = listOf(
        0.32f to 0.44f,
        0.32f to 0.48f,
        0.32f to 0.52f,
        0.28f to 0.48f,
        0.38f to 0.48f,
    )
    for ((xFraction, yFraction) in candidateTapPoints) {
        device.click(
            (device.displayWidth * xFraction).toInt(),
            (device.displayHeight * yFraction).toInt(),
        )
        device.waitForIdle()
        if (findPackOpening(timeoutMs = 2_500L) != null) {
            return
        }
        Thread.sleep(600)
    }
}

private fun MacrobenchmarkScope.findPackOpening(
    timeoutMs: Long = DefaultUiTimeoutMs,
): UiObject2? = waitForTag("pack-opening-title", TagProbeTimeoutMs) ?: findText("Pack Opening", timeoutMs)

private fun MacrobenchmarkScope.findText(
    text: String,
    timeoutMs: Long = DefaultUiTimeoutMs,
): UiObject2? = device.wait(
    Until.findObject(By.text(text)),
    timeoutMs,
)

private fun MacrobenchmarkScope.findTextContains(
    text: String,
    timeoutMs: Long = DefaultUiTimeoutMs,
): UiObject2? = device.wait(
    Until.findObject(By.textContains(text)),
    timeoutMs,
)

private fun MacrobenchmarkScope.tapObject(target: UiObject2) {
    val bounds = target.visibleBounds
    device.click(bounds.centerX(), bounds.centerY())
    device.waitForIdle()
}

private fun launchMainMenuIntent(resetProgressOnLaunch: Boolean): Intent = Intent(Intent.ACTION_MAIN).apply {
    setClassName(TargetPackageName, "$TargetPackageName.MainActivity")
    addCategory(Intent.CATEGORY_LAUNCHER)
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    putExtra(LaunchSceneExtraKey, LaunchSceneMainMenu)
    putExtra(ResetProgressExtraKey, resetProgressOnLaunch)
}
