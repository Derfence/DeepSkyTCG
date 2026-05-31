# Bibliotheque et badges

[← Index documentation](../README.md) | [Echange NFC](trade-nfc.md) | [Onboarding](../flows/new-player-onboarding.md)

## Bibliotheque

La bibliotheque affiche la collection locale persistante.

- Les cartes non obtenues restent visibles avec leur cadre et leurs informations, mais leur illustration est masquee.
- Les cartes obtenues peuvent etre ouvertes en apercu puis en plein ecran.
- L'aperçu réserve la place du sélecteur de variantes et de l'action d'échange, puis dimensionne la carte selon la largeur ou la hauteur encore disponible.
- La fiche détaillée dimensionne également la carte selon la largeur ou la hauteur disponible avant d'afficher sa description.
- Le selecteur de variantes permet de consulter les qualites de ciel et finitions possedees.
- Les nouvelles cartes peuvent afficher l'indicateur a trois etoiles pendant la premiere visite.
- Une variante avec au moins `2` copies peut proposer l'action `Échanger`.

## Variantes pedagogiques

Pendant l'onboarding, une modale de variantes explique les différences de ciel et de finition. Les cartes d'exemple sont tirées dans le vrai catalogue par rareté ; elles peuvent varier d'un joueur à l'autre. Les visuels utilisent la largeur maximale permise par la hauteur disponible afin que chaque page reste entièrement visible.

## Badges

Le carnet de badges est accessible apres le premier pack. Les badges couvrent notamment :

- premier pack ouvert ;
- pack amélioré volontairement opaque ;
- progression par qualite de ciel ;
- cartes tamponnees et holographiques tamponnees ;
- collection parfaite ;
- equipements actives et effets simultanes.

Les celebrations de badges peuvent etre differees pendant l'onboarding, puis rejouees au retour sur l'accueil.

## Tests associes

- `LibraryViewModelTest`
- `LibraryScreenTest`
- `LibraryOnboardingVariantWalkthroughTest`
- `BadgeAssemblerTest`
- `BadgeBookViewModelTest`
- `BadgeBookScreenTest`
- `BadgeUnlockCelebrationOverlayTest`
- `BadgeCelebrationLayoutTest`

[← Index documentation](../README.md)
