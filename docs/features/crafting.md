# Artisanat

[← Index documentation](../README.md) | [Bibliotheque](library-badges.md) | [Onboarding](../flows/new-player-onboarding.md)

L'artisanat transforme des doublons de cartes astronomiques en variantes plus avancees.

## Deblocage

Le menu `Atelier de fabrication` devient disponible depuis l'accueil quand :

- `openedPackCount >= 3` ;
- au moins une carte est eligible au mode `Assombrir le ciel`, ou l'onboarding a deja atteint le chapitre fabrication.

## Modes

| Mode | Effet | Cout |
| --- | --- | --- |
| `DarkenSky` / `Assombrir le ciel` | transforme une qualite de ciel vers la suivante | cout par qualite |
| `SpaceAgency` / `Agence spatiale` | transforme une finition standard en tamponnee | `10` copies |

Les couts de ciel viennent de `game_balance.json` si `skyUpgradeCosts` existe. Sinon, le fallback est :

- `city = 2`
- `suburban = 2`
- `rural = 3`
- `mountain = 6`

## Regles

- La carte cible garde le meme `cardId`.
- `DarkenSky` repart sur la finition standard de la qualite cible.
- Une qualite deja maximale n'est pas eligible.
- `SpaceAgency` accepte uniquement les finitions standard.
- L'application consomme les copies source puis ajoute `1` copie cible.
- Les candidats sont groupes par extension puis carte.

## Onboarding

Le chapitre fabrication guide seulement `Assombrir le ciel`, mais commence par `LearnCraftingTools`, une modale paginee de deux pages qui explique les deux outils et leurs couts :

- `Assombrir le ciel` : `Ville -> Periurbain = 2`, `Periurbain -> Campagne = 2`, `Campagne -> Montagne = 3`, `Montagne -> Holographique = 6`.
- `Agence spatiale` : `Standard -> Tamponnee = 10`.

Pendant `LearnCraftingTools`, la modale bloque l'Atelier. Pendant `ViewCraftingMenu` et `UseSkyDarkening`, le mode `DarkenSky` est le seul mode autorise pour la selection et l'application guidees.

## Tests associes

- `CraftingOperationsTest`
- `CraftingRepositoryTest`
- `CraftingViewModelTest`
- `CraftingScreenTest`
- `CraftingOnboardingToolsContentTest`
- `CraftingOnboardingToolsWalkthroughTest`
- `NewPlayerOnboardingCoordinatorTest`

[← Index documentation](../README.md)
