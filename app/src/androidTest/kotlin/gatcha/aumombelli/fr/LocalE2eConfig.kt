package fr.aumombelli.gatcha

import androidx.test.platform.app.InstrumentationRegistry

object LocalE2eConfig {
    private val arguments
        get() = InstrumentationRegistry.getArguments()
    private val defaultRunSeed = "manual-${System.currentTimeMillis()}"
    private val generatedUsername by lazy {
        "e2e_${runId.lowercase().replace(Regex("[^a-z0-9]"), "").ifBlank { "manual" }.takeLast(16)}"
    }
    private val generatedEmail by lazy { "$generatedUsername@example.com" }

    val runId: String
        get() = arguments.getString("e2e.runId") ?: defaultRunSeed

    val username: String
        get() = arguments.getString("e2e.username") ?: generatedUsername

    val email: String
        get() = arguments.getString("e2e.email") ?: generatedEmail

    val password: String
        get() = arguments.getString("e2e.password") ?: "Password123!"

    val extensionId: String
        get() = arguments.getString("e2e.extensionId") ?: "astronomes-en-herbe"
}
