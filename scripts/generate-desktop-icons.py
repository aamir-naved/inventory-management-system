#!/usr/bin/env python3
"""Create simple PNG/ICO assets for the Windows desktop shell."""

from __future__ import annotations

import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "desktop" / "src-tauri" / "icons"
ORANGE = (219, 109, 52, 255)
CREAM = (255, 250, 242, 255)
INK = (24, 33, 47, 255)


def png_chunk(tag: bytes, data: bytes) -> bytes:
    return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)


def write_png(path: Path, size: int, pixels: list[tuple[int, int, int, int]]) -> None:
    raw = b"".join(b"\x00" + bytes(channel for pixel in pixels[row * size : (row + 1) * size] for channel in pixel) for row in range(size))
    ihdr = struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n" + png_chunk(b"IHDR", ihdr) + png_chunk(b"IDAT", zlib.compress(raw, 9)) + png_chunk(b"IEND", b"")
    path.write_bytes(png)


def icon_pixels(size: int) -> list[tuple[int, int, int, int]]:
    pixels: list[tuple[int, int, int, int]] = []
    margin = max(1, size // 16)
    for y in range(size):
        for x in range(size):
            if x < margin or y < margin or x >= size - margin or y >= size - margin:
                pixels.append((0, 0, 0, 0))
                continue
            # Rounded-ish square using a distance from corners
            corner = size // 6
            in_corner = (
                (x < margin + corner and y < margin + corner and (margin + corner - x) ** 2 + (margin + corner - y) ** 2 > corner ** 2)
                or (x >= size - margin - corner and y < margin + corner and (x - (size - margin - corner)) ** 2 + (margin + corner - y) ** 2 > corner ** 2)
                or (x < margin + corner and y >= size - margin - corner and (margin + corner - x) ** 2 + (y - (size - margin - corner)) ** 2 > corner ** 2)
                or (x >= size - margin - corner and y >= size - margin - corner and (x - (size - margin - corner)) ** 2 + (y - (size - margin - corner)) ** 2 > corner ** 2)
            )
            if in_corner:
                pixels.append((0, 0, 0, 0))
                continue
            # Ledger lines
            inner = x - margin
            inner_y = y - margin
            body = size - 2 * margin
            if inner_y < body // 4:
                pixels.append(ORANGE)
            elif inner_y % max(3, body // 8) == 0:
                pixels.append(INK)
            elif body // 3 < inner < body * 2 // 3 and body // 2 < inner_y < body * 3 // 4:
                pixels.append(ORANGE)
            else:
                pixels.append(CREAM)
    return pixels


def write_ico(path: Path, png_bytes: bytes, size: int) -> None:
    # PNG-in-ICO (Vista+)
    header = struct.pack("<HHH", 0, 1, 1)
    entry = struct.pack("<BBBBHHII", size if size < 256 else 0, size if size < 256 else 0, 0, 0, 1, 32, len(png_bytes), 22)
    path.write_bytes(header + entry + png_bytes)


def main() -> None:
    ROOT.mkdir(parents=True, exist_ok=True)
    sizes = {
        "32x32.png": 32,
        "128x128.png": 128,
        "128x128@2x.png": 256,
        "icon.png": 512,
    }
    png_32 = None
    for name, size in sizes.items():
        path = ROOT / name
        write_png(path, size, icon_pixels(size))
        if size == 32:
            png_32 = path.read_bytes()
        if size == 256:
            write_ico(ROOT / "icon.ico", path.read_bytes(), 256)
    if png_32 is None:
        raise SystemExit("32px PNG was not generated")


if __name__ == "__main__":
    main()
