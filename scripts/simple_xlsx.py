from __future__ import annotations

from dataclasses import dataclass
from decimal import Decimal
from pathlib import Path
from typing import Iterable
from xml.etree import ElementTree as ET
from xml.sax.saxutils import escape
import zipfile


MAIN_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
PACKAGE_REL_NS = "http://schemas.openxmlformats.org/package/2006/relationships"

NS = {
    "a": MAIN_NS,
    "r": REL_NS,
}


@dataclass(frozen=True)
class Sheet:
    name: str
    rows: list[list[object | None]]
    hidden: bool = False


@dataclass(frozen=True)
class FormulaCell:
    formula: str
    value: object | None = None


def write_workbook(path: Path, sheets: Iterable[Sheet]) -> None:
    workbook_sheets = list(sheets)
    path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        archive.writestr("[Content_Types].xml", build_content_types_xml(len(workbook_sheets)))
        archive.writestr("_rels/.rels", build_root_relationships_xml())
        archive.writestr("xl/workbook.xml", build_workbook_xml(workbook_sheets))
        archive.writestr("xl/_rels/workbook.xml.rels", build_workbook_relationships_xml(len(workbook_sheets)))
        for index, sheet in enumerate(workbook_sheets, start=1):
            archive.writestr(
                f"xl/worksheets/sheet{index}.xml",
                build_sheet_xml(sheet.rows),
            )


def read_workbook(path: Path) -> list[Sheet]:
    with zipfile.ZipFile(path, "r") as archive:
        workbook_xml = ET.fromstring(archive.read("xl/workbook.xml"))
        workbook_relationships = ET.fromstring(archive.read("xl/_rels/workbook.xml.rels"))
        relationship_targets = {
            relationship.attrib["Id"]: relationship.attrib["Target"]
            for relationship in workbook_relationships.findall(f"{{{PACKAGE_REL_NS}}}Relationship")
        }
        shared_strings = load_shared_strings(archive)

        sheets: list[Sheet] = []
        for sheet_node in workbook_xml.findall("a:sheets/a:sheet", NS):
            relationship_id = sheet_node.attrib[f"{{{REL_NS}}}id"]
            target = relationship_targets[relationship_id]
            rows = read_sheet_rows(archive.read(f"xl/{target}"), shared_strings)
            sheets.append(
                Sheet(
                    name=sheet_node.attrib["name"],
                    rows=rows,
                    hidden=sheet_node.attrib.get("state") == "hidden",
                ),
            )
        return sheets


def build_content_types_xml(sheet_count: int) -> str:
    overrides = [
        (
            f'<Override PartName="/xl/worksheets/sheet{index}.xml" '
            'ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>'
        )
        for index in range(1, sheet_count + 1)
    ]
    overrides.append(
        '<Override PartName="/xl/workbook.xml" '
        'ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>'
    )
    return (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">'
        '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
        '<Default Extension="xml" ContentType="application/xml"/>'
        + "".join(overrides)
        + "</Types>"
    )


def build_root_relationships_xml() -> str:
    return (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        f'<Relationships xmlns="{PACKAGE_REL_NS}">'
        f'<Relationship Id="rId1" '
        'Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" '
        'Target="xl/workbook.xml"/>'
        "</Relationships>"
    )


def build_workbook_xml(sheets: list[Sheet]) -> str:
    sheet_nodes: list[str] = []
    for index, sheet in enumerate(sheets, start=1):
        hidden_attr = ' state="hidden"' if sheet.hidden else ""
        sheet_nodes.append(
            f'<sheet name="{escape_attribute(sheet.name)}" sheetId="{index}" '
            f'r:id="rId{index}"{hidden_attr}/>',
        )
    return (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        f'<workbook xmlns="{MAIN_NS}" xmlns:r="{REL_NS}">'
        "<sheets>"
        + "".join(sheet_nodes)
        + "</sheets>"
        '<calcPr calcId="0" fullCalcOnLoad="1" forceFullCalc="1"/>'
        "</workbook>"
    )


def build_workbook_relationships_xml(sheet_count: int) -> str:
    relationships = [
        (
            f'<Relationship Id="rId{index}" '
            'Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" '
            f'Target="worksheets/sheet{index}.xml"/>'
        )
        for index in range(1, sheet_count + 1)
    ]
    return (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        f'<Relationships xmlns="{PACKAGE_REL_NS}">'
        + "".join(relationships)
        + "</Relationships>"
    )


