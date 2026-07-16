# Installation et tests

[← Index documentation](README.md) | [Architecture](architecture.md)

## Prérequis locaux

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

Depuis WSL/Bash, privilégie le wrapper Windows pour réutiliser `sdk.dir` :

```bash
cmd.exe /c gradlew.bat :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:assembleDebug
```

Tests du pipeline catalogue :

```bash
python3 -m unittest discover -s scripts -p 'test_*.py'
```

## Tests instrumentés

`connectedDebugAndroidTest` demande un émulateur ou un appareil ADB disponible. Le workflow GitHub lance ces tests sur émulateurs API 26 et API 36 sur pull request vers `master` ou `workflow_dispatch`.

## CI

- `.github/workflows/android-ci.yml` : unit tests + `assembleDebug` sur `dev`, `master`, PR vers `master`, et lancement manuel.
- `.github/workflows/android-instrumented.yml` : tests instrumentés sur émulateurs API 26 et API 36.

## Benchmarks

Le module `benchmark` cible `fr.aumombelli.dstcg`. Les scénarios macrobench couvrent notamment :

- démarrage jusqu'à l'accueil ;
- ouverture bibliothèque et scroll ;
- ouverture d'un pack puis retour accueil.

Avant publication d'une évolution du format de sauvegarde, mesurer manuellement un export et un import sur API 26. Relever la durée de PBKDF2 sans imposer de seuil automatique fragile et vérifier que l'interface reste réactive pendant le calcul.

## Couverture actuelle

Les tests couvrent les repositories, le tirage local, la recharge, la météo, l'onboarding, les écrans Compose principaux, l'artisanat, l'échange Bluetooth, les badges, les équipements et les animations critiques.

[← Index documentation](README.md)
