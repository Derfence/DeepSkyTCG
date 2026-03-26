package fr.aumombelli.gatcha

import androidx.test.platform.app.InstrumentationRegistry

object LocalE2eConfig {
    private val arguments
        get() = InstrumentationRegistry.getArguments()
    val extensionId: String
        get() = arguments.getString("e2e.extensionId") ?: "astronomes-en-herbe"
}
