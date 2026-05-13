# Mini-jeux

[← Index documentation](../README.md) | [Accueil](home.md)

Cette page documente l'accès aux mini-jeux et leur socle commun.

## Déblocage

Le menu `Mini-jeux` se débloque après :

- fabrication guidée terminée (`UseSkyDarkening`) ;
- passage vers l'étape d'onboarding `DiscoverMiniGames`.

Le déblocage est persisté par `miniGamesMenuUnlocked`. La première disponibilité active aussi l'indicateur de nouveauté `miniGames` sur l'accueil.

Pendant `DiscoverMiniGames`, le coachmark Home explique que la carte centrale a un verso mini-jeux, accessible par bouton ou swipe, puis par clic sur la carte. Il indique aussi le plafond fonctionnel : les quatre jeux peuvent réduire la recharge d'un pack jusqu'à `4 h` par jour. Ouvrir le menu avance l'onboarding vers `ShowConclusion`, mais la conclusion attend le retour du joueur au Home.

## Accès depuis l'accueil

- La carte centrale affiche `Ouvrir un pack` par défaut.
- Après déblocage, un bouton rond retourne la carte.
- Un swipe horizontal retourne aussi la carte si le geste commence sur la carte.
- Le verso affiche `Mini-jeux du jour` et le bouton `Menu des jeux`.

## Menu

Le menu affiche un fond ciel animé commun aux mini-jeux, puis la carte SVG plein écran `mini-games-map.svg`.

Les quatre lieux sont positionnés sur une diagonale bas-gauche vers haut-droite :

- Quiz / Ville ;
- Memory / Périurbain ;
- Timeline / Campagne ;
- Observatoire / Montagne.

Les boutons `Quiz / Ville` et `Memory / Périurbain` sont actifs et utilisent le halo de node commun. Les boutons Timeline et Observatoire restent visibles mais désactivés tant que leurs gameplays ne sont pas implémentés.

## Jeux implémentés

- [Quiz](quiz.md)
- [Memory](memory.md)

## Socle commun

Le socle commun est utilisé par Quiz et Memory, et reste prêt pour les futurs jeux :

- `MiniGameId` identifie Quiz, Memory, Timeline et Observatoire ;
- `MiniGameDifficulty` porte les quatre niveaux communs et leurs réductions maximales de `15`, `30`, `45` et `60` minutes ;
- `MiniGamesProgress` persiste les états quotidiens, les récompenses déjà obtenues et les difficultés débloquées ;
- `MiniGamesRepository` expose l'état du jour UTC, prépare les cartes résolues et applique les récompenses ;
- `MiniGameRewardApplier` réduit la recharge des packs une seule fois par jour et par jeu, avec plafonnement au stock maximal.
- `MiniGameReward` stocke les réductions en secondes pour supporter les gains proportionnels du Quiz.
- `hasPlayed` signifie que l'essai quotidien du jeu est consommé. Si `reward` vaut `null`, l'essai a été utilisé sans récompense, par exemple après abandon.

Le socle visuel partage les briques Compose réutilisables :

- `MiniGameSceneBackdrop` pour le fond ciel, les constellations et les paillettes adaptées au profil de performance ;
- `MiniGameBoardSurface` pour encadrer les zones de jeu sans multiplier les styles locaux ;
- `MiniGameHudPill` pour les compteurs compacts ;
- `MiniGameFeedbackEvent`, `MiniGameFeedbackTone` et `MiniGameFeedbackOverlay` pour les feedbacks succès, erreur, spécial et fin de partie ;
- `MiniGamePulsingRing` pour les nodes de carte ou d'objectif.

Le tirage global des cartes est déterministe et ne dépend jamais de l'état du joueur. Il utilise le jeu, la date UTC, le slot, l'extension cible éventuelle et la version d'algorithme. Si le joueur ne possède pas la carte globale, `MiniGameCardResolver` choisit un fallback déterministe parmi ses cartes de la même extension, puis parmi ses cartes des autres extensions si besoin. La carte résolue est persistée pour rester stable pendant la journée, même si la collection évolue.

## Assets

Sources attendues :

- `design-explorations/mini-games/home-mini-games-card.svg`
- `design-explorations/mini-games/mini-games-map.svg`

Ces fichiers sont synchronisés vers les assets runtime par Gradle. Si un SVG manque, l'application affiche un fallback Compose pour éviter un écran vide.

[← Index documentation](../README.md)
