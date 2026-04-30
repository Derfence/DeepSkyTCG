# Documentation Deep Sky TCG

[← README racine](../README.md)

Cette documentation est organisee en petits fichiers. Chaque page revient ici et pointe vers les pages voisines pour eviter les longs blocs isoles.

## Carte de lecture

```text
docs/README.md
|-- setup-and-tests.md
|-- overview.md
|-- architecture.md
|-- visual-design.md
|-- catalog-assets.md
`-- features/home.md
    |-- features/packs.md
    |   `-- features/weather-recharge.md
    |-- features/library-badges.md
    |   `-- features/trade-nfc.md
    |-- features/equipment.md
    |-- features/crafting.md
    `-- new-player-onboarding.md
        `-- flows/new-player-onboarding.md
            `-- flows/onboarding-ui-tags.md
```

## Par objectif

| Besoin | Page |
| --- | --- |
| Installer, compiler, lancer les tests | [Installation et tests](setup-and-tests.md) |
| Comprendre le produit offline | [Vue d'ensemble](overview.md) |
| Comprendre les frontieres techniques | [Architecture](architecture.md) |
| Retrouver les couleurs et conventions visuelles | [Design visuel](visual-design.md) |
| Modifier le catalogue ou les images | [Catalogue et assets](catalog-assets.md) |
| Documenter un ecran precis | Pages [fonctionnalites](features/home.md) |
| Verifier le premier parcours joueur | [Onboarding](new-player-onboarding.md) |

## Fonctionnalites

- [Accueil](features/home.md)
- [Packs et tirage local](features/packs.md)
- [Meteo et recharge](features/weather-recharge.md)
- [Bibliotheque et badges](features/library-badges.md)
- [Equipements](features/equipment.md)
- [Artisanat](features/crafting.md)
- [Echange NFC](features/trade-nfc.md)

## Rattrapage depuis git

L'audit a compare les docs existantes, les fichiers sources et l'historique recent. Les points suivants etaient disperses ou manquants dans la documentation centrale :

- `v1.6.0` : ajout de l'artisanat, menu `Atelier`, modes `Assombrir le ciel` et `Agence spatiale`.
- `v1.5.0` : echange de cartes par NFC, validation par rarete/variante et empreinte de catalogue.
- Refactor scenes : `HomeScene`, `LibraryScene`, `CraftingScene`, `EquipmentScene`, `BadgeBookScene`, `PackScene` via `AppSceneContent`.
- Visuels recents : nouveaux boosters, fond Home, portail-livre, portail-equipement, arrivee holographique, logos et tampons SVG.
- Progression : nouveaux champs `tradeLedgerState`, `equipmentBadgeProgress`, indicateurs de nouveaute et onboarding etendu.

## Anciennes pages conservees

Ces chemins restent presents pour ne pas casser les liens existants :

- [new-player-onboarding.md](new-player-onboarding.md)
- [equipment-system.md](equipment-system.md)
- [start-screen-home.md](start-screen-home.md)
- [weather-system.md](weather-system.md)

[← README racine](../README.md)
