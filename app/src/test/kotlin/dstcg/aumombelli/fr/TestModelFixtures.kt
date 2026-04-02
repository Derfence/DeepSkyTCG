package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.model.OwnedVariantCount
import fr.aumombelli.dstcg.testsupport.fixtures.ownedCollectionOf as fixtureOwnedCollectionOf
import fr.aumombelli.dstcg.testsupport.fixtures.ownedCollectionWithVariants as fixtureOwnedCollectionWithVariants
import fr.aumombelli.dstcg.testsupport.fixtures.testCardDefinition as fixtureTestCardDefinition
import fr.aumombelli.dstcg.testsupport.fixtures.testPackCard as fixtureTestPackCard
import fr.aumombelli.dstcg.testsupport.fixtures.testSkyEventCardDefinition as fixtureTestSkyEventCardDefinition
import fr.aumombelli.dstcg.testsupport.fixtures.testVariantProfiles as fixtureTestVariantProfiles

fun testVariantProfiles() = fixtureTestVariantProfiles()

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
) = fixtureTestCardDefinition(
    id = id,
    extensionId = extensionId,
    name = name,
    commonName = commonName,
    catalogNumber = catalogNumber,
    rarityLabel = rarityLabel,
    drawWeight = drawWeight,
    imageRef = imageRef,
    variantProfileId = variantProfileId,
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
) = fixtureTestPackCard(
    cardId = cardId,
    name = name,
    rarityLabel = rarityLabel,
    imageRef = imageRef,
    skyQuality = skyQuality,
    skyQualityLabel = skyQualityLabel,
    finish = finish,
    finishLabel = finishLabel,
    isHolographic = isHolographic,
)

fun testSkyEventCardDefinition(
    id: String,
    name: String = "Perseides",
    withVisualSize: Boolean = true,
) = fixtureTestSkyEventCardDefinition(
    id = id,
    name = name,
    withVisualSize = withVisualSize,
)

fun ownedCollectionOf(vararg cards: Pair<String, Int>) = fixtureOwnedCollectionOf(*cards)

fun ownedCollectionWithVariants(
    cardId: String,
    vararg variants: OwnedVariantCount,
) = fixtureOwnedCollectionWithVariants(cardId, *variants)
