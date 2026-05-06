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
- `new-player-modal-conclusion`
- `new-player-modal-library-variants`
- `new-player-modal-crafting-tools`
- `new-player-modal-page-<index>`
- `new-player-modal-previous`
- `new-player-modal-next`
- `new-player-modal-finish`
- `new-player-modal-crafting-tools-costs-<pageIndex>`
- `new-player-modal-crafting-tools-cost-<pageIndex>-<costIndex>`

## Hints locaux

- `library-onboarding-hint`
- `pack-opening-swipe-hint-label`

## Mascotte Aster

- `aster-mascot`

Aster est rendue seulement avec les modales d'introduction/conclusion et les bulles de coachmark hors scene Equipement. Quand elle apparait, son overlay reste au-dessus des filtres, halos et bulles de coachmark, avec une taille `1.5x`, et un multiplicateur local `2x` dans les modales centrees. Les cartes `new-player-modal-welcome` et `new-player-modal-conclusion` remontent pour eviter Aster lorsque la hauteur disponible le permet. Elle est en bas centre avec deux mains ouvertes dans `new-player-modal-welcome`, en bas centre avec une main cartes et une main telescope dans `new-player-modal-conclusion` apres retour a l'accueil, puis en bas a droite, sauf sur le coachmark `HomeCrafting` ou elle est en bas a gauche avec cheveux et main en miroir horizontal. Elle utilise le telescope sur `HomeEquipment` et la cle a molette sur le chapitre Atelier. Elle est absente des modales `new-player-modal-library-variants` et `new-player-modal-crafting-tools`, de `ActivateFirstEquipment`, des hints locaux, de `OpenSecondPackMenu`, de `AwaitCraftingEligibility` et de `Completed`, et peut etre masquee temporairement si elle recouvre la cible guidee.

Le coachmark `HomeOpenPack` utilise un placement dedie : la bulle est posee par-dessus la zone de texte de la grande carte centrale.

## Tests associes

- `ProgressRepositoryTest`
- `NewPlayerOnboardingCoordinatorTest`
- `NewPlayerMascotResolverTest`
- `AsterMascotTest`
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
- `CraftingOnboardingToolsContentTest`
- `CraftingOnboardingToolsWalkthroughTest`
- `NewPlayerCoachmarkOverlayTest`
- `CraftingScreenTest`
- `CraftingViewModelTest`

Les tests relies a un device Android restent a executer via `connectedDebugAndroidTest` depuis Windows avec un emulateur ou un appareil ADB disponible.

[← Parcours onboarding](new-player-onboarding.md)
