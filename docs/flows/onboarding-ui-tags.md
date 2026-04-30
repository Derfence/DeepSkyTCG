# Tags UI de l'onboarding

[← Index documentation](../README.md) | [Parcours onboarding](new-player-onboarding.md)

Cette page centralise les tags Compose et la validation associee au parcours nouveaux joueurs.

## Coachmarks globaux

- `new-player-coachmark-HomeOpenPack`
- `new-player-coachmark-PackSelectionExtension`
- `new-player-coachmark-PackSelectionBooster`
- `new-player-coachmark-HomeLibrary`
- `new-player-coachmark-HomeBadges`
- `new-player-coachmark-HomeEquipment`
- `new-player-coachmark-EquipmentActivation`
- `new-player-coachmark-HomeCrafting`
- `new-player-coachmark-CraftingDarkenSkyMode`
- `new-player-coachmark-CraftingCandidate`
- `new-player-coachmark-CraftingConfirm`

## Cibles mises en evidence

- `new-player-coachmark-target-HomeOpenPack`
- `new-player-coachmark-target-PackSelectionExtension`
- `new-player-coachmark-target-HomeLibrary`
- `new-player-coachmark-target-HomeBadges`
- `new-player-coachmark-target-HomeEquipment`
- `new-player-coachmark-target-EquipmentActivation`
- `new-player-coachmark-target-HomeCrafting`
- `new-player-coachmark-target-CraftingCandidate`
- `new-player-coachmark-target-CraftingConfirm`

`PackSelectionBooster` n'a pas de halo cible dedie : la bulle est centree sur la zone de boosters avec `targetEffect = None`.

## Cibles speciales

- `new-player-coachmark-scroll-down`
- `new-player-coachmark-touch-zone-CraftingDarkenSkyMode`
- `new-player-coachmark-muted-zone-CraftingDarkenSkyMode`

## Modales

- `new-player-modal-welcome`
- `new-player-modal-library-variants`
- `new-player-modal-page-<index>`
- `new-player-modal-previous`
- `new-player-modal-next`
- `new-player-modal-finish`

## Hints locaux

- `library-onboarding-hint`
- `pack-opening-swipe-hint-label`

## Tests associes

- `ProgressRepositoryTest`
- `NewPlayerOnboardingCoordinatorTest`
- `LocalPackEngineEquipmentTest`
- `PackRepositoryTest`
- `LibraryViewModelTest`
- `LocalEndToEndTest`
- `AstroCardThumbnailTest`
- `LibraryScreenTest`
- `HomeMenuNoveltyIntegrationTest`
- `HomeViewModelTest`
- `PackOpeningScreenTest`
- `NewPlayerOnboardingInteractionPolicyTest`
- `NewPlayerBlockingModalTest`
- `NewPlayerCoachmarkOverlayTest`
- `CraftingScreenTest`
- `CraftingViewModelTest`

Les tests relies a un device Android restent a executer via `connectedDebugAndroidTest` depuis Windows avec un emulateur ou un appareil ADB disponible.

[← Parcours onboarding](new-player-onboarding.md)
