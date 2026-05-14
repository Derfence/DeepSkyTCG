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

Le joueur classe les cartes par glisser-déposer dans des emplacements ordonnés, puis valide lorsque tous les emplacements sont remplis. Les cartes peuvent être déplacées depuis la main ou depuis un emplacement déjà rempli.

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
