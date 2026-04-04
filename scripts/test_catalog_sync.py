from __future__ import annotations

import importlib.util
import json
import sys
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SCRIPT_PATH = ROOT / "scripts" / "catalog_sync.py"
SOURCE_CATALOG_DIR = ROOT / "app" / "src" / "main" / "assets" / "catalog"
SOURCE_WORKBOOK_PATH = ROOT / "catalogue_astronomie.xlsx"


def load_catalog_sync_module():
    spec = importlib.util.spec_from_file_location("catalog_sync_test_module", SCRIPT_PATH)
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


class CatalogSyncTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.catalog_sync = load_catalog_sync_module()

    def create_temp_catalog_dir(self) -> tuple[Path, Path]:
        temp_dir = Path(tempfile.mkdtemp(prefix="catalog-sync-"))
        catalog_dir = temp_dir / "catalog"
        catalog_dir.mkdir(parents=True, exist_ok=True)
        for file_name in ["extensions.json", "cards.json", "variant_profiles.json"]:
            payload = (SOURCE_CATALOG_DIR / file_name).read_bytes()
            (catalog_dir / file_name).write_bytes(payload)
        return temp_dir, catalog_dir

    def read_json(self, path: Path):
        return json.loads(path.read_text(encoding="utf-8"))

    def write_json(self, path: Path, payload) -> None:
        path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    def export_workbook(self, workbook_path: Path, catalog_dir: Path) -> None:
        self.catalog_sync.export_workbook(
            sheet_path=workbook_path,
            extensions=self.read_json(catalog_dir / "extensions.json"),
            cards=self.read_json(catalog_dir / "cards.json"),
            variant_profiles=self.read_json(catalog_dir / "variant_profiles.json"),
            settings=self.catalog_sync.default_global_settings(),
        )

    def read_sheets(self, workbook_path: Path):
        return {sheet.name: sheet for sheet in self.catalog_sync.read_workbook(workbook_path)}

    def write_sheets(self, workbook_path: Path, sheets) -> None:
        ordered_names = [
            self.catalog_sync.CATALOGUE_SHEET_NAME,
            self.catalog_sync.PROBABILITIES_SHEET_NAME,
            self.catalog_sync.RESULTS_SHEET_NAME,
            self.catalog_sync.CALIBRATION_SHEET_NAME,
        ]
        self.catalog_sync.write_workbook(
            workbook_path,
            [sheets[name] for name in ordered_names if name in sheets],
        )

    def set_named_table_value(
        self,
        sheet,
        section_name: str,
        identifier_field: str,
        identifier_value: str,
        target_field: str,
        new_value,
    ) -> None:
        rows = sheet.rows
        section_row = next(
            index for index, row in enumerate(rows) if row and row[0].strip() == section_name
        )
        header_row = rows[section_row + 1]
        header_index = {name: idx for idx, name in enumerate(header_row)}
        data_start = section_row + 2
        for row_index in range(data_start, len(rows)):
            row = rows[row_index]
            if not row or all(not str(value).strip() for value in row):
                break
            if row[0].strip() in {
                self.catalog_sync.GLOBAL_SETTINGS_SECTION,
                self.catalog_sync.VARIANT_PROFILES_SECTION,
                self.catalog_sync.EXTENSION_BALANCE_SECTION,
            }:
                break
            while len(rows[row_index]) <= max(header_index[identifier_field], header_index[target_field]):
                rows[row_index].append("")
            if str(rows[row_index][header_index[identifier_field]]).strip() == identifier_value:
                rows[row_index][header_index[target_field]] = new_value
                return
        raise AssertionError(f"Unable to find {identifier_value} in section {section_name}.")

    def set_catalogue_value(self, sheet, card_id: str, field: str, new_value) -> None:
        header = sheet.rows[0]
        header_index = {name: idx for idx, name in enumerate(header)}
        for row_index in range(1, len(sheet.rows)):
            row = sheet.rows[row_index]
            if not row:
                continue
            while len(row) <= header_index[field]:
                row.append("")
            if row[header_index["cardId"]].strip() == card_id:
                row[header_index[field]] = new_value
                return
        raise AssertionError(f"Unable to find card {card_id}.")

    def find_combo_row(self, result_sheet):
        rows = result_sheet.rows
        section_row = next(
            index for index, row in enumerate(rows) if row and row[0].strip() == "RarestComboChecks"
        )
        header = rows[section_row + 1]
        header_index = {name: idx for idx, name in enumerate(header)}
        data_row = rows[section_row + 2]
        return {
            key: data_row[index]
            for key, index in header_index.items()
        }

    def find_first_section_row(self, sheet, section_name: str):
        rows = sheet.rows
        section_row = next(
            index for index, row in enumerate(rows) if row and row[0].strip() == section_name
        )
        header = rows[section_row + 1]
        header_index = {name: idx for idx, name in enumerate(header)}
        data_row = rows[section_row + 2]
        return {
            key: data_row[index]
            for key, index in header_index.items()
        }

    def get_named_table_value(
        self,
        sheet,
        section_name: str,
        identifier_field: str,
        identifier_value: str,
        target_field: str,
    ):
        for _, row in self.catalog_sync.parse_named_table(sheet.rows, section_name):
            if row[identifier_field] == identifier_value:
                return row[target_field]
        raise AssertionError(f"Unable to find {identifier_value} in section {section_name}.")

    def test_export_then_apply_preserves_existing_weights(self):
        temp_dir, catalog_dir = self.create_temp_catalog_dir()
        workbook_path = temp_dir / "catalogue.xlsx"
        self.export_workbook(workbook_path, catalog_dir)

        original_cards = self.read_json(catalog_dir / "cards.json")
        original_profiles = self.read_json(catalog_dir / "variant_profiles.json")

        _, applied_cards, applied_profiles = self.catalog_sync.apply_workbook(workbook_path, catalog_dir)

        self.assertEqual(
            {card["id"]: card["drawWeight"] for card in original_cards},
            {card["id"]: card["drawWeight"] for card in applied_cards},
        )
        self.assertEqual(
            {
                profile["id"]: {
                    "sky": {entry["code"]: entry["weight"] for entry in profile["skyQualityWeights"]},
                    "finish": {entry["code"]: entry["weight"] for entry in profile["finishWeights"]},
                }
                for profile in original_profiles
            },
            {
                profile["id"]: {
                    "sky": {entry["code"]: entry["weight"] for entry in profile["skyQualityWeights"]},
                    "finish": {entry["code"]: entry["weight"] for entry in profile["finishWeights"]},
                }
                for profile in applied_profiles
            },
        )
        self.assertTrue(all(card["drawWeight"] > 0 for card in applied_cards))

    def test_apply_rejects_non_positive_city_probability(self):
        temp_dir, catalog_dir = self.create_temp_catalog_dir()
        workbook_path = temp_dir / "catalogue.xlsx"
        self.export_workbook(workbook_path, catalog_dir)

        sheets = self.read_sheets(workbook_path)
        probabilities = sheets[self.catalog_sync.PROBABILITIES_SHEET_NAME]
        self.set_named_table_value(
            probabilities,
            self.catalog_sync.VARIANT_PROFILES_SECTION,
            "variantProfileId",
            "observation-default",
            "suburbanMeanDays",
            "0.1",
        )
        self.set_named_table_value(
            probabilities,
            self.catalog_sync.VARIANT_PROFILES_SECTION,
            "variantProfileId",
            "observation-default",
            "ruralMeanDays",
            "0.1",
        )
        self.set_named_table_value(
            probabilities,
            self.catalog_sync.VARIANT_PROFILES_SECTION,
            "variantProfileId",
            "observation-default",
            "mountainMeanDays",
            "0.1",
        )
        self.write_sheets(workbook_path, sheets)

        with self.assertRaisesRegex(self.catalog_sync.CatalogSheetError, "city probability"):
            self.catalog_sync.apply_workbook(workbook_path, catalog_dir)

    def test_apply_rejects_non_positive_standard_probability(self):
        temp_dir, catalog_dir = self.create_temp_catalog_dir()
        workbook_path = temp_dir / "catalogue.xlsx"
        self.export_workbook(workbook_path, catalog_dir)

        sheets = self.read_sheets(workbook_path)
        probabilities = sheets[self.catalog_sync.PROBABILITIES_SHEET_NAME]
        self.set_named_table_value(
            probabilities,
            self.catalog_sync.VARIANT_PROFILES_SECTION,
            "variantProfileId",
            "observation-default",
            "holographicMeanDays",
            "0.04",
        )
        self.write_sheets(workbook_path, sheets)

        with self.assertRaisesRegex(self.catalog_sync.CatalogSheetError, "standard probability"):
            self.catalog_sync.apply_workbook(workbook_path, catalog_dir)

    def test_apply_rejects_non_positive_common_probability(self):
        temp_dir, catalog_dir = self.create_temp_catalog_dir()
        workbook_path = temp_dir / "catalogue.xlsx"
        self.export_workbook(workbook_path, catalog_dir)

        sheets = self.read_sheets(workbook_path)
        probabilities = sheets[self.catalog_sync.PROBABILITIES_SHEET_NAME]
        self.set_named_table_value(
            probabilities,
            self.catalog_sync.EXTENSION_BALANCE_SECTION,
            "extensionId",
            "astronomes-en-herbe",
            "uncommonBaseCardMeanDays",
            "0.2",
        )
        self.set_named_table_value(
            probabilities,
            self.catalog_sync.EXTENSION_BALANCE_SECTION,
            "extensionId",
            "astronomes-en-herbe",
            "rareBaseCardMeanDays",
            "0.2",
        )
        self.set_named_table_value(
            probabilities,
            self.catalog_sync.EXTENSION_BALANCE_SECTION,
            "extensionId",
            "astronomes-en-herbe",
            "rarestComboMeanDays",
            "0.1",
        )
        self.write_sheets(workbook_path, sheets)

        with self.assertRaisesRegex(self.catalog_sync.CatalogSheetError, "Common probability"):
            self.catalog_sync.apply_workbook(workbook_path, catalog_dir)

    def test_higher_card_multiplier_makes_a_card_rarer(self):
        temp_dir, catalog_dir = self.create_temp_catalog_dir()
        workbook_path = temp_dir / "catalogue.xlsx"
        self.export_workbook(workbook_path, catalog_dir)

        sheets = self.read_sheets(workbook_path)
        catalogue = sheets[self.catalog_sync.CATALOGUE_SHEET_NAME]
        self.set_catalogue_value(catalogue, "aeh-m31-andromeda", "cardRarityMultiplier", "4")
        self.set_catalogue_value(catalogue, "aeh-m42-orion-nebula", "cardRarityMultiplier", "1")
        self.write_sheets(workbook_path, sheets)

        _, cards, _ = self.catalog_sync.apply_workbook(workbook_path, catalog_dir)
        weights_by_id = {card["id"]: card["drawWeight"] for card in cards}

        self.assertLess(weights_by_id["aeh-m31-andromeda"], weights_by_id["aeh-m42-orion-nebula"])

    def test_results_sheet_uses_formulas_for_numeric_columns(self):
        temp_dir, catalog_dir = self.create_temp_catalog_dir()
        workbook_path = temp_dir / "catalogue.xlsx"
        self.export_workbook(workbook_path, catalog_dir)
        self.catalog_sync.apply_workbook(workbook_path, catalog_dir)

        result_sheet = self.read_sheets(workbook_path)[self.catalog_sync.RESULTS_SHEET_NAME]
        variant_row = self.find_first_section_row(result_sheet, "VariantProfiles")
        card_row = self.find_first_section_row(result_sheet, "Cards")
        combo_row = self.find_combo_row(result_sheet)

        self.assertTrue(str(variant_row["weight"]).startswith("="))
        self.assertTrue(str(variant_row["probability"]).startswith("="))
        self.assertTrue(str(card_row["drawWeight"]).startswith("="))
        self.assertTrue(str(card_row["meanDays"]).startswith("="))
        self.assertTrue(str(combo_row["comboProbability"]).startswith("="))
        self.assertTrue(str(combo_row["targetMeanDays"]).startswith("="))

    def test_runtime_rarest_combo_mean_days_matches_target(self):
        temp_dir, catalog_dir = self.create_temp_catalog_dir()
        workbook_path = temp_dir / "catalogue.xlsx"
        self.export_workbook(workbook_path, catalog_dir)

        sheets = self.read_sheets(workbook_path)
        target_mean_days = float(
            self.get_named_table_value(
                sheets[self.catalog_sync.PROBABILITIES_SHEET_NAME],
                self.catalog_sync.EXTENSION_BALANCE_SECTION,
                "extensionId",
                "astronomes-en-herbe",
                "rarestComboMeanDays",
            ),
        )

        _, applied_cards, applied_profiles = self.catalog_sync.apply_workbook(workbook_path, catalog_dir)

        settings = self.catalog_sync.default_global_settings()
        cards_by_extension: dict[str, list[dict]] = {}
        for card in applied_cards:
            cards_by_extension.setdefault(card["extensionId"], []).append(card)
        profiles_by_id = {profile["id"]: profile for profile in applied_profiles}
        extension_variant_profiles = self.catalog_sync.require_single_variant_profile_per_extension(applied_cards)

        extension_cards = cards_by_extension["astronomes-en-herbe"]
        total_weight = sum(card["drawWeight"] for card in extension_cards)
        cards_by_rarity: dict[str, list[dict]] = {}
        for card in extension_cards:
            cards_by_rarity.setdefault(card["rarityLabel"], []).append(card)
        highest_rarity = sorted(cards_by_rarity, key=self.catalog_sync.rarity_sort_priority)[-1]
        rarest_card = min(
            cards_by_rarity[highest_rarity],
            key=lambda card: (card["drawWeight"], card["id"]),
        )
        variant_profile = profiles_by_id[extension_variant_profiles["astronomes-en-herbe"]]
        rarest_sky = self.catalog_sync.rarest_weighted_code(variant_profile["skyQualityWeights"])
        holographic = self.catalog_sync.weighted_code_lookup(variant_profile["finishWeights"], "holographic")
        combo_probability = (
            self.catalog_sync.Fraction(rarest_card["drawWeight"], total_weight)
            * self.catalog_sync.Fraction(
                rarest_sky["weight"],
                self.catalog_sync.total_weight_of_codes(variant_profile["skyQualityWeights"]),
            )
            * self.catalog_sync.Fraction(
                holographic["weight"],
                self.catalog_sync.total_weight_of_codes(variant_profile["finishWeights"]),
            )
        )
        actual_mean_days = float(self.catalog_sync.mean_days(combo_probability, settings))

        self.assertAlmostEqual(actual_mean_days, target_mean_days, places=6)

    def test_apply_supports_resultats_workbook_without_probabilities_sheet(self):
        if not SOURCE_WORKBOOK_PATH.exists():
            self.skipTest("Le workbook Resultats source n'est pas disponible dans ce workspace.")

        temp_dir, catalog_dir = self.create_temp_catalog_dir()
        workbook_path = temp_dir / "catalogue-resultats.xlsx"
        workbook_path.write_bytes(SOURCE_WORKBOOK_PATH.read_bytes())

        _, applied_cards, applied_profiles = self.catalog_sync.apply_workbook(workbook_path, catalog_dir)

        weights_by_id = {card["id"]: card["drawWeight"] for card in applied_cards}
        self.assertEqual(20000, weights_by_id["aeh-albireo"])
        self.assertEqual(21429, weights_by_id["aeh-algol"])
        self.assertEqual(18750, weights_by_id["aeh-m16-eagle"])
        self.assertEqual(16667, weights_by_id["aeh-aurora-borealis"])

        profiles_by_id = {profile["id"]: profile for profile in applied_profiles}
        observation_profile = profiles_by_id["observation-default"]
        self.assertEqual(
            {"city": 10, "suburban": 6, "rural": 3, "mountain": 1},
            {entry["code"]: entry["weight"] for entry in observation_profile["skyQualityWeights"]},
        )
        self.assertEqual(
            {"standard": 18, "holographic": 2},
            {entry["code"]: entry["weight"] for entry in observation_profile["finishWeights"]},
        )

        self.assertEqual(
            {"Catalogue", "Donnees", "Resultats"},
            set(self.read_sheets(workbook_path).keys()),
        )


if __name__ == "__main__":
    unittest.main()
