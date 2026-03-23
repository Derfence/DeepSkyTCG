# Gatcha Client Android

Client Android natif du projet Gatcha.

## Objectif

Cette application permet au joueur de :

- créer un compte ;
- se connecter ;
- récupérer sa collection ;
- consulter sa bibliothèque de cartes ;
- ouvrir un pack par extension ;
- visualiser l'ouverture du pack puis révéler les cartes par glissement.

## Stack technique

- Kotlin
- Jetpack Compose
- Navigation Compose
- MVVM
- Ktor Client
- DataStore

## Structure fonctionnelle

- `MainActivity` : point d'entrée Android.
- `GatchaApp` : navigation Compose.
- `data/` : persistance locale, chiffrement de collection, repositories.
- `network/` : client HTTP de l'API serveur.
- `ui/viewmodel/` : logique d'écran.
- `ui/screen/` : écrans Compose.
- `assets/catalog/` : extensions et cartes statiques de la v1.

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

## Test bout en bout local

- Le client pointe désormais vers `http://gatcha.aumombelli.fr:8080`.
- Le package Android utilisé par l'application est `fr.aumombelli.gatcha`.
- En build `debug`, le client résout localement `gatcha.aumombelli.fr` vers `127.0.0.1` dans l'appareil.
- Le script racine `routage.bat` active ensuite un `adb reverse tcp:8080 tcp:8080`, ce qui redirige ce `localhost` de l'appareil vers la machine hôte.
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

## Dépendance au dépôt racine

Ce dépôt a vocation à être référencé par le dépôt racine `Gatcha-game` en tant que sous-module Git.
