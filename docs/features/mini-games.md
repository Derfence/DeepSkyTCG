# Mini-jeux

[← Index documentation](../README.md) | [Accueil](home.md)

Cette page documente seulement l'acces aux mini-jeux. Les mini-jeux eux-memes restent hors scope pour cette iteration.

## Deblocage

Le menu `Mini-jeux` se debloque apres :

- fabrication guidee terminee (`UseSkyDarkening`) ;
- passage vers l'etape d'onboarding `DiscoverMiniGames`.

Le deblocage est persiste par `miniGamesMenuUnlocked`. La premiere disponibilite active aussi l'indicateur de nouveaute `miniGames` sur l'accueil.

Pendant `DiscoverMiniGames`, le coachmark Home explique que la carte centrale a un verso mini-jeux, accessible par bouton ou swipe, puis par clic sur la carte. Il indique aussi le plafond fonctionnel : les quatre jeux peuvent reduire la recharge d'un pack jusqu'a `4 h` par jour. Ouvrir le menu avance l'onboarding vers `ShowConclusion`, mais la conclusion attend le retour du joueur au Home.

## Acces depuis l'accueil

- La carte centrale affiche `Ouvrir un pack` par defaut.
- Apres deblocage, un bouton rond retourne la carte.
- Un swipe horizontal retourne aussi la carte si le geste commence sur la carte.
- Le verso affiche `Mini-jeux du jour` et le bouton `Menu des jeux`.

## Menu

Le menu affiche un fond SVG plein ecran `mini-games-map.svg`.

Les quatre lieux sont positionnes sur une diagonale bas-gauche vers haut-droite :

- Quiz / Ville ;
- Memory / Periurbain ;
- Timeline / Campagne ;
- Observatoire / Montagne.

Les boutons ronds sont visibles mais desactives tant que les jeux ne sont pas implementes.

## Assets

Sources attendues :

- `design-explorations/mini-games/home-mini-games-card.svg`
- `design-explorations/mini-games/mini-games-map.svg`

Ces fichiers sont synchronises vers les assets runtime par Gradle. Si un SVG manque, l'application affiche un fallback Compose pour eviter un ecran vide.

[← Index documentation](../README.md)
