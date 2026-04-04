#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import sys
from dataclasses import dataclass
from decimal import Decimal, ROUND_HALF_UP, getcontext
from fractions import Fraction
from math import gcd
from pathlib import Path
from typing import Any

SCRIPT_DIR = Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

from simple_xlsx import FormulaCell, Sheet, column_label, read_workbook, write_workbook


getcontext().prec = 50

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SHEET = ROOT / "catalogue_astronomie.xlsx"
CATALOG_DIR = ROOT / "app" / "src" / "main" / "assets" / "catalog"

CATALOGUE_SHEET_NAME = "Catalogue"
PROBABILITIES_SHEET_NAME = "Probabilites"
RESULTS_SHEET_NAME = "Resultats"
CALIBRATION_SHEET_NAME = "_Calibration"
RESULTS_VARIANT_SECTION = "VariantProfiles"
RESULTS_CARDS_SECTION = "Cards"

RARITY_ORDER = ["Common", "Uncommon", "Rare", "Epic"]
SUPPORTED_SKY_CODES = ["city", "suburban", "rural", "mountain"]
SUPPORTED_FINISH_CODES = ["standard", "holographic"]
WEIGHT_SCALE = Decimal("1000000")

CATALOGUE_FIELDS = [
    "rowType",
    "extensionId",
    "extensionName",
    "extensionCoverImageRef",
    "cardId",
    "name",
    "rarityLabel",
    "cardRarityMultiplier",
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
    "rightAscensionLabel",
    "declinationSign",
    "declinationDegrees",
    "declinationArcMinutes",
    "declinationArcSeconds",
    "declinationLabel",
    "coordinatesLabel",
    "shortDescription",
    "detailType",
    "distanceLightYears",
    "distanceLabel",
    "realSizeLightYears",
    "realSizeLabel",
    "visualFullMoonWidth",
    "visualFullMoonHeight",
    "angularWidthDegrees",
    "angularWidthArcMinutes",
    "angularWidthArcSeconds",
    "angularWidthLabel",
    "angularHeightDegrees",
    "angularHeightArcMinutes",
    "angularHeightArcSeconds",
    "angularHeightLabel",
    "visualSizeLabel",
    "absoluteMagnitudeValue",
    "absoluteMagnitudeLabel",
]

GLOBAL_SETTINGS_SECTION = "GlobalSettings"
VARIANT_PROFILES_SECTION = "VariantProfiles"
EXTENSION_BALANCE_SECTION = "ExtensionBalance"

VARIANT_PROFILE_FIELDS = [
    "variantProfileId",
    "suburbanMeanDays",
    "ruralMeanDays",
    "mountainMeanDays",
    "holographicMeanDays",
]

EXTENSION_BALANCE_FIELDS = [
    "extensionId",
    "rarestComboMeanDays",
    "uncommonBaseCardMeanDays",
    "rareBaseCardMeanDays",
]


class CatalogSheetError(Exception):
    pass


@dataclass(frozen=True)
class GlobalSettings:
    cards_per_draw: int
    draw_cooldown_hours: Decimal

    @property
    def slots_per_day(self) -> Fraction:
        return Fraction(self.cards_per_draw * 24, 1) / Fraction(self.draw_cooldown_hours)


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
        description="Export or apply the astronomy catalog workbook.",
    )
    subparsers = parser.add_subparsers(dest="command", required=True)

    export_parser = subparsers.add_parser(
        "export",
        help="Create or refresh the root XLSX workbook from the current standalone catalog.",
    )
    export_parser.add_argument(
        "--sheet",
        type=Path,
        default=DEFAULT_SHEET,
        help=f"Workbook path to write (default: {DEFAULT_SHEET.name}).",
    )
    export_parser.set_defaults(handler=handle_export)

    apply_parser = subparsers.add_parser(
        "apply",
        help="Validate the XLSX workbook, then overwrite the standalone client catalog files with it.",
    )
    apply_parser.add_argument(
        "--sheet",
        type=Path,
        default=DEFAULT_SHEET,
        help=f"Workbook path to read (default: {DEFAULT_SHEET.name}).",
    )
    apply_parser.set_defaults(handler=handle_apply)

    return parser


def handle_export(args: argparse.Namespace) -> None:
    sheet_path = args.sheet.resolve()
    extensions = load_json(CATALOG_DIR / "extensions.json")
    cards = load_json(CATALOG_DIR / "cards.json")
    variant_profiles = load_json(CATALOG_DIR / "variant_profiles.json")
    export_workbook(
        sheet_path=sheet_path,
        extensions=extensions,
        cards=cards,
        variant_profiles=variant_profiles,
        settings=default_global_settings(),
    )
    print(f"Workbook exported to {sheet_path}")


def handle_apply(args: argparse.Namespace) -> None:
    sheet_path = args.sheet.resolve()
    if not sheet_path.exists():
        raise CatalogSheetError(f"Workbook not found: {sheet_path}")

    extensions, cards, variant_profiles = apply_workbook(
        sheet_path=sheet_path,
        catalog_dir=CATALOG_DIR,
    )

    print(
        "Catalog applied successfully: "
        f"{len(extensions)} extension(s), {len(cards)} card(s), {len(variant_profiles)} variant profile(s)."
    )


def export_workbook(
    sheet_path: Path,
    extensions: list[dict[str, Any]],
    cards: list[dict[str, Any]],
    variant_profiles: list[dict[str, Any]],
    settings: GlobalSettings,
) -> None:
    export_cards = build_export_input_cards(cards)
    extension_variant_profiles = require_single_variant_profile_per_extension(cards)
    variant_inputs = build_export_variant_inputs(variant_profiles, settings)
    extension_inputs = build_export_extension_inputs(
        cards=cards,
        extension_variant_profiles=extension_variant_profiles,
        variant_profiles_by_id={profile["id"]: profile for profile in variant_profiles},
        settings=settings,
    )
    catalogue_rows = build_catalogue_sheet_rows_from_json(extensions, cards)
    probability_rows = build_probability_sheet_rows(settings, variant_inputs, extension_inputs)
    fingerprint = build_visible_fingerprint(
        extensions=extensions,
        cards=export_cards,
        settings=settings,
        variant_inputs=variant_inputs,
        extension_inputs=extension_inputs,
    )
    result_rows = build_results_sheet_rows(
        settings=settings,
        extensions=extensions,
        cards=cards,
        variant_profiles=variant_profiles,
        extension_inputs=extension_inputs,
    )
    calibration_rows = build_calibration_sheet_rows(
        fingerprint=fingerprint,
        cards=cards,
        variant_profiles=variant_profiles,
    )
    write_workbook(
        sheet_path,
        [
            Sheet(CATALOGUE_SHEET_NAME, catalogue_rows),
            Sheet(PROBABILITIES_SHEET_NAME, probability_rows),
            Sheet(RESULTS_SHEET_NAME, result_rows),
            Sheet(CALIBRATION_SHEET_NAME, calibration_rows, hidden=True),
        ],
    )


def apply_workbook(
    sheet_path: Path,
    catalog_dir: Path,
) -> tuple[list[dict[str, Any]], list[dict[str, Any]], list[dict[str, Any]]]:
    sheets = {sheet.name: sheet for sheet in read_workbook(sheet_path)}
    workbook_sheet = require_sheet(sheets, CATALOGUE_SHEET_NAME)

    variant_profile_templates = load_json(catalog_dir / "variant_profiles.json")
    variant_profile_ids = {profile["id"] for profile in variant_profile_templates}

    extensions, input_cards = parse_catalogue_sheet(workbook_sheet.rows, variant_profile_ids)
    probabilities_sheet = sheets.get(PROBABILITIES_SHEET_NAME)
    if probabilities_sheet is None:
        results_sheet = require_sheet(sheets, RESULTS_SHEET_NAME)
        variant_profiles = parse_result_variant_profiles(results_sheet.rows, variant_profile_templates)
        cards = parse_result_cards(results_sheet.rows, input_cards)
        write_json(catalog_dir / "extensions.json", extensions)
        write_json(catalog_dir / "cards.json", cards)
        write_json(catalog_dir / "variant_profiles.json", variant_profiles)
        return extensions, cards, variant_profiles

    settings, variant_inputs, extension_inputs = parse_probabilities_sheet(probabilities_sheet.rows)

    extension_variant_profiles = require_single_variant_profile_per_extension(input_cards)
    fingerprint = build_visible_fingerprint(
        extensions=extensions,
        cards=input_cards,
        settings=settings,
        variant_inputs=variant_inputs,
        extension_inputs=extension_inputs,
    )

    calibration = parse_calibration_sheet(sheets.get(CALIBRATION_SHEET_NAME))
    if calibration is not None and calibration.get("fingerprint") == fingerprint:
        cards = apply_cached_card_weights(input_cards, calibration)
        variant_profiles = apply_cached_variant_weights(variant_profile_templates, calibration)
    else:
        variant_profiles, profile_metrics = compute_variant_profiles(
            templates=variant_profile_templates,
            settings=settings,
            variant_inputs=variant_inputs,
        )
        cards = compute_cards(
            extensions=extensions,
            input_cards=input_cards,
            settings=settings,
            extension_inputs=extension_inputs,
            profile_metrics=profile_metrics,
            extension_variant_profiles=extension_variant_profiles,
        )

    write_json(catalog_dir / "extensions.json", extensions)
    write_json(catalog_dir / "cards.json", cards)
    write_json(catalog_dir / "variant_profiles.json", variant_profiles)

    refreshed_catalogue_rows = build_catalogue_sheet_rows_from_input(extensions, input_cards)
    refreshed_probability_rows = build_probability_sheet_rows(settings, variant_inputs, extension_inputs)
    refreshed_result_rows = build_results_sheet_rows(
        settings=settings,
        extensions=extensions,
        cards=cards,
        variant_profiles=variant_profiles,
        extension_inputs=extension_inputs,
    )
    refreshed_calibration_rows = build_calibration_sheet_rows(
        fingerprint=fingerprint,
        cards=cards,
        variant_profiles=variant_profiles,
    )
    write_workbook(
        sheet_path,
        [
            Sheet(CATALOGUE_SHEET_NAME, refreshed_catalogue_rows),
            Sheet(PROBABILITIES_SHEET_NAME, refreshed_probability_rows),
            Sheet(RESULTS_SHEET_NAME, refreshed_result_rows),
            Sheet(CALIBRATION_SHEET_NAME, refreshed_calibration_rows, hidden=True),
        ],
    )

    return extensions, cards, variant_profiles


