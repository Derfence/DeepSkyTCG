package fr.aumombelli.gatcha.feature.badges

import fr.aumombelli.gatcha.model.CardDefinition
import fr.aumombelli.gatcha.model.ExtensionDefinition
import fr.aumombelli.gatcha.model.OwnedCollection
import fr.aumombelli.gatcha.model.OwnedVariantCount
import fr.aumombelli.gatcha.model.SkyQualityDefinition
import fr.aumombelli.gatcha.model.StandaloneProgress
import fr.aumombelli.gatcha.model.VariantProfile
import fr.aumombelli.gatcha.model.normalized

internal fun buildBadgeBookSections(
    extensions: List<ExtensionDefinition>,
    cards: List<CardDefinition>,
    variantProfiles: List<VariantProfile>,
    progress: StandaloneProgress,
): List<BadgeSection> {
    val variantProfilesById = variantProfiles.associateBy { it.id }
    val collection = progress.collection

    return buildList {
        add(buildGeneralSection(progress))
        addAll(
            extensions.map { extension ->
                val extensionCards = cards
                    .filter { it.extensionId == extension.id }
                    .sortedBy { it.id }
                val profilesByCardId = extensionCards.associate { card ->
                    card.id to checkNotNull(variantProfilesById[card.variantProfileId]) {
                        "Profil de variante inconnu '${card.variantProfileId}' pour '${card.id}'."
                    }
                }
                val profiles = profilesByCardId.values.toList()

                BadgeSection(
                    extensionId = extension.id,
                    extensionName = extension.name,
                    sectionType = BadgeSectionType.Extension,
                    badges = buildList {
                        commonSkyQualities(profiles).forEach { skyQuality ->
                            add(
                                buildSkyQualityBadge(
                                    extension = extension,
                                    cards = extensionCards,
                                    collection = collection,
                                    profilesByCardId = profilesByCardId,
                                    skyQuality = skyQuality,
                                ),
                            )
                        }

                        if (supportsHolographicBadges(profiles)) {
                            add(
                                buildHolographicBadge(
                                    extension = extension,
                                    cards = extensionCards,
                                    collection = collection,
                                    profilesByCardId = profilesByCardId,
                                ),
                            )
                        }

                        if (supportsMountainHolographicBadges(profiles)) {
                            add(
                                buildMountainHolographicBadge(
                                    extension = extension,
                                    cards = extensionCards,
                                    collection = collection,
                                    profilesByCardId = profilesByCardId,
                                ),
                            )
                        }

                        if (supportsPerfectCollectionBadge(profiles)) {
                            add(
                                buildPerfectCollectionBadge(
                                    extension = extension,
                                    cards = extensionCards,
                                    collection = collection,
                                    profilesByCardId = profilesByCardId,
                                ),
                            )
                        }
                    },
                )
            },
        )
    }
}

private fun buildGeneralSection(progress: StandaloneProgress): BadgeSection = BadgeSection(
    extensionId = GeneralBadgeSectionId,
    extensionName = "Général",
    sectionType = BadgeSectionType.General,
    badges = listOf(buildFirstPackOpenedBadge(progress)),
)

private fun buildFirstPackOpenedBadge(progress: StandaloneProgress): BadgeItem {
    val openedPackProgress = progress.openedPackCount.coerceIn(0, 1)
    return BadgeItem(
        id = "$GeneralBadgeSectionId::pack::first-opened",
        extensionId = GeneralBadgeSectionId,
        extensionName = "Général",
        title = "Premier pack ouvert",
        description = "Ouvre ton premier pack pour lancer ta collection et debloquer ce badge général.",
        requirementType = BadgeRequirementType.FirstPackOpened,
        progress = BadgeProgress(
            matchedCards = openedPackProgress,
            totalCards = 1,
            unitLabel = "pack ouvert",
        ),
    )
}

private fun buildSkyQualityBadge(
    extension: ExtensionDefinition,
    cards: List<CardDefinition>,
    collection: OwnedCollection,
    profilesByCardId: Map<String, VariantProfile>,
    skyQuality: SkyQualityDefinition,
): BadgeItem {
    val progress = BadgeProgress(
        matchedCards = cards.count { card ->
            val profile = checkNotNull(profilesByCardId[card.id])
            cardMatches(collection = collection, card = card, profile = profile) { ownedVariant ->
                profile.isAtLeastSkyQuality(
                    candidateCode = ownedVariant.skyQuality,
                    thresholdCode = skyQuality.code,
                )
            }
        },
        totalCards = cards.size,
    )

    return BadgeItem(
        id = "${extension.id}::sky::${skyQuality.code}",
        extensionId = extension.id,
        extensionName = extension.name,
        title = skyQuality.label,
        description = "Obtiens chaque carte de ${extension.name} avec une qualite du ciel au moins ${skyQuality.label}. Une meilleure qualite compte aussi.",
        requirementType = BadgeRequirementType.SkyQuality,
        progress = progress,
        skyQualityCode = skyQuality.code,
    )
}

