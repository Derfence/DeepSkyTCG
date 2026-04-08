# Accueil

La scene d'accueil est maintenant unique et remplace `Start` et `Main menu`.

Elle conserve :

- l'animation d'ouverture avec le logo au centre qui monte ensuite en haut de l'ecran ;
- le fond visuel historique de `Start` ;
- une grande carte centrale `Ouvrir un pack` ;
- une icone `Bibliotheque` en bas a gauche ;
- une icone `Equipements` en bas au centre, debloquee apres la premiere carte d'equipement obtenue ;
- une icone `Badges` en bas a droite ;
- un bouton `Parametres` en haut a droite.

## Layout Responsive

La mise en page verticale de `Home` repose maintenant sur une geometrie partagee entre l'ecran d'accueil et l'animation de lancement.

La source de verite vit dans :

- `app/src/main/kotlin/dstcg/aumombelli/fr/feature/home/HomeResponsiveLayout.kt`

Les choix actuels sont les suivants :

- taille du badge du logo d'arrivee : `16%` de la hauteur utile, bornee entre `96.dp` et `124.dp` ;
- padding haut du logo : `2%` de la hauteur utile, borne entre `8.dp` et `18.dp` ;
- taille des boutons de menu du bas : `10.5%` de la hauteur utile, bornee entre `64.dp` et `78.dp` ;
- largeur de la carte centrale : minimum entre `82%` de la largeur utile, `37%` de la hauteur utile, la place verticale libre convertie avec le ratio de la carte, puis `320.dp`.

La carte `Ouvrir un pack` garde toujours le ratio `TRADING_CARD_WIDTH_OVER_HEIGHT`.

Son placement vertical ne depend plus d'un simple centrage global :

- on calcule d'abord le bas reel du logo affiche ;
- on calcule ensuite le haut reel de la ligne des boutons du bas ;
- la carte est placee exactement au milieu entre ces deux bornes.

Ce choix permet :

- d'eviter une carte trop haute sur les ecrans compacts comme le Pixel 2 ;
- d'agrandir legerement le logo, la carte et les boutons sur des ecrans plus hauts comme le Pixel 8a ;
- de garder une scene visuellement equilibree sans casser l'animation de lancement.

## Transition Initiale

L'animation d'ouverture reutilise les memes mesures responsives que l'ecran final pour eviter tout decalage entre le badge de depart et le lockup final du `Home`.

Concretement :

- `HomeScreen` remonte la position verticale du centre du badge et sa taille d'arrivee ;
- `AppSceneHost` s'en sert pour calculer la cible du logo anime au lieu d'utiliser une valeur fixe.

Les points d'integration sont :

- `app/src/main/kotlin/dstcg/aumombelli/fr/feature/home/HomeScreen.kt`
- `app/src/main/kotlin/dstcg/aumombelli/fr/app/AppSceneHost.kt`
- `app/src/main/kotlin/dstcg/aumombelli/fr/app/AppSceneState.kt`

## Non Regression

La non regression de cette geometrie est couverte par :

- `app/src/test/kotlin/dstcg/aumombelli/fr/HomeResponsiveLayoutTest.kt` pour les calculs purs ;
- `app/src/androidTest/kotlin/dstcg/aumombelli/fr/HomeScreenResponsiveLaunchTest.kt` pour la position finale apres la transition de lancement ;
- `app/src/androidTest/kotlin/dstcg/aumombelli/fr/HomeScreenStateTest.kt` pour l'adaptation de taille de la carte selon le viewport.

## Equipements

Le bouton `Equipements` apparait uniquement apres l'obtention de la premiere carte d'equipement, puis ouvre un ecran dedie depuis l'accueil principal.

L'ecran affiche :

- un resume des effets actifs ;
- trois sections fixes `Observatoire`, `Telescope` et `Monture` ;
- les cartes possedees par type, avec leur niveau, leur bonus, `Stock xN` et `Activations totales : N` ;
- le dernier equipement utilise par type quand il existe.

Une seule carte peut etre active a la fois pour un type donne. Les trois types peuvent cependant coexister.

La version n'est plus affichee sur l'ecran d'accueil.
Elle reste visible dans la section `A propos`.

## Parametres

Le menu ancre en haut a droite contient :

- `Réinitialiser la bibliothèque`
- `A propos`

## Reset

Le reset reste disponible depuis l'accueil, meme sans erreur prealable.

Le flux attendu est :

1. Ouvrir `Parametres`.
2. Appuyer sur `Réinitialiser la bibliothèque`.
3. Lire la confirmation.
4. Attendre 2 secondes pour que `Valider` devienne actif.
5. Valider ou annuler.

## Credits

Le panneau `A propos` s'ouvre depuis `Parametres`.

Le panneau se ferme :

- par tap sur le fond ;
- par balayage vers le bas ;
- avec le bouton retour Android.

## Modifier Les Credits

La version affichee et le contenu du panneau sont centralises dans :

- `app/src/main/kotlin/dstcg/aumombelli/fr/feature/home/HomeAboutContent.kt`

Pour changer les credits :

1. Modifier `HomeAboutAppVersion` pour la version affichee.
2. Modifier `HomeAboutSections` pour ajuster les titres et les lignes du panneau.

Les styles et comportements d'ouverture/fermeture vivent dans :

- `app/src/main/kotlin/dstcg/aumombelli/fr/feature/home/HomeOverlays.kt`
