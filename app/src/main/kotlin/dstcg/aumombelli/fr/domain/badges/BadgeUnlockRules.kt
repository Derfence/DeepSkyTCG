package fr.aumombelli.dstcg.domain.badges

import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.EquipmentCardDefinition
import fr.aumombelli.dstcg.model.EquipmentType
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.OwnedVariantCount
import fr.aumombelli.dstcg.model.SkyQualityDefinition
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.model.VariantProfile
import fr.aumombelli.dstcg.model.activatedEquipmentCardCount
import fr.aumombelli.dstcg.model.normalized
import fr.aumombelli.dstcg.model.totalEquipmentActivationCount

fun buildNewlyUnlockedBadgeIds(
    extensions: List<ExtensionDefinition>,
    cards: List<CardDefinition>,
    equipmentCards: List<EquipmentCardDefinition>,
    variantProfiles: List<VariantProfile>,
    beforeProgress: StandaloneProgress,
    afterProgress: StandaloneProgress,
): Set<String> {
    val beforeUnlockedIds = buildUnlockedBadgeIds(
        extensions = extensions,
        cards = cards,
        equipmentCards = equipmentCards,
        variantProfiles = variantProfiles,
        progress = beforeProgress,
    )

    return buildUnlockedBadgeIds(
        extensions = extensions,
        cards = cards,
        equipmentCards = equipmentCards,
        variantProfiles = variantProfiles,
        progress = afterProgress,
    ).filterNotTo(mutableSetOf()) { badgeId -> badgeId in beforeUnlockedIds }
}

fun buildUnlockedBadgeIds(
    extensions: List<ExtensionDefinition>,
    cards: List<CardDefinition>,
    equipmentCards: List<EquipmentCardDefinition>,
    variantProfiles: List<VariantProfile>,
    progress: StandaloneProgress,
): Set<String> {
    val variantProfilesById = variantProfiles.associateBy { it.id }
    return buildSet {
        addUnlockedGeneralBadgeIds(progress, equipmentCards)
        extensions.forEach { extension ->
            addUnlockedExtensionBadgeIds(
                extension = extension,
                cards = cards.filter { card -> card.extensionId == extension.id },
                collection = progress.collection,
                variantProfilesById = variantProfilesById,
            )
        }
    }
}

private fun MutableSet<String>.addUnlockedGeneralBadgeIds(
    progress: StandaloneProgress,
    equipmentCards: List<EquipmentCardDefinition>,
) {
    if (progress.openedPackCount >= 1) {
        add("$GeneralBadgeSectionId::pack::first-opened")
    }
    if (progress.hasOpenedEpicBoostedPack) {
        add("$GeneralBadgeSectionId::pack::epic-boost-opened")
    }
    if (
        equipmentCards.isNotEmpty() &&
        progress.activatedEquipmentCardCount(equipmentCards) >= equipmentCards.size
    ) {
        add("$GeneralBadgeSectionId::equipment::all-used-once")
    }
    if (progress.equipmentBadgeProgress.maxSimultaneouslyActiveEquipmentTypeCount >= EquipmentType.entries.size) {
        add("$GeneralBadgeSectionId::equipment::three-types-active")
    }
    if (progress.equipmentBadgeProgress.maxSimultaneouslyActiveLevelThreeEquipmentTypeCount >= EquipmentType.entries.size) {
        add("$GeneralBadgeSectionId::equipment::three-level-three-active")
    }
    if (progress.equipmentBadgeProgress.affectedPackCount >= 100) {
        add("$GeneralBadgeSectionId::equipment::packs-affected-100")
    }
    if (progress.totalEquipmentActivationCount() >= 100) {
        add("$GeneralBadgeSectionId::equipment::activations-100")
    }
}

