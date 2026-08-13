#!/usr/bin/env python3
"""生成辉石彗星头 / 光晕贴图，可供后续辉石弹道复用。"""
from __future__ import annotations

import struct
import zlib
from pathlib import Path

PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"


def png_chunk(chunk_type: bytes, data: bytes) -> bytes:
    chunk = chunk_type + data
    return struct.pack(">I", len(data)) + chunk + struct.pack(">I", zlib.crc32(chunk) & 0xFFFFFFFF)


def save_rgba(path: Path, width: int, height: int, pixels: list[tuple[int, int, int, int]]) -> None:
    raw = bytearray()
    for y in range(height):
        raw.append(0)
        for x in range(width):
            raw.extend(pixels[y * width + x])
    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(
        PNG_SIGNATURE
        + png_chunk(b"IHDR", ihdr)
        + png_chunk(b"IDAT", zlib.compress(bytes(raw), 9))
        + png_chunk(b"IEND", b"")
    )
    print(f"Wrote {path}")


def crystal_texture(size: int = 16) -> list[tuple[int, int, int, int]]:
    """青色晶体表面：对角高光 + 深蓝青底。"""
    pixels: list[tuple[int, int, int, int]] = []
    for y in range(size):
        for x in range(size):
            u = x / (size - 1)
            v = y / (size - 1)
            # 斜向高光带
            highlight = max(0.0, 1.0 - abs((u - v) * 2.2))
            edge = min(u, v, 1 - u, 1 - v) * 4
            edge = max(0.0, min(1.0, edge))
            base_r, base_g, base_b = 20, 140, 160
            hi_r, hi_g, hi_b = 210, 255, 255
            t = highlight * 0.75 + 0.15
            r = int(base_r + (hi_r - base_r) * t)
            g = int(base_g + (hi_g - base_g) * t)
            b = int(base_b + (hi_b - base_b) * t)
            # 边缘略暗，增强立体感
            shade = 0.55 + 0.45 * edge
            pixels.append((int(r * shade), int(g * shade), int(b * shade), 255))
    return pixels


def glow_texture(size: int = 32) -> list[tuple[int, int, int, int]]:
    """柔和径向辉光，中心近白青、外圈透明。"""
    pixels: list[tuple[int, int, int, int]] = []
    cx = cy = (size - 1) / 2.0
    for y in range(size):
        for x in range(size):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5 / (size * 0.5)
            if d >= 1.0:
                pixels.append((0, 0, 0, 0))
                continue
            falloff = (1.0 - d) ** 2
            r = int(80 + 160 * falloff)
            g = int(220 + 35 * falloff)
            b = int(230 + 25 * falloff)
            a = int(200 * falloff)
            pixels.append((r, g, b, a))
    return pixels


def main() -> int:
    root = Path(__file__).resolve().parents[1]
    entity = root / "src" / "main" / "resources" / "assets" / "elden_ring_spells" / "textures" / "entity" / "glintstone"
    save_rgba(entity / "comet_head.png", 16, 16, crystal_texture(16))
    save_rgba(entity / "comet_glow.png", 32, 32, glow_texture(32))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
