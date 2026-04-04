#!/usr/bin/env python3
"""Download raw card art candidates from Wikimedia Commons."""

from __future__ import annotations

import argparse
import csv
import html
import json
import re
import sys
import time
import unicodedata
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable
from urllib.parse import urlencode
from urllib.request import Request, urlopen


COMMONS_API_URL = "https://commons.wikimedia.org/w/api.php"
USER_AGENT = "DstcgCardArtDownloader/1.0 (https://github.com/openai)"
MIN_WIDTH = 1024
MIN_HEIGHT = 1797
DOWNLOAD_HEIGHT = 2200
REQUEST_DELAY_SECONDS = 0.2

PREFERRED_LICENSE_PATTERNS = (
    "cc0",
    "public domain",
    "pd",
    "cc by",
    "attribution",
)

REJECT_LICENSE_PATTERNS = (
    "cc by sa",
    "sharealike",
    "gfdl",
    "noncommercial",
    "no derivatives",
    "fair use",
)

NEGATIVE_TITLE_TOKENS = {
    "icon",
    "logo",
    "symbol",
    "diagram",
    "comparison",
    "light curve",
    "spectrum",
    "plot",
    "map",
    "chart",
    "finder",
    "animation",
    "video",
    "audio",
}

SEARCH_KEYWORD_OVERRIDES = {
    "double_cluster": ["double cluster perseus", "ngc 869 884"],
    "coathanger": ["coathanger cluster", "cr 399"],
    "aurora_borealis": ["aurora borealis sky", "northern lights"],
    "46p_wirtanen": ["46p wirtanen comet"],
    "halley_comet": ["halley comet"],
    "comet_neowise": ["comet neowise", "c/2020 f3 neowise"],
    "perseids_meteor_shower": ["perseids meteor shower"],
    "geminids_meteor_shower": ["geminids meteor shower"],
    "leonids_meteor_shower": ["leonids meteor shower"],
}

MANUAL_FILE_OVERRIDES = {
    "algol": {
        "title": "File:Algol.png",
        "allow_sharealike": True,
        "allow_undersized": True,
        "note": "Exception: CC BY-SA file below the requested minimum height; retained because no larger CC0 or CC BY image was found.",
    },
    "almach": {
        "title": "File:GammaAnd SIZE.png",
        "allow_sharealike": True,
        "allow_undersized": False,
        "note": "Exception: CC BY-SA schematic used because no larger CC0 or CC BY image was found for Almach.",
    },
    "ngc752_open_cluster": {
        "title": "File:NGC 752 60f GraXpert.jpg",
        "allow_sharealike": False,
        "allow_undersized": False,
        "note": "",
    },
}


@dataclass
class CardRow:
    image_ref: str
    common_name: str
    catalog_number: str
    extension_id: str
    object_family: str
    object_type: str


@dataclass
class Candidate:
    score: int
    query: str
    title: str
    source_url: str
    download_url: str
    mime: str
    width: int
    height: int
    license_short_name: str
    license_url: str
    artist: str
    credit: str
    note: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Download raw card art images from Wikimedia Commons for all cards in the catalog."
        )
    )
    parser.add_argument(
        "--catalog",
        type=Path,
        default=Path("catalogue_astronomie.csv"),
        help="Path to the source catalog CSV",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=Path("artwork/card_art/raw/astronomes-en-herbes"),
        help="Folder where the raw downloaded images will be stored",
    )
    parser.add_argument(
        "--limit",
        type=int,
        default=None,
        help="Optional limit for local testing",
    )
    return parser.parse_args()


def normalize_text(value: str) -> str:
    value = unicodedata.normalize("NFKD", value)
    value = "".join(ch for ch in value if not unicodedata.combining(ch))
    value = value.lower()
    value = re.sub(r"[^a-z0-9]+", " ", value)
    return " ".join(value.split())


