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

| Difficulté | Cibles | Tolérance | Décroissance capture | Récompense |
| --- | ---: | ---: | ---: | ---: |
| `Apprenti` | `1` | `±6%` | `40%/s` | `15min` |
| `Observateur` | `2` | `±4,5%` | `60%/s` | `30min` |
| `Scientifique` | `3` | `±3%` | `75%/s` | `45min` |
| `Explorateur` | `4` | `±2%` | `80%/s` | `1h` |

Terminer une session débloque la difficulté suivante si elle existe. Le joueur ne peut pas perdre : il n'y a ni score ni chronomètre.

## Cibles et réglages

Observatoire prépare une cible par niveau de difficulté avec le tirage déterministe commun des mini-jeux. Les cartes sont distinctes et limitées à la bibliothèque du joueur.

La session commence par une ouverture unique de la coupole, puis toutes les cibles suivent la même boucle courte :

- régler l'azimut et l'altitude ;
- ajuster la netteté ;
- capturer l'observation avec une pression répétée sur le bouton de capture.

Après l'ouverture de la coupole, le passage nuageux vit au niveau de la session complète : après un délai d'attente aléatoire, son opacité augmente avec le temps, sans dépendre des entrées du joueur. Si le nuage atteint `100%`, l'étape en cours est mise en pause et la bande nuageuse devient directement manipulable. Chaque tap ou frottement sur le nuage baisse son opacité ; la partie reprend exactement l'étape interrompue une fois le nuage dissipé. Le cycle continue sans remise à zéro entre les cibles et ne s'arrête qu'à la fermeture complète de la coupole.

Le réglage du cycle se fait dans `ObservatoryCloudTuning.kt` :

- `ObservatoryCloudInterCycleWaitMinMillis` et `ObservatoryCloudInterCycleWaitMaxMillis` bornent le temps d'attente aléatoire entre deux apparitions ;
- `ObservatoryCloudAccumulationDurationMillis` règle la vitesse d'apparition du nuage : plus la valeur est basse, plus l'opacité monte vite ;
- `ObservatoryCloudAccumulationTickMillis` règle la cadence de rafraîchissement de cette montée.
- `ObservatoryCloudTapScrubAmount` et `ObservatoryCloudDragPixelsForFullScrub` règlent l'efficacité des taps et du frottement pour dissiper le nuage.

La hauteur visuelle de la bande nuageuse se règle dans `ObservatoryCloudLayer.kt` avec `ObservatoryCloudBandCenterYRatio`.

Après la dernière capture, le joueur referme la coupole avant l'attribution de la récompense.

Chaque appui sur le bouton de capture ajoute `20%` à la barre de capture. Cette barre redescend automatiquement, avec une vitesse plus élevée sur les difficultés avancées et une cadence de rafraîchissement proche de `60 FPS`.
Pendant cette étape, la carte ciblée apparaît au centre du réticule avec une opacité pilotée par la barre de capture. À `100%`, elle reste visible pendant l'effet de validation, puis disparaît dès le passage à l'étape suivante.

Les valeurs d'azimut, d'altitude et de netteté sont déterministes pour le jour UTC, la difficulté et la carte. Une session du même jour reste donc stable après retour au menu.

## Présentation et feedbacks

Les écrans hors partie utilisent le fond `Montagne`, les surfaces et les pills de HUD communs aux mini-jeux.

La zone de jeu est une scène Compose manipulable plein écran inspirée de la carte Home `carte_finale.svg` :

- la coupole s'ouvre visuellement une seule fois au début de la session ;
- la coupole se referme visuellement après la dernière cible, sans réticule ni ligne de visée ;
- la cible lumineuse est affichée directement dans la scène ;
- le réglage d'azimut déplace le réticule de toute la largeur de la scène et oriente la monture ;
- le réglage d'altitude déplace le réticule entre l'observatoire et le haut de la scène, avec rotation réelle du tube ;
- le slider d'altitude est vertical, fin, proche du bord gauche et étiré à environ `92%` de la hauteur de sa boîte pendant l'alignement ;
- le réticule commence centré sur l'azimut et l'altitude ;
- le joueur doit aligner le réticule mobile sur la cible visuelle, sans repère textuel de pourcentage ;
- quand le réticule entre dans la tolérance de la difficulté, l'indicateur du réglage passe à l'état prêt sans déplacer le réticule pendant le glissé ;
- la couleur du réticule dépend de sa position animée réelle, pas seulement de la valeur instantanée du slider ;
- les sliders valident leur étape uniquement au relâchement, si la valeur courante est dans la plage de tolérance ;
- une fois l'alignement validé, le réticule se cale exactement sur la cible ;
- au passage à une nouvelle cible, le réticule garde sa position précédente et les sliders restent synchronisés avec cette position ;
- le passage nuageux attend un délai aléatoire, s'accumule visuellement en opacité avec le temps sous forme de bande nuageuse pleine largeur à hauteur fixe, puis interrompt l'étape en cours à `100%` jusqu'à dissipation par tap ou frottement direct ;
- la mise au point se règle avec deux roues dentées de type Crayford : une molette directe et une molette fine avec réduction `1:5` ;
- le rendu de la mise au point suit immédiatement la valeur des molettes, sans interpolation ;
- la netteté resserre le réticule jusqu'à faire coïncider les anneaux visuels avant la capture ;
- la capture affiche une barre qui monte à chaque appui et redescend tant qu'elle n'est pas pleine ;
- la carte observée apparaît centrée sur le réticule, en grand format lisible, avec l'opacité de la barre de capture.

Le HUD et les actionneurs sont affichés en premier plan par-dessus la scène.
Les contrôles ne sont affichés que lorsqu'ils sont utiles à l'étape active.
Les cartes servant de cibles restent utilisées par la logique déterministe, mais elles ne sont pas affichées comme cartes permanentes : elles apparaissent seulement pendant la capture.

Le HUD affiche :

- gain de la difficulté ;
- progression des cibles ;
- précision demandée.

Les réglages sont non punitifs : une action valide avance simplement à l'étape suivante. Le passage nuageux est un détour court qui ne peut pas faire échouer la session.

## Récompense

Terminer toutes les cibles attribue directement la récompense de la difficulté via `MiniGameRewardApplier`.

`MiniGamesProgress.hasPlayed` signifie que l'essai quotidien est consommé. `reward == null` signifie que l'essai a été utilisé sans récompense.

[← Mini-jeux](mini-games.md)
