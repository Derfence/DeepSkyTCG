# Logos et assets de marque

[Documentation design visuel](../../docs/visual-design.md)

Ces fichiers conservent les explorations et les assets retenus pour l'application. La palette de reference reste :

- `MidnightBlue` `#0C1424`
- `AuroraTeal` `#68E1D2`
- `EmberGold` `#F6B73C`
- `Frost` `#EAF3FF`

## Assets retenus et recents

```text
artwork/logo-concepts/
  17-badge-logo.svg                         -> icone et badge principal
  19-badge-plus-texte-deep-sky-blanc.svg    -> lockup sombre utilise au lancement
  21-badge-logo-blanc.svg                   -> logo blanc pour badges generaux
  22-tampon.svg                             -> tampon applique aux cartes
```

- `17-badge-logo.svg` : badge de marque retenu pour les exports Android et les usages compacts.
- `19-badge-plus-texte-deep-sky-blanc.svg` : lockup sombre branche dans l'interface de lancement.
- `21-badge-logo-blanc.svg` : version SVG blanche embarquee dans `app/src/main/assets/branding/21-badge-logo-blanc.svg`.
- `22-tampon.svg` : tampon final embarque dans `app/src/main/assets/branding/22-tampon.svg`.

Exports Android associes :

- `app/src/main/res/drawable-nodpi/logo_badge_17.png`
- `app/src/main/res/drawable-nodpi/ic_launcher_badge_17.png`
- `app/src/main/res/drawable-nodpi/logo_lockup_19.png`

## Anciens concepts exploratoires

Les fichiers `01` a `04` sont conserves comme historique de recherche visuelle. Ils ne sont plus la recommandation active.

### 1. `01-round-badge-border.svg`

Badge rond avec bordure doree. C'est la piste la plus solide pour une icone d'application Android ou une vignette de store, car la silhouette reste lisible meme en petit.

### 2. `02-round-orbit-no-border.svg`

Version ronde plus legere, sans bordure, centree sur l'idee d'orbite et d'energie cosmique. Bonne piste pour un ecran de lancement ou une variante plus premium.

### 3. `03-card-frame-emblem.svg`

Version non ronde inspiree d'une carte a collectionner. Elle met davantage en avant l'aspect `Trading Card Game` et peut aussi servir de base pour des badges, des packs ou une identite secondaire.

### 4. `04-wordmark-lockup.svg`

Version avec le nom complet. Elle fonctionne mieux pour un splash screen, la page Play Store, un header GitHub ou une ecran titre.
