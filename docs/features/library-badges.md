# Bibliotheque et badges

[← Index documentation](../README.md) | [Echange NFC](trade-nfc.md) | [Onboarding](../flows/new-player-onboarding.md)

## Bibliotheque

La bibliotheque affiche la collection locale persistante.

- Les cartes non obtenues restent visibles avec leur cadre et leurs informations, mais leur illustration est masquee.
- Les cartes obtenues peuvent etre ouvertes en apercu puis en plein ecran.
- Le selecteur de variantes permet de consulter les qualites de ciel et finitions possedees.
- Les nouvelles cartes peuvent afficher l'indicateur a trois etoiles pendant la premiere visite.
- Une variante avec au moins `2` copies peut proposer l'action `Échanger`.

## Variantes pedagogiques

Pendant l'onboarding, une modale de variantes explique les differences de ciel et de finition. Les cartes d'exemple sont tirees dans le vrai catalogue par rarete ; elles peuvent varier d'un joueur a l'autre.

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
