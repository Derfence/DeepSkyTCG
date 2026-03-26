# Gatcha Client Android

Client Android natif du projet Gatcha.

## Objectif

Cette application permet au joueur de :

- vérifier la compatibilité catalogue avec le serveur au démarrage ;
- voir au lancement un logo animé devant un ciel étoilé choisi aléatoirement parmi les quatre qualités de ciel du jeu, puis apparaître le formulaire de connexion ;
- créer un compte ;
- se connecter ;
- traverser une transition animée du login vers le menu principal puis rejouer l'inverse au `logout` ;
- récupérer sa collection ;
- consulter sa bibliothèque de cartes ;
- traverser une transition animée de livre entre menu principal et bibliothèque ;
- ouvrir l'aperçu d'une carte possédée depuis la bibliothèque puis l'agrandir en plein écran ;
- ouvrir un pack par extension à travers une scène d'introduction animée ;
- visualiser une animation d'extension, choisir un booster parmi quatre boosters visuels, voir une explosion de rareté dense et prolongée, puis révéler les cartes qui montent depuis le bas de l'écran ;
- faire disparaître l'ouverture de pack par glissement vertical vers le haut pour revenir au menu ;
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
- `ui/motion/` : modèles de motion design, variantes de ciel, animations d'extension et composants de transition partagés.
- `ui/viewmodel/` : logique d'écran.
- `ui/screen/` : écrans Compose.
- `assets/catalog/` : `metadata.json`, `extensions.json`, `cards.json` et `variant_profiles.json`.

## Flux visuel et animations

- Le client utilise un décor céleste partagé entre login, menu principal, sélection d'extension et scènes de pack.
- Le lancement choisit aléatoirement une des quatre qualités de ciel existantes : `city`, `suburban`, `rural`, `mountain`.
- Chaque qualité de ciel fait varier la densité d'étoiles scintillantes du fond, ainsi que le premier plan :
  - `city` : grands immeubles ;
  - `suburban` : maisons denses ;
  - `rural` : deux ou trois maisons espacées ;
  - `mountain` : relief montagneux sans lumières d'horizon.
- La séquence de démarrage est la suivante :
  - fondu du logo au centre ;
  - montée lente du logo vers une position calculée dynamiquement entre le haut utile de l'écran et le formulaire, avec compensation de la barre de statut ;
  - fondu du formulaire de connexion au centre.
- Lors d'un login réussi, le formulaire disparaît d'abord en fondu ; ensuite seulement démarre la transition vers le menu principal.
- La transition `login -> menu` fait disparaître le premier plan, déplace les étoiles avec ce mouvement, éteint les lumières colorées en même temps que le décor lorsqu'elles existent, puis ramène le ciel vers le rendu de la situation `mountain` sans passer par un noir pur.
- Le `logout` depuis le menu principal joue exactement l'inverse de cette transition pour revenir au login.
- La transition `menu -> bibliothèque` fait disparaître le menu en fondu, fait monter depuis le bas un livre fermé qui s'ouvre pendant son déplacement, puis fait apparaître la bibliothèque en fondu ; le retour rejoue cette chorégraphie en sens inverse.
- Le livre de transition est rendu en Compose sans asset externe :
  - deux couvertures en parallélogrammes marron ;
  - deux blocs de pages plus fins en blanc cassé ;
  - une reliure centrale arrondie en demi-cercle ;
  - des textures simples sur les couvertures et les pages pour suggérer relief, tranches et matière.
- Le fond bleu sombre de la bibliothèque est inclus dans le même fondu que le contenu pour éviter toute apparition brutale.
- La transition `menu -> choix de l'extension` suit cette séquence :
  - disparition du menu en fondu ;
  - apparition en fondu de la nouvelle scène sans la liste ;
  - apparition du titre `Choisis l'extension à contempler.` ;
  - montée des blocs d'extension depuis le bas, comme une file qui s'arrête progressivement.
- Les blocs de sélection d'extension n'affichent plus l'identifiant technique de l'extension.
- Chaque bloc d'extension affiche à droite du titre un petit logo animé propre à l'extension ; son animation ne démarre qu'une fois le mouvement d'entrée du bloc terminé.
- Lors du choix d'une extension, le bloc sélectionné grandit pour transitionner lui-même vers la scène suivante :
  - le bouton d'entrée disparaît en fondu ;
  - le titre reste affiché en haut et centré ;
  - les quatre boosters visuels apparaissent un par un.
