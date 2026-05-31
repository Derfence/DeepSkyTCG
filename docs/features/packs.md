# Packs et tirage local

[← Index documentation](../README.md) | [Meteo et recharge](weather-recharge.md) | [Equipements](equipment.md)

Les packs sont tires entierement en local par `LocalPackEngine`. Aucun serveur ne choisit les cartes.

## Boucle

```text
Joueur
  |
  | choisit une extension
  v
PackSelection
  |
  | drawPack(extensionId, progress, trustedNow, isEpicBoosted)
  v
LocalPackEngine
  |
  | consomme une ouverture
  v
ProgressRepository
  ^
  | fusion collection + equipements
  |
PackSelection <- reveal slots tries <- LocalPackEngine
```

## Regles principales

- `maxStoredDraws = 10`.
- `cardsPerDraw = 5`.
- `drawCooldownHours = 6.0`.
- Tirage en deux phases : rarete, puis carte dans la rarete.
- Les probabilites de cartes sont recalculees depuis le catalogue embarque.
- Un meme pack ne renvoie jamais deux fois la meme carte astronomique exacte.
- Hors fallback de catalogue invalide, un pack ne contient jamais plus d'une carte astronomique holographique.
- Chaque slot `Common` peut etre remplace par une carte d'equipement selon `equipment_settings.json`.

## Variantes

Les variantes sont tirees au runtime depuis `variant_profiles.json` et `game_balance.json` :

- qualite de ciel : `city`, `suburban`, `rural`, `mountain`, `holographic` ;
- finition : `standard`, `stamped`.

Les premieres ouvertures de l'onboarding protegent les variantes premium : pas d'holographique ni de tamponne.

## Boost Epic

Quand le joueur selectionne une extension, un roll local a 10% peut choisir l'un des quatre boosters affiches. Ce booster montre une etoile `Epic` supplementaire sur sa cover.

Si ce booster est ouvert, la probabilite de rarete `Epic` gagne 3 points. Les autres raretes sont reduites proportionnellement pour conserver une somme de probabilites egale a 1.

Il n'y a pas de garde anti-farming : revenir en arriere puis selectionner a nouveau une extension relance bien une chance de boost.

## Presentation

- Selection d'extension avec boosters animes.
- La liste des extensions défile verticalement lorsque toutes les extensions ne tiennent pas dans la hauteur disponible.
- Ouverture plein ecran.
- La fiche détaillée ouverte depuis une carte tirée dimensionne la carte selon la largeur ou la hauteur disponible avant d'afficher sa description.
- Tri de reveal stable : equipements et cartes non holo avant cartes holo, puis rarete, ciel et ordre initial.
- Alertes de premiere rencontre pendant l'ouverture.
- Sortie d'ouverture : retour accueil pendant les étapes guidées de l'onboarding ou si au moins un badge vient d'être obtenu.
- Pendant la pause silencieuse `AwaitCraftingEligibility`, retour à la sélection des extensions tant que l'artisanat n'est pas éligible. Dès que le coordinateur avance vers `ViewCraftingMenu`, retour accueil pour reprendre le guidage.
- Hors de ces cas, retour à la sélection des extensions.

## Tests associes

- `LocalPackEngineTest`
- `LocalPackEngineEquipmentTest`
- `PackRepositoryTest`
- `PackViewModelTest`
- `PackSelectionScreenTest`
- `PackOpeningScreenTest`
- `PackOpeningViewModelTest`
- `EpicBoostManagerTest`

[← Index documentation](../README.md)
