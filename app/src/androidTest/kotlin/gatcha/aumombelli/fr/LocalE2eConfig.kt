package fr.aumombelli.gatcha

import androidx.test.platform.app.InstrumentationRegistry

object LocalE2eConfig {
    private val arguments
        get() = InstrumentationRegistry.getArguments()

    val runId: String
        get() = arguments.getString("e2e.runId") ?: "manual"

    val username: String
        get() = arguments.getString("e2e.username") ?: "e2e_manual"

    val email: String
        get() = arguments.getString("e2e.email") ?: "e2e_manual@example.com"

    val password: String
        get() = arguments.getString("e2e.password") ?: "Password123!"

    val extensionId: String
        get() = arguments.getString("e2e.extensionId") ?: "astronomes-en-herbe"
}