private fun MutableSet<String>.addUnlockedExtensionBadgeIds(
    extension: ExtensionDefinition,
    cards: List<CardDefinition>,
    collection: OwnedCollection,
    variantProfilesById: Map<String, VariantProfile>,
) {
    if (cards.isEmpty()) return

    val profilesByCardId = cards.associate { card ->
        card.id to checkNotNull(variantProfilesById[card.variantProfileId]) {
            "Profil de variante inconnu '${card.variantProfileId}' pour '${card.id}'."
        }
    }
    val profiles = profilesByCardId.values.toList()

    commonSkyQualities(profiles).forEach { skyQuality ->
        if (
            cards.all { card ->
                val profile = checkNotNull(profilesByCardId[card.id])
                cardMatches(collection = collection, card = card, profile = profile) { ownedVariant ->
                    profile.isAtLeastSkyQuality(
                        candidateCode = ownedVariant.skyQuality,
                        thresholdCode = skyQuality.code,
                    )
                }
            }
        ) {
            add("${extension.id}::sky::${skyQuality.code}")
        }
    }

    if (
        supportsStampedBadges(profiles) &&
        cards.all { card ->
            val profile = checkNotNull(profilesByCardId[card.id])
            cardMatches(collection = collection, card = card, profile = profile) { ownedVariant ->
                profile.isStampedFinish(ownedVariant.finish)
            }
        }
    ) {
        add("${extension.id}::finish::stamped")
    }

    if (
        supportsHolographicStampedBadges(profiles) &&
        cards.all { card ->
            val profile = checkNotNull(profilesByCardId[card.id])
            cardMatches(collection = collection, card = card, profile = profile) { ownedVariant ->
                profile.isStampedFinish(ownedVariant.finish) &&
                    profile.isAtLeastSkyQuality(
                        candidateCode = ownedVariant.skyQuality,
                        thresholdCode = "holographic",
                    )
            }
        }
    ) {
        add("${extension.id}::finish::holographic-stamped")
    }

    if (
        supportsPerfectCollectionBadge(profiles) &&
        cards.all { card ->
            val profile = checkNotNull(profilesByCardId[card.id])
            cardHasEveryVariation(
                collection = collection,
                card = card,
                profile = profile,
            )
        }
    ) {
        add("${extension.id}::collection::perfect")
    }
}

private fun cardMatches(
    collection: OwnedCollection,
    card: CardDefinition,
    profile: VariantProfile,
    predicate: (OwnedVariantCount) -> Boolean,
): Boolean {
    val variants = collection.cards[card.id]
        ?.normalized()
        ?.variants
        .orEmpty()

    return variants.any { variant ->
        variant.count > 0 && profile.supportsVariant(variant) && predicate(variant)
    }
}

private fun cardHasEveryVariation(
    collection: OwnedCollection,
    card: CardDefinition,
    profile: VariantProfile,
): Boolean {
    val ownedKeys = collection.cards[card.id]
        ?.normalized()
        ?.variants
        .orEmpty()
        .filter { variant -> variant.count > 0 && profile.supportsVariant(variant) }
        .mapTo(mutableSetOf()) { variant -> "${variant.skyQuality}::${variant.finish}" }
    val requiredKeys = profile.skyQualities.flatMap { skyQuality ->
        profile.finishes.map { finish ->
            "${skyQuality.code}::${finish.code}"
        }
    }

    return requiredKeys.all(ownedKeys::contains)
}

private fun commonSkyQualities(profiles: List<VariantProfile>): List<SkyQualityDefinition> {
    if (profiles.isEmpty()) return emptyList()
    val referenceProfile = profiles.first()
    return referenceProfile.skyQualities.filter { candidate ->
        profiles.all { profile ->
            profile.skyQualities.any { it.code == candidate.code }
        }
    }
}

private fun supportsStampedBadges(profiles: List<VariantProfile>): Boolean =
    profiles.isNotEmpty() && profiles.all { profile ->
        profile.finishes.any { it.isStamped }
    }

private fun supportsHolographicStampedBadges(profiles: List<VariantProfile>): Boolean =
    supportsStampedBadges(profiles) && profiles.all { profile ->
        profile.skyQualities.any { it.code == "holographic" }
    }

private fun supportsPerfectCollectionBadge(profiles: List<VariantProfile>): Boolean =
    profiles.isNotEmpty() && profiles.all { profile ->
        profile.skyQualities.size * profile.finishes.size == 10
    }

private fun VariantProfile.supportsVariant(variant: OwnedVariantCount): Boolean =
    skyQualities.any { it.code == variant.skyQuality } &&
        finishes.any { it.code == variant.finish }

private fun VariantProfile.isStampedFinish(finishCode: String): Boolean =
    finishes.firstOrNull { it.code == finishCode }?.isStamped == true

private fun VariantProfile.isAtLeastSkyQuality(
    candidateCode: String,
    thresholdCode: String,
): Boolean {
    val candidateIndex = skyQualities.indexOfFirst { it.code == candidateCode }
    val thresholdIndex = skyQualities.indexOfFirst { it.code == thresholdCode }
    if (candidateIndex < 0 || thresholdIndex < 0) return false
    return candidateIndex >= thresholdIndex
}

private const val GeneralBadgeSectionId: String = "general"
