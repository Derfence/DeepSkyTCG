package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.feature.packs.selection.PackEvent
import fr.aumombelli.dstcg.feature.packs.selection.buildLiveChargeStatus
import fr.aumombelli.dstcg.data.EntropySource
import fr.aumombelli.dstcg.data.StandaloneGameSettings
import fr.aumombelli.dstcg.model.DrawPackResponse
import fr.aumombelli.dstcg.model.ActiveEquipmentEffect
import fr.aumombelli.dstcg.model.EquipmentType
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.ui.viewmodel.ActiveEquipmentPackReminderUi
import fr.aumombelli.dstcg.ui.viewmodel.ExtensionCardProgress
import fr.aumombelli.dstcg.ui.viewmodel.PackViewModel
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PackViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    private val fixedNow = Instant.parse("2026-03-24T12:00:00Z")
    private val gameSettings = testGameSettings(now = fixedNow)

    @Test
    fun `select extension and clear selection reset transient booster state`() = runTest {
        val viewModel = PackViewModel(
            catalogRepository = FakeCatalogGateway(),
            progressRepository = FakeProgressGateway(),
            packRepository = FakePackGateway(),
            gameSettings = gameSettings,
        )
        advanceUntilIdle()

        viewModel.selectExtension("core-alpha")
        viewModel.selectBooster(3)
        viewModel.clearExtensionSelection()

        assertEquals(null, viewModel.uiState.value.selectedExtensionId)
        assertEquals(null, viewModel.uiState.value.selectedBoosterIndex)
        assertEquals(emptyList<Int>(), viewModel.uiState.value.boosterDecorSeeds)
        assertEquals(null, viewModel.uiState.value.epicBoostBoosterIndex)
        assertEquals(false, viewModel.uiState.value.isAwaitingPackResult)
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `refresh loads extensions collection and charge status from local progress`() = runTest {
        val catalogGateway = FakeCatalogGateway().apply {
            extensions = listOf(ExtensionDefinition("core-alpha", "Core Alpha", "cover"))
            cards = listOf(
                testCardDefinition("ALP-001", extensionId = "core-alpha"),
                testCardDefinition("ALP-002", extensionId = "core-alpha"),
            )
        }
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = ownedCollectionOf("ALP-001" to 1),
                rechargeState = testRechargeStateWithNextChargeAt(
                    availableDrawCount = 4,
                    nextChargeAt = "2026-03-24T18:00:00Z",
                ),
            )
        }

        val viewModel = PackViewModel(
            catalogRepository = catalogGateway,
            progressRepository = progressGateway,
            packRepository = FakePackGateway(),
            gameSettings = gameSettings,
        )
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isLoading)
        assertEquals(4, viewModel.uiState.value.availableDrawCount)
        assertEquals("2026-03-24T18:00:00Z", viewModel.uiState.value.nextChargeAt)
        assertEquals(Duration.ofHours(6), viewModel.uiState.value.remainingDuration)
        assertEquals(false, viewModel.uiState.value.isDrawLocked)
        assertEquals(1, viewModel.uiState.value.currentCollection.cards["ALP-001"]?.totalOwned)
        assertEquals(
            ExtensionCardProgress(obtainedCount = 1, totalCount = 2),
            viewModel.uiState.value.extensionCardProgress["core-alpha"],
        )
    }

    @Test
    fun `refresh keeps active observatory recharge multiplier in live charge status`() = runTest {
        val observatory = testEquipmentCardDefinition(
            id = "observatory-master",
            type = EquipmentType.Observatory,
            level = 3,
            bonusValue = 2.0,
        )
        val catalogGateway = FakeCatalogGateway().apply {
            extensions = listOf(ExtensionDefinition("core-alpha", "Core Alpha", "cover"))
            equipmentCards = listOf(observatory)
        }
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = ownedCollectionOf(),
                rechargeState = testRechargeStateWithNextChargeAt(
                    availableDrawCount = 0,
                    nextChargeAt = "2026-03-24T18:00:00Z",
                    now = fixedNow,
                ),
                activeEquipmentByType = mapOf(
                    EquipmentType.Observatory to ActiveEquipmentEffect(
                        equipmentCardId = observatory.id,
                        equipmentType = EquipmentType.Observatory,
                        packsRemaining = 3,
                    ),
                ),
            )
        }

        val viewModel = PackViewModel(
            catalogRepository = catalogGateway,
            progressRepository = progressGateway,
            packRepository = FakePackGateway(),
            gameSettings = gameSettings,
        )
        advanceUntilIdle()

        assertEquals(2.0, viewModel.uiState.value.rechargeMultiplier, 0.0001)
        assertEquals("2026-03-24T15:00:00Z", viewModel.uiState.value.nextChargeAt)
        assertEquals(
            "2026-03-24T15:00:00Z",
            viewModel.uiState.value.buildLiveChargeStatus(fixedNow).nextChargeAt,
        )
        assertEquals(
            listOf(
                ActiveEquipmentPackReminderUi(
                    type = EquipmentType.Observatory,
                    level = 3,
                    packsRemaining = 3,
                ),
            ),
            viewModel.uiState.value.activeEquipmentReminders,
        )
    }

    @Test
    fun `refresh immediately clears transient pack selection state`() = runTest {
        val viewModel = PackViewModel(
            catalogRepository = FakeCatalogGateway().apply {
                extensions = listOf(ExtensionDefinition("core-alpha", "Core Alpha", "cover"))
            },
            progressRepository = FakeProgressGateway(),
            packRepository = FakePackGateway(),
            gameSettings = gameSettings,
        )
        advanceUntilIdle()

        viewModel.selectExtension("core-alpha")
        viewModel.selectBooster(1)

        viewModel.refresh()

        assertEquals(true, viewModel.uiState.value.isLoading)
        assertEquals(null, viewModel.uiState.value.selectedExtensionId)
        assertEquals(null, viewModel.uiState.value.selectedBoosterIndex)
        assertEquals(emptyList<Int>(), viewModel.uiState.value.boosterDecorSeeds)
        assertEquals(null, viewModel.uiState.value.epicBoostBoosterIndex)
        assertEquals(false, viewModel.uiState.value.isAwaitingPackResult)
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `select extension assigns random decor seeds and selected booster exposes its seed`() = runTest {
        val viewModel = PackViewModel(
            catalogRepository = FakeCatalogGateway().apply {
                extensions = listOf(ExtensionDefinition("core-alpha", "Core Alpha", "cover"))
            },
            progressRepository = FakeProgressGateway(),
            packRepository = FakePackGateway(),
            gameSettings = queuedEpicBoostGameSettings(100_000),
            decorEntropySource = QueuedEntropySource(listOf(11, 22, 33, 44)),
        )
        advanceUntilIdle()

        viewModel.selectExtension("core-alpha")
        viewModel.selectBooster(2)

        assertEquals(listOf(11, 22, 33, 44), viewModel.uiState.value.boosterDecorSeeds)
        assertEquals(33, viewModel.uiState.value.selectedBoosterDecorSeed)
    }

    @Test
    fun `open pack reloads saved progress and emits navigation`() = runTest {
        val response = DrawPackResponse.fromCards(
            extensionId = "core-alpha",
            drawnAt = "2026-03-23T12:00:00Z",
            rechargeState = testRechargeStateWithNextChargeAt(
                availableDrawCount = 9,
                nextChargeAt = "2026-03-24T18:00:00Z",
            ),
            cards = listOf(
                testPackCard("ALP-001", "Nebuleuse d'Orion", "Common", "spark_fox"),
                testPackCard(
                    "ALP-002",
                    "Galaxie d'Andromede",
                    "Common",
                    "steam_golem",
                    skyQuality = "rural",
                    skyQualityLabel = "Campagne",
                ),
            ),
        )
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = ownedCollectionOf("ALP-001" to 1),
                rechargeState = testRechargeState(),
            )
        }
        val packGateway = FakePackGateway().apply {
            openPackResponse = response
            onOpenPack = { _, _ ->
                progressGateway.progress = StandaloneProgress(
                    collection = ownedCollectionOf("ALP-001" to 2, "ALP-002" to 1),
                    rechargeState = response.rechargeState,
                )
            }
        }
        val catalogGateway = FakeCatalogGateway().apply {
            extensions = listOf(ExtensionDefinition("core-alpha", "Core Alpha", "cover"))
            cards = listOf(
                testCardDefinition("ALP-001", extensionId = "core-alpha"),
                testCardDefinition("ALP-002", extensionId = "core-alpha"),
            )
        }

        val viewModel = PackViewModel(
            catalogRepository = catalogGateway,
            progressRepository = progressGateway,
            packRepository = packGateway,
            gameSettings = gameSettings,
        )
        advanceUntilIdle()

        val event = async { viewModel.events.first() }
        viewModel.selectExtension("core-alpha")
        viewModel.selectBooster(2)
        viewModel.openPack("core-alpha")
        advanceUntilIdle()

        assertEquals(PackEvent.PackReadyForReveal(), event.await())
        assertEquals(listOf("core-alpha" to false), packGateway.openPackCalls)
        assertEquals(2, viewModel.uiState.value.currentCollection.cards["ALP-001"]?.totalOwned)
        assertEquals(1, viewModel.uiState.value.currentCollection.cards["ALP-002"]?.totalOwned)
        assertEquals(
            ExtensionCardProgress(obtainedCount = 2, totalCount = 2),
            viewModel.uiState.value.extensionCardProgress["core-alpha"],
        )
        assertEquals(9, viewModel.uiState.value.availableDrawCount)
        assertEquals("2026-03-24T18:00:00Z", viewModel.uiState.value.nextChargeAt)
        assertEquals(Duration.ofHours(6), viewModel.uiState.value.remainingDuration)
        assertEquals("core-alpha", viewModel.uiState.value.selectedExtensionId)
        assertEquals(2, viewModel.uiState.value.selectedBoosterIndex)
        assertEquals(false, viewModel.uiState.value.isAwaitingPackResult)
    }

    @Test
    fun `open pack forwards Epic Boost when selected booster is boosted`() = runTest {
        val response = DrawPackResponse.fromCards(
            extensionId = "core-alpha",
            drawnAt = "2026-03-23T12:00:00Z",
            rechargeState = testRechargeState(availableDrawCount = 9),
            cards = emptyList(),
        )
        val packGateway = FakePackGateway().apply {
            openPackResponse = response
        }
        val viewModel = PackViewModel(
            catalogRepository = FakeCatalogGateway().apply {
                extensions = listOf(ExtensionDefinition("core-alpha", "Core Alpha", "cover"))
            },
            progressRepository = FakeProgressGateway(),
            packRepository = packGateway,
            gameSettings = queuedEpicBoostGameSettings(0, 2),
        )
        advanceUntilIdle()

        viewModel.selectExtension("core-alpha")
        viewModel.selectBooster(2)
        viewModel.openPack("core-alpha")
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.epicBoostBoosterIndex)
        assertEquals(listOf("core-alpha" to true), packGateway.openPackCalls)
    }

    @Test
    fun `open pack emits newly unlocked badges when collection crosses a badge threshold`() = runTest {
        val response = DrawPackResponse.fromCards(
            extensionId = "core-alpha",
            drawnAt = "2026-03-23T12:00:00Z",
            rechargeState = testRechargeState(availableDrawCount = 9),
            cards = listOf(
                testPackCard("ALP-001", "Nebuleuse d'Orion", "Common", "spark_fox"),
            ),
        )
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = ownedCollectionOf(),
                rechargeState = testRechargeState(),
            )
        }
        val packGateway = FakePackGateway().apply {
            openPackResponse = response
            onOpenPack = { _, _ ->
                progressGateway.progress = StandaloneProgress(
                    collection = ownedCollectionOf("ALP-001" to 1),
                    rechargeState = response.rechargeState,
                )
            }
        }
        val catalogGateway = FakeCatalogGateway().apply {
            extensions = listOf(ExtensionDefinition("core-alpha", "Core Alpha", "cover"))
            cards = listOf(testCardDefinition("ALP-001", extensionId = "core-alpha"))
        }

        val viewModel = PackViewModel(
            catalogRepository = catalogGateway,
            progressRepository = progressGateway,
            packRepository = packGateway,
            gameSettings = gameSettings,
        )
        advanceUntilIdle()

        val event = async { viewModel.events.first() }
        viewModel.openPack("core-alpha")
        advanceUntilIdle()

        assertEquals(
            listOf("core-alpha::sky::city"),
            (event.await() as PackEvent.PackReadyForReveal).newlyUnlockedBadges.map { it.id },
        )
    }

    @Test
    fun `open pack emits first pack opened badge when progress count increases to one`() = runTest {
        val response = DrawPackResponse.fromCards(
            extensionId = "core-alpha",
            drawnAt = "2026-03-23T12:00:00Z",
            rechargeState = testRechargeState(availableDrawCount = 9),
            cards = emptyList(),
        )
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = ownedCollectionOf(),
                rechargeState = testRechargeState(),
                openedPackCount = 0,
            )
        }
        val packGateway = FakePackGateway().apply {
            openPackResponse = response
            onOpenPack = { _, _ ->
                progressGateway.progress = StandaloneProgress(
                    collection = ownedCollectionOf(),
                    rechargeState = response.rechargeState,
                    openedPackCount = 1,
                )
            }
        }
        val catalogGateway = FakeCatalogGateway().apply {
            extensions = listOf(ExtensionDefinition("core-alpha", "Core Alpha", "cover"))
        }

        val viewModel = PackViewModel(
            catalogRepository = catalogGateway,
            progressRepository = progressGateway,
            packRepository = packGateway,
            gameSettings = gameSettings,
        )
        advanceUntilIdle()

        val event = async { viewModel.events.first() }
        viewModel.openPack("core-alpha")
        advanceUntilIdle()

        assertEquals(
            listOf("general::pack::first-opened"),
            (event.await() as PackEvent.PackReadyForReveal).newlyUnlockedBadges.map { it.id },
        )
    }

    @Test
    fun `open pack emits boosted pack badge when boosted progress is recorded`() = runTest {
        val response = DrawPackResponse.fromCards(
            extensionId = "core-alpha",
            drawnAt = "2026-03-23T12:00:00Z",
            rechargeState = testRechargeState(availableDrawCount = 9),
            cards = emptyList(),
        ).copy(isEpicBoosted = true)
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = ownedCollectionOf(),
                rechargeState = testRechargeState(),
                openedPackCount = 1,
            )
        }
        val packGateway = FakePackGateway().apply {
            openPackResponse = response
            onOpenPack = { _, isEpicBoosted ->
                progressGateway.progress = StandaloneProgress(
                    collection = ownedCollectionOf(),
                    rechargeState = response.rechargeState,
                    openedPackCount = 2,
                    hasOpenedEpicBoostedPack = isEpicBoosted,
                )
            }
        }
        val catalogGateway = FakeCatalogGateway().apply {
            extensions = listOf(ExtensionDefinition("core-alpha", "Core Alpha", "cover"))
        }

        val viewModel = PackViewModel(
            catalogRepository = catalogGateway,
            progressRepository = progressGateway,
            packRepository = packGateway,
            gameSettings = queuedEpicBoostGameSettings(0, 2),
        )
        advanceUntilIdle()

        val event = async { viewModel.events.first() }
        viewModel.selectExtension("core-alpha")
        viewModel.selectBooster(2)
        viewModel.openPack("core-alpha")
        advanceUntilIdle()

        assertEquals(
            listOf("general::pack::epic-boost-opened"),
            (event.await() as PackEvent.PackReadyForReveal).newlyUnlockedBadges.map { it.id },
        )
    }

    @Test
    fun `open pack generic failure resets booster selection and uses default message`() = runTest {
        val viewModel = PackViewModel(
            catalogRepository = FakeCatalogGateway().apply {
                extensions = listOf(ExtensionDefinition("core-alpha", "Core Alpha", "cover"))
            },
            progressRepository = FakeProgressGateway(),
            packRepository = FakePackGateway().apply {
                openPackFailure = IllegalStateException()
            },
            gameSettings = gameSettings,
        )
        advanceUntilIdle()

        viewModel.selectExtension("core-alpha")
        viewModel.selectBooster(1)
        viewModel.openPack("core-alpha")
        advanceUntilIdle()

        assertEquals("core-alpha", viewModel.uiState.value.selectedExtensionId)
        assertEquals(null, viewModel.uiState.value.selectedBoosterIndex)
        assertEquals(false, viewModel.uiState.value.isAwaitingPackResult)
        assertEquals("Impossible d'ouvrir le pack.", viewModel.uiState.value.errorMessage)
    }

    private fun queuedEpicBoostGameSettings(vararg values: Int): StandaloneGameSettings = StandaloneGameSettings(
        timeSource = gameSettings.timeSource,
        entropySource = QueuedEntropySource(values.toList()),
    )

    private class QueuedEntropySource(
        private val values: List<Int>,
    ) : EntropySource {
        private var index = 0

        override fun nextInt(bound: Int): Int {
            val value = values.getOrNull(index) ?: 0
            index += 1
            return value.mod(bound.coerceAtLeast(1))
        }
    }
}
