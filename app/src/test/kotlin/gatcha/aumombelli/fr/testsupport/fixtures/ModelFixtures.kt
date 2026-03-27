package fr.aumombelli.gatcha.testsupport.fixtures

import fr.aumombelli.gatcha.model.AbsoluteMagnitudeMeasurement
import fr.aumombelli.gatcha.model.AngularMeasurement
import fr.aumombelli.gatcha.model.AstronomyInfo
import fr.aumombelli.gatcha.model.CardDefinition
import fr.aumombelli.gatcha.model.CardFinishDefinition
import fr.aumombelli.gatcha.model.CardVariant
import fr.aumombelli.gatcha.model.CelestialCoordinates
import fr.aumombelli.gatcha.model.Declination
import fr.aumombelli.gatcha.model.DeepSkyDetails
import fr.aumombelli.gatcha.model.LightYearMeasurement
import fr.aumombelli.gatcha.model.OwnedCardEntry
import fr.aumombelli.gatcha.model.OwnedCollection
import fr.aumombelli.gatcha.model.OwnedVariantCount
import fr.aumombelli.gatcha.model.PackCard
import fr.aumombelli.gatcha.model.RightAscension
import fr.aumombelli.gatcha.model.SkyEventDetails
import fr.aumombelli.gatcha.model.SkyQualityDefinition
import fr.aumombelli.gatcha.model.VariantProfile
import fr.aumombelli.gatcha.model.VisualSize
import fr.aumombelli.gatcha.model.WeightedCode

fun testVariantProfiles(): List<VariantProfile> = listOf(
    VariantProfile(
        id = "observation-default",
        skyQualities = listOf(
            SkyQualityDefinition("city", "Ville"),
            SkyQualityDefinition("suburban", "Periurbain"),
            SkyQualityDefinition("rural", "Campagne"),
            SkyQualityDefinition("mountain", "Montagne"),
        ),
        finishes = listOf(
            CardFinishDefinition("standard", "Standard"),
            CardFinishDefinition("holographic", "Holographique", isHolographic = true),
        ),
        skyQualityWeights = listOf(
            WeightedCode("city", 40),
            WeightedCode("suburban", 30),
            WeightedCode("rural", 20),
            WeightedCode("mountain", 10),
        ),
        finishWeights = listOf(
            WeightedCode("standard", 88),
            WeightedCode("holographic", 12),
        ),
    ),
)

fun testCardDefinition(
    id: String,
    extensionId: String = "astronomes-en-herbe",
    name: String = "Nebuleuse d'Orion",
    commonName: String? = name,
    catalogNumber: String = id,
    rarityLabel: String = "Common",
    drawWeight: Int = 1,
    imageRef: String = "m42_orion_nebula",
    variantProfileId: String = "observation-default",
): CardDefinition = CardDefinition(
    id = id,
    extensionId = extensionId,
    name = name,
    rarityLabel = rarityLabel,
    drawWeight = drawWeight,
    imageRef = imageRef,
    variantProfileId = variantProfileId,
    astronomy = AstronomyInfo(
        commonName = commonName,
        primaryCatalogName = "Messier",
        catalogNumber = catalogNumber,
        objectFamily = "deep_sky",
        objectTypeLabel = "Nebuleuse",
        constellation = "Orion",
        mainSeason = "Hiver",
        coordinates = CelestialCoordinates(
            rightAscension = RightAscension(5, 35, 17.3, "AD 05h 35m 17,3s"),
            declination = Declination("-", 5, 23, 28, "-05° 23′ 28″"),
            label = "AD 05h 35m 17,3s ; Dec -05° 23′ 28″",
        ),
        shortDescription = "Objet de test astronomique.",
        details = DeepSkyDetails(
            distance = LightYearMeasurement(1500.0, "1 500 annees-lumiere"),
            realSize = LightYearMeasurement(25.0, "25 annees-lumiere"),
            visualSize = VisualSize(
                fullMoonWidth = 2.1,
                fullMoonHeight = 1.94,
                angularWidth = AngularMeasurement(1, 5, 0, "1°05′00″"),
                angularHeight = AngularMeasurement(1, 0, 0, "1°00′00″"),
                label = "2,10 × 1,94 (1°05′00″ × 1°00′00″)",
            ),
            absoluteMagnitude = AbsoluteMagnitudeMeasurement(-4.1, "-4.1"),
        ),
    ),
)

fun testPackCard(
    cardId: String,
    name: String,
    rarityLabel: String,
    imageRef: String,
    skyQuality: String = "city",
    skyQualityLabel: String = "Ville",
    finish: String = "standard",
    finishLabel: String = "Standard",
    isHolographic: Boolean = false,
): PackCard = PackCard(
    cardId = cardId,
    name = name,
    rarityLabel = rarityLabel,
    imageRef = imageRef,
    variant = CardVariant(
        skyQuality = skyQuality,
        skyQualityLabel = skyQualityLabel,
        finish = finish,
        finishLabel = finishLabel,
        isHolographic = isHolographic,
    ),
)

fun testSkyEventCardDefinition(
    id: String,
    name: String = "Perseides",
    withVisualSize: Boolean = true,
): CardDefinition = CardDefinition(
    id = id,
    extensionId = "astronomes-en-herbe",
    name = name,
    rarityLabel = "Epic",
    drawWeight = 1,
    imageRef = "sky_event",
    variantProfileId = "observation-default",
    astronomy = AstronomyInfo(
        commonName = name,
        primaryCatalogName = "IMO",
        catalogNumber = id,
        objectFamily = "sky_event",
        objectTypeLabel = "Essaim d'etoiles filantes",
        constellation = "Persee",
        mainSeason = "Ete",
        coordinates = CelestialCoordinates(
            rightAscension = RightAscension(3, 4, 0.0, "AD 03h 04m 00,0s"),
            declination = Declination("+", 58, 0, 0, "+58° 00′ 00″"),
            label = "AD 03h 04m 00,0s ; Dec +58° 00′ 00″",
        ),
        shortDescription = "Evenement celeste de test.",
        details = SkyEventDetails(
            visualSize = if (withVisualSize) {
                VisualSize(
                    fullMoonWidth = 180.0,
                    fullMoonHeight = 90.0,
                    angularWidth = AngularMeasurement(90, 0, 0, "90°00′00″"),
                    angularHeight = AngularMeasurement(45, 0, 0, "45°00′00″"),
                    label = "180,00 × 90,00 (90°00′00″ × 45°00′00″)",
                )
            } else {
                null
            },
        ),
    ),
)

fun ownedCollectionOf(vararg cards: Pair<String, Int>): OwnedCollection =
    OwnedCollection(
        cards = cards.associate { (cardId, count) ->
            cardId to OwnedCardEntry(
                totalOwned = count,
                variants = listOf(OwnedVariantCount("city", "standard", count)),
            )
        }.toSortedMap(),
    )

fun ownedCollectionWithVariants(
    cardId: String,
    vararg variants: OwnedVariantCount,
): OwnedCollection =
    OwnedCollection(
        cards = sortedMapOf(
            cardId to OwnedCardEntry(
                totalOwned = variants.sumOf { it.count },
                variants = variants.toList(),
            ),
        ),
    )
