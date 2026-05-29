# Design visuel

[← Index documentation](README.md) | [Accueil](features/home.md) | [Catalogue et assets](catalog-assets.md)

Cette page sert de reference rapide pour les couleurs, figures et assets. Les valeurs viennent des fichiers Compose actuels ; en cas de conflit, le code fait foi.

## Palette de base

Source : `app/src/main/kotlin/dstcg/aumombelli/fr/ui/theme/Color.kt`.

| Token | Hex | Usage |
| --- | --- | --- |
| `MidnightBlue` | `#0C1424` | base sombre de marque |
| `AuroraTeal` | `#68E1D2` | accent froid, energie, highlights |
| `EmberGold` | `#F6B73C` | accent rare/premium |
| `Frost` | `#EAF3FF` | texte clair |
| `Mist` | `#9CB3C9` | texte secondaire |
| `Void` | `#06080D` | fond tres sombre |

## Raretes

Source : `ui/theme/CardRarity.kt`.

| Rarete | Couleur | Glow | Branches |
| --- | --- | --- | --- |
| `Common` | `#F6FBFF` | `#66FFFFFF` | 4 |
| `Uncommon` | `#6CCBFF` | `#664AA8FF` | 4 |
| `Rare` | `#FFD76A` | `#66FFB400` | 4 |
| `Epic` | `#C69BFF` | `#887A3DFF` | 6 |

## Qualites de ciel

| Code | Haut | Bas | Intention visuelle |
| --- | --- | --- | --- |
| `city` | `#8E845F` | `#3E382C` | ciel urbain lumineux |
| `suburban` | `#5F4A46` | `#211C24` | ciel periurbain |
| `rural` | `#123660` | `#08182C` | ciel profond bleu |
| `mountain` | `#061323` | `#010308` | ciel tres sombre |
| `holographic` | `#05070D` | `#000000` | effet premium cyan/rose |

## Equipements

Source : `ui/component/EquipmentCategoryColorTokens.kt`.

```text
Observatoire  (#63E0D7) -> Recharge
Telescope     (#F0CC6A) -> Holographie
Monture       (#FF9B7A) -> Promotion de rarete
```

| Type | Accent | Texte accent | Usage |
| --- | --- | --- | --- |
| `Observatoire` | `#63E0D7` | `#ABF7F0` | vitesse de recharge |
| `Telescope` | `#F0CC6A` | `#FFE7A6` | chance holographique |
| `Monture` | `#FF9B7A` | `#FFD2C2` | promotion de rarete |

## Assets et figures

- Logo Android : exports depuis `artwork/logo-concepts/17-badge-logo.svg`.
- Lockup sombre : `artwork/logo-concepts/19-badge-plus-texte-deep-sky-blanc.svg`.
- Logo blanc pour badges generaux : `app/src/main/assets/branding/21-badge-logo-blanc.svg`.
- Tampon de carte : `app/src/main/assets/branding/22-tampon.svg`.
- Illustrations runtime : `app/src/main/assets/card_art/**`.

Les cartes runtime utilisent le ratio `1024x1796`. La bibliotheque masque les illustrations non obtenues avec un placeholder Compose, pas avec `_fallbacks/missing.webp`.

## Lisibilite documentaire

- Une page = un sujet.
- Les anciennes pages longues renvoient vers leur page active.
- Les figures ASCII decrivent les flux et frontieres ; les couleurs exactes restent dans des tableaux.
- Les identifiants techniques restent en monospace pour faciliter la recherche dans le code.

[← Index documentation](README.md)
