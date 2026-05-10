# Analyse fonctionnelle — Mini-jeux quotidiens du TCG Astronomie

## 1\. Contexte général

Le jeu est un **Trading Card Game solo** sur le thème de l’astronomie.

Les cartes représentent des objets célestes : planètes, nébuleuses, étoiles, galaxies, constellations, amas, etc.

Le jeu ne repose pas sur un système de combat.  
Il propose un ensemble de **quatre mini-jeux quotidiens**, chacun permettant de réduire le temps de recharge global d’un pack.

L’objectif principal des mini-jeux est de permettre au joueur d’obtenir un bonus de recharge.  
L’apprentissage de notions d’astronomie est un bénéfice secondaire, mais ce n’est pas l’objectif principal.

L’expérience souhaitée est :

* solo ;
* quotidienne ;
* arcade/mobile ;
* ergonomique ;
* poétique et contemplative dans l’ambiance générale ;
* sans longs textes explicatifs initiaux si possible.

\---

## 2\. Déblocage général des mini-jeux

Les quatre mini-jeux sont débloqués après l’onboarding sur l’artisanat puis une nouvelle ouverture de pack.

Une fois débloqués, les quatre mini-jeux sont disponibles indépendamment les uns des autres.

Chaque mini-jeu peut être joué **une fois par jour**.

Le fait de lancer un mini-jeu consomme immédiatement l’essai quotidien correspondant.

Si le joueur quitte un mini-jeu à n’importe quel moment avant la fin :

* l’essai quotidien est perdu ;
* aucune récompense n’est donnée.

\---

## 3\. Structure des quatre mini-jeux

|Catégorie graphique|Thème|Mini-jeu|
|-|-|-|
|Ville|Université|Quiz|
|Périurbain|Association / amateurs|Memory|
|Campagne|Skygazing dans un champ|Timeline / classement|
|Montagne|Observatoire sur la montagne|Mini-réglages|

Les catégories **ville**, **périurbain**, **campagne** et **montagne** servent uniquement au graphisme des menus des mini-jeux.

Elles ne modifient pas les récompenses, les tirages ou la rareté des cartes.

\---

## 4\. Recharge de pack et récompenses

Chaque mini-jeu peut donner jusqu’à **1 heure de réduction**.

La récompense maximale quotidienne totale est donc de :

> \\\*\\\*4 heures de réduction par jour\\\*\\\*

Les réductions obtenues par les différents mini-jeux se cumulent.



\---

## 5\. Difficultés

Trois mini-jeux possèdent quatre niveaux de difficulté :

* Quiz ;
* Memory ;
* Observatoire.

Le jeu Timeline n’a qu’une seule difficulté.

Les noms de difficulté retenus sont :

|Niveau|Nom|Récompense maximale|
|-:|-|-:|
|1|Apprenti|15 minutes|
|2|Observateur|30 minutes|
|3|Scientifique|45 minutes|
|4|Explorateur|1 heure|

Le choix d’une difficulté consomme l’essai quotidien du mini-jeu, quelle que soit la difficulté choisie.

\---

## 6\. Déblocage des difficultés

Les difficultés se débloquent progressivement.

Pour débloquer le niveau suivant, le joueur doit réussir parfaitement le niveau précédent.

Le déblocage est permanent.

Donc :

* terminer parfaitement le niveau Apprenti débloque Observateur ;
* terminer parfaitement Observateur débloque Scientifique ;
* terminer parfaitement Scientifique débloque Explorateur.

Ce principe s’applique séparément à chaque mini-jeu concerné.

La Timeline n’a pas de système de déblocage, car elle ne possède qu’une seule difficulté.

\---

## 7\. Règle de récompense selon les jeux

### 7.1 Memory et Observatoire

Le joueur ne peut pas perdre ces deux mini-jeux.

La récompense dépend uniquement de la difficulté choisie.

|Difficulté|Récompense|
|-|-:|
|Apprenti|15 minutes|
|Observateur|30 minutes|
|Scientifique|45 minutes|
|Explorateur|1 heure|

### 7.2 Quiz et Timeline

Le Quiz et la Timeline donnent une récompense qui dépend de la réussite du joueur.

Le joueur obtient toujours :

* la moitié de la récompense maximale ;
* plus un bonus proportionnel à son score sur l’autre moitié.

Formule fonctionnelle :

> Récompense = 50 % de la récompense maximale + score obtenu × 50 % de la récompense maximale

