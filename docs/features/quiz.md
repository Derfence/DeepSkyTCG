# Quiz

[← Mini-jeux](mini-games.md) | [Index documentation](../README.md)

Quiz est le deuxième mini-jeu quotidien implémenté. Il utilise une carte de la bibliothèque du joueur et donne une réduction de recharge selon la difficulté et le score obtenu.

## Accès et essai quotidien

Le joueur peut ouvrir l'écran Quiz, voir la carte du jour et revenir au menu sans perdre son essai. L'essai quotidien est consommé au choix d'une difficulté jouable.

Si le joueur quitte ensuite avant la fin :

- l'essai reste consommé ;
- aucune récompense n'est attribuée ;
- Quiz ne peut pas être rejoué avant le prochain jour UTC.

## Carte du jour

Quiz prépare une seule carte du jour avec le tirage déterministe commun des mini-jeux. La carte résolue est persistée pour rester stable pendant la journée, même si la collection évolue.

Le joueur voit la carte avant de choisir la difficulté. Cette prévisualisation ne consomme pas l'essai quotidien.

## Difficultés

| Difficulté | Questions | Récompense maximale |
| --- | ---: | ---: |
| `Apprenti` | `1` | `15min` |
| `Observateur` | `2` | `30min` |
| `Scientifique` | `3` | `45min` |
| `Explorateur` | `4` | `1h` |

Un score parfait débloque la difficulté suivante si elle existe.

## Questions

Les questions sont factuelles et générées depuis les champs de la carte :

- type d'objet ;
- constellation ;
- saison principale ;
- catalogue principal ;
- désignation dans le catalogue.

Chaque question propose `4` réponses distinctes. Les distracteurs viennent des autres cartes du catalogue. Si le catalogue ne permet pas de produire assez de questions valides pour la difficulté choisie, le Quiz affiche un état indisponible sans consommer l'essai.

## Déroulement

Pour chaque question :

- le joueur choisit une réponse ;
- la réponse choisie est corrigée immédiatement ;
- la bonne réponse est affichée si le joueur s'est trompé ;
- un bouton permet de passer à la question suivante ou au résultat.

## Récompense

Quiz garantit la moitié de la récompense maximale de la difficulté, puis ajoute un bonus proportionnel au score sur l'autre moitié.

Exemple en `Explorateur` :

| Score | Récompense |
| ---: | ---: |
| `0/4` | `30min` |
| `1/4` | `37min 30s` |
| `2/4` | `45min` |
| `3/4` | `52min 30s` |
| `4/4` | `1h` |

Les récompenses de mini-jeux sont stockées en secondes pour conserver ces demi-minutes sans arrondi.

[← Mini-jeux](mini-games.md)
