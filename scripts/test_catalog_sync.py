from __future__ import annotations

import argparse
import importlib.util
import json
import sys
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SCRIPT_PATH = ROOT / "scripts" / "catalog_sync.py"
SOURCE_CATALOG_DIR = ROOT / "app" / "src" / "main" / "assets" / "catalog"


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
        for file_name in [
            "extensions.json",
            "cards.json",
            "variant_profiles.json",
            "game_balance.json",
            "equipment_cards.json",
            "equipment_settings.json",
        ]:
            payload = (SOURCE_CATALOG_DIR / file_name).read_bytes()
            (catalog_dir / file_name).write_bytes(payload)
        return temp_dir, catalog_dir

    def read_json(self, path: Path):
        return json.loads(path.read_text(encoding="utf-8"))

    def export_workbook(self, workbook_path: Path, catalog_dir: Path) -> None:
        self.catalog_sync.export_workbook(
            sheet_path=workbook_path,
            extensions=self.read_json(catalog_dir / "extensions.json"),
            cards=self.read_json(catalog_dir / "cards.json"),
            variant_profiles=self.read_json(catalog_dir / "variant_profiles.json"),
            balance_data=self.catalog_sync.load_balance_data(catalog_dir / "game_balance.json"),
            equipment_cards=self.catalog_sync.load_equipment_cards_data(catalog_dir / "equipment_cards.json"),
            equipment_settings=self.catalog_sync.load_equipment_settings_data(catalog_dir / "equipment_settings.json"),
        )

    def read_sheets(self, workbook_path: Path):
        return {sheet.name: sheet for sheet in self.catalog_sync.read_workbook(workbook_path)}

    def write_sheets(self, workbook_path: Path, sheets) -> None:
        ordered_names = [
            self.catalog_sync.CATALOGUE_SHEET_NAME,
            self.catalog_sync.DATA_SHEET_NAME,
            self.catalog_sync.EQUIPMENT_SHEET_NAME,
            self.catalog_sync.RESULTS_SHEET_NAME,
        ]
        self.catalog_sync.write_workbook(
            workbook_path,
            [sheets[name] for name in ordered_names if name in sheets],
        )

    def set_cell(self, sheet, row_number: int, column_number: int, new_value) -> None:
        row_index = row_number - 1
        column_index = column_number - 1
        while len(sheet.rows) <= row_index:
            sheet.rows.append([])
        while len(sheet.rows[row_index]) <= column_index:
            sheet.rows[row_index].append("")
        sheet.rows[row_index][column_index] = new_value

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

    def section_rows(self, sheet, section_name: str):
        return [
            row
            for _, row in self.catalog_sync.parse_section_table(
                sheet.rows,
                section_name,
                self.catalog_sync.RESULTS_SHEET_NAME,
            )
        ]

    def build_result_sheet(
        self,
        extensions,
        cards,
        variant_profiles,
        balance_data,
        equipment_cards,
    ):
        raw_rows = self.catalog_sync.build_results_sheet_rows(
            extensions=extensions,
            cards=cards,
            variant_profiles=variant_profiles,
            balance_data=balance_data,
            equipment_cards=equipment_cards,
        )
        return self.catalog_sync.Sheet(
            self.catalog_sync.RESULTS_SHEET_NAME,
            [
                ["" if value is None else str(value) for value in row]
                for row in raw_rows
            ],
        )

    def test_export_then_apply_preserves_raw_catalog_and_balance(self):
        temp_dir, catalog_dir = self.create_temp_catalog_dir()
        workbook_path = temp_dir / "catalogue.xlsx"
        self.export_workbook(workbook_path, catalog_dir)

        original_cards = self.read_json(catalog_dir / "cards.json")
        original_profiles = self.read_json(catalog_dir / "variant_profiles.json")
        original_balance = self.read_json(catalog_dir / "game_balance.json")
        original_equipment_cards = self.read_json(catalog_dir / "equipment_cards.json")
        original_equipment_settings = self.read_json(catalog_dir / "equipment_settings.json")

        _, applied_cards, applied_profiles, applied_balance, applied_equipment_cards, applied_equipment_settings = (
            self.catalog_sync.apply_workbook(workbook_path, catalog_dir)
        )

        self.assertEqual(
            {card["id"]: card["cardRarityMultiplier"] for card in original_cards},
            {card["id"]: card["cardRarityMultiplier"] for card in applied_cards},
        )
        self.assertEqual(original_profiles, applied_profiles)
        self.assertEqual(original_balance, self.catalog_sync.balance_data_to_json(applied_balance))
        self.assertEqual(original_equipment_cards, applied_equipment_cards)
        self.assertEqual(
            original_equipment_settings,
            self.catalog_sync.equipment_settings_to_json(applied_equipment_settings),
        )
        self.assertEqual(
            {"Catalogue", "Donnees", "Equipements", "Resultats"},
            set(self.read_sheets(workbook_path).keys()),
        )

    def test_apply_does_not_rewrite_workbook(self):
        temp_dir, catalog_dir = self.create_temp_catalog_dir()
        workbook_path = temp_dir / "catalogue.xlsx"
        self.export_workbook(workbook_path, catalog_dir)

        sheets = self.read_sheets(workbook_path)
        data_sheet = sheets[self.catalog_sync.DATA_SHEET_NAME]
        self.set_cell(data_sheet, 19, 1, "EquipmentChancePercent")
        self.set_cell(data_sheet, 19, 2, "7.5")
        self.write_sheets(workbook_path, sheets)

        workbook_bytes_before_apply = workbook_path.read_bytes()
        self.catalog_sync.apply_workbook(workbook_path, catalog_dir)

        self.assertEqual(workbook_bytes_before_apply, workbook_path.read_bytes())

    def test_export_command_is_read_only_error(self):
        with self.assertRaisesRegex(self.catalog_sync.CatalogSheetError, "read-only"):
            self.catalog_sync.handle_export(argparse.Namespace(sheet=Path("catalogue.xlsx")))

    def test_apply_rejects_non_positive_city_probability(self):
        temp_dir, catalog_dir = self.create_temp_catalog_dir()
        workbook_path = temp_dir / "catalogue.xlsx"
        self.export_workbook(workbook_path, catalog_dir)

        sheets = self.read_sheets(workbook_path)
        data_sheet = sheets[self.catalog_sync.DATA_SHEET_NAME]
        self.set_cell(data_sheet, 7, 2, "10")
        self.set_cell(data_sheet, 8, 2, "10")
        self.set_cell(data_sheet, 9, 2, "10")
        self.write_sheets(workbook_path, sheets)

        with self.assertRaisesRegex(self.catalog_sync.CatalogSheetError, "city probability"):
            self.catalog_sync.apply_workbook(workbook_path, catalog_dir)

    def test_apply_rejects_non_positive_standard_probability(self):
        temp_dir, catalog_dir = self.create_temp_catalog_dir()
        workbook_path = temp_dir / "catalogue.xlsx"
        self.export_workbook(workbook_path, catalog_dir)

        sheets = self.read_sheets(workbook_path)
        data_sheet = sheets[self.catalog_sync.DATA_SHEET_NAME]
        self.set_cell(data_sheet, 11, 4, "100")
        self.write_sheets(workbook_path, sheets)

        with self.assertRaisesRegex(self.catalog_sync.CatalogSheetError, "standard probability"):
            self.catalog_sync.apply_workbook(workbook_path, catalog_dir)

    def test_apply_rejects_non_positive_common_probability(self):
        temp_dir, catalog_dir = self.create_temp_catalog_dir()
        workbook_path = temp_dir / "catalogue.xlsx"
        self.export_workbook(workbook_path, catalog_dir)

        sheets = self.read_sheets(workbook_path)
        data_sheet = sheets[self.catalog_sync.DATA_SHEET_NAME]
        self.set_cell(data_sheet, 15, 5, "40")
        self.set_cell(data_sheet, 16, 5, "35")
        self.set_cell(data_sheet, 17, 5, "25")
        self.write_sheets(workbook_path, sheets)

        with self.assertRaisesRegex(self.catalog_sync.CatalogSheetError, "common probability"):
            self.catalog_sync.apply_workbook(workbook_path, catalog_dir)

    def test_higher_card_multiplier_makes_a_card_more_frequent(self):
        temp_dir, catalog_dir = self.create_temp_catalog_dir()
        workbook_path = temp_dir / "catalogue.xlsx"
        self.export_workbook(workbook_path, catalog_dir)

        sheets = self.read_sheets(workbook_path)
        catalogue = sheets[self.catalog_sync.CATALOGUE_SHEET_NAME]
        self.set_catalogue_value(catalogue, "aeh-m31-andromeda", "cardRarityMultiplier", "4")
        self.set_catalogue_value(catalogue, "aeh-m42-orion-nebula", "cardRarityMultiplier", "1")
        self.write_sheets(workbook_path, sheets)

        extensions, cards, variant_profiles, balance_data, equipment_cards, _ = self.catalog_sync.apply_workbook(
            workbook_path,
            catalog_dir,
        )
        result_sheet = self.build_result_sheet(
            extensions=extensions,
            cards=cards,
            variant_profiles=variant_profiles,
            balance_data=balance_data,
            equipment_cards=equipment_cards,
        )
        rows = self.section_rows(result_sheet, "Cards")
        probabilities_by_id = {
            row["cardId"]: float(row["finalProbability"])
            for row in rows
        }

        self.assertGreater(probabilities_by_id["aeh-m31-andromeda"], probabilities_by_id["aeh-m42-orion-nebula"])

    def test_extension_rarity_probabilities_are_renormalized_over_present_rarities(self):
        temp_dir, catalog_dir = self.create_temp_catalog_dir()
        workbook_path = temp_dir / "catalogue.xlsx"
        self.export_workbook(workbook_path, catalog_dir)

        sheets = self.read_sheets(workbook_path)
        catalogue = sheets[self.catalog_sync.CATALOGUE_SHEET_NAME]
        extra_extension_row = ["extension", "rare-et-epic", "Rare et Epic", "cover_rare_epic"]
        header = catalogue.rows[0]
        template_card = {name: "" for name in header}
        template_card.update(
            {
                "rowType": "card",
                "extensionId": "rare-et-epic",
                "rarityLabel": "Rare",
                "cardRarityMultiplier": "1",
                "imageRef": "img_1",
                "variantProfileId": "observation-default",
                "commonName": "Rare 1",
                "primaryCatalogName": "Messier",
                "catalogNumber": "R-1",
                "objectFamily": "deep_sky",
                "objectTypeLabel": "Nebuleuse",
                "constellation": "Orion",
                "mainSeason": "Hiver",
                "rightAscensionHours": "5",
                "rightAscensionMinutes": "35",
                "rightAscensionSeconds": "17.3",
                "rightAscensionLabel": "AD 05h 35m 17,3s",
                "declinationSign": "-",
                "declinationDegrees": "5",
                "declinationArcMinutes": "23",
                "declinationArcSeconds": "28",
                "declinationLabel": "-05° 23′ 28″",
                "coordinatesLabel": "AD 05h 35m 17,3s ; Dec -05° 23′ 28″",
                "shortDescription": "Carte rare",
                "detailType": "deep_sky",
                "distanceLightYears": "1500",
                "distanceLabel": "1 500 annees-lumiere",
                "realSizeLightYears": "25",
                "realSizeLabel": "25 annees-lumiere",
                "visualFullMoonWidth": "2.1",
                "visualFullMoonHeight": "1.94",
                "angularWidthDegrees": "1",
                "angularWidthArcMinutes": "5",
                "angularWidthArcSeconds": "0",
                "angularWidthLabel": "1°05′00″",
                "angularHeightDegrees": "1",
                "angularHeightArcMinutes": "0",
                "angularHeightArcSeconds": "0",
                "angularHeightLabel": "1°00′00″",
                "visualSizeLabel": "2,10 × 1,94 (1°05′00″ × 1°00′00″)",
                "absoluteMagnitudeValue": "-4.1",
                "absoluteMagnitudeLabel": "-4.1",
            },
        )
        rare_card = template_card | {"cardId": "rare-1", "name": "Rare 1", "catalogNumber": "R-1", "rarityLabel": "Rare"}
        epic_card = template_card | {"cardId": "epic-1", "name": "Epic 1", "catalogNumber": "E-1", "rarityLabel": "Epic"}
        catalogue.rows.append(extra_extension_row)
        catalogue.rows.append([rare_card.get(column, "") for column in header])
        catalogue.rows.append([epic_card.get(column, "") for column in header])
        self.write_sheets(workbook_path, sheets)

        extensions, cards, variant_profiles, balance_data, equipment_cards, _ = self.catalog_sync.apply_workbook(
            workbook_path,
            catalog_dir,
        )
        result_sheet = self.build_result_sheet(
            extensions=extensions,
            cards=cards,
            variant_profiles=variant_profiles,
            balance_data=balance_data,
            equipment_cards=equipment_cards,
        )
        rarity_rows = [
            row for row in self.section_rows(result_sheet, "RaritySummary")
            if row["extensionId"] == "rare-et-epic"
        ]
        probabilities_by_rarity = {
            row["rarityLabel"]: float(row["extensionProbability"])
            for row in rarity_rows
        }

        self.assertAlmostEqual(0.75, probabilities_by_rarity["Rare"], places=6)
        self.assertAlmostEqual(0.25, probabilities_by_rarity["Epic"], places=6)

    def test_apply_rejects_invalid_equipment_bonus_unit(self):
        temp_dir, catalog_dir = self.create_temp_catalog_dir()
        workbook_path = temp_dir / "catalogue.xlsx"
        self.export_workbook(workbook_path, catalog_dir)

        sheets = self.read_sheets(workbook_path)
        equipment_sheet = sheets[self.catalog_sync.EQUIPMENT_SHEET_NAME]
        header = equipment_sheet.rows[0]
        bonus_unit_index = header.index("bonusUnit")
        equipment_sheet.rows[1][bonus_unit_index] = "rarityBoost"
        self.write_sheets(workbook_path, sheets)

        with self.assertRaisesRegex(self.catalog_sync.CatalogSheetError, "must use bonusUnit"):
            self.catalog_sync.apply_workbook(workbook_path, catalog_dir)

    def test_apply_persists_equipment_settings_and_builds_equipment_diagnostics(self):
        temp_dir, catalog_dir = self.create_temp_catalog_dir()
        workbook_path = temp_dir / "catalogue.xlsx"
        self.export_workbook(workbook_path, catalog_dir)

        sheets = self.read_sheets(workbook_path)
        data_sheet = sheets[self.catalog_sync.DATA_SHEET_NAME]
        self.set_cell(data_sheet, 19, 1, "EquipmentChancePercent")
        self.set_cell(data_sheet, 19, 2, "7.5")
        self.write_sheets(workbook_path, sheets)

        (
            extensions,
            cards,
            variant_profiles,
            balance_data,
            equipment_cards,
            applied_equipment_settings,
        ) = self.catalog_sync.apply_workbook(workbook_path, catalog_dir)
        self.assertEqual(
            {"commonReplacementChancePercent": 7.5},
            self.catalog_sync.equipment_settings_to_json(applied_equipment_settings),
        )

        result_sheet = self.build_result_sheet(
            extensions=extensions,
            cards=cards,
            variant_profiles=variant_profiles,
            balance_data=balance_data,
            equipment_cards=equipment_cards,
        )
        equipment_rows = self.section_rows(result_sheet, self.catalog_sync.RESULTS_EQUIPMENT_SECTION)
        self.assertTrue(equipment_rows)
        self.assertEqual(
            {"equipmentCardId", "equipmentType", "displayName", "level", "packsAffected", "bonusValue", "bonusUnit", "dropWeight", "description"},
            set(equipment_rows[0].keys()),
        )

    def test_apply_reads_legacy_equipment_chance_from_b19(self):
        temp_dir, catalog_dir = self.create_temp_catalog_dir()
        workbook_path = temp_dir / "catalogue.xlsx"
        self.export_workbook(workbook_path, catalog_dir)

        sheets = self.read_sheets(workbook_path)
        data_sheet = sheets[self.catalog_sync.DATA_SHEET_NAME]
        self.set_cell(data_sheet, 19, 1, "EquipmentChancePercent")
        self.set_cell(data_sheet, 19, 2, "12.5")
        self.write_sheets(workbook_path, sheets)

        _, _, _, _, _, applied_equipment_settings = self.catalog_sync.apply_workbook(workbook_path, catalog_dir)

        self.assertEqual(
            {"commonReplacementChancePercent": 12.5},
            self.catalog_sync.equipment_settings_to_json(applied_equipment_settings),
        )

    def test_apply_writes_raw_assets_without_precomputed_weights(self):
        temp_dir, catalog_dir = self.create_temp_catalog_dir()
        workbook_path = temp_dir / "catalogue.xlsx"
        self.export_workbook(workbook_path, catalog_dir)

        self.catalog_sync.apply_workbook(workbook_path, catalog_dir)

        cards = self.read_json(catalog_dir / "cards.json")
        variant_profiles = self.read_json(catalog_dir / "variant_profiles.json")
        balance = self.read_json(catalog_dir / "game_balance.json")

        self.assertIn("cardRarityMultiplier", cards[0])
        self.assertNotIn("drawWeight", cards[0])
        self.assertNotIn("skyQualityWeights", variant_profiles[0])
        self.assertNotIn("finishWeights", variant_profiles[0])
        self.assertEqual(5, balance["cardsPerDraw"])


if __name__ == "__main__":
    unittest.main()
