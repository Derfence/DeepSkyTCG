package fr.aumombelli.dstcg.testsupport.fixtures

import fr.aumombelli.dstcg.model.AbsoluteMagnitudeMeasurement
import fr.aumombelli.dstcg.model.AngularMeasurement
import fr.aumombelli.dstcg.model.AstronomyInfo
import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.CardFinishDefinition
import fr.aumombelli.dstcg.model.CardVariant
import fr.aumombelli.dstcg.model.CelestialCoordinates
import fr.aumombelli.dstcg.model.Declination
import fr.aumombelli.dstcg.model.DeepSkyDetails
import fr.aumombelli.dstcg.model.EquipmentBonusUnit
import fr.aumombelli.dstcg.model.EquipmentCardDefinition
import fr.aumombelli.dstcg.model.EquipmentType
import fr.aumombelli.dstcg.model.GameBalanceDefinition
import fr.aumombelli.dstcg.model.LightYearMeasurement
import fr.aumombelli.dstcg.model.OwnedCardEntry
import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.OwnedVariantCount
import fr.aumombelli.dstcg.model.PackCard
import fr.aumombelli.dstcg.model.RightAscension
import fr.aumombelli.dstcg.model.SkyEventDetails
import fr.aumombelli.dstcg.model.SkyQualityDefinition
import fr.aumombelli.dstcg.model.VariantProfile
import fr.aumombelli.dstcg.model.VisualSize

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
    ),
)

fun testGameBalanceDefinition(
    cardsPerDraw: Int = 5,
    drawCooldownHours: Double = 6.0,
    percentUncommonPerDay: Double = 30.0,
    percentRarePerDay: Double = 15.0,
    percentEpicPerDay: Double = 5.0,
    suburbanMeanPerDay: Double = 6.0,
    ruralMeanPerDay: Double = 3.0,
    mountainMeanPerDay: Double = 1.0,
    percentHoloMeanPerDay: Double = 10.0,
): GameBalanceDefinition = GameBalanceDefinition(
    cardsPerDraw = cardsPerDraw,
    drawCooldownHours = drawCooldownHours,
    percentUncommonPerDay = percentUncommonPerDay,
    percentRarePerDay = percentRarePerDay,
    percentEpicPerDay = percentEpicPerDay,
    suburbanMeanPerDay = suburbanMeanPerDay,
    ruralMeanPerDay = ruralMeanPerDay,
    mountainMeanPerDay = mountainMeanPerDay,
    percentHoloMeanPerDay = percentHoloMeanPerDay,
)

fun testCardDefinition(
    id: String,
    extensionId: String = "astronomes-en-herbe",
    name: String = "Nebuleuse d'Orion",
    commonName: String? = name,
    catalogNumber: String = id,
    rarityLabel: String = "Common",
    cardRarityMultiplier: Double = 1.0,
    imageRef: String = "m42_orion_nebula",
    variantProfileId: String = "observation-default",
): CardDefinition = CardDefinition(
    id = id,
    extensionId = extensionId,
    name = name,
    rarityLabel = rarityLabel,
    cardRarityMultiplier = cardRarityMultiplier,
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

fun testEquipmentCardDefinition(
    id: String,
    type: EquipmentType = EquipmentType.Observatory,
    displayName: String = "Equipement test",
    level: Int = 1,
    imageRef: String = "equipment_test",
    packsAffected: Int = 3,
    bonusValue: Double = when (type) {
        EquipmentType.Observatory -> 1.25
        EquipmentType.Telescope -> 8.0
        EquipmentType.Mount -> 10.0
    },
    bonusUnit: EquipmentBonusUnit = when (type) {
        EquipmentType.Observatory -> EquipmentBonusUnit.RechargeMultiplier
        EquipmentType.Telescope -> EquipmentBonusUnit.HolographicPercent
        EquipmentType.Mount -> EquipmentBonusUnit.RarityBoost
    },
    dropWeight: Int = 10,
    description: String = "Carte d'equipement de test.",
): EquipmentCardDefinition = EquipmentCardDefinition(
    id = id,
    type = type,
    displayName = displayName,
    level = level,
    imageRef = imageRef,
    packsAffected = packsAffected,
    bonusValue = bonusValue,
    bonusUnit = bonusUnit,
    dropWeight = dropWeight,
    description = description,
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
    cardRarityMultiplier = 1.0,
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
