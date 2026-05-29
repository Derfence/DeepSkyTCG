package fr.aumombelli.dstcg.model

data class EquipmentState(
    val inventory: OwnedEquipmentInventory = OwnedEquipmentInventory(),
    val activeEquipmentByType: Map<EquipmentType, ActiveEquipmentEffect> = emptyMap(),
    val lastActivatedCardIdByType: Map<EquipmentType, String> = emptyMap(),
)

fun OwnedEquipmentCardEntry.normalized(): OwnedEquipmentCardEntry = copy(
    countOwned = countOwned.coerceAtLeast(0),
    activationCount = activationCount.coerceAtLeast(0),
)

fun OwnedEquipmentInventory.normalized(): OwnedEquipmentInventory = copy(
    cards = cards.mapNotNull { (cardId, entry) ->
        val normalizedEntry = entry.normalized()
        if (normalizedEntry.countOwned <= 0 && normalizedEntry.activationCount <= 0) {
            null
        } else {
            cardId to normalizedEntry
        }
    }.toMap().toSortedMap(),
)

fun OwnedEquipmentInventory.entryFor(cardId: String): OwnedEquipmentCardEntry =
    cards[cardId]?.normalized() ?: OwnedEquipmentCardEntry()

fun OwnedEquipmentInventory.addReward(cardId: String): OwnedEquipmentInventory {
    val updated = cards.toMutableMap()
    val currentEntry = entryFor(cardId)
    updated[cardId] = currentEntry.copy(countOwned = currentEntry.countOwned + 1)
    return copy(cards = updated).normalized()
}

fun OwnedEquipmentInventory.addRewards(cards: List<EquipmentCardDefinition>): OwnedEquipmentInventory =
    cards.fold(this) { inventory, definition ->
        inventory.addReward(definition.id)
    }

fun OwnedEquipmentInventory.consumeForActivation(cardId: String): OwnedEquipmentInventory {
    val currentEntry = entryFor(cardId)
    require(currentEntry.countOwned > 0) {
        "Aucune carte d'equipement disponible pour '$cardId'."
    }
    val updated = cards.toMutableMap()
    updated[cardId] = currentEntry.copy(
        countOwned = currentEntry.countOwned - 1,
        activationCount = currentEntry.activationCount + 1,
    )
    return copy(cards = updated).normalized()
}

fun ActiveEquipmentEffect.normalized(): ActiveEquipmentEffect? {
    val normalizedRemaining = packsRemaining.coerceAtLeast(0)
    if (normalizedRemaining == 0) return null
    return copy(packsRemaining = normalizedRemaining)
}

fun StandaloneProgress.normalizedEquipmentState(): StandaloneProgress = copy(
    equipmentInventory = equipmentInventory.normalized(),
    activeEquipmentByType = activeEquipmentByType.mapNotNull { (type, effect) ->
        effect.normalized()?.let { normalizedEffect ->
            type to normalizedEffect
        }
    }.toMap(),
    lastActivatedCardIdByType = lastActivatedCardIdByType.toSortedMap(compareBy { it.code }),
    equipmentBadgeProgress = equipmentBadgeProgress.normalized(),
)

fun StandaloneProgress.consumeEquipmentEffectsAfterPackOpen(): StandaloneProgress = copy(
    activeEquipmentByType = activeEquipmentByType.mapNotNull { (type, effect) ->
        effect.copy(packsRemaining = effect.packsRemaining - 1)
            .normalized()
            ?.let { updatedEffect -> type to updatedEffect }
    }.toMap(),
).normalizedEquipmentState()

fun StandaloneProgress.toEquipmentState(): EquipmentState = EquipmentState(
    inventory = equipmentInventory.normalized(),
    activeEquipmentByType = activeEquipmentByType.mapNotNull { (type, effect) ->
        effect.normalized()?.let { normalizedEffect ->
            type to normalizedEffect
        }
    }.toMap(),
    lastActivatedCardIdByType = lastActivatedCardIdByType.toSortedMap(compareBy { it.code }),
)

fun StandaloneProgress.totalEquipmentActivationCount(): Int =
    equipmentInventory.cards.values.sumOf { entry -> entry.normalized().activationCount }

fun StandaloneProgress.activatedEquipmentCardCount(
    equipmentCards: List<EquipmentCardDefinition>,
): Int = equipmentCards.count { definition ->
    equipmentInventory.entryFor(definition.id).activationCount > 0
}

fun StandaloneProgress.recordEquipmentActivationSnapshot(
    equipmentCards: List<EquipmentCardDefinition>,
): StandaloneProgress {
    val equipmentCardsById = equipmentCards.associateBy(EquipmentCardDefinition::id)
    val simultaneouslyActiveTypeCount = activeEquipmentByType.count { (type, effect) ->
        equipmentCardsById[effect.equipmentCardId]?.type == type
    }
    val simultaneouslyActiveLevelThreeTypeCount = activeEquipmentByType.count { (type, effect) ->
        val definition = equipmentCardsById[effect.equipmentCardId] ?: return@count false
        definition.type == type && definition.level == 3
    }

    return copy(
        equipmentBadgeProgress = equipmentBadgeProgress.copy(
            maxSimultaneouslyActiveEquipmentTypeCount = maxOf(
                equipmentBadgeProgress.maxSimultaneouslyActiveEquipmentTypeCount,
                simultaneouslyActiveTypeCount,
            ),
            maxSimultaneouslyActiveLevelThreeEquipmentTypeCount = maxOf(
                equipmentBadgeProgress.maxSimultaneouslyActiveLevelThreeEquipmentTypeCount,
                simultaneouslyActiveLevelThreeTypeCount,
            ),
        ),
    ).normalizedEquipmentState()
}

fun StandaloneProgress.recordAffectedPackIfEquipmentActive(): StandaloneProgress = copy(
    equipmentBadgeProgress = equipmentBadgeProgress.copy(
        affectedPackCount = equipmentBadgeProgress.affectedPackCount + if (activeEquipmentByType.isNotEmpty()) {
            1
        } else {
            0
        },
    ),
).normalizedEquipmentState()

fun StandaloneProgress.hasUnlockedEquipmentMenu(): Boolean =
    equipmentInventory.cards.isNotEmpty() ||
        activeEquipmentByType.isNotEmpty() ||
        lastActivatedCardIdByType.isNotEmpty()
