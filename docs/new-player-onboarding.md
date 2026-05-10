# Onboarding nouveaux joueurs

[← Index documentation](README.md) | [Parcours actif](flows/new-player-onboarding.md) | [Tags UI](flows/onboarding-ui-tags.md)

Cette page reste la porte d'entree stable pour l'onboarding. Elle fait partie de la refonte et garde le chemin historique `docs/new-player-onboarding.md`, pendant que le detail vivant est reparti en fichiers plus petits.

## A lire ici

| Besoin | Page |
| --- | --- |
| Comprendre le parcours joueur de bout en bout | [Parcours actif](flows/new-player-onboarding.md) |
| Retrouver les tags Compose, modales et tests | [Tags UI](flows/onboarding-ui-tags.md) |
| Comprendre les ecrans traverses | [Accueil](features/home.md), [Packs](features/packs.md), [Bibliotheque et badges](features/library-badges.md), [Equipements](features/equipment.md), [Artisanat](features/crafting.md) |

## Figure de parcours

```text
Accueil
  -> Packs
  -> Ouverture
  -> Bibliotheque
  -> Badges
  -> Pause libre
  -> Equipements
  -> Artisanat
  -> Mini-jeux
  -> Conclusion
  -> Completed
```

## Ce que le parcours garantit

- une intro bloquante au premier lancement ;
- un premier pack simple, sans equipement, holo ni tampon ;
- une visite bibliotheque avec pedagogie des variantes ;
- une celebration de badge differee tant que le joueur doit d'abord voir sa bibliotheque ;
- une pause libre apres `Badges`, puis un deuxieme tirage garantissant une carte d'equipement ;
- une activation d'equipement guidee ;
- une pause jusqu'a l'eligibilite de l'artisanat, une explication des deux outils et de leurs couts, puis le guidage `DarkenSky` ;
- une decouverte du verso mini-jeux de la carte Home, puis une conclusion affichee seulement apres retour au Home.

## Decoupage refondu

Cette page historique a ete scindee pour garder des fichiers plus lisibles :

- [flows/new-player-onboarding.md](flows/new-player-onboarding.md) decrit le parcours, les pauses silencieuses, les reprises et les regles de tirage speciales.
- [flows/onboarding-ui-tags.md](flows/onboarding-ui-tags.md) liste les tags Compose, cibles de coachmark, modales et tests associes.

Les changements deja presents dans cette page ont ete conserves dans ces deux nouvelles pages.

[← Index documentation](README.md)
