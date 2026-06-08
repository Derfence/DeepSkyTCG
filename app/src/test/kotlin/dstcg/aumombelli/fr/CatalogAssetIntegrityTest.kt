package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.model.AstronomyDetails
import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.ConstellationDetails
import fr.aumombelli.dstcg.model.DeepSkyDetails
import fr.aumombelli.dstcg.model.SkyEventDetails
import fr.aumombelli.dstcg.model.SolarSystemDetails
import fr.aumombelli.dstcg.model.StarDetails
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogAssetIntegrityTest {
    private val astronomyDetailsSerializersModule = SerializersModule {
        polymorphic(AstronomyDetails::class) {
            subclass(DeepSkyDetails::class)
            subclass(StarDetails::class)
            subclass(ConstellationDetails::class)
            subclass(SkyEventDetails::class)
            subclass(SolarSystemDetails::class)
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "detailType"
        serializersModule = astronomyDetailsSerializersModule
    }

    @Test
    fun `catalog card arts resolve to runtime assets`() {
        val cardArtRoot = File(assetsRoot(), "card_art")
        val imageRefsByExtension = imageRefsByExtension()
        val reservedRuntimeFolders = setOf("_fallbacks", "equipements")

        val missingAssets = imageRefsByExtension.flatMap { (extensionId, imageRefs) ->
            imageRefs.mapNotNull { imageRef ->
                val assetPath = "$extensionId/$imageRef.webp"
                assetPath.takeUnless { File(cardArtRoot, it).isFile }
            }
        }.sorted()
        val orphanAssets = imageRefsByExtension.flatMap { (extensionId, imageRefs) ->
            val extensionFolder = File(cardArtRoot, extensionId)
            extensionFolder.listFiles { file -> file.isFile && file.extension == "webp" }
                .orEmpty()
                .mapNotNull { file ->
                    val imageRef = file.nameWithoutExtension
                    "$extensionId/${file.name}".takeIf { imageRef !in imageRefs }
                }
        }.sorted()
        val unexpectedRuntimeFolders = cardArtRoot
            .listFiles { file -> file.isDirectory }
            .orEmpty()
            .map(File::getName)
            .filter { it !in imageRefsByExtension.keys && it !in reservedRuntimeFolders }
            .sorted()

        assertTrue(
            "Missing card art assets:\n${missingAssets.joinToString("\n")}",
            missingAssets.isEmpty(),
        )
        assertTrue(
            "Runtime card art assets without catalog imageRef:\n${orphanAssets.joinToString("\n")}",
            orphanAssets.isEmpty(),
        )
        assertTrue(
            "Unexpected runtime card art folders:\n${unexpectedRuntimeFolders.joinToString("\n")}",
            unexpectedRuntimeFolders.isEmpty(),
        )
    }

    @Test
    fun `card art credits match catalog image refs`() {
        val creditsFile = File(assetsRoot(), "card_art/card_art_credits.json")
        val credits = json.decodeFromString<Map<String, Map<String, JsonElement>>>(
            creditsFile.readText(),
        )
        val imageRefsByExtension = imageRefsByExtension()

        val missingCredits = imageRefsByExtension.flatMap { (extensionId, imageRefs) ->
            val extensionCredits = credits[extensionId].orEmpty()
            imageRefs.mapNotNull { imageRef ->
                "$extensionId/$imageRef".takeIf { imageRef !in extensionCredits }
            }
        }.sorted()
        val orphanCredits = credits.flatMap { (extensionId, extensionCredits) ->
            val imageRefs = imageRefsByExtension[extensionId].orEmpty()
            extensionCredits.keys.mapNotNull { imageRef ->
                "$extensionId/$imageRef".takeIf { imageRef !in imageRefs }
            }
        }.sorted()

        assertTrue(
            "Missing card art credits:\n${missingCredits.joinToString("\n")}",
            missingCredits.isEmpty(),
        )
        assertTrue(
            "Card art credits without catalog imageRef:\n${orphanCredits.joinToString("\n")}",
            orphanCredits.isEmpty(),
        )
    }

    private fun imageRefsByExtension(): Map<String, Set<String>> =
        catalogCards()
            .groupBy(CardDefinition::extensionId)
            .mapValues { (_, cards) -> cards.map(CardDefinition::imageRef).toSet() }

    private fun catalogCards(): List<CardDefinition> {
        val file = existingFile(
            "app/src/main/assets/catalog/cards.json",
            "src/main/assets/catalog/cards.json",
        )
        return json.decodeFromString<List<CardDefinition>>(file.readText())
    }

    private fun assetsRoot(): File =
        existingFile("app/src/main/assets", "src/main/assets")

    private fun existingFile(vararg candidates: String): File =
        candidates.map(::File).firstOrNull(File::exists)
            ?: error("None of these paths exist: ${candidates.joinToString()}")
}
