# Gatcha Client Android Standalone

Client Android natif autonome du projet Gatcha.

## Objectif

Cette application fonctionne entierement hors ligne. Elle embarque le catalogue, gere une progression locale unique et ne depend d'aucun acces internet, d'aucun serveur et d'aucun flux d'authentification.

Au lancement, l'application :

- affiche le decor et les animations d'introduction existants ;
- ouvre l'ecran de demarrage avec un unique bouton `Commencer` ;
- enchaine vers le menu principal ;
- permet d'ouvrir des packs localement ;
- persiste la collection et le prochain tirage autorise ;
- conserve le cooldown de 12 heures entre deux ouvertures de pack.

## Stack technique

- Kotlin
- Jetpack Compose
- MVVM
- DataStore Preferences

## Architecture

- `MainActivity` : point d'entree Android.
- `GatchaApp` : facade stable qui instancie le shell Compose.
- `app/` : orchestration des scenes, etat global et transitions visuelles.
- `feature/start/` : ecran de demarrage et ViewModel du bouton `Commencer`.
- `feature/library/` : lecture de la collection locale, bibliotheque et apercus.
- `feature/packs/selection/` : choix d'extension, cooldown, lancement d'ouverture.
- `feature/packs/opening/` : reveal du pack, navigation locale et plein ecran.
- `data/ProgressRepository.kt` : persistance DataStore de `collection_json` et `next_draw_at`.
- `data/CollectionRepository.kt` : chargement, sauvegarde et fusion locale de la collection.
- `data/LocalPackEngine.kt` : tirage local pondere et verification du cooldown.
- `data/PackRepository.kt` : orchestration du tirage local puis persistance de la progression.
- `model/` : modeles de catalogue, collection, packs et progression locale.
- `assets/catalog/` : catalogue embarque (`metadata.json`, `extensions.json`, `cards.json`, `variant_profiles.json`).

## Flux utilisateur

- Demarrage : animation d'introduction puis carte `Commencer`.
- `Commencer -> menu principal` : transition visuelle conservee.
- `Menu -> packs` : ouverture d'un booster local sans appel reseau.
- `Menu -> bibliotheque` : consultation de la collection persistante.
- Retour Android depuis le menu principal : fermeture de l'activite.

Le standalone est mono-profil. Aucun ecran de login, de creation de compte, de compatibilite client/serveur ou de logout n'est present.

## Persistance locale

La progression locale contient uniquement :

- `collection_json` : la collection possedee ;
- `next_draw_at` : la prochaine date ISO autorisant l'ouverture d'un pack.

`OwnedCollection.version` reste migree via `CollectionMigrationService`. Au premier lancement, si aucune sauvegarde n'existe, l'application cree automatiquement une collection vide a la version du catalogue embarque. Si une sauvegarde ancienne est detectee, elle est migree puis reecrite localement.

## Regles de jeu offline

- `cardsPerPack = 5`
- `drawCooldown = 12h`
- tirage pondere des cartes par extension
- tirage pondere de `skyQuality` et `finish`

Le moteur local reutilise les profils de variantes embarques pour produire un resultat equivalent a un tirage serveur, mais entierement local.

## Animations et interface

Le standalone conserve :

- le decor celeste partage ;
- les transitions de scenes ;
- les animations de selection d'extension et d'ouverture de pack ;
- la bibliotheque, les apercus et le plein ecran des cartes ;
- le cooldown visible dans l'interface.

La seule simplification fonctionnelle voulue au demarrage est le remplacement du login par le bouton `Commencer`.

## Tests

Les tests du standalone se lancent depuis `client-android-standalone/`.

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:compileDebugAndroidTestKotlin
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

La couverture actuelle verifie notamment :

- `StartViewModel`
- `ProgressRepository`
- `LocalPackEngine`
- `CollectionRepository`
- `PackRepository`
- `PackViewModel`
- les ecrans Compose du flux `Commencer`
- le scenario offline `Commencer -> menu -> pack -> cooldown -> bibliotheque`

## Pre-requis locaux

- SDK Android Platform 36
- Android 16 QPR2 / SDK Platform `36.1`
- Android SDK Build-Tools `36.1.0`

Le fichier `local.properties` doit pointer vers le SDK Android Windows local, par exemple :

```properties
sdk.dir=C\:\\Users\\Derfence\\AppData\\Local\\Android\\Sdk
```

Les tests instrumentes `connectedDebugAndroidTest` doivent etre lances depuis Windows avec un emulateur ou un appareil ADB disponible.

## Catalogue

Le catalogue source editable reste le fichier racine `catalogue_astronomie.csv`, applique via :

```bash
python3 scripts/catalog_sync.py apply
```

Toute evolution du format de collection doit continuer a fournir une migration explicite `n -> n+1`.
