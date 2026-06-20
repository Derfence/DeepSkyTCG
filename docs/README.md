# Documentation Deep Sky TCG

[← README racine](../README.md)

Cette documentation est organisée en petits fichiers. Chaque page revient ici et pointe vers les pages voisines pour éviter les longs blocs isolés.

## Carte de lecture

```text
docs/README.md
|-- setup-and-tests.md
|-- overview.md
|-- architecture.md
|-- visual-design.md
|-- catalog-assets.md
|-- audio-assets.md
`-- features/home.md
    |-- features/packs.md
    |   `-- features/weather-recharge.md
    |-- features/library-badges.md
    |   `-- features/trade-bluetooth.md
|-- features/equipment.md
|-- features/crafting.md
|-- features/mini-games.md
|   |-- features/quiz.md
|   |-- features/memory.md
|   `-- features/timeline.md
`-- new-player-onboarding.md
        `-- flows/new-player-onboarding.md
            `-- flows/onboarding-ui-tags.md
```

## Par objectif

| Besoin | Page |
| --- | --- |
| Installer, compiler, lancer les tests | [Installation et tests](setup-and-tests.md) |
| Comprendre le produit offline | [Vue d'ensemble](overview.md) |
| Comprendre les frontières techniques | [Architecture](architecture.md) |
| Vérifier la politique de confidentialité officielle | [Politique de confidentialité](../POLITIQUE_DE_CONFIDENTIALITE.md) |
| Retrouver les couleurs et conventions visuelles | [Design visuel](visual-design.md) |
| Modifier le catalogue ou les images | [Catalogue et assets](catalog-assets.md) |
| Remplacer sons, musiques et crédits audio | [Sons et crédits audio](audio-assets.md) |
| Documenter un écran précis | Pages [fonctionnalités](features/home.md) |
| Vérifier le premier parcours joueur | [Onboarding](new-player-onboarding.md) |

## Fonctionnalités

- [Accueil](features/home.md)
- [Packs et tirage local](features/packs.md)
- [Météo et recharge](features/weather-recharge.md)
- [Bibliothèque et badges](features/library-badges.md)
- [Équipements](features/equipment.md)
- [Artisanat](features/crafting.md)
- [Mini-jeux](features/mini-games.md)
- [Quiz](features/quiz.md)
- [Memory](features/memory.md)
- [Timeline](features/timeline.md)
- [Échange Bluetooth](features/trade-bluetooth.md)

## Rattrapage depuis git

L'audit a comparé les docs existantes, les fichiers sources et l'historique récent. Les points suivants étaient dispersés ou manquants dans la documentation centrale :

- `v1.6.0` : ajout de l'artisanat, menu `Atelier`, modes `Assombrir le ciel` et `Agence spatiale`.
- Mini-jeux : accès Home recto-verso, menu carte, Quiz, Memory et Timeline.
- Migration actuelle : échange de cartes par Bluetooth LE, découverte de partenaire et confirmation bilatérale.
- Refactor scenes : `HomeScene`, `LibraryScene`, `CraftingScene`, `EquipmentScene`, `BadgeBookScene`, `PackScene` via `AppSceneContent`.
- Visuels récents : nouveaux boosters, fond Home, portail-livre, portail-équipement, arrivée holographique, logos et tampons SVG.
- Progression : nouveaux champs `tradeLedgerState`, `equipmentBadgeProgress`, indicateurs de nouveauté et onboarding étendu.

## Anciennes pages conservées

Ces chemins restent présents pour ne pas casser les liens existants :

- [new-player-onboarding.md](new-player-onboarding.md)
- [equipment-system.md](equipment-system.md)
- [start-screen-home.md](start-screen-home.md)
- [weather-system.md](weather-system.md)

[← README racine](../README.md)