def clean_metadata_value(raw: str | None) -> str:
    if not raw:
        return ""
    text = re.sub(r"<[^>]+>", " ", raw)
    text = html.unescape(text)
    return " ".join(text.split())


def read_cards(path: Path, limit: int | None) -> list[CardRow]:
    with path.open(newline="", encoding="utf-8-sig") as handle:
        rows = list(csv.DictReader(handle, delimiter=";"))

    cards = [
        CardRow(
            image_ref=row["imageRef"],
            common_name=row["commonName"],
            catalog_number=row["catalogNumber"],
            extension_id=row["extensionId"],
            object_family=row["objectFamily"],
            object_type=row["objectTypeLabel"],
        )
        for row in rows
        if row["rowType"] == "card"
    ]
    return cards[:limit] if limit else cards


def build_queries(card: CardRow) -> list[str]:
    base_queries: list[str] = []
    base_queries.extend(SEARCH_KEYWORD_OVERRIDES.get(card.image_ref, []))
    base_queries.append(card.image_ref.replace("_", " "))
    if card.catalog_number:
        base_queries.append(card.catalog_number)
        base_queries.append(f"{card.catalog_number} {card.image_ref.replace('_', ' ')}")
    if card.common_name:
        base_queries.append(card.common_name)
    if card.object_family == "constellation":
        base_queries.append(f"{card.image_ref.replace('_', ' ')} night sky")
    if card.object_family == "sky_event":
        base_queries.append(f"{card.image_ref.replace('_', ' ')} astrophotography")
    if card.object_family == "star":
        base_queries.append(f"{card.image_ref.replace('_', ' ')} star")

    seen: set[str] = set()
    deduped: list[str] = []
    for query in base_queries:
        clean = " ".join(query.split())
        if clean and clean not in seen:
            seen.add(clean)
            deduped.append(clean)
    return deduped


def fetch_json(params: dict[str, str]) -> dict:
    request = Request(
        f"{COMMONS_API_URL}?{urlencode(params)}",
        headers={"User-Agent": USER_AGENT},
    )
    with urlopen(request, timeout=30) as response:
        return json.load(response)


def iter_search_pages(query: str) -> Iterable[dict]:
    params = {
        "action": "query",
        "format": "json",
        "generator": "search",
        "gsrsearch": query,
        "gsrnamespace": "6",
        "gsrlimit": "12",
        "prop": "imageinfo",
        "iiprop": "url|mime|size|extmetadata",
        "iiurlheight": str(DOWNLOAD_HEIGHT),
    }
    payload = fetch_json(params)
    pages = payload.get("query", {}).get("pages", {})
    return pages.values()


def iter_exact_title_pages(title: str) -> Iterable[dict]:
    params = {
        "action": "query",
        "format": "json",
        "titles": title,
        "prop": "imageinfo",
        "iiprop": "url|mime|size|extmetadata",
        "iiurlheight": str(DOWNLOAD_HEIGHT),
    }
    payload = fetch_json(params)
    pages = payload.get("query", {}).get("pages", {})
    return pages.values()


def license_is_acceptable(license_short_name: str, allow_sharealike: bool = False) -> bool:
    normalized = normalize_text(license_short_name)
    if not normalized:
        return False
    if allow_sharealike and "cc by sa" in normalized:
        return True
    if any(normalize_text(token) in normalized for token in REJECT_LICENSE_PATTERNS):
        return False
    return any(normalize_text(token) in normalized for token in PREFERRED_LICENSE_PATTERNS)


def license_score(license_short_name: str) -> int:
    normalized = normalize_text(license_short_name)
    if "cc by sa" in normalized:
        return 55
    if "cc0" in normalized:
        return 100
    if "public domain" in normalized or normalized == "pd":
        return 95
    if "cc by" in normalized:
        return 85
    if "attribution" in normalized:
        return 80
    return 0


