Card art files are stored by extension, with one file per `imageRef`.

Structure:
- `card_art/<extensionId>/<imageRef>.webp`
- `card_art/_fallbacks/missing.webp`

Rules:
- Keep the filename exactly equal to the card `imageRef`.
- Keep the folder name exactly equal to the card `extensionId`.
- Use `.webp` files prepared at card ratio for predictable cropping.
- Add new extension folders next to the existing ones when new catalogs arrive.
