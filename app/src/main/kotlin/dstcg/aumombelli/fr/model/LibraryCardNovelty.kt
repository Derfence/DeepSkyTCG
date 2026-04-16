package fr.aumombelli.dstcg.model

import kotlinx.serialization.Serializable

@Serializable
data class LibraryCardNoveltyState(
    val newCardIds: Set<String> = emptySet(),
)

fun buildNewLibraryCardIds(
    beforeProgress: StandaloneProgress,
    afterProgress: StandaloneProgress,
): Set<String> = afterProgress.collection.cards.keys.filterTo(mutableSetOf()) { cardId ->
    beforeProgress.collection.ownedCountFor(cardId) == 0 &&
        afterProgress.collection.ownedCountFor(cardId) > 0
}

fun hasNewLibraryCard(
    beforeProgress: StandaloneProgress,
    afterProgress: StandaloneProgress,
): Boolean = buildNewLibraryCardIds(
    beforeProgress = beforeProgress,
    afterProgress = afterProgress,
).isNotEmpty()

fun StandaloneProgress.markLibraryNoveltyPresented(): StandaloneProgress {
    if (!homeMenuNoveltyState.library && libraryCardNoveltyState.newCardIds.isEmpty()) {
        return this
    }
    return copy(
        homeMenuNoveltyState = homeMenuNoveltyState.consume(HomeMenuDestination.Library),
        libraryCardNoveltyState = LibraryCardNoveltyState(),
    )
}
