# Accueil

La scene d'accueil est maintenant unique et remplace `Start` et `Main menu`.

Elle conserve :

- l'animation d'ouverture avec le logo au centre qui monte ensuite en haut de l'ecran ;
- le fond visuel historique de `Start` ;
- une grande carte centrale `Ouvrir un pack` ;
- une icone `Bibliotheque` en bas a gauche ;
- une icone `Equipements` en bas au centre ;
- une icone `Badges` en bas a droite ;
- un bouton `Parametres` en haut a droite.

## Equipements

Le bouton `Equipements` ouvre un ecran dedie depuis l'accueil principal.

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
