# Meteo et recharge

[← Index documentation](../README.md) | [Packs](packs.md)

La recharge des packs utilise une meteo offline, deterministe et commune a tous les appareils pour une meme date UTC de confiance.

## Source de temps

Le moteur repose sur `trustedNow` fourni par `ProgressRepository`.

- meme instant UTC de confiance => meme meteo ;
- meme progression + meme instant UTC => meme recharge ;
- l'anti-retour-arriere horloge passe par le temps de confiance local.

Limite connue : sur une premiere installation sans serveur, si l'horloge locale est deja incorrecte, l'application ne peut pas retrouver une verite externe.

## Etats

| Etat | Multiplicateur | Unites par seconde |
| --- | --- | --- |
| `Pluie` | `x0` | `0` |
| `Nuageux` | `x0.8` | `4` |
| `Clair` | `x1` | `5` |
| `Pur` | `x2` | `10` |

La base `x1` vaut `5` unites par seconde. Une recharge complete vaut `drawCooldown.seconds * 5`.

Quand un `Observatoire` est actif, la meteo est appliquee d'abord, puis le multiplicateur d'equipement.

## Calendrier deterministe

Le cycle est ancre au `2026-01-01` UTC et repete `20` jours.

```text
dayOffset = jours UTC entre 2026-01-01 et la date observee
slot = ((dayOffset * 11) + 7) mod 20
```

Mapping :

- `0` => `Pluie`
- `1..4` => `Nuageux`
- `5..15` => `Clair`
- `16..19` => `Pur`

Moyenne garantie :

```text
(1 * 0 + 4 * 0.8 + 11 * 1 + 4 * 2) / 20 = 1.11
```

## Persistance

`PackRechargeState` contient :

- `availableDrawCount` ;
- `accumulatedChargeUnits` ;
- `lastChargeEvaluationAt`.

Invariants :

- stock borne entre `0` et `10` ;
- stock plein => accumulation remise a `0` ;
- calcul segmente par jour UTC pour respecter les changements de meteo ;
- `nextChargeAt` est derive, pas persiste.

## UI

L'ecran des packs affiche une prevision sur `7` jours, de `J` a `J+6`, avec heure de reference `UTC`, icone et multiplicateur.
Cette prevision est affichee au-dessus du bloc stock/recharge des packs.
Le bloc stock/recharge colore aussi la barre de progression selon la météo courante. Un indicateur placé au bout de la progression reprend l'icône météo et ajoute un signal de vitesse : pause pour la pluie, flèche ralentie pour le nuageux, flèche normale pour le clair et double flèche pour le ciel pur. L'indicateur reste entièrement visible aux extrémités de la barre, avec une marque sur la barre pour garder le point réel de progression lisible.

Representations actuelles :

- `Pluie` : nuage + pluie ;
- `Nuageux` : nuages superposes ;
- `Clair` : soleil ;
- `Pur` : croissant de lune.

## Tests associes

- `DeterministicWeatherCalendarTest`
- `PackChargeStateTest`
- `WeatherForecastUiTest`
- `PackViewModelTest`

[← Index documentation](../README.md)
