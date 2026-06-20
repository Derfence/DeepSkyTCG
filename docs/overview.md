# Vue d'ensemble

[← Index documentation](README.md) | [Architecture](architecture.md) | [Fonctionnalités](features/home.md)

Deep Sky TCG est une application Android Compose offline. Elle remplace les flux compte/serveur par une progression locale chiffrée et par un catalogue embarqué.

## Boucle de jeu

```text
Accueil
  |
  v
Packs -> Ouverture -> Collection
                      |-- Bibliothèque -> Artisanat -> Bibliothèque
                      |                `-- Échange Bluetooth
                      `-- Badges

Ouverture -> Équipements -> Packs
```

Le joueur ouvre des packs, enrichit sa collection, consulte ses variantes, active des équipements, débloque des badges, fabrique des variantes avec ses doublons et peut échanger certaines cartes en Bluetooth LE.

## Contraintes produit

- Aucun login, logout, compte distant ou compatibilité client/serveur.
- Aucun appel réseau requis pour jouer.
- Un seul profil local par installation.
- La météo de recharge est déterministe à partir d'une date UTC de confiance.
- Le Bluetooth LE est optionnel : l'application reste utilisable sans support d'annonce BLE, hors échange de cartes.

## Parcours principal

1. Lancement avec animation de marque.
2. Accueil centre sur `Ouvrir un pack`.
3. Premier parcours guidé : pack, bibliothèque, badges, équipements, puis artisanat.
4. Boucle libre : packs, consultation, activation d'équipement, fabrication, échange Bluetooth.

## Données embarquées

Les données runtime viennent de `app/src/main/assets/catalog/` :

- `extensions.json`
- `cards.json`
- `variant_profiles.json`
- `game_balance.json`
- `equipment_cards.json`
- `equipment_settings.json`

Le classeur source est `catalogue_astronomie.xlsx`. La synchronisation est documentée dans [Catalogue et assets](catalog-assets.md).

[← Index documentation](README.md)
