package fr.aumombelli.dstcg.model

import kotlinx.serialization.Serializable

@Serializable
data class HomeMenuNoveltyState(
    val library: Boolean = false,
    val equipment: Boolean = false,
    val badgeBook: Boolean = false,
)

enum class HomeMenuDestination {
    Library,
    Equipment,
    BadgeBook,
}

fun HomeMenuNoveltyState.consume(destination: HomeMenuDestination): HomeMenuNoveltyState = when (destination) {
    HomeMenuDestination.Library -> copy(library = false)
    HomeMenuDestination.Equipment -> copy(equipment = false)
    HomeMenuDestination.BadgeBook -> copy(badgeBook = false)
}

fun HomeMenuNoveltyState.or(other: HomeMenuNoveltyState): HomeMenuNoveltyState = HomeMenuNoveltyState(
    library = library || other.library,
    equipment = equipment || other.equipment,
    badgeBook = badgeBook || other.badgeBook,
)

fun StandaloneProgress.markHomeMenuSeen(destination: HomeMenuDestination): StandaloneProgress = copy(
    homeMenuNoveltyState = homeMenuNoveltyState.consume(destination),
)

fun hasEquipmentStockCrossedZero(
    beforeProgress: StandaloneProgress,
    afterProgress: StandaloneProgress,
): Boolean = afterProgress.equipmentInventory.cards.keys.any { cardId ->
    beforeProgress.equipmentInventory.entryFor(cardId).countOwned == 0 &&
        afterProgress.equipmentInventory.entryFor(cardId).countOwned > 0
}
