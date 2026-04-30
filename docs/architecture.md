# Architecture

[← Index documentation](README.md) | [Vue d'ensemble](overview.md) | [Installation](setup-and-tests.md)

## Stack

- Kotlin `2.0.21`
- Android Gradle Plugin `8.13.2`
- Jetpack Compose + Material 3
- MVVM avec `ViewModel`
- Kotlin serialization
- Jetpack DataStore
- Android Keystore pour la progression chiffree
- Module `benchmark` pour macrobenchmarks et baseline profile

## Vue logique

```text
MainActivity
  |
  v
DstcgApp / DstcgAppRoot
  |
  v
AppSceneContent
  |-- HomeScene
  |-- LibraryScene
  |-- CraftingScene
  |-- EquipmentScene
  |-- BadgeBookScene
  `-- PackScene
        |
        v
     ViewModels
        |
        v
   Repositories
     |-- Assets catalogue JSON
     |-- DataStore chiffre
     `-- NFC HCE
```

## Frontieres principales

| Zone | Responsabilite |
| --- | --- |
| `app/` | Orchestration de scenes, transitions, onboarding global, coachmarks. |
| `feature/*` | Ecrans Compose, composants de feature et ViewModels proches de l'UI. |
| `data/` | Repositories, tirage local, recharge, meteo, catalogue, persistance. |
| `domain/` | Regles pures partagees, notamment les badges. |
| `model/` | Modeles serialisables, operations de collection, artisanat, echange. |
| `ui/component` | Surfaces de cartes, assets, indicateurs, glyphes. |
| `ui/motion` | Transitions, portails, decor celeste et animations reutilisables. |

## Flux de donnees

```text
Compose UI
   |
   | action joueur
   v
ViewModel
   |
   | commande metier
   v
Repository
   |-- lit le catalogue embarque
   |-- lit/mute la progression DataStore
   `-- renvoie un etat UI
          |
          v
      ViewModel -- StateFlow --> Compose UI
```

## Persistance locale

`ProgressSnapshot.CURRENT_SCHEMA_VERSION` vaut `8`. Le snapshot contient notamment :

- collection et variantes possedees ;
- stock/recharge de packs ;
- compteur `openedPackCount` ;
- etape d'onboarding ;
- inventaire et effets actifs d'equipements ;
- progression de badges d'equipements ;
- indicateurs de nouveaute Home/Bibliotheque ;
- ledger des echanges NFC deja appliques.

Le fichier DataStore est `dstcg_standalone_secure_progress.json`. Son contenu est enveloppe puis chiffre en AES-GCM via Android Keystore.

## Points de vigilance

- Toute regle de tirage doit rester dans `data/` ou `model/`, pas dans Compose.
- Toute evolution de persistance doit passer par `ProgressSnapshot.schemaVersion`.
- Les nouvelles scenes doivent respecter le decoupage `AppSceneContent` pour eviter de regonfler `AppSceneHost`.
- Les tests doivent couvrir les regles pures avant les tests Compose quand c'est possible.

[← Index documentation](README.md)
