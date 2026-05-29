# Catalogue et assets

[← Index documentation](README.md) | [Design visuel](visual-design.md) | [Packs](features/packs.md)

## Source catalogue

Le catalogue editable est `catalogue_astronomie.xlsx`.

Feuilles utilisees :

- `Catalogue` : extensions, cartes, raretes, multiplicateurs.
- `Donnees` : balance de jeu, dont `EquipmentChancePercent` en `A19/B19`.
- `Equipements` : cartes d'equipement data-driven.
- `Resultats` : feuille diagnostique legacy, ignoree par la synchronisation.

Synchronisation :

```bash
python3 scripts/catalog_sync.py apply --sheet catalogue_astronomie.xlsx
```

Le script lit le classeur et regenere :

- `app/src/main/assets/catalog/extensions.json`
- `app/src/main/assets/catalog/cards.json`
- `app/src/main/assets/catalog/variant_profiles.json`
- `app/src/main/assets/catalog/game_balance.json`
- `app/src/main/assets/catalog/equipment_cards.json`
- `app/src/main/assets/catalog/equipment_settings.json`

Il n'a pas de dependance Python externe pour le XLSX : `scripts/simple_xlsx.py` est embarque.

## Balance runtime

Valeurs actuelles dans `game_balance.json` :

- `cardsPerDraw = 5`
- `drawCooldownHours = 6.0`
- `percentUncommonPerDay = 30.0`
- `percentRarePerDay = 15.0`
- `percentEpicPerDay = 5.0`
- `percentStampedPerDay = 10.0`

Les probabilites finales ne sont pas copiees telles quelles depuis le classeur. L'application les recalcule dans `CatalogBalanceRuntimeCalculator` a partir des donnees brutes, des multiplicateurs de rarete et des variantes disponibles.

`skyUpgradeCosts` peut etre absent du JSON : l'application retombe alors sur `city=2`, `suburban=2`, `rural=3`, `mountain=6`.

## Illustrations de cartes

Sources de travail :

- `artwork/card_art/raw/<rawExtension>/<imageRef>.<ext>`
- `artwork/card_art/raw/equipements/<imageRef>.<ext>`

Assets embarques :

- `app/src/main/assets/card_art/<extensionId>/<imageRef>.webp`
- `app/src/main/assets/card_art/equipements/<imageRef>.webp`
- `app/src/main/assets/card_art/card_art_credits.json`
- `app/src/main/assets/card_art/_fallbacks/missing.webp`

Preparation d'un dossier complet :

```bash
python3 scripts/prepare_card_art.py artwork/card_art/raw/<rawExtension> app/src/main/assets/card_art/<extensionId>
```

Cas reel a connaitre : le dossier source historique utilise `astronomes-en-herbes`, tandis que l'asset runtime attendu utilise `astronomes-en-herbe`.

```bash
python3 scripts/prepare_card_art.py artwork/card_art/raw/astronomes-en-herbes app/src/main/assets/card_art/astronomes-en-herbe
```

Dans ce cas, il faut toujours passer la sortie explicite. Sinon le mode batch miroir garderait le nom du dossier source et produirait un chemin runtime incorrect.

Le script croppe au ratio carte, redimensionne en `1024x1796` et exporte en WebP. Il demande Pillow.

## Credits images

Les credits runtime sont dans `app/src/main/assets/card_art/card_art_credits.json`, indexes par `extensionId` puis `imageRef`.

Export optionnel depuis un manifest source :

```bash
python3 scripts/export_card_art_credits.py artwork/card_art/raw/<rawExtension>/sources_manifest.csv <extensionId>
```

Details complementaires :

- [artwork/card_art/README.md](../artwork/card_art/README.md)
- [app/src/main/assets/card_art/README.md](../app/src/main/assets/card_art/README.md)
- [artwork/logo-concepts/README.md](../artwork/logo-concepts/README.md)

[← Index documentation](README.md)
