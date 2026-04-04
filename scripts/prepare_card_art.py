#!/usr/bin/env python3
"""Prepare a card art image for the Android client."""

from __future__ import annotations

import argparse
from pathlib import Path

try:
    from PIL import Image, ImageOps
except ImportError as exc:  # pragma: no cover - defensive CLI guard
    raise SystemExit(
        "Pillow is required to run this script. Install it with: python3 -m pip install Pillow"
    ) from exc


TARGET_WIDTH = 1024
TARGET_HEIGHT = 1796
TARGET_RATIO = TARGET_WIDTH / TARGET_HEIGHT
DEFAULT_QUALITY = 90
RAW_ART_ROOT = Path("artwork/card_art/raw")
RUNTIME_ART_ROOT = Path("app/src/main/assets/card_art")
IMAGE_EXTENSIONS = {
    ".jpg",
    ".jpeg",
    ".png",
    ".webp",
    ".tif",
    ".tiff",
    ".bmp",
    ".gif",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Crop an image to the card ratio, resize it to 1024x1796, and export it as WebP."
        )
    )
    parser.add_argument(
        "input",
        type=Path,
        help="Source image path or raw folder path",
    )
    parser.add_argument(
        "output",
        nargs="?",
        type=Path,
        help=(
            "Output .webp path for a single file, or output root for a directory input. "
            "Defaults to the source filename with a .webp extension for files, and to the "
            "matching runtime asset folder for directories under artwork/card_art/raw/."
        ),
    )
    parser.add_argument(
        "--quality",
        type=int,
        default=DEFAULT_QUALITY,
        help=f"WebP quality from 0 to 100 (default: {DEFAULT_QUALITY})",
    )
    return parser.parse_args()


def resolve_output_path(input_path: Path, output_path: Path | None) -> Path:
    if output_path is not None:
        return output_path
    return input_path.with_suffix(".webp")


def resolve_directory_output_root(input_dir: Path, output_root: Path | None) -> Path:
    if output_root is not None:
        return output_root

    raw_art_root = RAW_ART_ROOT.resolve()
    runtime_art_root = RUNTIME_ART_ROOT.resolve()
    try:
        relative_to_raw = input_dir.relative_to(raw_art_root)
        return runtime_art_root / relative_to_raw
    except ValueError:
        pass

    return input_dir.parent / f"{input_dir.name}_prepared"


def compute_center_crop(width: int, height: int) -> tuple[int, int, int, int]:
    source_ratio = width / height
    if abs(source_ratio - TARGET_RATIO) < 1e-6:
        return (0, 0, width, height)

    if source_ratio > TARGET_RATIO:
        crop_width = min(width, round(height * TARGET_RATIO))
        left = (width - crop_width) // 2
        return (left, 0, left + crop_width, height)

    crop_height = min(height, round(width / TARGET_RATIO))
    top = (height - crop_height) // 2
    return (0, top, width, top + crop_height)


def needs_alpha(image: Image.Image) -> bool:
    return image.mode in {"RGBA", "LA"} or (
        image.mode == "P" and "transparency" in image.info
    )


def prepare_card_art(input_path: Path, output_path: Path, quality: int) -> None:
    if not input_path.is_file():
        raise SystemExit(f"Input file not found: {input_path}")

    if not 0 <= quality <= 100:
        raise SystemExit("Quality must be between 0 and 100.")

    with Image.open(input_path) as source:
        image = ImageOps.exif_transpose(source)
        crop_box = compute_center_crop(image.width, image.height)
        cropped = image.crop(crop_box)
        resized = cropped.resize((TARGET_WIDTH, TARGET_HEIGHT), Image.Resampling.LANCZOS)

        if needs_alpha(resized):
            prepared = resized.convert("RGBA")
        else:
            prepared = resized.convert("RGB")

        output_path.parent.mkdir(parents=True, exist_ok=True)
        prepared.save(output_path, format="WEBP", quality=quality, method=6)

    print(
        "\n".join(
            [
                f"Input: {input_path}",
                f"Crop: left={crop_box[0]}, top={crop_box[1]}, right={crop_box[2]}, bottom={crop_box[3]}",
                f"Output: {output_path}",
                f"Size: {TARGET_WIDTH}x{TARGET_HEIGHT}",
            ]
        )
    )


def iter_input_images(input_dir: Path) -> list[Path]:
    return sorted(
        path
        for path in input_dir.rglob("*")
        if path.is_file()
        and not path.name.startswith(".")
        and path.suffix.lower() in IMAGE_EXTENSIONS
    )


def prepare_card_art_directory(
    input_dir: Path,
    output_root: Path,
    quality: int,
) -> None:
    image_paths = iter_input_images(input_dir)
    if not image_paths:
        raise SystemExit(f"No supported images found in: {input_dir}")

    print(f"Batch input: {input_dir}")
    print(f"Batch output root: {output_root}")

    for image_path in image_paths:
        relative_path = image_path.relative_to(input_dir).with_suffix(".webp")
        output_path = output_root / relative_path
        prepare_card_art(
            input_path=image_path,
            output_path=output_path,
            quality=quality,
        )
        print("---")

    print(f"Prepared {len(image_paths)} image(s).")


def main() -> None:
    args = parse_args()
    input_path = args.input.resolve()
    output_arg = args.output.resolve() if args.output else None

    if input_path.is_dir():
        output_root = resolve_directory_output_root(
            input_dir=input_path,
            output_root=output_arg,
        )
        prepare_card_art_directory(
            input_dir=input_path,
            output_root=output_root,
            quality=args.quality,
        )
        return

    output_path = resolve_output_path(
        input_path=input_path,
        output_path=output_arg,
    )
    prepare_card_art(input_path=input_path, output_path=output_path, quality=args.quality)


if __name__ == "__main__":
    main()
