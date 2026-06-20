# Architecture

[← Index documentation](README.md) | [Vue d'ensemble](overview.md) | [Installation](setup-and-tests.md)

## Stack

- Kotlin `2.0.21`
- Android Gradle Plugin `8.13.2`
- Jetpack Compose + Material 3
- MVVM avec `ViewModel`
- Kotlin serialization
- Jetpack DataStore
- Android Keystore pour la progression chiffrée
- Audio Android natif (`SoundPool` et `MediaPlayer`)
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
     |-- DataStore chiffré
     |-- DataStore préférences audio
     |-- DataStore réglages d'échange
     `-- Bluetooth LE GATT
```

## Frontières principales

| Zone | Responsabilité |
| --- | --- |
| `app/` | Orchestration de scènes, transitions, onboarding global, coachmarks. |
| `audio/` | Contrôleur audio, cues SFX, mix runtime, ambiance et préférence globale sons activés/désactivés. |
| `feature/*` | Écrans Compose, composants de feature et ViewModels proches de l'UI. |
| `data/` | Repositories, tirage local, recharge, météo, catalogue, persistance. |
| `domain/` | Règles pures partagées, notamment les badges. |
| `model/` | Modèles sérialisables, opérations de collection, artisanat, échange. |
| `ui/component` | Surfaces de cartes, assets, indicateurs, glyphes. |
| `ui/motion` | Transitions, portails, décor céleste et animations réutilisables. |

## Flux de données

```text
Compose UI
   |
   | action joueur
   v
ViewModel
   |
   | commande métier
   v
Repository
   |-- lit le catalogue embarqué
   |-- lit/mute la progression DataStore
   `-- renvoie un état UI
          |
          v
      ViewModel -- StateFlow --> Compose UI
```

## Persistance locale

`ProgressSnapshot.CURRENT_SCHEMA_VERSION` vaut `14`. Le snapshot contient notamment :

- collection et variantes possédées ;
- stock/recharge de packs ;
- compteur `openedPackCount` ;
- étape d'onboarding ;
- compteur `newPlayerOnboardingPackCount` des packs guidés du tutoriel ;
- inventaire et effets actifs d'équipements ;
- progression de badges d'équipements ;
- indicateurs de nouveauté Home/Bibliothèque ;
- ledger des échanges Bluetooth déjà appliqués ou préparés ;
- déblocage du menu mini-jeux et progression commune des mini-jeux.

Le fichier DataStore est `dstcg_standalone_secure_progress.json`. Son contenu est enveloppé puis chiffré en AES-GCM via Android Keystore.

La préférence audio globale est volontairement séparée de la progression et stockée dans `dstcg_audio_settings.preferences_pb`.
Elle ne change pas `ProgressSnapshot.schemaVersion` et n'est pas effacée par la réinitialisation de la bibliothèque.

Le nom visible pendant l'échange Bluetooth est stocké séparément dans `dstcg_trade_settings.preferences_pb`. Il est limité à 12 octets UTF-8 pour tenir dans l'annonce BLE, avec un défaut court du type `Obs. 4821`.

## Points de vigilance

- Toute règle de tirage doit rester dans `data/` ou `model/`, pas dans Compose.
- Les cues audio doivent rester branchés depuis la couche UI/app et ne doivent pas modifier les ViewModels métier.
- Les réglages audio doivent rester centralisés dans `AudioMix.kt` pour éviter les volumes, pitchs ou fichiers dupliqués dans les écrans.
- Toute évolution de persistance doit passer par `ProgressSnapshot.schemaVersion`.
- Les nouvelles scènes doivent respecter le découpage `AppSceneContent` pour éviter de regonfler `AppSceneHost`.
- Les tests doivent couvrir les règles pures avant les tests Compose quand c'est possible.

[← Index documentation](README.md)
