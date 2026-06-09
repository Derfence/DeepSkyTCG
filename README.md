# Deep Sky TCG - Client Android Standalone

Client Android natif et autonome de Deep Sky TCG. L'application fonctionne hors ligne : catalogue embarque, progression locale chiffree, tirage de packs, collection, badges, equipements, artisanat et echange NFC sans serveur.

## Documentation

L'index principal est [docs/README.md](docs/README.md).

Lectures utiles :

- [Vue d'ensemble](docs/overview.md) : objectif produit, boucle de jeu et parcours.
- [Architecture](docs/architecture.md) : modules, scenes Compose, repositories et persistance.
- [Installation et tests](docs/setup-and-tests.md) : prerequis Android, commandes Gradle, CI et benchmarks.
- [Design visuel](docs/visual-design.md) : couleurs, raretes, qualites de ciel, equipements et assets de marque.
- [Catalogue et assets](docs/catalog-assets.md) : XLSX, JSON embarques, scripts et illustrations.

Fonctionnalites :

- [Accueil](docs/features/home.md)
- [Packs et tirage local](docs/features/packs.md)
- [Meteo et recharge](docs/features/weather-recharge.md)
- [Bibliotheque et badges](docs/features/library-badges.md)
- [Equipements](docs/features/equipment.md)
- [Artisanat](docs/features/crafting.md)
- [Echange NFC](docs/features/trade-nfc.md)
- [Onboarding nouveaux joueurs](docs/new-player-onboarding.md)

## Demarrage rapide

Depuis Windows :

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Depuis WSL/Bash, utilise le wrapper Windows pour reutiliser le SDK local :

```bash
cmd.exe /c gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Prerequis principaux :

- Java 21
- Android SDK Platform 36.1
- Android SDK Build-Tools 36.1.0
- `local.properties` pointant vers le SDK Android Windows local

Les commandes detaillees sont dans [Installation et tests](docs/setup-and-tests.md).

## Etat fonctionnel actuel

- Version affichée dans l'application : `v2.6.1`.
- Application mono-profil, sans login, sans backend et sans publicite.
- Progression securisee dans DataStore avec chiffrement AES-GCM via Android Keystore.
- Stock local de `10` ouvertures, `5` cartes par pack et cooldown de `6 h` module par la meteo UTC.
- Catalogue embarque synchronise depuis `catalogue_astronomie.xlsx`.
- NFC optionnel pour echanger une variante en doublon contre une variante compatible.
- Onboarding local guide jusqu'a l'artisanat, avec explication des outils et de leurs couts.

Ne pousse rien depuis ce depot sans instruction explicite.
