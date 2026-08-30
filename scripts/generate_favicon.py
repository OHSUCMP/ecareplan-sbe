#!/usr/bin/env python3

import argparse
from pathlib import Path
from PIL import Image


DEFAULT_SIZES = [16, 24, 32, 48, 64, 128, 256]


def parse_sizes(value):
    try:
        sizes = [int(size.strip()) for size in value.split(",") if size.strip()]
    except ValueError as exc:
        raise argparse.ArgumentTypeError("sizes must be a comma-separated list of integers") from exc

    if not sizes:
        raise argparse.ArgumentTypeError("at least one size is required")

    invalid_sizes = [size for size in sizes if size <= 0 or size > 256]
    if invalid_sizes:
        raise argparse.ArgumentTypeError("sizes must be between 1 and 256")

    return sizes


def validate_source_image(image, source_path):
    width, height = image.size

    if width != height:
        raise ValueError(f"{source_path} must be square; got {width}x{height}")

    largest_required_size = max(DEFAULT_SIZES)
    if width < largest_required_size:
        raise ValueError(
            f"{source_path} should be at least {largest_required_size}x{largest_required_size}; "
            f"got {width}x{height}"
        )


def generate_favicon(source_path, output_path, sizes):
    source_path = Path(source_path)
    output_path = Path(output_path)

    if not source_path.exists():
        raise FileNotFoundError(f"source PNG not found: {source_path}")

    output_path.parent.mkdir(parents=True, exist_ok=True)

    with Image.open(source_path) as image:
        if image.format != "PNG":
            raise ValueError(f"{source_path} must be a PNG file")

        validate_source_image(image, source_path)

        image = image.convert("RGBA")

        icon_sizes = [(size, size) for size in sizes]
        image.save(output_path, format="ICO", sizes=icon_sizes)

    print(f"Wrote {output_path}")
    print("Included sizes: " + ", ".join(f"{size}x{size}" for size in sizes))


def main():
    parser = argparse.ArgumentParser(
        description="Create a multi-size favicon.ico from a square PNG."
    )
    parser.add_argument(
        "source",
        help="Path to a square source PNG, ideally at least 256x256."
    )
    parser.add_argument(
        "-o",
        "--output",
        default="src/main/resources/static/favicon.ico",
        help="Output .ico path. Default: src/main/resources/static/favicon.ico"
    )
    parser.add_argument(
        "--sizes",
        type=parse_sizes,
        default=DEFAULT_SIZES,
        help="Comma-separated icon sizes to include. Default: 16,24,32,48,64,128,256"
    )

    args = parser.parse_args()
    generate_favicon(args.source, args.output, args.sizes)


if __name__ == "__main__":
    main()