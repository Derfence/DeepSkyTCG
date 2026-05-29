package fr.aumombelli.dstcg.model

const val StampedCraftingCost: Int = 10

fun buildCraftingCandidates(
    mode: CraftingMode,
    collection: OwnedCollection,
    cardsById: Map<String, CardDefinition>,
    extensionsById: Map<String, ExtensionDefinition>,
    variantProfilesById: Map<String, VariantProfile>,
    skyUpgradeCosts: Map<String, Int>,
): List<CraftingCardCandidate> =
    collection.normalized().cards.flatMap { (cardId, entry) ->
        val card = cardsById[cardId] ?: return@flatMap emptyList()
        val extension = extensionsById[card.extensionId] ?: return@flatMap emptyList()
        val profile = variantProfilesById[card.variantProfileId] ?: return@flatMap emptyList()
        entry.variants.mapNotNull { variant ->
            val sourceRef = CraftingCardRef(
                cardId = cardId,
                skyQuality = variant.skyQuality,
                finish = variant.finish,
            )
            val recipe = validateCraftingRecipe(
                mode = mode,
                collection = collection,
                source = sourceRef,
                card = card,
                variantProfile = profile,
                skyUpgradeCosts = skyUpgradeCosts,
            ).getOrNull() ?: return@mapNotNull null
            CraftingCardCandidate(
                card = card,
                extensionName = extension.name,
                mode = mode,
                sourceVariant = profile.toDisplayVariant(
                    skyQuality = recipe.source.skyQuality,
                    finish = recipe.source.finish,
                    count = variant.count,
                ),
                targetVariant = profile.toDisplayVariant(
                    skyQuality = recipe.target.skyQuality,
                    finish = recipe.target.finish,
                    count = collection.craftingCountFor(recipe.target),
                ),
                consumedCount = recipe.consumedCount,
            )
        }
    }.sortedWith(
        compareBy<CraftingCardCandidate> { raritySortPriority(it.card.rarityLabel) }
            .thenBy { it.card.name }
            .thenByDescending { skyQualitySortPriority(it.sourceVariant.skyQuality) }
            .thenBy { it.sourceVariant.finishLabel },
    )

fun validateCraftingRecipe(
    mode: CraftingMode,
    collection: OwnedCollection,
    source: CraftingCardRef,
    cardsById: Map<String, CardDefinition>,
    variantProfilesById: Map<String, VariantProfile>,
    skyUpgradeCosts: Map<String, Int>,
): CraftingRecipeValidation {
    val card = cardsById[source.cardId]
        ?: return CraftingRecipeValidation.Invalid("Carte inconnue.")
    val profile = variantProfilesById[card.variantProfileId]
        ?: return CraftingRecipeValidation.Invalid("Profil de variante inconnu.")
    return validateCraftingRecipe(
        mode = mode,
        collection = collection,
        source = source,
        card = card,
        variantProfile = profile,
        skyUpgradeCosts = skyUpgradeCosts,
    )
}

fun validateCraftingRecipe(
    mode: CraftingMode,
    collection: OwnedCollection,
    source: CraftingCardRef,
    card: CardDefinition,
    variantProfile: VariantProfile,
    skyUpgradeCosts: Map<String, Int>,
): CraftingRecipeValidation {
    if (card.id != source.cardId) {
        return CraftingRecipeValidation.Invalid("La carte source ne correspond pas au catalogue.")
    }
    val sourceSky = variantProfile.skyQualities.firstOrNull { it.code == source.skyQuality }
        ?: return CraftingRecipeValidation.Invalid("Qualite de ciel inconnue.")
    val sourceFinish = variantProfile.finishes.firstOrNull { it.code == source.finish }
        ?: return CraftingRecipeValidation.Invalid("Finition inconnue.")
    val sourceCount = collection.craftingCountFor(source)

    val recipe = when (mode) {
        CraftingMode.DarkenSky -> {
            val nextSky = variantProfile.nextSkyQualityAfter(sourceSky.code)
                ?: return CraftingRecipeValidation.Invalid("Cette qualite de ciel est deja au maximum.")
            val standardFinish = variantProfile.standardFinish()
                ?: return CraftingRecipeValidation.Invalid("Aucune finition standard n'est configuree.")
            val cost = skyUpgradeCosts[sourceSky.code]
                ?: return CraftingRecipeValidation.Invalid("Aucun cout d'amelioration n'est configure.")
            if (cost <= 0) {
                return CraftingRecipeValidation.Invalid("Le cout d'amelioration doit etre positif.")
            }
            CraftingRecipe(
                mode = mode,
                source = source,
                target = CraftingCardRef(
                    cardId = source.cardId,
                    skyQuality = nextSky.code,
                    finish = standardFinish.code,
                ),
                consumedCount = cost,
            )
        }

        CraftingMode.SpaceAgency -> {
            val standardFinish = variantProfile.standardFinish()
                ?: return CraftingRecipeValidation.Invalid("Aucune finition standard n'est configuree.")
            val stampedFinish = variantProfile.stampedFinish()
                ?: return CraftingRecipeValidation.Invalid("Aucune finition tamponnee n'est configuree.")
            if (sourceFinish.code != standardFinish.code || sourceFinish.isStamped) {
                return CraftingRecipeValidation.Invalid("Seules les cartes standard peuvent etre tamponnees.")
            }
            CraftingRecipe(
                mode = mode,
                source = source,
                target = CraftingCardRef(
                    cardId = source.cardId,
                    skyQuality = sourceSky.code,
                    finish = stampedFinish.code,
                ),
                consumedCount = StampedCraftingCost,
            )
        }
    }

    if (sourceCount < recipe.consumedCount) {
        return CraftingRecipeValidation.Invalid("Pas assez de copies pour cette fabrication.")
    }
    return CraftingRecipeValidation.Valid(recipe)
}

