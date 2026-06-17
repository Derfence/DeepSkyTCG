# Accueil

[← Index documentation](../README.md) | [Onboarding](../flows/new-player-onboarding.md) | [Design visuel](../visual-design.md)

L'accueil est la scene principale. Il remplace les anciens ecrans `Start` et `Main menu`.

## Elements visibles

- animation de lancement avec logo centre puis remonte ;
- grande carte `Ouvrir un pack` ;
- acces `Bibliotheque` et `Badges` apres le premier pack ;
- acces `Equipements` apres la premiere carte d'equipement obtenue ;
- acces `Atelier de fabrication` apres au moins `3` packs et une carte eligible ;
- acces `Mini-jeux` pendant l'etape d'onboarding `DiscoverMiniGames` ;
- menu `Parametres` en haut a droite.

## Navigation

```text
Accueil
|-- Ouvrir un pack
|-- Bibliotheque
|-- Atelier de fabrication
|-- Equipements
|-- Badges
|-- Mini-jeux
`-- Parametres
```

## Carte recto-verso

La grande carte reste sur le recto `Ouvrir un pack` par defaut.

Quand les mini-jeux sont debloques, un bouton visible permet de retourner la carte. Un swipe horizontal fonctionne aussi si le geste commence sur la carte. Le verso montre un apercu `Mini-jeux du jour` et un bouton `Menu des jeux`.

Pendant `DiscoverMiniGames`, un coachmark cible la carte centrale et explique le bouton, le swipe, le clic sur le verso, et le plafond de `4 h` de reduction quotidienne.

## Indicateurs de nouveaute

Les boutons `Bibliotheque`, `Equipements`, `Badges` et `Mini-jeux` peuvent afficher un groupe de trois etoiles. L'indicateur disparait quand le menu correspondant est ouvert depuis l'accueil.

- `Bibliotheque` : nouvelles cartes obtenues.
- `Equipements` : premiere apparition d'une carte d'equipement en stock.
- `Badges` : badge debloque.
- `Mini-jeux` : premiere disponibilite du menu des mini-jeux pendant l'onboarding.

Quand la bibliotheque est marquee comme nouvelle, les vignettes des cartes nouvellement obtenues affichent aussi cet indicateur pendant la premiere visite.

## Layout responsive

Source de verite : `feature/home/HomeResponsiveLayout.kt`.

- logo d'arrivee : `16%` de la hauteur utile, borne entre `96.dp` et `124.dp` ;
- padding haut du logo : `2%`, borne entre `8.dp` et `18.dp` ;
- boutons bas : `10.5%`, bornes entre `64.dp` et `78.dp` ;
- carte centrale : ratio `TRADING_CARD_WIDTH_OVER_HEIGHT`, placée entre le logo et les boutons bas, à la plus grande taille permise par la largeur et la hauteur disponibles avec marges de sécurité.

`AppSceneHost` reutilise ces mesures pour aligner la fin de l'animation de lancement avec le layout final.

## Parametres

Le menu contient :

- `Réinitialiser la bibliothèque` ;
- `Réinitialiser le tutoriel` ;
- `Sons` ;
- `À propos`.

Les deux resets demandent une confirmation puis attendent `2 s` avant d'activer `Valider`. Le reset du tutoriel relance l'onboarding et ses packs guidés sans effacer la collection ni la progression réelle.
Le toggle `Sons` active ou coupe les SFX et l'ambiance. Il est stocké dans les préférences audio, séparément de la progression.

Le panneau `À propos` affiche la version et les credits depuis `feature/home/HomeAboutContent.kt`.

## Tests associes

- `HomeViewModelTest`
- `HomeResponsiveLayoutTest`
- `HomeScreenStateTest`
- `HomeScreenResponsiveLaunchTest`
- `HomeMenuNoveltyIntegrationTest`

[← Index documentation](../README.md)