def build_sheet_xml(rows: list[list[object | None]]) -> str:
    row_nodes: list[str] = []
    for row_index, row in enumerate(rows, start=1):
        cell_nodes: list[str] = []
        for column_index, value in enumerate(row, start=1):
            if value is None or value == "":
                continue
            cell_reference = f"{column_label(column_index)}{row_index}"
            cell_nodes.append(build_cell_xml(cell_reference, value))
        row_nodes.append(f'<row r="{row_index}">' + "".join(cell_nodes) + "</row>")
    return (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        f'<worksheet xmlns="{MAIN_NS}"><sheetData>'
        + "".join(row_nodes)
        + "</sheetData></worksheet>"
    )


def build_cell_xml(reference: str, value: object) -> str:
    if isinstance(value, FormulaCell):
        value_xml = build_formula_value_xml(value.value)
        return f'<c r="{reference}"><f>{escape(value.formula)}</f>{value_xml}</c>'
    if isinstance(value, bool):
        return f'<c r="{reference}" t="b"><v>{"1" if value else "0"}</v></c>'
    if isinstance(value, int):
        return f'<c r="{reference}"><v>{value}</v></c>'
    if isinstance(value, float):
        return f'<c r="{reference}"><v>{value}</v></c>'
    if isinstance(value, Decimal):
        return f'<c r="{reference}"><v>{value}</v></c>'
    text = str(value)
    return (
        f'<c r="{reference}" t="inlineStr"><is><t xml:space="preserve">{escape(text)}</t></is></c>'
    )


def load_shared_strings(archive: zipfile.ZipFile) -> list[str]:
    try:
        payload = archive.read("xl/sharedStrings.xml")
    except KeyError:
        return []
    root = ET.fromstring(payload)
    strings: list[str] = []
    for item in root.findall("a:si", NS):
        strings.append(read_string_item(item))
    return strings


def read_sheet_rows(payload: bytes, shared_strings: list[str]) -> list[list[str]]:
    root = ET.fromstring(payload)
    rows: list[list[str]] = []
    for row_node in root.findall("a:sheetData/a:row", NS):
        row_index = int(row_node.attrib.get("r", str(len(rows) + 1)))
        while len(rows) < row_index - 1:
            rows.append([])
        values: list[str] = []
        expected_column = 1
        for cell_node in row_node.findall("a:c", NS):
            reference = cell_node.attrib.get("r", "")
            column_index = column_index_from_reference(reference) if reference else expected_column
            while expected_column < column_index:
                values.append("")
                expected_column += 1
            values.append(read_cell_value(cell_node, shared_strings))
            expected_column += 1
        rows.append(values)
    return rows


def read_cell_value(cell_node: ET.Element, shared_strings: list[str]) -> str:
    formula = read_child_text(cell_node, "f")
    if formula:
        cached_value = read_child_text(cell_node, "v")
        if cached_value:
            return cached_value
        return "=" + formula
    cell_type = cell_node.attrib.get("t")
    if cell_type == "inlineStr":
        inline_string = cell_node.find("a:is", NS)
        return "".join(inline_string.itertext()) if inline_string is not None else ""
    if cell_type == "s":
        index_text = read_child_text(cell_node, "v")
        if not index_text:
            return ""
        return shared_strings[int(index_text)]
    if cell_type == "b":
        return "1" if read_child_text(cell_node, "v") == "1" else "0"
    formula_text = read_child_text(cell_node, "v")
    return formula_text or ""


def read_child_text(node: ET.Element, local_name: str) -> str:
    child = node.find(f"a:{local_name}", NS)
    return child.text if child is not None and child.text is not None else ""


def read_string_item(node: ET.Element) -> str:
    return "".join(node.itertext())


def column_label(index: int) -> str:
    letters: list[str] = []
    cursor = index
    while cursor > 0:
        cursor, remainder = divmod(cursor - 1, 26)
        letters.append(chr(ord("A") + remainder))
    return "".join(reversed(letters))


def column_index_from_reference(reference: str) -> int:
    letters = []
    for character in reference:
        if character.isalpha():
            letters.append(character.upper())
        else:
            break
    value = 0
    for character in letters:
        value = value * 26 + (ord(character) - ord("A") + 1)
    return value


def escape_attribute(value: str) -> str:
    return escape(value, {'"': "&quot;"})


def build_formula_value_xml(value: object | None) -> str:
    if value is None or value == "":
        return ""
    if isinstance(value, bool):
        return f"<v>{'1' if value else '0'}</v>"
    if isinstance(value, Decimal):
        return f"<v>{value}</v>"
    return f"<v>{escape(str(value))}</v>"
