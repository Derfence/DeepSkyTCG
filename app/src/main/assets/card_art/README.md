Card art files are stored by extension, with one file per `imageRef`.
This folder is part of the shipped Android assets, so it should only contain runtime-ready files.

Structure:
- `card_art/<extensionId>/<imageRef>.webp`
- `card_art/card_art_credits.json`
- `card_art/_fallbacks/missing.webp`

Rules:
- Keep the filename exactly equal to the card `imageRef`.
- Keep the folder name exactly equal to the card `extensionId`.
- Use `.webp` files prepared at card ratio for predictable cropping.
- Add new extension folders next to the existing ones when new catalogs arrive.

Preparation:
- Use `python3 scripts/prepare_card_art.py <source-image> <card_art/<extensionId>/<imageRef>.webp>`.
- Store raw working files outside the Android module under `artwork/card_art/raw/`.
- Use `python3 scripts/prepare_card_art.py artwork/card_art/raw` to process every image found in the raw subfolders and write the `.webp` files into `app/src/main/assets/card_art/`.
- In batch mode, the script mirrors the raw subfolder names as-is. If a raw subfolder name differs from the final asset folder name, pass the explicit output destination for that subfolder.
- The script center-crops the image to the card ratio, keeps the shortest side fully preserved, resizes to `1024x1796`, and exports to `.webp`.
- Any input format supported by Pillow can be used (`.jpg`, `.jpeg`, `.png`, `.webp`, and similar).

Credits:
- Store runtime image credits in `card_art/card_art_credits.json`, keyed by `extensionId` and then `imageRef`.
- Export credits from a raw manifest with `python3 scripts/export_card_art_credits.py artwork/card_art/raw/<rawExtension>/sources_manifest.csv <extensionId>`.
- The enlarged card always shows `Credit image`, with `Inconnu` as a fallback when no artist is available.
