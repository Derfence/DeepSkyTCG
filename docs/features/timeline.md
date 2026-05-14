# Timeline

[← Mini-jeux](mini-games.md) | [Index documentation](../README.md)

Timeline est le troisième mini-jeu quotidien implémenté. Il utilise les cartes de la bibliothèque du joueur et donne une réduction de recharge selon le nombre de cartes placées au bon endroit.

## Accès et essai quotidien

Timeline n'a pas de difficulté. Ouvrir une Timeline jouable consomme immédiatement l'essai quotidien du jeu.

Si le joueur quitte avant de valider :

- l'essai reste consommé ;
- aucune récompense n'est attribuée ;
- Timeline ne peut pas être rejouée avant le prochain jour UTC.

Si le joueur possède moins de deux cartes au total, l'écran affiche une indisponibilité et l'essai n'est pas consommé. À partir de deux cartes possédées, la Timeline choisit toujours un critère jouable.

## Critère du jour

Les critères principaux sont ordonnés de manière déterministe depuis la date UTC. Le jeu prend le premier critère qui possède au moins deux cartes compatibles dans la bibliothèque du joueur.

La V1 utilise une liste fermée de critères calculables avec le catalogue actuel :

- distance des étoiles et objets du ciel profond, du plus proche au plus lointain ;
- taille réelle des objets du ciel profond, du plus petit au plus grand ;
- diamètre des objets du Système solaire, du plus petit au plus grand ;
- taille apparente, de la plus petite à la plus grande dans le ciel ;
- luminosité des étoiles et objets du ciel profond, du plus lumineux au moins lumineux.

Si aucun critère principal ne peut utiliser au moins deux cartes possédées, le jeu utilise le critère de secours `Position dans le ciel`, qui classe les cartes du sud vers le nord à partir de leur déclinaison. Ce secours reste limité aux cartes de la bibliothèque et évite les cartes invitées.

Le critère retenu filtre aussi les cartes éligibles au tirage et aux fallbacks, pour éviter de demander un classement impossible à calculer.

## Déroulement

Timeline prépare jusqu'à `5` cartes compatibles. Elle accepte une main de `4`, `3` ou `2` cartes si la bibliothèque ne permet pas d'en afficher davantage.

Le joueur classe les cartes par glisser-déposer dans une timeline horizontale scrollable. Les emplacements ont le format des cartes, avec un affichage agrandi pour rester lisibles, et la main est placée sous la timeline avec les cartes partiellement visibles au bord bas du plateau. Les cartes jouables affichent directement la carte complète, sans cadre secondaire ni nom ajouté sous la carte, afin d'utiliser toute la place disponible. Quand le cadre vertical est trop court pour tout afficher, la timeline remonte au plus haut possible ; dès que les emplacements et la main peuvent être visibles entièrement, l'ensemble est centré verticalement.

La main conserve des positions fixes : quand une carte est placée dans la timeline, son emplacement d'origine reste vide afin que les autres cartes ne se déplacent pas. Une carte déjà placée peut être glissée vers n'importe quelle place vide de la main pour la retirer de la timeline. Déposer une carte sur un emplacement occupé échange les deux cartes : l'ancienne carte prend la place d'origine de la carte déplacée, que celle-ci vienne de la main ou d'un autre emplacement.

Les zones de dépôt suivent les emplacements visibles : une carte est placée lorsque son centre visuel arrive dans un emplacement. Une carte déjà placée repasse au premier plan lorsqu'elle est glissée vers un autre emplacement.

Les emplacements extrêmes explicitent le sens du classement sans numéro visible : par exemple `La plus proche` / `La plus lointaine`, `La plus lumineuse` / `La moins lumineuse` ou `Le plus au sud` / `Le plus au nord` selon le critère du jour. Les emplacements intermédiaires restent sans texte.

Le bouton de validation apparaît uniquement lorsque tous les emplacements sont remplis. Son espace est réservé dès l'ouverture pour éviter de redimensionner le plateau au moment de son apparition. Les cartes peuvent être déplacées depuis la main ou depuis un emplacement déjà rempli.

## Correction

Après validation :

- chaque emplacement correct apparaît comme réussi ;
- chaque erreur affiche la carte placée et la carte attendue ;
- si au moins une carte est incorrecte, l'ordre correct complet est affiché.

Le score correspond uniquement au nombre de cartes à la bonne place.

## Récompense

Timeline donne jusqu'à `1h`. La récompense garantit `30min`, puis ajoute un bonus proportionnel au score sur les `30min` restantes.

Exemple avec `5` cartes :

| Score | Récompense |
| ---: | ---: |
| `0/5` | `30min` |
| `1/5` | `36min` |
| `2/5` | `42min` |
| `3/5` | `48min` |
| `4/5` | `54min` |
| `5/5` | `1h` |

[← Mini-jeux](mini-games.md)
