# Observatoire

[← Mini-jeux](mini-games.md) | [Index documentation](../README.md)

Observatoire est le quatrième mini-jeu quotidien implémenté. Il utilise les cartes de la bibliothèque du joueur et donne une réduction de recharge fixe selon la difficulté terminée.

## Accès et essai quotidien

Le joueur peut ouvrir l'écran Observatoire sans perdre son essai. L'essai quotidien est consommé au choix d'une difficulté jouable.

Si le joueur quitte ensuite avant de terminer la session :

- l'essai reste consommé ;
- aucune récompense n'est attribuée ;
- Observatoire ne peut pas être rejoué avant le prochain jour UTC.

## Difficultés

| Difficulté | Cibles | Tolérance | Récompense |
| --- | ---: | ---: | ---: |
| `Apprenti` | `1` | `±12%` | `15min` |
| `Observateur` | `2` | `±9%` | `30min` |
| `Scientifique` | `3` | `±6%` | `45min` |
| `Explorateur` | `4` | `±4%` | `1h` |

Terminer une session débloque la difficulté suivante si elle existe. Le joueur ne peut pas perdre : il n'y a ni score ni chronomètre.

## Cibles et réglages

Observatoire prépare une cible par niveau de difficulté avec le tirage déterministe commun des mini-jeux. Les cartes sont distinctes et limitées à la bibliothèque du joueur.

Chaque cible suit la même boucle courte :

- ouvrir la coupole ;
- régler l'azimut et l'altitude ;
- gérer le passage nuageux lorsqu'il apparaît ;
- ajuster la netteté ;
- capturer l'observation.

Les valeurs d'azimut, d'altitude, de netteté et la cible concernée par le passage nuageux sont déterministes pour le jour UTC, la difficulté et la carte. Une session du même jour reste donc stable après retour au menu.

## Présentation et feedbacks

Observatoire utilise le fond `Montagne`, les surfaces et les pills de HUD communs aux mini-jeux.

Le HUD affiche :

- gain de la difficulté ;
- progression des cibles ;
- précision demandée.

Les réglages sont non punitifs : une action valide avance simplement à l'étape suivante. Le passage nuageux est un détour court qui ne peut pas faire échouer la session.

## Récompense

Terminer toutes les cibles attribue directement la récompense de la difficulté via `MiniGameRewardApplier`.

`MiniGamesProgress.hasPlayed` signifie que l'essai quotidien est consommé. `reward == null` signifie que l'essai a été utilisé sans récompense.

[← Mini-jeux](mini-games.md)
