package fr.aumombelli.gatcha.model

fun VariantProfile.requireSkyQualityDefinition(code: String): SkyQualityDefinition =
    checkNotNull(skyQualities.firstOrNull { it.code == code }) {
        "Qualite de ciel inconnue '$code' pour le profil de variante '$id'."
    }

fun VariantProfile.requireFinishDefinition(code: String): CardFinishDefinition =
    checkNotNull(finishes.firstOrNull { it.code == code }) {
        "Finition inconnue '$code' pour le profil de variante '$id'."
    }

fun VariantProfile.toDisplayVariant(
    skyQuality: String,
    finish: String,
    count: Int = 0,
): DisplayCardVariant {
    val skyQualityDefinition = requireSkyQualityDefinition(skyQuality)
    val finishDefinition = requireFinishDefinition(finish)
    return DisplayCardVariant(
        skyQuality = skyQualityDefinition.code,
        skyQualityLabel = skyQualityDefinition.label,
        finish = finishDefinition.code,
        finishLabel = finishDefinition.label,
        isHolographic = finishDefinition.isHolographic,
        count = count,
    )
}

fun CardVariant.toDisplayVariant(count: Int = 0): DisplayCardVariant =
    DisplayCardVariant(
        skyQuality = skyQuality,
        skyQualityLabel = skyQualityLabel,
        finish = finish,
        finishLabel = finishLabel,
        isHolographic = isHolographic,
        count = count,
    )

fun CardDefinition.toDisplayCard(
    extensionName: String,
    activeVariant: DisplayCardVariant,
    availableVariants: List<DisplayCardVariant> = listOf(activeVariant),
): DisplayCard =
    DisplayCard(
        definition = this,
        extensionName = extensionName,
        activeVariant = activeVariant,
        availableVariants = availableVariants,
    )

fun LibraryCardItem.toDisplayCard(selectedVariantKey: String? = null): DisplayCard? {
    val activeVariant = availableVariants.firstOrNull { it.key == selectedVariantKey }
        ?: availableVariants.firstOrNull()
        ?: return null
    return definition.toDisplayCard(
        extensionName = extensionName,
        activeVariant = activeVariant,
        availableVariants = availableVariants,
    )
}

fun DisplayCard.withSelectedVariant(selectedVariantKey: String?): DisplayCard {
    val selectedVariant = availableVariants.firstOrNull { it.key == selectedVariantKey }
        ?: availableVariants.firstOrNull()
        ?: activeVariant
    return copy(activeVariant = selectedVariant)
}

fun OwnedCardEntry.toDisplayVariants(variantProfile: VariantProfile): List<DisplayCardVariant> =
    normalized().variants
        .map { variantCount ->
            variantProfile.toDisplayVariant(
                skyQuality = variantCount.skyQuality,
                finish = variantCount.finish,
                count = variantCount.count,
            )
        }
        .sortedWith(
            compareByDescending<DisplayCardVariant> { it.isHolographic }
                .thenByDescending { skyQualityRank(it.skyQuality) }
                .thenByDescending { it.count }
                .thenBy { it.skyQualityLabel }
                .thenBy { it.finishLabel },
        )

private fun skyQualityRank(code: String): Int = when (code) {
    "mountain" -> 4
    "rural" -> 3
    "suburban" -> 2
    "city" -> 1
    else -> 0
}
