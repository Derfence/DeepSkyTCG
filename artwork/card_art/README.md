# Sources d'illustrations de cartes

[Documentation catalogue et assets](../../docs/catalog-assets.md)

Raw card art working files live here so they are not packaged into the Android APK.

Structure:
- `artwork/card_art/raw/<rawExtension>/<imageRef>.<ext>`
- `artwork/card_art/raw/<rawExtension>/sources_manifest.csv` (optional)

Typical workflow:
- Download raw candidates with `python3 scripts/download_card_art_sources.py`.
- Prepare runtime-ready `.webp` assets with `python3 scripts/prepare_card_art.py artwork/card_art/raw/<rawExtension> app/src/main/assets/card_art/<extensionId>`.
- If a raw folder name differs from the final runtime `extensionId`, always pass the explicit output directory to `prepare_card_art.py`.
- Keep the runtime credits directly in `app/src/main/assets/card_art/card_art_credits.json`.
- Export runtime credits from a raw manifest only if you actually keep one with `python3 scripts/export_card_art_credits.py artwork/card_art/raw/<rawExtension>/sources_manifest.csv <extensionId>`.
