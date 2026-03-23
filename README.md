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

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

## Dépendance au dépôt racine

Ce dépôt a vocation à être référencé par le dépôt racine `Gatcha-game` en tant que sous-module Git.
