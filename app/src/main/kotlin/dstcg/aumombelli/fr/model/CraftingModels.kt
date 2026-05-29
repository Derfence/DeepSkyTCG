package fr.aumombelli.dstcg.model

enum class CraftingMode {
    DarkenSky,
    SpaceAgency,
}

data class CraftingCardRef(
    val cardId: String,
    val skyQuality: String,
    val finish: String,
) {
    val variantKey: String get() = "$skyQuality::$finish"
}

data class CraftingRecipe(
    val mode: CraftingMode,
    val source: CraftingCardRef,
    val target: CraftingCardRef,
    val consumedCount: Int,
)

data class CraftingCardCandidate(
    val card: CardDefinition,
    val extensionName: String,
    val mode: CraftingMode,
    val sourceVariant: DisplayCardVariant,
    val targetVariant: DisplayCardVariant,
    val consumedCount: Int,
) {
    val sourceRef: CraftingCardRef = CraftingCardRef(
        cardId = card.id,
        skyQuality = sourceVariant.skyQuality,
        finish = sourceVariant.finish,
    )

    val targetRef: CraftingCardRef = CraftingCardRef(
        cardId = card.id,
        skyQuality = targetVariant.skyQuality,
        finish = targetVariant.finish,
    )
}

sealed interface CraftingApplyResult {
    data class Success(
        val recipe: CraftingRecipe,
    ) : CraftingApplyResult

    data class Invalid(
        val message: String,
    ) : CraftingApplyResult
}
