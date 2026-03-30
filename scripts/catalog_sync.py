#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import json
import sys
from decimal import Decimal
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SHEET = ROOT / "catalogue_astronomie.csv"
CATALOG_DIR = ROOT / "app" / "src" / "main" / "assets" / "catalog"

CSV_FIELDS = [
    "rowType",
    "extensionId",
    "extensionName",
    "extensionCoverImageRef",
    "cardId",
    "rarityLabel",
    "drawWeight",
    "imageRef",
    "variantProfileId",
    "commonName",
    "primaryCatalogName",
    "catalogNumber",
    "objectFamily",
    "objectTypeLabel",
    "constellation",
    "mainSeason",
    "rightAscensionHours",
    "rightAscensionMinutes",
    "rightAscensionSeconds",
    "declinationSign",
    "declinationDegrees",
    "declinationArcMinutes",
    "declinationArcSeconds",
    "shortDescription",
    "detailType",
    "distanceLightYears",
    "realSizeLightYears",
    "visualFullMoonWidth",
    "visualFullMoonHeight",
    "angularWidthDegrees",
    "angularWidthArcMinutes",
    "angularWidthArcSeconds",
    "angularHeightDegrees",
    "angularHeightArcMinutes",
    "angularHeightArcSeconds",
    "absoluteMagnitudeValue",
]


class CatalogSheetError(Exception):
    pass


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()
    try:
        args.handler(args)
    except CatalogSheetError as error:
        print(f"Error: {error}", file=sys.stderr)
        return 1
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Export or apply the astronomy catalog spreadsheet.",
    )
    subparsers = parser.add_subparsers(dest="command", required=True)

    export_parser = subparsers.add_parser(
        "export",
        help="Create or refresh the root CSV sheet from the current standalone catalog.",
    )
    export_parser.add_argument(
        "--sheet",
        type=Path,
        default=DEFAULT_SHEET,
        help=f"CSV path to write (default: {DEFAULT_SHEET.name}).",
    )
    export_parser.set_defaults(handler=handle_export)

    apply_parser = subparsers.add_parser(
        "apply",
        help="Validate the CSV sheet, then overwrite the standalone client catalog files with it.",
    )
    apply_parser.add_argument(
        "--sheet",
        type=Path,
        default=DEFAULT_SHEET,
        help=f"CSV path to read (default: {DEFAULT_SHEET.name}).",
    )
    apply_parser.set_defaults(handler=handle_apply)

    return parser


def handle_export(args: argparse.Namespace) -> None:
    sheet_path = args.sheet.resolve()
    extensions = load_json(CATALOG_DIR / "extensions.json")
    cards = load_json(CATALOG_DIR / "cards.json")

    rows: list[dict[str, str]] = []

    for extension in extensions:
        rows.append(
            {
                "rowType": "extension",
                "extensionId": extension["id"],
                "extensionName": extension["name"],
                "extensionCoverImageRef": extension["coverImageRef"],
            },
        )

    for card in cards:
        rows.append(card_to_csv_row(card))

    sheet_path.parent.mkdir(parents=True, exist_ok=True)
    with sheet_path.open("w", encoding="utf-8-sig", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=CSV_FIELDS, delimiter=";")
        writer.writeheader()
        for row in rows:
            writer.writerow({field: row.get(field, "") for field in CSV_FIELDS})

    print(f"Spreadsheet exported to {sheet_path}")


def handle_apply(args: argparse.Namespace) -> None:
    sheet_path = args.sheet.resolve()
    if not sheet_path.exists():
        raise CatalogSheetError(f"Spreadsheet not found: {sheet_path}")

    extensions, cards = parse_sheet(sheet_path)

    write_json(CATALOG_DIR / "extensions.json", extensions)
    write_json(CATALOG_DIR / "cards.json", cards)

    print(
        "Catalog applied successfully: "
        f"{len(extensions)} extension(s), {len(cards)} card(s)."
    )


