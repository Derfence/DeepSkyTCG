package fr.aumombelli.dstcg.audio

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioAssetsTest {
    @Test
    fun `all runtime audio assets exist under sounds`() {
        requiredAudioAssetPaths().forEach { assetPath ->
            assertTrue(
                "Missing audio asset: $assetPath",
                File(assetsRoot(), assetPath).isFile,
            )
        }
    }

    @Test
    fun `all runtime audio assets live in sounds folder`() {
        assertEquals(
            true,
            requiredAudioAssetPaths().all { it.startsWith("$SoundsAssetDirectory/") },
        )
    }

    @Test
    fun `all sound files are wired into runtime audio`() {
        assertEquals(runtimeSoundFileNames(), requiredAudioFileNames())
    }

    @Test
    fun `audio credits catalog lives in sounds folder`() {
        assertEquals("$SoundsAssetDirectory/audio_credits.json", AudioCreditsAssetPath)
        assertTrue(File(assetsRoot(), AudioCreditsAssetPath).isFile)
    }

    @Test
    fun `audio credits match runtime sound files`() {
        val catalog = decodeAudioCreditsCatalog(File(assetsRoot(), AudioCreditsAssetPath).readText())
        val creditedFileNames = catalog.entries.map { it.fileName }.toSet()

        assertEquals(catalog.entries.size, creditedFileNames.size)
        assertEquals(runtimeSoundFileNames(), creditedFileNames)
    }

    private fun assetsRoot(): File = listOf(
        File("src/main/assets"),
        File("app/src/main/assets"),
    ).first { it.isDirectory }

    private fun runtimeSoundFileNames(): Set<String> =
        File(assetsRoot(), SoundsAssetDirectory)
            .listFiles { file -> file.extension in setOf("mp3", "ogg") }
            .orEmpty()
            .mapTo(mutableSetOf()) { it.name }

    private fun requiredAudioFileNames(): Set<String> =
        requiredAudioAssetPaths().mapTo(mutableSetOf()) { it.substringAfter("$SoundsAssetDirectory/") }
}