def score_candidate(card: CardRow, title: str, metadata_text: str, license_short_name: str, width: int, height: int) -> int:
    corpus = normalize_text(f"{title} {metadata_text}")
    score = license_score(license_short_name)

    image_tokens = normalize_text(card.image_ref.replace("_", " ")).split()
    common_tokens = normalize_text(card.common_name).split()
    catalog_tokens = normalize_text(card.catalog_number).split()

    for token in image_tokens:
        if token in corpus:
            score += 7
    for token in common_tokens:
        if token in corpus:
            score += 5
    for token in catalog_tokens:
        if token in corpus:
            score += 10

    if card.object_family == "constellation" and "constellation" in corpus:
        score += 12
    if card.object_family == "sky_event" and "meteor" in corpus:
        score += 10
    if "photo" in corpus or "astrophotography" in corpus:
        score += 5

    for negative in NEGATIVE_TITLE_TOKENS:
        if negative in corpus:
            score -= 30

    score += min(width, 6000) // 400
    score += min(height, 6000) // 400
    return score


def candidate_from_page(
    *,
    card: CardRow,
    page: dict,
    query: str,
    allow_sharealike: bool = False,
    allow_undersized: bool = False,
    note: str = "",
) -> Candidate | None:
    imageinfo = (page.get("imageinfo") or [None])[0]
    if not imageinfo:
        return None

    width = int(imageinfo.get("width") or 0)
    height = int(imageinfo.get("height") or 0)
    if (width < MIN_WIDTH or height < MIN_HEIGHT) and not allow_undersized:
        return None

    mime = imageinfo.get("mime") or ""
    if not mime.startswith("image/") or mime.endswith("svg+xml"):
        return None

    metadata = imageinfo.get("extmetadata") or {}
    license_short_name = clean_metadata_value(
        (metadata.get("LicenseShortName") or {}).get("value")
    )
    if not license_is_acceptable(license_short_name, allow_sharealike=allow_sharealike):
        return None

    download_url = imageinfo.get("thumburl") or imageinfo.get("url", "")
    download_width = int(imageinfo.get("thumbwidth") or width)
    download_height = int(imageinfo.get("thumbheight") or height)
    if (download_width < MIN_WIDTH or download_height < MIN_HEIGHT) and not allow_undersized:
        download_url = imageinfo.get("url", "")
        download_width = width
        download_height = height

    object_name = clean_metadata_value((metadata.get("ObjectName") or {}).get("value"))
    description = clean_metadata_value((metadata.get("ImageDescription") or {}).get("value"))
    artist = clean_metadata_value((metadata.get("Artist") or {}).get("value"))
    credit = clean_metadata_value((metadata.get("Credit") or {}).get("value"))
    metadata_text = " ".join([object_name, description, artist, credit])
    title = page.get("title", "")
    score = score_candidate(
        card=card,
        title=title,
        metadata_text=metadata_text,
        license_short_name=license_short_name,
        width=download_width,
        height=download_height,
    )
    if allow_sharealike:
        score -= 15
    if allow_undersized:
        score -= 20

    return Candidate(
        score=score,
        query=query,
        title=title,
        source_url=imageinfo.get("descriptionurl", ""),
        download_url=download_url,
        mime=mime,
        width=download_width,
        height=download_height,
        license_short_name=license_short_name,
        license_url=clean_metadata_value((metadata.get("LicenseUrl") or {}).get("value")),
        artist=artist,
        credit=credit,
        note=note,
    )


def select_candidate(card: CardRow) -> Candidate | None:
    best: Candidate | None = None

    override = MANUAL_FILE_OVERRIDES.get(card.image_ref)
    if override is not None:
        for page in iter_exact_title_pages(override["title"]):
            candidate = candidate_from_page(
                card=card,
                page=page,
                query=override["title"],
                allow_sharealike=bool(override["allow_sharealike"]),
                allow_undersized=bool(override["allow_undersized"]),
                note=str(override["note"]),
            )
            if candidate is not None:
                best = candidate
                break

    for query in build_queries(card):
        for page in iter_search_pages(query):
            candidate = candidate_from_page(
                card=card,
                page=page,
                query=query,
            )
            if candidate is None:
                continue
            if best is None or candidate.score > best.score:
                best = candidate

        time.sleep(REQUEST_DELAY_SECONDS)

    return best


