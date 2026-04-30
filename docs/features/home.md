# Accueil

[← Index documentation](../README.md) | [Onboarding](../flows/new-player-onboarding.md) | [Design visuel](../visual-design.md)

L'accueil est la scene principale. Il remplace les anciens ecrans `Start` et `Main menu`.

## Elements visibles

- animation de lancement avec logo centre puis remonte ;
- grande carte `Ouvrir un pack` ;
- acces `Bibliotheque` et `Badges` apres le premier pack ;
- acces `Equipements` apres la premiere carte d'equipement obtenue ;
- acces `Atelier de fabrication` apres au moins `3` packs et une carte eligible ;
- menu `Parametres` en haut a droite.

## Navigation

```text
Accueil
|-- Ouvrir un pack
|-- Bibliotheque
|-- Atelier de fabrication
|-- Equipements
|-- Badges
`-- Parametres
```

## Indicateurs de nouveaute

Les boutons `Bibliotheque`, `Equipements` et `Badges` peuvent afficher un groupe de trois etoiles. L'indicateur disparait quand le menu correspondant est ouvert depuis l'accueil.

- `Bibliotheque` : nouvelles cartes obtenues.
- `Equipements` : premiere apparition d'une carte d'equipement en stock.
- `Badges` : badge debloque.

Quand la bibliotheque est marquee comme nouvelle, les vignettes des cartes nouvellement obtenues affichent aussi cet indicateur pendant la premiere visite.

## Layout responsive

Source de verite : `feature/home/HomeResponsiveLayout.kt`.

- logo d'arrivee : `16%` de la hauteur utile, borne entre `96.dp` et `124.dp` ;
- padding haut du logo : `2%`, borne entre `8.dp` et `18.dp` ;
- boutons bas : `10.5%`, bornes entre `64.dp` et `78.dp` ;
- carte centrale : ratio `TRADING_CARD_WIDTH_OVER_HEIGHT`, placee entre le logo et les boutons bas.

`AppSceneHost` reutilise ces mesures pour aligner la fin de l'animation de lancement avec le layout final.

## Parametres

Le menu contient :

- `Réinitialiser la bibliothèque` ;
- `À propos`.

Le reset demande une confirmation puis attend `2 s` avant d'activer `Valider`.

Le panneau `À propos` affiche la version et les credits depuis `feature/home/HomeAboutContent.kt`.

## Tests associes

- `HomeViewModelTest`
- `HomeResponsiveLayoutTest`
- `HomeScreenStateTest`
- `HomeScreenResponsiveLaunchTest`
- `HomeMenuNoveltyIntegrationTest`

[← Index documentation](../README.md)