def require_sheet(sheets: dict[str, Sheet], name: str) -> Sheet:
    sheet = sheets.get(name)
    if sheet is None:
        raise CatalogSheetError(f"Workbook is missing the '{name}' sheet.")
    return sheet


def parse_section_table(
    rows: list[list[str]],
    section_name: str,
    sheet_name: str,
) -> list[tuple[int, dict[str, str]]]:
    section_row_number: int | None = None
    for row_number, row in enumerate(rows, start=1):
        if row and row[0].strip() == section_name:
            section_row_number = row_number
            break
    if section_row_number is None:
        raise CatalogSheetError(f"The {sheet_name} sheet is missing the '{section_name}' section.")

    header_row_number = section_row_number + 1
    if header_row_number > len(rows):
        raise CatalogSheetError(
            f"The {sheet_name} sheet section '{section_name}' is missing its header row.",
        )
    header = [value.strip() for value in rows[header_row_number - 1] if value.strip()]
    if not header:
        raise CatalogSheetError(
            f"The {sheet_name} sheet section '{section_name}' has an empty header row.",
        )

    table_rows: list[tuple[int, dict[str, str]]] = []
    for row_number in range(header_row_number + 1, len(rows) + 1):
        raw_row = rows[row_number - 1]
        if not raw_row or all(not value.strip() for value in raw_row):
            if table_rows:
                break
            continue
        if not raw_row[0].strip():
            if table_rows:
                break
            continue
        table_rows.append((row_number, map_row(header, raw_row)))

    if not table_rows:
        raise CatalogSheetError(
            f"The {sheet_name} sheet section '{section_name}' must contain at least one data row.",
        )
    return table_rows


def parse_result_variant_profiles(
    rows: list[list[str]],
    templates: list[dict[str, Any]],
) -> list[dict[str, Any]]:
    variant_rows = parse_section_table(rows, RESULTS_VARIANT_SECTION, RESULTS_SHEET_NAME)
    weights_by_profile: dict[str, dict[str, dict[str, int]]] = {}
    seen_entries: set[tuple[str, str, str]] = set()

    for row_number, row in variant_rows:
        variant_profile_id = require_text(row, "variantProfileId", row_number, RESULTS_SHEET_NAME)
        category = require_text(row, "category", row_number, RESULTS_SHEET_NAME)
        code = require_text(row, "code", row_number, RESULTS_SHEET_NAME)
        key = (variant_profile_id, category, code)
        if key in seen_entries:
            raise CatalogSheetError(
                f"{RESULTS_SHEET_NAME} row {row_number}: duplicate VariantProfiles entry "
                f"for '{variant_profile_id}' / '{category}' / '{code}'.",
            )
        seen_entries.add(key)
        weight = parse_int(row, "weight", row_number, RESULTS_SHEET_NAME, required=True)
        if weight is None or weight <= 0:
            raise CatalogSheetError(
                f"{RESULTS_SHEET_NAME} row {row_number}: 'weight' must be strictly positive.",
            )
        weights_by_profile.setdefault(variant_profile_id, {}).setdefault(category, {})[code] = weight

    known_profile_ids = {template["id"] for template in templates}
    unknown_profile_ids = sorted(weights_by_profile.keys() - known_profile_ids)
    if unknown_profile_ids:
        raise CatalogSheetError(
            "Resultats: unknown variantProfileId(s): " + ", ".join(unknown_profile_ids),
        )

    variant_profiles: list[dict[str, Any]] = []
    for template in templates:
        profile_weights = weights_by_profile.get(template["id"])
        if profile_weights is None:
            raise CatalogSheetError(
                f"Resultats: missing VariantProfiles rows for '{template['id']}'.",
            )
        sky_weights = extract_result_category_weights(
            profile_weights,
            profile_id=template["id"],
            category="skyQuality",
            definitions=template["skyQualities"],
        )
        finish_weights = extract_result_category_weights(
            profile_weights,
            profile_id=template["id"],
            category="finish",
            definitions=template["finishes"],
        )
        variant_profiles.append(
            {
                **template,
                "skyQualityWeights": [
                    {"code": definition["code"], "weight": sky_weights[definition["code"]]}
                    for definition in template["skyQualities"]
                ],
                "finishWeights": [
                    {"code": definition["code"], "weight": finish_weights[definition["code"]]}
                    for definition in template["finishes"]
                ],
            },
        )
    return variant_profiles


def extract_result_category_weights(
    profile_weights: dict[str, dict[str, int]],
    profile_id: str,
    category: str,
    definitions: list[dict[str, Any]],
) -> dict[str, int]:
    category_weights = profile_weights.get(category)
    if category_weights is None:
        raise CatalogSheetError(
            f"Resultats: missing '{category}' rows for variant profile '{profile_id}'.",
        )

    ordered_codes = [definition["code"] for definition in definitions]
    missing_codes = [code for code in ordered_codes if code not in category_weights]
    if missing_codes:
        raise CatalogSheetError(
            f"Resultats: missing {category} code(s) for variant profile '{profile_id}': "
            + ", ".join(missing_codes),
        )

    unknown_codes = sorted(set(category_weights.keys()) - set(ordered_codes))
    if unknown_codes:
        raise CatalogSheetError(
            f"Resultats: unknown {category} code(s) for variant profile '{profile_id}': "
            + ", ".join(unknown_codes),
        )

    return {code: category_weights[code] for code in ordered_codes}


def parse_result_cards(
    rows: list[list[str]],
    input_cards: list[dict[str, Any]],
) -> list[dict[str, Any]]:
    result_rows = parse_section_table(rows, RESULTS_CARDS_SECTION, RESULTS_SHEET_NAME)
    results_by_card_id: dict[str, dict[str, Any]] = {}
    results_by_extension_name: dict[tuple[str, str], dict[str, Any]] = {}

    for row_number, row in result_rows:
        extension_id = require_text(row, "extensionId", row_number, RESULTS_SHEET_NAME)
        name = require_text(row, "name", row_number, RESULTS_SHEET_NAME)
        probability = require_positive_fraction_value(
            row.get("normalisedDrawWeight"),
            "normalisedDrawWeight",
            RESULTS_SHEET_NAME,
            row_number=row_number,
        )
        entry = {
            "extensionId": extension_id,
            "name": name,
            "probability": probability,
        }
        card_id = optional_text(row, "cardId")
        if card_id is not None:
            if card_id in results_by_card_id:
                raise CatalogSheetError(
                    f"{RESULTS_SHEET_NAME} row {row_number}: duplicate cardId '{card_id}'.",
                )
            results_by_card_id[card_id] = entry

        name_key = (extension_id, name)
        if name_key in results_by_extension_name:
            raise CatalogSheetError(
                f"{RESULTS_SHEET_NAME} row {row_number}: duplicate name '{name}' for extension '{extension_id}'.",
            )
        results_by_extension_name[name_key] = entry

    input_card_ids = {card["id"] for card in input_cards}
    unknown_result_card_ids = sorted(card_id for card_id in results_by_card_id if card_id not in input_card_ids)
    if unknown_result_card_ids:
        raise CatalogSheetError(
            "Resultats: unknown cardId(s): " + ", ".join(unknown_result_card_ids),
        )

    cards_by_extension: dict[str, list[dict[str, Any]]] = {}
    for card in input_cards:
        cards_by_extension.setdefault(card["extensionId"], []).append(card)

    output_cards: list[dict[str, Any]] = []
    for extension_id, extension_cards in cards_by_extension.items():
        probabilities: dict[str, Fraction] = {}
        ordered_card_ids: list[str] = []
        for card in extension_cards:
            result_entry = results_by_card_id.get(card["id"])
            if result_entry is None:
                result_entry = results_by_extension_name.get((card["extensionId"], card["name"]))
            if result_entry is None:
                raise CatalogSheetError(
                    f"Resultats: missing card probability for '{card['id']}' / '{card['name']}'.",
                )
            probabilities[card["id"]] = result_entry["probability"]
            ordered_card_ids.append(card["id"])
        weights = weights_from_probability_order(probabilities, ordered_card_ids)
        output_cards.extend(
            input_card_to_runtime_card(card, draw_weight)
            for card, draw_weight in zip(extension_cards, weights, strict=True)
        )

    return output_cards


def parse_catalogue_sheet(
    rows: list[list[str]],
    variant_profile_ids: set[str],
) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    if not rows:
        raise CatalogSheetError("The Catalogue sheet is empty.")
    header = [value.strip() for value in rows[0]]
    missing_columns = [field for field in CATALOGUE_FIELDS if field not in header]
    if missing_columns:
        raise CatalogSheetError(
            "The Catalogue sheet is missing required columns: " + ", ".join(missing_columns),
        )

    extensions: list[dict[str, Any]] = []
    cards: list[dict[str, Any]] = []

    for row_number, raw_row in enumerate(rows[1:], start=2):
        row = map_row(header, raw_row)
        if is_blank_row(row):
            continue

        row_type = row["rowType"]
        if row_type == "extension":
            extensions.append(parse_extension_row(row, row_number))
        elif row_type == "card":
            cards.append(parse_catalogue_card_row(row, row_number, variant_profile_ids))
        else:
            raise CatalogSheetError(
                f"Catalogue row {row_number}: unsupported rowType '{row_type}'. "
                "Use extension or card.",
            )

    if not extensions:
        raise CatalogSheetError("The Catalogue sheet must contain at least one extension row.")
    if not cards:
        raise CatalogSheetError("The Catalogue sheet must contain at least one card row.")

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


