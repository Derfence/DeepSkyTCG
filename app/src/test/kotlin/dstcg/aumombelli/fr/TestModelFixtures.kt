package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.DEFAULT_DRAW_COOLDOWN
import fr.aumombelli.dstcg.model.OwnedVariantCount
import fr.aumombelli.dstcg.model.PackRechargeState
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
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

private val defaultRechargeReferenceNow: Instant = Instant.parse("2026-03-24T12:00:00Z")

fun testRechargeState(
    availableDrawCount: Int = 10,
    accumulatedChargeUnits: Long = 0L,
    lastChargeEvaluationAt: String? = null,
) = PackRechargeState(
    availableDrawCount = availableDrawCount,
    accumulatedChargeUnits = accumulatedChargeUnits,
    lastChargeEvaluationAt = lastChargeEvaluationAt,
)

fun testRechargeStateWithNextChargeAt(
    availableDrawCount: Int,
    nextChargeAt: String?,
    now: Instant = defaultRechargeReferenceNow,
    drawCooldown: Duration = DEFAULT_DRAW_COOLDOWN,
) : PackRechargeState {
    if (availableDrawCount >= 10 || nextChargeAt == null) {
        return PackRechargeState(availableDrawCount = availableDrawCount)
    }

    val normalizedNow = now.truncatedTo(ChronoUnit.SECONDS)
    val parsedNextChargeAt = Instant.parse(nextChargeAt).truncatedTo(ChronoUnit.SECONDS)
    val drawCooldownMillis = drawCooldown.toMillis()
    val effectiveAvailableDrawCount: Int
    val normalizedNextChargeAt: Instant
    if (normalizedNow.isBefore(parsedNextChargeAt)) {
        effectiveAvailableDrawCount = availableDrawCount
        normalizedNextChargeAt = parsedNextChargeAt
    } else {
        val elapsedMillis = Duration.between(parsedNextChargeAt, normalizedNow).toMillis()
        val recoveredDrawCount = 1 + (elapsedMillis / drawCooldownMillis)
        effectiveAvailableDrawCount = (availableDrawCount + recoveredDrawCount.toInt()).coerceAtMost(10)
        if (effectiveAvailableDrawCount >= 10) {
            return PackRechargeState(availableDrawCount = 10)
        }
        normalizedNextChargeAt = parsedNextChargeAt.plusMillis(recoveredDrawCount * drawCooldownMillis)
    }
    val remainingSeconds = Duration.between(
        normalizedNow,
        normalizedNextChargeAt,
    ).seconds.coerceAtLeast(0L)
    val cooldownUnits = drawCooldown.seconds * 5L
    return PackRechargeState(
        availableDrawCount = effectiveAvailableDrawCount,
        accumulatedChargeUnits = (cooldownUnits - (remainingSeconds * 5L)).coerceIn(0L, cooldownUnits - 1L),
        lastChargeEvaluationAt = normalizedNow.toString(),
    )
}
