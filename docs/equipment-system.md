# Systeme d'equipements

## Objectif

Le standalone ajoute un sous-systeme d'equipements completement distinct des extensions, de la bibliotheque et des badges.

Les trois types sont :

- `Observatoire`
- `Telescope`
- `Monture`

Chaque carte d'equipement est un consommable data-driven avec un niveau immuable.

## Boucle de jeu

1. Un pack est ouvert localement.
2. Chaque slot resout sa rarete.
3. Si la rarete finale est `Common`, le slot peut etre remplace par une carte d'equipement selon `commonReplacementChancePercent`.
4. La carte d'equipement obtenue va dans un inventaire dedie.
5. Le joueur active plus tard une carte depuis l'ecran `Equipements`.
6. L'activation consomme exactement une carte possedee et applique l'effet sur les prochains packs.
7. A l'interieur d'un meme pack, le moteur ne renvoie jamais deux fois la meme carte exacte. Si une rarete planifiee n'a plus de carte libre, il choisit une autre carte encore inedite dans l'extension.

## Overrides onboarding

- 1er pack de l'onboarding :
  - aucune carte d'equipement ne peut apparaitre, meme si `EquipmentChancePercent` vaut `100` ;
  - toutes les cartes astronomiques sont forcees en `Common`.
- 2e pack de l'onboarding, apres la visite du carnet `Badges` :
  - il correspond au premier pack effectivement ouvert apres la pause silencieuse `OpenSecondPackMenu` ;
  - les cartes astronomiques y sont plafonnees a `Uncommon` ;
  - exactement une carte d'equipement est garantie ;
  - seules les cartes `level == 1` avec `dropWeight > 0` sont candidates ;
  - le remplacement aleatoire normal est desactive pour ce pack ;
  - un slot `Common` est remplace en priorite ;
  - s'il n'existe aucun slot `Common`, le slot de plus faible rarete finale est remplace ;
  - la taille du pack reste strictement identique.
- Une fois ce deuxieme pack termine ou hors onboarding, les regles standards reprennent immediatement.

## Bonus

- `Monture` : ajoute une chance additive de promouvoir un slot d'un palier de rarete, sans depasser `Epic`.
- `Telescope` : ajoute une chance additive de finition holographique, capee a `100%`.
- `Observatoire` : multiplie la vitesse de recharge des packs tant que l'effet est actif.

## Activation et persistance

- une seule carte active par type ;
- des types differents peuvent etre actifs en meme temps ;
- chaque ouverture de pack decremente `packsRemaining` pour tous les effets actifs ;
- quand `packsRemaining` atteint `0`, l'effet expire automatiquement ;
- l'historique persistant est un compteur `activationCount` par carte, sans date ;
- `lastActivatedCardIdByType` conserve le dernier equipement utilise pour chaque type.

## UI

L'ecran `Equipements` de l'accueil affiche :

- un bouton `Equipements` sur `Home`, masque tant qu'aucune carte d'equipement n'a encore ete obtenue ;
- un resume des effets actifs ;
- une section visuelle par type, sur une ligne dediee ;
- une icone miniature dediee a chaque categorie ;
- un header de section simplifie avec uniquement le titre de la categorie et son libelle de bonus ;
- le dernier equipement utilise par type quand il existe ;
- les cartes possedees dans une rangee horizontale par categorie, avec niveau, bonus, stock, compteur d'usage libelle `Utilisés` ou `Utilisées` selon la categorie, et etat actif ;
- un clic sur une carte ouvre directement sa fiche detaillee dans une modale plein ecran, sans seconde etape intermediaire.

## Catalogue

Les donnees sont synchronisees depuis `catalogue_astronomie.xlsx`.

Feuilles et sections :

- `Equipements` : definitions data-driven des cartes d'equipement
- `Donnees` / `EquipmentChancePercent` en `A19/B19` : regle globale de remplacement des slots `Common`
- `Resultats` / `EquipmentCards` : sortie diagnostique legacy eventuellement presente dans le classeur, mais ignoree par la synchronisation

Colonnes de `Equipements` :

- `equipmentCardId`
- `equipmentType`
- `displayName`
- `level`
- `imageRef`
- `packsAffected`
- `bonusValue`
- `bonusUnit`
- `dropWeight`
- `description`

## Assets derives

Le pipeline `scripts/catalog_sync.py apply --sheet catalogue_astronomie.xlsx` regenere :

- `app/src/main/assets/catalog/equipment_cards.json`
- `app/src/main/assets/catalog/equipment_settings.json`

Le script lit le classeur en lecture seule et ne reecrit jamais `catalogue_astronomie.xlsx`.