def parse_sheet(sheet_path: Path) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    variant_profile_ids = load_variant_profile_ids(CATALOG_DIR / "variant_profiles.json")

    with sheet_path.open("r", encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle, delimiter=";")
        if reader.fieldnames is None:
            raise CatalogSheetError("Spreadsheet header is missing.")
        missing_columns = [field for field in CSV_FIELDS if field not in reader.fieldnames]
        if missing_columns:
            raise CatalogSheetError(
                "Spreadsheet is missing required columns: " + ", ".join(missing_columns),
            )

        extensions: list[dict[str, Any]] = []
        cards: list[dict[str, Any]] = []

        for line_number, raw_row in enumerate(reader, start=2):
            row = {key: (value or "").strip() for key, value in raw_row.items()}
            if is_blank_row(row):
                continue

            row_type = row["rowType"]
            if row_type == "metadata":
                continue
            elif row_type == "extension":
                extensions.append(parse_extension_row(row, line_number))
            elif row_type == "card":
                cards.append(parse_card_row(row, line_number, variant_profile_ids))
            else:
                raise CatalogSheetError(
                    f"Line {line_number}: unsupported rowType '{row_type}'. "
                    "Use metadata, extension or card.",
                )

    if not extensions:
        raise CatalogSheetError("The spreadsheet must contain at least one extension row.")
    if not cards:
        raise CatalogSheetError("The spreadsheet must contain at least one card row.")

    extension_ids = [extension["id"] for extension in extensions]
    duplicate_extension_ids = sorted(find_duplicates(extension_ids))
    if duplicate_extension_ids:
        raise CatalogSheetError(
            "Duplicate extension ids found: " + ", ".join(duplicate_extension_ids),
        )

    card_ids = [card["id"] for card in cards]
    duplicate_card_ids = sorted(find_duplicates(card_ids))
    if duplicate_card_ids:
        raise CatalogSheetError("Duplicate card ids found: " + ", ".join(duplicate_card_ids))

    known_extension_ids = set(extension_ids)
    unknown_extension_ids = sorted(
        {card["extensionId"] for card in cards if card["extensionId"] not in known_extension_ids},
    )
    if unknown_extension_ids:
        raise CatalogSheetError(
            "Some cards reference unknown extensions: " + ", ".join(unknown_extension_ids),
        )

    return extensions, cards


def parse_extension_row(row: dict[str, str], line_number: int) -> dict[str, Any]:
    return {
        "id": require_text(row, "extensionId", line_number),
        "name": require_text(row, "extensionName", line_number),
        "coverImageRef": require_text(row, "extensionCoverImageRef", line_number),
    }