- Le retour arrière depuis cette scène rejoue la transition inverse sans coupure visuelle entre les deux états.
- Les quatre boosters affichés après choix d'extension sont purement visuels ; le tirage serveur reste unique et dépend uniquement de l'identifiant d'extension.
- Le booster choisi conserve son logo déjà animé, se recentre exactement sur la future zone d'ouverture et les trois autres disparaissent en fondu.
- Le passage vers l'ouverture conserve visuellement le même booster, sans relance du logo, avec fondu du reste de la scène.
- L'ouverture joue ensuite une explosion de rareté :
  - très dense, avec des étoiles dans toutes les directions ;
  - plus grandes et étalées dans le temps ;
  - émises depuis le centre du booster ;
  - suivant une trajectoire balistique de type projectile lancé vers le haut puis retombant ;
  - maintenues jusqu'à la fin de la descente du booster hors champ par le bas.
- Une carte holographique ajoute une pluie supplémentaire d'étoiles tombant depuis le haut de l'écran.
- Les cartes du pack n'apparaissent qu'après la fin complète de cette séquence, puis entrent depuis le bas de l'écran avant de prendre leur position de révélation.
- `Astronomes en herbe` dispose d'une animation dédiée :
  - le logo reprend la constellation de la Grande Casserole ;
  - aucun point ni trait n'apparaît avant le début de l'apparition du logo ;
  - les points apparaissent en même temps que les premiers traits qui les atteignent ;
  - les points disparaissent dès que le dernier trait qui les touche commence à s'effacer ;
  - au retour arrière, l'animation est jouée en sens inverse avec disparition synchronisée des points et des traits.
- Les positions et connexions du dessin d'`Astronomes en herbe` sont définies dans `ui/motion/AppMotion.kt`, et leur rendu dans `ui/motion/AppMotionComponents.kt`.
- Une miniature de bibliothèque reste atténuée tant que la carte n'est pas possédée.
- La bibliothèque trie les cartes d'une extension par rareté, de `Common` vers `Epic`.
- Les miniatures de bibliothèque gardent un rendu simplifié et n'affichent plus le texte central de catalogue ni la variante au centre.
- Un clic sur une carte possédée ouvre un aperçu centré au ratio de carte à collectionner ; un second clic ouvre le plein écran.
- La fermeture du plein écran ouvert depuis la bibliothèque revient directement à la grille, sans repasser par l'aperçu.
- L'écran d'ouverture de pack permet aussi d'ouvrir la carte révélée en plein écran.
- Le retour du pack vers le menu principal se fait par glissement vertical vers le haut.
- Les cartes d'aperçu de bibliothèque et les cartes révélées dans les packs utilisent un ratio fixe `hauteur / largeur = 1.754`.
- Le fond de carte dépend de la qualité du ciel (`city`, `suburban`, `rural`, `mountain`).
- Les cartes holographiques ajoutent une surcouche d'étoiles scintillantes.
- Le badge de rareté utilise un logo étoilé dédié :
  - `Common` : étoile blanche à 4 branches ;
  - `Uncommon` : étoile bleue à 4 branches ;
  - `Rare` : étoile or à 4 branches ;
  - `Epic` : étoile violette à 6 branches.
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

## Couverture de tests actuelle

- Les tests unitaires couvrent maintenant aussi :
  - la sélection déterministe d'une variante de ciel ;
  - la résolution des animations d'extension ;
  - la projection du dessin d'extension et la conservation de son orientation ;
  - la synchronisation d'apparition et de disparition des points de constellation avec les traits ;
  - le calcul de la rareté maximale et la détection holographique pour l'ouverture de pack ;
  - les nouveaux événements et états transitoires des ViewModels de login et de pack.
- Les tests Compose/instrumentés couvrent le splash de lancement, l'affichage du login, la constellation de la Grande Casserole, le retour arrière depuis une extension sélectionnée, la disparition des boosters non sélectionnés, l'ouverture de pack avec explosion, l'entrée des cartes depuis le bas et le retour par glissement vertical.
- Les tests E2E attendent désormais la séquence complète : login -> menu -> extension -> booster -> ouverture -> swipe haut -> bibliothèque.

## Commandes utiles

Depuis le dossier `client-android/` :

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:compileDebugAndroidTestKotlin
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