Il n’y a pas d’arrondi.

Exemple pour une récompense maximale de 1 heure et un score de 1/4 :

* base garantie : 30 minutes ;
* bonus de score : 1/4 de 30 minutes ;
* récompense totale : 37 minutes 30 secondes.

Exemple complet avec 4 questions ou 4 cartes :

|Score|Récompense|
|-:|-:|
|0/4|30 minutes|
|1/4|37 minutes 30 secondes|
|2/4|45 minutes|
|3/4|52 minutes 30 secondes|
|4/4|1 heure|

\---

# 8\. Mini-jeu 1 — Quiz

## 8.1 Identité

|Élément|Description|
|-|-|
|Catégorie|Ville|
|Thème|Université|
|Type de jeu|Quiz|
|Source des cartes|Bibliothèque du joueur|
|Difficultés|4|
|Récompense maximale|1 heure|

\---

## 8.2 Principe général

Le joueur répond à un quiz portant sur une carte aléatoire de sa bibliothèque.

La carte du quiz est choisie aléatoirement par le jeu.

La carte est choisie une fois par jour et reste la même pour toute la journée.

Le choix de la carte du quiz est déterministe et doit être le même pour tous les joueurs pour un jour donné (pseudo aléatoire, comme pour le widget météo).

Si le joueur quitte le menu et revient plus tard, la carte du jour ne change pas.

La carte est visible avant le choix de la difficulté.

La carte est commune à toutes les difficultés du jour.

\---

## 8.3 Difficultés du quiz

|Difficulté|Nombre de questions|Récompense maximale|
|-|-:|-:|
|Apprenti|1 question|15 minutes|
|Observateur|2 questions|30 minutes|
|Scientifique|3 questions|45 minutes|
|Explorateur|4 questions|1 heure|

\---

## 8.4 Questions

Les questions sont générées à partir des informations de la carte.

Les questions sont exclusivement factuelles.

Les réponses doivent être sans nuance : une réponse est vraie ou fausse.

Chaque question propose **4 réponses possibles**.

Les réponses peuvent être très simples, car elles sont tirées aléatoirement.

Il n’y a pas de mécanisme empêchant la répétition des questions.

Il n’y a pas de pondération particulière selon la rareté de la carte.

\---

## 8.5 Déroulement

1. Le joueur ouvre le mini-jeu Quiz.
2. L’essai quotidien est consommé.
3. La carte du jour est affichée.
4. Le joueur choisit une difficulté disponible.
5. Le joueur répond au nombre de questions correspondant.
6. Chaque mauvaise réponse est corrigée immédiatement.
7. La récompense est calculée selon la difficulté et le score.
8. Si le score est parfait, la difficulté suivante est débloquée définitivement.

\---

## 8.6 Correction

Si le joueur se trompe :

* sa réponse apparaît en rouge ;
* la bonne réponse apparaît en vert.

La correction se fait question par question.

Une mauvaise réponse ne bloque pas la suite du quiz.

\---

## 8.7 Réussite parfaite

La réussite parfaite signifie que toutes les réponses sont correctes.

Exemples :

* 1/1 en Apprenti ;
* 2/2 en Observateur ;
* 3/3 en Scientifique ;
* 4/4 en Explorateur.

Une réussite parfaite débloque la difficulté suivante si elle existe.

\---

## 8.8 Après la partie

Une fois le quiz joué :

* la carte du jour reste visible ;
* le mini-jeu est considéré comme déjà joué pour la journée ;
* la récompense obtenue peut être affichée ;
* le joueur ne peut pas rejouer le quiz avant le lendemain.

\---

# 9\. Mini-jeu 2 — Memory

## 9.1 Identité

|Élément|Description|
|-|-|
|Catégorie|Périurbain|
|Thème|Association / amateurs|
|Type de jeu|Memory|
|Source des cartes|Bibliothèque du joueur|
|Difficultés|4|
|Récompense maximale|1 heure|

\---

## 9.2 Principe général

Le joueur retourne des cartes face cachée pour retrouver des paires.

Les cartes utilisées proviennent de la bibliothèque du joueur.

Les paires sont composées de deux exemplaires strictement identiques :

* même carte ;
* même variation.

Exemple :

> M42 ville doit être associée à M42 ville.

Il ne s’agit pas d’associations logiques du type :

* M42 avec Orion ;
* M42 avec nébuleuse ;
* planète avec Système solaire.

\---

