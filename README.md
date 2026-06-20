# Deep Sky TCG - Client Android Standalone

Client Android natif et autonome de Deep Sky TCG. L'application fonctionne hors ligne : catalogue embarqué, progression locale chiffrée, tirage de packs, collection, badges, équipements, artisanat et échange Bluetooth sans serveur.

## Documentation

L'index principal est [docs/README.md](docs/README.md).

Lectures utiles :

- [Vue d'ensemble](docs/overview.md) : objectif produit, boucle de jeu et parcours.
- [Architecture](docs/architecture.md) : modules, scenes Compose, repositories et persistance.
- [Installation et tests](docs/setup-and-tests.md) : prérequis Android, commandes Gradle, CI et benchmarks.
- [Design visuel](docs/visual-design.md) : couleurs, raretés, qualités de ciel, équipements et assets de marque.
- [Catalogue et assets](docs/catalog-assets.md) : XLSX, JSON embarqués, scripts et illustrations.
- [Politique de confidentialité](POLITIQUE_DE_CONFIDENTIALITE.md) : document officiel sur les données locales, le NFC et l'absence de collecte.

Fonctionnalites :

- [Accueil](docs/features/home.md)
- [Packs et tirage local](docs/features/packs.md)
- [Météo et recharge](docs/features/weather-recharge.md)
- [Bibliothèque et badges](docs/features/library-badges.md)
- [Équipements](docs/features/equipment.md)
- [Artisanat](docs/features/crafting.md)
- [Échange Bluetooth](docs/features/trade-bluetooth.md)
- [Onboarding nouveaux joueurs](docs/new-player-onboarding.md)

## Démarrage rapide

Depuis Windows :

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Depuis WSL/Bash, utilise le wrapper Windows pour réutiliser le SDK local :

```bash
cmd.exe /c gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Prérequis principaux :

- Java 21
- Android SDK Platform 36.1
- Android SDK Build-Tools 36.1.0
- `local.properties` pointant vers le SDK Android Windows local

Les commandes détaillées sont dans [Installation et tests](docs/setup-and-tests.md).

## État fonctionnel actuel

- Version affichée dans l'application : `v2.7.8`.
- Application mono-profil, sans login, sans backend et sans publicité.
- Progression sécurisée dans DataStore avec chiffrement AES-GCM via Android Keystore.
- Stock local de `10` ouvertures, `5` cartes par pack et cooldown de `6 h` modulé par la météo UTC.
- Catalogue embarqué synchronisé depuis `catalogue_astronomie.xlsx`.
- Bluetooth LE pour échanger une variante en doublon contre une variante compatible, sans serveur ni appairage système.
- Onboarding local guidé jusqu'à l'artisanat, avec explication des outils et de leurs coûts.

Ne pousse rien depuis ce dépôt sans instruction explicite.
