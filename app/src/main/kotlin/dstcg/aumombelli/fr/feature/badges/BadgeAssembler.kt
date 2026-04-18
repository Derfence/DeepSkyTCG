package fr.aumombelli.dstcg.feature.badges

import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.OwnedVariantCount
import fr.aumombelli.dstcg.model.SkyQualityDefinition
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.model.VariantProfile
import fr.aumombelli.dstcg.model.normalized

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

                        if (supportsStampedBadges(profiles)) {
                            add(
                                buildStampedBadge(
                                    extension = extension,
                                    cards = extensionCards,
                                    collection = collection,
                                    profilesByCardId = profilesByCardId,
                                ),
                            )
                        }

                        if (supportsHolographicStampedBadges(profiles)) {
                            add(
                                buildHolographicStampedBadge(
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

private fun buildStampedBadge(
    extension: ExtensionDefinition,
    cards: List<CardDefinition>,
    collection: OwnedCollection,
    profilesByCardId: Map<String, VariantProfile>,
): BadgeItem {
    val progress = BadgeProgress(
        matchedCards = cards.count { card ->
            val profile = checkNotNull(profilesByCardId[card.id])
            cardMatches(collection = collection, card = card, profile = profile) { ownedVariant ->
                profile.isStampedFinish(ownedVariant.finish)
            }
        },
        totalCards = cards.size,
    )

    return BadgeItem(
        id = "${extension.id}::finish::stamped",
        extensionId = extension.id,
        extensionName = extension.name,
        title = "Tamponnee",
        description = "Obtiens chaque carte de ${extension.name} en version tamponnee, quelle que soit la qualite du ciel.",
        requirementType = BadgeRequirementType.Stamped,
        progress = progress,
    )
}

private fun buildHolographicStampedBadge(
    extension: ExtensionDefinition,
    cards: List<CardDefinition>,
    collection: OwnedCollection,
    profilesByCardId: Map<String, VariantProfile>,
): BadgeItem {
    val progress = BadgeProgress(
        matchedCards = cards.count { card ->
            val profile = checkNotNull(profilesByCardId[card.id])
            cardMatches(collection = collection, card = card, profile = profile) { ownedVariant ->
                profile.isStampedFinish(ownedVariant.finish) &&
                    profile.isAtLeastSkyQuality(
                        candidateCode = ownedVariant.skyQuality,
                        thresholdCode = "holographic",
                    )
            }
        },
        totalCards = cards.size,
    )

    return BadgeItem(
        id = "${extension.id}::finish::holographic-stamped",
        extensionId = extension.id,
        extensionName = extension.name,
        title = "Holo tamponnee",
        description = "Obtiens chaque carte de ${extension.name} en qualite holographique et en finition tamponnee.",
        requirementType = BadgeRequirementType.HolographicStamped,
        progress = progress,
        skyQualityCode = "holographic",
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
        description = "Obtiens les dix variations de chaque carte de ${extension.name} : les cinq qualites du ciel en standard et en tamponnee.",
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

internal const val GeneralBadgeSectionId: String = "general"
