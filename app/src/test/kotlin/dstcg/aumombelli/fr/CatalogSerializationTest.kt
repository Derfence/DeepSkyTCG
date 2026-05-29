package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.model.AstronomyDetails
import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.ConstellationDetails
import fr.aumombelli.dstcg.model.DeepSkyDetails
import fr.aumombelli.dstcg.model.SkyEventDetails
import fr.aumombelli.dstcg.model.SolarSystemDetails
import fr.aumombelli.dstcg.model.StarDetails
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogSerializationTest {
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
    fun `client catalog decodes sky event with visual size`() {
        val card = json.decodeFromString<CardDefinition>(
            """
            {
              "id": "evt-perseides",
              "extensionId": "astronomes-en-herbe",
              "name": "Perseides",
              "rarityLabel": "Epic",
              "cardRarityMultiplier": 1.0,
              "imageRef": "perseides",
              "variantProfileId": "observation-default",
              "astronomy": {
                "commonName": "Perseides",
                "primaryCatalogName": "IMO",
                "catalogNumber": "PER",
                "objectFamily": "sky_event",
                "objectTypeLabel": "Essaim d'etoiles filantes",
                "constellation": "Persee",
                "mainSeason": "Ete",
                "coordinates": {
                  "rightAscension": {
                    "hours": 3,
                    "minutes": 4,
                    "seconds": 0.0,
                    "label": "AD 03h 04m 00,0s"
                  },
                  "declination": {
                    "sign": "+",
                    "degrees": 58,
                    "arcMinutes": 0,
                    "arcSeconds": 0,
                    "label": "+58° 00′ 00″"
                  },
                  "label": "AD 03h 04m 00,0s ; Dec +58° 00′ 00″"
                },
                "shortDescription": "Essaim de test.",
                "details": {
                  "detailType": "sky_event",
                  "visualSize": {
                    "fullMoonWidth": 180.0,
                    "fullMoonHeight": 90.0,
                    "angularWidth": {
                      "degrees": 90,
                      "arcMinutes": 0,
                      "arcSeconds": 0,
                      "label": "90°00′00″"
                    },
                    "angularHeight": {
                      "degrees": 45,
                      "arcMinutes": 0,
                      "arcSeconds": 0,
                      "label": "45°00′00″"
                    },
                    "label": "180,00 × 90,00 (90°00′00″ × 45°00′00″)"
                  }
                }
              }
            }
            """.trimIndent(),
        )

        val details = card.astronomy.details
        assertTrue(details is SkyEventDetails)
        assertEquals(
            "180,00 × 90,00 (90°00′00″ × 45°00′00″)",
            (details as SkyEventDetails).visualSize?.label,
        )
    }

    @Test
    fun `client catalog decodes sky event without visual size`() {
        val card = json.decodeFromString<CardDefinition>(
            """
            {
              "id": "evt-halley",
              "extensionId": "astronomes-en-herbe",
              "name": "Comete de Halley",
              "rarityLabel": "Epic",
              "cardRarityMultiplier": 1.0,
              "imageRef": "halley",
              "variantProfileId": "observation-default",
              "astronomy": {
                "commonName": "Comete de Halley",
                "primaryCatalogName": "Periodic Comet",
                "catalogNumber": "1P/Halley",
                "objectFamily": "sky_event",
                "objectTypeLabel": "Comete periodique",
                "constellation": "Variable",
                "mainSeason": "Variable",
                "coordinates": {
                  "rightAscension": {
                    "hours": 0,
                    "minutes": 0,
                    "seconds": 0.0,
                    "label": "AD 00h 00m 00,0s"
                  },
                  "declination": {
                    "sign": "+",
                    "degrees": 0,
                    "arcMinutes": 0,
                    "arcSeconds": 0,
                    "label": "+00° 00′ 00″"
                  },
                  "label": "AD 00h 00m 00,0s ; Dec +00° 00′ 00″"
                },
                "shortDescription": "Comete de test.",
                "details": {
                  "detailType": "sky_event"
                }
              }
            }
            """.trimIndent(),
        )

        val details = card.astronomy.details
        assertTrue(details is SkyEventDetails)
        assertNull((details as SkyEventDetails).visualSize)
    }

    @Test
    fun `client catalog decodes solar system details`() {
        val card = json.decodeFromString<CardDefinition>(
            """
            {
              "id": "ss-mercury",
              "extensionId": "systeme-solaire",
              "name": "Mercure",
              "rarityLabel": "Common",
              "cardRarityMultiplier": 1.0,
              "imageRef": "planet_mercury",
              "variantProfileId": "observation-default",
              "astronomy": {
                "commonName": "Mercure",
                "primaryCatalogName": "Planète",
                "catalogNumber": "1",
                "objectFamily": "solar_system",
                "objectTypeLabel": "Planète tellurique",
                "constellation": "Variable",
                "mainSeason": "Toute l'année",
                "coordinates": {
                  "rightAscension": { "hours": 0, "minutes": 0, "seconds": 0.0, "label": "AD variable" },
                  "declination": { "sign": "+", "degrees": 0, "arcMinutes": 0, "arcSeconds": 0, "label": "Dec variable" },
                  "label": "Position variable selon la date"
                },
                "shortDescription": "Petite planète rocheuse.",
                "details": {
                  "detailType": "solar_system",
                  "distance": { "lightYears": 0.39, "label": "0,39 UA du Soleil" },
                  "realSize": { "lightYears": 4879.0, "label": "4 879 km" },
                  "absoluteMagnitude": { "value": -2.0, "label": "mag. -2,0 max." }
                }
              }
            }
            """.trimIndent(),
        )

        val details = card.astronomy.details
        assertTrue(details is SolarSystemDetails)
        assertEquals("0,39 UA du Soleil", (details as SolarSystemDetails).distance?.label)
        assertEquals("4 879 km", details.realSize?.label)
        assertEquals("mag. -2,0 max.", details.absoluteMagnitude?.label)
    }
}
