# Equipements

[← Index documentation](../README.md) | [Packs](packs.md) | [Design visuel](../visual-design.md)

Les equipements sont des cartes consommables distinctes des cartes astronomiques. Ils sont synchronises depuis le catalogue et tires dans les packs.

## Types

| Type | Effet |
| --- | --- |
| `Observatoire` | multiplie la vitesse de recharge apres la meteo |
| `Telescope` | ajoute une chance holographique |
| `Monture` | ajoute une chance de promotion d'un palier de rarete, sans depasser `Epic` |

Chaque type peut avoir un effet actif. Une seule carte d'un meme type peut etre active a la fois.

## Tirage

- Un slot `Common` peut etre remplace par une carte d'equipement.
- La chance actuelle est `commonReplacementChancePercent = 5.0`.
- Les cartes candidates utilisent `dropWeight`.
- Un pack ne renvoie pas deux fois la meme carte d'equipement exacte.

## Onboarding

Premier pack guide :

- aucun equipement ;
- cartes astronomiques forcees en `Common` ;
- pas d'holographique ni de tamponne.

Deuxieme pack guide :

- exactement un equipement `level == 1` ;
- cartes astronomiques plafonnees a `Uncommon` ;
- remplacement aleatoire normal des equipements desactive.

## Activation

- L'activation consomme `1` exemplaire possédé.
- Pour `Télescope` et `Monture`, `packsRemaining` diminue à chaque ouverture de pack.
- Pour `Observatoire`, `packsRemaining` diminue chaque fois qu'une nouvelle charge de pack est effectivement régénérée. Une ouverture n'en consomme pas, et un stock déjà plein non plus.
- L'effet expire automatiquement a `0`.
- `activationCount` conserve l'historique d'utilisation par carte.
- `lastActivatedCardIdByType` conserve le dernier equipement utilise par type.

## UI

L'ecran `Equipements` affiche :

- resume des effets actifs ;
- sections `Observatoire`, `Telescope`, `Monture` ;
- illustration runtime depuis `imageRef` ;
- niveau, bonus, stock, compteur `Utilisés` ou `Utilisées` ;
- modal plein ecran au clic sur une carte.

Si l'asset runtime manque, l'application retombe sur un fond de palette et une icone vectorielle de categorie.

## Tests associes

- `EquipmentRepositoryTest`
- `EquipmentRuntimeTest`
- `EquipmentOperationsTest`
- `EquipmentScreenTest`
- `EquipmentActivationCoachmarkVisibilityTest`
- `EquipmentArtTest`

[← Index documentation](../README.md)
