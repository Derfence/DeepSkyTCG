# Installation et tests

[← Index documentation](README.md) | [Architecture](architecture.md)

## Prerequis locaux

- Java 21.
- Android SDK Platform `36.1`.
- Android SDK Build-Tools `36.1.0`.
- Un `local.properties` pointant vers le SDK Android Windows local.

Exemple :

```properties
sdk.dir=C\:\\Users\\Derfence\\AppData\\Local\\Android\\Sdk
```

## Commandes courantes

Depuis Windows :

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:compileDebugAndroidTestKotlin
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

Depuis WSL/Bash, privilegie le wrapper Windows pour reutiliser `sdk.dir` :

```bash
cmd.exe /c gradlew.bat :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:assembleDebug
```

Tests du pipeline catalogue :

```bash
python3 -m unittest discover -s scripts -p 'test_*.py'
```

## Tests instrumentes

`connectedDebugAndroidTest` demande un emulateur ou un appareil ADB disponible. Le workflow GitHub lance ces tests sur emulateur API 36 uniquement sur pull request vers `master` ou `workflow_dispatch`.

## CI

- `.github/workflows/android-ci.yml` : unit tests + `assembleDebug` sur `dev`, `master`, PR vers `master`, et lancement manuel.
- `.github/workflows/android-instrumented.yml` : tests instrumentes sur emulateur API 36.

## Benchmarks

Le module `benchmark` cible `fr.aumombelli.dstcg`. Les scenarios macrobench couvrent notamment :

- demarrage jusqu'a l'accueil ;
- ouverture bibliotheque et scroll ;
- ouverture d'un pack puis retour accueil.

## Couverture actuelle

Les tests couvrent les repositories, le tirage local, la recharge, la meteo, l'onboarding, les ecrans Compose principaux, l'artisanat, l'echange NFC, les badges, les equipements et les animations critiques.

[← Index documentation](README.md)
