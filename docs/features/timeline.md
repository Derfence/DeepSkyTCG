# Timeline

[← Mini-jeux](mini-games.md) | [Index documentation](../README.md)

Timeline est le troisième mini-jeu quotidien implémenté. Il utilise les cartes de la bibliothèque du joueur et donne une réduction de recharge selon une série de comparaisons entre deux cartes.

## Accès et essai quotidien

Le joueur peut ouvrir l'écran Timeline, choisir une difficulté, puis lancer une partie. L'essai quotidien est consommé au choix d'une difficulté jouable.

Si le joueur quitte ensuite avant la fin :

- l'essai reste consommé ;
- aucune récompense n'est attribuée ;
- Timeline ne peut pas être rejouée avant le prochain jour UTC.

Si le joueur possède moins de deux cartes comparables, l'écran affiche une indisponibilité et l'essai n'est pas consommé. À partir de deux cartes comparables, la Timeline choisit toujours un critère jouable.

## Difficultés

| Difficulté | Comparaisons | Récompense maximale |
| --- | ---: | ---: |
| `Apprenti` | `1` | `15min` |
| `Observateur` | `2` | `30min` |
| `Scientifique` | `3` | `45min` |
| `Explorateur` | `4` | `1h` |

Un score parfait débloque la difficulté suivante si elle existe.

## Critère du jour

Les critères principaux sont ordonnés de manière déterministe depuis la date UTC. Le jeu prend le premier critère qui possède au moins deux cartes compatibles dans la bibliothèque du joueur.

La V1 utilise une liste fermée de critères calculables avec le catalogue actuel :

- distance des étoiles et objets du ciel profond ;
- taille réelle des objets du ciel profond ;
- diamètre des objets du Système solaire ;
- taille apparente dans le ciel ;
- luminosité des étoiles et objets du ciel profond.

Si aucun critère principal ne peut utiliser au moins deux cartes possédées, le jeu utilise le critère de secours `Position dans le ciel`, qui compare les cartes du sud vers le nord à partir de leur déclinaison. Ce secours reste limité aux cartes de la bibliothèque et évite les cartes invitées.

Le critère retenu filtre aussi les cartes éligibles au tirage et aux fallbacks, pour éviter de demander une comparaison impossible à calculer.

## Déroulement

Une partie utilise un seul critère et enchaîne `1` à `4` comparaisons selon la difficulté choisie.

Pour chaque comparaison :

- deux emplacements sont affichés, par exemple `La plus proche` et `La plus lointaine` ;
- deux cartes sont affichées sous les emplacements ;
- le joueur place les cartes par glisser-déposer ;
- déposer une carte sur un emplacement occupé échange les deux cartes ;
- le bouton de validation apparaît uniquement lorsque les deux emplacements sont remplis.

Les cartes et emplacements sont dimensionnés dynamiquement pour que les deux cartes tiennent côte à côte dans la largeur disponible. Il n'y a plus de timeline horizontale scrollable.

Les paires dont les deux cartes ont exactement la même valeur sur le critère sont exclues.

## Correction

Après validation d'une comparaison :

- un feedback immédiat indique si la comparaison est réussie ;
- le jeu passe à la comparaison suivante, ou au résultat si la série est terminée.

Le résultat affiche :

- le score sur le nombre de comparaisons ;
- la récompense obtenue ;
- la correction de chaque comparaison, avec les valeurs attendues.

## Récompense

Timeline utilise la même formule que le Quiz. Elle garantit la moitié de la récompense maximale de la difficulté, puis ajoute un bonus proportionnel au score sur l'autre moitié.

Exemple en `Explorateur` :

| Score | Récompense |
| ---: | ---: |
| `0/4` | `30min` |
| `1/4` | `37min 30s` |
| `2/4` | `45min` |
| `3/4` | `52min 30s` |
| `4/4` | `1h` |

[← Mini-jeux](mini-games.md)
