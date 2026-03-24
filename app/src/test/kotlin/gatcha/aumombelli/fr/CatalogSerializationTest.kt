package fr.aumombelli.gatcha

import fr.aumombelli.gatcha.model.CardDefinition
import fr.aumombelli.gatcha.model.SkyEventDetails
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogSerializationTest {
    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "detailType"
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
              "drawWeight": 1,
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
              "rarityLabel": "Legendary",
              "drawWeight": 1,
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
}