def file_extension_from_mime(mime: str) -> str:
    if mime == "image/jpeg":
        return ".jpg"
    if mime == "image/png":
        return ".png"
    if mime == "image/tiff":
        return ".tif"
    if mime == "image/webp":
        return ".webp"
    return ".bin"


def download_file(url: str, destination: Path) -> None:
    request = Request(url, headers={"User-Agent": USER_AGENT})
    with urlopen(request, timeout=60) as response:
        destination.write_bytes(response.read())


def write_manifest_row(writer: csv.DictWriter, card: CardRow, candidate: Candidate | None, output_file: str, status: str, note: str) -> None:
    writer.writerow(
        {
            "imageRef": card.image_ref,
            "catalogNumber": card.catalog_number,
            "commonName": card.common_name,
            "objectFamily": card.object_family,
            "objectType": card.object_type,
            "query": candidate.query if candidate else "",
            "sourceTitle": candidate.title if candidate else "",
            "license": candidate.license_short_name if candidate else "",
            "licenseUrl": candidate.license_url if candidate else "",
            "artist": candidate.artist if candidate else "",
            "credit": candidate.credit if candidate else "",
            "width": candidate.width if candidate else "",
            "height": candidate.height if candidate else "",
            "sourcePage": candidate.source_url if candidate else "",
            "downloadUrl": candidate.download_url if candidate else "",
            "outputFile": output_file,
            "status": status,
            "note": note or (candidate.note if candidate else ""),
        }
    )


def main() -> None:
    args = parse_args()
    cards = read_cards(args.catalog, limit=args.limit)
    output_dir = args.output_dir.resolve()
    output_dir.mkdir(parents=True, exist_ok=True)
    manifest_path = output_dir / "sources_manifest.csv"

    with manifest_path.open("w", newline="", encoding="utf-8") as manifest_handle:
        writer = csv.DictWriter(
            manifest_handle,
            fieldnames=[
                "imageRef",
                "catalogNumber",
                "commonName",
                "objectFamily",
                "objectType",
                "query",
                "sourceTitle",
                "license",
                "licenseUrl",
                "artist",
                "credit",
                "width",
                "height",
                "sourcePage",
                "downloadUrl",
                "outputFile",
                "status",
                "note",
            ],
        )
        writer.writeheader()

        for index, card in enumerate(cards, start=1):
            print(f"[{index}/{len(cards)}] {card.image_ref}", flush=True)
            try:
                candidate = select_candidate(card)
            except Exception as exc:  # pragma: no cover - network-facing CLI tool
                write_manifest_row(
                    writer=writer,
                    card=card,
                    candidate=None,
                    output_file="",
                    status="error",
                    note=str(exc),
                )
                continue

            if candidate is None:
                write_manifest_row(
                    writer=writer,
                    card=card,
                    candidate=None,
                    output_file="",
                    status="missing",
                    note="No acceptable Wikimedia file found",
                )
                continue

            extension = file_extension_from_mime(candidate.mime)
            output_path = output_dir / f"{card.image_ref}{extension}"
            try:
                download_file(candidate.download_url, output_path)
            except Exception as exc:  # pragma: no cover - network-facing CLI tool
                write_manifest_row(
                    writer=writer,
                    card=card,
                    candidate=candidate,
                    output_file=str(output_path.name),
                    status="download_error",
                    note=str(exc),
                )
                continue

            write_manifest_row(
                writer=writer,
                card=card,
                candidate=candidate,
                output_file=output_path.name,
                status="downloaded",
                note="",
            )
            time.sleep(REQUEST_DELAY_SECONDS)

    print(f"Manifest written to {manifest_path}")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:  # pragma: no cover - CLI control flow
        sys.exit(130)
