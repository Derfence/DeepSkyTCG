#!/usr/bin/env python3
"""Export card art credits from a raw sources manifest to the runtime JSON asset."""

from __future__ import annotations

import argparse
import csv
import json
import re
from pathlib import Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Build card_art_credits.json from a raw sources_manifest.csv file and an extension id."
        )
    )
    parser.add_argument("manifest", type=Path, help="Path to sources_manifest.csv")
    parser.add_argument("extension_id", help="Target extension id in the output JSON")
    parser.add_argument(
        "output",
        nargs="?",
        type=Path,
        default=Path("app/src/main/assets/card_art/card_art_credits.json"),
        help="Output JSON path",
    )
    return parser.parse_args()


def clean_value(value: str | None) -> str | None:
    if value is None:
        return None
    normalized = " ".join(value.split())
    normalized = re.sub(r"\[\s*\d+\s*\]", "", normalized).strip()
    return normalized or None


def load_existing_catalog(path: Path) -> dict[str, dict[str, dict[str, str]]]:
    if not path.is_file():
        return {}
    return json.loads(path.read_text(encoding="utf-8"))


def build_extension_credits(manifest_path: Path) -> dict[str, dict[str, str]]:
    with manifest_path.open(newline="", encoding="utf-8") as handle:
        rows = list(csv.DictReader(handle))

    credits: dict[str, dict[str, str]] = {}
    for row in rows:
        image_ref = row["imageRef"]
        artist = clean_value(row.get("artist"))
        credit = clean_value(row.get("credit"))
        license_name = clean_value(row.get("license"))
        source_page = clean_value(row.get("sourcePage"))

        entry = {
            key: value
            for key, value in {
                "artist": artist,
                "credit": credit,
                "license": license_name,
                "sourcePage": source_page,
            }.items()
            if value is not None
        }
        credits[image_ref] = entry

    return dict(sorted(credits.items()))


def main() -> None:
    args = parse_args()
    catalog = load_existing_catalog(args.output)
    catalog[args.extension_id] = build_extension_credits(args.manifest)

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(dict(sorted(catalog.items())), indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    print(f"Wrote {args.output}")


if __name__ == "__main__":
    main()