def parse_card_row(
    row: dict[str, str],
    line_number: int,
    variant_profile_ids: set[str],
) -> dict[str, Any]:
    card_id = require_text(row, "cardId", line_number)
    common_name = optional_text(row, "commonName")
    catalog_number = require_text(row, "catalogNumber", line_number)
    name = optional_text(row, "name") or common_name or catalog_number
    variant_profile_id = optional_text(row, "variantProfileId") or "observation-default"
    if variant_profile_id not in variant_profile_ids:
        raise CatalogSheetError(
            f"Line {line_number}: unknown variantProfileId '{variant_profile_id}'.",
        )

    right_ascension = {
        "hours": parse_int(row, "rightAscensionHours", line_number, required=True),
        "minutes": parse_int(row, "rightAscensionMinutes", line_number, required=True),
        "seconds": parse_float(row, "rightAscensionSeconds", line_number, required=True),
    }
    right_ascension["label"] = optional_text(row, "rightAscensionLabel") or format_right_ascension_label(
        right_ascension["hours"],
        right_ascension["minutes"],
        right_ascension["seconds"],
    )

    declination = {
        "sign": require_sign(row, "declinationSign", line_number),
        "degrees": parse_int(row, "declinationDegrees", line_number, required=True),
        "arcMinutes": parse_int(row, "declinationArcMinutes", line_number, required=True),
        "arcSeconds": parse_int(row, "declinationArcSeconds", line_number, required=True),
    }
    declination["label"] = optional_text(row, "declinationLabel") or format_declination_label(
        declination["sign"],
        declination["degrees"],
        declination["arcMinutes"],
        declination["arcSeconds"],
    )

    coordinates_label = optional_text(row, "coordinatesLabel") or (
        f"{right_ascension['label']} ; Dec {declination['label']}"
    )

    detail_type = require_text(row, "detailType", line_number)
    details = build_details(row, line_number, detail_type)

    return {
        "id": card_id,
        "extensionId": require_text(row, "extensionId", line_number),
        "name": name,
        "rarityLabel": require_text(row, "rarityLabel", line_number),
        "drawWeight": parse_int(row, "drawWeight", line_number, required=True),
        "imageRef": require_text(row, "imageRef", line_number),
        "variantProfileId": variant_profile_id,
        "astronomy": {
            "commonName": common_name,
            "primaryCatalogName": require_text(row, "primaryCatalogName", line_number),
            "catalogNumber": catalog_number,
            "objectFamily": require_text(row, "objectFamily", line_number),
            "objectTypeLabel": require_text(row, "objectTypeLabel", line_number),
            "constellation": require_text(row, "constellation", line_number),
            "mainSeason": require_text(row, "mainSeason", line_number),
            "coordinates": {
                "rightAscension": right_ascension,
                "declination": declination,
                "label": coordinates_label,
            },
            "shortDescription": require_text(row, "shortDescription", line_number),
            "details": details,
        },
    }


def build_details(row: dict[str, str], line_number: int, detail_type: str) -> dict[str, Any]:
    if detail_type == "deep_sky":
        details = {
            "detailType": detail_type,
            "distance": build_light_year_measurement(
                row,
                line_number,
                value_field="distanceLightYears",
                label_field="distanceLabel",
                required=True,
            ),
            "realSize": build_light_year_measurement(
                row,
                line_number,
                value_field="realSizeLightYears",
                label_field="realSizeLabel",
                required=True,
            ),
            "visualSize": build_visual_size(row, line_number, required=True),
        }
        absolute_magnitude = build_absolute_magnitude(row, line_number, required=False)
        if absolute_magnitude is not None:
            details["absoluteMagnitude"] = absolute_magnitude
        return details

    if detail_type == "star":
        details: dict[str, Any] = {
            "detailType": detail_type,
            "distance": build_light_year_measurement(
                row,
                line_number,
                value_field="distanceLightYears",
                label_field="distanceLabel",
                required=True,
            ),
        }
        real_size = build_light_year_measurement(
            row,
            line_number,
            value_field="realSizeLightYears",
            label_field="realSizeLabel",
            required=False,
        )
        visual_size = build_visual_size(row, line_number, required=False)
        if real_size is not None:
            details["realSize"] = real_size
        if visual_size is not None:
            details["visualSize"] = visual_size
        details["absoluteMagnitude"] = build_absolute_magnitude(row, line_number, required=True)
        return details

    if detail_type == "constellation":
        return {
            "detailType": detail_type,
            "visualSize": build_visual_size(row, line_number, required=True),
        }

    if detail_type == "sky_event":
        details = {
            "detailType": detail_type,
        }
        visual_size = build_visual_size(row, line_number, required=False)
        if visual_size is not None:
            details["visualSize"] = visual_size
        return details

    raise CatalogSheetError(
        f"Line {line_number}: unsupported detailType '{detail_type}'. "
        "Use deep_sky, star, constellation or sky_event.",
    )


def build_light_year_measurement(
    row: dict[str, str],
    line_number: int,
    value_field: str,
    label_field: str,
    required: bool,
) -> dict[str, Any] | None:
    value = parse_float(row, value_field, line_number, required=required)
    if value is None:
        return None
    label = optional_text(row, label_field) or format_light_year_label(value)
    return {
        "lightYears": value,
        "label": label,
    }


