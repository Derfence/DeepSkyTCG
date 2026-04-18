package fr.aumombelli.dstcg.data

import fr.aumombelli.dstcg.feature.badges.buildNewlyUnlockedBadgeIds
import fr.aumombelli.dstcg.model.StandaloneProgress

class HomeMenuNoveltyEvaluator(
    private val catalogRepository: CatalogGateway,
) {
    suspend fun hasNewBadgeUnlock(
        beforeProgress: StandaloneProgress,
        afterProgress: StandaloneProgress,
    ): Boolean = buildNewlyUnlockedBadgeIds(
        extensions = catalogRepository.loadExtensions(),
        cards = catalogRepository.loadCards(),
        equipmentCards = catalogRepository.loadEquipmentCards(),
        variantProfiles = catalogRepository.loadVariantProfiles(),
        beforeProgress = beforeProgress,
        afterProgress = afterProgress,
    ).isNotEmpty()
}
