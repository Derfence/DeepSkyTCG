# Systeme meteo et recharge des boosters

## Objectif

Le standalone utilise une meteo 100% offline, deterministe et commune a tous les appareils pour une meme date UTC de confiance. Cette meteo ne depend ni d'une API externe, ni du fuseau du telephone, ni de la locale.

Le systeme n'influe que sur la recharge des boosters.

## Source de temps

Le moteur repose uniquement sur le `trustedNow` fourni par `ProgressRepository`.

- meme instant UTC de confiance => meme meteo ;
- meme etat persiste + meme instant UTC de confiance => meme progression de recharge ;
- l'anti-retour-arriere horloge continue de passer par le mecanisme de temps de confiance deja en place.

Limite connue : sur une toute premiere installation sans serveur, si l'horloge locale est deja incorrecte, l'application ne peut pas reconstruire une verite externe. Cette limite est assumee et documentee.

## Etats meteo

- `Pluie` : `x0`
- `Nuageux` : `x0.8`
- `Clair` : `x1`
- `Pur` : `x2`

Le moteur de recharge n'utilise pas de flottants. Il convertit les multiplicateurs en unites entieres exactes par seconde :

- `Pluie` = `0`
- `Nuageux` = `4`
- `Clair` = `5`
- `Pur` = `10`

La base `x1` vaut donc `5` unites par seconde. Pour un cooldown de `6h`, une recharge complete represente `drawCooldown.seconds * 5` unites.

## Calendrier meteo deterministe

Le calendrier est ancre au `2026-01-01` en UTC et repete un cycle exact de `20` jours.

Formule :

```text
dayOffset = jours UTC entre 2026-01-01 et la date observee
slot = ((dayOffset * 11) + 7) mod 20
```

Mapping :

- `0` => `Pluie`
- `1..4` => `Nuageux`
- `5..15` => `Clair`
- `16..19` => `Pur`

Cette distribution garantit une moyenne strictement superieure a `1` sur chaque cycle complet :

```text
(1 * 0 + 4 * 0.8 + 11 * 1 + 4 * 2) / 20 = 1.11
```

## Modele de persistance

La source de verite de recharge est `PackRechargeState`.

- `availableDrawCount` : stock utilisable immediatement
- `accumulatedChargeUnits` : progression exacte deja accumulee vers la prochaine recharge
- `lastChargeEvaluationAt` : dernier instant UTC de confiance utilise pour avancer le moteur

Le snapshot securise de progression stocke directement ce modele. Comme l'application n'est pas sortie et qu'aucune sauvegarde n'existe, le schema actuel ne contient pas de migration retrocompatible.

## Moteur de recharge

Le moteur central se trouve dans `data/PackChargeState.kt`.

Responsabilites :

- normaliser `PackRechargeState` pour un instant de confiance ;
- avancer la recharge en segments par jour UTC pour appliquer la bonne meteo a chaque portion de temps ;
- calculer la progression UI ;
- deriver le prochain `nextChargeAt` sans le persister comme source de verite ;
- consommer une ouverture de pack sans dupliquer la logique dans Compose ou dans les repositories.

Invariants :

- le stock est borne entre `0` et `maxStoredDraws` ;
- si le stock est plein, l'accumulation est remise a `0` et `lastChargeEvaluationAt` est vide ;
- la recharge est tronquee a la seconde pour rester stable entre persistance et affichage ;
- le passage minuit UTC peut changer la meteo sans casser la progression deja accumulee.

## UI

La V1 affiche la meteo uniquement sur l'ecran des packs, dans un module place sous le titre `Ouvrir un pack` et au-dessus du bloc stock/recharge.

Le module presente :

- une prevision glissante sur `7` jours (`J` a `J+6`) ;
- une heure de reference `UTC` visible dans l'entete ;
- un jour abrege par colonne (`Lun`, `Mar`, `Mer`, `Jeu`, `Ven`, `Sam`, `Dim`) ;
- un icone par etat meteo ;
- le multiplicateur affiche sous chaque icone.

Representation visuelle actuelle :

- `Pluie` : nuage + traits de pluie
- `Nuageux` : nuages superposes
- `Clair` : soleil
- `Pur` : croissant de lune

L'UI ne rederive plus la logique a partir d'un cooldown brut. Elle consomme le statut produit par le moteur central et la politique meteo partagee.

## Tests

La couverture associee verifie notamment :

- la determinisme du calendrier meteo en UTC ;
- la moyenne exacte `1.11` sur le cycle de `20` jours ;
- le gel en `Pluie`, le ralentissement en `Nuageux` et l'acceleration en `Pur` ;
- le franchissement de minuit UTC ;
- la recuperation sur plusieurs jours ;
- le cap de stock a `10` ;
- la persistance du nouveau `PackRechargeState` ;
- l'exposition de la meteo et de la recharge dans le `PackViewModel` et l'ecran Compose des packs ;
- le builder de prevision sur 7 jours, le format `HH:mm UTC` et la presence des multiplicateurs dans le module Compose.
