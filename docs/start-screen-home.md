# Accueil

La scene `Start` affiche maintenant :

- aucun message de progression locale quand le chargement est sain ou seulement recupere ;
- un bouton `Réinitialiser la bibliothèque` toujours visible ;
- une confirmation avant reset avec validation activee apres 2 secondes ;
- un footer avec la version `v1.0` a gauche ;
- une zone `à propos` au centre ouvrant un panneau de credits.

## Reset

Le reset reste disponible depuis l'accueil, meme sans erreur prealable.

Le flux attendu est :

1. Appuyer sur `Réinitialiser la bibliothèque`.
2. Lire la confirmation.
3. Attendre 2 secondes pour que `Valider` devienne actif.
4. Valider ou annuler.

## Credits

Le panneau de credits peut s'ouvrir :

- par balayage vers le haut sur la zone `à propos` ;
- par tap sur la zone `à propos`.

Le panneau se ferme :

- par tap sur le fond ;
- par balayage vers le bas ;
- avec le bouton retour Android.

## Modifier Les Credits

La version affichee et le contenu du panneau sont centralises dans :

- `app/src/main/kotlin/dstcg/aumombelli/fr/feature/start/StartAboutContent.kt`

Pour changer les credits :

1. Modifier `StartFooterAppVersion` pour la version affichee.
2. Modifier `StartAboutSections` pour ajuster les titres et les lignes du panneau.

Les styles et comportements d'ouverture/fermeture vivent dans :

- `app/src/main/kotlin/dstcg/aumombelli/fr/feature/start/StartOverlays.kt`