def build_absolute_magnitude(
    row: dict[str, str],
    line_number: int,
    required: bool,
) -> dict[str, Any] | None:
    value = parse_float(row, "absoluteMagnitudeValue", line_number, required=required)
    if value is None:
        return None
    label = optional_text(row, "absoluteMagnitudeLabel") or format_signed_number(value)
    return {
        "value": value,
        "label": label,
    }


def build_visual_size(
    row: dict[str, str],
    line_number: int,
    required: bool,
) -> dict[str, Any] | None:
    visual_fields = [
        "visualFullMoonWidth",
        "visualFullMoonHeight",
        "angularWidthDegrees",
        "angularWidthArcMinutes",
        "angularWidthArcSeconds",
        "angularHeightDegrees",
        "angularHeightArcMinutes",
        "angularHeightArcSeconds",
    ]
    present_fields = [field for field in visual_fields if optional_text(row, field)]

    if not required and not present_fields:
        return None
    if not required and present_fields and len(present_fields) != len(visual_fields):
        raise CatalogSheetError(
            f"Line {line_number}: visual size is partial. Fill every visual field or leave them all blank.",
        )

    full_moon_width = parse_float(row, "visualFullMoonWidth", line_number, required=True)
    full_moon_height = parse_float(row, "visualFullMoonHeight", line_number, required=True)

    angular_width = build_angular_measurement(
        row,
        line_number,
        degrees_field="angularWidthDegrees",
        minutes_field="angularWidthArcMinutes",
        seconds_field="angularWidthArcSeconds",
        label_field="angularWidthLabel",
    )
    angular_height = build_angular_measurement(
        row,
        line_number,
        degrees_field="angularHeightDegrees",
        minutes_field="angularHeightArcMinutes",
        seconds_field="angularHeightArcSeconds",
        label_field="angularHeightLabel",
    )

    label = optional_text(row, "visualSizeLabel") or (
        f"{format_fixed_number(full_moon_width, 2)} × {format_fixed_number(full_moon_height, 2)} "
        f"({angular_width['label']} × {angular_height['label']})"
    )
    return {
        "fullMoonWidth": full_moon_width,
        "fullMoonHeight": full_moon_height,
        "angularWidth": angular_width,
        "angularHeight": angular_height,
        "label": label,
    }


def build_angular_measurement(
    row: dict[str, str],
    line_number: int,
    degrees_field: str,
    minutes_field: str,
    seconds_field: str,
    label_field: str,
) -> dict[str, Any]:
    degrees = parse_int(row, degrees_field, line_number, required=True)
    minutes = parse_int(row, minutes_field, line_number, required=True)
    seconds = parse_int(row, seconds_field, line_number, required=True)
    label = optional_text(row, label_field) or format_angular_label(degrees, minutes, seconds)
    return {
        "degrees": degrees,
        "arcMinutes": minutes,
        "arcSeconds": seconds,
        "label": label,
    }


