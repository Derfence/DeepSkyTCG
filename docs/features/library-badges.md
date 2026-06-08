# Bibliothèque et badges

[← Index documentation](../README.md) | [Échange NFC](trade-nfc.md) | [Onboarding](../flows/new-player-onboarding.md)

## Bibliothèque

La bibliothèque affiche la collection locale persistante.

- Les cartes non obtenues restent visibles avec leur cadre et leurs informations, mais leur illustration est masquée.
- Les cartes obtenues peuvent être ouvertes en aperçu puis en plein écran.
- Les filtres sont affichés entre la description de la bibliothèque et la liste des cartes.
- Les filtres `Extension`, `Rareté`, `Qualité du ciel` et `Échangeable` sont combinables.
- Dans chaque groupe catalogue, une option active remplace l'option déjà active du même groupe ; toucher l'option active la désactive.
- `Holographique` est une qualité du ciel, pas un groupe de filtre séparé.
- Un filtre de qualité du ciel masque les cartes non obtenues, car aucune variante possédée ne peut confirmer cette qualité.
- Quand `Échangeable` est combiné avec une qualité du ciel, seule une variante de cette qualité disponible en double rend la carte visible.
- L'aperçu réserve la place du sélecteur de variantes et de l'action d'échange, puis dimensionne la carte selon la largeur ou la hauteur encore disponible.
- La fiche détaillée dimensionne également la carte selon la largeur ou la hauteur disponible avant d'afficher sa description.
- Le sélecteur de variantes permet de consulter les qualités de ciel et finitions possédées.
- Les nouvelles cartes peuvent afficher l'indicateur à trois étoiles pendant la première visite.
- Une variante avec au moins `2` copies peut proposer l'action `Échanger`.

## Variantes pédagogiques

Pendant l'onboarding, une modale de variantes explique les différences de ciel et de finition. Les cartes d'exemple sont tirées dans le vrai catalogue par rareté ; elles peuvent varier d'un joueur à l'autre. Les visuels utilisent la largeur maximale permise par la hauteur disponible afin que chaque page reste entièrement visible.

## Badges

Le carnet de badges est accessible après le premier pack. Les badges couvrent notamment :

- premier pack ouvert ;
- pack amélioré volontairement opaque ;
- progression par qualité de ciel ;
- cartes tamponnées et holographiques tamponnées ;
- collection parfaite ;
- équipements activés et effets simultanés.

Les célébrations de badges peuvent être différées pendant l'onboarding, puis rejouées au retour sur l'accueil.

## Tests associés

- `LibraryViewModelTest`
- `LibraryScreenTest`
- `LibraryOnboardingVariantWalkthroughTest`
- `BadgeAssemblerTest`
- `BadgeBookViewModelTest`
- `BadgeBookScreenTest`
- `BadgeUnlockCelebrationOverlayTest`
- `BadgeCelebrationLayoutTest`

[← Index documentation](../README.md)
