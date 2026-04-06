# Onboarding Nouveaux Joueurs

## Objectif

Le standalone guide maintenant le premier parcours joueur sans bloquer l'interface.

La sequence persistante est :

1. `OpenFirstPackMenu`
2. `SelectFirstExtension`
3. `SelectFirstBooster`
4. `ViewLibrary`
5. `ViewBadges`
6. `OpenSecondPackMenu`
7. `ViewEquipmentMenu`
8. `ActivateFirstEquipment`
9. `Completed`

Chaque etape est stockee dans la progression locale securisee et rejouee jusqu'a completion.
Les hints d'onboarding n'apparaissent qu'une fois les animations de transition terminees et la scene stabilisee.

## Parcours

### 1. Premier pack

- A l'accueil, un coachmark cible la grande carte `Ouvrir un pack`.
- Dans la selection de packs, un coachmark cible le premier bouton `Observer` disponible.
- Ce coachmark disparait des le clic sur l'extension pour ne pas accompagner l'animation d'ouverture.
- Une fois l'extension ouverte, un coachmark cible le premier booster seulement apres la fin de l'introduction visuelle des quatre boosters.
- Ce coachmark disparait des le clic sur un booster pour ne pas suivre l'animation de selection.
- Chaque coachmark attend la fin du mouvement de transition avant d'apparaitre.

### 2. Retour apres le premier pack

- Le hint vertical existant dans `PackOpeningScreen` reste en place.
- Quand le swipe est debloque, un libelle explicite s'affiche :
  `Glisse vers le haut pour revenir a l'accueil.`
- Au retour accueil, la priorite va a `Bibliotheque`.
- Si un badge vient d'etre debloque, sa celebration reste memoiree mais differee tant que l'etape `ViewLibrary` n'est pas terminee.

### 3. Bibliotheque puis badges

- Au premier retour accueil apres ouverture d'un pack, le coachmark cible `Bibliotheque`.
- Lors de la premiere visite bibliotheque, un micro-hint local s'affiche :
  `Touche une carte obtenue pour l'ouvrir.`
- Quand le joueur revient a l'accueil, la celebration de badge differee est rejouee si elle est encore en memoire.
- Apres cette celebration, un coachmark cible `Badges` tant que le carnet n'a pas ete ouvert.

### 4. Chapitre equipement

- L'ouverture du carnet `Badges` ne termine plus l'onboarding ; elle debloque l'etape `OpenSecondPackMenu`.
- Au retour accueil apres `Badges`, aucun nouveau coachmark n'est affiche.
- Le second pack reste accessible normalement via `Ouvrir un pack`, mais cette etape n'est plus guidee visuellement.
- Pendant ce deuxieme tirage d'onboarding :
  - aucun coachmark n'est ajoute dans `PackSelection` ;
  - les remplacements aleatoires d'equipement sont desactives ;
  - exactement un slot est remplace par une carte d'equipement `level == 1`, choisie selon `dropWeight` ;
  - un slot `Common` est privilegie ; sinon le slot de plus faible rarete finale est remplace ;
  - le premier tirage reste force sans equipement, meme si `EquipmentChancePercent` vaut `100`.
- De retour accueil, un coachmark cible `Equipements`.
- Une fois le menu ouvert, un dernier coachmark cible le premier bouton `Activer` eligible.
- L'onboarding se termine a la premiere activation reussie.

## Persistance et reprise

- Le champ persiste dans la progression est `newPlayerOnboardingStep`.
- Un reset de progression remet toujours l'etape a `OpenFirstPackMenu`.
- Les anciennes sauvegardes sans champ d'onboarding sont migrees ainsi :
  - collection vide et `openedPackCount == 0` : `OpenFirstPackMenu`
  - collection non vide ou `openedPackCount > 0` : `Completed`
- Si l'application est relancee entre le premier pack et la bibliotheque, le guidage reprend depuis l'etape persistante. La celebration de badge etant transitoire, le fallback reste le coachmark `Badges`.
- Si l'application est relancee pendant le chapitre equipement, le guidage reprend sur `OpenSecondPackMenu`, `ViewEquipmentMenu` ou `ActivateFirstEquipment` selon l'etape sauvegardee.

## Cibles et tags UI

### Coachmarks globaux

- `new-player-coachmark-HomeOpenPack`
- `new-player-coachmark-PackSelectionExtension`
- `new-player-coachmark-PackSelectionBooster`
- `new-player-coachmark-HomeLibrary`
- `new-player-coachmark-HomeBadges`
- `new-player-coachmark-HomeEquipment`
- `new-player-coachmark-EquipmentActivation`

### Cibles mises en evidence

- `new-player-coachmark-target-HomeOpenPack`
- `new-player-coachmark-target-PackSelectionExtension`
- `new-player-coachmark-target-PackSelectionBooster`
- `new-player-coachmark-target-HomeLibrary`
- `new-player-coachmark-target-HomeBadges`
- `new-player-coachmark-target-HomeEquipment`
- `new-player-coachmark-target-EquipmentActivation`

### Hints locaux

- `library-onboarding-hint`
- `pack-opening-swipe-hint-label`

## Validation

La couverture ajoutee ou mise a jour comprend :

- `ProgressRepositoryTest`
- `NewPlayerOnboardingCoordinatorTest`
- `LocalPackEngineEquipmentTest`
- `PackRepositoryTest`
- `LocalEndToEndTest`
- `LibraryScreenTest`
- `PackOpeningScreenTest`

Les tests relies a un device Android restent a executer via `connectedDebugAndroidTest` depuis Windows avec un emulateur ou un appareil ADB disponible.