def card_to_csv_row(card: dict[str, Any]) -> dict[str, str]:
    astronomy = card["astronomy"]
    coordinates = astronomy["coordinates"]
    right_ascension = coordinates["rightAscension"]
    declination = coordinates["declination"]
    details = astronomy["details"]
    row = {
        "rowType": "card",
        "extensionId": card["extensionId"],
        "cardId": card["id"],
        "rarityLabel": card["rarityLabel"],
        "drawWeight": str(card["drawWeight"]),
        "imageRef": card["imageRef"],
        "variantProfileId": card["variantProfileId"],
        "commonName": astronomy.get("commonName") or "",
        "primaryCatalogName": astronomy["primaryCatalogName"],
        "catalogNumber": astronomy["catalogNumber"],
        "objectFamily": astronomy["objectFamily"],
        "objectTypeLabel": astronomy["objectTypeLabel"],
        "constellation": astronomy["constellation"],
        "mainSeason": astronomy["mainSeason"],
        "rightAscensionHours": str(right_ascension["hours"]),
        "rightAscensionMinutes": str(right_ascension["minutes"]),
        "rightAscensionSeconds": format_raw_number(right_ascension["seconds"]),
        "rightAscensionLabel": right_ascension["label"],
        "declinationSign": declination["sign"],
        "declinationDegrees": str(declination["degrees"]),
        "declinationArcMinutes": str(declination["arcMinutes"]),
        "declinationArcSeconds": str(declination["arcSeconds"]),
        "declinationLabel": declination["label"],
        "coordinatesLabel": coordinates["label"],
        "shortDescription": astronomy["shortDescription"],
        "detailType": details["detailType"],
    }

    if "distance" in details:
        row["distanceLightYears"] = format_raw_number(details["distance"]["lightYears"])
        row["distanceLabel"] = details["distance"]["label"]
    if "realSize" in details:
        row["realSizeLightYears"] = format_raw_number(details["realSize"]["lightYears"])
        row["realSizeLabel"] = details["realSize"]["label"]
    if "visualSize" in details:
        visual_size = details["visualSize"]
        angular_width = visual_size["angularWidth"]
        angular_height = visual_size["angularHeight"]
        row.update(
            {
                "visualFullMoonWidth": format_raw_number(visual_size["fullMoonWidth"]),
                "visualFullMoonHeight": format_raw_number(visual_size["fullMoonHeight"]),
                "angularWidthDegrees": str(angular_width["degrees"]),
                "angularWidthArcMinutes": str(angular_width["arcMinutes"]),
                "angularWidthArcSeconds": str(angular_width["arcSeconds"]),
                "angularWidthLabel": angular_width["label"],
                "angularHeightDegrees": str(angular_height["degrees"]),
                "angularHeightArcMinutes": str(angular_height["arcMinutes"]),
                "angularHeightArcSeconds": str(angular_height["arcSeconds"]),
                "angularHeightLabel": angular_height["label"],
                "visualSizeLabel": visual_size["label"],
            },
        )
    if "absoluteMagnitude" in details:
        row["absoluteMagnitudeValue"] = format_raw_number(details["absoluteMagnitude"]["value"])
        row["absoluteMagnitudeLabel"] = details["absoluteMagnitude"]["label"]

    return row


def load_variant_profile_ids(path: Path) -> set[str]:
    return {profile["id"] for profile in load_json(path)}


def load_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        handle.write(render_json(strip_none(payload)))
        handle.write("\n")


def require_text(row: dict[str, str], field: str, line_number: int) -> str:
    value = optional_text(row, field)
    if value is None:
        raise CatalogSheetError(f"Line {line_number}: '{field}' is required.")
    return value


def optional_text(row: dict[str, str], field: str) -> str | None:
    value = row.get(field, "").strip()
    return value or None


def require_sign(row: dict[str, str], field: str, line_number: int) -> str:
    value = require_text(row, field, line_number)
    if value not in {"+", "-"}:
        raise CatalogSheetError(f"Line {line_number}: '{field}' must be '+' or '-'.")
    return value


def parse_int(
    row: dict[str, str],
    field: str,
    line_number: int,
    required: bool,
) -> int | None:
    raw_value = optional_text(row, field)
    if raw_value is None:
        if required:
            raise CatalogSheetError(f"Line {line_number}: '{field}' is required.")
        return None

    cleaned = clean_number(raw_value)
    try:
        if any(character in cleaned for character in {".", "e", "E"}):
            value = float(cleaned)
            if not value.is_integer():
                raise ValueError
            return int(value)
        return int(cleaned)
    except ValueError as error:
        raise CatalogSheetError(
            f"Line {line_number}: '{field}' must be an integer. Received '{raw_value}'.",
        ) from error


def parse_float(
    row: dict[str, str],
    field: str,
    line_number: int,
    required: bool,
) -> float | None:
    raw_value = optional_text(row, field)
    if raw_value is None:
        if required:
            raise CatalogSheetError(f"Line {line_number}: '{field}' is required.")
        return None

    cleaned = clean_number(raw_value)
    try:
        return float(cleaned)
    except ValueError as error:
        raise CatalogSheetError(
            f"Line {line_number}: '{field}' must be a number. Received '{raw_value}'.",
        ) from error