## 9.3 Difficultés du memory

|Difficulté|Grille|Récompense|
|-|-:|-:|
|Apprenti|2x2|15 minutes|
|Observateur|3x3|30 minutes|
|Scientifique|4x4|45 minutes|
|Explorateur|5x5|1 heure|

\---

## 9.4 Cartes holographiques seules

Pour les grilles impaires, une carte holographique seule est ajoutée.

Cela concerne notamment :

* 3x3 ;
* 5x5.

Règle de la carte holographique seule :

* si elle est retournée en première carte, elle est validée ;
* si elle est retournée en deuxième carte, c’est une erreur.

\---

## 9.5 Déroulement

1. Le joueur ouvre le mini-jeu Memory.
2. L’essai quotidien est consommé.
3. Le joueur choisit une difficulté disponible.
4. Les cartes sont affichées face cachée.
5. Le joueur retourne les cartes deux par deux.
6. Les paires identiques sont validées.
7. Le joueur termine lorsque toutes les paires et la carte holographique éventuelle sont validées.
8. La récompense est donnée selon la difficulté choisie.
9. Terminer le niveau débloque la difficulté suivante si elle existe.

\---

## 9.6 Échec

Le joueur ne peut pas perdre le Memory.

Il n’y a pas de limite d’essais.

Il n’y a pas de chronomètre.

Les erreurs n’empêchent pas de terminer le mini-jeu.

\---

## 9.7 Réussite parfaite

Comme le joueur ne peut pas perdre, terminer le Memory est considéré comme une réussite suffisante pour débloquer le niveau suivant.

\---

# 10\. Mini-jeu 3 — Timeline / Classement

## 10.1 Identité

|Élément|Description|
|-|-|
|Catégorie|Campagne|
|Thème|Skygazing dans un champ|
|Type de jeu|Timeline / classement|
|Source des cartes|Bibliothèque du joueur|
|Difficultés|1|
|Récompense maximale|1 heure|

\---

## 10.2 Principe général

Le joueur reçoit une main de cartes et doit les placer selon un ordre ou dans des emplacements imposés.

Le critère de classement est choisi aléatoirement chaque jour.

Les cartes à classer sont choisies aléatoirement chaque jour.

Le critère et les cartes restent les mêmes pour toute la journée.

Le critère n’est pas annoncé avant le lancement du mini-jeu.

Le critère est affiché après l’apparition des cartes.

\---

## 10.3 Nombre de cartes

Le mini-jeu utilise de préférence 5 cartes.

Dans certains cas, il peut utiliser moins de cartes si le critère ou la catégorie demandée ne permet pas d’en utiliser autant.

Le nombre de cartes peut donc être :

* 5 de préférence ;
* 4 ou moins si nécessaire.

Il n’existe pas de comportement de secours si la bibliothèque du joueur n’a pas assez de cartes compatibles.

Le choix des cartes et du critère est déterministe et doit être le même pour tous les joueurs pour un jour donné (pseudo aléatoire, comme pour le widget météo).

\---

## 10.4 Types de critères

Les critères peuvent être réels ou ludiques.

Exemples de critères possibles :

* taille ;
* distance ;
* luminosité ;
* distance au soleil ;
* étapes de la vie d’une étoile ;
* catégories d’objets ;
* ordre imposé entre types d’objets.

La liste exacte des critères autorisés reste à définir.

La liste devra être fermée.

\---

## 10.5 Formats de placement

Le format peut être linéaire ou plus libre.

### Format linéaire

Le joueur classe les cartes dans un ordre.

Exemples :

* du plus petit au plus grand ;
* du plus proche au plus lointain ;
* de l’étape la plus précoce à l’étape la plus tardive.

### Format avec emplacements nommés

Le joueur place les cartes dans des zones ou catégories définies.

Exemples :

* nébuleuse ;
* étoile ;
* planète ;
* galaxie ;
* amas.

\---

## 10.6 Vie d’une étoile

Pour les critères liés à la vie d’une étoile, les cartes ne doivent pas forcément représenter des étapes strictement exactes.

Elles peuvent représenter des objets associés plus largement aux étapes de l’évolution stellaire.

\---

## 10.7 Déroulement

1. Le joueur ouvre le mini-jeu Timeline.
2. L’essai quotidien est consommé.
3. Les cartes apparaissent.
4. Le critère de classement est affiché.
5. Le joueur place toutes les cartes.
6. Le joueur valide son classement.
7. Les cartes sont corrigées.
8. La récompense est calculée selon le nombre de cartes à la bonne place.
9. Le mini-jeu est terminé pour la journée.

