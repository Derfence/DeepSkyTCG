# Memory

[← Mini-jeux](mini-games.md) | [Index documentation](../README.md)

Memory est le premier mini-jeu quotidien implémenté. Il utilise les cartes de la bibliothèque du joueur et donne une réduction de recharge selon la difficulté terminée.

## Accès et essai quotidien

Le joueur peut ouvrir l'écran Memory sans perdre son essai. L'essai quotidien est consommé au choix d'une difficulté jouable.

Si le joueur quitte ensuite avant de terminer la grille :

- l'essai reste consommé ;
- aucune récompense n'est attribuée ;
- Memory ne peut pas être rejoué avant le prochain jour UTC.

## Difficultés

| Difficulté | Grille | Récompense |
| --- | ---: | ---: |
| `Apprenti` | `2x2` | `15min` |
| `Observateur` | `3x3` | `30min` |
| `Scientifique` | `4x4` | `45min` |
| `Explorateur` | `5x5` | `1h` |

Terminer une grille débloque la difficulté suivante si elle existe.

## Cartes et paires

Memory utilise uniquement les cartes possédées par le joueur. Une variante possédée suffit à générer une paire : les cartes ne sont pas consommées et il n'est pas nécessaire de posséder deux exemplaires.

Les paires sont strictes :

- même carte ;
- même extension ;
- même qualité de ciel ;
- même finition.

Une même carte ne peut pas apparaître dans deux paires différentes, même si le joueur possède plusieurs variantes de cette carte. Si le tirage commun retombe sur une carte déjà utilisée, le plateau complète les paires avec une autre carte possédée de façon déterministe.

## Carte holographique seule

Les grilles impaires ajoutent une carte seule holographique.

Le jeu choisit d'abord une vraie variante holographique possédée. Si le joueur n'en possède aucune, il utilise une carte possédée avec un rendu holographique visuel de secours.

Règles de sélection :

- retournée en première carte, elle est validée immédiatement ;
- retournée en deuxième carte, c'est une erreur et les deux cartes se retournent.

## Présentation et feedbacks

Memory utilise le socle visuel commun des mini-jeux : fond ciel animé, surface de plateau, pills de HUD et overlay de feedback.

Le HUD affiche :

- gain de la difficulté ;
- cartes validées ;
- nombre de coups ;
- série courante et meilleure série.

Les cartes ont un dos astronomique, une animation de retournement et des retours visuels selon l'état :

- match : halo et paillettes de succès ;
- erreur : tremblement court et feedback rouge ;
- carte holographique : surbrillance et paillettes renforcées ;
- fin de grille : célébration plein écran.

Le plateau calcule la hauteur des cartes depuis l'espace restant à l'écran. Les tuiles peuvent donc devenir rectangulaires pour maximiser la surface jouable, notamment sur les grandes grilles.

## Récompense

Terminer la grille attribue la récompense de la difficulté via `MiniGameRewardApplier`.

`MiniGamesProgress.hasPlayed` signifie que l'essai quotidien est consommé. `reward == null` signifie que l'essai a été utilisé sans récompense.

[← Mini-jeux](mini-games.md)