def clean_number(value: str) -> str:
    return (
        value.replace("\u202f", "")
        .replace("\u00a0", "")
        .replace(" ", "")
        .replace(",", ".")
    )


def is_blank_row(row: dict[str, str]) -> bool:
    return all(not value for value in row.values())


def find_duplicates(values: list[str]) -> set[str]:
    seen: set[str] = set()
    duplicates: set[str] = set()
    for value in values:
        if value in seen:
            duplicates.add(value)
        seen.add(value)
    return duplicates


def strip_none(value: Any) -> Any:
    if isinstance(value, dict):
        return {
            key: strip_none(item)
            for key, item in value.items()
            if item is not None
        }
    if isinstance(value, list):
        return [strip_none(item) for item in value]
    return value


def render_json(value: Any, indent: int = 0) -> str:
    spacing = "  " * indent
    child_spacing = "  " * (indent + 1)

    if isinstance(value, dict):
        if not value:
            return "{}"
        parts = [
            f"{child_spacing}{json.dumps(key, ensure_ascii=False)}: {render_json(item, indent + 1)}"
            for key, item in value.items()
        ]
        return "{\n" + ",\n".join(parts) + f"\n{spacing}" + "}"

    if isinstance(value, list):
        if not value:
            return "[]"
        parts = [f"{child_spacing}{render_json(item, indent + 1)}" for item in value]
        return "[\n" + ",\n".join(parts) + f"\n{spacing}" + "]"

    if isinstance(value, str):
        return json.dumps(value, ensure_ascii=False)
    if isinstance(value, bool):
        return "true" if value else "false"
    if value is None:
        return "null"
    if isinstance(value, int):
        return str(value)
    if isinstance(value, float):
        return format_json_float(value)

    raise TypeError(f"Unsupported JSON value: {value!r}")


def format_json_float(value: float) -> str:
    return format(Decimal(str(value)), "f")


def format_right_ascension_label(hours: int, minutes: int, seconds: float) -> str:
    return f"AD {hours:02d}h {minutes:02d}m {format_right_ascension_seconds(seconds)}s"


def format_declination_label(sign: str, degrees: int, arc_minutes: int, arc_seconds: int) -> str:
    return f"{sign}{degrees:02d}° {arc_minutes:02d}′ {arc_seconds:02d}″"


def format_angular_label(degrees: int, arc_minutes: int, arc_seconds: int) -> str:
    return f"{degrees}°{arc_minutes:02d}′{arc_seconds:02d}″"


def format_fixed_number(value: float, decimals: int) -> str:
    return f"{value:.{decimals}f}".replace(".", ",")


def format_french_number(value: float) -> str:
    decimal_value = Decimal(str(value))
    if decimal_value == decimal_value.to_integral():
        return f"{int(value):,}".replace(",", " ")
    text = format(decimal_value.normalize(), "f")
    integer_part, fractional_part = text.split(".", 1)
    grouped_integer = f"{int(integer_part):,}".replace(",", " ")
    cleaned_fractional = fractional_part.rstrip("0")
    if not cleaned_fractional:
        return grouped_integer
    return f"{grouped_integer},{cleaned_fractional}"


def format_signed_number(value: float) -> str:
    text = f"{value:.2f}".rstrip("0").rstrip(".")
    if text == "-0":
        return "0"
    return text


def format_raw_number(value: float) -> str:
    if float(value).is_integer():
        return str(int(value))
    return format(value, ".15g")


def format_right_ascension_seconds(value: float) -> str:
    text = format_fixed_number(value, 1)
    integer_part, fractional_part = text.split(",", 1)
    return f"{integer_part.zfill(2)},{fractional_part}"


def format_light_year_label(value: float) -> str:
    unit = "annee-lumiere" if abs(value) <= 1 else "annees-lumiere"
    return f"{format_french_number(value)} {unit}"


if __name__ == "__main__":
    raise SystemExit(main())