def parse_catalogue_card_row(
    row: dict[str, str],
    row_number: int,
    variant_profile_ids: set[str],
) -> dict[str, Any]:
    card_id = require_text(row, "cardId", row_number, "Catalogue")
    common_name = optional_text(row, "commonName")
    catalog_number = require_text(row, "catalogNumber", row_number, "Catalogue")
    name = optional_text(row, "name") or common_name or catalog_number
    variant_profile_id = optional_text(row, "variantProfileId") or "observation-default"
    if variant_profile_id not in variant_profile_ids:
        raise CatalogSheetError(
            f"Catalogue row {row_number}: unknown variantProfileId '{variant_profile_id}'.",
        )

    card_rarity_multiplier = parse_decimal(
        row,
        "cardRarityMultiplier",
        row_number,
        sheet_name="Catalogue",
        required=True,
    )
    if card_rarity_multiplier is None or card_rarity_multiplier <= 0:
        raise CatalogSheetError(
            f"Catalogue row {row_number}: 'cardRarityMultiplier' must be strictly positive.",
        )

    right_ascension = {
        "hours": parse_int(row, "rightAscensionHours", row_number, "Catalogue", required=True),
        "minutes": parse_int(row, "rightAscensionMinutes", row_number, "Catalogue", required=True),
        "seconds": parse_float(row, "rightAscensionSeconds", row_number, "Catalogue", required=True),
    }
    right_ascension["label"] = optional_text(row, "rightAscensionLabel") or format_right_ascension_label(
        right_ascension["hours"],
        right_ascension["minutes"],
        right_ascension["seconds"],
    )

    declination = {
        "sign": require_sign(row, "declinationSign", row_number, "Catalogue"),
        "degrees": parse_int(row, "declinationDegrees", row_number, "Catalogue", required=True),
        "arcMinutes": parse_int(row, "declinationArcMinutes", row_number, "Catalogue", required=True),
        "arcSeconds": parse_int(row, "declinationArcSeconds", row_number, "Catalogue", required=True),
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

    detail_type = require_text(row, "detailType", row_number, "Catalogue")
    details = build_details(row, row_number, "Catalogue", detail_type)

    return {
        "id": card_id,
        "extensionId": require_text(row, "extensionId", row_number, "Catalogue"),
        "name": name,
        "rarityLabel": require_text(row, "rarityLabel", row_number, "Catalogue"),
        "cardRarityMultiplier": card_rarity_multiplier,
        "imageRef": require_text(row, "imageRef", row_number, "Catalogue"),
        "variantProfileId": variant_profile_id,
        "astronomy": {
            "commonName": common_name,
            "primaryCatalogName": require_text(row, "primaryCatalogName", row_number, "Catalogue"),
            "catalogNumber": catalog_number,
            "objectFamily": require_text(row, "objectFamily", row_number, "Catalogue"),
            "objectTypeLabel": require_text(row, "objectTypeLabel", row_number, "Catalogue"),
            "constellation": require_text(row, "constellation", row_number, "Catalogue"),
            "mainSeason": require_text(row, "mainSeason", row_number, "Catalogue"),
            "coordinates": {
                "rightAscension": right_ascension,
                "declination": declination,
                "label": coordinates_label,
            },
            "shortDescription": require_text(row, "shortDescription", row_number, "Catalogue"),
            "details": details,
        },
    }


def parse_extension_row(row: dict[str, str], row_number: int) -> dict[str, Any]:
    return {
        "id": require_text(row, "extensionId", row_number, "Catalogue"),
        "name": require_text(row, "extensionName", row_number, "Catalogue"),
        "coverImageRef": require_text(row, "extensionCoverImageRef", row_number, "Catalogue"),
    }


def parse_probabilities_sheet(
    rows: list[list[str]],
) -> tuple[GlobalSettings, list[dict[str, Any]], list[dict[str, Any]]]:
    global_rows = parse_named_table(rows, GLOBAL_SETTINGS_SECTION)
    settings_map = {
        require_text(row, "parameter", row_number, PROBABILITIES_SHEET_NAME): require_text(
            row,
            "value",
            row_number,
            PROBABILITIES_SHEET_NAME,
        )
        for row_number, row in global_rows
    }
    cards_per_draw = parse_int_value(
        settings_map.get("cardsPerDraw"),
        "cardsPerDraw",
        PROBABILITIES_SHEET_NAME,
    )
    draw_cooldown_hours = parse_decimal_value(
        settings_map.get("drawCooldownHours"),
        "drawCooldownHours",
        PROBABILITIES_SHEET_NAME,
    )
    if cards_per_draw <= 0:
        raise CatalogSheetError("Probabilites: 'cardsPerDraw' must be strictly positive.")
    if draw_cooldown_hours <= 0:
        raise CatalogSheetError("Probabilites: 'drawCooldownHours' must be strictly positive.")
    settings = GlobalSettings(
        cards_per_draw=cards_per_draw,
        draw_cooldown_hours=draw_cooldown_hours,
    )

    variant_inputs: list[dict[str, Any]] = []
    seen_variant_profile_ids: set[str] = set()
    for row_number, row in parse_named_table(rows, VARIANT_PROFILES_SECTION):
        variant_profile_id = require_text(
            row,
            "variantProfileId",
            row_number,
            PROBABILITIES_SHEET_NAME,
        )
        if variant_profile_id in seen_variant_profile_ids:
            raise CatalogSheetError(
                f"Probabilites row {row_number}: duplicate variantProfileId '{variant_profile_id}'.",
            )
        seen_variant_profile_ids.add(variant_profile_id)
        variant_inputs.append(
            {
                "variantProfileId": variant_profile_id,
                "suburbanMeanDays": require_positive_decimal(
                    row,
                    "suburbanMeanDays",
                    row_number,
                    PROBABILITIES_SHEET_NAME,
                ),
                "ruralMeanDays": require_positive_decimal(
                    row,
                    "ruralMeanDays",
                    row_number,
                    PROBABILITIES_SHEET_NAME,
                ),
                "mountainMeanDays": require_positive_decimal(
                    row,
                    "mountainMeanDays",
                    row_number,
                    PROBABILITIES_SHEET_NAME,
                ),
                "holographicMeanDays": require_positive_decimal(
                    row,
                    "holographicMeanDays",
                    row_number,
                    PROBABILITIES_SHEET_NAME,
                ),
            },
        )
    if not variant_inputs:
        raise CatalogSheetError("The Probabilites sheet must contain at least one VariantProfiles row.")

    extension_inputs: list[dict[str, Any]] = []
    seen_extension_ids: set[str] = set()
    for row_number, row in parse_named_table(rows, EXTENSION_BALANCE_SECTION):
        extension_id = require_text(row, "extensionId", row_number, PROBABILITIES_SHEET_NAME)
        if extension_id in seen_extension_ids:
            raise CatalogSheetError(
                f"Probabilites row {row_number}: duplicate extensionId '{extension_id}'.",
            )
        seen_extension_ids.add(extension_id)
        extension_inputs.append(
            {
                "extensionId": extension_id,
                "rarestComboMeanDays": require_positive_decimal(
                    row,
                    "rarestComboMeanDays",
                    row_number,
                    PROBABILITIES_SHEET_NAME,
                ),
                "uncommonBaseCardMeanDays": optional_positive_decimal(
                    row,
                    "uncommonBaseCardMeanDays",
                    row_number,
                    PROBABILITIES_SHEET_NAME,
                ),
                "rareBaseCardMeanDays": optional_positive_decimal(
                    row,
                    "rareBaseCardMeanDays",
                    row_number,
                    PROBABILITIES_SHEET_NAME,
                ),
            },
        )
    if not extension_inputs:
        raise CatalogSheetError("The Probabilites sheet must contain at least one ExtensionBalance row.")

    return settings, variant_inputs, extension_inputs


def parse_named_table(rows: list[list[str]], section_name: str) -> list[tuple[int, dict[str, str]]]:
    section_row_number: int | None = None
    for row_number, row in enumerate(rows, start=1):
        if row and row[0].strip() == section_name:
            section_row_number = row_number
            break
    if section_row_number is None:
        raise CatalogSheetError(f"The Probabilites sheet is missing the '{section_name}' section.")

    header_row_number = section_row_number + 1
    if header_row_number > len(rows):
        raise CatalogSheetError(
            f"The Probabilites sheet section '{section_name}' is missing its header row.",
        )
    header = [value.strip() for value in rows[header_row_number - 1] if value.strip()]
    if not header:
        raise CatalogSheetError(
            f"The Probabilites sheet section '{section_name}' has an empty header row.",
        )

    table_rows: list[tuple[int, dict[str, str]]] = []
    for row_number in range(header_row_number + 1, len(rows) + 1):
        raw_row = rows[row_number - 1]
        if not raw_row or all(not value.strip() for value in raw_row):
            if table_rows:
                break
            continue
        if raw_row and raw_row[0].strip() in {
            GLOBAL_SETTINGS_SECTION,
            VARIANT_PROFILES_SECTION,
            EXTENSION_BALANCE_SECTION,
        }:
            break
        table_rows.append((row_number, map_row(header, raw_row)))

    if not table_rows:
        raise CatalogSheetError(
            f"The Probabilites sheet section '{section_name}' must contain at least one data row.",
        )
    return table_rows


def parse_calibration_sheet(sheet: Sheet | None) -> dict[str, Any] | None:
    if sheet is None or not sheet.rows:
        return None
    header = [value.strip() for value in sheet.rows[0]]
    required_columns = {"entryType", "entityId", "scope1", "scope2", "label", "weight", "value"}
    if not required_columns.issubset(header):
        return None
    data: dict[str, Any] = {}
    card_weights: dict[str, int] = {}
    variant_weights: dict[str, dict[str, dict[str, int]]] = {}
    for raw_row in sheet.rows[1:]:
        row = map_row(header, raw_row)
        entry_type = row.get("entryType", "").strip()
        if not entry_type:
            continue
        if entry_type == "meta":
            if row.get("entityId", "").strip() == "fingerprint":
                data["fingerprint"] = row.get("value", "")
            continue

        entity_id = row.get("entityId", "").strip()
        weight_text = row.get("weight", "").strip()
        if not entity_id:
            raise CatalogSheetError("Calibration data contains a row without entityId.")
        try:
            weight = int(clean_number(weight_text))
        except ValueError as error:
            raise CatalogSheetError(
                f"Calibration row for '{entity_id}' has an invalid weight '{weight_text}'.",
            ) from error
        if weight <= 0:
            raise CatalogSheetError(
                f"Calibration row for '{entity_id}' must keep a strictly positive weight.",
            )

        if entry_type == "card":
            card_weights[entity_id] = weight
            continue
        if entry_type == "variant":
            category = row.get("scope1", "").strip()
            code = row.get("scope2", "").strip()
            if not category or not code:
                raise CatalogSheetError("Calibration data contains an incomplete variant row.")
            profile_cache = variant_weights.setdefault(entity_id, {})
            profile_cache.setdefault(category, {})[code] = weight
            continue
        raise CatalogSheetError(f"Unsupported calibration entryType '{entry_type}'.")

    data["cardWeights"] = card_weights
    data["variantWeights"] = variant_weights
    return data


def apply_cached_card_weights(
    input_cards: list[dict[str, Any]],
    calibration: dict[str, Any],
) -> list[dict[str, Any]]:
    cached_weights = calibration.get("cardWeights")
    if not isinstance(cached_weights, dict):
        raise CatalogSheetError("Calibration data is missing cached card weights.")
    cards: list[dict[str, Any]] = []
    for card in input_cards:
        weight = cached_weights.get(card["id"])
        if not isinstance(weight, int) or weight <= 0:
            raise CatalogSheetError(
                f"Calibration data is missing a positive weight for card '{card['id']}'.",
            )
        cards.append(input_card_to_runtime_card(card, weight))
    return cards


def apply_cached_variant_weights(
    templates: list[dict[str, Any]],
    calibration: dict[str, Any],
) -> list[dict[str, Any]]:
    cached_weights = calibration.get("variantWeights")
    if not isinstance(cached_weights, dict):
        raise CatalogSheetError("Calibration data is missing cached variant weights.")

    variant_profiles: list[dict[str, Any]] = []
    for template in templates:
        profile_cache = cached_weights.get(template["id"])
        if not isinstance(profile_cache, dict):
            raise CatalogSheetError(
                f"Calibration data is missing cached weights for variant profile '{template['id']}'.",
            )
        sky_weights = profile_cache.get("skyQuality")
        finish_weights = profile_cache.get("finish")
        if not isinstance(sky_weights, dict) or not isinstance(finish_weights, dict):
            raise CatalogSheetError(
                f"Calibration data for variant profile '{template['id']}' is malformed.",
            )
        variant_profiles.append(
            {
                **template,
                "skyQualityWeights": [
                    {
                        "code": definition["code"],
                        "weight": require_cached_weight(
                            sky_weights,
                            definition["code"],
                            template["id"],
                            "sky quality",
                        ),
                    }
                    for definition in template["skyQualities"]
                ],
                "finishWeights": [
                    {
                        "code": definition["code"],
                        "weight": require_cached_weight(
                            finish_weights,
                            definition["code"],
                            template["id"],
                            "finish",
                        ),
                    }
                    for definition in template["finishes"]
                ],
            },
        )
    return variant_profiles


def require_cached_weight(
    weights: dict[str, Any],
    code: str,
    profile_id: str,
    weight_type: str,
) -> int:
    weight = weights.get(code)
    if not isinstance(weight, int) or weight <= 0:
        raise CatalogSheetError(
            f"Calibration data is missing a positive {weight_type} weight for "
            f"'{code}' in variant profile '{profile_id}'.",
        )
    return weight


def compute_variant_profiles(
    templates: list[dict[str, Any]],
    settings: GlobalSettings,
    variant_inputs: list[dict[str, Any]],
) -> tuple[list[dict[str, Any]], dict[str, dict[str, Any]]]:
    inputs_by_id = {row["variantProfileId"]: row for row in variant_inputs}
    profiles: list[dict[str, Any]] = []
    metrics: dict[str, dict[str, Any]] = {}

    for template in templates:
        profile_id = template["id"]
        input_row = inputs_by_id.get(profile_id)
        if input_row is None:
            raise CatalogSheetError(
                f"Probabilites: missing VariantProfiles row for '{profile_id}'.",
            )
        sky_codes = [definition["code"] for definition in template["skyQualities"]]
        finish_codes = [definition["code"] for definition in template["finishes"]]
        if sorted(sky_codes) != sorted(SUPPORTED_SKY_CODES):
            raise CatalogSheetError(
                f"Variant profile '{profile_id}' must expose exactly the sky qualities "
                f"{', '.join(SUPPORTED_SKY_CODES)}.",
            )
        if sorted(finish_codes) != sorted(SUPPORTED_FINISH_CODES):
            raise CatalogSheetError(
                f"Variant profile '{profile_id}' must expose exactly the finishes "
                f"{', '.join(SUPPORTED_FINISH_CODES)}.",
            )

        target_sky_probabilities: dict[str, Fraction] = {
            "suburban": probability_from_mean_days(settings, input_row["suburbanMeanDays"]),
            "rural": probability_from_mean_days(settings, input_row["ruralMeanDays"]),
            "mountain": probability_from_mean_days(settings, input_row["mountainMeanDays"]),
        }
        target_sky_probabilities["city"] = Fraction(1, 1) - sum(
            target_sky_probabilities.values(),
            start=Fraction(0, 1),
        )
        if target_sky_probabilities["city"] <= 0:
            raise CatalogSheetError(
                f"Variant profile '{profile_id}': derived city probability must stay strictly positive.",
            )

        target_finish_probabilities: dict[str, Fraction] = {
            "holographic": probability_from_mean_days(settings, input_row["holographicMeanDays"]),
        }
        target_finish_probabilities["standard"] = Fraction(1, 1) - target_finish_probabilities["holographic"]
        if target_finish_probabilities["standard"] <= 0:
            raise CatalogSheetError(
                f"Variant profile '{profile_id}': derived standard probability must stay strictly positive.",
            )

        sky_weights = weights_from_probability_order(
            target_sky_probabilities,
            [definition["code"] for definition in template["skyQualities"]],
        )
        finish_weights = weights_from_probability_order(
            target_finish_probabilities,
            [definition["code"] for definition in template["finishes"]],
        )

        profile = {
            **template,
            "skyQualityWeights": [
                {"code": definition["code"], "weight": weight}
                for definition, weight in zip(template["skyQualities"], sky_weights, strict=True)
            ],
            "finishWeights": [
                {"code": definition["code"], "weight": weight}
                for definition, weight in zip(template["finishes"], finish_weights, strict=True)
            ],
        }
        profiles.append(profile)
        metrics[profile_id] = {
            "targetSkyProbabilities": target_sky_probabilities,
            "targetFinishProbabilities": target_finish_probabilities,
            "actualSkyProbabilities": probability_map_from_weighted_codes(profile["skyQualityWeights"]),
            "actualFinishProbabilities": probability_map_from_weighted_codes(profile["finishWeights"]),
            "rarestSkyCode": rarest_code_from_weighted_codes(profile["skyQualityWeights"]),
        }

    unknown_variant_inputs = sorted(inputs_by_id.keys() - {profile["id"] for profile in templates})
    if unknown_variant_inputs:
        raise CatalogSheetError(
            "Probabilites: unknown variantProfileId(s): " + ", ".join(unknown_variant_inputs),
        )

    return profiles, metrics


def compute_cards(
    extensions: list[dict[str, Any]],
    input_cards: list[dict[str, Any]],
    settings: GlobalSettings,
    extension_inputs: list[dict[str, Any]],
    profile_metrics: dict[str, dict[str, Any]],
    extension_variant_profiles: dict[str, str],
) -> list[dict[str, Any]]:
    extension_inputs_by_id = {row["extensionId"]: row for row in extension_inputs}
    cards_by_extension: dict[str, list[dict[str, Any]]] = {}
    for card in input_cards:
        cards_by_extension.setdefault(card["extensionId"], []).append(card)

    output_cards: list[dict[str, Any]] = []
    for extension in extensions:
        extension_id = extension["id"]
        cards = cards_by_extension.get(extension_id, [])
        if not cards:
            continue
        extension_input = extension_inputs_by_id.get(extension_id)
        if extension_input is None:
            raise CatalogSheetError(
                f"Probabilites: missing ExtensionBalance row for '{extension_id}'.",
            )
        variant_profile_id = extension_variant_profiles[extension_id]
        metrics = profile_metrics.get(variant_profile_id)
        if metrics is None:
            raise CatalogSheetError(
                f"Missing computed variant profile metrics for '{variant_profile_id}'.",
            )

        cards_by_rarity: dict[str, list[dict[str, Any]]] = {}
        for card in cards:
            cards_by_rarity.setdefault(card["rarityLabel"], []).append(card)

        present_rarities = sorted(cards_by_rarity, key=rarity_sort_priority)
        if "Common" not in present_rarities:
            raise CatalogSheetError(
                f"Extension '{extension_id}' must contain at least one Common card.",
            )
        highest_rarity = present_rarities[-1]

        rarity_inverse_sums = {
            rarity: sum(
                Fraction(1, 1) / Fraction(card["cardRarityMultiplier"])
                for card in rarity_cards
            )
            for rarity, rarity_cards in cards_by_rarity.items()
        }

        target_rarity_probabilities: dict[str, Fraction] = {}
        if "Uncommon" in cards_by_rarity and highest_rarity != "Uncommon":
            uncommon_mean_days = extension_input["uncommonBaseCardMeanDays"]
            if uncommon_mean_days is None:
                raise CatalogSheetError(
                    f"Extension '{extension_id}': uncommonBaseCardMeanDays is required.",
                )
            target_rarity_probabilities["Uncommon"] = (
                rarity_inverse_sums["Uncommon"] / (settings.slots_per_day * Fraction(uncommon_mean_days))
            )
        if "Rare" in cards_by_rarity and highest_rarity != "Rare":
            rare_mean_days = extension_input["rareBaseCardMeanDays"]
            if rare_mean_days is None:
                raise CatalogSheetError(
                    f"Extension '{extension_id}': rareBaseCardMeanDays is required.",
                )
            target_rarity_probabilities["Rare"] = (
                rarity_inverse_sums["Rare"] / (settings.slots_per_day * Fraction(rare_mean_days))
            )

        if highest_rarity != "Common":
            rarest_card = max(
                cards_by_rarity[highest_rarity],
                key=lambda card: (card["cardRarityMultiplier"], card["id"]),
            )
            rarest_card_multiplier = Fraction(rarest_card["cardRarityMultiplier"])
            rarest_combo_probability = probability_from_mean_days(
                settings,
                extension_input["rarestComboMeanDays"],
            )
            rarest_sky_probability = metrics["actualSkyProbabilities"][metrics["rarestSkyCode"]]
            holographic_probability = metrics["actualFinishProbabilities"]["holographic"]
            if rarest_sky_probability <= 0 or holographic_probability <= 0:
                raise CatalogSheetError(
                    f"Extension '{extension_id}': the rarest sky quality and holographic finish "
                    "must both keep a strictly positive probability.",
                )
            target_rarity_probabilities[highest_rarity] = (
                rarest_combo_probability
                * rarity_inverse_sums[highest_rarity]
                * rarest_card_multiplier
                / rarest_sky_probability
                / holographic_probability
            )

        remaining_common_probability = Fraction(1, 1) - sum(
            target_rarity_probabilities.values(),
            start=Fraction(0, 1),
        )
        if remaining_common_probability <= 0:
            raise CatalogSheetError(
                f"Extension '{extension_id}': derived Common probability must stay strictly positive.",
            )
        target_rarity_probabilities["Common"] = remaining_common_probability

        target_card_probabilities: dict[str, Fraction] = {}
        for rarity, rarity_cards in cards_by_rarity.items():
            rarity_probability = target_rarity_probabilities[rarity]
            inverse_sum = rarity_inverse_sums[rarity]
            if rarity_probability <= 0:
                raise CatalogSheetError(
                    f"Extension '{extension_id}': rarity '{rarity}' ended up with a non-positive probability.",
                )
            for card in rarity_cards:
                card_probability = (
                    rarity_probability
                    * (Fraction(1, 1) / Fraction(card["cardRarityMultiplier"]))
                    / inverse_sum
                )
                target_card_probabilities[card["id"]] = card_probability

        weights = weights_from_probability_order(
            target_card_probabilities,
            [card["id"] for card in cards],
        )
        output_cards.extend(
            input_card_to_runtime_card(card, draw_weight)
            for card, draw_weight in zip(cards, weights, strict=True)
        )

    unknown_extension_inputs = sorted(extension_inputs_by_id.keys() - {extension["id"] for extension in extensions})
    if unknown_extension_inputs:
        raise CatalogSheetError(
            "Probabilites: unknown extensionId(s): " + ", ".join(unknown_extension_inputs),
        )

    return output_cards


def probability_from_mean_days(settings: GlobalSettings, mean_days: Decimal) -> Fraction:
    return Fraction(1, 1) / (settings.slots_per_day * Fraction(mean_days))


def weights_from_probability_order(
    probabilities: dict[str, Fraction],
    ordered_codes: list[str],
) -> list[int]:
    weights = [
        max(1, round_fraction_to_int(probabilities[code] * Fraction(WEIGHT_SCALE)))
        for code in ordered_codes
    ]
    return reduce_weights(weights)


def round_fraction_to_int(value: Fraction) -> int:
    decimal_value = Decimal(value.numerator) / Decimal(value.denominator)
    return int(decimal_value.quantize(Decimal("1"), rounding=ROUND_HALF_UP))


def reduce_weights(weights: list[int]) -> list[int]:
    current_gcd = 0
    for weight in weights:
        current_gcd = gcd(current_gcd, weight)
    divisor = current_gcd or 1
    return [weight // divisor for weight in weights]


def build_catalogue_sheet_rows_from_json(
    extensions: list[dict[str, Any]],
    cards: list[dict[str, Any]],
) -> list[list[object | None]]:
    multipliers_by_card_id = build_multipliers_from_cards(cards)
    rows: list[list[object | None]] = [CATALOGUE_FIELDS]
    rows.extend(row_to_values(build_extension_catalogue_row(extension)) for extension in extensions)
    rows.extend(
        row_to_values(card_to_catalogue_row(card, multipliers_by_card_id[card["id"]]))
        for card in cards
    )
    return rows


def build_catalogue_sheet_rows_from_input(
    extensions: list[dict[str, Any]],
    cards: list[dict[str, Any]],
) -> list[list[object | None]]:
    rows: list[list[object | None]] = [CATALOGUE_FIELDS]
    rows.extend(row_to_values(build_extension_catalogue_row(extension)) for extension in extensions)
    rows.extend(row_to_values(input_card_to_catalogue_row(card)) for card in cards)
    return rows


def build_extension_catalogue_row(extension: dict[str, Any]) -> dict[str, object | None]:
    return {
        "rowType": "extension",
        "extensionId": extension["id"],
        "extensionName": extension["name"],
        "extensionCoverImageRef": extension["coverImageRef"],
    }


def build_probability_sheet_rows(
    settings: GlobalSettings,
    variant_inputs: list[dict[str, Any]],
    extension_inputs: list[dict[str, Any]],
) -> list[list[object | None]]:
    rows: list[list[object | None]] = []
    rows.append([GLOBAL_SETTINGS_SECTION])
    rows.append(["parameter", "value", "description"])
    rows.append(["cardsPerDraw", settings.cards_per_draw, "Cartes tirees a chaque ouverture"])
    rows.append(["drawCooldownHours", settings.draw_cooldown_hours, "Heures entre deux recharges"])
    rows.append([])
    rows.append([VARIANT_PROFILES_SECTION])
    rows.append(VARIANT_PROFILE_FIELDS + ["notes"])
    for row in variant_inputs:
        rows.append(
            [
                row["variantProfileId"],
                row["suburbanMeanDays"],
                row["ruralMeanDays"],
                row["mountainMeanDays"],
                row["holographicMeanDays"],
                "Ville et Standard sont derives automatiquement.",
            ],
        )
    rows.append([])
    rows.append([EXTENSION_BALANCE_SECTION])
    rows.append(EXTENSION_BALANCE_FIELDS + ["notes"])
    for row in extension_inputs:
        rows.append(
            [
                row["extensionId"],
                row["rarestComboMeanDays"],
                row.get("uncommonBaseCardMeanDays"),
                row.get("rareBaseCardMeanDays"),
                "Common et la rarete la plus haute sont derives automatiquement.",
            ],
        )
    return rows


def result_ref(column_index: int, row_index: int) -> str:
    return f"{column_label(column_index)}{row_index}"


def build_results_sheet_rows(
    settings: GlobalSettings,
    extensions: list[dict[str, Any]],
    cards: list[dict[str, Any]],
    variant_profiles: list[dict[str, Any]],
    extension_inputs: list[dict[str, Any]],
) -> list[list[object | None]]:
    profile_by_id = {profile["id"]: profile for profile in variant_profiles}
    extension_variant_profiles = require_single_variant_profile_per_extension(cards)
    cards_by_extension: dict[str, list[dict[str, Any]]] = {}
    for card in cards:
        cards_by_extension.setdefault(card["extensionId"], []).append(card)

    rows: list[list[object | None]] = []
    slots_per_day_formula = "((Probabilites!$B$3*24)/Probabilites!$B$4)"
    calibration_last_row = 2 + len(cards) + sum(
        len(profile["skyQualityWeights"]) + len(profile["finishWeights"])
        for profile in variant_profiles
    )
    calibration_entry_type = f"_Calibration!$A$2:$A${calibration_last_row}"
    calibration_entity_id = f"_Calibration!$B$2:$B${calibration_last_row}"
    calibration_scope1 = f"_Calibration!$C$2:$C${calibration_last_row}"
    calibration_scope2 = f"_Calibration!$D$2:$D${calibration_last_row}"
    calibration_weight = f"_Calibration!$F$2:$F${calibration_last_row}"

    rows.append(["VariantProfiles"])
    rows.append(
        [
            "variantProfileId",
            "category",
            "code",
            "label",
            "weight",
            "probability",
            "meanSlots",
            "meanPacks",
            "meanDays",
        ],
    )
    for profile in variant_profiles:
        for category, definitions_key in [
            ("skyQuality", "skyQualities"),
            ("finish", "finishes"),
        ]:
            for definition in profile[definitions_key]:
                row_number = len(rows) + 1
                weight_cell = result_ref(5, row_number)
                probability_cell = result_ref(6, row_number)
                mean_slots_cell = result_ref(7, row_number)
                rows.append(
                    [
                        profile["id"],
                        category,
                        definition["code"],
                        definition["label"],
                        FormulaCell(
                            (
                                f'SUMPRODUCT(({calibration_entry_type}="variant")'
                                f"*({calibration_entity_id}={result_ref(1, row_number)})"
                                f"*({calibration_scope1}={result_ref(2, row_number)})"
                                f"*({calibration_scope2}={result_ref(3, row_number)})"
                                f"*{calibration_weight})"
                            ),
                        ),
                        FormulaCell(
                            (
                                f'{weight_cell}/SUMPRODUCT(({calibration_entry_type}="variant")'
                                f"*({calibration_entity_id}={result_ref(1, row_number)})"
                                f"*({calibration_scope1}={result_ref(2, row_number)})"
                                f"*{calibration_weight})"
                            ),
                        ),
                        FormulaCell(f"1/{probability_cell}"),
                        FormulaCell(f"{mean_slots_cell}/Probabilites!$B$3"),
                        FormulaCell(f"{mean_slots_cell}/{slots_per_day_formula}"),
                    ],
                )
    rows.append([])
    rows.append(["RaritySummary"])
    rows.append(
        [
            "extensionId",
            "rarityLabel",
            "cardCount",
            "totalWeight",
            "rarityProbability",
            "baseCardWeight",
            "baseCardMeanDays",
        ],
    )
    for extension in extensions:
        extension_cards = cards_by_extension.get(extension["id"], [])
        cards_by_rarity: dict[str, list[dict[str, Any]]] = {}
        for card in extension_cards:
            cards_by_rarity.setdefault(card["rarityLabel"], []).append(card)
        for rarity in sorted(cards_by_rarity, key=rarity_sort_priority):
            row_number = len(rows) + 1
            extension_total_weight = (
                f'SUMPRODUCT(({calibration_entry_type}="card")'
                f"*({calibration_scope1}={result_ref(1, row_number)})"
                f"*{calibration_weight})"
            )
            rows.append(
                [
                    extension["id"],
                    rarity,
                    FormulaCell(
                        (
                            f'SUMPRODUCT(--({calibration_entry_type}="card"),'
                            f"--({calibration_scope1}={result_ref(1, row_number)}),"
                            f"--({calibration_scope2}={result_ref(2, row_number)}))"
                        ),
                    ),
                    FormulaCell(
                        (
                            f'SUMPRODUCT(({calibration_entry_type}="card")'
                            f"*({calibration_scope1}={result_ref(1, row_number)})"
                            f"*({calibration_scope2}={result_ref(2, row_number)})"
                            f"*{calibration_weight})"
                        ),
                    ),
                    FormulaCell(f"{result_ref(4, row_number)}/{extension_total_weight}"),
                    FormulaCell(
                        (
                            f"AGGREGATE(14,6,"
                            f"{calibration_weight}/(({calibration_entry_type}=\"card\")"
                            f"*({calibration_scope1}={result_ref(1, row_number)})"
                            f"*({calibration_scope2}={result_ref(2, row_number)})),1)"
                        ),
                    ),
                    FormulaCell(
                        f"1/(({result_ref(6, row_number)}/{extension_total_weight})*{slots_per_day_formula})",
                    ),
                ],
            )
    rows.append([])
    rows.append(["Cards"])
    rows.append(
        [
            "extensionId",
            "cardId",
            "name",
            "rarityLabel",
            "drawWeight",
            "probability",
            "meanSlots",
            "meanPacks",
            "meanDays",
        ],
    )
    for extension in extensions:
        extension_cards = cards_by_extension.get(extension["id"], [])
        for card in extension_cards:
            row_number = len(rows) + 1
            extension_total_weight = (
                f'SUMPRODUCT(({calibration_entry_type}="card")'
                f"*({calibration_scope1}={result_ref(1, row_number)})"
                f"*{calibration_weight})"
            )
            weight_cell = result_ref(5, row_number)
            probability_cell = result_ref(6, row_number)
            mean_slots_cell = result_ref(7, row_number)
            rows.append(
                [
                    card["extensionId"],
                    card["id"],
                    card["name"],
                    card["rarityLabel"],
                    FormulaCell(
                        (
                            f'SUMPRODUCT(({calibration_entry_type}="card")'
                            f"*({calibration_scope1}={result_ref(1, row_number)})"
                            f"*({calibration_entity_id}={result_ref(2, row_number)})"
                            f"*{calibration_weight})"
                        ),
                    ),
                    FormulaCell(f"{weight_cell}/{extension_total_weight}"),
                    FormulaCell(f"1/{probability_cell}"),
                    FormulaCell(f"{mean_slots_cell}/Probabilites!$B$3"),
                    FormulaCell(f"{mean_slots_cell}/{slots_per_day_formula}"),
                ],
            )
    rows.append([])
    rows.append(["RarestComboChecks"])
    rows.append(
        [
            "extensionId",
            "variantProfileId",
            "highestRarity",
            "rarestCardId",
            "rarestSkyCode",
            "holographicWeight",
            "comboProbability",
            "actualMeanDays",
            "targetMeanDays",
        ],
    )
    for extension in extensions:
        extension_cards = cards_by_extension.get(extension["id"], [])
        cards_by_rarity: dict[str, list[dict[str, Any]]] = {}
        for card in extension_cards:
            cards_by_rarity.setdefault(card["rarityLabel"], []).append(card)
        highest_rarity = sorted(cards_by_rarity, key=rarity_sort_priority)[-1]
        rarest_card = min(
            cards_by_rarity[highest_rarity],
            key=lambda card: (card["drawWeight"], card["id"]),
        )
        variant_profile = profile_by_id[extension_variant_profiles[extension["id"]]]
        rarest_sky = rarest_weighted_code(variant_profile["skyQualityWeights"])
        row_number = len(rows) + 1
        extension_total_weight = (
            f'SUMPRODUCT(({calibration_entry_type}="card")'
            f"*({calibration_scope1}={result_ref(1, row_number)})"
            f"*{calibration_weight})"
        )
        rarest_card_weight = (
            f'SUMPRODUCT(({calibration_entry_type}="card")'
            f"*({calibration_scope1}={result_ref(1, row_number)})"
            f"*({calibration_entity_id}={result_ref(4, row_number)})"
            f"*{calibration_weight})"
        )
        sky_total_weight = (
            f'SUMPRODUCT(({calibration_entry_type}="variant")'
            f"*({calibration_entity_id}={result_ref(2, row_number)})"
            f'*({calibration_scope1}="skyQuality")'
            f"*{calibration_weight})"
        )
        rarest_sky_weight = (
            f'SUMPRODUCT(({calibration_entry_type}="variant")'
            f"*({calibration_entity_id}={result_ref(2, row_number)})"
            f'*({calibration_scope1}="skyQuality")'
            f"*({calibration_scope2}={result_ref(5, row_number)})"
            f"*{calibration_weight})"
        )
        finish_total_weight = (
            f'SUMPRODUCT(({calibration_entry_type}="variant")'
            f"*({calibration_entity_id}={result_ref(2, row_number)})"
            f'*({calibration_scope1}="finish")'
            f"*{calibration_weight})"
        )
        rows.append(
            [
                extension["id"],
                variant_profile["id"],
                highest_rarity,
                rarest_card["id"],
                rarest_sky["code"],
                FormulaCell(
                    (
                        f'SUMPRODUCT(({calibration_entry_type}="variant")'
                        f"*({calibration_entity_id}={result_ref(2, row_number)})"
                        f'*({calibration_scope1}="finish")'
                        f'*({calibration_scope2}="holographic")'
                        f"*{calibration_weight})"
                    ),
                ),
                FormulaCell(
                    (
                        f"({rarest_card_weight}/{extension_total_weight})"
                        f"*({rarest_sky_weight}/{sky_total_weight})"
                        f"*({result_ref(6, row_number)}/{finish_total_weight})"
                    ),
                ),
                FormulaCell(f"1/({result_ref(7, row_number)}*{slots_per_day_formula})"),
                FormulaCell(
                    f"INDEX(Probabilites!$B:$B,MATCH({result_ref(1, row_number)},Probabilites!$A:$A,0))",
                ),
            ],
        )
    return rows


def build_calibration_sheet_rows(
    fingerprint: str,
    cards: list[dict[str, Any]],
    variant_profiles: list[dict[str, Any]],
) -> list[list[object | None]]:
    rows: list[list[object | None]] = [
        ["entryType", "entityId", "scope1", "scope2", "label", "weight", "value"],
        ["meta", "fingerprint", "", "", "", "", fingerprint],
    ]
    for card in cards:
        rows.append(
            [
                "card",
                card["id"],
                card["extensionId"],
                card["rarityLabel"],
                card["name"],
                card["drawWeight"],
                "",
            ],
        )
    for profile in variant_profiles:
        sky_labels = {definition["code"]: definition["label"] for definition in profile["skyQualities"]}
        finish_labels = {definition["code"]: definition["label"] for definition in profile["finishes"]}
        for entry in profile["skyQualityWeights"]:
            rows.append(
                [
                    "variant",
                    profile["id"],
                    "skyQuality",
                    entry["code"],
                    sky_labels.get(entry["code"], entry["code"]),
                    entry["weight"],
                    "",
                ],
            )
        for entry in profile["finishWeights"]:
            rows.append(
                [
                    "variant",
                    profile["id"],
                    "finish",
                    entry["code"],
                    finish_labels.get(entry["code"], entry["code"]),
                    entry["weight"],
                    "",
                ],
            )
    return rows


def build_export_variant_inputs(
    variant_profiles: list[dict[str, Any]],
    settings: GlobalSettings,
) -> list[dict[str, Any]]:
    inputs: list[dict[str, Any]] = []
    for profile in variant_profiles:
        sky_probabilities = probability_map_from_weighted_codes(profile["skyQualityWeights"])
        finish_probabilities = probability_map_from_weighted_codes(profile["finishWeights"])
        inputs.append(
            {
                "variantProfileId": profile["id"],
                "suburbanMeanDays": decimal_cell(mean_days(sky_probabilities["suburban"], settings), 15),
                "ruralMeanDays": decimal_cell(mean_days(sky_probabilities["rural"], settings), 15),
                "mountainMeanDays": decimal_cell(mean_days(sky_probabilities["mountain"], settings), 15),
                "holographicMeanDays": decimal_cell(mean_days(finish_probabilities["holographic"], settings), 15),
            },
        )
    return inputs


def build_export_extension_inputs(
    cards: list[dict[str, Any]],
    extension_variant_profiles: dict[str, str],
    variant_profiles_by_id: dict[str, dict[str, Any]],
    settings: GlobalSettings,
) -> list[dict[str, Any]]:
    cards_by_extension: dict[str, list[dict[str, Any]]] = {}
    for card in cards:
        cards_by_extension.setdefault(card["extensionId"], []).append(card)

    inputs: list[dict[str, Any]] = []
    for extension_id, extension_cards in cards_by_extension.items():
        total_weight = sum(card["drawWeight"] for card in extension_cards)
        cards_by_rarity: dict[str, list[dict[str, Any]]] = {}
        for card in extension_cards:
            cards_by_rarity.setdefault(card["rarityLabel"], []).append(card)
        highest_rarity = sorted(cards_by_rarity, key=rarity_sort_priority)[-1]
        highest_cards = cards_by_rarity[highest_rarity]
        rarest_card = min(highest_cards, key=lambda card: (card["drawWeight"], card["id"]))

        variant_profile = variant_profiles_by_id[extension_variant_profiles[extension_id]]
        rarest_sky = rarest_weighted_code(variant_profile["skyQualityWeights"])
        holographic = weighted_code_lookup(variant_profile["finishWeights"], "holographic")

        uncommon_row: Decimal | None = None
        if "Uncommon" in cards_by_rarity:
            uncommon_row = decimal_cell(
                mean_days(
                    Fraction(max(card["drawWeight"] for card in cards_by_rarity["Uncommon"]), total_weight),
                    settings,
                ),
                15,
            )

        rare_row: Decimal | None = None
        if "Rare" in cards_by_rarity:
            rare_row = decimal_cell(
                mean_days(
                    Fraction(max(card["drawWeight"] for card in cards_by_rarity["Rare"]), total_weight),
                    settings,
                ),
                15,
            )

        combo_probability = (
            Fraction(rarest_card["drawWeight"], total_weight)
            * Fraction(rarest_sky["weight"], total_weight_of_codes(variant_profile["skyQualityWeights"]))
            * Fraction(holographic["weight"], total_weight_of_codes(variant_profile["finishWeights"]))
        )
        inputs.append(
            {
                "extensionId": extension_id,
                "rarestComboMeanDays": decimal_cell(mean_days(combo_probability, settings), 15),
                "uncommonBaseCardMeanDays": uncommon_row,
                "rareBaseCardMeanDays": rare_row,
            },
        )
    return inputs


def build_visible_fingerprint(
    extensions: list[dict[str, Any]],
    cards: list[dict[str, Any]],
    settings: GlobalSettings,
    variant_inputs: list[dict[str, Any]],
    extension_inputs: list[dict[str, Any]],
) -> str:
    payload = {
        "settings": {
            "cardsPerDraw": settings.cards_per_draw,
            "drawCooldownHours": stable_number(settings.draw_cooldown_hours),
        },
        "extensions": [
            {
                "id": extension["id"],
                "name": extension["name"],
                "coverImageRef": extension["coverImageRef"],
            }
            for extension in extensions
        ],
        "cards": [
            canonical_catalogue_payload(card)
            for card in cards
        ],
        "variantInputs": [
            {
                "variantProfileId": row["variantProfileId"],
                "suburbanMeanDays": stable_number(row["suburbanMeanDays"]),
                "ruralMeanDays": stable_number(row["ruralMeanDays"]),
                "mountainMeanDays": stable_number(row["mountainMeanDays"]),
                "holographicMeanDays": stable_number(row["holographicMeanDays"]),
            }
            for row in variant_inputs
        ],
        "extensionInputs": [
            {
                "extensionId": row["extensionId"],
                "rarestComboMeanDays": stable_number(row["rarestComboMeanDays"]),
                "uncommonBaseCardMeanDays": stable_number(row["uncommonBaseCardMeanDays"]),
                "rareBaseCardMeanDays": stable_number(row["rareBaseCardMeanDays"]),
            }
            for row in extension_inputs
        ],
    }
    encoded = json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def canonical_catalogue_payload(card: dict[str, Any]) -> dict[str, Any]:
    astronomy = card["astronomy"]
    coordinates = astronomy["coordinates"]
    right_ascension = coordinates["rightAscension"]
    declination = coordinates["declination"]
    details = astronomy["details"]
    payload: dict[str, Any] = {
        "id": card["id"],
        "extensionId": card["extensionId"],
        "name": card["name"],
        "rarityLabel": card["rarityLabel"],
        "cardRarityMultiplier": stable_number(card["cardRarityMultiplier"]),
        "imageRef": card["imageRef"],
        "variantProfileId": card["variantProfileId"],
        "commonName": astronomy.get("commonName"),
        "primaryCatalogName": astronomy["primaryCatalogName"],
        "catalogNumber": astronomy["catalogNumber"],
        "objectFamily": astronomy["objectFamily"],
        "objectTypeLabel": astronomy["objectTypeLabel"],
        "constellation": astronomy["constellation"],
        "mainSeason": astronomy["mainSeason"],
        "rightAscensionHours": right_ascension["hours"],
        "rightAscensionMinutes": right_ascension["minutes"],
        "rightAscensionSeconds": stable_number(right_ascension["seconds"]),
        "rightAscensionLabel": right_ascension["label"],
        "declinationSign": declination["sign"],
        "declinationDegrees": declination["degrees"],
        "declinationArcMinutes": declination["arcMinutes"],
        "declinationArcSeconds": declination["arcSeconds"],
        "declinationLabel": declination["label"],
        "coordinatesLabel": coordinates["label"],
        "shortDescription": astronomy["shortDescription"],
        "detailType": details["detailType"],
    }
    if "distance" in details:
        payload["distanceLightYears"] = stable_number(details["distance"]["lightYears"])
        payload["distanceLabel"] = details["distance"]["label"]
    if "realSize" in details:
        payload["realSizeLightYears"] = stable_number(details["realSize"]["lightYears"])
        payload["realSizeLabel"] = details["realSize"]["label"]
    if "visualSize" in details:
        visual_size = details["visualSize"]
        payload["visualFullMoonWidth"] = stable_number(visual_size["fullMoonWidth"])
        payload["visualFullMoonHeight"] = stable_number(visual_size["fullMoonHeight"])
        payload["angularWidthDegrees"] = visual_size["angularWidth"]["degrees"]
        payload["angularWidthArcMinutes"] = visual_size["angularWidth"]["arcMinutes"]
        payload["angularWidthArcSeconds"] = visual_size["angularWidth"]["arcSeconds"]
        payload["angularWidthLabel"] = visual_size["angularWidth"]["label"]
        payload["angularHeightDegrees"] = visual_size["angularHeight"]["degrees"]
        payload["angularHeightArcMinutes"] = visual_size["angularHeight"]["arcMinutes"]
        payload["angularHeightArcSeconds"] = visual_size["angularHeight"]["arcSeconds"]
        payload["angularHeightLabel"] = visual_size["angularHeight"]["label"]
        payload["visualSizeLabel"] = visual_size["label"]
    if "absoluteMagnitude" in details:
        payload["absoluteMagnitudeValue"] = stable_number(details["absoluteMagnitude"]["value"])
        payload["absoluteMagnitudeLabel"] = details["absoluteMagnitude"]["label"]
    return payload


def build_multipliers_from_cards(cards: list[dict[str, Any]]) -> dict[str, Decimal]:
    max_weight_by_rarity: dict[str, int] = {}
    for card in cards:
        rarity = card["rarityLabel"]
        max_weight_by_rarity[rarity] = max(max_weight_by_rarity.get(rarity, 0), card["drawWeight"])
    multipliers: dict[str, Decimal] = {}
    for card in cards:
        multipliers[card["id"]] = (
            Decimal(max_weight_by_rarity[card["rarityLabel"]]) / Decimal(card["drawWeight"])
        ).quantize(Decimal("0.000000000000001"))
    return multipliers


def build_export_input_cards(cards: list[dict[str, Any]]) -> list[dict[str, Any]]:
    multipliers_by_card_id = build_multipliers_from_cards(cards)
    return [
        {
            **card,
            "cardRarityMultiplier": multipliers_by_card_id[card["id"]],
        }
        for card in cards
    ]


def card_to_catalogue_row(card: dict[str, Any], card_rarity_multiplier: Decimal) -> dict[str, object | None]:
    astronomy = card["astronomy"]
    coordinates = astronomy["coordinates"]
    right_ascension = coordinates["rightAscension"]
    declination = coordinates["declination"]
    details = astronomy["details"]
    row = {
        "rowType": "card",
        "extensionId": card["extensionId"],
        "cardId": card["id"],
        "name": card["name"],
        "rarityLabel": card["rarityLabel"],
        "cardRarityMultiplier": card_rarity_multiplier,
        "imageRef": card["imageRef"],
        "variantProfileId": card["variantProfileId"],
        "commonName": astronomy.get("commonName") or "",
        "primaryCatalogName": astronomy["primaryCatalogName"],
        "catalogNumber": astronomy["catalogNumber"],
        "objectFamily": astronomy["objectFamily"],
        "objectTypeLabel": astronomy["objectTypeLabel"],
        "constellation": astronomy["constellation"],
        "mainSeason": astronomy["mainSeason"],
        "rightAscensionHours": right_ascension["hours"],
        "rightAscensionMinutes": right_ascension["minutes"],
        "rightAscensionSeconds": right_ascension["seconds"],
        "rightAscensionLabel": right_ascension["label"],
        "declinationSign": declination["sign"],
        "declinationDegrees": declination["degrees"],
        "declinationArcMinutes": declination["arcMinutes"],
        "declinationArcSeconds": declination["arcSeconds"],
        "declinationLabel": declination["label"],
        "coordinatesLabel": coordinates["label"],
        "shortDescription": astronomy["shortDescription"],
        "detailType": details["detailType"],
    }

    if "distance" in details:
        row["distanceLightYears"] = details["distance"]["lightYears"]
        row["distanceLabel"] = details["distance"]["label"]
    if "realSize" in details:
        row["realSizeLightYears"] = details["realSize"]["lightYears"]
        row["realSizeLabel"] = details["realSize"]["label"]
    if "visualSize" in details:
        visual_size = details["visualSize"]
        angular_width = visual_size["angularWidth"]
        angular_height = visual_size["angularHeight"]
        row.update(
            {
                "visualFullMoonWidth": visual_size["fullMoonWidth"],
                "visualFullMoonHeight": visual_size["fullMoonHeight"],
                "angularWidthDegrees": angular_width["degrees"],
                "angularWidthArcMinutes": angular_width["arcMinutes"],
                "angularWidthArcSeconds": angular_width["arcSeconds"],
                "angularWidthLabel": angular_width["label"],
                "angularHeightDegrees": angular_height["degrees"],
                "angularHeightArcMinutes": angular_height["arcMinutes"],
                "angularHeightArcSeconds": angular_height["arcSeconds"],
                "angularHeightLabel": angular_height["label"],
                "visualSizeLabel": visual_size["label"],
            },
        )
    if "absoluteMagnitude" in details:
        row["absoluteMagnitudeValue"] = details["absoluteMagnitude"]["value"]
        row["absoluteMagnitudeLabel"] = details["absoluteMagnitude"]["label"]

    return row


def input_card_to_catalogue_row(card: dict[str, Any]) -> dict[str, object | None]:
    return card_to_catalogue_row(card, card["cardRarityMultiplier"])


def input_card_to_runtime_card(card: dict[str, Any], draw_weight: int) -> dict[str, Any]:
    return {
        "id": card["id"],
        "extensionId": card["extensionId"],
        "name": card["name"],
        "rarityLabel": card["rarityLabel"],
        "drawWeight": draw_weight,
        "imageRef": card["imageRef"],
        "variantProfileId": card["variantProfileId"],
        "astronomy": card["astronomy"],
    }


def probability_map_from_weighted_codes(entries: list[dict[str, Any]]) -> dict[str, Fraction]:
    total_weight = total_weight_of_codes(entries)
    return {
        entry["code"]: Fraction(entry["weight"], total_weight)
        for entry in entries
    }


def rarest_code_from_weighted_codes(entries: list[dict[str, Any]]) -> str:
    return min(entries, key=lambda entry: (entry["weight"], entry["code"]))["code"]


def rarest_weighted_code(entries: list[dict[str, Any]]) -> dict[str, Any]:
    return min(entries, key=lambda entry: (entry["weight"], entry["code"]))


def weighted_code_lookup(entries: list[dict[str, Any]], code: str) -> dict[str, Any]:
    for entry in entries:
        if entry["code"] == code:
            return entry
    raise CatalogSheetError(f"Missing weighted code '{code}'.")


def total_weight_of_codes(entries: list[dict[str, Any]]) -> int:
    total = sum(entry["weight"] for entry in entries)
    if total <= 0:
        raise CatalogSheetError("Weights must stay strictly positive.")
    return total


def mean_slots(probability: Fraction) -> Fraction:
    return Fraction(1, 1) / probability


def mean_packs(probability: Fraction, settings: GlobalSettings) -> Fraction:
    return mean_slots(probability) / Fraction(settings.cards_per_draw, 1)


def mean_days(probability: Fraction, settings: GlobalSettings) -> Fraction:
    return mean_slots(probability) / settings.slots_per_day


def default_global_settings() -> GlobalSettings:
    return GlobalSettings(
        cards_per_draw=5,
        draw_cooldown_hours=Decimal("6"),
    )


def require_single_variant_profile_per_extension(cards: list[dict[str, Any]]) -> dict[str, str]:
    profiles_by_extension: dict[str, set[str]] = {}
    for card in cards:
        profiles_by_extension.setdefault(card["extensionId"], set()).add(card["variantProfileId"])

    mapping: dict[str, str] = {}
    for extension_id, variant_profile_ids in profiles_by_extension.items():
        if len(variant_profile_ids) != 1:
            raise CatalogSheetError(
                f"Extension '{extension_id}' references multiple variant profiles: "
                + ", ".join(sorted(variant_profile_ids))
                + ". This workbook v1 expects exactly one variant profile per extension."
            )
        mapping[extension_id] = next(iter(variant_profile_ids))
    return mapping


def row_to_values(row: dict[str, object | None]) -> list[object | None]:
    return [row.get(field, "") for field in CATALOGUE_FIELDS]


def map_row(header: list[str], raw_row: list[str]) -> dict[str, str]:
    return {
        key: (raw_row[index].strip() if index < len(raw_row) else "")
        for index, key in enumerate(header)
    }


def load_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        handle.write(render_json(strip_none(payload)))
        handle.write("\n")


def require_text(row: dict[str, str], field: str, row_number: int, sheet_name: str) -> str:
    value = optional_text(row, field)
    if value is None:
        raise CatalogSheetError(f"{sheet_name} row {row_number}: '{field}' is required.")
    return value


def optional_text(row: dict[str, str], field: str) -> str | None:
    value = row.get(field, "").strip()
    return value or None


def require_sign(row: dict[str, str], field: str, row_number: int, sheet_name: str) -> str:
    value = require_text(row, field, row_number, sheet_name)
    if value not in {"+", "-"}:
        raise CatalogSheetError(f"{sheet_name} row {row_number}: '{field}' must be '+' or '-'.")
    return value


def parse_int(
    row: dict[str, str],
    field: str,
    row_number: int,
    sheet_name: str,
    required: bool,
) -> int | None:
    raw_value = optional_text(row, field)
    if raw_value is None:
        if required:
            raise CatalogSheetError(f"{sheet_name} row {row_number}: '{field}' is required.")
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
            f"{sheet_name} row {row_number}: '{field}' must be an integer. Received '{raw_value}'.",
        ) from error


def parse_float(
    row: dict[str, str],
    field: str,
    row_number: int,
    sheet_name: str,
    required: bool,
) -> float | None:
    raw_value = optional_text(row, field)
    if raw_value is None:
        if required:
            raise CatalogSheetError(f"{sheet_name} row {row_number}: '{field}' is required.")
        return None

    cleaned = clean_number(raw_value)
    try:
        return float(cleaned)
    except ValueError as error:
        raise CatalogSheetError(
            f"{sheet_name} row {row_number}: '{field}' must be a number. Received '{raw_value}'.",
        ) from error


def parse_decimal(
    row: dict[str, str],
    field: str,
    row_number: int,
    sheet_name: str,
    required: bool,
) -> Decimal | None:
    raw_value = optional_text(row, field)
    if raw_value is None:
        if required:
            raise CatalogSheetError(f"{sheet_name} row {row_number}: '{field}' is required.")
        return None
    return parse_decimal_value(raw_value, field, sheet_name, row_number=row_number)


def parse_decimal_value(
    raw_value: str | None,
    field: str,
    sheet_name: str,
    row_number: int | None = None,
) -> Decimal:
    if raw_value is None or not str(raw_value).strip():
        location = f"{sheet_name}: '{field}' is required."
        raise CatalogSheetError(location)
    cleaned = clean_number(str(raw_value))
    try:
        return Decimal(cleaned)
    except Exception as error:
        location = f"{sheet_name}: '{field}' must be a number. Received '{raw_value}'."
        if row_number is not None:
            location = f"{sheet_name} row {row_number}: '{field}' must be a number. Received '{raw_value}'."
        raise CatalogSheetError(location) from error


def require_positive_fraction_value(
    raw_value: str | None,
    field: str,
    sheet_name: str,
    row_number: int | None = None,
) -> Fraction:
    value = Fraction(parse_decimal_value(raw_value, field, sheet_name, row_number=row_number))
    if value <= 0:
        if row_number is not None:
            raise CatalogSheetError(
                f"{sheet_name} row {row_number}: '{field}' must be strictly positive.",
            )
        raise CatalogSheetError(f"{sheet_name}: '{field}' must be strictly positive.")
    return value


def parse_int_value(raw_value: str | None, field: str, sheet_name: str) -> int:
    if raw_value is None or not str(raw_value).strip():
        raise CatalogSheetError(f"{sheet_name}: '{field}' is required.")
    cleaned = clean_number(str(raw_value))
    try:
        if any(character in cleaned for character in {".", "e", "E"}):
            value = float(cleaned)
            if not value.is_integer():
                raise ValueError
            return int(value)
        return int(cleaned)
    except ValueError as error:
        raise CatalogSheetError(
            f"{sheet_name}: '{field}' must be an integer. Received '{raw_value}'.",
        ) from error


def require_positive_decimal(
    row: dict[str, str],
    field: str,
    row_number: int,
    sheet_name: str,
) -> Decimal:
    value = parse_decimal(row, field, row_number, sheet_name, required=True)
    if value is None or value <= 0:
        raise CatalogSheetError(
            f"{sheet_name} row {row_number}: '{field}' must be strictly positive.",
        )
    return value


def optional_positive_decimal(
    row: dict[str, str],
    field: str,
    row_number: int,
    sheet_name: str,
) -> Decimal | None:
    value = parse_decimal(row, field, row_number, sheet_name, required=False)
    if value is None:
        return None
    if value <= 0:
        raise CatalogSheetError(
            f"{sheet_name} row {row_number}: '{field}' must be strictly positive when provided.",
        )
    return value


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


def build_details(
    row: dict[str, str],
    row_number: int,
    sheet_name: str,
    detail_type: str,
) -> dict[str, Any]:
    if detail_type == "deep_sky":
        details = {
            "detailType": detail_type,
            "distance": build_light_year_measurement(
                row,
                row_number,
                sheet_name=sheet_name,
                value_field="distanceLightYears",
                label_field="distanceLabel",
                required=True,
            ),
            "realSize": build_light_year_measurement(
                row,
                row_number,
                sheet_name=sheet_name,
                value_field="realSizeLightYears",
                label_field="realSizeLabel",
                required=True,
            ),
            "visualSize": build_visual_size(row, row_number, sheet_name, required=True),
        }
        absolute_magnitude = build_absolute_magnitude(row, row_number, sheet_name, required=False)
        if absolute_magnitude is not None:
            details["absoluteMagnitude"] = absolute_magnitude
        return details

    if detail_type == "star":
        details: dict[str, Any] = {
            "detailType": detail_type,
            "distance": build_light_year_measurement(
                row,
                row_number,
                sheet_name=sheet_name,
                value_field="distanceLightYears",
                label_field="distanceLabel",
                required=True,
            ),
        }
        real_size = build_light_year_measurement(
            row,
            row_number,
            sheet_name=sheet_name,
            value_field="realSizeLightYears",
            label_field="realSizeLabel",
            required=False,
        )
        visual_size = build_visual_size(row, row_number, sheet_name, required=False)
        if real_size is not None:
            details["realSize"] = real_size
        if visual_size is not None:
            details["visualSize"] = visual_size
        details["absoluteMagnitude"] = build_absolute_magnitude(row, row_number, sheet_name, required=True)
        return details

    if detail_type == "constellation":
        return {
            "detailType": detail_type,
            "visualSize": build_visual_size(row, row_number, sheet_name, required=True),
        }

    if detail_type == "sky_event":
        details = {
            "detailType": detail_type,
        }
        visual_size = build_visual_size(row, row_number, sheet_name, required=False)
        if visual_size is not None:
            details["visualSize"] = visual_size
        return details

    raise CatalogSheetError(
        f"{sheet_name} row {row_number}: unsupported detailType '{detail_type}'. "
        "Use deep_sky, star, constellation or sky_event.",
    )


def build_light_year_measurement(
    row: dict[str, str],
    row_number: int,
    sheet_name: str,
    value_field: str,
    label_field: str,
    required: bool,
) -> dict[str, Any] | None:
    value = parse_float(row, value_field, row_number, sheet_name, required=required)
    if value is None:
        return None
    label = optional_text(row, label_field) or format_light_year_label(value)
    return {
        "lightYears": value,
        "label": label,
    }


def build_absolute_magnitude(
    row: dict[str, str],
    row_number: int,
    sheet_name: str,
    required: bool,
) -> dict[str, Any] | None:
    value = parse_float(row, "absoluteMagnitudeValue", row_number, sheet_name, required=required)
    if value is None:
        return None
    label = optional_text(row, "absoluteMagnitudeLabel") or format_signed_number(value)
    return {
        "value": value,
        "label": label,
    }


def build_visual_size(
    row: dict[str, str],
    row_number: int,
    sheet_name: str,
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
            f"{sheet_name} row {row_number}: visual size is partial. "
            "Fill every visual field or leave them all blank.",
        )

    full_moon_width = parse_float(row, "visualFullMoonWidth", row_number, sheet_name, required=True)
    full_moon_height = parse_float(row, "visualFullMoonHeight", row_number, sheet_name, required=True)

    angular_width = build_angular_measurement(
        row,
        row_number,
        sheet_name=sheet_name,
        degrees_field="angularWidthDegrees",
        minutes_field="angularWidthArcMinutes",
        seconds_field="angularWidthArcSeconds",
        label_field="angularWidthLabel",
    )
    angular_height = build_angular_measurement(
        row,
        row_number,
        sheet_name=sheet_name,
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
    row_number: int,
    sheet_name: str,
    degrees_field: str,
    minutes_field: str,
    seconds_field: str,
    label_field: str,
) -> dict[str, Any]:
    degrees = parse_int(row, degrees_field, row_number, sheet_name, required=True)
    minutes = parse_int(row, minutes_field, row_number, sheet_name, required=True)
    seconds = parse_int(row, seconds_field, row_number, sheet_name, required=True)
    label = optional_text(row, label_field) or format_angular_label(degrees, minutes, seconds)
    return {
        "degrees": degrees,
        "arcMinutes": minutes,
        "arcSeconds": seconds,
        "label": label,
    }


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
        if all(is_json_inline_scalar(item) for item in value.values()):
            parts = [
                f"{json.dumps(key, ensure_ascii=False)}: {render_json(item, indent + 1)}"
                for key, item in value.items()
            ]
            return "{ " + ", ".join(parts) + " }"
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


def is_json_inline_scalar(value: Any) -> bool:
    return isinstance(value, (str, bool, int, float)) or value is None


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


def format_right_ascension_seconds(value: float) -> str:
    text = format_fixed_number(value, 1)
    integer_part, fractional_part = text.split(",", 1)
    return f"{integer_part.zfill(2)},{fractional_part}"


def format_light_year_label(value: float) -> str:
    unit = "annee-lumiere" if abs(value) <= 1 else "annees-lumiere"
    return f"{format_french_number(value)} {unit}"


def decimal_cell(value: Fraction, decimals: int) -> Decimal:
    quantizer = Decimal("1").scaleb(-decimals)
    return (Decimal(value.numerator) / Decimal(value.denominator)).quantize(quantizer)


def stable_number(value: Any) -> str | None:
    if value is None or value == "":
        return None
    if isinstance(value, Decimal):
        return format(value, "f")
    if isinstance(value, Fraction):
        return format(Decimal(value.numerator) / Decimal(value.denominator), "f")
    if isinstance(value, float):
        return format(Decimal(str(value)), "f")
    return str(value)


def rarity_sort_priority(rarity_label: str) -> int:
    try:
        return RARITY_ORDER.index(rarity_label)
    except ValueError:
        return len(RARITY_ORDER)


if __name__ == "__main__":
    raise SystemExit(main())
