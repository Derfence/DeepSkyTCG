# Onboarding nouveaux joueurs

[← Index documentation](../README.md) | [Tags UI](onboarding-ui-tags.md) | [Accueil](../features/home.md)

Le standalone guide le premier parcours joueur avec une intro obligatoire, des etapes guidees pack/bibliotheque/equipement/fabrication, une conclusion obligatoire, puis des pauses silencieuses quand le parcours redevient libre.

Aster, la mascotte de guidage, accompagne uniquement certaines boîtes de texte d'onboarding : les modales d'introduction et de conclusion, et les bulles de coachmark hors scène Équipement. Elle reste décorative, ne prend pas le focus accessibilité, ne bloque pas les interactions et est toujours rendue au premier plan quand elle apparaît. Sa taille est multipliée par `1.5` par rapport au gabarit initial, avec un multiplicateur local `2x` dans les deux modales centrées. Dans ces modales, la boîte de dialogue et Aster partagent le même centre horizontal. La taille d'Aster dépend de la largeur et de la hauteur disponibles, puis les deux éléments sont distribués verticalement. Elle a deux mains ouvertes dans l'introduction, les mains cartes et télescope dans la conclusion, puis elle est en bas à droite pour les coachmarks, sauf pour le coachmark d'ouverture de l'Atelier où elle passe en bas à gauche avec cheveux et main en miroir horizontal. Elle utilise le télescope sur `HomeEquipment`, la clé à molette sur le chapitre Atelier et le pointage sur la découverte des mini-jeux. Elle est masquée pendant les transitions, les célébrations de badge, la modale d'explication des variantes, la scène Équipement, les hints sans bulle de texte et les pauses silencieuses `OpenSecondPackMenu`, `AwaitCraftingEligibility` et `Completed`.

## Sequence persistante

```text
ShowWelcomeIntro
  -> OpenFirstPackMenu
  -> SelectFirstExtension
  -> SelectFirstBooster
  -> ViewLibrary
  -> LearnLibraryVariants
  -> ViewBadges
  -> OpenSecondPackMenu
  -> ViewEquipmentMenu
  -> ActivateFirstEquipment
  -> AwaitCraftingEligibility
  -> ViewCraftingMenu
  -> LearnCraftingTools
  -> UseSkyDarkening
  -> DiscoverMiniGames
  -> ShowConclusion
  -> Completed
```

Chaque etape est stockee dans la progression locale securisee et rejouee jusqu'a completion. Les actions hors parcours deviennent des no-op silencieux pendant les etapes bloquees, sans changement visuel. Les hints apparaissent seulement apres stabilisation des transitions.

## Introduction

- `ShowWelcomeIntro` affiche la modale bloquante `new-player-modal-welcome` quand l'accueil est visible et stabilise.
- Aster apparaît dans la modale en version compacte, centrée sous le texte, avec deux mains ouvertes et un multiplicateur local `2x`.
- Le texte presente Aster comme guide du parcours.
- Le bouton `Commencer` avance vers `OpenFirstPackMenu`.
- Aucun coachmark global n'est affiche tant que cette modale est active.

## Premier pack

- Accueil : coachmark sur `Ouvrir un pack`.
- Aster apparait avec la bulle de coachmark, avec l'index pointe vers le guidage quand l'espace le permet.
- La bulle `HomeOpenPack` est placee par-dessus la zone de texte de la grande carte centrale.
- Selection : coachmark sur le premier bouton `Observer`.
- Booster : coachmark apres la fin de l'introduction visuelle des quatre boosters.
- Le coachmark disparait des le clic pour ne pas suivre l'animation.
- Le tout premier pack contient uniquement des cartes `Common`.
- Aucune carte holographique, tamponnee ou d'equipement n'est autorisee.

## Retour apres premier pack

- `PackOpeningScreen` garde le hint vertical.
- Aster ne s'affiche pas pendant ce hint, car il ne s'agit pas d'une boite de texte de coachmark.
- Le premier nudge affiche `Glisse vers le haut pour revenir a l'accueil.`
- Au retour accueil, la priorite va a `Bibliotheque`.
- Si un badge est debloque, sa celebration est differee jusqu'a la fin de `ViewLibrary`.

## Bibliotheque puis badges

- Coachmark sur `Bibliotheque`.
- Aster accompagne ce coachmark avec sa main cartes.
- Transition par portail-livre frontal avec couverture, pages et decor d'etoiles.
- L'ouverture avance vers `LearnLibraryVariants`.
- La modale `new-player-modal-library-variants` bloque les interactions bibliotheque, l'echange et le retour.
- Aster ne s'affiche pas dans la modale de variantes, pour laisser toute la place aux exemples de cartes.
- Les exemples utilisent la largeur maximale permise par la hauteur disponible, afin que chaque page reste entièrement visible.
- Apres `Terminer`, micro-hint local : `Touche une carte obtenue pour l'ouvrir.`
- Le micro-hint disparait apres environ `2.8 s`.
- Les cartes pedagogiques sont tirees aleatoirement dans le vrai catalogue par rarete.
- Les vignettes nouvelles gardent l'indicateur a trois etoiles pendant cette visite.
- Au retour accueil, la celebration de badge differee est rejouee si elle existe encore.
- Ensuite, un coachmark cible `Badges`.

