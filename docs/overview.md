# Vue d'ensemble

[← Index documentation](README.md) | [Architecture](architecture.md) | [Fonctionnalites](features/home.md)

Deep Sky TCG est une application Android Compose offline. Elle remplace les flux compte/serveur par une progression locale chiffree et par un catalogue embarque.

## Boucle de jeu

```text
Accueil
  |
  v
Packs -> Ouverture -> Collection
                      |-- Bibliotheque -> Artisanat -> Bibliotheque
                      |                `-- Echange NFC
                      `-- Badges

Ouverture -> Equipements -> Packs
```

Le joueur ouvre des packs, enrichit sa collection, consulte ses variantes, active des equipements, debloque des badges, fabrique des variantes avec ses doublons et peut echanger certaines cartes en NFC.

## Contraintes produit

- Aucun login, logout, compte distant ou compatibilite client/serveur.
- Aucun appel reseau requis pour jouer.
- Un seul profil local par installation.
- La meteo de recharge est deterministe a partir d'une date UTC de confiance.
- Le NFC est optionnel : l'application reste utilisable sans puce NFC.

## Parcours principal

1. Lancement avec animation de marque.
2. Accueil centre sur `Ouvrir un pack`.
3. Premier parcours guide : pack, bibliotheque, badges, equipements, puis artisanat.
4. Boucle libre : packs, consultation, activation d'equipement, fabrication, echange NFC.

## Donnees embarquees

Les donnees runtime viennent de `app/src/main/assets/catalog/` :

- `extensions.json`
- `cards.json`
- `variant_profiles.json`
- `game_balance.json`
- `equipment_cards.json`
- `equipment_settings.json`

Le classeur source est `catalogue_astronomie.xlsx`. La synchronisation est documentee dans [Catalogue et assets](catalog-assets.md).

[← Index documentation](README.md)
