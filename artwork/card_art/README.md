Raw card art working files live here so they are not packaged into the Android APK.

Structure:
- `artwork/card_art/raw/<rawExtension>/<imageRef>.<ext>`
- `artwork/card_art/raw/<rawExtension>/sources_manifest.csv`

Typical workflow:
- Download raw candidates with `python3 scripts/download_card_art_sources.py`.
- Prepare runtime-ready `.webp` assets with `python3 scripts/prepare_card_art.py artwork/card_art/raw`.
- If a raw folder name differs from the final runtime `extensionId`, pass an explicit output directory to `prepare_card_art.py`.
- Export runtime credits with `python3 scripts/export_card_art_credits.py artwork/card_art/raw/<rawExtension>/sources_manifest.csv <extensionId>`.
