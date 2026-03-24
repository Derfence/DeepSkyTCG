# Gatcha Client Android

Client Android natif du projet Gatcha.

## Objectif

Cette application permet au joueur de :

- vérifier la compatibilité catalogue avec le serveur au démarrage ;
- créer un compte ;
- se connecter ;
- récupérer sa collection ;
- consulter sa bibliothèque de cartes ;
- ouvrir l'aperçu d'une carte possédée depuis la bibliothèque puis l'agrandir en plein écran ;
- ouvrir un pack par extension ;
- visualiser l'ouverture du pack puis révéler les cartes par glissement ;
- ouvrir une carte tirée en plein écran pour consulter ses informations scientifiques.

## Stack technique

- Kotlin
- Jetpack Compose
- Navigation Compose
- MVVM
- Ktor Client
- DataStore

## Structure fonctionnelle

- `MainActivity` : point d'entrée Android.
- `GatchaApp` : bootstrap de compatibilité puis navigation Compose.
- `data/` : persistance locale, chiffrement de collection, migration de deck, repositories.
- `network/` : client HTTP de l'API serveur.
- `ui/component/` : rendu Compose partagé des cartes astro, badges de rareté et variantes visuelles.
- `ui/viewmodel/` : logique d'écran.
- `ui/screen/` : écrans Compose.
- `assets/catalog/` : `metadata.json`, `extensions.json`, `cards.json` et `variant_profiles.json`.

## Affichage des cartes astro

- Une miniature de bibliothèque reste atténuée tant que la carte n'est pas possédée.
- Les miniatures de bibliothèque gardent un rendu simplifié et n'affichent plus le texte central de catalogue ni la variante au centre.
- Un clic sur une carte possédée ouvre un aperçu centré au ratio de carte a collectionner ; un second clic ouvre le plein écran.
- La fermeture du plein écran ouvert depuis la bibliothèque revient directement a la grille, sans repasser par l'aperçu.
- L'écran d'ouverture de pack permet aussi d'ouvrir la carte révélée en plein écran.
- Les cartes d'aperçu de bibliothèque et les cartes révélées dans les packs utilisent un ratio fixe `hauteur / largeur = 1.754`.
- Le fond de carte dépend de la qualité du ciel (`city`, `suburban`, `rural`, `mountain`).
- Les cartes holographiques ajoutent une surcouche d'étoiles scintillantes.
- Le badge de rareté utilise un logo étoilé dédié :
  - `Common` : étoile blanche à 4 branches ;
  - `Uncommon` : étoile bleue à 4 branches ;
  - `Rare` : étoile or à 4 branches ;
  - `Epic` : étoile à 6 branches.
- Le plein écran intègre directement les données scientifiques du catalogue local : description, identité, coordonnées célestes et mesures.

## Compatibilité catalogue

- Le client charge `assets/catalog/metadata.json` pour obtenir `catalogVersion`.
- Au démarrage, l'application appelle `POST /api/app/status` et bloque l'UI tant que la compatibilité n'est pas validée.
- Toutes les autres requêtes HTTP envoient `X-Gatcha-Catalog-Version`.
- Si le serveur répond `client_update_required` ou `server_update_pending`, l'application rebascule sur l'écran bloquant.
- `OwnedCollection.version` est la version de deck persistée dans le blob chiffré ; les blobs anciens sont migrés côté client avant usage et resauvegarde.

## Workflow de release

- Toute nouvelle extension implique la mise à jour coordonnée de `metadata.json`, `extensions.json`, `cards.json` et `variant_profiles.json`.
- Une évolution de format du deck doit ajouter une migration explicite `n -> n+1` avant publication.
- Le catalogue source éditable est maintenant le fichier racine `catalogue_astronomie.csv`, appliqué via `python3 scripts/catalog_sync.py apply`.

## Branches Git prévues

- `master` : branche stable destinée aux releases.
- `dev` : branche d'intégration courante.

## Releases

L'état actuel du code doit correspondre à une première release technique `v0.3.0` une fois le dépôt Git initialisé.

## Politique de tests recommandée

- Sur chaque `push` vers `dev` :
  - build debug ;
  - tests unitaires ;
  - vérifications statiques légères.
