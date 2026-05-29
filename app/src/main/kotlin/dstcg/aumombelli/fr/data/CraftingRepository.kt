package fr.aumombelli.dstcg.data

import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.CraftingApplyResult
import fr.aumombelli.dstcg.model.CraftingCardCandidate
import fr.aumombelli.dstcg.model.CraftingCardRef
import fr.aumombelli.dstcg.model.CraftingMode
import fr.aumombelli.dstcg.model.CraftingRecipeValidation
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.VariantProfile
import fr.aumombelli.dstcg.model.applyCraftingRecipe
import fr.aumombelli.dstcg.model.buildCraftingCandidates
import fr.aumombelli.dstcg.model.resolvedSkyUpgradeCosts
import fr.aumombelli.dstcg.model.validateCraftingRecipe

class CraftingRepository(
    private val catalogRepository: CatalogGateway,
    private val progressRepository: ProgressGateway,
) : CraftingGateway {
    override suspend fun loadCraftingCandidates(mode: CraftingMode): List<CraftingCardCandidate> {
        val context = loadCraftingCatalogContext()
        val progress = progressRepository.loadProgress().requireUsableProgress().progress
        return buildCraftingCandidates(
            mode = mode,
            collection = progress.collection,
            cardsById = context.cardsById,
            extensionsById = context.extensionsById,
            variantProfilesById = context.variantProfilesById,
            skyUpgradeCosts = context.skyUpgradeCosts,
        )
    }

    override suspend fun hasDarkenSkyCandidates(): Boolean =
        loadCraftingCandidates(CraftingMode.DarkenSky).isNotEmpty()

    override suspend fun applyCrafting(
        mode: CraftingMode,
        source: CraftingCardRef,
    ): CraftingApplyResult {
        val context = loadCraftingCatalogContext()
        val currentProgress = progressRepository.loadProgress().requireUsableProgress().progress
        val preflight = validateCraftingRecipe(
            mode = mode,
            collection = currentProgress.collection,
            source = source,
            cardsById = context.cardsById,
            variantProfilesById = context.variantProfilesById,
            skyUpgradeCosts = context.skyUpgradeCosts,
        )
        if (preflight is CraftingRecipeValidation.Invalid) {
            return CraftingApplyResult.Invalid(preflight.message)
        }

        var result: CraftingApplyResult = CraftingApplyResult.Invalid("Fabrication non appliquee.")
        progressRepository.updateProgress { progress ->
            when (
                val validation = validateCraftingRecipe(
                    mode = mode,
                    collection = progress.collection,
                    source = source,
                    cardsById = context.cardsById,
                    variantProfilesById = context.variantProfilesById,
                    skyUpgradeCosts = context.skyUpgradeCosts,
                )
            ) {
                is CraftingRecipeValidation.Invalid -> {
                    result = CraftingApplyResult.Invalid(validation.message)
                    progress
                }

                is CraftingRecipeValidation.Valid -> {
                    result = CraftingApplyResult.Success(validation.recipe)
                    progress.copy(
                        collection = progress.collection.applyCraftingRecipe(validation.recipe),
                    )
                }
            }
        }
        return result
    }

    private suspend fun loadCraftingCatalogContext(): CraftingCatalogContext {
        val gameBalance = catalogRepository.loadGameBalance().validated()
        return CraftingCatalogContext(
            cardsById = catalogRepository.loadCards().associateBy(CardDefinition::id),
            extensionsById = catalogRepository.loadExtensions().associateBy(ExtensionDefinition::id),
            variantProfilesById = catalogRepository.loadVariantProfiles().associateBy(VariantProfile::id),
            skyUpgradeCosts = gameBalance.resolvedSkyUpgradeCosts(),
        )
    }
}

private data class CraftingCatalogContext(
    val cardsById: Map<String, CardDefinition>,
    val extensionsById: Map<String, ExtensionDefinition>,
    val variantProfilesById: Map<String, VariantProfile>,
    val skyUpgradeCosts: Map<String, Int>,
)
