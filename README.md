# Deep Sky Trading Card Game (Deep Sky TCG) Client Android Standalone

Client Android natif autonome du projet Deep Sky TCG.

## Objectif

Cette application fonctionne entierement hors ligne. Elle embarque le catalogue, gere une progression locale unique et ne depend d'aucun acces internet, d'aucun serveur et d'aucun flux d'authentification.

Au lancement, l'application :

- affiche le decor et les animations d'introduction existants ;
- ouvre l'ecran de demarrage avec un unique bouton `Commencer` ;
- enchaine vers le menu principal ;
- guide les nouveaux joueurs vers leur premier pack, leur bibliotheque puis leurs badges ;
- permet d'ouvrir des packs localement ;
- persiste la collection et le prochain tirage autorise ;
- gere un stock local de 10 ouvertures, recharge a raison d'une ouverture toutes les 6 heures.

## Stack technique

- Kotlin
- Jetpack Compose
- MVVM
- DataStore Preferences

## Architecture

- `MainActivity` : point d'entree Android.
- `DstcgApp` : facade stable qui instancie le shell Compose.
- `app/` : orchestration des scenes, etat global et transitions visuelles.
- `feature/start/` : ecran de demarrage et ViewModel du bouton `Commencer`.
- `feature/library/` : lecture de la collection locale, bibliotheque et apercus.
- `feature/packs/selection/` : choix d'extension, stock d'ouvertures, barre de recharge et lancement d'ouverture.
- `feature/packs/opening/` : reveal du pack, navigation locale et plein ecran.
- `app/NewPlayerOnboardingCoordinator.kt` : orchestration du guidage contextuel du premier parcours joueur.
- `data/ProgressRepository.kt` : persistance DataStore de `collection_json`, `available_draw_count` et `next_charge_at`.
- `data/CollectionRepository.kt` : chargement, sauvegarde et fusion locale de la collection.
- `data/LocalPackEngine.kt` : tirage local pondere, consommation des ouvertures et recharge par palier.
- `data/PackRepository.kt` : orchestration du tirage local puis persistance de la progression.
- `model/` : modeles de catalogue, collection, packs et progression locale.
- `assets/catalog/` : catalogue embarque (`extensions.json`, `cards.json`, `variant_profiles.json`).
- `ui/motion/LaunchLogoMark.kt` : composant de logo reutilisant les exports finaux de marque.
- `res/drawable-nodpi/` et `res/mipmap-anydpi-*/` : exports PNG et ressources launcher Android derives des logos valides.

## Flux utilisateur

- Demarrage : animation d'introduction puis carte `Commencer`.
- `Commencer -> menu principal` : transition visuelle conservee.
- `Menu -> packs` : ouverture d'un booster local sans appel reseau.
- `Menu -> packs -> bibliotheque -> badges` : premier parcours guide par coachmarks persistants, affiches apres stabilisation des transitions.
- `Menu -> bibliotheque` : consultation de la collection persistante.
- Dans la bibliotheque, les cartes non obtenues gardent leur cadre et leurs informations visibles, mais leur illustration reste masquee jusqu'a la premiere obtention.
- Retour Android depuis le menu principal : fermeture de l'activite.

Le standalone est mono-profil. Aucun ecran de login, de creation de compte, de compatibilite client/serveur ou de logout n'est present.

## Persistance locale

La progression locale contient uniquement :

- `collection_json` : la collection possedee ;
- `available_draw_count` : le nombre d'ouvertures actuellement disponibles ;
- `next_charge_at` : la prochaine date ISO rechargeant une ouverture quand le stock est inferieur a 10 ;
- `newPlayerOnboardingStep` : l'etape persistante du premier parcours joueur.

La collection locale ne porte plus de version de catalogue. Au premier lancement, l'application cree automatiquement une collection vide. Les anciennes sauvegardes contenant encore `collection.version` restent lisibles grace a la deserialisation tolerante, puis sont reecrites au prochain reset ou a la prochaine sauvegarde.

## Regles de jeu offline

- `cardsPerPack = 5`
- `drawCooldown = 6h`
- `maxStoredDraws = 10`
- tirage pondere des cartes par extension
- tirage pondere de `skyQuality` et `finish`

Le moteur local reutilise les profils de variantes embarques pour produire un resultat equivalent a un tirage serveur, mais entierement local.

## Animations et interface

Le standalone conserve :

- le decor celeste partage ;
- les transitions de scenes ;
- le badge de marque `17` pour l'icone Android ;
- le lockup `19` pour les contextes sombres de l'interface de lancement ;
- les animations de selection d'extension et d'ouverture de pack ;
- la bibliotheque, les apercus et le plein ecran des cartes ;
- le masquage volontaire des illustrations des cartes non obtenues dans la grille de bibliotheque ;
- le stock disponible et la recharge visible dans l'interface.

La seule simplification fonctionnelle voulue au demarrage est le remplacement du login par le bouton `Commencer`.

Les sources creatives de reference restent dans `artwork/logo-concepts/` :

- `17-badge-logo.svg` : badge seul, utilise pour l'icone applicative ;
- `18-badge-plus-texte.svg` : lockup clair de reference ;
- `19-badge-plus-texte-deep-sky-blanc.svg` : lockup sombre branche dans l'application.

## Tests

Les tests du standalone se lancent depuis `client-android-standalone/`.

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:compileDebugAndroidTestKotlin
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

Depuis Bash/WSL, preferer le wrapper Windows pour reutiliser le `sdk.dir` local :

```bash
cmd.exe /c gradlew.bat :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:assembleDebug
```

Commande de verification utilisee pour l'integration logo :

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:assembleDebug
```

La couverture actuelle verifie notamment :

- `StartViewModel`
- `ProgressRepository`
- `NewPlayerOnboardingCoordinator`
- `LocalPackEngine`
- `CollectionRepository`
- `PackRepository`
- `PackViewModel`
- le flux guide `menu -> packs -> bibliotheque -> badges`
- les ecrans Compose du flux `Commencer`
- le scenario offline `Commencer -> menu -> pack -> recharge -> bibliotheque`

La documentation du guidage des nouveaux joueurs est detaillee dans :

- `docs/new-player-onboarding.md`

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

Le catalogue source editable est maintenant le fichier racine `catalogue_astronomie.xlsx`.

Le classeur contient :

- `Catalogue` : les extensions, les cartes et le multiplicateur `cardRarityMultiplier` ;
- `Probabilites` : les parametres d'entree pour les probabilites de carte, de qualite du ciel et d'holographique ;
- `Resultats` : les probabilites et temps moyens recalcules ;
- `_Calibration` : cache technique masque permettant de conserver un cycle `export -> apply` stable a poids egaux.

Exporter ou recharger le classeur :

```bash
python3 scripts/catalog_sync.py export
python3 scripts/catalog_sync.py apply
```

Le script n'a pas besoin de dependance Python externe : le support XLSX est embarque dans `scripts/simple_xlsx.py`.

Les tests du pipeline catalogue peuvent etre lances avec :

```bash
python3 -m unittest discover -s scripts -p 'test_*.py'
```

Toute evolution du format de persistance securisee doit continuer a passer par `ProgressSnapshot.schemaVersion`.