## Pause libre puis equipement

`OpenSecondPackMenu` est silencieuse :

- pas de coachmark ;
- pas d'Aster, pour signaler que la pause est volontairement libre ;
- pas de blocage supplementaire ;
- `Home`, `Bibliotheque`, `Badges`, `Packs` et retour Android redeviennent normaux ;
- `Equipements` reste absent tant qu'aucune carte d'equipement n'a ete obtenue.

Le premier pack effectivement ouvert apres cette pause est le deuxieme tirage d'onboarding :

- cartes astronomiques plafonnees a `Uncommon` ;
- remplacements aleatoires d'equipement desactives ;
- exactement un slot remplace par une carte d'equipement `level == 1` avec `dropWeight > 0` ;
- un slot `Common` est privilegie, sinon le slot de plus faible rarete finale ;
- pas de carte holographique ni tamponnee.

Au retour accueil, le bouton `Equipements` devient visible et recoit un coachmark avec Aster et sa main telescope. Dans l'ecran, le coachmark cible le premier bouton `Activer` eligible, sans Aster dans la scene Equipement. Si ce bouton est hors viewport, la bulle devient une fleche vers le bas jusqu'a ce que la cible soit visible.

## Chapitre fabrication

- La premiere activation reussie avance vers `AwaitCraftingEligibility`.
- `AwaitCraftingEligibility` est silencieuse et sans Aster jusqu'a ce que la fabrication soit eligible.
- Pendant cette attente, la sortie d'une ouverture de pack revient à la sélection des extensions pour permettre d'enchaîner les tirages. Un nouveau badge conserve sa priorité et ramène temporairement à l'accueil pour afficher sa célébration.
- Quand `openedPackCount >= 3` et qu'une carte est eligible a `DarkenSky`, le coordinateur avance vers `ViewCraftingMenu`.
- Dès que cette condition est remplie, la sortie de l'ouverture revient à l'accueil pour reprendre le guidage vers l'Atelier.
- Accueil : coachmark sur `Atelier de fabrication`, avec Aster en bas a gauche.
- L'ouverture de l'Atelier avance vers `LearnCraftingTools`.
- La modale `new-player-modal-crafting-tools` explique les deux outils avant tout coachmark dans l'Atelier.
- Page `Assombrir le ciel` : couts explicites `Ville -> Periurbain : 2`, `Periurbain -> Campagne : 2`, `Campagne -> Montagne : 3`, `Montagne -> Holographique : 6`.
- Page `Agence spatiale` : cout explicite `Standard -> Tamponnee : 10`.
- Apres `Terminer`, le parcours avance vers `UseSkyDarkening`.
- Atelier : guide alors le mode `Assombrir le ciel`, la premiere carte eligible, puis le bouton de confirmation.
- Aster utilise sa main cle a molette pendant le chapitre fabrication.
- Aster est absente de la modale d'explication des outils, comme dans la modale des variantes de bibliotheque.
- Pendant ce chapitre, seul `DarkenSky` est autorise.
- Une application reussie de `DarkenSky` avance vers `DiscoverMiniGames`, debloque `miniGamesMenuUnlocked` et active l'indicateur de nouveaute `miniGames`.
- Le retour vers l'accueil redevient possible pour permettre au joueur de quitter l'Atelier.

## Decouverte des mini-jeux

- `DiscoverMiniGames` affiche un coachmark sur la grande carte Home, cible `HomeMiniGames`.
- Le message explique que la carte a un verso mini-jeux, accessible par bouton ou swipe, puis par clic sur la carte.
- Tant que la carte Home affiche encore son recto pack, le bouton de retournement pulse et une main décorative indique le geste horizontal.
- Le message indique le plafond de recompense : quatre jeux pouvant reduire la recharge d'un pack jusqu'a `4 h` par jour.
- Pendant cette etape, seul l'acces au menu mini-jeux est autorise depuis l'accueil.
- L'ouverture du menu mini-jeux avance vers `ShowConclusion`, mais la conclusion ne s'affiche pas dans le menu.
- Le joueur n'est pas force a revenir : bouton retour et Back Android gardent le controle du retour Home.

## Conclusion

- `ShowConclusion` affiche la modale bloquante `new-player-modal-conclusion` une fois l'accueil retrouve apres la visite du menu mini-jeux, sur le meme format que l'introduction.
- Aster apparait dans la modale en version compacte, en bas centre, avec une main cartes, une main telescope et un multiplicateur local `2x`.
- Le bouton `Terminer` avance vers `Completed`.
- `Completed` ne montre plus de boite, de coachmark, ni d'Aster.

## Persistance et reprise

- Champ persiste : `newPlayerOnboardingStep`.
- Un reset remet l'etape a `ShowWelcomeIntro`.
- Anciennes sauvegardes sans champ :
  - collection vide et `openedPackCount == 0` : `ShowWelcomeIntro` ;
  - collection non vide ou `openedPackCount > 0` : `Completed`.
- Entre le premier pack et `LearnLibraryVariants`, le guidage reprend depuis l'etape persistante.
- Pendant les chapitres equipement ou fabrication, le guidage reprend sur l'etape sauvegardee.
- `AwaitCraftingEligibility` reste silencieuse jusqu'a l'eligibilite de fabrication.

[← Index documentation](../README.md)