- Sur chaque PR de `dev` vers `master` :
  - tests unitaires obligatoires ;
  - tests UI/automatisés si disponibles ;
  - build de validation de release.
- Sur `master` :
  - génération de release ;
  - tag Git.

## Commandes utiles

Depuis le dossier `client-android/` :

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

Depuis la racine du dépôt principal :

```powershell
.\test-all.bat
.\test-all.bat e2e
```

## Pré-requis locaux

- SDK Android Platform 36.
- Android 16 QPR2 / SDK Platform `36.1`.
- Android SDK Build-Tools `36.1.0`.
- Le projet utilise `compileSdk { version = release(36) { minorApiLevel = 1 } }` pour cibler correctement l'API `36.1`.
- Le projet force `buildToolsVersion = "36.1.0"` pour éviter l'utilisation par défaut de `35.0.0`.
- Le fichier `local.properties` doit pointer vers un chemin Windows, par exemple :

```properties
sdk.dir=C\:\\Users\\Derfence\\AppData\\Local\\Android\\Sdk
```

## Exécution locale recommandée

- Les tests unitaires Android peuvent être lancés depuis Windows avec `gradlew.bat`.
- Les tests instrumentés `connectedDebugAndroidTest` doivent être lancés depuis Windows lorsque le SDK Android installé est un SDK Windows.
- Avant `connectedDebugAndroidTest`, démarrer un émulateur Android ou brancher un appareil puis vérifier `adb devices` depuis Windows.
- Si Android Studio ou Gradle signale que `Build Tools 35.0.0` est corrompu, supprimer cette version dans le SDK Manager Windows ou supprimer le dossier `C:\Users\Derfence\AppData\Local\Android\Sdk\build-tools\35.0.0`, puis relancer.
- Le dépôt racine fournit un lanceur `test-all.bat` qui enchaîne les tests client puis serveur.
- Le mode `.\test-all.bat e2e` lance en plus une vraie expérimentation bout en bout sur l'app Android debug avec un serveur local isolé.
- Le `test-all.bat` standard continue d'exécuter uniquement les tests instrumentés Android non-E2E.

## Test bout en bout local

- Le client pointe désormais vers `http://gatcha.aumombelli.fr:8080`.
- Le démarrage appelle d'abord `POST /api/app/status`, il faut donc que le serveur local expose aussi la même `catalogVersion`.
- Le package Android utilisé par l'application est `fr.aumombelli.gatcha`.
- En build `debug`, le client résout localement `gatcha.aumombelli.fr` vers `127.0.0.1` dans l'appareil.
- Le script racine `routage.bat` active ensuite un `adb reverse tcp:8080 tcp:8080`, ce qui redirige ce `localhost` de l'appareil vers la machine hôte.
- Le mode `.\test-all.bat e2e` orchestre automatiquement :
  - l'activation du routage ;
  - le `pm clear` de l'app ;
  - le démarrage du serveur local ;
  - l'exécution d'un vrai test instrumenté E2E ;
  - les validations serveur côté hôte ;
  - l'arrêt du serveur et la désactivation du routage.
- Le scénario E2E vérifie aussi :
  - l'ouverture plein écran d'une carte depuis l'écran d'ouverture de pack ;
  - l'aperçu puis le plein écran d'une carte possédée depuis la bibliothèque.
- Démarrer d'abord l'émulateur ou brancher un appareil, puis lancer le routage depuis un terminal Windows :

```powershell
.\routage.bat on
```

- Vérifier éventuellement l'état du routage :

```powershell
.\routage.bat status
```

- Lancer ensuite le serveur sur la machine hôte, par exemple depuis WSL :

```bash
cd /mnt/c/Users/Derfence/Documents/Gatcha/server
./gradlew run
```

- Depuis l'appareil, le domaine applicatif atteint alors la machine hôte via `localhost` + `adb reverse`.
- Une fois le test terminé, couper la redirection :

```powershell
.\routage.bat off
```

Pour exécuter la chaîne E2E complète :

```powershell
.\test-all.bat e2e
```

Le test E2E pilote la vraie app via des `testTag` stables et un utilisateur de test injecté par arguments d'instrumentation.

## Dépendance au dépôt racine

Ce dépôt a vocation à être référencé par le dépôt racine `Gatcha-game` en tant que sous-module Git.