\---

## 10.8 Correction

La correction se base uniquement sur le nombre de cartes à la bonne place.

Les cartes correctement placées apparaissent en vert.

Les cartes incorrectement placées apparaissent en rouge.

Si au moins une carte est incorrecte, l’ordre correct complet est affiché.

\---

## 10.9 Récompense

La Timeline donne jusqu’à 1 heure.

La récompense dépend uniquement du score obtenu.

Le score correspond au nombre de cartes placées correctement.

La formule est :

> 30 minutes garanties + proportion de cartes correctes × 30 minutes

Exemple avec 5 cartes :

|Score|Récompense|
|-:|-:|
|0/5|30 minutes|
|1/5|36 minutes|
|2/5|42 minutes|
|3/5|48 minutes|
|4/5|54 minutes|
|5/5|1 heure|

Il n’y a pas d’arrondi.

\---

## 10.10 Déblocage

La Timeline n’a qu’une seule difficulté.

Elle n’a donc pas de progression de difficulté ni de déblocage associé.

\---

# 11\. Mini-jeu 4 — Observatoire / Mini-réglages

## 11.1 Identité

|Élément|Description|
|-|-|
|Catégorie|Montagne|
|Thème|Observatoire sur la montagne|
|Type de jeu|Mini-réglages immersifs|
|Source des cartes|Bibliothèque du joueur|
|Difficultés|4|
|Récompense maximale|1 heure|

\---

## 11.2 Principe général

Le joueur réalise une séquence d’actions évoquant l’activité d’un observatoire astronomique.

Le mini-jeu est plus premium et immersif que les autres, mais sa récompense maximale reste identique : 1 heure.

Le joueur ne peut pas perdre.

Il n’y a pas de chronomètre.

Les actions doivent être relaxantes et immersives.

\---

## 11.3 Difficultés

La difficulté dépend de deux éléments :

* le nombre de cibles ;
* la précision demandée dans les réglages.

Mapping fonctionnel :

|Difficulté|Nombre de cibles|Récompense|
|-|-:|-:|
|Apprenti|1 cible|15 minutes|
|Observateur|2 cibles|30 minutes|
|Scientifique|3 cibles|45 minutes|
|Explorateur|4 cibles|1 heure|

La précision demandée augmente avec le niveau de difficulté.

\---

## 11.4 Cibles

Les cibles observées dépendent de la bibliothèque du joueur.

La rareté des cartes n’influence pas la difficulté ou la récompense.

\---

## 11.5 Séquence d’actions

Chaque cible demande exactement les mêmes étapes.

Les étapes exactes restent à définir plus tard.

Exemples d’actions possibles :

* attendre la nuit en restant appuyé sur un bouton ;
* ouvrir le panneau de la coupole en le glissant ;
* tourner la coupole avec un slider ;
* régler la position haut/bas ;
* régler la position gauche/droite ;
* prendre plusieurs photos en appuyant rapidement ;
* fermer la coupole ;
* attendre le jour.

\---

## 11.6 Événements aléatoires

Des événements aléatoires peuvent s’ajouter.

Ils prennent la forme de petits détours interactifs.

Exemples possibles :

* recentrer légèrement la cible ;
* attendre qu’un nuage passe ;
* essuyer ou ajuster un élément optique ;
* corriger une petite dérive ;
* refaire un mini-réglage.

Ces événements ne doivent pas faire perdre le joueur.

Ils peuvent simplement rallonger ou enrichir légèrement la séquence.

\---

## 11.7 Déroulement

1. Le joueur ouvre le mini-jeu Observatoire.
2. L’essai quotidien est consommé.
3. Le joueur choisit une difficulté disponible.
4. Le jeu sélectionne les cibles dans la bibliothèque du joueur.
5. Le joueur réalise la séquence d’actions pour chaque cible.
6. Des petits événements interactifs peuvent apparaître.
7. Une fois toutes les cibles traitées, le mini-jeu se termine.
8. La récompense est donnée selon la difficulté choisie.
9. Terminer le niveau débloque la difficulté suivante si elle existe.

\---

## 11.8 Échec

Le joueur ne peut pas perdre le mini-jeu Observatoire.

Il n’y a pas de chronomètre.

Il n’y a pas de score.

Il n’y a pas de mauvaise performance.

