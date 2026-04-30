# Onboarding nouveaux joueurs

[← Index documentation](../README.md) | [Tags UI](onboarding-ui-tags.md) | [Accueil](../features/home.md)

Le standalone guide le premier parcours joueur avec une intro obligatoire, des etapes guidees pack/bibliotheque/equipement/fabrication, puis des pauses silencieuses quand le parcours redevient libre.

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
  -> UseSkyDarkening
  -> Completed
```

Chaque etape est stockee dans la progression locale securisee et rejouee jusqu'a completion. Les actions hors parcours deviennent des no-op silencieux pendant les etapes bloquees, sans changement visuel. Les hints apparaissent seulement apres stabilisation des transitions.

## Introduction

- `ShowWelcomeIntro` affiche la modale bloquante `new-player-modal-welcome` quand l'accueil est visible et stabilise.
- Le bouton `Commencer` avance vers `OpenFirstPackMenu`.
- Aucun coachmark global n'est affiche tant que cette modale est active.

## Premier pack

- Accueil : coachmark sur `Ouvrir un pack`.
- Selection : coachmark sur le premier bouton `Observer`.
- Booster : coachmark apres la fin de l'introduction visuelle des quatre boosters.
- Le coachmark disparait des le clic pour ne pas suivre l'animation.
- Le tout premier pack contient uniquement des cartes `Common`.
- Aucune carte holographique, tamponnee ou d'equipement n'est autorisee.

## Retour apres premier pack

- `PackOpeningScreen` garde le hint vertical.
- Le premier nudge affiche `Glisse vers le haut pour revenir a l'accueil.`
- Au retour accueil, la priorite va a `Bibliotheque`.
- Si un badge est debloque, sa celebration est differee jusqu'a la fin de `ViewLibrary`.

## Bibliotheque puis badges

- Coachmark sur `Bibliotheque`.
- Transition par portail-livre frontal avec couverture, pages et decor d'etoiles.
- L'ouverture avance vers `LearnLibraryVariants`.
- La modale `new-player-modal-library-variants` bloque les interactions bibliotheque, l'echange et le retour.
- Apres `Terminer`, micro-hint local : `Touche une carte obtenue pour l'ouvrir.`
- Le micro-hint disparait apres environ `2.8 s`.
- Les cartes pedagogiques sont tirees aleatoirement dans le vrai catalogue par rarete.
- Les vignettes nouvelles gardent l'indicateur a trois etoiles pendant cette visite.
- Au retour accueil, la celebration de badge differee est rejouee si elle existe encore.
- Ensuite, un coachmark cible `Badges`.

## Pause libre puis equipement

`OpenSecondPackMenu` est silencieuse :

- pas de coachmark ;
- pas de blocage supplementaire ;
- `Home`, `Bibliotheque`, `Badges`, `Packs` et retour Android redeviennent normaux ;
- `Equipements` reste absent tant qu'aucune carte d'equipement n'a ete obtenue.

Le premier pack effectivement ouvert apres cette pause est le deuxieme tirage d'onboarding :

- cartes astronomiques plafonnees a `Uncommon` ;
- remplacements aleatoires d'equipement desactives ;
- exactement un slot remplace par une carte d'equipement `level == 1` avec `dropWeight > 0` ;
- un slot `Common` est privilegie, sinon le slot de plus faible rarete finale ;
- pas de carte holographique ni tamponnee.

Au retour accueil, le bouton `Equipements` devient visible et recoit un coachmark. Dans l'ecran, le coachmark cible le premier bouton `Activer` eligible. Si ce bouton est hors viewport, la bulle devient une fleche vers le bas jusqu'a ce que la cible soit visible.

## Chapitre fabrication

- La premiere activation reussie avance vers `AwaitCraftingEligibility`.
- `AwaitCraftingEligibility` est silencieuse.
- Quand `openedPackCount >= 3` et qu'une carte est eligible a `DarkenSky`, le coordinateur avance vers `ViewCraftingMenu`.
- Accueil : coachmark sur `Atelier de fabrication`.
- Atelier : guide le mode `Assombrir le ciel`, la premiere carte eligible, puis le bouton de confirmation.
- Pendant ce chapitre, seul `DarkenSky` est autorise.
- L'onboarding se termine apres une application reussie de `DarkenSky`.

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