private fun buildHolographicBadge(
    extension: ExtensionDefinition,
    cards: List<CardDefinition>,
    collection: OwnedCollection,
    profilesByCardId: Map<String, VariantProfile>,
): BadgeItem {
    val progress = BadgeProgress(
        matchedCards = cards.count { card ->
            val profile = checkNotNull(profilesByCardId[card.id])
            cardMatches(collection = collection, card = card, profile = profile) { ownedVariant ->
                profile.isHolographicFinish(ownedVariant.finish)
            }
        },
        totalCards = cards.size,
    )

    return BadgeItem(
        id = "${extension.id}::finish::holographic",
        extensionId = extension.id,
        extensionName = extension.name,
        title = "Holographique",
        description = "Obtiens chaque carte de ${extension.name} en holographique, quelle que soit la qualite du ciel.",
        requirementType = BadgeRequirementType.Holographic,
        progress = progress,
    )
}

private fun buildMountainHolographicBadge(
    extension: ExtensionDefinition,
    cards: List<CardDefinition>,
    collection: OwnedCollection,
    profilesByCardId: Map<String, VariantProfile>,
): BadgeItem {
    val progress = BadgeProgress(
        matchedCards = cards.count { card ->
            val profile = checkNotNull(profilesByCardId[card.id])
            cardMatches(collection = collection, card = card, profile = profile) { ownedVariant ->
                profile.isHolographicFinish(ownedVariant.finish) &&
                    profile.isAtLeastSkyQuality(
                        candidateCode = ownedVariant.skyQuality,
                        thresholdCode = "mountain",
                    )
            }
        },
        totalCards = cards.size,
    )

    return BadgeItem(
        id = "${extension.id}::finish::mountain-holographic",
        extensionId = extension.id,
        extensionName = extension.name,
        title = "Montagne holo",
        description = "Obtiens chaque carte de ${extension.name} en holographique avec une qualite du ciel Montagne.",
        requirementType = BadgeRequirementType.MountainHolographic,
        progress = progress,
        skyQualityCode = "mountain",
    )
}

private fun buildPerfectCollectionBadge(
    extension: ExtensionDefinition,
    cards: List<CardDefinition>,
    collection: OwnedCollection,
    profilesByCardId: Map<String, VariantProfile>,
): BadgeItem {
    val progress = BadgeProgress(
        matchedCards = cards.count { card ->
            val profile = checkNotNull(profilesByCardId[card.id])
            cardHasEveryVariation(
                collection = collection,
                card = card,
                profile = profile,
            )
        },
        totalCards = cards.size,
    )

    return BadgeItem(
        id = "${extension.id}::collection::perfect",
        extensionId = extension.id,
        extensionName = extension.name,
        title = "Collection parfaite",
        description = "Obtiens les huit variations de chaque carte de ${extension.name} : les quatre qualites du ciel en standard et en holographique.",
        requirementType = BadgeRequirementType.PerfectCollection,
        progress = progress,
    )
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

private fun supportsHolographicBadges(profiles: List<VariantProfile>): Boolean =
    profiles.isNotEmpty() && profiles.all { profile ->
        profile.finishes.any { it.isHolographic }
    }

private fun supportsMountainHolographicBadges(profiles: List<VariantProfile>): Boolean =
    supportsHolographicBadges(profiles) && profiles.all { profile ->
        profile.skyQualities.any { it.code == "mountain" }
    }

private fun supportsPerfectCollectionBadge(profiles: List<VariantProfile>): Boolean =
    profiles.isNotEmpty() && profiles.all { profile ->
        profile.skyQualities.size * profile.finishes.size == 8
    }

private fun VariantProfile.supportsVariant(variant: OwnedVariantCount): Boolean =
    skyQualities.any { it.code == variant.skyQuality } &&
        finishes.any { it.code == variant.finish }

private fun VariantProfile.isHolographicFinish(finishCode: String): Boolean =
    finishes.firstOrNull { it.code == finishCode }?.isHolographic == true

private fun VariantProfile.isAtLeastSkyQuality(
    candidateCode: String,
    thresholdCode: String,
): Boolean {
    val candidateIndex = skyQualities.indexOfFirst { it.code == candidateCode }
    val thresholdIndex = skyQualities.indexOfFirst { it.code == thresholdCode }
    if (candidateIndex < 0 || thresholdIndex < 0) return false
    return candidateIndex >= thresholdIndex
}

internal const val GeneralBadgeSectionId: String = "general"