La seule façon de ne pas recevoir de récompense est de quitter le mini-jeu avant la fin.

\---

## 11.9 Réussite parfaite

Comme le joueur ne peut pas perdre, terminer le niveau est considéré comme une réussite suffisante pour débloquer le niveau suivant.

\---

# 12\. Rôle de la bibliothèque du joueur

Tous les mini-jeux utilisent exclusivement les cartes de la bibliothèque du joueur.

Il n’y a pas de cartes invitées.

Il n’y a pas de comportement de secours si le joueur n’a pas assez de cartes compatibles.

Ce cas est considéré comme non problématique, car les mini-jeux ne sont activés qu’à partir du moment où le joueur possède assez de cartes.

La rareté d’une carte n’influence pas :

* la difficulté ;
* la récompense ;
* les probabilités de sélection ;
* l’accès à des questions plus difficiles.

Une carte reste une carte, quelle que soit sa rareté ou sa variante.

Il n’existe pas de système de maîtrise ou d’expérience de carte.

\---

# 13\. Règles quotidiennes

Chaque mini-jeu possède son propre état quotidien.

Pour chaque mini-jeu, le jeu doit savoir :

* s’il a déjà été joué aujourd’hui ;
* quelle récompense a été obtenue ;
* quelles difficultés sont débloquées ;
* dans le cas du quiz, quelle est la carte du jour ;
* dans le cas de la timeline, quelles sont les cartes du jour.

La ou les cartes du jour reste visible après avoir joué.

Les contenus du jour ne changent pas si le joueur quitte et revient dans le menu.

Le choix des cartes quiz et timeline est déterministe et doit être le même pour tous les joueurs pour un jour donné (pseudo aléatoire, comme pour le widget météo).

\---

# 14\. Ambiance et ergonomie

L’ambiance générale doit être :

* poétique ;
* contemplative ;
* inspirée par l’astronomie ;
* compatible avec des mini-jeux arcade/mobile.

L’ergonomie doit être prioritaire.

Les mini-jeux doivent être compréhensibles sans long texte initial.

Les explications détaillées pourront être ajustées après test d’usage.

\---

# 15\. Points restant à définir plus tard

## 15.1 Liste fermée des critères de Timeline

Il faudra définir la liste exacte des critères possibles.

Exemples de familles à étudier :

* taille ;
* distance ;
* ordre planétaire ;
* luminosité ;
* température ;
* âge ;
* type d’objet ;
* étape de vie stellaire ;
* catégories ludiques.

\---

## 15.2 Étapes exactes de l’Observatoire

Il faudra définir précisément la séquence d’actions demandée pour chaque cible.

La séquence devra être :

* identique pour chaque cible ;
* relaxante ;
* suffisamment interactive ;
* compatible avec différents niveaux de précision.

\---

## 15.3 Événements interactifs de l’Observatoire

Il faudra définir une liste d’événements possibles.

Ils devront être :

* courts ;
* non punitifs ;
* immersifs ;
* compatibles avec l’absence d’échec.

\---

## 15.4 Contenu exact des questions de Quiz

Le système de génération des questions devra produire des questions factuelles à partir des informations des cartes.

Les réponses doivent rester claires, binaires et sans nuance.

Chaque question devra proposer 4 réponses possibles.

\---

## 15.5 Présentation des résultats

Il faudra définir les écrans de fin pour chaque mini-jeu :

* récompense obtenue ;
* difficulté jouée ;
* score éventuel ;
* correction éventuelle ;
* état du prochain déblocage ;
* indication que le mini-jeu est terminé pour la journée.

\---

# 16\. Résumé court

Le projet prévoit quatre mini-jeux quotidiens indépendants pour un TCG solo d’astronomie.

Chaque mini-jeu utilise les cartes de la bibliothèque du joueur et permet de réduire la recharge globale d’un pack.

Les quatre jeux sont :

1. **Quiz universitaire** sur une carte aléatoire du jour.
2. **Memory amateur** avec des paires de cartes identiques.
3. **Timeline de skygazing** avec classement de cartes selon un critère quotidien.
4. **Observatoire de montagne** avec mini-réglages immersifs.

Chaque jeu peut rapporter jusqu’à 1 heure de réduction, soit 4 heures maximum par jour.

Les jeux sont pensés pour être courts, quotidiens, lisibles, non compétitifs, sans combat, et intégrés à une ambiance astronomique contemplative.