fun OwnedCollection.applyCraftingRecipe(recipe: CraftingRecipe): OwnedCollection =
    decrementCraftingVariant(recipe.source, recipe.consumedCount)
        .incrementCraftingVariant(recipe.target)
        .normalized()

fun OwnedCollection.craftingCountFor(ref: CraftingCardRef): Int =
    cards[ref.cardId]
        ?.normalized()
        ?.variants
        ?.firstOrNull { it.skyQuality == ref.skyQuality && it.finish == ref.finish }
        ?.count
        ?: 0

sealed interface CraftingRecipeValidation {
    data class Valid(
        val recipe: CraftingRecipe,
    ) : CraftingRecipeValidation

    data class Invalid(
        val message: String,
    ) : CraftingRecipeValidation
}

fun CraftingRecipeValidation.getOrNull(): CraftingRecipe? = when (this) {
    is CraftingRecipeValidation.Valid -> recipe
    is CraftingRecipeValidation.Invalid -> null
}

private fun VariantProfile.nextSkyQualityAfter(code: String): SkyQualityDefinition? {
    val index = skyQualities.indexOfFirst { it.code == code }
    if (index < 0 || index >= skyQualities.lastIndex) {
        return null
    }
    return skyQualities[index + 1]
}

private fun VariantProfile.standardFinish(): CardFinishDefinition? =
    finishes.firstOrNull { !it.isStamped }

private fun VariantProfile.stampedFinish(): CardFinishDefinition? =
    finishes.firstOrNull { it.isStamped }

private fun OwnedCollection.decrementCraftingVariant(
    ref: CraftingCardRef,
    amount: Int,
): OwnedCollection {
    if (amount <= 0) return this
    val updatedCards = cards.toMutableMap()
    val entry = updatedCards[ref.cardId]?.normalized() ?: return this
    val variants = entry.variants.mapNotNull { variant ->
        if (variant.skyQuality == ref.skyQuality && variant.finish == ref.finish) {
            val nextCount = variant.count - amount
            if (nextCount > 0) variant.copy(count = nextCount) else null
        } else {
            variant
        }
    }
    if (variants.isEmpty()) {
        updatedCards.remove(ref.cardId)
    } else {
        updatedCards[ref.cardId] = OwnedCardEntry(variants = variants).normalized()
    }
    return copy(cards = updatedCards.toSortedMap()).normalized()
}

private fun OwnedCollection.incrementCraftingVariant(ref: CraftingCardRef): OwnedCollection {
    val updatedCards = cards.toMutableMap()
    val entry = updatedCards[ref.cardId]?.normalized() ?: OwnedCardEntry()
    val variants = entry.variants.toMutableList()
    val existingIndex = variants.indexOfFirst {
        it.skyQuality == ref.skyQuality && it.finish == ref.finish
    }
    if (existingIndex >= 0) {
        val variant = variants[existingIndex]
        variants[existingIndex] = variant.copy(count = variant.count + 1)
    } else {
        variants += OwnedVariantCount(
            skyQuality = ref.skyQuality,
            finish = ref.finish,
            count = 1,
        )
    }
    updatedCards[ref.cardId] = OwnedCardEntry(variants = variants).normalized()
    return copy(cards = updatedCards.toSortedMap()).normalized()
}
